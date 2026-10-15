package com.digihori.pgp.player.android.audio

import com.digihori.pgp.core.api.AudioPcmSnapshot

/** Worker-owned output. No method may call back into an emulator session. */
internal interface PcmSink {
    /** Non-blocking, may accept fewer samples or zero; negative values are errors. */
    fun write(samples: ShortArray, offset: Int, count: Int): Int
    val pendingFrames: Long
    fun close()
}

/** UI enqueues immutable snapshots; only the worker calls step and touches the sink. */
internal class AudioPump(
    private val openSink: (Int) -> PcmSink,
    private val onFailure: (Long) -> Unit,
) {
    private data class Packet(val pcm: AudioPcmSnapshot, val rate: Int)
    // This lock protects commands and counts only. Never hold it while calling the device.
    private val lock = Any()
    private val queue = ArrayDeque<Packet>()
    private var queuedFrames = 0
    private var currentRemaining = 0
    private var generation = 0L
    private var failed = false
    // All fields below are worker-owned.
    private var workerGeneration = -1L
    private var sink: PcmSink? = null
    private var rate = 0
    private var current: ShortArray? = null
    private var offset = 0
    private var silent = false
    private var silenceWritten = 0L

    fun offer(pcm: AudioPcmSnapshot, playbackRate: Int) = synchronized(lock) {
        if (failed || pcm.frameCount == 0) return@synchronized generation
        require(playbackRate > 0)
        // At most 100 ms unsubmitted PCM, including the worker's partial packet.
        if (queuedFrames + currentRemaining + pcm.frameCount > playbackRate / 10) invalidate()
        if (pcm.frameCount <= playbackRate / 10) {
            queue.addLast(Packet(pcm, playbackRate))
            queuedFrames += pcm.frameCount
        }
        generation
    }

    fun stop(retry: Boolean = false) = synchronized(lock) {
        invalidate()
        if (retry) failed = false
        generation
    }

    private fun invalidate() {
        generation++
        queue.clear()
        queuedFrames = 0
        currentRemaining = 0
    }

    /** Worker-only: wait indefinitely when no output or command remains. */
    fun hasWork(): Boolean = sink != null || current != null || synchronized(lock) {
        queue.isNotEmpty() || workerGeneration != generation
    }

    /** One bounded non-blocking write. Stop requests take priority over queued PCM. */
    fun step() {
        val attemptedGeneration = synchronized(lock) { generation }
        try {
            if (attemptedGeneration != workerGeneration) {
                closeSink()
                current = null
                silent = false
                workerGeneration = attemptedGeneration
            }
            if (current == null) {
                val packet = synchronized(lock) {
                    if (generation != workerGeneration) return
                    if (queue.isEmpty()) null else queue.removeFirst().also {
                        queuedFrames -= it.pcm.frameCount
                        currentRemaining = it.pcm.frameCount
                    }
                }
                if (packet == null) {
                    finishSilence()
                    return
                }
                if (rate != packet.rate) closeSink()
                rate = packet.rate
                silent = packet.pcm.sampleCount == 0
                if (!silent) silenceWritten = 0
                if (silent) finishSilence()
                if (silent && sink == null) {
                    publishRemaining(0)
                    return
                }
                if (sink == null) sink = openSink(rate)
                current = if (silent) ShortArray(packet.pcm.frameCount) else packet.pcm.copySamples()
                offset = 0
            }
            // A stop may have arrived during device initialization. Never send that old packet.
            if (synchronized(lock) { generation != workerGeneration }) return
            val samples = checkNotNull(current)
            val requested = minOf(256, samples.size - offset)
            val written = checkNotNull(sink).write(samples, offset, requested)
            check(written in 0..requested) { "PCM output failed" }
            if (silent) silenceWritten += written
            offset += written
            publishRemaining(samples.size - offset)
            if (offset == samples.size) current = null
        } catch (_: Exception) {
            runCatching { closeSink() }
            current = null
            synchronized(lock) {
                if (generation == attemptedGeneration) {
                    queue.clear()
                    queuedFrames = 0
                    currentRemaining = 0
                    failed = true
                }
            }
            onFailure(attemptedGeneration)
        }
    }

    private fun publishRemaining(count: Int) = synchronized(lock) {
        if (generation == workerGeneration) currentRemaining = count
    }

    private fun finishSilence() {
        // Queued zeros must not keep a completed tone alive forever.
        if (silent && sink != null && checkNotNull(sink).pendingFrames <= silenceWritten) closeSink()
    }

    private fun closeSink() {
        val previous = sink
        sink = null
        silenceWritten = 0
        previous?.close()
    }
}
