package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.*

class Pc1360DisplaySymbolsTest {
    @Test fun kanjiModelExposesEachOfItsEightIndicatorsIndependently() {
        val flags = listOf(0x01 to DisplaySymbol.SMALL, 0x02 to DisplaySymbol.SHIFT,
            0x04 to DisplaySymbol.DEF, 0x08 to DisplaySymbol.RUN, 0x10 to DisplaySymbol.PRO,
            0x20 to DisplaySymbol.HIRAGANA, 0x40 to DisplaySymbol.KATAKANA, 0x80 to DisplaySymbol.S)
        for ((bit, symbol) in flags) {
            val session = session(bit, kanji = true)
            session.runCycles(256)
            assertEquals(listOf(symbol), session.displaySnapshot().symbols)
            session.reset()
            assertTrue(session.displaySnapshot().symbols.isEmpty())
        }
    }

    @Test fun standard1360KeepsKanaAndDoesNotAcquireKanjiOnlyIndicators() {
        val session = session(0xff, kanji = false)
        session.runCycles(256)
        assertEquals(setOf(DisplaySymbol.RUN, DisplaySymbol.PRO, DisplaySymbol.KANA,
            DisplaySymbol.SMALL, DisplaySymbol.SHIFT, DisplaySymbol.DEF), session.displaySnapshot().symbols.toSet())
    }

    private fun session(flags: Int, kanji: Boolean): EmulatorSession {
        val id = if (kanji) Pc1360RomDefinition.PC1360K_MACHINE_ID else Pc1360RomDefinition.MACHINE_ID
        // Original LIDP 303C; LIA flags; STD; JP 0006. No firmware fixture.
        val internal = ByteArray(0x2000)
        byteArrayOf(0x10, 0x30, 0x3c, 0x02, flags.toByte(), 0x52, 0x79, 0x00, 0x06).copyInto(internal)
        val rom = assertIs<RomImportResult.Success>(Pc1360ComponentRomImporter.importImages(
            internal, List(8) { ByteArray(0x4000) }, id)).romSet
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
    }
}
