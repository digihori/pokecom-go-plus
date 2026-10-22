package com.digihori.pgp.core.wav

/** S1 binary type 67 (also used by S2 machines). All checksums are validated. */
public object S1BinaryCodec {
    public fun encode(filename: String, startAddress: Int, body: LogicalByteSequence): CodecResult<S1BinaryEncoding> {
        if (body.size !in 1..65536 || startAddress !in 0..65535 || startAddress.toLong() + body.size > 65536) {
            return failure(CodecStage.TRANSFER_ENCODE, CodecDiagnosticCode.INVALID_ARGUMENT,
                "S1 binary requires a nonempty body within the 16-bit address space")
        }
        val name = filename.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' || it == ' ' }.take(6)
        val raw = ByteArray(19 + body.size + (body.size - 1) / 120 + 3)
        raw[0] = 0x67
        name.reversed().forEachIndexed { i, c -> raw[i + 2] = c.code.toByte() }
        raw[8] = 0xf5.toByte()
        raw[9] = checksum(raw, 1, 8).toByte()
        raw[14] = (startAddress ushr 8).toByte()
        raw[15] = startAddress.toByte()
        val lengthMinusOne = body.size - 1
        raw[16] = (lengthMinusOne ushr 8).toByte()
        raw[17] = lengthMinusOne.toByte()
        raw[18] = checksum(raw, 10, 8).toByte()
        val logical = body.copyBytes()
        val mappings = mutableListOf<CodecDataMapping>()
        var cursor = 19
        var offset = 0
        // The final 1..120 bytes belong to the tail, including a full chunk.
        var lastChunkStart = cursor
        while (offset < logical.size) {
            val length = minOf(120, logical.size - offset)
            lastChunkStart = cursor
            repeat(length) { raw[cursor + it] = swap(logical[offset + it].toInt() and 255).toByte() }
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.LOGICAL_BYTES, offset, length),
                CodecDataRange(CodecDataKind.RAW_BYTES, cursor, length), "body-data")
            cursor += length
            if (offset + length < logical.size) {
                raw[cursor++] = checksum(raw, lastChunkStart, length).toByte()
            }
            offset += length
        }
        raw[cursor++] = 0xff.toByte()
        val tail = checksum(raw, lastChunkStart, cursor - lastChunkStart)
        raw[cursor++] = 0xff.toByte()
        raw[cursor] = tail.toByte()
        return CodecResult.Success(S1BinaryEncoding(RawTransferByteSequence(raw), name, startAddress, mappings))
    }

    public fun decode(input: RawTransferByteSequence): CodecResult<S1BinaryDecoding> {
        val raw = input.copyBytes()
        fun fail(code: CodecDiagnosticCode, message: String, offset: Int, length: Int = 1) =
            failure(CodecStage.TRANSFER_DECODE, code, message, offset, length)
        if (raw.size < 19) return fail(CodecDiagnosticCode.TRUNCATED_INPUT, "S1 binary header is truncated", 0, raw.size)
        if (raw[0] != 0x67.toByte()) return fail(CodecDiagnosticCode.UNSUPPORTED_FORMAT, "Expected S1 binary type 67", 0)
        if (raw[1] != 0.toByte() || raw[8] != 0xf5.toByte())
            return fail(CodecDiagnosticCode.INVALID_HEADER, "S1 binary filename markers are invalid", 1, 8)
        for ((start, count) in listOf(1 to 8, 10 to 8)) {
            checkSum(raw, start, count, start + count)?.let { return it }
        }
        if ((10..13).any { raw[it] != 0.toByte() })
            return fail(CodecDiagnosticCode.INVALID_HEADER, "S1 binary reserved metadata must be zero", 10, 4)
        var padding = false
        for (i in 2..7) {
            val c = raw[i].toInt() and 255
            if (c == 0) padding = true
            else if (padding || !(c in 65..90 || c in 48..57 || c == 32))
                return fail(CodecDiagnosticCode.INVALID_HEADER, "S1 binary filename contains invalid characters or padding", i)
        }
        val name = raw.copyOfRange(2, 8).takeWhile { it != 0.toByte() }.reversed().map { (it.toInt() and 255).toChar() }.joinToString("")
        val address = (raw[14].toInt() and 255) * 256 + (raw[15].toInt() and 255)
        val length = (raw[16].toInt() and 255) * 256 + (raw[17].toInt() and 255) + 1
        if (address.toLong() + length > 65536) return fail(CodecDiagnosticCode.INVALID_HEADER,
            "S1 binary metadata exceeds the 16-bit address space", 14, 4)
        val expectedSize = 19 + length + (length - 1) / 120 + 3
        if (raw.size < expectedSize) return fail(CodecDiagnosticCode.TRUNCATED_INPUT,
            "S1 binary body or footer is truncated", raw.size, 0)
        val body = ByteArray(length)
        val mappings = mutableListOf<CodecDataMapping>()
        var cursor = 19
        var offset = 0
        // The final 1..120 bytes belong to the tail, including a full chunk.
        var lastChunkStart = cursor
        while (offset < length) {
            val count = minOf(120, length - offset)
            lastChunkStart = cursor
            repeat(count) { body[offset + it] = swap(raw[cursor + it].toInt() and 255).toByte() }
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.RAW_BYTES, cursor, count),
                CodecDataRange(CodecDataKind.LOGICAL_BYTES, offset, count), "body-data")
            cursor += count
            if (offset + count < length) {
                checkSum(raw, lastChunkStart, count, cursor)?.let { return it }
                cursor++
            }
            offset += count
        }
        if (raw[cursor] != 0xff.toByte() || raw[cursor + 1] != 0xff.toByte())
            return fail(CodecDiagnosticCode.MISSING_TERMINATOR, "S1 binary FF FF footer is missing", cursor, 2)
        checkSum(raw, lastChunkStart, cursor - lastChunkStart + 1, cursor + 2)?.let { return it }
        cursor += 3
        if (raw.size > cursor) return fail(CodecDiagnosticCode.TRAILING_DATA,
            "S1 binary has trailing raw data", cursor, raw.size - cursor)
        return CodecResult.Success(S1BinaryDecoding(name, address, LogicalByteSequence(body), cursor, mappings))
    }

    private fun checkSum(raw: ByteArray, offset: Int, count: Int, storedAt: Int): CodecResult.Failure? {
        val expected = checksum(raw, offset, count)
        val actual = raw[storedAt].toInt() and 255
        if (actual == expected) return null
        return CodecResult.Failure(listOf(CodecDiagnostic(CodecDiagnosticSeverity.ERROR, CodecStage.TRANSFER_DECODE,
            CodecDiagnosticCode.CHECKSUM_MISMATCH, "S1 binary checksum mismatch",
            CodecDataRange(CodecDataKind.RAW_BYTES, storedAt, 1),
            mapOf("expected" to expected.toString(16).uppercase().padStart(2, '0'),
                "actual" to actual.toString(16).uppercase().padStart(2, '0')))))
    }

    /** Low-nibble carry correction, then high nibble modulo 256; stored nibble-swapped. */
    private fun checksum(raw: ByteArray, offset: Int, count: Int): Int {
        var sum = 0
        for (i in offset until offset + count) {
            val value = raw[i].toInt() and 255
            sum += value and 15
            if (sum > 255) sum = (sum + 1) and 255
            sum = (sum + (value ushr 4)) and 255
        }
        return swap(sum)
    }
    private fun swap(value: Int): Int = (value ushr 4) or ((value and 15) shl 4)
    private fun failure(stage: CodecStage, code: CodecDiagnosticCode, message: String,
        offset: Int = 0, length: Int = 0): CodecResult.Failure = CodecResult.Failure(listOf(CodecDiagnostic(
            CodecDiagnosticSeverity.ERROR, stage, code, message,
            if (stage == CodecStage.TRANSFER_DECODE) CodecDataRange(CodecDataKind.RAW_BYTES, offset, length) else null)))
}

public class S1BinaryEncoding internal constructor(
    public val rawBytes: RawTransferByteSequence,
    public val normalizedFilename: String,
    public val startAddress: Int,
    bodyMappings: List<CodecDataMapping>,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}

public class S1BinaryDecoding internal constructor(
    public val filename: String,
    public val startAddress: Int,
    public val body: LogicalByteSequence,
    public val consumedRawBytes: Int,
    bodyMappings: List<CodecDataMapping>,
) {
    public val endAddress: Int = startAddress + body.size - 1
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}
