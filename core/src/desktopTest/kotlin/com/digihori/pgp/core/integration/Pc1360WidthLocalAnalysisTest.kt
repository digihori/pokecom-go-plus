package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import java.io.File
import com.digihori.pgp.core.debug.Sc61860Disassembler
import kotlin.test.*

/** Explicit local experiment; all firmware-derived output stays in ignored local-data. */
class Pc1360WidthLocalAnalysisTest {
    @Test
    fun compareAlphabetWidthPaths() {
        if (System.getenv("PGP_ANALYZE_PC1360_WIDTH") != "1") return
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "local-data/roms/pc-1360/pc1360mem.bin").isFile }
        val output = File(root, "local-data/analysis/pc-1360-width").also { it.mkdirs() }
        for ((model, wide, kanji) in listOf(Triple("pc-1360", false, false), Triple("pc-1360", true, false), Triple("pc-1360k", true, false), Triple("pc-1360k", true, true))) {
            val prefix = model.replace("-", "")
            val directory = File(root, "local-data/roms/$model")
            val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
                File(directory, "${prefix}mem.bin").readBytes(), File(directory, "${prefix}bank.bin").readBytes(),
                if (model.endsWith("k")) Pc1360RomDefinition.PC1360K_MACHINE_ID else Pc1360RomDefinition.MACHINE_ID,
            )).romSet
            val machine = Pc1360Machine(rom)
            fun fastRun(cycles: Long) { assertNull(machine.runCycles(cycles).stopReason); machine.buzzerState.drainPcm() }
            fun tap(key: PocketKey) {
                assertTrue(machine.keyboardState.press(key)); fastRun(100_000)
                machine.keyboardState.release(key); fastRun(200_000)
            }
            fastRun(1_000_000)
            tap(PocketKey.Y); tap(PocketKey.ENTER); tap(PocketKey.CLEAR); fastRun(1_000_000)
            if (wide) { machine.writeMemory(0xfd07, 18); machine.writeMemory(0xfd08, 2) }
            assertEquals(if (wide) 2 else 4, machine.readMemory(0xfd08))
            assertEquals(if (wide) 18 else 24, machine.readMemory(0xfd07))
            if (kanji) { tap(PocketKey.KANJI); fastRun(1_000_000); assertEquals(0xa8, machine.readMemory(0x303c)) }
            val label = "$model-${if (kanji) "kanji" else if (wide) "wide" else "normal"}"
            fun bytes(start: Int, length: Int) = ByteArray(length) { machine.readMemory(start + it).toByte() }
            val beforeWork = bytes(0x2000, 0x800)
            File(output, "$label-before-dots.bin").writeBytes(machine.displayState.copyDots())
            machine.setMemoryAccessTracing(true)
            File(output, "$label-trace.txt").bufferedWriter().use { log ->
                var sequence = 0
                var remaining = 0
                var writes = 0
                var selectors = 0
                var lcdWrites = 0
                var workWrites = 0
                val lcdValues = mutableListOf<Int>()
                val lcdWriters = mutableMapOf<String, Int>()
                var dataInstructions = 0
                var specialReads = 0
                val recent = ArrayDeque<String>()
                val bufferWriters = mutableMapOf<String, Int>()
                fun run(cycles: Long) {
                    var elapsed = 0L
                    while (elapsed < cycles) {
                        val state = machine.cpuState
                        val pc = state.programCounter
                        val op = machine.readMemory(pc)
                        val bank = machine.selectedRomBank
                        val dp = state.dataPointer
                        val a = state.internalRam[2]; val b = state.internalRam[3]
                        val x = state.internalRam[4] + (state.internalRam[5] shl 8)
                        val y = state.internalRam[6] + (state.internalRam[7] shl 8)
                        val p = state.p; val q = state.q
                        val result = machine.step(); assertNull(result.stopReason); elapsed += result.cycles
                        val accesses = machine.drainMemoryAccesses()
                        val hits = accesses.filter { it.kind == MemoryAccessKind.WRITE && it.address in 0xfeb0..0xfecf }
                        val lcdHits = accesses.filter { access -> access.kind == MemoryAccessKind.WRITE &&
                            Pc1360Display.VRAM_BLOCK_STARTS.any { access.address in it until it + Pc1360Display.VRAM_BLOCK_SIZE } }
                        val line = "%d bank=%d pc=%04x op=%02x dp=%04x a=%02x b=%02x p=%02x q=%02x x=%04x y=%04x ".format(
                            sequence, bank, pc, op, dp, a, b, p, q, x, y,
                        ) + accesses.joinToString(" ") { "%s:%04x=%02x".format(it.kind, it.address, it.value) }
                        if (lcdHits.isNotEmpty()) {
                            if (lcdWrites == 0) { log.appendLine("FIRST_LCD_WRITE"); recent.forEach { log.appendLine(it) }; remaining = 2048 }
                            lcdWrites += lcdHits.size
                            lcdValues.addAll(lcdHits.map { it.value })
                            val site = "%d:%04x".format(bank, pc)
                            lcdWriters[site] = (lcdWriters[site] ?: 0) + lcdHits.size
                            log.appendLine("LCD_WRITE $line")
                        }
                        workWrites += accesses.count { it.kind == MemoryAccessKind.WRITE && it.address in 0x2000..0x27ff }
                        if (hits.isNotEmpty()) {
                            if (writes == 0) { log.appendLine("FIRST_BUFFER_WRITE"); recent.forEach { log.appendLine(it) }; remaining = 1024 }
                            writes += hits.size
                            val site = "%d:%04x".format(bank, pc)
                            bufferWriters[site] = (bufferWriters[site] ?: 0) + hits.size
                            log.appendLine("BUFFER_WRITE $line")
                        }
                        if (accesses.any { it.kind == MemoryAccessKind.WRITE && it.address == 0x3600 }) {
                            selectors++; log.appendLine("SELECTOR $line")
                        }
                        if (op == 0x35) { dataInstructions++; log.appendLine("DATA $line") }
                        if (op == 0x54 || op == 0x56) { specialReads++; log.appendLine("SPECIAL $line") }
                        if (remaining > 0) { log.appendLine(line); remaining-- }
                        recent.addLast(line); if (recent.size > 64) recent.removeFirst()
                        sequence++
                    }
                    machine.buzzerState.drainPcm()
                }
                machine.keyboardState.press(PocketKey.A); run(100_000)
                machine.keyboardState.release(PocketKey.A); run(500_000)
                log.appendLine("SUMMARY $label workWrites=$workWrites lcdWrites=$lcdWrites lcdWriters=$lcdWriters writes=$writes writers=$bufferWriters selectors=$selectors DATA=$dataInstructions special=$specialReads mode=%02x width=%d max=%d".format(
                    machine.readMemory(0x303c), machine.readMemory(0xfd08), machine.readMemory(0xfd07),
                ))
                File(output, "$label-summary.txt").writeText("workWrites=$workWrites lcdWrites=$lcdWrites lcdWriters=$lcdWriters writes=$writes writers=$bufferWriters selectors=$selectors DATA=$dataInstructions special=$specialReads\n")
                assertTrue(lcdWrites > 0, "A must be drawn")
                if (kanji) { assertTrue(writes > 0); assertTrue(selectors > 0); assertTrue(specialReads > 0) }
                else {
                    assertEquals(0, writes, "ASCII path must not write FEB0 in this experiment")
                    assertEquals(0, selectors)
                    assertEquals(0, specialReads)
                    val font = if (wide) 0x4723 else 0x4230
                    val length = if (wide) 16 else 5
                    // All measured ASCII font reads and LCD writes are in normal ROM bank 1.
                    machine.writeMemory(0x3400, 1)
                    assertEquals((font until font + length).map(machine::readMemory), lcdValues.take(length))
                    File(output, "$label-font.bin").writeBytes(bytes(font, length))
                }
            }
            File(output, "$label-after-dots.bin").writeBytes(machine.displayState.copyDots())
            File(output, "$label-buffer.bin").writeBytes(bytes(0xfeb0, 32))
            machine.writeMemory(0x3400, 1)
            File(output, "$label-display-routines.asm.txt").writeText(
                Sc61860Disassembler.renderAssembly(0x51d8, 0x5265, machine::readMemory) + "\n" +
                Sc61860Disassembler.renderAssembly(0x5462, 0x5485, machine::readMemory),
            )
            val afterWork = bytes(0x2000, 0x800)
            File(output, "$label-work-diff.txt").writeText(beforeWork.indices.filter { beforeWork[it] != afterWork[it] }.joinToString("\n") {
                "%04x %02x -> %02x".format(0x2000 + it, beforeWork[it].toInt() and 255, afterWork[it].toInt() and 255)
            })
        }
        assertContentEquals(
            File(output, "pc-1360-wide-font.bin").readBytes(),
            File(output, "pc-1360k-wide-font.bin").readBytes(),
            "Both supplied ROM sets must use the same wide A glyph in this observation",
        )
    }
}
