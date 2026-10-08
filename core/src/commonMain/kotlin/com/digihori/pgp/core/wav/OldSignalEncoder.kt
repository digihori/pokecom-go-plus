package com.digihori.pgp.core.wav

/** Converts an OLD raw transfer payload to its 8000 Hz mono PCM signal. */
public object OldSignalEncoder {
    public const val SAMPLE_RATE: Int = 8000
    public const val SAMPLES_PER_SIGNAL_UNIT: Int = 16
    public const val SIGNAL_UNITS_PER_BYTE: Int = 19
    public const val SAMPLES_PER_BYTE: Int = SAMPLES_PER_SIGNAL_UNIT * SIGNAL_UNITS_PER_BYTE
    public const val LEADER_SIGNAL_UNITS: Int = 0x400
    public const val LEADER_SAMPLES: Int = LEADER_SIGNAL_UNITS * SAMPLES_PER_SIGNAL_UNIT

    public fun encode(payload: OldTransferPayload): CodecResult<OldSignalEncoding> =
        encode(RawTransferByteSequence(payload.copyRawBytes()))

    /** Encodes arbitrary raw bytes as a physical OLD signal without interpreting their payload. */
    public fun encode(rawBytes: RawTransferByteSequence): CodecResult<OldSignalEncoding> {
        val raw = rawBytes.copyBytes()
        val sampleCount = LEADER_SAMPLES.toLong() + raw.size.toLong() * SAMPLES_PER_BYTE.toLong()
        if (sampleCount > Int.MAX_VALUE) {
            return CodecResult.Failure(
                listOf(
                    CodecDiagnostic(
                        severity = CodecDiagnosticSeverity.ERROR,
                        stage = CodecStage.SIGNAL_ENCODE,
                        code = CodecDiagnosticCode.INVALID_ARGUMENT,
                        message = "OLD signal exceeds the supported PCM sample count",
                    ),
                ),
            )
        }

        val samples = ShortArray(sampleCount.toInt())
        var sampleOffset = 0
        repeat(LEADER_SIGNAL_UNITS) {
            sampleOffset = writeOne(samples, sampleOffset)
        }

        val mappings = ArrayList<CodecDataMapping>(raw.size)
        raw.forEachIndexed { rawOffset, byte ->
            val frameStart = sampleOffset
            sampleOffset = writeByte(samples, sampleOffset, byte.toInt() and 0xff)
            mappings += CodecDataMapping(
                source = CodecDataRange(CodecDataKind.RAW_BYTES, rawOffset, 1),
                target = CodecDataRange(CodecDataKind.PCM_SAMPLES, frameStart, SAMPLES_PER_BYTE),
                role = "raw-byte-frame",
            )
        }

        check(sampleOffset == samples.size)
        return CodecResult.Success(
            OldSignalEncoding(
                samples = PcmSampleSequence(samples),
                rawByteMappings = mappings,
            ),
        )
    }

    private fun writeByte(target: ShortArray, start: Int, value: Int): Int {
        var offset = writeZero(target, start)

        repeat(4) { bit ->
            offset = writeBit(target, offset, value and (1 shl bit) != 0)
        }
        repeat(4) { offset = writeOne(target, offset) }
        offset = writeZero(target, offset)
        repeat(4) { bit ->
            offset = writeBit(target, offset, value and (1 shl (bit + 4)) != 0)
        }
        repeat(5) { offset = writeOne(target, offset) }

        return offset
    }

    private fun writeBit(target: ShortArray, start: Int, one: Boolean): Int =
        if (one) writeOne(target, start) else writeZero(target, start)

    private fun writeOne(target: ShortArray, start: Int): Int {
        var offset = start
        repeat(8) {
            target[offset++] = HIGH_SAMPLE
            target[offset++] = LOW_SAMPLE
        }
        return offset
    }

    private fun writeZero(target: ShortArray, start: Int): Int {
        var offset = start
        repeat(4) {
            target[offset++] = HIGH_SAMPLE
            target[offset++] = HIGH_SAMPLE
            target[offset++] = LOW_SAMPLE
            target[offset++] = LOW_SAMPLE
        }
        return offset
    }

    private const val HIGH_SAMPLE: Short = 32512
    private const val LOW_SAMPLE: Short = -32768
}

/** PCM output and raw-to-sample mappings retained for analysis views. */
public class OldSignalEncoding internal constructor(
    public val samples: PcmSampleSequence,
    rawByteMappings: List<CodecDataMapping>,
) {
    public val sampleRate: Int = OldSignalEncoder.SAMPLE_RATE
    public val rawByteMappings: List<CodecDataMapping> = rawByteMappings.toList()
}
