package com.digihori.pgp.core.wav

/** Recovers raw transfer bytes from normalized OLD PCM samples. */
public object OldSignalDecoder {
    public fun decode(input: NormalizedPcmData): CodecResult<OldSignalDecoding> =
        decode(input.sampleRate, input.samples)

    public fun decode(
        sampleRate: Int,
        samples: PcmSampleSequence,
    ): CodecResult<OldSignalDecoding> {
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
        val delayed = IntArray(tap + 1)
        val raw = mutableListOf<Byte>()
        val mappings = mutableListOf<CodecDataMapping>()
        val syncPoints = mutableListOf<SignalSyncPoint>()
        val pendingSyncPoints = mutableListOf<SignalSyncPoint>()
        var frameStart = -1
        var leaderQualified = false
        val signalStart = inputSamples.indexOfFirstSignificantSample()

        for (sampleOffset in signalStart until inputSamples.size) {
            val sample = inputSamples[sampleOffset]
            delayed[0] = sample.toInt()
            val difference = delayed[0] - delayed[tap]

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

            for (index in tap downTo 1) delayed[index] = delayed[index - 1]
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
