package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245BasicTokenizerTest {
    @Test
    fun tokenizesACompleteProgramWithOldLineNumbers() {
        val bytes = tokenize("10 PRINT \"HELLO\"\n20 END")

        assertContentEquals(
            bytes(
                0xff,
                0xe0, 0x10, 0xc1, 0x12, 0x58, 0x55, 0x5c, 0x5c, 0x5f, 0x12, 0x00,
                0xe0, 0x20, 0xd4, 0x00,
                0xff,
            ),
            bytes,
        )
    }

    @Test
    fun keepsKeywordsLiteralInsideStringsAndRemarks() {
        val bytes = tokenize("10 PRINT \"GOTO\":REM PRINT GOTO")

        assertEquals(1, bytes.count { it == 0xc1.toByte() })
        assertEquals(0, bytes.count { it == 0xd7.toByte() })
        assertEquals(1, bytes.count { it == 0xd3.toByte() })
    }

    @Test
    fun encodesSpecialSymbolsRawBytesAndComparisonOperators() {
        val bytes = tokenize("10 IF A>=1 THEN PRINT \\PI,\\SQR,\\EX,\\BX,\\x7F")

        listOf(0xd0, 0x82, 0x92, 0xc1, 0x19, 0x1a, 0x4b, 0x4c, 0x7f).forEach { code ->
            assertEquals(true, code.toByte() in bytes, "code=${code.toString(16)}")
        }
    }

    @Test
    fun ignoresTheOptionalColonAfterTheLineNumber() {
        assertContentEquals(tokenize("10 PRINT A"), tokenize("10: PRINT A"))
    }

    @Test
    fun acceptsLinesLongerThanTheRomInputBuffer() {
        val source = "10 PRINT \"${"A".repeat(180)}\""
        val bytes = tokenize(source)

        assertEquals(188, bytes.size)
        assertEquals(180, bytes.count { it == 0x51.toByte() })
    }

    @Test
    fun reportsLineNumberAndCharacterErrorsAtTheirSourceLocations() {
        val lineNumber = failure("1000 PRINT A")
        assertEquals(1, lineNumber.line)
        assertEquals(1, lineNumber.column)

        val character = failure("10 PRINT _")
        assertEquals(1, character.line)
        assertEquals(10, character.column)
    }

    private fun tokenize(source: String): ByteArray {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        return assertIs<Pc1245BasicTokenizeResult.Success>(Pc1245BasicTokenizer.tokenize(document)).bytes
    }

    private fun failure(source: String): Pc1245BasicTokenizeError {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        return assertIs<Pc1245BasicTokenizeResult.Failure>(Pc1245BasicTokenizer.tokenize(document)).error
    }

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
