package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.*
import com.digihori.pgp.core.source.basic.*
import java.io.File
import kotlin.test.*

/** Optional local-ROM checks; no firmware or ROM bytes are stored in the test data. */
class Pc1245BasicNavigationTest {
    @Test fun loaderPointersMatchRomEditorAndShortReplacement() {
        val rom = findFile("local-data/roms/pc-1245/pc1245mem.bin") ?: return
        fun session(): EmulatorSession {
            val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(rom.readBytes()))
            return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet)).session.also {
                it.runCycles(1_000_000)
                it.setOperatingMode(OperatingMode.PROGRAM)
                it.runCycles(200_000)
            }
        }
        val root = requireNotNull(findFile("test-data/basic/io/pc1245/short.bas")).parentFile
        val short = File(root, "short.bas").readText()
        val expected = compile(short)
        val typed = session()
        typed.type(short)
        assertPointersAndBody(typed, expected)
        val romPointers = typed.memorySnapshot(0xc6e1, 4).copyBytes()

        // Check both Studio's direct BASIC path and the combined program-image planner.
        for (combined in listOf(false, true)) {
            val used = session()
            for (stage in listOf("full", "edited", "short")) {
                val bytes = compile(File(root, "$stage.bas").readText())
                if (combined) {
                    assertIs<ProgramImageLoadResult.Success>(used.loadProgramImage(bytes, null))
                } else {
                    assertIs<BasicProgramLoadResult.Success>(used.loadBasicProgram(bytes))
                }
                assertPointersAndBody(used, bytes)
                used.type(if (stage == "short") "LIST\n" else "LIST 950\n")
                assertPointersAndBody(used, bytes)
                println("${if (combined) "combined" else "direct"}/$stage start=C000 end=${(0xc000 + bytes.lastIndex).toString(16).uppercase()} bytes=${bytes.size}")
            }
            assertContentEquals(romPointers, used.memorySnapshot(0xc6e1, 4).copyBytes(), "ROM versus loader pointers")
            // After LIST, navigation must agree with the ROM-created short program.
            val fresh = session()
            fresh.type(short)
            fresh.type("LIST\n")
            for (key in listOf(PocketKey.DOWN, PocketKey.DOWN, PocketKey.UP, PocketKey.UP)) {
                fresh.tap(key)
                used.tap(key)
                assertContentEquals(fresh.displaySnapshot().copyDots(), used.displaySnapshot().copyDots(), "after $key, combined=$combined")
                assertPointersAndBody(used, expected)
            }
        }
    }

    @Test fun replacementAfterRunThenClearDoesNotKeepOldNavigation() {
        val rom = findFile("local-data/roms/pc-1245/pc1245mem.bin") ?: return
        val root = requireNotNull(findFile("test-data/basic/io/pc1245/short.bas")).parentFile
        for (combined in listOf(false, true)) {
            fun load(s: EmulatorSession, bytes: ByteArray) {
                if (combined) assertIs<ProgramImageLoadResult.Success>(s.loadProgramImage(bytes, null))
                else assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(bytes))
            }
            val sessions = listOf("full", "short").map { initial ->
                val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(rom.readBytes()))
                val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet)).session
                s.runCycles(1_000_000)
                load(s, compile(File(root, "$initial.bas").readText()))
                s.type("RUN\n")
                s.runCycles(2_000_000)
                assertEquals(0x10, s.cpuSnapshot().copyInternalRam()[0x15].toInt() and 0x10, "RUN context before replacement")
                s.tap(PocketKey.CLEAR)
                val before = s.cpuSnapshot().copyInternalRam()[0x15]
                if (combined) {
                    assertIs<ProgramImageLoadResult.BasicFailure>(s.loadProgramImage(byteArrayOf(0), null))
                    assertIs<ProgramImageLoadResult.Success>(s.loadProgramImage(null, null))
                } else {
                    assertIs<BasicProgramLoadResult.Failure>(s.loadBasicProgram(byteArrayOf(0)))
                }
                assertEquals(before, s.cpuSnapshot().copyInternalRam()[0x15], "failed/non-BASIC load preserves context")
                val short = compile(File(root, "short.bas").readText())
                load(s, short)
                assertEquals(0, s.cpuSnapshot().copyInternalRam()[0x15].toInt() and 0x10, "RUN context after replacement")
                assertPointersAndBody(s, short)
                s.setOperatingMode(OperatingMode.PROGRAM)
                s.runCycles(200_000)
                s
            }
            val reference = sessions[1]
            // Establish the display oracle through the ROM editor, not the loader.
            reference.type("NEW\n")
            reference.type(File(root, "short.bas").readText())
            reference.tap(PocketKey.CLEAR)
            reference.type("LIST 20\n")
            val line20 = reference.displaySnapshot().copyDots()
            reference.type("LIST 10\n")
            val line10 = reference.displaySnapshot().copyDots()
            val used = sessions[0]
            for ((index,key) in listOf(PocketKey.UP, PocketKey.UP, PocketKey.UP, PocketKey.CLEAR, PocketKey.UP).withIndex()) {
                used.setMemoryAccessTracing(true)
                used.tap(key)
                val oldTextReads = used.drainMemoryAccesses().filter {
                    it.kind == MemoryAccessKind.READ && it.address in 0xc014..0xc14b
                }
                used.setMemoryAccessTracing(false)
                assertTrue(oldTextReads.isEmpty(), "Read old text after $key: $oldTextReads")
                assertPointersAndBody(used, compile(File(root, "short.bas").readText()))
                if (key == PocketKey.UP) {
                    val expected = if (index == 0 || index == 4) line20 else line10
                    repeat(8) { phase ->
                        assertContentEquals(expected, used.displaySnapshot().copyDots(), "cursor-inclusive display after $index phase $phase, combined=$combined")
                        used.runCycles(100_000)
                    }
                }
            }
        }
    }

    private fun compile(text: String): ByteArray = assertIs<BasicProgramCompileResult.Success>(
        BasicProgramCodec.compileProgram(text.encodeToByteArray(), Pc1245RomDefinition.MACHINE_ID),
    ).bytes

    private fun assertPointersAndBody(session: EmulatorSession, expected: ByteArray) {
        val pointers = session.memorySnapshot(0xc6e1, 4).copyBytes()
        fun word(offset: Int) = (pointers[offset].toInt() and 255) or ((pointers[offset + 1].toInt() and 255) shl 8)
        assertEquals(0xc000, word(0))
        assertEquals(0xc000 + expected.lastIndex, word(2))
        assertEquals(0xff.toByte(), session.memorySnapshot(word(2), 1).copyBytes().single())
        assertContentEquals(expected, session.memorySnapshot(word(0), expected.size).copyBytes())
        assertContentEquals(expected, assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot()).copyBytes())
    }

    private fun EmulatorSession.tap(key: PocketKey) {
        pressKey(key)
        runCycles(38_400)
        releaseKey(key)
        runCycles(57_600)
    }

    private fun EmulatorSession.type(text: String) {
        val doc = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(text)).document
        assertIs<Pc1245RomInputResult.Success>(Pc1245RomBasicInput.compile(doc)).keys.forEach { tap(it) }
    }

    private fun findFile(path: String): File? = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, path) }.firstOrNull { it.isFile }
}
