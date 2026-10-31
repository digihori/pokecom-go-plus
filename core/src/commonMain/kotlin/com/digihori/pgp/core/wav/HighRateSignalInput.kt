package com.digihori.pgp.core.wav

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Decode high-rate recordings at 16 kHz, retaining coordinates in the original recording. */
internal class HighRateSignalInput(val rate: Int, source: PcmSampleSequence) {
    private val input = source.copySamples()
    private val outputRate = 16000
    private val radius = 32
    val samples: PcmSampleSequence

    init {
        // Rational phases repeat after 160 samples at 44.1 kHz and every sample at 48 kHz.
        val phases = outputRate / gcd(rate, outputRate)
        val kernels = Array(phases) { phase ->
            val fraction = ((phase.toLong() * rate) % outputRate).toDouble() / outputRate
            DoubleArray(radius * 2 + 1) { index ->
                val distance = index - radius - fraction
                val x = 2 * 6500.0 / rate * distance
                val sinc = if (x == 0.0) 1.0 else sin(PI * x) / (PI * x)
                val window = if (kotlin.math.abs(distance) > radius) 0.0 else 0.5 + 0.5 * cos(PI * distance / radius)
                sinc * window
            }.also { weights ->
                val sum = weights.sum()
                weights.indices.forEach { weights[it] /= sum }
            }
        }
        val count = (input.size.toLong() * outputRate / rate).toInt()
        samples = PcmSampleSequence(ShortArray(count) { i ->
            val center = (i.toLong() * rate / outputRate).toInt()
            val weights = kernels[i % phases]
            var value = 0.0
            weights.indices.forEach { tap ->
                val index = (center + tap - radius).coerceIn(0, input.lastIndex)
                value += input[index] * weights[tap]
            }
            value.roundToInt().coerceIn(-32768, 32767).toShort()
        })
    }

    fun position(offset: Int): Int = (offset.toLong() * rate / outputRate).toInt().coerceAtMost(input.size)
    fun range(range: CodecDataRange): CodecDataRange {
        if (range.kind != CodecDataKind.PCM_SAMPLES) return range
        val start = position(range.offset)
        val end = ((range.endExclusive.toLong() * rate + outputRate - 1) / outputRate).toInt().coerceAtMost(input.size)
        return CodecDataRange(range.kind, start, end - start)
    }
    fun mappings(items: List<CodecDataMapping>): List<CodecDataMapping> = items.map {
        it.copy(source = range(it.source), target = range(it.target))
    }
    fun sync(items: List<SignalSyncPoint>): List<SignalSyncPoint> = items.map { it.copy(sampleOffset = position(it.sampleOffset)) }
    fun diagnostics(items: List<CodecDiagnostic>): List<CodecDiagnostic> = items.map {
        CodecDiagnostic(it.severity, it.stage, it.code, it.message, it.range?.let(::range), it.details)
    }
    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
