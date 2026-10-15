package com.digihori.pgp.player.android.audio

import com.digihori.pgp.core.api.AudioPcmSnapshot
import kotlin.test.*

class AudioPumpTest {
    private class Sink : PcmSink {
        val samples = mutableListOf<Short>()
        var limit = 256
        var pending = 0L
        var closed = false
        override val pendingFrames get() = pending
        override fun write(samples: ShortArray, offset: Int, count: Int): Int {
            if (limit < 0) return -1
            val size = minOf(count, limit)
            this.samples.addAll(samples.slice(offset until offset + size))
            pending += size
            return size
        }
        override fun close() { closed = true }
    }
    private fun packet(vararg samples: Short) = AudioPcmSnapshot(22050, samples)

    @Test fun partialAndZeroWritesKeepOrderAndSilentGaps() {
        val sink = Sink()
        val pump = AudioPump({ sink }, { fail("unexpected failure") })
        pump.offer(packet(1, 2, 3), 22050)
        pump.offer(AudioPcmSnapshot(22050, shortArrayOf(), 3), 22050)
        pump.offer(packet(4, 5), 22050)
        sink.limit = 0
        pump.step()
        assertTrue(sink.samples.isEmpty())
        sink.limit = 2
        repeat(8) { pump.step() }
        assertEquals(listOf<Short>(1, 2, 3, 0, 0, 0, 4, 5), sink.samples)
        assertFalse(sink.closed) // queued tail still playing
        pump.offer(AudioPcmSnapshot(22050, shortArrayOf(), 1), 22050)
        sink.pending = 0
        pump.step()
        assertTrue(sink.closed)
    }

    @Test fun stopFlushesPartialPacketAndQueuedOldGeneration() {
        val sinks = mutableListOf<Sink>()
        val pump = AudioPump({ Sink().also { sinks += it; it.limit = 1 } }, { fail("unexpected failure") })
        pump.offer(packet(1, 2, 3), 22050)
        pump.offer(packet(4), 22050)
        pump.step()
        pump.stop()
        pump.offer(packet(9), 22050)
        pump.step()
        assertTrue(sinks[0].closed)
        assertEquals(listOf<Short>(1), sinks[0].samples)
        assertEquals(listOf<Short>(9), sinks[1].samples)
    }

    @Test fun overloadResynchronizesInsteadOfAccumulatingOldAudio() {
        val sink = Sink()
        val pump = AudioPump({ sink }, { fail("unexpected failure") })
        pump.offer(AudioPcmSnapshot(22050, ShortArray(2000) { 1 }), 22050)
        pump.offer(packet(9), 22050)
        pump.offer(AudioPcmSnapshot(22050, ShortArray(300) { 2 }), 22050)
        repeat(3) { pump.step() }
        assertEquals(300, sink.samples.size)
        assertTrue(sink.samples.all { it == 2.toShort() })
    }

    @Test fun failuresBlockRetryAndReportOnceUntilExplicitRestart() {
        var opens = 0
        var failures = 0
        val pump = AudioPump({ opens++; error("device unavailable") }, { failures++ })
        pump.offer(packet(1), 22050)
        pump.step()
        repeat(3) { pump.offer(packet(2), 22050); pump.step() }
        assertEquals(1, opens)
        assertEquals(1, failures)
        pump.stop(retry = true)
        pump.offer(packet(3), 22050)
        pump.step()
        assertEquals(2, failures)
    }

    @Test fun writeFailureClosesDeviceAndRateChangeRecreatesIt() {
        val rates = mutableListOf<Int>()
        val sinks = mutableListOf<Sink>()
        var failures = 0
        val pump = AudioPump({ rate -> rates += rate; Sink().also { sinks += it } }, { failures++ })
        pump.offer(packet(1), 22050)
        pump.step()
        pump.stop(retry = true)
        pump.offer(packet(2), 26460)
        pump.step()
        assertEquals(listOf(22050, 26460), rates)
        assertTrue(sinks[0].closed)
        sinks[1].limit = -1
        pump.offer(packet(3), 26460)
        pump.step()
        assertEquals(1, failures)
        assertTrue(sinks[1].closed)
    }

    @Test fun continuousToneKeepsDeviceEvenWhenHostQueueTemporarilyEmpties() {
        var opens = 0
        val sink = Sink()
        val pump = AudioPump({ opens++; sink }, { fail("unexpected failure") })
        pump.offer(packet(1), 22050)
        pump.step()
        sink.pending = 0
        repeat(10) { pump.step() }
        assertFalse(sink.closed)
        pump.offer(packet(2), 22050)
        pump.step()
        assertEquals(1, opens)
        assertEquals(listOf<Short>(1, 2), sink.samples)
    }

    @Test fun staleWriteFailureCannotDropNewGeneration() {
        lateinit var pump: AudioPump
        val sink = Sink()
        var first = true
        var failedGeneration = -1L
        pump = AudioPump({
            if (!first) sink else object : PcmSink {
                override val pendingFrames = 0L
                override fun write(samples: ShortArray, offset: Int, count: Int): Int {
                    first = false
                    pump.stop(retry = true)
                    pump.offer(packet(9), 22050)
                    error("old device failed")
                }
                override fun close() {}
            }
        }, { failedGeneration = it })
        val old = pump.offer(packet(1), 22050)
        pump.step()
        assertEquals(old, failedGeneration)
        pump.step()
        assertEquals(listOf<Short>(9), sink.samples)
    }

    @Test fun slowDeviceInitializationNeverBlocksUiStopOrEnqueue() {
        val entered = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val sinks = mutableListOf<Sink>()
        val pump = AudioPump({
            entered.countDown()
            check(release.await(3, java.util.concurrent.TimeUnit.SECONDS))
            Sink().also { sinks += it }
        }, { fail("unexpected failure") })
        pump.offer(packet(1), 22050)
        val worker = kotlin.concurrent.thread { pump.step() }
        val ui = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS))
            ui.submit<Long> { pump.stop(retry = true); pump.offer(packet(9), 22050) }
                .get(1, java.util.concurrent.TimeUnit.SECONDS)
        } finally {
            release.countDown()
            worker.join(2000)
            ui.shutdownNow()
        }
        assertTrue(sinks[0].samples.isEmpty())
        pump.step()
        assertTrue(sinks[0].closed)
        assertEquals(listOf<Short>(9), sinks[1].samples)
    }

    @Test fun silenceAloneNeverOpensDeviceAndOversizedPacketIsDropped() {
        var opens = 0
        val pump = AudioPump({ opens++; Sink() }, { fail("unexpected failure") })
        pump.offer(AudioPcmSnapshot(22050, shortArrayOf(), 100), 22050)
        pump.step()
        pump.offer(AudioPcmSnapshot(22050, ShortArray(3000) { 1 }), 22050)
        pump.step()
        assertEquals(0, opens)
    }
}
