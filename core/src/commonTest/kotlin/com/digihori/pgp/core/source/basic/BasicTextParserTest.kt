package com.digihori.pgp.core.source.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BasicTextParserTest {
    @Test
    fun parsesOrdinaryBasicAndNormalizesLineEndingsAndBom() {
        val result = BasicTextParser.parse("\uFEFF10 PRINT \"HELLO\"\r\n20 END\r")
        val document = assertIs<BasicTextParseResult.Success>(result).document

        assertEquals(
            listOf(
                *"10 PRINT \"HELLO\"".map(BasicTextElement::Character).toTypedArray(),
                BasicTextElement.LineBreak,
                *"20 END".map(BasicTextElement::Character).toTypedArray(),
                BasicTextElement.LineBreak,
            ),
            document.elements,
        )
    }

    @Test
    fun parsesLogicalEscapesUnicodeAliasesAndLiteralBackslash() {
        val result = BasicTextParser.parse("\\PI π \\SQR √ \\EX \\BX " + "\\\\")
        val document = assertIs<BasicTextParseResult.Success>(result).document

        assertEquals(
            listOf(
                BasicTextElement.Special(BasicSpecialSymbol.PI),
                BasicTextElement.Character(' '),
                BasicTextElement.Special(BasicSpecialSymbol.PI),
                BasicTextElement.Character(' '),
                BasicTextElement.Special(BasicSpecialSymbol.SQUARE_ROOT),
                BasicTextElement.Character(' '),
                BasicTextElement.Special(BasicSpecialSymbol.SQUARE_ROOT),
                BasicTextElement.Character(' '),
                BasicTextElement.Special(BasicSpecialSymbol.EXPONENT),
                BasicTextElement.Character(' '),
                BasicTextElement.Special(BasicSpecialSymbol.BLOCK),
                BasicTextElement.Character(' '),
                BasicTextElement.Character('\\'),
            ),
            document.elements,
        )
    }

    @Test
    fun parsesRawByteWithoutGivingItMachineSpecificMeaning() {
        val result = BasicTextParser.parse("PRINT \\xFC5")
        val document = assertIs<BasicTextParseResult.Success>(result).document

        assertEquals(BasicTextElement.RawByte(0xfc), document.elements[6])
        assertEquals(BasicTextElement.Character('5'), document.elements[7])
        assertEquals(BasicTextToken(BasicTextElement.RawByte(0xfc), 1, 7), document.tokens[6])
    }

    @Test
    fun reportsUnknownEscapeWithLineAndColumn() {
        val result = BasicTextParser.parse("10 PRINT\n20 \\UNKNOWN")
        val error = assertIs<BasicTextParseResult.Failure>(result).error

        assertEquals(2, error.line)
        assertEquals(4, error.column)
        assertEquals("Unknown escape: \\UNKNOWN", error.message)
    }

    @Test
    fun rejectsIncompleteAndInvalidRawByteEscapes() {
        val incomplete = assertIs<BasicTextParseResult.Failure>(BasicTextParser.parse("\\xF")).error
        assertEquals("Raw byte escape requires two hexadecimal digits", incomplete.message)

        val invalid = assertIs<BasicTextParseResult.Failure>(BasicTextParser.parse("\\xG1")).error
        assertEquals("Invalid raw byte escape: \\xG1", invalid.message)
    }
}
