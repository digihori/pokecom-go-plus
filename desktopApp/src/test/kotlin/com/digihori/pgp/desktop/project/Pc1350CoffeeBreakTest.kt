package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.PgpMemoryDumpWriter
import java.io.File
import java.io.ByteArrayOutputStream
import kotlinx.serialization.json.*
import kotlin.test.*

class Pc1350CoffeeBreakTest {
    @Test fun playsBothChasesAndCoffeeSceneAcrossAllLcdBlocksAndReturns() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "test-data/assembly/pc1350-coffee-break/src/demo.asm").isFile }
        val example = File(root, "test-data/assembly/pc1350-coffee-break")
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(File(example, "src/demo.asm").readText()))
        assertTrue(assembled.image.segments.all { it.startAddress >= 0x5000 && it.startAddress + it.size <= 0x6900 })
        val output = File(root, "desktopApp/build/generated/examples/pc1350-coffee-break.dmp")
        output.parentFile.mkdirs()
        output.writeText(assembled.image.segments.joinToString("") { PgpMemoryDumpWriter.write(it.startAddress, it.copyBytes()) })
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        val art = Json.parseToJsonElement(File(example, "expected-art.json").readText()).jsonObject
        fun columns(name: String) = art.getValue(name).jsonArray.map { it.jsonPrimitive.int }
        val title = columns("TITLE")
        val chaseFooter = columns("CHASE_FOOTER")
        val turnFooter = columns("TURN_FOOTER")
        val coffeeFooter = columns("COFFEE_FOOTER")
        val coffeeTop = columns("COFFEE_TOP")
        val coffeeBottom = columns("COFFEE_BOTTOM")
        val tapes = (1..2).associateWith { scene -> (0..1).associateWith { phase ->
            columns("S${scene}P${phase}TOP") to columns("S${scene}P${phase}BOTTOM")
        } }
        val rom = RomSet(Pc1350RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1350RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000).also {
                byteArrayOf(0x02, 0x50, 0x32, 0x12, 0x5f, 0x02, 0x03, 0xdb.toByte(), 0xdf.toByte(),
                    0x78, 0x60, 0, 0x79, 0, 12).copyInto(it)
            }),
            RomComponent(Pc1350RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x8000)),
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1350RomDefinition.MACHINE_ID, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        repeat(6) { session.step() }
        val before = session.cpuSnapshot()
        val pointersBefore = session.memorySnapshot(0x6f01, 4).copyBytes()
        session.step()
        val frameDrawn = address("FRAME_DRAWN")
        val hold = address("COFFEE_HOLD")
        val done = address("DONE")
        val tone = address("TONE")
        val pitches = mutableListOf<Int>()
        val pcm = ByteArrayOutputStream()
        var audibleSamples = 0
        fun drainAudio() {
            val chunk = session.drainAudioSamples()
            val samples = chunk.copySamples()
            repeat(chunk.frameCount) { index ->
                val sample = if (samples.isEmpty()) 0 else samples[index].toInt()
                if (sample != 0) audibleSamples++
                pcm.write(sample and 255)
                pcm.write((sample ushr 8) and 255)
            }
        }
        val counts = intArrayOf(0, 0)
        var coffeeChecked = false
        var steps = 0
        var cycles = 0L
        fun assertScreen(rows: List<List<Int>>, context: String) {
            val display = session.displaySnapshot()
            assertTrue(display.enabled)
            for (y in 0 until 32) for (x in 0 until 150) {
                val expected = rows[y / 8][x] and (1 shl (y % 8)) != 0
                assertEquals(expected, display.isDotOn(x, y), "$context pixel $x,$y")
            }
        }
        while (steps++ < 1_000_000) {
            val snapshot = session.cpuSnapshot()
            if (snapshot.programCounter == done) break
            if (snapshot.programCounter == tone) pitches += snapshot.copyInternalRam()[2].toInt() and 255
            if (snapshot.programCounter == frameDrawn) {
                drainAudio()
                val registers = snapshot.copyInternalRam()
                val scene = (registers[1].toInt() and 255) + 1
                val frame = counts[scene - 1]++
                val offset = if (scene == 1) frame * 2 else 250 - frame * 2
                val phase = frame % 2
                assertEquals(phase, registers[0].toInt() and 1)
                val tape = tapes.getValue(scene).getValue(phase)
                assertScreen(listOf(title, tape.first.subList(offset, offset + 150), tape.second.subList(offset, offset + 150),
                    if (scene == 1) chaseFooter else turnFooter), "scene $scene frame $frame")
            }
            if (snapshot.programCounter == hold) {
                coffeeChecked = true
                assertScreen(listOf(title, coffeeTop, coffeeBottom, coffeeFooter), "coffee")
            }
            val result = session.step()
            assertIs<ExecutionStatus.Ready>(result.status)
            cycles += result.cycles
        }
        assertContentEquals(intArrayOf(126, 126), counts)
        assertTrue(coffeeChecked)
        assertEquals(done, session.cpuSnapshot().programCounter)
        drainAudio()
        assertEquals(45, pitches.size) // Opening 8 + chase effects 32 + ending 5.
        assertTrue(pitches.toSet().containsAll(setOf(11, 15, 18, 19, 22, 26, 29)))
        assertTrue(audibleSamples > 10_000, "Software buzzer output must produce audible PCM")
        assertTrue(session.audioSnapshot().revision > 5_000, "Expected repeated port-C sound transitions")
        assertTrue(cycles in 2_000_000..8_000_000, "unexpected duration: $cycles cycles")
        println("Coffee break demo duration: ${cycles / 256_000.0} seconds")
        session.step()
        val after = session.cpuSnapshot()
        assertEquals(12, after.programCounter)
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        for (register in listOf(0, 1, 4, 5, 6, 7, 8, 9, 10, 0x5f)) {
            assertEquals(before.copyInternalRam()[register], after.copyInternalRam()[register], "internal register $register")
        }
        assertContentEquals(pointersBefore, session.memorySnapshot(0x6f01, 4).copyBytes())
        assertContentEquals(byteArrayOf(0), session.memorySnapshot(0x783c, 1).copyBytes())
        assertContentEquals(byteArrayOf(0), session.memorySnapshot(0x7e00, 1).copyBytes())
        for (segment in assembled.image.segments) {
            assertContentEquals(segment.copyBytes(), session.memorySnapshot(segment.startAddress, segment.size).copyBytes())
        }
        val samples = pcm.toByteArray()
        val wave = ByteArrayOutputStream()
        fun ascii(value: String) { wave.write(value.toByteArray(Charsets.US_ASCII)) }
        fun le(value: Int, size: Int) { repeat(size) { wave.write((value ushr (it * 8)) and 255) } }
        ascii("RIFF"); le(36 + samples.size, 4); ascii("WAVEfmt "); le(16, 4)
        le(1, 2); le(1, 2); le(22_050, 4); le(44_100, 4); le(2, 2); le(16, 2)
        ascii("data"); le(samples.size, 4); wave.write(samples)
        File(output.parentFile, "pc1350-coffee-break.wav").writeBytes(wave.toByteArray())
    }
}
