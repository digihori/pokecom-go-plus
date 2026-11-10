package com.digihori.pgp.desktop.audio

import com.digihori.pgp.core.api.AudioPcmSnapshot
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

internal class DesktopAudioPlayer(
    private val sinkFactory: PcmSinkFactory = JavaSoundPcmSinkFactory,
) : AutoCloseable {
    private var sink: PcmSink? = null
    private var sampleRate: Int? = null
    private var needsDrain = false

    fun write(snapshot: AudioPcmSnapshot): Result<Unit> {
        val samples = snapshot.copySamples()
        if (samples.isEmpty() || samples.all { it == 0.toShort() }) {
            return runCatching<Unit> {
                if (needsDrain) {
                    sink?.finish()
                    // The OS may still be presenting buffered audio after Java Sound drains.
                    needsDrain = false
                }
            }.onFailure { stop() }
        }
        return runCatching<Unit> {
            if (sink == null || sampleRate != snapshot.sampleRate) {
                stop()
                sink = sinkFactory.create(snapshot.sampleRate)
                sampleRate = snapshot.sampleRate
            }
            sink?.write(samples.toLittleEndianBytes())
            needsDrain = true
        }.onFailure { stop() }
    }

    fun stop() {
        sink?.close()
        sink = null
        sampleRate = null
        needsDrain = false
    }

    override fun close() = stop()
}

internal fun interface PcmSinkFactory {
    fun create(sampleRate: Int): PcmSink
}

internal interface PcmSink : AutoCloseable {
    fun write(bytes: ByteArray)
    fun finish() {}
}

private object JavaSoundPcmSinkFactory : PcmSinkFactory {
    override fun create(sampleRate: Int): PcmSink {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        try {
            line.open(format)
            line.start()
        } catch (error: Exception) {
            line.close()
            throw error
        }
        return JavaSoundPcmSink(line)
    }
}

private class JavaSoundPcmSink(private val line: SourceDataLine) : PcmSink {
    private val closed = AtomicBoolean(false)
    private val diagnostics = java.lang.Boolean.getBoolean("pgp.audio.trace")
    private var bytesWritten = 0L

    override fun write(bytes: ByteArray) {
        if (closed.get()) return
        val written = line.write(bytes, 0, bytes.size)
        if (diagnostics && (bytesWritten == 0L || written != bytes.size)) {
            System.err.println("StudioAudio: format=${line.format}, buffer=${line.bufferSize}, " +
                "write=$written/${bytes.size}, frame=${line.longFramePosition}, active=${line.isActive}")
        }
        bytesWritten += written
        check(written == bytes.size || closed.get()) {
            "Java Sound accepted $written of ${bytes.size} PCM bytes"
        }
    }

    override fun finish() {
        if (closed.get()) return
        line.drain()
        if (diagnostics) {
            System.err.println("StudioAudio: drained bytes=$bytesWritten, frame=${line.longFramePosition}")
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            line.stop()
            line.flush()
            line.close()
        }
    }
}

private fun ShortArray.toLittleEndianBytes(): ByteArray = ByteArray(size * 2).also { bytes ->
    forEachIndexed { index, sample ->
        val value = sample.toInt()
        bytes[index * 2] = value.toByte()
        bytes[index * 2 + 1] = (value ushr 8).toByte()
    }
}
