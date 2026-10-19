package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import java.io.File
import kotlin.test.*

class RealPc1360kRomSmokeTest {
    @Test
    fun localFirmwareBootsWithoutKanjiRom() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1360k") }
            .firstOrNull { File(it, "pc1360kmem.bin").isFile && File(it, "pc1360kbank.bin").isFile } ?: return
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        val imported = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(), id,
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, imported.romSet)).session
        val result = session.runCycles(1_000_000L)
        assertIs<ExecutionStatus.Ready>(result.status)
        assertEquals(id, session.machineId)
        assertTrue(result.executedInstructions > 0)
        assertTrue(session.displaySnapshot().copyDots().any { it.toInt() != 0 })
        assertTrue(session.drainBankSwitchEvents().isNotEmpty())
    }
    @Test fun localFirmwareSwitchesHiraganaKatakanaAndTheSIndicator() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1360k") }
            .firstOrNull { File(it, "pc1360kmem.bin").isFile && File(it, "pc1360kbank.bin").isFile } ?: return
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(),
            Pc1360RomDefinition.PC1360K_MACHINE_ID)).romSet
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(rom.machineId, rom)).session
        fun run(cycles: Long) { session.runCycles(cycles); session.drainAudioSamples() }
        fun key(key: PocketKey) { session.pressKey(key); run(80_000); session.releaseKey(key); run(100_000) }
        run(1_000_000); key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000)
        assertEquals(setOf(DisplaySymbol.RUN), session.displaySnapshot().symbols.toSet())
        for (input in listOf(PocketKey.W, PocketKey.I, PocketKey.D, PocketKey.T, PocketKey.H,
            PocketKey.COMMA, PocketKey.NUM_2, PocketKey.ENTER)) key(input)
        run(500_000)
        key(PocketKey.KANJI); run(500_000)
        assertEquals(0xa8, session.memorySnapshot(0x303c, 1).copyBytes().single().toInt() and 255)
        assertEquals(setOf(DisplaySymbol.RUN, DisplaySymbol.HIRAGANA, DisplaySymbol.S), session.displaySnapshot().symbols.toSet())
        key(PocketKey.HIRA_KATA); run(500_000)
        assertEquals(0xc8, session.memorySnapshot(0x303c, 1).copyBytes().single().toInt() and 255)
        assertEquals(setOf(DisplaySymbol.RUN, DisplaySymbol.KATAKANA, DisplaySymbol.S), session.displaySnapshot().symbols.toSet())
    }

}
