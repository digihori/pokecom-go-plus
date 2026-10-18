package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import java.io.File
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.debug.Sc61860Disassembler
import com.digihori.pgp.core.source.basic.BasicProgramCodec
import com.digihori.pgp.core.source.basic.BasicProgramCompileResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import kotlin.test.*

/** Explicitly enabled local experiment; never writes firmware-derived traces into tracked files. */
class Pc1360kLocalAnalysisTest {
    @Test
    fun hardwareReadProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-path-probe.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-path-probe.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        repeat(100) {
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(listOf(3, 0x12, 3, 0x12), (0xc200..0xc203).map(machine::readMemory))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-path-probe.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-path-probe.bas").writeText(probeBasic(segment.copyBytes()))
        }
    }

    @Test
    fun addressRangeProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-address-range.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-address-range.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        repeat(250) {
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(List(16) { 3 }, (0xc200..0xc20f).map(machine::readMemory))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-address-range.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-address-range.bas").writeText(rangeBasic(segment.copyBytes()))
        }
    }

    @Test
    fun blockProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-block.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-block.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        assertEquals(0xc005, assembled.symbols.first { it.name.equals("select", true) }.address)
        assertEquals(0xc00b, assembled.symbols.first { it.name.equals("source", true) }.address)
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        repeat(250) {
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(listOf(3), listOf(machine.readMemory(0xc200)))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-block.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-block.bas").writeText(blockBasic(segment.copyBytes()))
        }
    }

    @Test
    fun boundaryRepeatProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-boundary-repeat.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-boundary-repeat.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        val reads = mutableListOf<Pair<Int, Int>>()
        repeat(500) {
            if (machine.readMemory(machine.cpuState.programCounter) == 0x56) {
                reads += machine.readMemory(0x3600) to machine.cpuState.dataPointer
            }
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(List(2) { (0..1).flatMap { selector ->
            (0x3ffc..0x4003).map { selector to it }
        } }.flatten(), reads)
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(List(32) { 3 }, (0xc200..0xc21f).map(machine::readMemory))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-boundary-repeat.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-boundary-repeat.bas").writeText(boundaryBasic(segment.copyBytes()))
        }
    }

    @Test
    fun hiraganaTableProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-hiragana-table.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-hiragana-table.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        repeat(100) {
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(List(6) { 3 }, (0xc200..0xc205).map(machine::readMemory))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-hiragana-table.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-hiragana-table.bas").writeText(tableBasic(segment.copyBytes()))
        }
    }

    @Test
    fun hiraganaGlyphProbeHasDocumentedEmulatorBaseline() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-hiragana-glyph.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-hiragana-glyph.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 0xff)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val done = assembled.symbols.first { it.name.equals("done", true) }.address
        repeat(512) {
            if (machine.cpuState.programCounter != done) assertNull(machine.step().stopReason)
        }
        assertEquals(done, machine.cpuState.programCounter)
        assertEquals(List(30) { 3 }, (0xc200..0xc21d).map(machine::readMemory))
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(1, machine.readMemory(0x3600))
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            val segment = assembled.image.segments.single()
            File(output, "read-hiragana-glyph.bin").writeBytes(segment.copyBytes())
            File(root, "docs/analysis/pc1360k/read-hiragana-glyph.bas").writeText(glyphBasic(segment.copyBytes()))
        }
    }

    @Test
    fun matrixProbeVariesDpAtFixedReadSites() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-path-matrix.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-path-matrix.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 255)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e
        }
        val symbols = assembled.symbols.associate { it.name.lowercase() to it.address }
        val observed = mutableListOf<Pair<Int, Int>>()
        repeat(256) {
            val pc = machine.cpuState.programCounter
            if (pc != symbols.getValue("done")) {
                if (machine.readMemory(pc) == 0x56) observed += pc to machine.cpuState.dataPointer
                assertNull(machine.step().stopReason)
            }
        }
        assertEquals(symbols.getValue("done"), machine.cpuState.programCounter)
        assertEquals(List(2) { listOf(3, 18, 3, 18) }.flatten(), (0xc200..0xc207).map(machine::readMemory))
        assertEquals(List(2) { listOf(
            symbols.getValue("read_lib") to 0xc100, symbols.getValue("read_lip") to 0xc100,
            symbols.getValue("read_lib") to 0xc101, symbols.getValue("read_lip") to 0xc101,
        ) }.flatten(), observed)
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(0xa5, machine.readMemory(0xc100))
        assertEquals(0x5a, machine.readMemory(0xc101))
        val bytes = assembled.image.segments.single().copyBytes()
        assertTrue(bytes.size < 256)
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            File(root, "docs/analysis/pc1360k/read-path-matrix.bas").writeText(matrixBasic(bytes))
            val out = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            File(out, "read-path-matrix.bin").writeBytes(bytes)
        }
    }

    @Test
    fun readmProbePreservesItsInternalRamDestination() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "docs/analysis/pc1360k/read-path-readm.asm").isFile }
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            File(root, "docs/analysis/pc1360k/read-path-readm.asm").readText(),
        ))
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        assembled.image.segments.forEach { segment -> segment.copyBytes().forEachIndexed { index, byte ->
            machine.writeMemory(segment.startAddress + index, byte.toInt() and 255)
        } }
        machine.cpuState.apply {
            programCounter = 0xc000; r = 0x5b; p = 0x2c
            internalRam[2] = 0x99; internalRam[3] = 0x7e; internalRam[0x20] = 0x66
        }
        val symbols = assembled.symbols.associate { it.name.lowercase() to it.address }
        val observed = mutableListOf<Pair<Int, Int>>()
        repeat(256) {
            val pc = machine.cpuState.programCounter
            if (pc != symbols.getValue("done")) {
                if (machine.readMemory(pc) in setOf(0x54, 0x56)) observed += pc to machine.cpuState.dataPointer
                assertNull(machine.step().stopReason)
            }
        }
        assertEquals(symbols.getValue("done"), machine.cpuState.programCounter)
        assertEquals(List(2) { listOf(3, 89, 3, 89) }.flatten(), (0xc200..0xc207).map(machine::readMemory))
        assertEquals(List(2) { listOf(
            symbols.getValue("read_lib") to 0xc100, (symbols.getValue("read_mem") + 2) to 0xc100,
            symbols.getValue("read_lib") to 0xc101, (symbols.getValue("read_mem") + 2) to 0xc101,
        ) }.flatten(), observed)
        assertEquals(0x99, machine.cpuState.internalRam[2])
        assertEquals(0x7e, machine.cpuState.internalRam[3])
        assertEquals(0x2c, machine.cpuState.p)
        assertEquals(0x5b, machine.cpuState.r)
        assertEquals(0x66, machine.cpuState.internalRam[0x20])
        assertEquals(0xa5, machine.readMemory(0xc100))
        assertEquals(0x5a, machine.readMemory(0xc101))
        val bytes = assembled.image.segments.single().copyBytes()
        assertTrue(bytes.size < 256)
        if (System.getenv("PGP_ANALYZE_PC1360K") == "1") {
            File(root, "docs/analysis/pc1360k/read-path-readm.bas").writeText(matrixBasic(bytes))
            val out = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
            File(out, "read-path-readm.bin").writeBytes(bytes)
        }
    }

    @Test
    fun reproduceWidthKanjiAndA() {
        if (System.getenv("PGP_ANALYZE_PC1360K") != "1") return
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "local-data/roms/pc-1360k/pc1360kmem.bin").isFile }
        val directory = File(root, "local-data/roms/pc-1360k")
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(directory, "pc1360kmem.bin").readBytes(), File(directory, "pc1360kbank.bin").readBytes(),
            Pc1360RomDefinition.PC1360K_MACHINE_ID,
        )).romSet
        val machine = Pc1360Machine(rom)
        val output = File(root, "local-data/analysis/pc-1360k").also { it.mkdirs() }
        machine.writeMemory(0x3400, 1)
        File(output, "glyph-routine.asm.txt").writeText(Sc61860Disassembler.renderAssembly(0x5013, 0x527d, machine::readMemory))
        File(output, "row-addresses.tsv").bufferedWriter().use { table ->
            table.appendLine("row\tcolumn\tfirstReadPC\tcomputedDP")
            val cases = (0x21..0x7e).map { it to 0x22 } + (0x21..0x7e).map { 0x24 to it }
            for ((row, column) in cases) {
                val probe = Pc1360Machine(rom)
                probe.writeMemory(0x3400, 1)
                probe.cpuState.apply {
                    programCounter = 0x5099; p = 7; q = 7; r = 0x5b
                    internalRam[2] = 0; internalRam[3] = 0xfd
                    internalRam[4] = column; internalRam[5] = row
                    internalRam[6] = 0; internalRam[7] = 0
                }
                var reached = false
                repeat(256) {
                    if (!reached) {
                        val pc = probe.cpuState.programCounter
                        if (probe.readMemory(pc) == 0x56) {
                            val normalizedRow = row - 0x21 - if (row >= 0x30) 7 else 0
                            val expected = normalizedRow * 0x72 + ((column - 0x20) ushr 4) * 0x13
                            assertEquals(expected, probe.cpuState.dataPointer, "row=$row column=$column")
                            table.appendLine("%02x\t%02x\t%04x\t%04x".format(row, column, pc, probe.cpuState.dataPointer))
                            reached = true
                        } else assertNull(probe.step().stopReason)
                    }
                }
                assertTrue(reached, "No table-read entry for row $row")
            }
        }
        File(output, "controlled-header-check.tsv").bufferedWriter().use { table ->
            table.appendLine("injectedHeader\tsecondSelector\tcomputedGlyphX")
            for (header in 0..7) {
                val probe = Pc1360Machine(rom)
                probe.writeMemory(0x3400, 1)
                probe.cpuState.apply {
                    programCounter = 0x5099; p = 7; q = 7; r = 0x5b
                    internalRam[3] = 0xfd; internalRam[4] = 0x22; internalRam[5] = 0x24
                }
                var reached = false
                repeat(512) {
                    if (!reached) {
                        val pc = probe.cpuState.programCounter
                        if (pc == 0x5119) reached = true else {
                            assertNull(probe.step().stopReason)
                            // Test-only data injection after READ. This does not define a hardware mapping.
                            val data = when (pc) {
                                0x50dd -> header
                                0x50ef -> 0x12
                                0x50f2 -> 0x34
                                0x50ff -> 0
                                0x510a -> 1
                                else -> null
                            }
                            if (data != null) probe.cpuState.internalRam[2] = data
                        }
                    }
                }
                assertTrue(reached)
                val glyphX = probe.cpuState.internalRam[4] + (probe.cpuState.internalRam[5] shl 8)
                assertEquals((header and 4) ushr 2, probe.readMemory(0x3600))
                assertEquals(((header and 3) shl 14) + 0x1234, glyphX)
                assertEquals(1, probe.selectedRomBank)
                table.appendLine("%02x\t%d\t%04x".format(header, probe.readMemory(0x3600), glyphX))
            }
        }
        machine.coldReset()
        val runName = System.getenv("PGP_PC1360K_ANALYSIS_KEY") ?: "A"
        val inputKey = PocketKey.valueOf(runName)
        val katakana = System.getenv("PGP_PC1360K_ANALYSIS_KATA") == "1"
        val suffix = (if (runName == "A") "" else "-" + runName.lowercase()) + if (katakana) "-kata" else ""
        File(output, "width-kanji-a$suffix.txt").bufferedWriter().use { log ->
            var stage = "boot"
            var number = 0L
            var remaining = 0
            var selectors = 0
            var fChanges = 0
            var fWrites = 0
            var reads = 0
            val selectorValues = mutableListOf<Int>()
            val recent = ArrayDeque<String>()
            machine.setMemoryAccessTracing(true)
            fun hex(value: Int, digits: Int = 4) = value.toString(16).padStart(digits, '0')
            val blockFault = System.getenv("PGP_PC1360K_BLOCK_FAULT") == "1"
            var blockReads = 0
            fun run(budget: Long) {
                var cycles = 0L
                while (cycles < budget) {
                    val state = machine.cpuState
                    val pc = state.programCounter
                    val fo = state.fo
                    val op = machine.readMemory(pc)
                    val bank = machine.selectedRomBank
                    val dp = state.dataPointer
                    val a = state.internalRam[2]
                    val b = state.internalRam[3]
                    val p = state.p
                    val q = state.q
                    val x = state.internalRam[4] + (state.internalRam[5] shl 8)
                    val y = state.internalRam[6] + (state.internalRam[7] shl 8)
                    val step = machine.step()
                    if (stage == "basic-probe" && pc == 0xc00e && op == 0x56 && blockFault) {
                        // Test-only pattern and one damaged second-pass byte; never a production ROM mapping.
                        val value = (dp xor (dp ushr 8)) and 255
                        state.internalRam[2] = value xor (if (blockReads == 300) 1 else 0)
                        blockReads++
                    }
                    assertNull(step.stopReason)
                    cycles += step.cycles
                    val accesses = machine.drainMemoryAccesses()
                    val writes = accesses.filter { it.kind == MemoryAccessKind.WRITE && it.address == 0x3600 }
                    val line = "$number $stage bank=$bank pc=${hex(pc)} op=${hex(op,2)} dp=${hex(dp)} a=${hex(a,2)} b=${hex(b,2)} p=${hex(p,2)} q=${hex(q,2)} x=${hex(x)} y=${hex(y)} fo=${hex(fo,2)} " +
                        accesses.joinToString(" ") { "${it.kind}:${hex(it.address)}=${hex(it.value,2)}" }
                    if (writes.isNotEmpty()) {
                        selectors += writes.size
                        selectorValues.addAll(writes.map { it.value })
                        if (selectors <= 100) {
                            log.appendLine("SELECTOR $stage count=$selectors")
                            recent.forEach { log.appendLine(it) }
                            remaining = 256
                        }
                    }
                    if (remaining > 0) { log.appendLine(line); remaining-- }
                    if (stage == "a" && op == 0x56 && bank == 1 && pc in 0x5000..0x5270) {
                        reads++
                        if (reads <= 100) log.appendLine("GLYPH_READ $line")
                    }
                    if (state.fo != fo) { fChanges++; log.appendLine("F_CHANGE $line new=${hex(state.fo,2)}") }
                    // OUTF is recorded even when the output value does not change.
                    if (op == 0x5f) { fWrites++; log.appendLine("F_OPCODE $line") }
                    recent.addLast(line)
                    if (recent.size > 24) recent.removeFirst()
                    number++
                }
                machine.buzzerState.drainPcm()
            }
            fun summary() {
                log.appendLine("SUMMARY $stage selectors=$selectors fChanges=$fChanges fOpcodes=$fWrites bank=${machine.selectedRomBank} pc=${hex(machine.cpuState.programCounter)} fo=${hex(machine.cpuState.fo,2)} mode=${hex(machine.readMemory(0x303c),2)} charMax=${machine.readMemory(0xfd07)} width=${machine.readMemory(0xfd08)}")
                File(output, "$stage$suffix-vram.bin").writeBytes(machine.displayState.copyDots())
            }
            fun key(key: PocketKey) {
                assertTrue(machine.keyboardState.press(key)); run(100_000)
                machine.keyboardState.release(key); run(100_000)
            }
            run(1_000_000); summary()
            stage = "initialize"; key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000); summary()
            // Run the probe through the real BASIC CALL/RTN path before glyph entry.
            val blockRead = System.getenv("PGP_PC1360K_BLOCK") == "1"
            val boundaryRead = System.getenv("PGP_PC1360K_BOUNDARY") == "1"
            val rangeRead = System.getenv("PGP_PC1360K_RANGE") == "1"
            val glyphRead = System.getenv("PGP_PC1360K_GLYPH") == "1"
            val tableRead = System.getenv("PGP_PC1360K_TABLE") == "1"
            val readm = System.getenv("PGP_PC1360K_READM") == "1"
            val matrix = readm || System.getenv("PGP_PC1360K_MATRIX") == "1"
            val name = if (blockRead) "read-block" else if (boundaryRead) "read-boundary-repeat" else if (rangeRead) "read-address-range" else if (glyphRead) "read-hiragana-glyph" else if (tableRead) "read-hiragana-table" else if (readm) "read-path-readm" else if (matrix) "read-path-matrix" else "read-path-probe"
            val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
                File(root, "docs/analysis/pc1360k/$name.asm").readText(),
            ))
            val basic = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
                (if (blockRead) blockBasic(assembled.image.segments.single().copyBytes())
                    else if (boundaryRead) boundaryBasic(assembled.image.segments.single().copyBytes())
                    else if (rangeRead) rangeBasic(assembled.image.segments.single().copyBytes())
                    else if (glyphRead) glyphBasic(assembled.image.segments.single().copyBytes())
                    else if (tableRead) tableBasic(assembled.image.segments.single().copyBytes())
                    else if (matrix) matrixBasic(assembled.image.segments.single().copyBytes())
                    else probeBasic(assembled.image.segments.single().copyBytes())).encodeToByteArray(),
                Pc1360RomDefinition.PC1360K_MACHINE_ID,
            )).bytes
            assertIs<Pc1245BasicMemoryResult.Success>(machine.loadBasicProgram(basic))
            stage = "basic-probe"
            log.appendLine("BASIC_PROBE $name")
            for (char in "RUN") {
                val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
                keys.forEach { machine.keyboardState.press(it) }; run(80_000)
                keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
            }
            key(PocketKey.ENTER)
            val marker = if (blockRead) 0xc210 else if (boundaryRead) 0xc220 else if (rangeRead) 0xc210 else if (glyphRead) 0xc21e else if (tableRead) 0xc206 else if (matrix) 0xc208 else 0xc204
            repeat(100) { if (machine.readMemory(marker) != 165) run(500_000) }
            run(500_000); summary()
            val expected = (if (boundaryRead) List(32) { 3 } else if (rangeRead) List(16) { 3 } else if (glyphRead) List(30) { 3 } else if (tableRead) List(6) { 3 } else if (matrix) List(2) { if (readm) listOf(3, 89, 3, 89) else listOf(3, 18, 3, 18) }.flatten() else listOf(3, 18, 3, 18)) + 165
            if (blockRead) {
                assertEquals(165, machine.readMemory(marker), "Block BASIC must resume and finish both passes")
                assertEquals(if (blockFault) (0x100..0x1ff).map { (it xor (it ushr 8)) and 255 } else List(256) { 3 }, (0xd000..0xd0ff).map(machine::readMemory))
                assertEquals(if (blockFault) 32640 else 768, (machine.readMemory(0xc213) shl 8) + machine.readMemory(0xc214))
                if (blockFault) assertEquals(512, blockReads)
                assertEquals(if (blockFault) 1 else 0, ((machine.readMemory(0xc212) shl 8) + machine.readMemory(0xc215)), "Block reread comparison must detect injected damage")
                repeat(2) { key(PocketKey.ENTER); run(1_000_000) }
                assertEquals(90, machine.readMemory(0xc211), "Block output must finish")
            } else assertEquals(expected, (0xc200..marker).map(machine::readMemory), "BASIC must resume after CALL")
            if (boundaryRead) {
                repeat(5) { key(PocketKey.ENTER); run(1_000_000) }
                assertEquals(90, machine.readMemory(0xc221), "Boundary output must finish after five PRINT continuations")
                assertEquals(0, machine.readMemory(0xc222), "Repeated read comparison must report no differences")
            }
            if (rangeRead) {
                repeat(4) { key(PocketKey.ENTER); run(1_000_000) }
                assertEquals(90, machine.readMemory(0xc211), "Range output must finish after four PRINT continuations")
            }
            if (glyphRead) {
                run(2_000_000)
                File(output, "glyph-page0-vram.bin").writeBytes(machine.displayState.copyDots())
                repeat(8) { page ->
                    key(PocketKey.ENTER); run(1_000_000)
                    File(output, "glyph-page${page + 1}-vram.bin").writeBytes(machine.displayState.copyDots())
                }
                run(1_000_000)
                File(output, "glyph-print-final-vram.bin").writeBytes(machine.displayState.copyDots())
                assertEquals(90, machine.readMemory(0xc21f), "Glyph output must finish after eight PRINT continuations")
            }
            machine.coldReset()
            selectors = 0; selectorValues.clear(); fChanges = 0; fWrites = 0; reads = 0
            remaining = 0; recent.clear()
            log.appendLine("RESET_AFTER_BASIC_PROBE")
            stage = "boot-replay"; run(1_000_000); summary()
            stage = "initialize-replay"; key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000); summary()
            stage = "width-command"
            for (char in "WIDTH,2") {
                val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
                keys.forEach { assertTrue(machine.keyboardState.press(it)) }; run(20_000)
                keys.asReversed().forEach { machine.keyboardState.release(it) }; run(30_000)
            }
            key(PocketKey.ENTER); run(1_000_000); summary()
            assertEquals(2, machine.readMemory(0xfd08))
            assertEquals(18, machine.readMemory(0xfd07))
            stage = "kanji"; key(PocketKey.KANJI); run(100_000); summary()
            assertEquals(0xa8, machine.readMemory(0x303c))
            if (katakana) { stage = "kata"; key(PocketKey.HIRA_KATA); run(100_000); summary() }
            stage = "a"; key(inputKey); run(100_000); summary()
            if (inputKey in setOf(PocketKey.A, PocketKey.I, PocketKey.U, PocketKey.E, PocketKey.O))
                assertTrue(selectorValues.isNotEmpty(), "No Kanji read path reached")
            File(output, "probe-$runName$suffix.txt").writeText("selectors=$selectorValues reads=$reads fChanges=$fChanges fOpcodes=$fWrites\n")
        }
    }
}


private fun probeBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49668,165")
    appendLine("""60 PRINT PEEK 49664;",";PEEK 49665;",";PEEK 49666;",";PEEK 49667""")
    appendLine("70 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${100 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 0xff).toString() })
    }
}

private fun matrixBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49672,165")
    appendLine("""60 PRINT "0:";PEEK 49664;",";PEEK 49665;",";PEEK 49666;",";PEEK 49667""")
    appendLine("""70 PRINT "1:";PEEK 49668;",";PEEK 49669;",";PEEK 49670;",";PEEK 49671""")
    appendLine("""80 PRINT "R:";PEEK 49408;",";PEEK 49409""")
    appendLine("90 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${100 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun tableBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49670,165")
    appendLine("""60 PRINT "T0:";PEEK 49664;",";PEEK 49665;",";PEEK 49666""")
    appendLine("""70 PRINT "T1:";PEEK 49667;",";PEEK 49668;",";PEEK 49669""")
    appendLine("80 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${100 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun glyphBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49694,165")
    appendLine("60 FOR J=0 TO 6")
    appendLine("70 A=49664+4*J")
    appendLine("""80 PRINT J;":";PEEK A;",";PEEK (A+1);",";PEEK (A+2);",";PEEK (A+3)""")
    appendLine("100 NEXT J")
    appendLine("""110 PRINT "7:";PEEK 49692;",";PEEK 49693""")
    appendLine("120 POKE 49695,90")
    appendLine("130 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${200 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun rangeBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49680,165")
    listOf("0T", "0G", "1T", "1G").forEachIndexed { index, label ->
        val reads = (0..3).joinToString(";\",\";") { "PEEK ${49664 + index * 4 + it}" }
        appendLine("${60 + index * 10} PRINT \"$label:\";$reads")
    }
    appendLine("100 POKE 49681,90")
    appendLine("110 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${200 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun boundaryBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ A")
    appendLine("30 POKE 49152+I,A")
    appendLine("40 NEXT I")
    appendLine("50 CALL 49152")
    appendLine("55 POKE 49696,165")
    appendLine("60 D=0")
    appendLine("70 FOR I=0 TO 15")
    appendLine("80 IF PEEK (49664+I)<>PEEK (49680+I) THEN LET D=D+1")
    appendLine("90 NEXT I")
    appendLine("100 POKE 49698,D")
    listOf("0L", "0H", "1L", "1H").forEachIndexed { index, label ->
        val reads = (0..3).joinToString(";\",\";") { "PEEK ${49664 + index * 4 + it}" }
        appendLine("${110 + index * 10} PRINT \"$label:\";$reads")
    }
    appendLine("150 PRINT \"D:\";D")
    appendLine("160 POKE 49697,90")
    appendLine("170 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${200 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}

private fun blockBasic(bytes: ByteArray): String = buildString {
    appendLine("10 FOR I=0 TO ${bytes.size - 1}")
    appendLine("20 READ B")
    appendLine("30 POKE 49152+I,B")
    appendLine("40 NEXT I")
    appendLine("60 S=0:A=256:N=256")
    appendLine("70 D=0:C=0")
    appendLine("80 FOR J=0 TO 1")
    appendLine("90 FOR I=0 TO N-1")
    appendLine("100 Z=A+I:H=INT(Z/256)")
    appendLine("110 POKE 49158,S")
    appendLine("120 POKE 49164,H")
    appendLine("130 POKE 49165,Z-256*H")
    appendLine("140 CALL 49152")
    appendLine("150 B=PEEK 49664")
    appendLine("160 IF J=0 THEN POKE 53248+I,B")
    appendLine("170 IF J=0 THEN LET C=C+B")
    appendLine("180 IF J=1 AND PEEK (53248+I)<>B THEN LET D=D+1")
    appendLine("190 NEXT I")
    appendLine("200 NEXT J")
    appendLine("210 POKE 49682,INT(D/256)")
    appendLine("211 POKE 49685,D-256*INT(D/256)")
    appendLine("212 POKE 49683,INT(C/256)")
    appendLine("214 POKE 49684,C-256*INT(C/256)")
    appendLine("220 POKE 49680,165")
    appendLine("230 PRINT \"SUM:\";C")
    appendLine("240 PRINT \"D:\";D")
    appendLine("250 POKE 49681,90")
    appendLine("260 END")
    bytes.asList().chunked(12).forEachIndexed { index, chunk ->
        appendLine("${300 + index * 10} DATA " + chunk.joinToString(",") { (it.toInt() and 255).toString() })
    }
}
