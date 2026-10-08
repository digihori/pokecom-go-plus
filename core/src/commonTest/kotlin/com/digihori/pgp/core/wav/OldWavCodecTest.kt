package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OldWavCodecTest {
    @Test
    fun encodesAndDecodesBasicThroughOneApi() {
        val body = LogicalByteSequence(bytes(0xe0, 0x10, 0xd4, 0x00, 0xf0))

        val encoded = assertIs<CodecResult.Success<OldWavEncoding>>(
            OldWavCodec.encodeBasic("game.bas", body),
        )
        val decoded = assertIs<CodecResult.Success<OldWavDecoding>>(
            OldWavCodec.decode(encoded.value.wav.bytes),
        )

        assertEquals("GAME", encoded.value.payload.normalizedFilename)
        assertEquals(OldPayloadType.BASIC, decoded.value.payload.type)
        assertEquals("GAME", decoded.value.payload.filename)
        assertContentEquals(body.copyBytes(), decoded.value.payload.body.copyBytes())
        assertEquals(encoded.value.payload.payload.size, decoded.value.signal.rawBytes.size)
        assertEquals(encoded.value.payload.payload.size, decoded.value.signal.rawByteMappings.size)
        assertEquals(encoded.value.payload.payload.size * 2, decoded.value.signal.syncPoints.size)
        assertTrue(decoded.diagnostics.isEmpty())
    }

    @Test
    fun preservesPasswordAndBinaryMetadata() {
        val basicBody = LogicalByteSequence(bytes(0xf0))
        val protected = encodeBasic("secret", basicBody, passwordProtected = true)
        val protectedDecoded = decode(protected.value.wav.bytes)
        assertEquals(OldPayloadType.PASSWORD_PROTECTED_BASIC, protectedDecoded.value.payload.type)
        assertTrue(protectedDecoded.value.payload.passwordProtected)

        val binaryBody = LogicalByteSequence(bytes(0x12, 0x34, 0x56, 0x78))
        val binary = assertIs<CodecResult.Success<OldWavEncoding>>(
            OldWavCodec.encodeBinary("data.bin", 0x4000, binaryBody),
        )
        val binaryDecoded = decode(binary.value.wav.bytes)
        assertEquals(OldPayloadType.BINARY, binaryDecoded.value.payload.type)
        assertEquals("DATA", binaryDecoded.value.payload.filename)
        assertEquals(0x4000, binaryDecoded.value.payload.startAddress)
        assertEquals(0x4003, binaryDecoded.value.payload.endAddress)
        assertContentEquals(binaryBody.copyBytes(), binaryDecoded.value.payload.body.copyBytes())
    }

    @Test
    fun roundTripsChunkAndChecksumResetBoundariesThroughWav() {
        val boundarySizes = listOf(1, 7, 8, 9, 79, 80, 81, 88)

        boundarySizes.forEach { size ->
            val basicBytes = ByteArray(size) { index ->
                if (index == size - 1) 0xf0.toByte() else (index % 0xef).toByte()
            }
            val basic = encodeBasic("B$size", LogicalByteSequence(basicBytes))
            assertEncodingSizes(basic.value, BASIC_HEADER_SIZE, size, "BASIC size $size")
            assertContentEquals(
                basicBytes,
                decode(basic.value.wav.bytes).value.payload.body.copyBytes(),
                "BASIC size $size",
            )

            val binaryBytes = ByteArray(size) { index -> (index * 37 + 11).toByte() }
            val binary = assertIs<CodecResult.Success<OldWavEncoding>>(
                OldWavCodec.encodeBinary("D$size", 0x4000, LogicalByteSequence(binaryBytes)),
            )
            assertEncodingSizes(binary.value, BINARY_HEADER_SIZE, size, "binary size $size")
            assertContentEquals(
                binaryBytes,
                decode(binary.value.wav.bytes).value.payload.body.copyBytes(),
                "binary size $size",
            )
        }
    }

    @Test
    fun accumulatesWarningsFromContainerAndPayloadLayers() {
        val payload = encodeBasic("TEST", LogicalByteSequence(bytes(0xf0))).value.payload.payload.copyRawBytes()
        val rawWithTrailingByte = RawTransferByteSequence(payload + 0x7f.toByte())
        val signal = assertIs<CodecResult.Success<OldSignalEncoding>>(OldSignalEncoder.encode(rawWithTrailingByte)).value
        val wav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.writeOldSignal(signal)).value.bytes.copyBytes()
        val wavWithTrailingByte = WavByteSequence(wav + 0x55.toByte())

        val decoded = decode(wavWithTrailingByte)

        assertEquals(
            listOf(CodecStage.WAV_CONTAINER, CodecStage.TRANSFER_DECODE),
            decoded.diagnostics.map { it.stage },
        )
        assertTrue(decoded.diagnostics.all { it.code == CodecDiagnosticCode.TRAILING_DATA })
    }

    @Test
    fun reportsTheStageThatPreventsEndToEndDecode() {
        val invalidWav = assertIs<CodecResult.Failure>(OldWavCodec.decode(WavByteSequence(bytes(0x00))))
        assertEquals(CodecStage.WAV_CONTAINER, invalidWav.diagnostics.last().stage)

        val silence = PcmSampleSequence(ShortArray(2000))
        val silentWav = assertIs<CodecResult.Success<WavEncoding>>(PcmWavWriter.write8BitMono(8000, silence)).value
        val invalidSignal = assertIs<CodecResult.Failure>(OldWavCodec.decode(silentWav.bytes))
        assertEquals(CodecStage.SIGNAL_DECODE, invalidSignal.diagnostics.last().stage)
    }

    private fun encodeBasic(
        filename: String,
        body: LogicalByteSequence,
        passwordProtected: Boolean = false,
    ): CodecResult.Success<OldWavEncoding> = assertIs(
        OldWavCodec.encodeBasic(filename, body, passwordProtected),
    )

    private fun decode(bytes: WavByteSequence): CodecResult.Success<OldWavDecoding> =
        assertIs(OldWavCodec.decode(bytes))

    private fun assertEncodingSizes(
        encoding: OldWavEncoding,
        headerSize: Int,
        logicalBodySize: Int,
        message: String,
    ) {
        val checksumCount = logicalBodySize / OldTransferChecksum.CHUNK_SIZE
        val expectedRawSize = headerSize + logicalBodySize + checksumCount
        val expectedSampleSize = OldSignalEncoder.LEADER_SAMPLES +
            expectedRawSize * OldSignalEncoder.SAMPLES_PER_BYTE

        assertEquals(expectedRawSize, encoding.payload.payload.size, message)
        assertEquals(expectedSampleSize, encoding.signal.samples.size, message)
        assertEquals(WAV_HEADER_SIZE + expectedSampleSize, encoding.wav.bytes.size, message)
    }

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()

    private companion object {
        const val BASIC_HEADER_SIZE: Int = 10
        const val BINARY_HEADER_SIZE: Int = 19
        const val WAV_HEADER_SIZE: Int = 44
    }
}
