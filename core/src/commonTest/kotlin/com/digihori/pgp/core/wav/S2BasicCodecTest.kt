package com.digihori.pgp.core.wav

import kotlin.test.*

class S2BasicCodecTest {
    private fun body(size: Int) = byteArrayOf(0, 10, (size - 3).toByte()) + ByteArray(size - 4) { 65 } + byteArrayOf(13)
    @Test fun retainsTypeWithoutChangingBodyOrChecksums() {
        for (size in listOf(4, 119, 120, 121, 239, 240, 241, 258)) {
            val normal = assertIs<CodecResult.Success<S2BasicEncoding>>(S2BasicCodec.encode("Test", LogicalByteSequence(body(size)))).value
            val protected = assertIs<CodecResult.Success<S2BasicEncoding>>(S2BasicCodec.encode("Test", LogicalByteSequence(body(size)), S2BasicType.PASSWORD_PROTECTED_BASIC)).value
            assertEquals(0x27, normal.rawBytes.unsignedByteAt(0))
            assertEquals(0x37, protected.rawBytes.unsignedByteAt(0))
            assertContentEquals(normal.rawBytes.copyBytes().drop(1), protected.rawBytes.copyBytes().drop(1))
            for (encoded in listOf(normal, protected)) {
                val decoded = assertIs<CodecResult.Success<S2BasicDecoding>>(S2BasicCodec.decode(
                    RawTransferByteSequence(encoded.rawBytes.copyBytes() + byteArrayOf(-1)))).value
                assertEquals(encoded.type, decoded.type)
                assertTrue(decoded.physicalTrailer)
                assertContentEquals(body(size), decoded.body.copyBytes())
            }
        }
    }
    @Test fun validatesEnvelopeForBothTypes() {
        for (type in S2BasicType.entries) {
            val raw = assertIs<CodecResult.Success<S2BasicEncoding>>(S2BasicCodec.encode("TEST", LogicalByteSequence(body(241)), type)).value.rawBytes.copyBytes()
            for (offset in listOf(9, 130, 251, raw.lastIndex)) {
                val corrupted = raw.copyOf().also { it[offset] = (it[offset].toInt() xor 1).toByte() }
                val diagnostic = assertIs<CodecResult.Failure>(S2BasicCodec.decode(RawTransferByteSequence(corrupted))).diagnostics.single()
                assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, diagnostic.code)
                assertEquals(CodecDataRange(CodecDataKind.RAW_BYTES, offset, 1), diagnostic.range)
                assertTrue(diagnostic.message.startsWith("S2 BASIC"))
            }
            for (length in raw.indices) assertIs<CodecResult.Failure>(S2BasicCodec.decode(RawTransferByteSequence(raw.copyOf(length))))
            assertIs<CodecResult.Failure>(S2BasicCodec.decode(RawTransferByteSequence(raw + byteArrayOf(-1, -1))))
        }
        for (type in listOf(7, 0x67, 0x17)) assertIs<CodecResult.Failure>(S2BasicCodec.decode(RawTransferByteSequence(byteArrayOf(type.toByte()))))
    }
}
