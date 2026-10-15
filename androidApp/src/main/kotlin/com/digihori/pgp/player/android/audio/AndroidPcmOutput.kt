package com.digihori.pgp.player.android.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.digihori.pgp.core.api.AudioPcmSnapshot

/** Dedicated output thread receives only copied PCM. UI never waits for device writes. */
internal class AndroidPcmOutput(onFailure: (Long) -> Unit) : AutoCloseable {
    private val pump = AudioPump(::TrackSink, onFailure)
    @Volatile private var closed = false
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private val wake = Object()
    private val worker = Thread({
        try {
            while (!closed) {
                pump.step()
                synchronized(wake) { if (!closed) { if (pump.hasWork()) wake.wait(2) else wake.wait() } }
            }
        } finally {
            pump.stop()
            pump.step()
        }
    }, "Player PCM").apply { start() }

    fun offer(pcm: AudioPcmSnapshot, playbackRate: Int): Long {
        val generation = pump.offer(pcm, playbackRate)
        signal()
        return generation
    }
    fun stop(retry: Boolean = false): Long { val generation = pump.stop(retry); signal(); return generation }
    private fun signal() = synchronized(wake) { wake.notifyAll() }
    override fun close() { closed = true; signal() }
}

private class TrackSink(rate: Int) : PcmSink {
    private val track: AudioTrack
    private var written = 0L
    private var lastHead = 0L
    private var wraps = 0L
    init {
        val minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "Unsupported audio rate" }
        // Request 50 ms or the device minimum. Refuse an unbounded device latency.
        check(minimum <= rate * 2 / 10) { "Audio buffer exceeds 100 ms" }
        track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(minimum, rate / 20 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        try {
            check(track.state == AudioTrack.STATE_INITIALIZED)
            track.play()
        } catch (error: Exception) { track.release(); throw error }
    }
    override fun write(samples: ShortArray, offset: Int, count: Int): Int =
        track.write(samples, offset, count, AudioTrack.WRITE_NON_BLOCKING).also { if (it > 0) written += it }
    override val pendingFrames: Long get() {
        val head = track.playbackHeadPosition.toLong() and 0xffffffffL
        if (head < lastHead) wraps += 1L shl 32
        lastHead = head
        return (written - wraps - head).coerceAtLeast(0)
    }
    override fun close() {
        try { track.pause(); track.flush() } finally { track.release() }
    }
}
