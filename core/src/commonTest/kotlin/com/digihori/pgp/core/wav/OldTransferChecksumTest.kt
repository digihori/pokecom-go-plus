package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OldTransferChecksumTest {
    @Test
    fun swapsNibbles() {
        assertEquals(0x00, OldTransferChecksum.nibbleSwap(0x00))
        assertEquals(0xba, OldTransferChecksum.nibbleSwap(0xab))
        assertEquals(0x0f, OldTransferChecksum.nibbleSwap(0xf0))
        assertFailsWith<IllegalArgumentException> { OldTransferChecksum.nibbleSwap(-1) }
        assertFailsWith<IllegalArgumentException> { OldTransferChecksum.nibbleSwap(0x100) }
    }

    @Test
    fun calculatesChecksumInLogicalHighThenLowNibbleOrder() {
        val nameBlock = bytes(0x00, 0x45, 0x44, 0x42, 0x41, 0x53, 0x60, 0xf5)

        assertEquals(0x3e, OldTransferChecksum.calculate(LogicalByteSequence(nameBlock)))
        assertEquals(0xe3, OldTransferChecksum.nibbleSwap(0x3e))
    }

    @Test
    fun appliesEndAroundCarryAfterTheHighNibble() {
        val accumulator = OldChecksumAccumulator()
        repeat(8) { accumulator.addLogicalByte(0xff) }
        assertEquals(0xf0, accumulator.value)

        accumulator.addLogicalByte(0x0f)
        assertEquals(0xff, accumulator.value)
        accumulator.addLogicalByte(0x10)
        assertEquals(0x01, accumulator.value)
    }

    @Test
    fun encodesReferenceBodyVectorWithCumulativeChunkChecksums() {
        val logical = ByteArray(20) { it.toByte() }

        val raw = OldTransferChecksum.encodeBody(LogicalByteSequence(logical))

        assertContentEquals(
            bytes(
                0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0xc1,
                0x80, 0x90, 0xa0, 0xb0, 0xc0, 0xd0, 0xe0, 0xf0, 0x87,
                0x01, 0x11, 0x21, 0x31,
            ),
            raw.copyBytes(),
        )
    }

    @Test
    fun resetsCumulativeChecksumAfterEightyDataBytes() {
        val logical = ByteArray(96) { 0xff.toByte() }
        val raw = OldTransferChecksum.encodeBody(LogicalByteSequence(logical)).copyBytes()
        val checksumValues = List(12) { chunk -> raw[chunk * 9 + 8].toInt() and 0xff }

        assertEquals(
            listOf(0x0f, 0x0e, 0x1d, 0x1c, 0x2b, 0x2a, 0x39, 0x38, 0x47, 0x46, 0x0f, 0x0e),
            checksumValues,
        )
    }

    @Test
    fun addsChecksumsOnlyToCompleteChunks() {
        val expectedRawSizes = mapOf(
            0 to 0,
            1 to 1,
            7 to 7,
            8 to 9,
            9 to 10,
            79 to 88,
            80 to 90,
            81 to 91,
            88 to 99,
        )

        expectedRawSizes.forEach { (logicalSize, rawSize) ->
            val encoded = OldTransferChecksum.encodeBody(
                LogicalByteSequence(ByteArray(logicalSize) { it.toByte() }),
            )
            assertEquals(rawSize, encoded.size, "logical size $logicalSize")
        }
    }

    @Test
    fun accumulatorCanBeResetAndRejectsValuesOutsideOneByte() {
        val accumulator = OldChecksumAccumulator()
        accumulator.addLogicalByte(0xab)
        assertEquals(0x15, accumulator.value)
        accumulator.reset()
        assertEquals(0, accumulator.value)
        assertFailsWith<IllegalArgumentException> { accumulator.addLogicalByte(-1) }
        assertFailsWith<IllegalArgumentException> { accumulator.addLogicalByte(0x100) }
    }

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
