package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.*
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

class Pc1401BasicNavigationTest {
    private fun find(path: String) = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, path) }.firstOrNull(File::isFile)
    private fun session(model: String = "1401"): EmulatorSession? {
        val rom = find("local-data/roms/pc-1401/pc${model}mem.bin") ?: return null
        val imported = assertIs<RomImportResult.Success>(Pc1401FlatRomImporter.importImage(rom.readBytes(), if (model == "1401") Pc1401FamilyModel.PC_1401 else Pc1401FamilyModel.PC_1402))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(imported.romSet.machineId, imported.romSet)).session.also {
            it.runCycles(1_000_000)
            it.setOperatingMode(OperatingMode.RUN); it.runCycles(1_000_000)
        }
    }
    private fun EmulatorSession.tap(key: PocketKey) {
        pressKey(key); runCycles(38_400); releaseKey(key); runCycles(57_600)
    }
    private fun EmulatorSession.type(text: String) {
        for (c in text) if (c == '\n') tap(PocketKey.ENTER)
        else requireNotNull(Pc1401CharacterInput.keySequence(c)).forEach { tap(it) }
    }
    @Test fun remSeparatorMatchesRomInput() {
        val s = session() ?: return
        s.setOperatingMode(OperatingMode.PROGRAM); s.runCycles(1_000_000)
        for (source in listOf("10 REM XXX\n20 END\n", "10 REM  XXX\n20 END\n", "10 REM PRINT \"A:B\"  GOTO 900 <= <> >=\n20 END\n")) {
            s.type("NEW\n"); s.type(source)
            val native = assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot()).copyBytes()
            val compiled = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(source.encodeToByteArray(), s.machineId)).bytes
            assertContentEquals(native, compiled)
            var bytes = native
            repeat(3) {
                val text = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, s.machineId)).utf8Bytes
                assertEquals(source, text.decodeToString(), "REM whitespace must not grow")
                bytes = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(text, s.machineId)).bytes
                assertContentEquals(native, bytes)
            }
        }
    }

    @Test fun replacementAfterRunClearsCursorContext() {
        val root = requireNotNull(find("test-data/basic/io/pc1401/short.bas")).parentFile
        val full = File(root, "full.bas").readText()
        val short = File(root, "short.bas").readText()
        for (model in listOf("1401", "1402")) for (combined in listOf(false, true)) {
            val reference = session(model) ?: continue
            reference.setOperatingMode(OperatingMode.PROGRAM); reference.runCycles(1_000_000)
            reference.type("NEW\n"); reference.type(short); reference.tap(PocketKey.CLEAR)
            reference.tap(PocketKey.UP)
            val line20 = reference.displaySnapshot().copyDots()
            reference.tap(PocketKey.UP)
            val line10 = reference.displaySnapshot().copyDots()

            val s = requireNotNull(session(model))
            fun load(source: String) {
                val bytes = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(source.encodeToByteArray(), s.machineId)).bytes
                if (combined) assertIs<ProgramImageLoadResult.Success>(s.loadProgramImage(bytes, null))
                else assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(bytes))
            }
            load(full)
            s.type("RUN\n"); s.runCycles(2_000_000); s.tap(PocketKey.CLEAR)
            assertEquals(0x10, s.cpuSnapshot().copyInternalRam()[0x35].toInt() and 0x10, "RUN context before replacement")
            val flags = s.cpuSnapshot().copyInternalRam()[0x35]
            if (combined) {
                assertIs<ProgramImageLoadResult.BasicFailure>(s.loadProgramImage(byteArrayOf(0), null))
                assertIs<ProgramImageLoadResult.Success>(s.loadProgramImage(null, null))
            } else {
                assertIs<BasicProgramLoadResult.Failure>(s.loadBasicProgram(byteArrayOf(0)))
            }
            assertEquals(flags, s.cpuSnapshot().copyInternalRam()[0x35], "failed/non-BASIC load preserves flags")
            load(short)
            assertEquals(flags.toInt() and 0xef, s.cpuSnapshot().copyInternalRam()[0x35].toInt() and 255)
            s.setOperatingMode(OperatingMode.PROGRAM); s.runCycles(1_000_000)
            val image = assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot())
            for ((index, key) in listOf(PocketKey.UP, PocketKey.UP, PocketKey.UP, PocketKey.CLEAR, PocketKey.UP).withIndex()) {
                s.setMemoryAccessTracing(true)
                s.tap(key)
                val oldReads = s.drainMemoryAccesses().filter {
                    it.kind == MemoryAccessKind.READ && it.address in (image.endAddress + 1)..(image.startAddress + 500)
                }
                s.setMemoryAccessTracing(false)
                assertTrue(oldReads.isEmpty(), "old program read: $oldReads")
                if (key == PocketKey.UP) {
                    val expected = if (index == 0 || index == 4) line20 else line10
                    repeat(8) { phase ->
                        assertContentEquals(expected, s.displaySnapshot().copyDots(), "$model combined=$combined index=$index phase=$phase")
                        s.runCycles(100_000)
                    }
                }
            }
        }
    }
}
