package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputUnsupported
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1251RomBasicInputTest {
    @Test
    fun typesParenthesesThroughThePc1251ArrowLegends() {
        val result = assertIs<Pc1245RomInputResult.Success>(compile("10 PRINT (A)"))

        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.DOWN),
            result.keys.subList(9, 11),
        )
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.UP),
            result.keys.subList(12, 14),
        )
    }

    @Test
    fun typesVerifiedPc1251SpecialSymbolLegends() {
        val result = assertIs<Pc1245RomInputResult.Success>(compile("\\PI \\SQR \\EX"))

        assertEquals(
            listOf(
                PocketKey.SHIFT, PocketKey.NUM_0, PocketKey.SPACE,
                PocketKey.SHIFT, PocketKey.DOT, PocketKey.SPACE,
                PocketKey.SHIFT, PocketKey.PLUS, PocketKey.ENTER,
            ),
            result.keys,
        )
    }

    @Test
    fun rejectsTheBlockSymbolWhichHasNoPc1251KeyLegend() {
        val failure = assertIs<Pc1245RomInputResult.Failure>(compile("10 PRINT \\BX")).error

        assertEquals(
            Pc1245RomInputUnsupported.SpecialSymbol(BasicSpecialSymbol.BLOCK),
            failure.unsupported,
        )
    }

    private fun compile(source: String): Pc1245RomInputResult {
        val parsed = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source))
        return Pc1251RomBasicInput.compile(parsed.document)
    }
}
