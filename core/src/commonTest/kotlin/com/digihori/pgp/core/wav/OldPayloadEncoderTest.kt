package com.digihori.pgp.core.wav

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class OldPayloadEncoderTest {
    @Test
    fun encodesBasicReferencePayload() {
        val result = success(
            OldPayloadEncoder.encodeBasic(
                filename = "PC1245.BAS",
                body = LogicalByteSequence(bytes(0xe0, 0x10, 0xc1, 0x51, 0x00, 0xf0)),
            ),
        )

        assertEquals("PC1245", result.normalizedFilename)
        assertNull(result.startAddress)
        assertNull(result.endOffset)
        assertContentEquals(
            bytes(
                0x02, 0x00, 0x45, 0x44, 0x42, 0x41, 0x53, 0x60, 0xf5, 0xe3,
                0x0e, 0x01, 0x1c, 0x15, 0x00, 0x0f,
            ),
            result.payload.copyRawBytes(),
        )
        assertEquals(1, result.bodyMappings.size)
        assertEquals(10, result.bodyMappings[0].target.offset)
    }

    @Test
    fun encodesPasswordProtectedBasicType() {
        val result = success(
            OldPayloadEncoder.encodeBasic(
                filename = "SECRET",
                body = LogicalByteSequence(byteArrayOf(0xf0.toByte())),
                passwordProtected = true,
            ),
        )

        assertEquals(OldPayloadType.PASSWORD_PROTECTED_BASIC, result.payload.type)
        assertEquals(0x12, result.payload.copyRawBytes()[0].toInt() and 0xff)
    }

    @Test
    fun encodesBinaryReferencePayload() {
        val result = success(
            OldPayloadEncoder.encodeBinary(
                filename = "TEST.BIN",
                startAddress = 0xc000,
                body = LogicalByteSequence(ByteArray(20) { it.toByte() }),
            ),
        )

        assertEquals("TEST", result.normalizedFilename)
        assertEquals(0xc000, result.startAddress)
        assertEquals(0x0013, result.endOffset)
        assertContentEquals(
            bytes(
                0x62, 0x00, 0x00, 0x00, 0x64, 0x63, 0x55, 0x64, 0xf5, 0xb3,
                0x00, 0x00, 0x00, 0x60, 0xc0, 0x00, 0x00, 0x13, 0x61,
                0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0xc1,
                0x80, 0x90, 0xa0, 0xb0, 0xc0, 0xd0, 0xe0, 0xf0, 0x87,
                0x01, 0x11, 0x21, 0x31,
            ),
            result.payload.copyRawBytes(),
        )
        assertEquals(19, result.bodyMappings.first().target.offset)
    }

    @Test
    fun normalizesAndPadsFilenameLikeReferenceImplementation() {
        val result = success(
            OldPayloadEncoder.encodeBasic(
                filename = "a-b_123456789.bas",
                body = LogicalByteSequence(byteArrayOf(0xf0.toByte())),
            ),
        )

        assertEquals("AB12345", result.normalizedFilename)
        assertContentEquals(
            bytes(0x02, 0x45, 0x44, 0x43, 0x42, 0x41, 0x52, 0x51, 0xf5, 0x44, 0x0f),
            result.payload.copyRawBytes(),
        )
    }

    @Test
    fun rejectsMissingAndEarlyBasicTerminators() {
        val missing = failure(
            OldPayloadEncoder.encodeBasic("TEST", LogicalByteSequence(byteArrayOf(0x01))),
        )
        assertEquals(CodecDiagnosticCode.MISSING_TERMINATOR, missing.code)

        val trailing = failure(
            OldPayloadEncoder.encodeBasic(
                "TEST",
                LogicalByteSequence(bytes(0x01, 0xf0, 0x02)),
            ),
        )
        assertEquals(CodecDiagnosticCode.TRAILING_DATA, trailing.code)
        assertEquals(2, trailing.range?.offset)
        assertEquals(1, trailing.range?.length)
    }

    @Test
    fun rejectsInvalidBinaryRangesWithStructuredDiagnostics() {
        val empty = failure(
            OldPayloadEncoder.encodeBinary("TEST", 0xc000, LogicalByteSequence(ByteArray(0))),
        )
        assertEquals(CodecStage.TRANSFER_ENCODE, empty.stage)
        assertEquals(CodecDiagnosticCode.INVALID_ARGUMENT, empty.code)

        failure(OldPayloadEncoder.encodeBinary("TEST", -1, LogicalByteSequence(byteArrayOf(1))))
        failure(OldPayloadEncoder.encodeBinary("TEST", 0xffff, LogicalByteSequence(byteArrayOf(1, 2))))
        failure(OldPayloadEncoder.encodeBinary("TEST", 0, LogicalByteSequence(ByteArray(0x10001))))
    }

    private fun success(result: CodecResult<OldPayloadEncoding>): OldPayloadEncoding =
        assertIs<CodecResult.Success<OldPayloadEncoding>>(result).value

    private fun failure(result: CodecResult<OldPayloadEncoding>): CodecDiagnostic =
        assertIs<CodecResult.Failure>(result).diagnostics.single()

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
