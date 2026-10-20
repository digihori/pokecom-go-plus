package com.digihori.pgp.core.wav

import kotlin.math.abs
import kotlin.math.roundToInt

/** Shared S1/S2 physical decoder. Payload checksums and physical BASIC trailers belong to callers. */
public object S1SignalDecoder {
    public fun decode(input: NormalizedPcmData): CodecResult<S1SignalDecoding> =
        decode(input.sampleRate, input.samples)

    public fun decode(sampleRate: Int, samples: PcmSampleSequence): CodecResult<S1SignalDecoding> {
        if (sampleRate !in setOf(8000, 16000, 24000, 32000)) return CodecResult.Failure(listOf(diagnostic(
            CodecDiagnosticSeverity.ERROR, CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            "S1/S2 signal requires a sample rate of 8000, 16000, 24000, or 32000 Hz",
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

        // A carrier must precede the first frame (or recovery after damage). Silence cannot qualify.
        while (cursor < pcm.size) {
            if (!locked) {
                val difference = abs(pcm[cursor].toInt() - pcm[cursor - tap].toInt())
                if (abs(pcm[cursor].toInt()) < 1024) {
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
            val end = start + (16 * unit).roundToInt()
            val quietFrame = (start until minOf(end, pcm.size)).none { abs(pcm[it].toInt()) >= 1024 }
            if (quietFrame && (minOf(end, pcm.size) until pcm.size).none { abs(pcm[it].toInt()) >= 1024 }) break
            if (end > pcm.size) {
                diagnostics += diagnostic(CodecDiagnosticSeverity.WARNING, CodecDiagnosticCode.TRUNCATED_INPUT,
                    "S1/S2 signal ends with an incomplete raw byte", start, pcm.size - start)
                break
            }
            val bits = IntArray(16) { symbol(pcm, start, it, unit, sampleRate) }
            val framingValid = bits[0] == 0 && bits[5] == 1 && bits[6] == 0 &&
                (11..15).all { bits[it] == 1 } && bits.none { it < 0 }
            if (!framingValid) {
                // A quiet tail is normal. A damaged frame is never emitted as a fabricated byte.
                if (!bits.all { it == 1 }) {
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
                value = value or (bits[bit + 1] shl bit) or (bits[bit + 7] shl (bit + 4))
            }
            val index = raw.size
            raw += value.toByte()
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.PCM_SAMPLES, start, end - start),
                CodecDataRange(CodecDataKind.RAW_BYTES, index, 1), "decoded-raw-byte")
            sync += SignalSyncPoint(start, index, SignalNibble.LOW)
            sync += SignalSyncPoint(start + (6 * unit).roundToInt(), index, SignalNibble.HIGH)
            cursor = end
        }
        if (raw.isEmpty()) return CodecResult.Failure(diagnostics + diagnostic(
            CodecDiagnosticSeverity.ERROR, CodecDiagnosticCode.SYNCHRONIZATION_LOST,
            "No complete S1/S2 raw byte could be decoded", 0, pcm.size,
        ))
        return CodecResult.Success(S1SignalDecoding(RawTransferByteSequence(raw.toByteArray()), mappings,
            sync, sampleRate, pcm.size), diagnostics)
    }

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
