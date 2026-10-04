package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245BasicDetokenizerTest {
    @Test
    fun producesReadableCanonicalBasicText() {
        val source = detokenize(
            bytes(
                0xff,
                0xe0, 0x10, 0xc1, 0x12, 0x58, 0x55, 0x5c, 0x5c, 0x5f, 0x12, 0x00,
                0xe0, 0x20, 0xd4, 0x00,
                0xff,
            ),
        )

        assertEquals("10 PRINT \"HELLO\"\n20 END", source)
    }

    @Test
    fun preservesTokenizerOutputAcrossARoundTrip() {
        val original = tokenize(
            "10 IF A>=1 THEN PRINT \"PI=\";π,√,\\EX,\\BX\n" +
                "20 REM PRINT and raw \\x7F",
        )

        val canonicalText = detokenize(original)
        val encodedAgain = tokenize(canonicalText)

        assertContentEquals(original, encodedAgain)
    }

    @Test
    fun keepsSymbolicComparisonOperatorsCompact() {
        val original = tokenize("10 IF A>=1 AND B<=2 AND C<>O THEN END")

        assertEquals("10 IF A>=1 AND B<=2 AND C<>O THEN END", detokenize(original))
    }

    @Test
    fun doesNotInterpretKeywordBytesInsideStringsOrRemarks() {
        val program = bytes(
            0xff, 0xe0, 0x10,
            0xc1, 0x12, 0xc1, 0x12,
            0xd3, 0xc1,
            0x00, 0xff,
        )

        assertEquals("10 PRINT \"\\xC1\" REM \\xC1", detokenize(program))
    }

    @Test
    fun preservesUnknownCodesAsRawByteEscapes() {
        val program = bytes(0xff, 0xe0, 0x10, 0x70, 0x00, 0xff)

        assertEquals("10 \\x70", detokenize(program))
        assertContentEquals(program, tokenize(detokenize(program)))
    }

    @Test
    fun reportsMalformedProgramOffsets() {
        assertEquals(0, failure(bytes()).offset)
        assertEquals(1, failure(bytes(0xff, 0xd0, 0x10, 0x00, 0xff)).offset)
        assertEquals(3, failure(bytes(0xff, 0xe0, 0x10, 0xff)).offset)
        assertEquals(4, failure(bytes(0xff, 0xe0, 0x10, 0x00)).offset)
        assertEquals(2, failure(bytes(0xff, 0xff, 0x01)).offset)
    }

    private fun tokenize(source: String): ByteArray {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        return assertIs<Pc1245BasicTokenizeResult.Success>(Pc1245BasicTokenizer.tokenize(document)).bytes
    }

    private fun detokenize(program: ByteArray): String =
        assertIs<Pc1245BasicDetokenizeResult.Success>(Pc1245BasicDetokenizer.detokenize(program)).source

    private fun failure(program: ByteArray): Pc1245BasicDetokenizeError =
        assertIs<Pc1245BasicDetokenizeResult.Failure>(Pc1245BasicDetokenizer.detokenize(program)).error

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
