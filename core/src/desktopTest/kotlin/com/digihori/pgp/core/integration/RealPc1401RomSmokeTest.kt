package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401CharacterInput
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeResult
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RealPc1401RomSmokeTest {
    @Test
    fun locallySuppliedRomBootsAndUpdatesSingleLineDisplay() {
        val rom = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(Pc1401FlatRomImporter.importImage(rom.readBytes()))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, imported.romSet),
        ).session
        val result = session.runCycles(1_000_000L)
        val display = session.displaySnapshot()

        assertIs<ExecutionStatus.Ready>(result.status)
        assertTrue(result.executedInstructions > 0)
        assertEquals(16, display.characterColumns)
        assertEquals(1, display.characterRows)
        assertTrue(display.copyDots().any { it.toInt() != 0 }, "PC-1401 ROM did not update the LCD")
    }

    @Test
    fun romModeKeysReachProgramRunAndCalculator() {
        val rom = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(Pc1401FlatRomImporter.importImage(rom.readBytes()))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, imported.romSet)).session
        session.runCycles(1_000_000)
        assertEquals(OperatingMode.CALCULATOR, session.currentOperatingMode())
        for (mode in listOf(OperatingMode.RUN, OperatingMode.PROGRAM, OperatingMode.RUN, OperatingMode.CALCULATOR)) {
            session.setOperatingMode(mode)
            assertIs<ExecutionStatus.Ready>(session.runCycles(1_000_000).status)
            assertEquals(mode, session.currentOperatingMode())
        }
    }

    @Test
    fun directLoadRunsInsideRomAndRoundTrips() {
        val rom = findLocalRom() ?: return
        val imported = assertIs<RomImportResult.Success>(Pc1401FlatRomImporter.importImage(rom.readBytes()))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, imported.romSet)).session
        session.runCycles(1_000_000)
        session.setOperatingMode(OperatingMode.PROGRAM); session.runCycles(1_000_000)
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse("10 A=FAC(5):DIM B(5):B(1)=3\n20 BEEP 1:POKE 16384,A+B(1)\n30 END")).document
        val program = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1401BasicTokenizer.tokenize(document)).bytes
        assertIs<BasicProgramLoadResult.Success>(session.loadBasicProgram(program))
        assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot())
        session.setOperatingMode(OperatingMode.RUN); session.runCycles(1_000_000)
        for (key in "RUN".flatMap { Pc1401CharacterInput.keySequence(it)!! } + PocketKey.ENTER) {
            session.pressKey(key); session.runCycles(60_000)
            session.releaseKey(key); session.runCycles(30_000)
        }
        assertIs<ExecutionStatus.Ready>(session.runCycles(1_000_000).status)
        assertEquals(123, session.memorySnapshot(0x4000, 1).copyBytes()[0].toInt() and 255)
        assertTrue(DisplaySymbol.ERROR !in session.displaySnapshot().symbols)
        assertTrue(session.drainAudioSamples().copySamples().any { it.toInt() != 0 }, "BEEP produced no PCM")
    }

    private fun findLocalRom(): File? =
        generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1401/pc1401mem.bin") }
            .firstOrNull(File::isFile)
}
