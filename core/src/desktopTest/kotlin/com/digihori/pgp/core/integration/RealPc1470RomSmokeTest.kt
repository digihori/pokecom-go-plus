package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc14xx.*
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401CharacterInput
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

/** Optional local firmware checks; all assertions are behavioral, no firmware bytes are fixtures. */
class RealPc1470RomSmokeTest {
    @Test fun localRomArrowAndShiftEqualsStatUseRealScanInputs() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1470U") }
            .firstOrNull { File(it, "pc1470mem.bin").isFile && File(it, "pc1470bank.bin").isFile } ?: return
        val rom = assertIs<RomImportResult.Success>(Pc14xxFlatRomImporter.importImage(Pc14xxModel.PC_1470U,
            File(directory, "pc1470mem.bin").readBytes(), File(directory, "pc1470bank.bin").readBytes())).romSet
        fun tap(s: EmulatorSession, key: PocketKey) {
            assertIs<InputResult.Accepted>(s.pressKey(key)); s.runCycles(80_000)
            assertIs<InputResult.Accepted>(s.releaseKey(key)); s.runCycles(200_000)
        }
        fun session(mode: OperatingMode): EmulatorSession {
            val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc14xxModel.PC_1470U.machineId, rom)).session
            s.runCycles(1_000_000); tap(s, PocketKey.Y); tap(s, PocketKey.ENTER); s.runCycles(1_000_000)
            s.setOperatingMode(mode); s.runCycles(1_000_000)
            return s
        }
        fun type(s: EmulatorSession, text: String) {
            for (key in text.flatMap { checkNotNull(Pc1401CharacterInput.keySequence(it)) }) tap(s, key)
            tap(s, PocketKey.ENTER)
        }
        for (shift in listOf(false, true)) {
            val s = session(OperatingMode.RUN)
            type(s, "A=123")
            val before = s.displaySnapshot().copyDots()
            if (shift) tap(s, PocketKey.SHIFT)
            tap(s, PocketKey.TITLE) // Physical ↕ key; SHIFT selects its TITLE legend.
            val after = s.displaySnapshot()
            assertFalse(before.contentEquals(after.copyDots()))
            assertFalse(DisplaySymbol.SHIFT in after.symbols)
            assertTrue((0..6).any { row -> (0 until after.dotColumns).any { after.isDotOn(it, row) } })
            assertFalse((7..13).any { row -> (0 until after.dotColumns).any { after.isDotOn(it, row) } })
        }
        val calculator = session(OperatingMode.CALCULATOR)
        type(calculator, "123+456")
        val before = calculator.displaySnapshot().copyDots()
        tap(calculator, PocketKey.TITLE)
        assertFalse(before.contentEquals(calculator.displaySnapshot().copyDots()))
        val control = session(OperatingMode.CALCULATOR)
        type(control, "456")
        assertContentEquals(control.displaySnapshot().copyDots(), calculator.displaySnapshot().copyDots())
        tap(calculator, PocketKey.SHIFT); tap(calculator, PocketKey.EQUALS)
        assertTrue(DisplaySymbol.STAT in calculator.displaySnapshot().symbols)
        tap(calculator, PocketKey.SHIFT); tap(calculator, PocketKey.EQUALS)
        assertFalse(DisplaySymbol.STAT in calculator.displaySnapshot().symbols)
    }

    @Test fun localRomBootModesAndBasicExecution() {
        val directory = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "local-data/roms/pc-1470U") }
            .firstOrNull { File(it, "pc1470mem.bin").isFile && File(it, "pc1470bank.bin").isFile } ?: return
        val model = Pc14xxModel.PC_1470U
        val rom = assertIs<RomImportResult.Success>(Pc14xxFlatRomImporter.importImage(model,
            File(directory, "pc1470mem.bin").readBytes(), File(directory, "pc1470bank.bin").readBytes())).romSet
        val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
        assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        assertTrue(s.displaySnapshot().copyDots().any { it.toInt() != 0 })
        // Cold RAM boot asks MEMORY ALL CLEAR O.K.?; acknowledge through real keys.
        for (key in listOf(PocketKey.Y, PocketKey.ENTER)) {
            s.pressKey(key); s.runCycles(80_000); s.releaseKey(key); s.runCycles(1_000_000)
        }
        for (mode in listOf(OperatingMode.RUN, OperatingMode.PROGRAM, OperatingMode.RUN, OperatingMode.CALCULATOR)) {
            s.setOperatingMode(mode); s.runCycles(1_000_000); assertEquals(mode, s.currentOperatingMode())
        }
        fun tap(key: PocketKey) {
            assertIs<InputResult.Accepted>(s.pressKey(key)); s.runCycles(80_000)
            s.releaseKey(key); s.runCycles(200_000)
        }
        // Validate the previously omitted physical scan bits against local firmware.
        tap(PocketKey.NUM_1); tap(PocketKey.DIVIDE); tap(PocketKey.NUM_3); tap(PocketKey.EQUALS)
        val beforeFse = s.displaySnapshot().copyDots()
        tap(PocketKey.FORMAT)
        assertFalse(beforeFse.contentEquals(s.displaySnapshot().copyDots()), "FSE must change numeric display format")
        tap(PocketKey.CLEAR)
        s.setOperatingMode(OperatingMode.PROGRAM); s.runCycles(1_000_000)
        val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
            "10 A=FACT(5):POKE 49152,A\n20 END".encodeToByteArray(), model.machineId)).bytes
        assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(program))
        s.setOperatingMode(OperatingMode.RUN); s.runCycles(1_000_000)
        for (key in "RUN".flatMap { Pc1401CharacterInput.keySequence(it)!! } + PocketKey.ENTER) {
            s.pressKey(key); s.runCycles(80_000); s.releaseKey(key); s.runCycles(40_000)
        }
        assertIs<ExecutionStatus.Ready>(s.runCycles(1_000_000).status)
        assertEquals(120, s.memorySnapshot(0xc000, 1).copyBytes()[0].toInt() and 255)
        assertTrue(DisplaySymbol.ERROR !in s.displaySnapshot().symbols)
    }
}
