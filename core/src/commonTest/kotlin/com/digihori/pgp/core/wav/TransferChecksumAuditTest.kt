package com.digihori.pgp.core.wav

import kotlin.test.*

/** Arithmetic oracle is independent of production checksum helpers; payload ranges are explicit. */
class TransferChecksumAuditTest {
    private fun swap(value: Int): Int = value / 16 + value % 16 * 16
    private fun checksum(bytes: ByteArray, highFirst: Boolean): Int {
        var sum = 0
        for (byte in bytes) {
            val value = byte.toInt() and 255
            val first = if (highFirst) value / 16 else value % 16
            val second = if (highFirst) value % 16 else value / 16
            val intermediate = sum + first
            sum = (intermediate % 256 + intermediate / 256 + second) % 256
        }
        return swap(sum)
    }
    private fun verify(raw: ByteArray, start: Int, count: Int, stored: Int, highFirst: Boolean) {
        assertEquals(checksum(raw.copyOfRange(start, start + count), highFirst), raw[stored].toInt() and 255,
            "range $start+$count stored at $stored")
    }
    private fun damaged(raw: ByteArray, positions: List<Int>, decode: (RawTransferByteSequence) -> CodecResult<*>, logicalDetails: Boolean = false) {
        for (position in positions) for (bit in 0..7) {
            val changed = raw.copyOf().also { it[position] = (it[position].toInt() xor (1 shl bit)).toByte() }
            val diagnostic = assertIs<CodecResult.Failure>(decode(RawTransferByteSequence(changed))).diagnostics.first { it.severity == CodecDiagnosticSeverity.ERROR }
            assertEquals(CodecDiagnosticCode.CHECKSUM_MISMATCH, diagnostic.code)
            assertEquals(position, diagnostic.range?.offset)
            assertEquals(if (logicalDetails) swap(raw[position].toInt() and 255) else raw[position].toInt() and 255, diagnostic.details.getValue("expected").toInt(16))
            assertEquals(if (logicalDetails) swap(changed[position].toInt() and 255) else changed[position].toInt() and 255, diagnostic.details.getValue("actual").toInt(16))
        }
    }

    @Test fun oldAccumulatorCoversEveryIncomingStateAndByte() {
        for (state in 0..255) for (value in 0..255) {
            val accumulator = OldChecksumAccumulator()
            repeat(state) { accumulator.addLogicalByte(1) }
            accumulator.addLogicalByte(value)
            val first = state + value / 16
            val expected = (first % 256 + first / 256 + value % 16) % 256
            assertEquals(expected, accumulator.value, "state $state byte $value")
        }
    }

    @Test fun oldHeadersAndCumulativeBodyMatchIndependentOracle() {
        for (length in listOf(1, 7, 8, 9, 79, 80, 81, 88, 159, 160, 161, 240)) {
            for (pattern in listOf(0, 0x0f, 0xf0, 0xff, -1)) {
                val data = ByteArray(length) { if (pattern < 0) (it * 37 + 11).toByte() else pattern.toByte() }
                val raw = assertIs<CodecResult.Success<OldPayloadEncoding>>(OldPayloadEncoder.encodeBinary("CHECK", 0x4000, LogicalByteSequence(data))).value.payload.copyRawBytes()
                verify(raw, 1, 8, 9, true)
                verify(raw, 10, 8, 18, true)
                val positions = mutableListOf(9, 18)
                var accumulatorData = byteArrayOf()
                var cursor = 19
                for (offset in data.indices step 8) {
                    if (offset % 80 == 0) accumulatorData = byteArrayOf()
                    val count = minOf(8, length - offset)
                    accumulatorData += raw.copyOfRange(cursor, cursor + count)
                    cursor += count
                    if (count == 8) {
                        // Body is already nibble-swapped: low nibble first.
                        assertEquals(checksum(accumulatorData, false), raw[cursor].toInt() and 255)
                        positions += cursor++
                    }
                }
                assertEquals(raw.size, cursor)
                damaged(raw, positions, OldPayloadDecoder::decode, logicalDetails = true)
                assertContentEquals(data, assertIs<CodecResult.Success<OldPayloadDecoding>>(OldPayloadDecoder.decode(RawTransferByteSequence(raw))).value.body.copyBytes())
            }
        }
    }

    @Test fun oldBasicNormalAndProtectedUseSameChecksumIncludingTerminator() {
        for (length in listOf(1, 7, 8, 9, 79, 80, 81, 88, 159, 160, 161)) for (protected in listOf(false, true)) {
            val body = ByteArray(length) { if (it == length - 1) 0xf0.toByte() else 0xff.toByte() }
            val raw = assertIs<CodecResult.Success<OldPayloadEncoding>>(OldPayloadEncoder.encodeBasic("CHECK", LogicalByteSequence(body), protected)).value.payload.copyRawBytes()
            verify(raw, 1, 8, 9, true)
            val positions = mutableListOf(9)
            var cursor = 10
            var cumulative = byteArrayOf()
            for (offset in body.indices step 8) {
                if (offset % 80 == 0) cumulative = byteArrayOf()
                val count = minOf(8, length - offset)
                cumulative += raw.copyOfRange(cursor, cursor + count)
                cursor += count
                if (count == 8) {
                    assertEquals(checksum(cumulative, false), raw[cursor].toInt() and 255)
                    positions += cursor++
                }
            }
            assertEquals(raw.size, cursor)
            damaged(raw, positions, OldPayloadDecoder::decode, logicalDetails = true)
            assertContentEquals(body, assertIs<CodecResult.Success<OldPayloadDecoding>>(OldPayloadDecoder.decode(RawTransferByteSequence(raw))).value.body.copyBytes())
        }
    }

    private fun s1Checks(raw: ByteArray, bodySize: Int, headerSize: Int): List<Int> {
        verify(raw, 1, 8, 9, false)
        val positions = mutableListOf(9)
        if (headerSize == 19) { verify(raw, 10, 8, 18, false); positions += 18 }
        var cursor = headerSize
        var remaining = bodySize
        while (remaining > 120) {
            verify(raw, cursor, 120, cursor + 120, false)
            positions += cursor + 120
            cursor += 121
            remaining -= 120
        }
        assertEquals(0xff, raw[cursor + remaining].toInt() and 255)
        assertEquals(0xff, raw[cursor + remaining + 1].toInt() and 255)
        verify(raw, cursor, remaining + 1, cursor + remaining + 2, false)
        positions += cursor + remaining + 2
        assertEquals(raw.size - 1, positions.last())
        return positions
    }

    @Test fun s1AndS2BinaryChecksumsIncludeFinalFullOrPartialChunk() {
        for (length in listOf(1, 119, 120, 121, 239, 240, 241, 359, 360, 361, 65536)) {
            val body = LogicalByteSequence(ByteArray(length) { (it * 37 + 11).toByte() })
            val raw = assertIs<CodecResult.Success<S1BinaryEncoding>>(S1BinaryCodec.encode("CHECK", 0, body)).value.rawBytes.copyBytes()
            val positions = s1Checks(raw, length, 19)
            assertContentEquals(body.copyBytes(), assertIs<CodecResult.Success<S1BinaryDecoding>>(S1BinaryCodec.decode(RawTransferByteSequence(raw))).value.body.copyBytes())
            if (length < 1000) damaged(raw, positions, S1BinaryCodec::decode)
        }
    }

    @Test fun allBasicTypesUseSameChunkAndTailArithmetic() {
        for (length in listOf(4, 119, 120, 121, 239, 240, 241, 258)) {
            val body = LogicalByteSequence(byteArrayOf(0, 10, (length - 3).toByte()) + ByteArray(length - 4) { 0xff.toByte() } + byteArrayOf(13))
            for (type in listOf(7, 0x17, 0x27, 0x37)) {
                val raw = if (type < 0x20) assertIs<CodecResult.Success<S1BasicEncoding>>(S1BasicCodec.encode("CHECK", body, type == 0x17)).value.rawBytes.copyBytes()
                    else assertIs<CodecResult.Success<S2BasicEncoding>>(S2BasicCodec.encode("CHECK", body, if (type == 0x27) S2BasicType.BASIC else S2BasicType.PASSWORD_PROTECTED_BASIC)).value.rawBytes.copyBytes()
                val positions = s1Checks(raw, length, 10)
                damaged(raw, positions, if (type < 0x20) S1BasicCodec::decode else S2BasicCodec::decode)
            }
        }
    }
}
