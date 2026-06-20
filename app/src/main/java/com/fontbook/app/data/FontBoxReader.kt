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
