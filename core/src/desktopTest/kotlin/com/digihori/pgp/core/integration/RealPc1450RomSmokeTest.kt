package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc14xx.*
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401CharacterInput
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

/** Optional local firmware checks; all assertions are behavioral, no firmware bytes are fixtures. */
class RealPc1450RomSmokeTest {
    @Test fun localRomBootModesAndBasicExecution() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1450") }
            .firstOrNull { File(it, "pc1450-int.bin").isFile && File(it, "pc1450-ext.bin").isFile } ?: return
        val model = Pc14xxModel.PC_1450
        val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model,
            File(directory, "pc1450-int.bin").readBytes(), File(directory, "pc1450-ext.bin").readBytes())).romSet
        val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
        assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        assertTrue(s.displaySnapshot().copyDots().any { it.toInt() != 0 })
        assertEquals(OperatingMode.CALCULATOR, s.currentOperatingMode())
        for (mode in listOf(OperatingMode.RUN, OperatingMode.PROGRAM, OperatingMode.RUN, OperatingMode.CALCULATOR)) {
            s.setOperatingMode(mode); s.runCycles(1_000_000); assertEquals(mode, s.currentOperatingMode())
        }
        s.setOperatingMode(OperatingMode.PROGRAM); s.runCycles(1_000_000)
        val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
            "10 A=FACT(5):POKE 20480,A\n20 END".encodeToByteArray(), model.machineId)).bytes
        assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(program))
        s.setOperatingMode(OperatingMode.RUN); s.runCycles(1_000_000)
        for (key in "RUN".flatMap { Pc1401CharacterInput.keySequence(it)!! } + PocketKey.ENTER) {
            s.pressKey(key); s.runCycles(80_000); s.releaseKey(key); s.runCycles(40_000)
        }
        assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        assertEquals(120, s.memorySnapshot(0x5000, 1).copyBytes()[0].toInt() and 255)
        assertTrue(DisplaySymbol.ERROR !in s.displaySnapshot().symbols)
    }
}
