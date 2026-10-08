package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PcmWavCodecTest {
    @Test
    fun writesPcwavCompatibleCanonicalHeaderAndUnsignedSamples() {
        val encoded = write(shortArrayOf(-32768, 0, 32512))
        val bytes = encoded.bytes.copyBytes()

        assertEquals(47, bytes.size)
        assertContentEquals("RIFF".ascii(), bytes.copyOfRange(0, 4))
        assertEquals(39, u32(bytes, 4))
        assertContentEquals("WAVEfmt ".ascii(), bytes.copyOfRange(8, 16))
        assertEquals(16, u32(bytes, 16))
        assertEquals(1, u16(bytes, 20))
        assertEquals(1, u16(bytes, 22))
        assertEquals(8000, u32(bytes, 24))
        assertEquals(8000, u32(bytes, 28))
        assertEquals(1, u16(bytes, 32))
        assertEquals(8, u16(bytes, 34))
        assertContentEquals("data".ascii(), bytes.copyOfRange(36, 40))
        assertEquals(3, u32(bytes, 40))
        assertContentEquals(bytes(0x00, 0x80, 0xff), bytes.copyOfRange(44, 47))
        assertEquals(CodecDataRange(CodecDataKind.WAV_BYTES, 44, 3), encoded.pcmMapping.target)
    }

    @Test
    fun readsAndNormalizesOwnOddLengthOutput() {
        val written = write(shortArrayOf(-32768, 0, 32512))
        val read = read(written.bytes)
        val normalized = normalize(read.value)

        assertEquals(WavPcmFormat(8000, 1, 8), read.value.format)
        assertEquals(44, read.value.dataOffset)
        assertContentEquals(shortArrayOf(-32768, 0, 32512), normalized.samples.copySamples())
    }

    @Test
    fun skipsUnknownOddChunkAndNormalizesStereo16Pcm() {
        val wav = wavWithChunks(
            chunk("JUNK", bytes(1, 2, 3), includePad = true),
            formatChunk(sampleRate = 16000, channels = 2, bits = 16),
            chunk(
                "data",
                bytes(
                    0x00, 0x80, 0x00, 0x00,
                    0xff, 0x7f, 0x01, 0x80,
                ),
                includePad = false,
            ),
        )

        val read = read(WavByteSequence(wav))
        val normalized = normalize(read.value)

        assertEquals(WavPcmFormat(16000, 2, 16), read.value.format)
        assertContentEquals(shortArrayOf(-16384, 0), normalized.samples.copySamples())
    }

    @Test
    fun reportsBytesAfterDeclaredRiffContainer() {
        val wav = write(shortArrayOf(0)).bytes.copyBytes() + bytes(0xaa, 0xbb)

        val result = read(WavByteSequence(wav))

        assertEquals(CodecDiagnosticCode.TRAILING_DATA, result.diagnostics.single().code)
        assertEquals(wav.size - 2, result.diagnostics.single().range?.offset)
    }

    @Test
    fun rejectsUnsupportedAndTruncatedWavInputs() {
        val unsupported = wavWithChunks(
            formatChunk(sampleRate = 44100, channels = 1, bits = 8),
            chunk("data", bytes(0x80), includePad = false),
        )
        assertEquals(
            CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            failure(PcmWavReader.read(WavByteSequence(unsupported))).code,
        )

        val complete = write(shortArrayOf(0, 1)).bytes.copyBytes()
        assertEquals(
            CodecDiagnosticCode.TRUNCATED_INPUT,
            failure(PcmWavReader.read(WavByteSequence(complete.copyOf(complete.size - 1)))).code,
        )

        val invalidSignature = complete.copyOf().also { it[0] = 'X'.code.toByte() }
        assertEquals(
            CodecDiagnosticCode.INVALID_HEADER,
            failure(PcmWavReader.read(WavByteSequence(invalidSignature))).code,
        )
    }

    @Test
    fun writesAndReadsCompleteOldSignalWithoutFilesystemDependencies() {
        val signal = assertIs<CodecResult.Success<OldSignalEncoding>>(
            OldSignalEncoder.encode(RawTransferByteSequence(bytes(0x02, 0xf0))),
        ).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.writeOldSignal(signal)).value
        val parsed = read(wav.bytes)
        val normalized = normalize(parsed.value)

        assertEquals(44 + OldSignalEncoder.LEADER_SAMPLES + 2 * OldSignalEncoder.SAMPLES_PER_BYTE, wav.bytes.size)
        assertContentEquals(signal.samples.copySamples(), normalized.samples.copySamples())
    }

    private fun write(samples: ShortArray): WavEncoding =
        assertIs<CodecResult.Success<WavEncoding>>(
            PcmWavWriter.write8BitMono(8000, PcmSampleSequence(samples)),
        ).value

    private fun read(bytes: WavByteSequence): CodecResult.Success<WavPcmData> =
        assertIs(PcmWavReader.read(bytes))

    private fun normalize(data: WavPcmData): NormalizedPcmData =
        assertIs<CodecResult.Success<NormalizedPcmData>>(PcmNormalizer.normalize(data)).value

    private fun failure(result: CodecResult<WavPcmData>): CodecDiagnostic =
        assertIs<CodecResult.Failure>(result).diagnostics.single()

    private fun formatChunk(sampleRate: Int, channels: Int, bits: Int): ByteArray {
        val blockAlign = channels * bits / 8
        val data = ByteArray(16)
        putU16(data, 0, 1)
        putU16(data, 2, channels)
        putU32(data, 4, sampleRate)
        putU32(data, 8, sampleRate * blockAlign)
        putU16(data, 12, blockAlign)
        putU16(data, 14, bits)
        return chunk("fmt ", data, includePad = false)
    }

    private fun chunk(id: String, data: ByteArray, includePad: Boolean): ByteArray {
        val result = ByteArray(8 + data.size + if (includePad && data.size % 2 != 0) 1 else 0)
        id.ascii().copyInto(result)
        putU32(result, 4, data.size)
        data.copyInto(result, 8)
        return result
    }

    private fun wavWithChunks(vararg chunks: ByteArray): ByteArray {
        val bodySize = 4 + chunks.sumOf(ByteArray::size)
        val result = ByteArray(8 + bodySize)
        "RIFF".ascii().copyInto(result)
        putU32(result, 4, bodySize)
        "WAVE".ascii().copyInto(result, 8)
        var offset = 12
        chunks.forEach {
            it.copyInto(result, offset)
            offset += it.size
        }
        return result
    }

    private fun String.ascii(): ByteArray = map { it.code.toByte() }.toByteArray()
    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()

    private fun putU16(target: ByteArray, offset: Int, value: Int) {
        target[offset] = value.toByte()
        target[offset + 1] = (value ushr 8).toByte()
    }

    private fun putU32(target: ByteArray, offset: Int, value: Int) {
        repeat(4) { target[offset + it] = (value ushr (it * 8)).toByte() }
    }

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Int =
        u16(bytes, offset) or (u16(bytes, offset + 2) shl 16)
}
