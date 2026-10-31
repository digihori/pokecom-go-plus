package com.digihori.pgp.core.wav

import kotlin.math.abs
import kotlin.math.roundToInt

/** Shared S1/S2 physical decoder. Payload checksums and physical BASIC trailers belong to callers. */
public object S1SignalDecoder {
    public fun decode(input: NormalizedPcmData): CodecResult<S1SignalDecoding> =
        decode(input.sampleRate, input.samples)

    public fun decode(sampleRate: Int, samples: PcmSampleSequence): CodecResult<S1SignalDecoding> {
        if (sampleRate == 44100 || sampleRate == 48000) {
            val input = HighRateSignalInput(sampleRate, samples)
            return when (val result = decodeSignal(16000, input.samples, smoothCarrier = true)) {
                is CodecResult.Failure -> CodecResult.Failure(input.diagnostics(result.diagnostics))
                is CodecResult.Success -> CodecResult.Success(S1SignalDecoding(
                    result.value.rawBytes, input.mappings(result.value.rawByteMappings), input.sync(result.value.syncPoints),
                    sampleRate, samples.size), input.diagnostics(result.diagnostics))
            }
        }
        return decodeSignal(sampleRate, samples, smoothCarrier = false)
    }

    private fun decodeSignal(sampleRate: Int, samples: PcmSampleSequence, smoothCarrier: Boolean): CodecResult<S1SignalDecoding> {
        if (sampleRate !in setOf(8000, 16000, 24000, 32000)) return CodecResult.Failure(listOf(diagnostic(
            CodecDiagnosticSeverity.ERROR, CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            "S1/S2 signal requires a sample rate of 8000, 16000, 24000, 32000, 44100, or 48000 Hz",
        )))
        val pcm = samples.copySamples()
        val unit = sampleRate / 500.0
        val tap = (sampleRate / 4000.0).roundToInt()
        val raw = mutableListOf<Byte>()
        val mappings = mutableListOf<CodecDataMapping>()
        val sync = mutableListOf<SignalSyncPoint>()
        val diagnostics = mutableListOf<CodecDiagnostic>()
        var cursor = tap
        var leader = 0
        var locked = false
        var lastSignificantSample = -tap

        // A carrier must precede the first frame (or recovery after damage). Silence cannot qualify.
        while (cursor < pcm.size) {
            if (!locked) {
                val difference = if (smoothCarrier) {
                    val from = maxOf(tap, cursor - 2 * tap + 1)
                    (from..cursor).sumOf { abs(pcm[it].toInt() - pcm[it - tap].toInt()) } / (cursor - from + 1)
                } else abs(pcm[cursor].toInt() - pcm[cursor - tap].toInt())
                if (abs(pcm[cursor].toInt()) >= 1024) lastSignificantSample = cursor
                // A sine carrier crosses zero: require silence over a whole carrier period.
                if (cursor - lastSignificantSample > tap) {
                    leader = 0
                } else if (difference <= 5120) {
                    leader++
                } else {
                    if (leader >= sampleRate / 8) {
                        cursor = maxOf(0, cursor - tap / 2)
                        locked = true
                    }
                    leader = 0
                }
                if (!locked) { cursor++; continue }
            }

            val start = cursor
            // Recordings may shorten the high-nibble stop carrier to two units.
            // Reacquire every byte at its actual W1 -> W0 edge instead of accumulating clock error.
            val highOffset = 6
            val minimumUnits = highOffset + 7
            val maximumUnits = highOffset + 10
            val next = (minimumUnits..maximumUnits).firstNotNullOfOrNull { units ->
                nextStart(pcm, start + (units * unit).roundToInt(), tap, unit, sampleRate)?.let { units to it }
            }
            val tailUnits = (minimumUnits..maximumUnits).firstOrNull { units ->
                val tail = start + (units * unit).roundToInt()
                tail + (3 * unit).roundToInt() <= pcm.size &&
                    (0..2).all { symbol(pcm, tail, it, unit, sampleRate) < 0 }
            }
            val frameUnits = next?.first ?: tailUnits ?: maximumUnits
            val end = next?.second ?: (start + (frameUnits * unit).roundToInt())
            // Ignore only the short carrier-to-silence/filter transient at the frame edge.
            // A truncated start unit with continuing carrier still receives a diagnostic.
            val quietStart = minOf(pcm.size, start + 2 * tap)
            val quietFrame = (quietStart until minOf(end, pcm.size)).none { abs(pcm[it].toInt()) >= 1024 }
            if (quietFrame && (minOf(end, pcm.size) until pcm.size).none { abs(pcm[it].toInt()) >= 1024 }) break
            // The recorder may end with a large DC settling transient rather than zero PCM.
            // It is a normal tail only when no 2/4 kHz carrier remains, including later leaders.
            if (raw.isNotEmpty() && start + unit.roundToInt() <= pcm.size &&
                (start..pcm.size - unit.roundToInt() step unit.roundToInt()).none {
                    symbol(pcm, it, 0, unit, sampleRate) >= 0
                }) break
            if (end > pcm.size) {
                diagnostics += diagnostic(CodecDiagnosticSeverity.WARNING, CodecDiagnosticCode.TRUNCATED_INPUT,
                    "S1/S2 signal ends with an incomplete raw byte", start, pcm.size - start)
                break
            }
            val bits = IntArray(frameUnits) { symbol(pcm, start, it, unit, sampleRate) }
            val framingValid = bits[0] == 0 && (5 until highOffset).all { bits[it] == 1 } && bits[highOffset] == 0 &&
                (highOffset + 5 until frameUnits).all { bits[it] == 1 } && bits.none { it < 0 }
            if (!framingValid) {
                // A quiet tail is normal. A damaged frame is never emitted as a fabricated byte.
                // A trailing W1 carrier may decay into silence/DC without starting a byte.
                // Report damaged framing when a W0/start is actually present.
                val laterFrame = (end..pcm.size - (16 * unit).roundToInt() step tap).any {
                    framedStart(pcm, it, unit, sampleRate)
                }
                if (bits.any { it == 0 } || laterFrame) {
                    diagnostics += diagnostic(CodecDiagnosticSeverity.WARNING, CodecDiagnosticCode.SYNCHRONIZATION_LOST,
                        "S1/S2 byte framing or carrier was lost; waiting for a new leader", start, end - start)
                }
                locked = false
                leader = 0
                cursor++
                continue
            }
            var value = 0
            repeat(4) { bit ->
                value = value or (bits[bit + 1] shl bit) or (bits[bit + highOffset + 1] shl (bit + 4))
            }
            val index = raw.size
            raw += value.toByte()
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.PCM_SAMPLES, start, end - start),
                CodecDataRange(CodecDataKind.RAW_BYTES, index, 1), "decoded-raw-byte")
            sync += SignalSyncPoint(start, index, SignalNibble.LOW)
            sync += SignalSyncPoint(start + (highOffset * unit).roundToInt(), index, SignalNibble.HIGH)
            cursor = end
        }
        if (raw.isEmpty()) return CodecResult.Failure(diagnostics + diagnostic(
            CodecDiagnosticSeverity.ERROR, CodecDiagnosticCode.SYNCHRONIZATION_LOST,
            "No complete S1/S2 raw byte could be decoded", 0, pcm.size,
        ))
        return CodecResult.Success(S1SignalDecoding(RawTransferByteSequence(raw.toByteArray()), mappings,
            sync, sampleRate, pcm.size), diagnostics)
    }

    /** Bounded search over one quarter-unit; does not scan arbitrarily past damaged framing. */
    private fun nextStart(pcm: ShortArray, expected: Int, tap: Int, unit: Double, rate: Int): Int? {
        val radius = (unit / 4).roundToInt()
        fun difference(at: Int): Int = (at - 2 * tap + 1..at).sumOf {
            abs(pcm[it].toInt() - pcm[it - tap].toInt())
        } / (2 * tap)
        return (maxOf(3 * tap, expected - radius + tap / 2)..minOf(pcm.lastIndex, expected + radius + tap / 2))
            .filter { difference(it) > 5120 && difference(it - 1) <= 5120 }
            .filter {
                val candidate = it - tap / 2
                framedStart(pcm, candidate, unit, rate)
            }
            .minByOrNull { abs(it - tap / 2 - expected) }?.minus(tap / 2)
    }

    private fun framedStart(pcm: ShortArray, start: Int, unit: Double, rate: Int): Boolean =
        start + (16 * unit).roundToInt() <= pcm.size &&
            symbol(pcm, start, 0, unit, rate) == 0 && symbol(pcm, start, 5, unit, rate) == 1 &&
            symbol(pcm, start, 6, unit, rate) == 0 && symbol(pcm, start, 11, unit, rate) == 1

    /** Count crossings within the middle of each unit, avoiding transitions at symbol boundaries. */
    private fun symbol(pcm: ShortArray, start: Int, index: Int, unit: Double, rate: Int): Int {
        val from = start + ((index + 0.125) * unit).roundToInt()
        val until = start + ((index + 0.875) * unit).roundToInt()
        var minimum = Int.MAX_VALUE
        var maximum = Int.MIN_VALUE
        for (i in from until until) {
            minimum = minOf(minimum, pcm[i].toInt())
            maximum = maxOf(maximum, pcm[i].toInt())
        }
        if (maximum - minimum < 4096) return -1
        val center = (maximum + minimum) / 2
        var crossings = 0
        for (i in from + 1 until until) {
            if ((pcm[i].toInt() > center) != (pcm[i - 1].toInt() > center)) crossings++
        }
        val frequency = crossings.toDouble() * rate / (until - from - 1)
        return when {
            frequency in 6000.0..10000.0 -> 1
            frequency in 2500.0..5500.0 -> 0
            else -> -1
        }
    }

    private fun diagnostic(severity: CodecDiagnosticSeverity, code: CodecDiagnosticCode, message: String,
        offset: Int = 0, length: Int = 0): CodecDiagnostic = CodecDiagnostic(severity,
        CodecStage.SIGNAL_DECODE, code, message, CodecDataRange(CodecDataKind.PCM_SAMPLES, offset, length))
}

public class S1SignalDecoding internal constructor(
    public val rawBytes: RawTransferByteSequence,
    rawByteMappings: List<CodecDataMapping>,
    syncPoints: List<SignalSyncPoint>,
    public val sampleRate: Int,
    public val inputSampleCount: Int,
) {
    public val rawByteMappings: List<CodecDataMapping> = rawByteMappings.toList()
    public val syncPoints: List<SignalSyncPoint> = syncPoints.toList()
}
