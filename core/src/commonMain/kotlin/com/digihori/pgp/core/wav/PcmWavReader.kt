package com.digihori.pgp.core.wav

/** Parses PCM RIFF/WAVE bytes without performing filesystem I/O. */
public object PcmWavReader {
    public fun read(input: WavByteSequence): CodecResult<WavPcmData> {
        val bytes = input.copyBytes()
        if (bytes.size < RIFF_HEADER_SIZE) return truncated("WAV is shorter than its RIFF header", 0, bytes.size)
        if (!matchesAscii(bytes, 0, "RIFF")) return invalid("WAV RIFF signature is missing", 0, 4)
        if (!matchesAscii(bytes, 8, "WAVE")) return invalid("WAV WAVE signature is missing", 8, 4)

        val riffSize = readU32Le(bytes, 4)
        val declaredEnd = 8L + riffSize
        if (declaredEnd < RIFF_HEADER_SIZE.toLong()) return invalid("WAV RIFF size is invalid", 4, 4)
        if (declaredEnd > bytes.size.toLong()) {
            return truncated("WAV is shorter than its declared RIFF size", bytes.size, 0)
        }

        var offset = RIFF_HEADER_SIZE
        var format: WavPcmFormat? = null
        var data: ByteArray? = null
        var dataOffset = -1
        while (offset.toLong() < declaredEnd) {
            if (offset.toLong() + CHUNK_HEADER_SIZE > declaredEnd) {
                return truncated("WAV contains a truncated chunk header", offset, (declaredEnd - offset).toInt())
            }
            val chunkId = ascii(bytes, offset, 4)
            val chunkLength = readU32Le(bytes, offset + 4)
            if (chunkLength > Int.MAX_VALUE) {
                return invalid("WAV chunk is too large", offset + 4, 4)
            }
            val chunkDataOffset = offset + CHUNK_HEADER_SIZE
            val chunkEnd = chunkDataOffset.toLong() + chunkLength
            if (chunkEnd > declaredEnd) {
                return truncated("WAV chunk exceeds the declared RIFF size", chunkDataOffset, 0)
            }

            when (chunkId) {
                "fmt " -> {
                    if (format != null) return invalid("WAV contains more than one fmt chunk", offset, 4)
                    val parsed = parseFormat(bytes, chunkDataOffset, chunkLength.toInt())
                    when (parsed) {
                        is CodecResult.Failure -> return parsed
                        is CodecResult.Success -> format = parsed.value
                    }
                }
                "data" -> {
                    if (data != null) return invalid("WAV contains more than one data chunk", offset, 4)
                    dataOffset = chunkDataOffset
                    data = bytes.copyOfRange(chunkDataOffset, chunkEnd.toInt())
                }
            }

            // pcwav output omits the optional pad after a terminal odd-length data chunk.
            val paddedEnd = if (chunkLength and 1L != 0L && chunkEnd < declaredEnd) chunkEnd + 1L else chunkEnd
            if (paddedEnd > declaredEnd) {
                return truncated("WAV odd-length chunk is missing its pad byte", chunkEnd.toInt(), 0)
            }
            offset = paddedEnd.toInt()
        }

        val parsedFormat = format ?: return invalid("WAV fmt chunk is missing", 12, 0)
        val pcmData = data ?: return invalid("WAV data chunk is missing", 12, 0)
        val bytesPerSample = parsedFormat.bitsPerSample / 8
        val frameSize = parsedFormat.channelCount * bytesPerSample
        if (pcmData.size % frameSize != 0) {
            return invalid("WAV data does not contain complete sample frames", dataOffset, pcmData.size)
        }

        val diagnostics = if (declaredEnd < bytes.size.toLong()) {
            listOf(
                CodecDiagnostic(
                    severity = CodecDiagnosticSeverity.WARNING,
                    stage = CodecStage.WAV_CONTAINER,
                    code = CodecDiagnosticCode.TRAILING_DATA,
                    message = "WAV has ${bytes.size - declaredEnd.toInt()} byte(s) after its RIFF container",
                    range = CodecDataRange(
                        CodecDataKind.WAV_BYTES,
                        declaredEnd.toInt(),
                        bytes.size - declaredEnd.toInt(),
                    ),
                ),
            )
        } else {
            emptyList()
        }
        return CodecResult.Success(
            WavPcmData(parsedFormat, pcmData, dataOffset),
            diagnostics,
        )
    }

    private fun parseFormat(bytes: ByteArray, offset: Int, length: Int): CodecResult<WavPcmFormat> {
        if (length < PCM_FORMAT_SIZE) return truncated("WAV fmt chunk is shorter than 16 bytes", offset, length)
        val encodingCode = readU16Le(bytes, offset)
        if (encodingCode != WavAudioEncoding.PCM.formatCode) {
            return unsupported("WAV encoding $encodingCode is not PCM", offset, 2)
        }
        val channelCount = readU16Le(bytes, offset + 2)
        if (channelCount !in 1..2) {
            return unsupported("WAV channel count $channelCount is not supported", offset + 2, 2)
        }
        val sampleRateLong = readU32Le(bytes, offset + 4)
        if (sampleRateLong !in 1L..MAX_SAMPLE_RATE.toLong()) {
            return unsupported("WAV sample rate $sampleRateLong Hz is not supported", offset + 4, 4)
        }
        val bitsPerSample = readU16Le(bytes, offset + 14)
        if (bitsPerSample != 8 && bitsPerSample != 16) {
            return unsupported("WAV $bitsPerSample-bit PCM is not supported", offset + 14, 2)
        }
        val blockAlign = readU16Le(bytes, offset + 12)
        val expectedBlockAlign = channelCount * (bitsPerSample / 8)
        if (blockAlign != expectedBlockAlign) {
            return invalid("WAV block alignment does not match its PCM format", offset + 12, 2)
        }
        return CodecResult.Success(
            WavPcmFormat(
                sampleRate = sampleRateLong.toInt(),
                channelCount = channelCount,
                bitsPerSample = bitsPerSample,
            ),
        )
    }

    private fun invalid(message: String, offset: Int, length: Int): CodecResult.Failure =
        failure(CodecDiagnosticCode.INVALID_HEADER, message, offset, length)

    private fun truncated(message: String, offset: Int, length: Int): CodecResult.Failure =
        failure(CodecDiagnosticCode.TRUNCATED_INPUT, message, offset, length)

    private fun unsupported(message: String, offset: Int, length: Int): CodecResult.Failure =
        failure(CodecDiagnosticCode.UNSUPPORTED_FORMAT, message, offset, length)

    private fun failure(
        code: CodecDiagnosticCode,
        message: String,
        offset: Int,
        length: Int,
    ): CodecResult.Failure = CodecResult.Failure(
        listOf(
            CodecDiagnostic(
                severity = CodecDiagnosticSeverity.ERROR,
                stage = CodecStage.WAV_CONTAINER,
                code = code,
                message = message,
                range = CodecDataRange(CodecDataKind.WAV_BYTES, offset, length),
            ),
        ),
    )

    private fun matchesAscii(bytes: ByteArray, offset: Int, expected: String): Boolean =
        expected.indices.all { bytes[offset + it].toInt() and 0xff == expected[it].code }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        buildString(length) { repeat(length) { append((bytes[offset + it].toInt() and 0xff).toChar()) } }

    private fun readU16Le(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun readU32Le(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xffL) or
            ((bytes[offset + 1].toLong() and 0xffL) shl 8) or
            ((bytes[offset + 2].toLong() and 0xffL) shl 16) or
            ((bytes[offset + 3].toLong() and 0xffL) shl 24)

    private const val RIFF_HEADER_SIZE: Int = 12
    private const val CHUNK_HEADER_SIZE: Int = 8
    private const val PCM_FORMAT_SIZE: Int = 16
    private const val MAX_SAMPLE_RATE: Int = 32000
}

public class WavPcmData internal constructor(
    public val format: WavPcmFormat,
    data: ByteArray,
    public val dataOffset: Int,
) {
    private val content: ByteArray = data.copyOf()
    public val dataSize: Int get() = content.size
    public fun copyData(): ByteArray = content.copyOf()
}

/** Converts supported WAV PCM frames to mono signed 16-bit-equivalent samples. */
public object PcmNormalizer {
    public fun normalize(input: WavPcmData): CodecResult<NormalizedPcmData> {
        val format = input.format
        val data = input.copyData()
        val bytesPerSample = format.bitsPerSample / 8
        val frameSize = format.channelCount * bytesPerSample
        val samples = ShortArray(data.size / frameSize)

        samples.indices.forEach { frame ->
            val frameOffset = frame * frameSize
            val left = readSample(data, frameOffset, format.bitsPerSample)
            val mono = if (format.channelCount == 1) {
                left
            } else {
                val right = readSample(data, frameOffset + bytesPerSample, format.bitsPerSample)
                (left + right) / 2
            }
            samples[frame] = mono.toShort()
        }
        return CodecResult.Success(
            NormalizedPcmData(
                sampleRate = format.sampleRate,
                samples = PcmSampleSequence(samples),
            ),
        )
    }

    private fun readSample(data: ByteArray, offset: Int, bitsPerSample: Int): Int =
        if (bitsPerSample == 8) {
            ((data[offset].toInt() and 0xff) - 128) * 256
        } else {
            val unsigned = (data[offset].toInt() and 0xff) or ((data[offset + 1].toInt() and 0xff) shl 8)
            if (unsigned >= 0x8000) unsigned - 0x10000 else unsigned
        }
}

public class NormalizedPcmData internal constructor(
    public val sampleRate: Int,
    public val samples: PcmSampleSequence,
)
