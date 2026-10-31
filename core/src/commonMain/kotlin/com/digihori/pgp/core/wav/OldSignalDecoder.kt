package com.digihori.pgp.core.wav

/** Recovers raw transfer bytes from normalized OLD PCM samples. */
public object OldSignalDecoder {
    public fun decode(input: NormalizedPcmData): CodecResult<OldSignalDecoding> =
        decode(input.sampleRate, input.samples)

    public fun decode(
        sampleRate: Int,
        samples: PcmSampleSequence,
    ): CodecResult<OldSignalDecoding> {
        if (sampleRate == 44100 || sampleRate == 48000) {
            val input = HighRateSignalInput(sampleRate, samples)
            return when (val result = decodeSignal(16000, input.samples, smoothCarrier = true)) {
                is CodecResult.Failure -> CodecResult.Failure(input.diagnostics(result.diagnostics))
                is CodecResult.Success -> CodecResult.Success(OldSignalDecoding(
                    result.value.rawBytes, input.mappings(result.value.rawByteMappings), input.sync(result.value.syncPoints),
                    sampleRate, samples.size), input.diagnostics(result.diagnostics))
            }
        }
        return decodeSignal(sampleRate, samples, smoothCarrier = false)
    }

    private fun decodeSignal(sampleRate: Int, samples: PcmSampleSequence, smoothCarrier: Boolean): CodecResult<OldSignalDecoding> {
        if (sampleRate !in MIN_SAMPLE_RATE..MAX_SAMPLE_RATE) {
            return failure(
                code = CodecDiagnosticCode.UNSUPPORTED_FORMAT,
                message = "OLD signal sample rate $sampleRate Hz is not supported",
            )
        }

        val inputSamples = samples.copySamples()
        var level = 0
        var syncing = true
        var syncCount = 0
        val signalUnitSamples = sampleRate / SIGNAL_UNITS_PER_SECOND
        var gapCount = 0
        var readBit = 0x01
        var data = 0
        var lowDifferenceCount = 0
        val tap = maxOf(1, (sampleRate.toDouble() / DIFFERENCE_FREQUENCY_HZ + 0.5).toInt())
        val raw = mutableListOf<Byte>()
        val mappings = mutableListOf<CodecDataMapping>()
        val syncPoints = mutableListOf<SignalSyncPoint>()
        val pendingSyncPoints = mutableListOf<SignalSyncPoint>()
        var frameStart = -1
        var leaderQualified = false
        val signalStart = inputSamples.indexOfFirstSignificantSample()

        for (sampleOffset in signalStart until inputSamples.size) {
            val difference = if (smoothCarrier) {
                // Average two carrier periods so sample quantization of a square wave does not
                // turn isolated phase/edge errors into a false start bit.
                val from = maxOf(signalStart, sampleOffset - 2 * tap + 1)
                (from..sampleOffset).sumOf { kotlin.math.abs(differenceAt(inputSamples, it, signalStart, tap)) } / (sampleOffset - from + 1)
            } else differenceAt(inputSamples, sampleOffset, signalStart, tap)

            if (difference > DIFFERENCE_THRESHOLD || difference < -DIFFERENCE_THRESHOLD) {
                level = 0
                lowDifferenceCount = 0
            } else {
                if (lowDifferenceCount > 3) level = 1
                lowDifferenceCount++
            }

            if (syncing) {
                if (gapCount != 0) {
                    gapCount--
                } else if (level == 0) {
                    // A carrier-to-silence edge is not a new nibble start. Require signal
                    // in the middle of its start unit, beyond the edge/filter transient.
                    val from = minOf(inputSamples.size, sampleOffset + signalUnitSamples / 4)
                    val until = minOf(inputSamples.size, sampleOffset + signalUnitSamples * 3 / 4)
                    if (from < until && (from until until).none {
                        kotlin.math.abs(inputSamples[it].toInt()) > DIFFERENCE_THRESHOLD
                    }) continue
                    syncing = false
                    gapCount = signalUnitSamples * 3 / 2
                    if (readBit == 0x01) frameStart = sampleOffset
                    pendingSyncPoints += SignalSyncPoint(
                        sampleOffset = sampleOffset,
                        rawByteOffset = raw.size,
                        nibble = if (readBit == 0x01) SignalNibble.LOW else SignalNibble.HIGH,
                    )
                } else {
                    syncCount++
                    if (syncCount > MIN_SYNC_COUNT && readBit == 0x10) {
                        leaderQualified = true
                        readBit = 0x01
                        data = 0
                        frameStart = -1
                        pendingSyncPoints.clear()
                    }
                }
            } else if (--gapCount <= 0) {
                gapCount = signalUnitSamples
                if (level != 0) data = data or readBit
                readBit *= 2

                if (readBit == 0x100) {
                    syncing = true
                    syncCount = 0
                    if (leaderQualified) {
                        raw += (data and 0xff).toByte()
                        val start = if (frameStart >= 0) frameStart else sampleOffset
                        mappings += CodecDataMapping(
                            source = CodecDataRange(
                                CodecDataKind.PCM_SAMPLES,
                                start,
                                sampleOffset - start + 1,
                            ),
                            target = CodecDataRange(CodecDataKind.RAW_BYTES, raw.lastIndex, 1),
                            role = "decoded-raw-byte",
                        )
                        syncPoints += pendingSyncPoints
                    }
                    data = 0
                    readBit = 0x01
                    frameStart = -1
                    pendingSyncPoints.clear()
                }

                if (readBit == 0x10) {
                    syncing = true
                    syncCount = 0
                }
            }

        }

        if (raw.isEmpty()) {
            return failure(
                code = CodecDiagnosticCode.SYNCHRONIZATION_LOST,
                message = "No complete OLD raw byte could be decoded from the PCM signal",
                range = CodecDataRange(CodecDataKind.PCM_SAMPLES, 0, inputSamples.size),
            )
        }

        val diagnostics = if (frameStart >= 0 || !syncing || readBit != 0x01) {
            listOf(
                CodecDiagnostic(
                    severity = CodecDiagnosticSeverity.WARNING,
                    stage = CodecStage.SIGNAL_DECODE,
                    code = CodecDiagnosticCode.SYNCHRONIZATION_LOST,
                    message = "OLD signal ends with an incomplete raw byte",
                    range = CodecDataRange(
                        CodecDataKind.PCM_SAMPLES,
                        maxOf(0, frameStart),
                        inputSamples.size - maxOf(0, frameStart),
                    ),
                ),
            )
        } else {
            emptyList()
        }

        return CodecResult.Success(
            value = OldSignalDecoding(
                rawBytes = RawTransferByteSequence(raw.toByteArray()),
                rawByteMappings = mappings,
                syncPoints = syncPoints,
                sampleRate = sampleRate,
                inputSampleCount = inputSamples.size,
            ),
            diagnostics = diagnostics,
        )
    }

    /** Observes original PCM differences without payload interpretation.
     * High-rate decoding additionally filters/resamples and smooths differences; this view retains the recording.
     */
    public fun inspect(sampleRate: Int, samples: PcmSampleSequence): OldSignalInspection {
        require(sampleRate > 0)
        val input = samples.copySamples()
        val start = input.indexOfFirstSignificantSample()
        val tap = maxOf(1, (sampleRate.toDouble() / DIFFERENCE_FREQUENCY_HZ + 0.5).toInt())
        val differences = IntArray(input.size)
        for (index in start until input.size) differences[index] = differenceAt(input, index, start, tap)
        return OldSignalInspection(start, tap, DIFFERENCE_THRESHOLD, differences)
    }

    private fun differenceAt(samples: ShortArray, index: Int, start: Int, tap: Int): Int =
        samples[index].toInt() - if (index - tap >= start) samples[index - tap].toInt() else 0

    private fun failure(
        code: CodecDiagnosticCode,
        message: String,
        range: CodecDataRange? = null,
    ): CodecResult.Failure = CodecResult.Failure(
        listOf(
            CodecDiagnostic(
                severity = CodecDiagnosticSeverity.ERROR,
                stage = CodecStage.SIGNAL_DECODE,
                code = code,
                message = message,
                range = range,
            ),
        ),
    )

    /** Prevents leading silence from being mistaken for a long run of one bits. */
    private fun ShortArray.indexOfFirstSignificantSample(): Int {
        var previous = 0
        forEachIndexed { index, sample ->
            val current = sample.toInt()
            val difference = current - previous
            if (difference > DIFFERENCE_THRESHOLD || difference < -DIFFERENCE_THRESHOLD) return index
            previous = current
        }
        return size
    }

    private const val MIN_SAMPLE_RATE: Int = 4000
    private const val MAX_SAMPLE_RATE: Int = 32000
    private const val SIGNAL_UNITS_PER_SECOND: Int = 500
    private const val DIFFERENCE_FREQUENCY_HZ: Double = 4000.0
    private const val DIFFERENCE_THRESHOLD: Int = 20 * 256
    private const val MIN_SYNC_COUNT: Int = 1000
}

public enum class SignalNibble {
    LOW,
    HIGH,
}

public data class SignalSyncPoint(
    public val sampleOffset: Int,
    public val rawByteOffset: Int,
    public val nibble: SignalNibble,
)

/** Raw Decode result with physical positions retained for analysis. */
public class OldSignalDecoding internal constructor(
    public val rawBytes: RawTransferByteSequence,
    rawByteMappings: List<CodecDataMapping>,
    syncPoints: List<SignalSyncPoint>,
    public val sampleRate: Int,
    public val inputSampleCount: Int,
) {
    public val rawByteMappings: List<CodecDataMapping> = rawByteMappings.toList()
    public val syncPoints: List<SignalSyncPoint> = syncPoints.toList()
}

/** Signed differences can exceed the signed 16-bit PCM amplitude range. */
public class OldSignalInspection internal constructor(
    public val signalStart: Int,
    public val tapSamples: Int,
    public val threshold: Int,
    differences: IntArray,
) {
    private val values: IntArray = differences.copyOf()
    public fun copyDifferences(): IntArray = values.copyOf()
}
