package com.digihori.pgp.core.wav

/** Writes canonical PCM RIFF/WAVE bytes without performing filesystem I/O. */
public object PcmWavWriter {
    public fun writeOldSignal(signal: OldSignalEncoding): CodecResult<WavEncoding> =
        write8BitMono(signal.sampleRate, signal.samples)

    public fun write8BitMono(
        sampleRate: Int,
        samples: PcmSampleSequence,
    ): CodecResult<WavEncoding> {
        if (sampleRate <= 0) return failure("WAV sample rate must be positive")
        val dataLength = samples.size
        val fileSize = HEADER_SIZE.toLong() + dataLength.toLong()
        if (fileSize > Int.MAX_VALUE || dataLength.toLong() + 36L > 0xffff_ffffL) {
            return failure("WAV data exceeds the supported RIFF size")
        }

        val output = ByteArray(fileSize.toInt())
        writeAscii(output, 0, "RIFF")
        writeU32Le(output, 4, dataLength.toLong() + 36L)
        writeAscii(output, 8, "WAVE")
        writeAscii(output, 12, "fmt ")
        writeU32Le(output, 16, 16)
        writeU16Le(output, 20, WavAudioEncoding.PCM.formatCode)
        writeU16Le(output, 22, 1)
        writeU32Le(output, 24, sampleRate.toLong())
        writeU32Le(output, 28, sampleRate.toLong())
        writeU16Le(output, 32, 1)
        writeU16Le(output, 34, 8)
        writeAscii(output, 36, "data")
        writeU32Le(output, 40, dataLength.toLong())

        samples.copySamples().forEachIndexed { index, sample ->
            output[HEADER_SIZE + index] = ((sample.toInt() shr 8) + 128).coerceIn(0, 255).toByte()
        }

        return CodecResult.Success(
            WavEncoding(
                bytes = WavByteSequence(output),
                format = WavPcmFormat(sampleRate, channelCount = 1, bitsPerSample = 8),
                pcmMapping = CodecDataMapping(
                    source = CodecDataRange(CodecDataKind.PCM_SAMPLES, 0, samples.size),
                    target = CodecDataRange(CodecDataKind.WAV_BYTES, HEADER_SIZE, dataLength),
                    role = "pcm-data",
                ),
            ),
        )
    }

    private fun failure(message: String): CodecResult.Failure = CodecResult.Failure(
        listOf(
            CodecDiagnostic(
                severity = CodecDiagnosticSeverity.ERROR,
                stage = CodecStage.WAV_CONTAINER,
                code = CodecDiagnosticCode.INVALID_ARGUMENT,
                message = message,
            ),
        ),
    )

    private fun writeAscii(target: ByteArray, offset: Int, value: String) {
        value.forEachIndexed { index, character -> target[offset + index] = character.code.toByte() }
    }

    private fun writeU16Le(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeU32Le(target: ByteArray, offset: Int, value: Long) {
        repeat(4) { index -> target[offset + index] = (value ushr (index * 8)).toByte() }
    }

    private const val HEADER_SIZE: Int = 44
}

public class WavEncoding internal constructor(
    public val bytes: WavByteSequence,
    public val format: WavPcmFormat,
    public val pcmMapping: CodecDataMapping,
)
