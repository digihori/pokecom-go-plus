package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245RomBasicInputTest {
    @Test
    fun compilesOrdinaryAndShiftedCharactersAndTerminatesTheLastLine() {
        val result = compile("A!")
        val keys = assertIs<Pc1245RomInputResult.Success>(result).keys

        assertEquals(
            listOf(PocketKey.A, PocketKey.SHIFT, PocketKey.Q, PocketKey.ENTER),
            keys,
        )
    }

    @Test
    fun compilesPc1245KeyboardSpecialSymbols() {
        val result = compile("\\PI \\SQR \\EX\n")
        val keys = assertIs<Pc1245RomInputResult.Success>(result).keys

        assertEquals(
            listOf(
                PocketKey.SHIFT, PocketKey.NUM_0, PocketKey.SPACE,
                PocketKey.SHIFT, PocketKey.DOT, PocketKey.SPACE,
                PocketKey.SHIFT, PocketKey.PLUS, PocketKey.ENTER,
            ),
            keys,
        )
    }

    @Test
    fun doesNotAddADuplicateEnterAfterAnExistingFinalLineBreak() {
        val result = compile("10 END\n")
        val keys = assertIs<Pc1245RomInputResult.Success>(result).keys

        assertEquals(1, keys.count { it == PocketKey.ENTER })
    }

    @Test
    fun replacesOnlyAColonImmediatelyAfterTheLineNumberWithSpace() {
        val compact = assertIs<Pc1245RomInputResult.Success>(compile("10:PRINT A:B")).keys
        val separated = assertIs<Pc1245RomInputResult.Success>(compile("20 :PRINT")).keys

        assertEquals(PocketKey.SPACE, compact[2])
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.I), compact.subList(10, 12))
        assertEquals(PocketKey.SPACE, separated[2])
        assertEquals(PocketKey.SPACE, separated[3])
    }

    @Test
    fun reportsBlockSymbolAsUnavailableFromThePc1245Keyboard() {
        val failure = assertIs<Pc1245RomInputResult.Failure>(compile("10 PRINT \\BX")).error

        assertEquals(1, failure.line)
        assertEquals(10, failure.column)
        assertEquals(
            Pc1245RomInputUnsupported.SpecialSymbol(BasicSpecialSymbol.BLOCK),
            failure.unsupported,
        )
    }

    @Test
    fun reportsRawByteAndUnsupportedCharacterLocations() {
        val rawByte = assertIs<Pc1245RomInputResult.Failure>(compile("10 PRINT\n20 \\xFC")).error
        assertEquals(2, rawByte.line)
        assertEquals(4, rawByte.column)
        assertEquals(Pc1245RomInputUnsupported.RawByte(0xfc), rawByte.unsupported)

        val character = assertIs<Pc1245RomInputResult.Failure>(compile("10 _")).error
        assertEquals(1, character.line)
        assertEquals(4, character.column)
        assertEquals(Pc1245RomInputUnsupported.Character('_'), character.unsupported)
    }

    private fun compile(source: String): Pc1245RomInputResult {
        val parsed = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source))
        return Pc1245RomBasicInput.compile(parsed.document)
    }
}
