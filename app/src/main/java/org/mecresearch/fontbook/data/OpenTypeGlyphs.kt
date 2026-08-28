package org.mecresearch.fontbook.data

import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeCollection
import org.apache.fontbox.ttf.TrueTypeFont
import java.io.File
import java.io.RandomAccessFile

object OpenTypeGlyphs {

    private const val TAG_TTCF = 0x74746366 // 'ttcf'
    private const val TAG_OTTO = 0x4F54544F // 'OTTO'

    data class Glyph(val codePoint: Int) {
        val text: String = String(Character.toChars(codePoint))
        val label: String = "U+${codePoint.toString(16).uppercase().padStart(4, '0')}"
    }

    fun read(file: File): List<Glyph> = try {
        when (readTag(file)) {
            TAG_TTCF -> {
                var glyphs: List<Glyph> = emptyList()
                TrueTypeCollection(file).use { collection ->
                    collection.processAllFonts { font ->
                        if (glyphs.isEmpty()) glyphs = extract(font)
                    }
                }
                glyphs
            }
            TAG_OTTO -> OTFParser().parse(file).use(::extract)
            else -> TTFParser().parse(file).use(::extract)
        }
    } catch (_: Throwable) {
        emptyList()
    }

    @Suppress("DEPRECATION")
    private fun extract(font: TrueTypeFont): List<Glyph> {
        val cmap = runCatching { font.getUnicodeCmap(false) }.getOrNull() ?: return emptyList()
        val glyphCount = runCatching { font.numberOfGlyphs }.getOrNull() ?: return emptyList()
        val codePoints = linkedSetOf<Int>()

        for (glyphId in 0 until glyphCount) {
            val codes = runCatching { cmap.getCharCodes(glyphId) }.getOrNull().orEmpty()
            codes.forEach { codePoint ->
                if (codePoint.isDisplayableCodePoint()) codePoints += codePoint
            }
        }

        return codePoints.sorted().map(::Glyph)
    }

    private fun Int.isDisplayableCodePoint(): Boolean =
        Character.isValidCodePoint(this) &&
            Character.getType(this) != Character.CONTROL.toInt() &&
            Character.getType(this) != Character.FORMAT.toInt() &&
            Character.getType(this) != Character.SURROGATE.toInt() &&
            Character.getType(this) != Character.UNASSIGNED.toInt()

    private fun readTag(file: File): Int = try {
        RandomAccessFile(file, "r").use { it.readInt() }
    } catch (_: Exception) {
        0
    }
}
