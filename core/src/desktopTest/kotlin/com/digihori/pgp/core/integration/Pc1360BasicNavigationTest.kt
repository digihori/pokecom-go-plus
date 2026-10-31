package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

class Pc1360BasicNavigationTest {
    private fun find(path: String) = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, path) }.firstOrNull(File::isFile)
    private fun session(model: String = "1360k"): EmulatorSession? {
        val mem = find("local-data/roms/pc-$model/pc${model}mem.bin") ?: return null
        val bank = requireNotNull(find("local-data/roms/pc-$model/pc${model}bank.bin"))
        val id = if (model == "1360k") Pc1360RomDefinition.PC1360K_MACHINE_ID else Pc1360RomDefinition.MACHINE_ID
        val imported = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(mem.readBytes(), bank.readBytes(), id))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, imported.romSet)).session.also {
            it.runCycles(1_000_000)
            it.tap(PocketKey.Y); it.tap(PocketKey.ENTER); it.tap(PocketKey.CLEAR); it.runCycles(500_000)
            assertTrue(DisplaySymbol.RUN in it.displaySnapshot().symbols)
        }
    }
    private fun EmulatorSession.tap(key: PocketKey) {
        assertIs<InputResult.Accepted>(pressKey(key)); runCycles(80_000)
        releaseKey(key); runCycles(100_000)
    }
    private fun EmulatorSession.type(text: String) {
        for (c in text) if (c == '\n') tap(PocketKey.ENTER)
        else requireNotNull(Pc1360CharacterInput.keySequence(c)).forEach { tap(it) }
    }
    @Test fun replacementAfterRunClearsCursorContext() {
        val root = requireNotNull(find("test-data/basic/io/pc1360k/short.bas")).parentFile
        val short = File(root, "short.bas").readText()
        val full = File(root, "full.bas").readText()
        val keys = listOf(PocketKey.UP, PocketKey.UP, PocketKey.UP, PocketKey.CLEAR, PocketKey.UP)
        for (model in listOf("1360k", "1360")) for (combined in listOf(false, true)) {
            val reference = session(model) ?: continue
            reference.tap(PocketKey.MODE)
            assertTrue(DisplaySymbol.PRO in reference.displaySnapshot().symbols)
            reference.type("NEW\n"); reference.type(short); reference.tap(PocketKey.CLEAR)
            val expectedProgram = assertIs<BasicProgramSnapshotResult.Success>(reference.basicProgramSnapshot()).copyBytes()
            val displays = keys.map { key ->
                reference.tap(key)
                reference.displaySnapshot().copyDots()
            }
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
            } else assertIs<BasicProgramLoadResult.Failure>(s.loadBasicProgram(byteArrayOf(0)))
            assertEquals(flags, s.cpuSnapshot().copyInternalRam()[0x35])
            load(short)
            assertEquals(flags.toInt() and 0xef, s.cpuSnapshot().copyInternalRam()[0x35].toInt() and 255)
            s.tap(PocketKey.MODE)
            assertTrue(DisplaySymbol.PRO in s.displaySnapshot().symbols)
            val image = assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot())
            assertContentEquals(expectedProgram, image.copyBytes(), "ROM versus loader image")
            for ((index,key) in keys.withIndex()) {
                s.setMemoryAccessTracing(true)
                s.tap(key)
                val oldReads = s.drainMemoryAccesses().filter {
                    it.kind == MemoryAccessKind.READ && it.address in (image.endAddress + 1)..(image.startAddress + 700)
                }
                s.setMemoryAccessTracing(false)
                assertTrue(oldReads.isEmpty(), "Read old program: $oldReads")
                repeat(8) { phase ->
                    assertContentEquals(displays[index], s.displaySnapshot().copyDots(), "$model combined=$combined key=$key index=$index phase=$phase")
                    s.runCycles(100_000)
                }
            }
        }
    }
}
