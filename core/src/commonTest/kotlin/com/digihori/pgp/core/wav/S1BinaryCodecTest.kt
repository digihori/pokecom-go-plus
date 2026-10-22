package com.digihori.pgp.core.wav

import kotlin.test.*

class S1BinaryCodecTest {
    private fun encode(length: Int = 241, address: Int = 0x4000): S1BinaryEncoding =
        assertIs<CodecResult.Success<S1BinaryEncoding>>(S1BinaryCodec.encode("Ab-12 cXYZ", address,
            LogicalByteSequence(ByteArray(length) { (it * 37 + 18).toByte() }))).value
    private fun decode(raw: ByteArray) = S1BinaryCodec.decode(RawTransferByteSequence(raw))

    @Test fun coversChunkBoundariesAndFullAddressSpace() {
        for (length in listOf(1, 119, 120, 121, 239, 240, 241, 4112, 65536)) {
            val encoded = encode(length, if (length == 65536) 0 else 0x4000)
            val decoded = assertIs<CodecResult.Success<S1BinaryDecoding>>(decode(encoded.rawBytes.copyBytes())).value
            assertEquals(length, decoded.body.size)
            assertContentEquals(ByteArray(length) { (it * 37 + 18).toByte() }, decoded.body.copyBytes())
            assertEquals("AB12 C", decoded.filename)
            assertEquals(19 + length + (length - 1) / 120 + 3, decoded.consumedRawBytes)
            assertEquals((length + 119) / 120, decoded.bodyMappings.size)
            if (length == 65536) assertEquals(65535, decoded.endAddress)
        }
        assertEquals(65535, assertIs<CodecResult.Success<S1BinaryDecoding>>(
            decode(encode(1, 65535).rawBytes.copyBytes())).value.startAddress)
    }

    @Test fun finalFullChunkUsesTailChecksumAndRejectsLegacyExtraChecksum() {
        // All-zero data: checksum(data) = 0x00; checksum(data + first FF) = 0xE1.
        // These fixed values do not come from the production encoder.
        for (length in listOf(120, 240)) {
            val header = if (length == 120) "6700000000000000F541000000009000007771"
                else "6700000000000000F54100000000900000EF62"
            val expected = header.chunked(2).map { it.toInt(16).toByte() }.toByteArray() +
                ByteArray(length + (length - 1) / 120) + byteArrayOf(-1, -1, 0xE1.toByte())
            val encoded = assertIs<CodecResult.Success<S1BinaryEncoding>>(S1BinaryCodec.encode(
                "", 0x9000, LogicalByteSequence(ByteArray(length)))).value
            assertContentEquals(expected, encoded.rawBytes.copyBytes())
            assertContentEquals(ByteArray(length),
                assertIs<CodecResult.Success<S1BinaryDecoding>>(decode(expected)).value.body.copyBytes())
            val footer = expected.size - 3
            val legacy = expected.copyOfRange(0, footer) + byteArrayOf(0) + expected.copyOfRange(footer, expected.size)
            assertIs<CodecResult.Failure>(decode(legacy))
            val damaged = expected.copyOf().also { it[it.lastIndex] = 0 }
            assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, damaged.lastIndex, 1),
                assertIs<CodecResult.Failure>(decode(damaged)).diagnostics.single().range)
            assertEquals(CodecDiagnosticCode.TRUNCATED_INPUT,
                assertIs<CodecResult.Failure>(decode(expected.dropLast(1).toByteArray())).diagnostics.single().code)
        }
    }

    @Test fun rejectsEmptyOverflowAndInvalidStart() {
        for ((address, count) in listOf(0 to 0, -1 to 1, 65536 to 1, 65535 to 2, 0 to 65537)) {
            assertIs<CodecResult.Failure>(S1BinaryCodec.encode("TEST", address, LogicalByteSequence(ByteArray(count))))
        }
    }

    @Test fun validatesAllChecksumPositionsWithRanges() {
        val raw = encode().rawBytes.copyBytes()
        for (position in listOf(9, 18, 139, 260, raw.lastIndex)) {
            val damaged = raw.copyOf()
            damaged[position] = (damaged[position].toInt() xor 1).toByte()
            val diagnostic = assertIs<CodecResult.Failure>(decode(damaged)).diagnostics.single()
            assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, diagnostic.code)
            assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, position, 1), diagnostic.range)
            assertEquals(setOf("expected", "actual"), diagnostic.details.keys)
        }
    }

    @Test fun rejectsEveryTruncationAndTrailingBytes() {
        val raw = encode().rawBytes.copyBytes()
        for (length in raw.indices) {
            assertEquals(CodecDiagnosticCode.TRUNCATED_INPUT,
                assertIs<CodecResult.Failure>(decode(raw.copyOf(length))).diagnostics.single().code, "length $length")
        }
        assertEquals(CodecDiagnosticCode.TRAILING_DATA,
            assertIs<CodecResult.Failure>(decode(raw + byteArrayOf(0xff.toByte()))).diagnostics.single().code)
        for (offset in listOf(raw.size - 3, raw.size - 2)) {
            val damaged = raw.copyOf().also { it[offset] = 0 }
            assertEquals(CodecDiagnosticCode.MISSING_TERMINATOR,
                assertIs<CodecResult.Failure>(decode(damaged)).diagnostics.single().code)
        }
    }

    @Test fun rejectsAddressOverflowEvenWithValidMetadataChecksum() {
        val raw = encode(1, 65535).rawBytes.copyBytes()
        raw[17] = 1 // FFFF + two bytes, metadata nibble sum 3D -> stored D3.
        raw[18] = 0xd3.toByte()
        val diagnostic = assertIs<CodecResult.Failure>(decode(raw)).diagnostics.single()
        assertEquals(CodecDiagnosticCode.INVALID_HEADER, diagnostic.code)
        assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, 14, 4), diagnostic.range)
    }

    @Test fun rejectsWrongTypeAndNameMarkers() {
        for (offset in listOf(0, 1, 8)) {
            val raw = encode().rawBytes.copyBytes().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
            assertIs<CodecResult.Failure>(decode(raw))
        }
    }
}
