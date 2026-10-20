package com.digihori.pgp.core.wav

import kotlin.test.*

class S1BasicCodecTest {
    private fun body(size: Int): ByteArray = byteArrayOf(0, 10, (size - 3).toByte()) +
        ByteArray(size - 4) { 65 } + byteArrayOf(13)
    private fun encode(data: ByteArray, name: String = "Test"): S1BasicEncoding =
        assertIs<CodecResult.Success<S1BasicEncoding>>(S1BasicCodec.encode(name, LogicalByteSequence(data))).value
    private fun decode(raw: ByteArray) = S1BasicCodec.decode(RawTransferByteSequence(raw))

    @Test fun boundariesKeepLastFullChunkInTail() {
        for (size in listOf(4, 119, 120, 121, 239, 240, 241, 258)) {
            val data = body(size)
            val encoded = encode(data)
            val raw = encoded.rawBytes.copyBytes()
            assertEquals(10 + size + (size - 1) / 120 + 3, raw.size)
            val decoded = assertIs<CodecResult.Success<S1BasicDecoding>>(decode(raw)).value
            assertContentEquals(data, decoded.body.copyBytes())
            assertEquals("Test", decoded.filename)
            assertEquals(raw.size, decoded.consumedRawBytes)
            assertFalse(decoded.physicalTrailer)
            val trailer = assertIs<CodecResult.Success<S1BasicDecoding>>(decode(raw + byteArrayOf(-1))).value
            assertTrue(trailer.physicalTrailer)
            assertEquals(raw.size, trailer.consumedRawBytes)
            assertEquals(size, decoded.bodyMappings.size)
        }
    }

    @Test fun preservesFfFfInsideLineAndMarkerAdapter() {
        val data = byteArrayOf(0, 10, 3, -1, -1, 13)
        val encoded = encode(data)
        assertContentEquals(data, assertIs<CodecResult.Success<S1BasicDecoding>>(decode(encoded.rawBytes.copyBytes())).value.body.copyBytes())
        val image = byteArrayOf(-1) + data + byteArrayOf(-1)
        val adapted = assertIs<CodecResult.Success<LogicalByteSequence>>(S1BasicCodec.toTransferBody(image)).value
        assertContentEquals(data, adapted.copyBytes())
        assertContentEquals(image, assertIs<CodecResult.Success<LogicalByteSequence>>(S1BasicCodec.toProgramImage(adapted)).value.copyBytes())
        assertIs<CodecResult.Failure>(S1BasicCodec.toTransferBody(data))
    }

    @Test fun rejectsChecksumCorruptionAndEveryPayloadTruncation() {
        val raw = encode(body(241)).rawBytes.copyBytes()
        for (offset in listOf(9, 130, 251, raw.lastIndex)) {
            val corrupted = raw.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            val diagnostic = assertIs<CodecResult.Failure>(decode(corrupted)).diagnostics.single()
            assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, diagnostic.code)
            assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, offset, 1), diagnostic.range)
        }
        for (length in raw.indices) assertIs<CodecResult.Failure>(decode(raw.copyOf(length)), "length $length")
        assertIs<CodecResult.Failure>(decode(raw + byteArrayOf(-1, -1)))
        assertIs<CodecResult.Failure>(decode(raw + byteArrayOf(0)))
    }

    @Test fun validatesDecodedLineHeaderAndEolBeforeTextConversion() {
        val original = encode(body(4)).rawBytes.copyBytes()
        for ((offset, value, code) in listOf(Triple(12, 0, CodecDiagnosticCode.INVALID_HEADER),
            Triple(13, 0, CodecDiagnosticCode.MISSING_TERMINATOR), Triple(0, 0x27, CodecDiagnosticCode.UNSUPPORTED_FORMAT))) {
            val raw = original.copyOf().also { it[offset] = value.toByte() }
            assertEquals(code, assertIs<CodecResult.Failure>(decode(raw)).diagnostics.single().code)
        }
        val maximumLine = byteArrayOf(0xfe.toByte(), 0xff.toByte(), 1, 13)
        assertContentEquals(maximumLine, assertIs<CodecResult.Success<S1BasicDecoding>>(
            decode(encode(maximumLine).rawBytes.copyBytes())).value.body.copyBytes())
    }

    @Test fun rejectsInvalidFilenameAndLineRecords() {
        for (name in listOf("12345678", "日本語", "A\nB")) assertIs<CodecResult.Failure>(
            S1BasicCodec.encode(name, LogicalByteSequence(body(4))))
        assertEquals("1234567", encode(body(4), "1234567").filename)
        for (data in listOf(byteArrayOf(), byteArrayOf(0, 0, 1, 13), byteArrayOf(-1, 0, 1, 13),
            byteArrayOf(0, 10, 0), byteArrayOf(0, 10, 2, 13), byteArrayOf(0, 10, 1, 0))) {
            assertIs<CodecResult.Failure>(S1BasicCodec.encode("TEST", LogicalByteSequence(data)))
        }
    }
}
