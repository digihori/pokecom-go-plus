package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.*
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.*

class Pc1360kKanjiLocalTest {
    @Test
    fun capturedKanjiRomDisplaysRecoveredHiraganaThroughUnmodifiedFirmware() {
        if (System.getenv("PGP_VERIFY_PC1360K_KANJI") != "1") return
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "local-data/roms/pc-1360k/pc1360kmem.bin").isFile }
        val romDir = File(root, "local-data/roms/pc-1360k")
        val analysis = File(root, "local-data/analysis/pc-1360k")
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            File(romDir, "pc1360kmem.bin").readBytes(), File(romDir, "pc1360kbank.bin").readBytes(),
            Pc1360RomDefinition.PC1360K_MACHINE_ID,
            List(4) { File(analysis, "batch-all/read-s$it-0000-ffff.bin").readBytes() },
        )).romSet
        val machine = Pc1360Machine(rom)
        val output = File(analysis, "studio-kanji-four").also { it.mkdirs() }
        val log = StringBuilder()
        var stage = "boot"
        var reads = 0
        var selectorWrites = 0
        val conversionSelectors = mutableSetOf<Int>()
        val conversionGlyphTables = mutableListOf<Int>()
        machine.setMemoryAccessTracing(true)
        fun run(budget: Long) {
            var cycles = 0L
            while (cycles < budget) {
                val pc = machine.cpuState.programCounter
                val op = machine.readMemory(pc)
                val dp = machine.cpuState.dataPointer
                val selector = machine.readMemory(0x3600)
                val result = machine.step(); assertNull(result.stopReason); cycles += result.cycles
                val accesses = machine.drainMemoryAccesses()
                if (op == 0x54 || op == 0x56) {
                    reads++
                    if (stage.contains("conversion") && selector in 2..3) conversionSelectors += selector
                    if (stage == "conversion" && pc == 0x50dd) conversionGlyphTables += dp
                    if (reads < 1024) log.appendLine("$stage READ pc=%04X dp=%04X selector=%d value=%02X".format(pc, dp, selector, accesses.last().value))
                }
                for (access in accesses) if (access.kind == MemoryAccessKind.WRITE && access.address == 0x3600) {
                    selectorWrites++
                    if (selectorWrites < 1024) log.appendLine("$stage SELECT pc=%04X value=%02X".format(pc, access.value))
                }
            }
            machine.buzzerState.drainPcm()
        }
        fun key(key: PocketKey) {
            assertTrue(machine.keyboardState.press(key)); run(100_000)
            machine.keyboardState.release(key); run(100_000)
        }
        fun save(name: String) {
            val dots = machine.displayState.copyDots()
            File(output, "$name-dots.bin").writeBytes(dots)
            File(output, "$name-buffer.bin").writeBytes(ByteArray(32) { machine.readMemory(0xfeb0 + it).toByte() })
            val scale = 5
            val image = BufferedImage(150 * scale, 32 * scale, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until 32 * scale) for (x in 0 until 150 * scale) {
                image.setRGB(x, y, if (dots[(y / scale) * 150 + x / scale].toInt() != 0) 0x263222 else 0xc6cead)
            }
            ImageIO.write(image, "png", File(output, "$name.png"))
            log.appendLine("$name reads=$reads selectors=$selectorWrites pc=%04X mode=%02X".format(machine.cpuState.programCounter, machine.readMemory(0x303c)))
            File(output, "trace.txt").writeText(log.toString())
        }
        run(1_000_000); key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000)
        stage = "width"
        for (char in "WIDTH,2") {
            val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
            keys.forEach { machine.keyboardState.press(it) }; run(80_000)
            keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
        }
        key(PocketKey.ENTER); run(1_000_000)
        assertEquals(18, machine.readMemory(0xfd07)); assertEquals(2, machine.readMemory(0xfd08))
        stage = "kanji"; key(PocketKey.KANJI); run(500_000); save("kanji-mode")
        stage = "hiragana-a"; key(PocketKey.A); run(500_000); save("hiragana-a")
        val glyph = File(analysis, "glyph-selector0-2378.bin").readBytes()
        val expected = glyph.take(15) + 0.toByte() + glyph.drop(15) + 0.toByte()
        assertEquals(expected, (0xfeb0..0xfecf).map { machine.readMemory(it).toByte() }, "The actual firmware must decode the recovered A glyph")
        assertTrue(reads > 0); assertTrue(selectorWrites > 0)
        stage = "conversion"; key(PocketKey.KANJI_CONVERT); run(1_000_000); save("conversion")
        assertTrue(conversionSelectors.containsAll(listOf(2, 3)), "Conversion must access both recovered dictionary selectors")
        assertTrue(0x0390 in conversionGlyphTables, "Conversion must request the glyph table reached for the displayed A kanji candidate")
        stage = "fixed"; key(PocketKey.KANJI_FIX); run(500_000); save("fixed")
        // Exercise a word after a fresh boot as an observation, without hard-coding dictionary candidates.
        machine.coldReset(); stage = "word-boot"
        run(1_000_000); key(PocketKey.Y); key(PocketKey.ENTER); key(PocketKey.CLEAR); run(500_000)
        for (char in "WIDTH,2") {
            val keys = assertNotNull(Pc1360CharacterInput.keySequence(char))
            keys.forEach { machine.keyboardState.press(it) }; run(80_000)
            keys.asReversed().forEach { machine.keyboardState.release(it) }; run(100_000)
        }
        key(PocketKey.ENTER); run(1_000_000); key(PocketKey.KANJI); run(500_000)
        stage = "word-input"
        for (char in "KANJI") key(PocketKey.valueOf(char.toString()))
        run(500_000); save("word-hiragana")
        stage = "word-conversion"; key(PocketKey.KANJI_CONVERT); run(1_000_000); save("word-conversion")
        val firstCandidate = machine.displayState.copyDots()
        stage = "word-next-candidate"; key(PocketKey.KANJI_CONVERT); run(500_000); save("word-next-candidate")
        assertFalse(firstCandidate.contentEquals(machine.displayState.copyDots()), "Repeated conversion must advance to another candidate")
        stage = "word-fixed"; key(PocketKey.KANJI_FIX); run(500_000); save("word-fixed")
        File(output, "summary.txt").writeText("Recovered A glyph matched; A conversion used selectors 2/3 and glyph table 0390. Word input/conversion/candidate cycling/fix captured. reads=$reads selectors=$selectorWrites\n")
    }
}
