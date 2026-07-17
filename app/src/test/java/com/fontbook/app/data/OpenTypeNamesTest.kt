package com.fontbook.app.data

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpenTypeNamesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun prefersTypographicFamilyAndSubfamilyNames() {
        val file = temporaryFolder.newFile("font.ttf")
        file.writeBytes(
            minimalFont(
                NameRecord(1, "Legacy Family"),
                NameRecord(2, "Regular"),
                NameRecord(4, "Legacy Family Regular"),
                NameRecord(16, "Typographic Family"),
                NameRecord(17, "Book"),
            ),
        )

        val names = OpenTypeNames.read(file)

        assertEquals("Typographic Family", names?.family)
        assertEquals("Book", names?.subfamily)
        assertEquals("Legacy Family Regular", names?.full)
    }

    @Test
    fun fallsBackToLegacyFamilyNames() {
        val file = temporaryFolder.newFile("font.ttf")
        file.writeBytes(
            minimalFont(
                NameRecord(1, "Legacy Family"),
                NameRecord(2, "Italic"),
                NameRecord(4, "Legacy Family Italic"),
            ),
        )

        val names = OpenTypeNames.read(file)

        assertEquals("Legacy Family", names?.family)
        assertEquals("Italic", names?.subfamily)
        assertEquals("Legacy Family Italic", names?.full)
    }

    private data class NameRecord(val id: Int, val value: String)

    private fun minimalFont(vararg records: NameRecord): ByteArray {
        val encodedStrings = records.map { it.value.toByteArray(Charsets.UTF_16BE) }
        val stringStorageOffset = 6 + records.size * 12
        val nameTableLength = stringStorageOffset + encodedStrings.sumOf { it.size }
        val nameTableOffset = 28
        val buffer = ByteBuffer.allocate(nameTableOffset + nameTableLength).order(ByteOrder.BIG_ENDIAN)

        buffer.putInt(0x00010000)
        buffer.putShort(1)
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.putShort(0)
        buffer.put("name".toByteArray(Charsets.US_ASCII))
        buffer.putInt(0)
        buffer.putInt(nameTableOffset)
        buffer.putInt(nameTableLength)

        buffer.putShort(0)
        buffer.putShort(records.size.toShort())
        buffer.putShort(stringStorageOffset.toShort())
        var stringOffset = 0
        records.zip(encodedStrings).forEach { (record, encoded) ->
            buffer.putShort(3)
            buffer.putShort(1)
            buffer.putShort(0x0409)
            buffer.putShort(record.id.toShort())
            buffer.putShort(encoded.size.toShort())
            buffer.putShort(stringOffset.toShort())
            stringOffset += encoded.size
        }
        encodedStrings.forEach(buffer::put)
        return buffer.array()
    }

    private fun ByteBuffer.putShort(value: Int) {
        putShort(value.toShort())
    }
}
