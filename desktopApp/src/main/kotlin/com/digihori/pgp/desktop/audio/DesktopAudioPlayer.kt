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

    fun write(snapshot: AudioPcmSnapshot): Result<Unit> {
        val samples = snapshot.copySamples()
        if (samples.isEmpty() || samples.all { it == 0.toShort() }) {
            return runCatching<Unit> {
                sink?.finish()
                stop()
            }.onFailure { stop() }
        }
        return runCatching<Unit> {
            if (sink == null || sampleRate != snapshot.sampleRate) {
                sink?.close()
                sink = sinkFactory.create(snapshot.sampleRate)
                sampleRate = snapshot.sampleRate
            }
            sink?.write(samples.toLittleEndianBytes())
        }.onFailure { stop() }
    }

    fun stop() {
        sink?.close()
        sink = null
        sampleRate = null
    }

    override fun close() = stop()
}

internal fun interface PcmSinkFactory {
    fun create(sampleRate: Int): PcmSink
}

internal interface PcmSink : AutoCloseable {
    fun write(bytes: ByteArray)
    fun finish() = close()
}

private object JavaSoundPcmSinkFactory : PcmSinkFactory {
    override fun create(sampleRate: Int): PcmSink {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format)
        line.start()
        return JavaSoundPcmSink(line)
    }
}

private class JavaSoundPcmSink(private val line: SourceDataLine) : PcmSink {
    private val closed = AtomicBoolean(false)

    override fun write(bytes: ByteArray) {
        if (!closed.get()) line.write(bytes, 0, bytes.size)
    }

    override fun finish() {
        if (!closed.get()) line.drain()
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
