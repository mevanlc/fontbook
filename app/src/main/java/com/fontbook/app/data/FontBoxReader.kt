package com.fontbook.app.data

import android.util.Log
import com.fontbook.app.model.FontMetadata
import org.apache.fontbox.ttf.NamingTable
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.fontbox.ttf.TrueTypeFont
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Parses font metadata with Apache FontBox (`org.apache.pdfbox:fontbox`).
 *
 * We deliberately touch only the metadata tables (`name`, `head`, `OS/2`) and
 * never request glyph outlines — `TrueTypeFont.getPath()` returns an AWT
 * `GeneralPath`, which does not exist on Android. Parsing is wrapped so any
 * failure (including a class-load issue) degrades to null rather than crashing.
 */
object FontBoxReader {

    private const val TAG = "FontBoxReader"
    private const val TAG_TTCF = 0x74746366 // 'ttcf'
    private const val TAG_OTTO = 0x4F54544F // 'OTTO'

    // OpenType `name` IDs.
    private const val ID_COPYRIGHT = 0
    private const val ID_FULL = 4
    private const val ID_VERSION = 5
    private const val ID_TRADEMARK = 7
    private const val ID_MANUFACTURER = 8
    private const val ID_DESIGNER = 9
    private const val ID_DESCRIPTION = 10
    private const val ID_VENDOR_URL = 11
    private const val ID_DESIGNER_URL = 12
    private const val ID_LICENSE = 13
    private const val ID_LICENSE_URL = 14

    // OS/2 `sFamilyClass` — high byte: IBM font class.
    private val FAMILY_CLASSES = mapOf(
        1 to "Oldstyle Serifs",
        2 to "Transitional Serifs",
        3 to "Modern Serifs",
        4 to "Clarendon Serifs",
        5 to "Slab Serifs",
        7 to "Freeform Serifs",
        8 to "Sans Serif",
        9 to "Ornamentals",
        10 to "Scripts",
        12 to "Symbolic",
    )

    // OS/2 `sFamilyClass` — low byte: class-specific subclass.
    private val FAMILY_SUBCLASSES: Map<Int, Map<Int, String>> = mapOf(
        1 to mapOf(
            1 to "IBM Rounded Legibility", 2 to "Garalde", 3 to "Venetian",
            4 to "Modified Venetian", 5 to "Dutch Modern", 6 to "Dutch Traditional",
            7 to "Contemporary", 8 to "Calligraphic", 15 to "Miscellaneous",
        ),
        2 to mapOf(1 to "Direct Line", 2 to "Script", 15 to "Miscellaneous"),
        3 to mapOf(1 to "Italian", 2 to "Script", 15 to "Miscellaneous"),
        4 to mapOf(
            1 to "Clarendon", 2 to "Modern", 3 to "Traditional", 4 to "Newspaper",
            5 to "Stub Serif", 6 to "Monotone", 7 to "Typewriter", 15 to "Miscellaneous",
        ),
        5 to mapOf(
            1 to "Monotone", 2 to "Humanist", 3 to "Geometric", 4 to "Swiss",
            5 to "Typewriter", 15 to "Miscellaneous",
        ),
        7 to mapOf(1 to "Modern", 15 to "Miscellaneous"),
        8 to mapOf(
            1 to "IBM Neo-grotesque Gothic", 2 to "Humanist",
            3 to "Low-x Round Geometric", 4 to "High-x Round Geometric",
            5 to "Neo-grotesque Gothic", 6 to "Modified Neo-grotesque Gothic",
            9 to "Typewriter Gothic", 10 to "Matrix", 15 to "Miscellaneous",
        ),
        9 to mapOf(
            1 to "Engraver", 2 to "Black Letter", 3 to "Decorative",
            4 to "Three Dimensional", 15 to "Miscellaneous",
        ),
        10 to mapOf(
            1 to "Uncial", 2 to "Brush Joined", 3 to "Formal Joined",
            4 to "Monotone Joined", 5 to "Calligraphic", 6 to "Brush Unjoined",
            7 to "Formal Unjoined", 8 to "Monotone Unjoined", 15 to "Miscellaneous",
        ),
        12 to mapOf(
            3 to "Mixed Serif", 6 to "Oldstyle Serif",
            7 to "Neo-grotesque Sans Serif", 15 to "Miscellaneous",
        ),
    )

    fun read(file: File): FontMetadata? = try {
        when (readTag(file)) {
            TAG_TTCF -> {
                var meta: FontMetadata? = null
                TrueTypeCollection(file).use { collection ->
                    collection.processAllFonts { ttf ->
                        if (meta == null) meta = extract(ttf, file)
                    }
                }
                meta
            }
            TAG_OTTO -> OTFParser().parse(file).use { extract(it, file) }
            else -> TTFParser().parse(file).use { extract(it, file) }
        }
    } catch (t: Throwable) {
        // Throwable on purpose: also swallow NoClassDefFoundError from any AWT path.
        Log.w(TAG, "Failed to parse ${file.name}", t)
        null
    }

    private fun extract(font: TrueTypeFont, file: File): FontMetadata {
        val naming = runCatching { font.naming }.getOrNull()
        val names = naming?.let(::bestNames) ?: emptyMap()
        val os2 = runCatching { font.getOS2Windows() }.getOrNull()
        val header = runCatching { font.header }.getOrNull()

        val tables = runCatching { font.tableMap.keys }.getOrNull() ?: emptySet()
        val format = when {
            "CFF " in tables -> "OpenType · CFF (PostScript outlines)"
            "glyf" in tables -> "TrueType outlines"
            else -> "OpenType"
        }

        val version = names[ID_VERSION]
            ?: header?.fontRevision?.takeIf { it > 0f }?.let { String.format(Locale.US, "%.3f", it) }

        return FontMetadata(
            format = format,
            numGlyphs = runCatching { font.numberOfGlyphs }.getOrNull(),
            unitsPerEm = runCatching { font.unitsPerEm }.getOrNull()?.takeIf { it > 0 },
            family = runCatching { naming?.fontFamily }.getOrNull(),
            subfamily = runCatching { naming?.fontSubFamily }.getOrNull(),
            fullName = names[ID_FULL],
            postScriptName = runCatching { naming?.postScriptName }.getOrNull(),
            version = version,
            copyright = names[ID_COPYRIGHT],
            manufacturer = names[ID_MANUFACTURER],
            designer = names[ID_DESIGNER],
            description = names[ID_DESCRIPTION],
            vendorUrl = names[ID_VENDOR_URL],
            designerUrl = names[ID_DESIGNER_URL],
            license = names[ID_LICENSE],
            licenseUrl = names[ID_LICENSE_URL],
            trademark = names[ID_TRADEMARK],
            vendorId = os2?.achVendId?.trim()?.takeIf { it.isNotBlank() },
            weightClass = os2?.weightClass?.let(::weightLabel),
            widthClass = os2?.widthClass?.let(::widthLabel),
            familyClass = os2?.familyClass?.let(::familyClassLabel),
            panoseFamily = os2?.panose?.let(::panoseFamilyLabel),
            embedding = os2?.fsType?.toInt()?.let(::embeddingLabel),
            created = header?.created?.let(::formatDate),
            modified = header?.modified?.let(::formatDate),
            fileName = file.name,
            filePath = file.path,
            fileSizeBytes = file.length(),
        )
    }

    /** Picks the best string per name ID, preferring the English Windows record. */
    private fun bestNames(naming: NamingTable): Map<Int, String> {
        val best = HashMap<Int, Pair<Int, String>>()
        for (record in naming.nameRecords) {
            val value = record.string?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val score = nameScore(record.platformId, record.languageId)
            val current = best[record.nameId]
            if (current == null || score > current.first) {
                best[record.nameId] = score to value
            }
        }
        return best.mapValues { it.value.second }
    }

    private fun nameScore(platformId: Int, languageId: Int): Int = when {
        platformId == 3 && languageId == 0x409 -> 100
        platformId == 3 -> 80
        platformId == 0 -> 60
        platformId == 1 && languageId == 0 -> 40
        platformId == 1 -> 20
        else -> 10
    }

    private fun readTag(file: File): Int = try {
        RandomAccessFile(file, "r").use { it.readInt() }
    } catch (_: Exception) {
        0
    }

    private fun formatDate(calendar: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

    private fun weightLabel(weight: Int): String {
        val name = when (weight) {
            100 -> "Thin"
            200 -> "Extra Light"
            300 -> "Light"
            400 -> "Regular"
            500 -> "Medium"
            600 -> "Semi Bold"
            700 -> "Bold"
            800 -> "Extra Bold"
            900 -> "Black"
            else -> null
        }
        return if (name != null) "$weight ($name)" else "$weight"
    }

    private fun widthLabel(width: Int): String {
        val name = when (width) {
            1 -> "Ultra Condensed"
            2 -> "Extra Condensed"
            3 -> "Condensed"
            4 -> "Semi Condensed"
            5 -> "Normal"
            6 -> "Semi Expanded"
            7 -> "Expanded"
            8 -> "Extra Expanded"
            9 -> "Ultra Expanded"
            else -> null
        }
        return if (name != null) "$width ($name)" else "$width"
    }

    /**
     * Decodes the OS/2 `sFamilyClass` field: the high byte is an IBM font class,
     * the low byte a class-specific subclass. Returns null when unclassified.
     */
    private fun familyClassLabel(value: Int): String? {
        val classId = (value shr 8) and 0xFF
        val subId = value and 0xFF
        val className = FAMILY_CLASSES[classId] ?: return null
        val subName = FAMILY_SUBCLASSES[classId]?.get(subId)
            ?.takeIf { subId != 0 } // 0 == "No Classification", not worth repeating
        return if (subName != null) "$className · $subName" else className
    }

    /** Decodes the first PANOSE digit (bFamilyType). Returns null when "Any". */
    private fun panoseFamilyLabel(panose: ByteArray): String? {
        val type = panose.getOrNull(0)?.toInt()?.and(0xFF) ?: return null
        return when (type) {
            1 -> "No Fit"
            2 -> "Latin Text"
            3 -> "Latin Hand Written"
            4 -> "Latin Decorative"
            5 -> "Latin Symbol"
            else -> null // 0 == "Any" (the common all-zero default)
        }
    }

    private fun embeddingLabel(fsType: Int): String {
        val t = fsType and 0xFFFF
        val base = when {
            t == 0 -> "Installable (no restrictions)"
            t and 0x0002 != 0 -> "Restricted license"
            t and 0x0004 != 0 -> "Preview & print"
            t and 0x0008 != 0 -> "Editable"
            else -> "Installable"
        }
        return buildString {
            append(base)
            if (t and 0x0100 != 0) append(" · no subsetting")
            if (t and 0x0200 != 0) append(" · bitmap only")
        }
    }
}
