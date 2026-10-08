package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OldPayloadDecoderTest {
    @Test
    fun decodesBasicReferencePayload() {
        val raw = bytes(
            0x02, 0x00, 0x45, 0x44, 0x42, 0x41, 0x53, 0x60, 0xf5, 0xe3,
            0x0e, 0x01, 0x1c, 0x15, 0x00, 0x0f,
        )

        val result = success(OldPayloadDecoder.decode(RawTransferByteSequence(raw)))

        assertEquals(OldPayloadType.BASIC, result.value.type)
        assertEquals("PC1245", result.value.filename)
        assertFalse(result.value.passwordProtected)
        assertContentEquals(bytes(0xe0, 0x10, 0xc1, 0x51, 0x00, 0xf0), result.value.body.copyBytes())
        assertEquals(raw.size, result.value.consumedRawBytes)
        assertEquals(1, result.value.bodyMappings.size)
    }

    @Test
    fun decodesBinaryReferencePayload() {
        val raw = bytes(
            0x62, 0x00, 0x00, 0x00, 0x64, 0x63, 0x55, 0x64, 0xf5, 0xb3,
            0x00, 0x00, 0x00, 0x60, 0xc0, 0x00, 0x00, 0x13, 0x61,
            0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0xc1,
            0x80, 0x90, 0xa0, 0xb0, 0xc0, 0xd0, 0xe0, 0xf0, 0x87,
            0x01, 0x11, 0x21, 0x31,
        )

        val result = success(OldPayloadDecoder.decode(RawTransferByteSequence(raw)))

        assertEquals(OldPayloadType.BINARY, result.value.type)
        assertEquals("TEST", result.value.filename)
        assertEquals(0xc000, result.value.startAddress)
        assertEquals(0x13, result.value.endOffset)
        assertEquals(0xc013, result.value.endAddress)
        assertContentEquals(ByteArray(20) { it.toByte() }, result.value.body.copyBytes())
        assertContentEquals(bytes(0x00, 0x00, 0x00, 0x60), result.value.copyReservedMetadata())
    }

    @Test
    fun roundTripsEightyByteChecksumBoundaryAndPasswordType() {
        val logical = ByteArray(81) { if (it == 80) 0xf0.toByte() else 0xff.toByte() }
        val encoded = assertIs<CodecResult.Success<OldPayloadEncoding>>(
            OldPayloadEncoder.encodeBasic(
                filename = "BOUNDARY",
                body = LogicalByteSequence(logical),
                passwordProtected = true,
            ),
        ).value

        val decoded = success(
            OldPayloadDecoder.decode(RawTransferByteSequence(encoded.payload.copyRawBytes())),
        )

        assertEquals(OldPayloadType.PASSWORD_PROTECTED_BASIC, decoded.value.type)
        assertTrue(decoded.value.passwordProtected)
        assertContentEquals(logical, decoded.value.body.copyBytes())
    }

    @Test
    fun roundTripsBasicAndBinaryChunkBoundaries() {
        listOf(1, 7, 8, 9, 79, 80, 81, 88).forEach { size ->
            val basicBody = ByteArray(size) { index ->
                if (index == size - 1) 0xf0.toByte() else (index and 0x7f).toByte()
            }
            val basic = success(
                OldPayloadDecoder.decode(RawTransferByteSequence(basicPayload(basicBody))),
            )
            assertContentEquals(basicBody, basic.value.body.copyBytes(), "BASIC size $size")

            val binaryBody = ByteArray(size) { it.toByte() }
            val binary = success(
                OldPayloadDecoder.decode(RawTransferByteSequence(binaryPayload(binaryBody))),
            )
            assertContentEquals(binaryBody, binary.value.body.copyBytes(), "binary size $size")
        }
    }

    @Test
    fun reportsHeaderAndBodyChecksumOffsets() {
        val encoded = binaryPayload(ByteArray(16) { it.toByte() })
        val nameCorrupt = encoded.copyOf().also { it[9] = (it[9].toInt() xor 1).toByte() }
        val nameFailure = failure(OldPayloadDecoder.decode(RawTransferByteSequence(nameCorrupt)))
        assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, nameFailure.code)
        assertEquals(9, nameFailure.range?.offset)

        val bodyCorrupt = encoded.copyOf().also { it[27] = (it[27].toInt() xor 1).toByte() }
        val bodyFailure = failure(OldPayloadDecoder.decode(RawTransferByteSequence(bodyCorrupt)))
        assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, bodyFailure.code)
        assertEquals(27, bodyFailure.range?.offset)
    }

    @Test
    fun stopsAtDeclaredOrTerminatedBodyAndReportsTrailingRawData() {
        val basic = basicPayload(bytes(0xe0, 0x10, 0xd4, 0x00, 0xf0)) + bytes(0xaa, 0xbb)
        val basicResult = success(OldPayloadDecoder.decode(RawTransferByteSequence(basic)))
        assertEquals(basic.size - 2, basicResult.value.consumedRawBytes)
        assertEquals(CodecDiagnosticCode.TRAILING_DATA, basicResult.diagnostics.single().code)
        assertEquals(basic.size - 2, basicResult.diagnostics.single().range?.offset)

        val binary = binaryPayload(bytes(1, 2, 3)) + bytes(0xcc)
        val binaryResult = success(OldPayloadDecoder.decode(RawTransferByteSequence(binary)))
        assertContentEquals(bytes(1, 2, 3), binaryResult.value.body.copyBytes())
        assertEquals(CodecDiagnosticCode.TRAILING_DATA, binaryResult.diagnostics.single().code)
    }

    @Test
    fun reportsMissingTerminatorAndTruncatedBinaryBody() {
        val basicWithoutTerminator = basicPayloadUnchecked(bytes(1, 2, 3))
        assertEquals(
            CodecDiagnosticCode.MISSING_TERMINATOR,
            failure(OldPayloadDecoder.decode(RawTransferByteSequence(basicWithoutTerminator))).code,
        )

        val binary = binaryPayload(bytes(1, 2, 3, 4))
        val truncated = binary.copyOf(binary.size - 1)
        assertEquals(
            CodecDiagnosticCode.TRUNCATED_INPUT,
            failure(OldPayloadDecoder.decode(RawTransferByteSequence(truncated))).code,
        )
    }

    @Test
    fun warnsAboutUndefinedFilenameBytesAndReservedMetadata() {
        val raw = binaryPayload(bytes(1)).copyOf()
        raw[1] = 0x20
        rewriteChecksum(raw, dataOffset = 1, checksumOffset = 9)
        raw[10] = 0x01
        rewriteChecksum(raw, dataOffset = 10, checksumOffset = 18)

        val result = success(OldPayloadDecoder.decode(RawTransferByteSequence(raw)))

        assertTrue(result.value.filename.endsWith('?'))
        assertEquals(
            listOf(CodecDiagnosticCode.UNDEFINED_BYTE, CodecDiagnosticCode.INVALID_HEADER),
            result.diagnostics.map(CodecDiagnostic::code),
        )
    }

    @Test
    fun rejectsUnknownTypeAndInvalidAddressMetadata() {
        val unknown = basicPayload(bytes(0xf0)).copyOf().also { it[0] = 0x7f }
        assertEquals(
            CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            failure(OldPayloadDecoder.decode(RawTransferByteSequence(unknown))).code,
        )

        val invalidAddress = binaryPayload(bytes(1)).copyOf()
        invalidAddress[14] = 0xff.toByte()
        invalidAddress[15] = 0xff.toByte()
        invalidAddress[16] = 0x00
        invalidAddress[17] = 0x01
        rewriteChecksum(invalidAddress, dataOffset = 10, checksumOffset = 18)
        assertEquals(
            CodecDiagnosticCode.INVALID_HEADER,
            failure(OldPayloadDecoder.decode(RawTransferByteSequence(invalidAddress))).code,
        )
    }

    private fun basicPayload(body: ByteArray): ByteArray =
        assertIs<CodecResult.Success<OldPayloadEncoding>>(
            OldPayloadEncoder.encodeBasic("TEST", LogicalByteSequence(body)),
        ).value.payload.copyRawBytes()

    private fun basicPayloadUnchecked(body: ByteArray): ByteArray {
        val valid = basicPayload(bytes(0xf0))
        return valid.copyOf(10) + OldTransferChecksum.encodeBody(LogicalByteSequence(body)).copyBytes()
    }

    private fun binaryPayload(body: ByteArray): ByteArray =
        assertIs<CodecResult.Success<OldPayloadEncoding>>(
            OldPayloadEncoder.encodeBinary("TEST", 0xc000, LogicalByteSequence(body)),
        ).value.payload.copyRawBytes()

    private fun rewriteChecksum(raw: ByteArray, dataOffset: Int, checksumOffset: Int) {
        val logical = raw.copyOfRange(dataOffset, checksumOffset)
        raw[checksumOffset] = OldTransferChecksum.nibbleSwap(
            OldTransferChecksum.calculate(LogicalByteSequence(logical)),
        ).toByte()
    }

    private fun success(result: CodecResult<OldPayloadDecoding>): CodecResult.Success<OldPayloadDecoding> =
        assertIs(result)

    private fun failure(result: CodecResult<OldPayloadDecoding>): CodecDiagnostic =
        assertIs<CodecResult.Failure>(result).diagnostics.single()

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
