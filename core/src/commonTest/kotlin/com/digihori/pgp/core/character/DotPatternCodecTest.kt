package com.digihori.pgp.core.character

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DotPatternCodecTest {
    private val format = CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP

    @Test
    fun encodesFiveBySevenColumnsWithTopDotInLeastSignificantBit() {
        var pattern = DotPattern(5, 7)
        pattern = pattern.withDot(0, 0, true).withDot(0, 6, true).withDot(4, 1, true)

        assertContentEquals(byteArrayOf(0x41, 0, 0, 0, 0x02), DotPatternCodec.encode(pattern, format))
    }

    @Test
    fun roundTripsPatternsAndSupportsClearAndInvert() {
        val pattern = DotPattern(5, 7).withDot(2, 3, true).withDot(4, 6, true)
        assertEquals(pattern, DotPatternCodec.decode(DotPatternCodec.encode(pattern, format), format))
        assertFalse(pattern.clear().copyDots().any { it })
        assertTrue(pattern.clear().inverted().copyDots().all { it })
    }

    @Test
    fun supportsRowPackingAndMostSignificantFirstDefinitions() {
        val rowFormat = DotPatternFormat("test", "Test", 9, 2, DotPackingAxis.ROWS, DotBitOrder.MOST_SIGNIFICANT_FIRST)
        val pattern = DotPattern(9, 2).withDot(0, 0, true).withDot(8, 0, true).withDot(7, 1, true)

        val encoded = DotPatternCodec.encode(pattern, rowFormat)

        assertContentEquals(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01, 0), encoded)
        assertEquals(pattern, DotPatternCodec.decode(encoded, rowFormat))
    }

    @Test
    fun parsesAndFormatsPasteableByteText() {
        val parsed = assertIs<DotByteTextResult.Success>(DotPatternTextCodec.parse("0x0E, &11 31, 17, 17", 5))
        assertContentEquals(byteArrayOf(14, 17, 31, 17, 17), parsed.bytes)
        assertEquals("0x0E, 0x11, 0x1F, 0x11, 0x11", DotPatternTextCodec.hexBytes(parsed.bytes))
        assertEquals("DATA 14, 17, 31, 17, 17", DotPatternTextCodec.basicData(parsed.bytes))
        assertEquals("DB 0x0E, 0x11, 0x1F, 0x11, 0x11", DotPatternTextCodec.assemblerDb(parsed.bytes))
    }

    @Test
    fun reportsWrongCountsAndInvalidValues() {
        assertIs<DotByteTextResult.Failure>(DotPatternTextCodec.parse("1, 2", 5))
        assertIs<DotByteTextResult.Failure>(DotPatternTextCodec.parse("1, 2, 3, 4, 256", 5))
        assertFailsWith<IllegalArgumentException> {
            DotPatternCodec.decode(byteArrayOf(0x80.toByte(), 0, 0, 0, 0), format)
        }
    }
}
