package com.digihori.pgp.desktop.audio

import com.digihori.pgp.core.api.AudioPcmSnapshot
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopAudioPlayerTest {
    @Test
    fun streamsLittleEndianPcmAndReusesTheSink() {
        val factory = FakeSinkFactory()
        val player = DesktopAudioPlayer(factory)

        player.write(AudioPcmSnapshot(22_050, shortArrayOf(0x1234, (-2).toShort()))).getOrThrow()
        player.write(AudioPcmSnapshot(22_050, shortArrayOf(1))).getOrThrow()

        assertEquals(listOf(22_050), factory.sampleRates)
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0xfe.toByte(), 0xff.toByte()),
            factory.sinks.single().writes.first(),
        )
        assertContentEquals(byteArrayOf(1, 0), factory.sinks.single().writes.last())
    }

    @Test
    fun recreatesTheSinkForAnotherSampleRateAndStopClosesIt() {
        val factory = FakeSinkFactory()
        val player = DesktopAudioPlayer(factory)

        player.write(AudioPcmSnapshot(22_050, shortArrayOf(1))).getOrThrow()
        player.write(AudioPcmSnapshot(44_100, shortArrayOf(2))).getOrThrow()
        player.stop()

        assertEquals(listOf(22_050, 44_100), factory.sampleRates)
        assertTrue(factory.sinks.all { it.closed })
    }

    @Test
    fun ignoresEmptyFramesAndReportsDeviceFailure() {
        val unused = DesktopAudioPlayer { error("must not open") }
        unused.write(AudioPcmSnapshot(22_050, shortArrayOf())).getOrThrow()

        val failing = DesktopAudioPlayer { error("no audio device") }
        assertTrue(failing.write(AudioPcmSnapshot(22_050, shortArrayOf(1))).isFailure)
    }

    @Test
    fun closesTheAudioLineAsSoonAsTheToneBecomesInactive() {
        val factory = FakeSinkFactory()
        val player = DesktopAudioPlayer(factory)

        player.update(AudioPcmSnapshot(22_050, shortArrayOf(1, 2)), toneActive = true).getOrThrow()
        player.update(AudioPcmSnapshot(22_050, shortArrayOf(3)), toneActive = false).getOrThrow()
        player.update(AudioPcmSnapshot(22_050, shortArrayOf()), toneActive = false).getOrThrow()

        assertEquals(1, factory.sinks.size)
        assertTrue(factory.sinks.single().closed)
        assertEquals(1, factory.sinks.single().writes.size)
    }

    private class FakeSinkFactory : PcmSinkFactory {
        val sampleRates = mutableListOf<Int>()
        val sinks = mutableListOf<FakeSink>()
        override fun create(sampleRate: Int): PcmSink {
            sampleRates += sampleRate
            return FakeSink().also(sinks::add)
        }
    }

    private class FakeSink : PcmSink {
        val writes = mutableListOf<ByteArray>()
        var closed = false
        override fun write(bytes: ByteArray) { writes += bytes.copyOf() }
        override fun close() { closed = true }
    }
}
