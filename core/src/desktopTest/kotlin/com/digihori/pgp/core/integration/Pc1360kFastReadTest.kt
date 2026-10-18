package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import com.digihori.pgp.core.source.basic.BasicProgramCodec
import com.digihori.pgp.core.source.basic.BasicProgramCompileResult
import java.io.File
import kotlin.test.*

class Pc1360kFastReadTest {
    private fun root() = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .first { File(it, "docs/analysis/pc1360k/read-fast-4k.asm").isFile }

    private fun assemble() = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
        File(root(), "docs/analysis/pc1360k/read-fast-4k.asm").readText(),
    ))

    @Test
    fun fastCopyPreservesRegistersAndChecksAllBytes() {
        val assembly = assemble()
        val bytes = assembly.image.segments.single().copyBytes()
        assertTrue(bytes.size < 0x200, "Code must not overlap parameters")
        val symbols = assembly.symbols.associate { it.name.lowercase() to it.address }
        val readSites = setOf(symbols.getValue("copy_read"), symbols.getValue("verify_read"))
        // Cover initial X wrap, both selections, last 4KiB, sum carries and 16-bit mismatch carries.
        for ((selector, start, mode) in listOf(
            Triple(0, 0, "baseline"), Triple(0, 0, "pattern"),
            Triple(1, 0x2000, "one-fault"), Triple(1, 0xf000, "all-fault"),
            Triple(0, 0, "maximum"),
        )) {
            val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
                ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
            )).romSet
            val machine = Pc1360Machine(rom)
            bytes.forEachIndexed { i, b -> machine.writeMemory(0xc000 + i, b.toInt() and 255) }
            val state = machine.cpuState
            state.programCounter = 0xc000; state.r = 0x5b; state.p = 0x2c; state.q = 0x2d
            (0..12).forEach { state.internalRam[it] = (it * 17 + 29) and 255 }
            val saved = (0..12).map { state.internalRam[it] }
            val previous = (start - 1) and 0xffff
            machine.writeMemory(0xc200, selector)
            machine.writeMemory(0xc201, previous and 255)
            machine.writeMemory(0xc202, previous ushr 8)
            machine.writeMemory(0xcfff, 0xa5); machine.writeMemory(0xe000, 0x5a)
            var count = 0
            var steps = 0
            val expected = List(4096) { offset ->
                when (mode) {
                    "baseline" -> 3
                    "maximum" -> 255
                    else -> ((start + offset) xor ((start + offset) ushr 8) xor (selector * 0x5a)) and 255
                }
            }
            while (state.programCounter != symbols.getValue("done") && steps++ < 300_000) {
                val pc = state.programCounter
                val isRead = pc in readSites
                val index = count % 4096
                if (isRead) {
                    assertEquals(start + index, state.dataPointer, "READ DP in $mode")
                    assertEquals(selector, machine.readMemory(0x3600))
                }
                assertNull(machine.step().stopReason)
                if (isRead) {
                    if (mode != "baseline") {
                        val damaged = count >= 4096 && (mode == "all-fault" || (mode == "one-fault" && index == 300))
                        state.internalRam[2] = expected[index] xor if (damaged) 1 else 0
                    }
                    count++
                }
            }
            assertEquals(symbols.getValue("done"), state.programCounter)
            assertEquals(8192, count)
            assertEquals(expected, (0xd000..0xdfff).map(machine::readMemory))
            assertEquals(expected.sum(), machine.readMemory(0xc212) + (machine.readMemory(0xc213) shl 8) + (machine.readMemory(0xc214) shl 16))
            val differences = machine.readMemory(0xc210) + (machine.readMemory(0xc211) shl 8)
            assertEquals(when (mode) { "one-fault" -> 1; "all-fault" -> 4096; else -> 0 }, differences)
            assertEquals(saved, (0..12).map { state.internalRam[it] })
            assertEquals(0x2c, state.p); assertEquals(0x2d, state.q); assertEquals(0x5b, state.r)
            assertEquals(0xa5, machine.readMemory(0xcfff)); assertEquals(0x5a, machine.readMemory(0xe000))
            assertEquals(165, machine.readMemory(0xc215)); assertEquals(1, machine.readMemory(0x3600))
            assertEquals(0, machine.selectedRomBank); assertEquals(0, state.fo)
        }
        // Git may check out BASIC text as CRLF on Windows; compare its content as LF.
        val publishedBasic = File(root(), "tools/pc1360k-rom-dump/pc1360k-kanji-dump.bas")
            .readText().replace("\r\n", "\n")
        assertEquals(batchBasic(bytes), publishedBasic)
        if (System.getenv("PGP_ANALYZE_PC1360K_FAST") == "1") {
            File(root(), "docs/analysis/pc1360k/read-fast-4k.bas").writeText(fastBasic(bytes))
            File(root(), "local-data/analysis/pc-1360k/read-fast-4k.bin").apply { parentFile.mkdirs(); writeBytes(bytes) }
        }
    }

    @Test
    fun localRomBatchVisitsEveryBlockAndStopsBeforeSavingBadData() {
        if (System.getenv("PGP_ANALYZE_PC1360K_FAST") != "1") return
        val directory = File(root(), "local-data/roms/pc-1360k")
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(),
            Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val assembly = assemble()
        val sites = assembly.symbols.filter { it.name.lowercase() in setOf("copy_read", "verify_read") }.associate { it.name.lowercase() to it.address }
        for (page in 0..3) for (fault in listOf(false, true)) {
            val machine = Pc1360Machine(rom)
            val packets = mutableListOf<ByteArray>()
            fun value(selector: Int, dp: Int) = (dp xor (dp ushr 8) xor (selector * 0x5a)) and 255
            fun run(budget: Long) {
                var cycles = 0L
                while (cycles < budget) {
                    val pc = machine.cpuState.programCounter
                    val dp = machine.cpuState.dataPointer
                    val selector = machine.readMemory(0x3600)
                    if (pc == 0xc300) packets += ByteArray(4112) { machine.readMemory(0xcff0 + it).toByte() }
                    val result = machine.step(); assertNull(result.stopReason); cycles += result.cycles
                    if (pc in sites.values) {
                        val damage = fault && pc == sites["verify_read"] && selector == page && dp == 0x112c
                        machine.cpuState.internalRam[2] = value(selector, dp) xor if (damage) 1 else 0
                    }
                }
                machine.buzzerState.drainPcm()
            }
            fun key(key: PocketKey) {
                assertTrue(machine.keyboardState.press(key)); run(100_000)
                machine.keyboardState.release(key); run(100_000)
            }
            run(1_000_000); key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000)
            // Only the cassette operation is replaced in this test. Hardware recording remains unverified.
            val text = File(root(), "tools/pc1360k-rom-dump/pc1360k-kanji-dump.bas").readText()
                .replace("400 CSAVE M &CFF0,&DFFF", "400 CALL &C300")
            assertFalse(text.contains("CSAVE M"))
            val basic = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
                text.encodeToByteArray(), Pc1360RomDefinition.PC1360K_MACHINE_ID,
            )).bytes
            assertIs<Pc1245BasicMemoryResult.Success>(machine.loadBasicProgram(basic))
            machine.writeMemory(0xc300, 0x37) // RTN: stand-in for completed cassette save.
            for (char in "RUN") {
                val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
                keys.forEach { machine.keyboardState.press(it) }; run(80_000)
                keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
            }
            key(PocketKey.ENTER)
            run(10_000_000) // DATA initialization, then INPUT prompt.
            fun input(value: String) {
                for (char in value) {
                    val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
                    keys.forEach { machine.keyboardState.press(it) }; run(80_000)
                    keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
                }
                key(PocketKey.ENTER)
            }
            if (page == 0 && !fault) {
                for (invalid in listOf("4", "-1", "0.5")) {
                    input(invalid); run(1_000_000)
                    assertTrue(packets.isEmpty(), "Invalid page must return to INPUT")
                }
            }
            input(page.toString())
            repeat(200) { if (machine.readMemory(0xc220) == 0) run(500_000) }
            assertEquals(if (fault) 238 else 90, machine.readMemory(0xc220), "page=$page fault=$fault packets=${packets.size} pc=${machine.cpuState.programCounter}")
            assertEquals(if (fault) 1 else 16, packets.size)
            packets.forEachIndexed { offset, packet ->
                val index = page * 16 + offset
                assertEquals("K136", packet.take(4).map { (it.toInt() and 255).toChar() }.joinToString(""))
                assertEquals(1, packet[4].toInt()); assertEquals(index / 16, packet[5].toInt())
                assertEquals(0, packet[6].toInt()); assertEquals((index % 16) * 16, packet[7].toInt() and 255)
                assertEquals(0, packet[8].toInt()); assertEquals(16, packet[9].toInt())
                assertEquals(index, packet[15].toInt())
                val expected = List(4096) { value(index / 16, (index % 16) * 4096 + it) }
                assertEquals(expected, packet.drop(16).map { it.toInt() and 255 })
                val sum = (packet[10].toInt() and 255) + ((packet[11].toInt() and 255) shl 8) + ((packet[12].toInt() and 255) shl 16)
                assertEquals(expected.sum(), sum)
                assertEquals(0, packet[13].toInt()); assertEquals(0, packet[14].toInt())
            }
        }
    }

    @Test
    fun localRomRunsFastReaderThroughBasicCallAndReturn() {
        if (System.getenv("PGP_ANALYZE_PC1360K_FAST") != "1") return
        val root = root()
        val directory = File(root, "local-data/roms/pc-1360k")
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(),
            Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val assembly = assemble()
        val sites = assembly.symbols.filter { it.name.lowercase() in setOf("copy_read", "verify_read") }.map { it.address }.toSet()
        val fixtureMode = System.getenv("PGP_PC1360K_FAST_FIXTURES") == "1"
        val table = if (fixtureMode) File(root, "local-data/analysis/pc-1360k/read-s0-0100-01ff.bin").readBytes() else byteArrayOf()
        var reads = 0
        val machine = Pc1360Machine(rom)
        fun run(budget: Long) {
            var cycles = 0L
            while (cycles < budget) {
                val pc = machine.cpuState.programCounter
                val dp = machine.cpuState.dataPointer
                val result = machine.step(); assertNull(result.stopReason); cycles += result.cycles
                if (pc in sites) {
                    // Optional fixture injection tests this tool; it does not change production CPU behavior.
                    if (fixtureMode) machine.cpuState.internalRam[2] = if (dp in 0x100..0x1ff) table[dp - 0x100].toInt() and 255 else (dp xor (dp ushr 8)) and 255
                    reads++
                }
            }
            machine.buzzerState.drainPcm()
        }
        fun key(key: PocketKey) {
            assertTrue(machine.keyboardState.press(key)); run(100_000)
            machine.keyboardState.release(key); run(100_000)
        }
        run(1_000_000); key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000)
        val basic = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
            fastBasic(assembly.image.segments.single().copyBytes()).encodeToByteArray(), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).bytes
        assertIs<Pc1245BasicMemoryResult.Success>(machine.loadBasicProgram(basic))
        for (char in "RUN") {
            val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
            keys.forEach { machine.keyboardState.press(it) }; run(80_000)
            keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
        }
        key(PocketKey.ENTER)
        repeat(50) { if (machine.readMemory(0xc215) != 165) run(500_000) }
        assertEquals(165, machine.readMemory(0xc215)); assertEquals(8192, reads)
        val expected = List(4096) { dp -> if (!fixtureMode) 3 else if (dp in 0x100..0x1ff) table[dp - 0x100].toInt() and 255 else (dp xor (dp ushr 8)) and 255 }
        assertEquals(expected, (0xd000..0xdfff).map(machine::readMemory))
        assertEquals(expected.sum(), machine.readMemory(0xc212) + (machine.readMemory(0xc213) shl 8) + (machine.readMemory(0xc214) shl 16))
        assertEquals(0, machine.readMemory(0xc210) + (machine.readMemory(0xc211) shl 8))
        run(500_000); repeat(2) { key(PocketKey.ENTER); run(500_000) }
        assertEquals(90, machine.readMemory(0xc216), "BASIC must print both results and finish")
        assertEquals(expected.sum(), machine.readMemory(0xc217) + (machine.readMemory(0xc218) shl 8) + (machine.readMemory(0xc219) shl 16), "BASIC must calculate the displayed sum correctly")
        assertEquals(0, machine.readMemory(0xc21a) + (machine.readMemory(0xc21b) shl 8))
    }
}

private fun fastBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ B")
    appendLine("30 POKE 49152+I,B")
    appendLine("40 NEXT I")
    appendLine("60 S=0:A=0")
    appendLine("70 Z=A-1:IF Z<0 THEN LET Z=65535")
    appendLine("80 H=INT(Z/256)")
    appendLine("90 POKE 49664,S")
    appendLine("100 POKE 49665,Z-256*H")
    appendLine("110 POKE 49666,H")
    appendLine("120 CALL 49152")
    appendLine("130 D=PEEK (49680)+256*PEEK (49681)")
    appendLine("140 C=PEEK (49682)+256*PEEK (49683)+65536*PEEK (49684)")
    appendLine("141 POKE 49687,C-256*INT(C/256)")
    appendLine("142 POKE 49688,INT(C/256)-256*INT(C/65536)")
    appendLine("143 POKE 49689,INT(C/65536)")
    appendLine("144 POKE 49690,D-256*INT(D/256)")
    appendLine("145 POKE 49691,INT(D/256)")
    appendLine("150 PRINT \"SUM:\";C")
    appendLine("160 PRINT \"D:\";D")
    appendLine("170 POKE 49686,90")
    appendLine("180 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${300 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun batchBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO &${(bytes.size - 1).toString(16).uppercase()}")
    appendLine("20 READ B")
    appendLine("30 POKE &C000+I,B")
    appendLine("40 NEXT I")
    appendLine("45 INPUT \"PAGE(0-3)\";P")
    appendLine("52 IF P<0 THEN GOTO 45")
    appendLine("54 IF P>3 THEN GOTO 45")
    appendLine("56 IF P<>INT(P) THEN GOTO 45")
    appendLine("58 POKE &C220,0")
    appendLine("60 FOR K=&10*P TO &10*P+&F")
    appendLine("70 S=INT(K/&10):A=&1000*(K-&10*S)")
    appendLine("80 Z=A-1:IF Z<0 THEN LET Z=&FFFF")
    appendLine("90 H=INT(Z/&100)")
    appendLine("100 POKE &C200,S")
    appendLine("110 POKE &C201,Z-&100*H")
    appendLine("120 POKE &C202,H")
    appendLine("130 CALL &C000")
    appendLine("140 D=PEEK (&C210)+&100*PEEK (&C211)")
    appendLine("150 IF D<>0 THEN GOTO 700")
    appendLine("200 POKE &CFF0,&4B,&31,&33,&36,1")
    appendLine("210 POKE &CFF5,S")
    appendLine("220 POKE &CFF6,0")
    appendLine("230 POKE &CFF7,A/&100")
    appendLine("240 POKE &CFF8,0,&10")
    appendLine("250 POKE &CFFA,PEEK (&C212)")
    appendLine("260 POKE &CFFB,PEEK (&C213)")
    appendLine("270 POKE &CFFC,PEEK (&C214)")
    appendLine("280 POKE &CFFD,0,0,K")
    appendLine("400 CSAVE M &CFF0,&DFFF")
    appendLine("410 NEXT K")
    appendLine("420 POKE &C220,&5A")
    appendLine("430 PRINT \"DONE\"")
    appendLine("440 END")
    appendLine("700 POKE &C220,&EE")
    appendLine("710 PRINT \"ERROR BLOCK:\";K")
    appendLine("720 PRINT \"D:\";D")
    appendLine("730 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${1000 + index * 10} DATA " + chunk.joinToString(",") { "&" + (it.toInt() and 255).toString(16).uppercase().padStart(2, '0') })
    }
}
