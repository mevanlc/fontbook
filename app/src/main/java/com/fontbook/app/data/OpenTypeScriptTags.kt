package com.fontbook.app.data

import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/**
 * Reads OpenType Layout script tags from GSUB/GPOS tables.
 *
 * FontBox exposes some of these internals, but reading the compact ScriptList
 * records directly keeps scanning cheap and avoids instantiating glyph data.
 */
object OpenTypeScriptTags {

    private const val TAG_TTCF = 0x74746366 // 'ttcf'
    private val LAYOUT_TABLES = setOf("GSUB", "GPOS")
    private val LEGACY_SUPPORT_TAGS = setOf("latn", "grek", "cyrl")

    fun read(file: File): List<String> = try {
        RandomAccessFile(file, "r").use { raf ->
            val first = raf.readInt()
            val sfntOffset: Long = if (first == TAG_TTCF) {
                raf.seek(12)
                raf.readInt().toLong() and 0xFFFFFFFFL
            } else {
                0L
            }
            readFromSfnt(raf, sfntOffset)
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun readFromSfnt(raf: RandomAccessFile, sfntOffset: Long): List<String> {
        raf.seek(sfntOffset)
        raf.readInt() // sfntVersion
        val numTables = raf.readUnsignedShort()
        raf.skipBytes(6)

        val tableRanges = HashMap<String, Pair<Long, Int>>()
        val tag = ByteArray(4)
        repeat(numTables) {
            raf.readFully(tag)
            raf.readInt() // checksum
            val offset = raf.readInt().toLong() and 0xFFFFFFFFL
            val length = raf.readInt().toLong() and 0xFFFFFFFFL
            val name = String(tag, Charsets.US_ASCII)
            if (name in LAYOUT_TABLES && length <= Int.MAX_VALUE) {
                tableRanges[name] = offset to length.toInt()
            }
        }

        val tags = linkedSetOf<String>()
        for ((offset, length) in tableRanges.values) {
            if (length < 6) continue
            val data = ByteArray(length)
            raf.seek(offset)
            raf.readFully(data)
            tags += parseLayoutTable(data)
        }
        return orderTags(tags
            .filterNot { it.equals("dflt", ignoreCase = true) }
            .sorted())
    }

    private fun orderTags(tags: List<String>): List<String> {
        val hasLegacyCluster = LEGACY_SUPPORT_TAGS.all { it in tags }
        if (!hasLegacyCluster) return tags

        val purposeTags = tags.filterNot { it in LEGACY_SUPPORT_TAGS }
        if (purposeTags.isEmpty()) return tags

        return purposeTags + tags.filter { it in LEGACY_SUPPORT_TAGS }
    }

    private fun parseLayoutTable(data: ByteArray): List<String> {
        if (data.size < 6) return emptyList()
        val scriptListOffset = u16(data, 4)
        if (scriptListOffset <= 0 || scriptListOffset + 2 > data.size) return emptyList()

        val count = u16(data, scriptListOffset)
        var p = scriptListOffset + 2
        val tags = ArrayList<String>(count)
        repeat(count) {
            if (p + 6 > data.size) return@repeat
            val tag = String(data, p, 4, Charsets.US_ASCII)
                .lowercase(Locale.ROOT)
            tags += tag
            p += 6
        }
        return tags
    }

    private fun u16(data: ByteArray, i: Int): Int =
        ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
}
