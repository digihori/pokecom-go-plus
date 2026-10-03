package com.digihori.pgp.desktop.audio

import com.digihori.pgp.core.api.AudioSnapshot
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.math.abs

internal class DesktopAudioPlayer(
    private val clipFactory: ToneClipFactory = JavaSoundToneClipFactory,
) : AutoCloseable {
    private var activeClip: ToneClip? = null
    private var appliedRevision: Long = Long.MIN_VALUE

    fun update(snapshot: AudioSnapshot): Result<Unit> {
        if (snapshot.revision == appliedRevision) return Result.success(Unit)
        appliedRevision = snapshot.revision
        activeClip?.close()
        activeClip = null

        if (snapshot.frequencyHz == 0) return Result.success(Unit)
        return runCatching {
            activeClip = clipFactory.create(snapshot.frequencyHz).also(ToneClip::startLooping)
        }
    }

    /** Stops host sound and forces the current Core tone to be reconsidered on the next update. */
    fun stop() {
        activeClip?.close()
        activeClip = null
        appliedRevision = Long.MIN_VALUE
    }

    override fun close() = stop()
}

internal fun interface ToneClipFactory {
    fun create(frequencyHz: Int): ToneClip
}

internal interface ToneClip : AutoCloseable {
    fun startLooping()
}

private object JavaSoundToneClipFactory : ToneClipFactory {
    override fun create(frequencyHz: Int): ToneClip {
        val pcm = squareWavePcm(frequencyHz)
        val clip = AudioSystem.getClip()
        clip.open(AUDIO_FORMAT, pcm, 0, pcm.size)
        return JavaSoundToneClip(clip)
    }
}

private class JavaSoundToneClip(private val clip: Clip) : ToneClip {
    private val closed = AtomicBoolean(false)

    override fun startLooping() {
        clip.loop(Clip.LOOP_CONTINUOUSLY)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            clip.stop()
            clip.close()
        }
    }
}

internal fun squareWavePcm(frequencyHz: Int): ByteArray {
    require(frequencyHz > 0) { "frequencyHz must be greater than zero" }
    require(frequencyHz <= SAMPLE_RATE / 2) { "frequencyHz exceeds the Nyquist limit" }

    val frameCount = SAMPLE_RATE / greatestCommonDivisor(SAMPLE_RATE, frequencyHz)
    val bytes = ByteArray(frameCount * BYTES_PER_SAMPLE)
    var phase = 0
    repeat(frameCount) { frame ->
        val sample = if (phase < SAMPLE_RATE / 2) AMPLITUDE else -AMPLITUDE
        bytes[frame * 2] = sample.toByte()
        bytes[frame * 2 + 1] = (sample shr 8).toByte()
        phase = (phase + frequencyHz) % SAMPLE_RATE
    }
    return bytes
}

private tailrec fun greatestCommonDivisor(a: Int, b: Int): Int =
    if (b == 0) abs(a) else greatestCommonDivisor(b, a % b)

private const val SAMPLE_RATE: Int = 44_100
private const val BYTES_PER_SAMPLE: Int = 2
private const val AMPLITUDE: Int = 4_096
private val AUDIO_FORMAT: AudioFormat = AudioFormat(
    SAMPLE_RATE.toFloat(),
    BYTES_PER_SAMPLE * 8,
    1,
    true,
    false,
)
