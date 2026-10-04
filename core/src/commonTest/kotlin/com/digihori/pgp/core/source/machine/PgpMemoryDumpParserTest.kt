package com.digihori.pgp.core.source.machine

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PgpMemoryDumpParserTest {
    @Test
    fun acceptsSupportedSeparatorsSpacingAndVariableLengths() {
        val result = success(
            """
            ; initialization
            9000 00 01 02 03
            9100:1112
            A000 : FF EEFF
            """.trimIndent(),
        )

        assertEquals(3, result.segments.size)
        assertEquals(9, result.byteCount)
        assertSegment(result.segments[0], 0x9000, byteArrayOf(0x00, 0x01, 0x02, 0x03))
        assertSegment(result.segments[1], 0x9100, byteArrayOf(0x11, 0x12))
        assertSegment(result.segments[2], 0xa000, byteArrayOf(0xff.toByte(), 0xee.toByte(), 0xff.toByte()))
    }

    @Test
    fun acceptsBomCrLfBlankLinesAndInlineComments() {
        val result = success("\uFEFF9000:0001 ; first\r\n\r\n9100 02 # second\r\n")

        assertEquals(listOf(0x9000, 0x9100), result.segments.map { it.startAddress })
    }

    @Test
    fun acceptsAndIgnoresOptionalLineChecksums() {
        val result = success("9000 00010203:7A\n9100:1112:ff")

        assertSegment(result.segments[0], 0x9000, byteArrayOf(0, 1, 2, 3))
        assertSegment(result.segments[1], 0x9100, byteArrayOf(0x11, 0x12))
    }

    @Test
    fun rejectsMalformedOptionalChecksums() {
        val error = assertIs<PgpMemoryDumpError.Syntax>(failure("9000 0001:XX"))

        assertEquals("Optional checksum must contain exactly two hexadecimal digits", error.message)
    }

    @Test
    fun sortsNonOverlappingSegmentsByAddress() {
        val result = success("A000 FF\n9000 00")

        assertEquals(listOf(0x9000, 0xa000), result.segments.map { it.startAddress })
    }

    @Test
    fun rejectsOverlappingRangesEvenWhenBytesDiffer() {
        val error = failure("9000 00010203\n9002 1011")

        assertEquals(PgpMemoryDumpError.Overlap(line = 2, previousLine = 1, address = 0x9002), error)
    }

    @Test
    fun rejectsAddressSpaceOverflow() {
        val error = failure("FFFF 0011")

        assertEquals(PgpMemoryDumpError.AddressOverflow(line = 1, startAddress = 0xffff, byteCount = 2), error)
    }

    @Test
    fun rejectsIncompleteByte() {
        val error = assertIs<PgpMemoryDumpError.Syntax>(failure("9000 001"))

        assertEquals(1, error.line)
        assertEquals("Data must contain complete two-digit bytes", error.message)
    }

    @Test
    fun rejectsEmptyDocuments() {
        assertIs<PgpMemoryDumpError.Empty>(failure("; no data\n# yet"))
    }

    private fun success(source: String): AddressedMemoryImage =
        assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(source)).image

    private fun failure(source: String): PgpMemoryDumpError =
        assertIs<PgpMemoryDumpParseResult.Failure>(PgpMemoryDumpParser.parse(source)).error

    private fun assertSegment(segment: AddressedMemorySegment, address: Int, bytes: ByteArray) {
        assertEquals(address, segment.startAddress)
        assertContentEquals(bytes, segment.copyBytes())
    }
}
