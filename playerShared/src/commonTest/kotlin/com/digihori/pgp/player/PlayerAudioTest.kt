package com.digihori.pgp.player

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import com.digihori.pgp.core.rom.*
import kotlin.test.*

class PlayerAudioTest {
    @Test
    fun bothMachinesDeliverOscillatorsAndConsumeEachTick() {
        for (model in listOf("pc-1245", "pc-1401")) for (control in listOf(0x20, 0x30)) {
            val runner = runner(model, control)
            runner.run(0)
            val audio = assertNotNull(runner.tick(10_000_000).audio)
            assertEquals(22_050, audio.sampleRate)
            assertTrue(audio.frameCount in 219..222)
            assertEquals(audio.frameCount, audio.sampleCount)
            assertTrue(audio.copySamples().any { it > 0 })
            assertTrue(audio.copySamples().any { it < 0 })
            // Discard the first packet (mute); the next packet contains only the next 10 ms.
            repeat(100) { index ->
                val next = assertNotNull(runner.tick((index + 2L) * 10_000_000).audio)
                assertTrue(next.frameCount in 219..222)
            }
            assertNull(runner.tick(1_010_000_000).audio)
            runner.pause()
            assertNull(runner.tick(2_000_000_000).audio)
            runner.reset()
            runner.run(2_000_000_000)
            assertNull(runner.tick(2_000_000_000).audio)
            assertTrue(assertNotNull(runner.tick(2_010_000_000).audio).frameCount in 219..222)
        }
    }

    @Test
    fun directOutputProducesAudioWithoutAnOscillatorOnBothMachines() {
        val program = buildList<Byte> {
            addAll(listOf(0x12, 0x5f, 0x02, 0x50, 0xdb, 0xdf).map(Int::toByte))
            repeat(48) { add(0x4d) }
            addAll(listOf(0x12, 0x5f, 0x02, 0x40, 0xdb, 0xdf).map(Int::toByte))
            repeat(48) { add(0x4d) }
            addAll(listOf(0x79, 0x00, 0x00).map(Int::toByte))
        }.toByteArray()
        for (model in listOf("pc-1245", "pc-1401")) {
            val runner = runner(model, 0, program)
            runner.run(0)
            val samples = assertNotNull(runner.tick(10_000_000).audio).copySamples()
            assertTrue(samples.any { it > 0 })
            assertTrue(samples.any { it < 0 })
        }
    }

    @Test
    fun silentFramesRetainDurationAndSnapshotsAreImmutable() {
        for (model in listOf("pc-1245", "pc-1401")) {
            val runner = runner(model, 0)
            runner.run(0)
            val silent = assertNotNull(runner.tick(10_000_000).audio)
            assertEquals(0, silent.sampleCount)
            assertTrue(silent.frameCount in 219..222)
        }
        val samples = shortArrayOf(1, 2)
        val packet = com.digihori.pgp.core.api.AudioPcmSnapshot(22_050, samples)
        samples[0] = 9
        packet.copySamples()[1] = 9
        assertContentEquals(shortArrayOf(1, 2), packet.copySamples())
    }

    private fun runner(model: String, control: Int, program: ByteArray? = null): PlayerRunner {
        val is1401 = model == "pc-1401"
        val internal = ByteArray(if (is1401) Pc1401RomDefinition.INTERNAL_SIZE else Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d }
        // Independently authored: LIP 5f; LIA control; EXAM; OUTC; NOPW padding.
        (program ?: byteArrayOf(0x12, 0x5f, 0x02, control.toByte(), 0xdb.toByte(), 0xdf.toByte())).copyInto(internal)
        val external = ByteArray(if (is1401) Pc1401RomDefinition.EXTERNAL_SIZE else Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d }
        val rom = RomSet(MachineId(model), listOf(
            RomComponent(if (is1401) Pc1401RomDefinition.INTERNAL_ID else Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
            RomComponent(if (is1401) Pc1401RomDefinition.EXTERNAL_ID else Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
        ))
        return PlayerRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(rom.machineId, rom)).session)
    }
}
