package org.mecresearch.fontbook.data

import java.io.File
import java.io.RandomAccessFile

/**
 * Reads `unitsPerEm` from a font's `head` table so glyph metrics can be reported
 * in the font's own design units. Shares the same sfnt/`ttcf` traversal style as
 * [OpenTypeNames].
 */
object FontHeadReader {

    private const val TAG_TTCF = 0x74746366 // 'ttcf'

    fun unitsPerEm(file: File): Int? = try {
        RandomAccessFile(file, "r").use { raf ->
            val first = raf.readInt()
            val sfntOffset: Long = if (first == TAG_TTCF) {
                raf.seek(12)
                raf.readInt().toLong() and 0xFFFFFFFFL
            } else {
                0L
            }
            val headOffset = findTable(raf, sfntOffset, "head") ?: return null
            raf.seek(headOffset + 18) // unitsPerEm sits at offset 18 within `head`
            raf.readUnsignedShort().takeIf { it in 16..16384 }
        }
    } catch (_: Exception) {
        null
    }

    private fun findTable(raf: RandomAccessFile, sfntOffset: Long, tag: String): Long? {
        raf.seek(sfntOffset)
        raf.readInt() // sfntVersion
        val numTables = raf.readUnsignedShort()
        raf.skipBytes(6)
        val t = ByteArray(4)
        repeat(numTables) {
            raf.readFully(t)
            raf.readInt() // checksum
            val offset = raf.readInt().toLong() and 0xFFFFFFFFL
            raf.readInt() // length
            if (String(t, Charsets.US_ASCII) == tag) return offset
        }
        return null
    }
}
