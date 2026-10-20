package com.digihori.pgp.core.wav

/** S1 type 07/17 cassette payload. Token interpretation remains machine-specific. */
public object S1BasicCodec {
    public fun encode(filename: String, body: LogicalByteSequence, passwordProtected: Boolean = false): CodecResult<S1BasicEncoding> = encodeTyped(filename, body, if (passwordProtected) 0x17 else 7)

    internal fun encodeTyped(filename: String, body: LogicalByteSequence, type: Int): CodecResult<S1BasicEncoding> {
        if (filename.length > 7 || filename.any { it.code !in 0x20..0x7e })
            return failure(CodecStage.TRANSFER_ENCODE, CodecDiagnosticCode.INVALID_ARGUMENT,
                "S1 BASIC filename must contain at most seven printable ASCII characters")
        val logical = body.copyBytes()
        validateBody(logical)?.let { return failure(CodecStage.TRANSFER_ENCODE,
            CodecDiagnosticCode.INVALID_ARGUMENT, it.second, it.first, 0, CodecDataKind.LOGICAL_BYTES) }
        val raw = ByteArray(10 + logical.size + (logical.size - 1) / 120 + 3)
        raw[0] = type.toByte()
        filename.padEnd(7, '\u0000').reversed().forEachIndexed { i, c -> raw[i + 1] = c.code.toByte() }
        raw[8] = 0xf5.toByte()
        raw[9] = checksum(raw.copyOfRange(1, 9)).toByte()
        val mappings = mutableListOf<CodecDataMapping>()
        var offset = 0
        var cursor = 10
        var lastStart = cursor
        while (offset < logical.size) {
            val length = minOf(120, logical.size - offset)
            lastStart = cursor
            repeat(length) { raw[cursor + it] = swap(logical[offset + it].toInt() and 255).toByte() }
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.LOGICAL_BYTES, offset, length),
                CodecDataRange(CodecDataKind.RAW_BYTES, cursor, length), "body-data")
            cursor += length
            offset += length
            if (offset < logical.size) {
                raw[cursor] = checksum(raw.copyOfRange(lastStart, cursor)).toByte()
                cursor++
            }
        }
        raw[cursor++] = 0xff.toByte()
        val tail = checksum(raw.copyOfRange(lastStart, cursor))
        raw[cursor++] = 0xff.toByte()
        raw[cursor] = tail.toByte()
        return CodecResult.Success(S1BasicEncoding(RawTransferByteSequence(raw), filename, mappings, type == 0x17))
    }

    /** Accepts a payload alone or its single optional physical FF trailer. Never scans inside line data. */
    public fun decode(input: RawTransferByteSequence): CodecResult<S1BasicDecoding> = decodeTyped(input, if (input.size > 0 && input.unsignedByteAt(0) == 0x17) 0x17 else 7)

    internal fun decodeTyped(input: RawTransferByteSequence, type: Int): CodecResult<S1BasicDecoding> {
        val raw = input.copyBytes()
        fun fail(code: CodecDiagnosticCode, message: String, offset: Int, length: Int = 0) =
            failure(CodecStage.TRANSFER_DECODE, code, message, offset, length)
        if (raw.size < 10) return fail(CodecDiagnosticCode.TRUNCATED_INPUT, "S1 BASIC header is truncated", 0, raw.size)
        if (raw[0] != type.toByte()) return fail(CodecDiagnosticCode.UNSUPPORTED_FORMAT, "Expected S1 BASIC type 07", 0, 1)
        if (raw[8] != 0xf5.toByte()) return fail(CodecDiagnosticCode.INVALID_HEADER, "S1 BASIC filename terminator is missing", 8, 1)
        checksumFailure(raw.copyOfRange(1, 9), raw, 9)?.let { return it }
        val nameBytes = raw.copyOfRange(1, 8).reversed()
        var padding = false
        for ((index, byte) in nameBytes.withIndex()) {
            val code = byte.toInt() and 255
            if (code == 0) padding = true
            else if (padding || code !in 0x20..0x7e) return fail(CodecDiagnosticCode.INVALID_HEADER,
                "S1 BASIC filename characters or padding are invalid", 7 - index, 1)
        }
        val filename = nameBytes.takeWhile { it != 0.toByte() }.map { (it.toInt() and 255).toChar() }.joinToString("")
        val logical = mutableListOf<Byte>()
        val chunk = mutableListOf<Byte>()
        val mappings = mutableListOf<CodecDataMapping>()
        var cursor = 10
        var error: CodecResult.Failure? = null
        fun read(): Int? {
            if (chunk.size == 120) {
                if (cursor >= raw.size) {
                    error = fail(CodecDiagnosticCode.TRUNCATED_INPUT, "S1 BASIC chunk checksum is missing", cursor)
                    return null
                }
                checksumFailure(chunk.toByteArray(), raw, cursor)?.let { error = it; return null }
                cursor++
                chunk.clear()
            }
            if (cursor >= raw.size) {
                error = fail(CodecDiagnosticCode.TRUNCATED_INPUT, "S1 BASIC line is truncated", cursor)
                return null
            }
            chunk += raw[cursor]
            val value = swap(raw[cursor].toInt() and 255)
            mappings += CodecDataMapping(CodecDataRange(CodecDataKind.RAW_BYTES, cursor++, 1),
                CodecDataRange(CodecDataKind.LOGICAL_BYTES, logical.size, 1), "body-data")
            logical += value.toByte()
            return value
        }
        while (true) {
            // FF cannot begin a valid line number (maximum FEFF). Only inspect at a line boundary.
            if (cursor + 1 < raw.size && raw[cursor] == 0xff.toByte() && raw[cursor + 1] == 0xff.toByte()) break
            if (cursor >= raw.size) return fail(CodecDiagnosticCode.MISSING_TERMINATOR, "S1 BASIC FF FF footer is missing", cursor)
            val start = cursor
            val high = read() ?: return requireNotNull(error)
            val low = read() ?: return requireNotNull(error)
            val length = read() ?: return requireNotNull(error)
            if (high * 256 + low !in 1..65279 || length == 0) return fail(CodecDiagnosticCode.INVALID_HEADER,
                "S1 BASIC line number or length is invalid", start, cursor - start)
            repeat(length) { index ->
                val value = read() ?: return requireNotNull(error)
                if (index == length - 1 && value != 13) return fail(CodecDiagnosticCode.MISSING_TERMINATOR,
                    "S1 BASIC line must end with 0D", cursor - 1, 1)
            }
            if (logical.size > 65536) return fail(CodecDiagnosticCode.INVALID_ARGUMENT, "S1 BASIC body exceeds 65536 bytes", start)
        }
        if (logical.isEmpty()) return fail(CodecDiagnosticCode.INVALID_HEADER, "S1 BASIC body must contain a line", cursor)
        if (cursor + 2 >= raw.size) return fail(CodecDiagnosticCode.TRUNCATED_INPUT, "S1 BASIC tail checksum is missing", raw.size)
        checksumFailure(chunk.toByteArray() + byteArrayOf(0xff.toByte()), raw, cursor + 2)?.let { return it }
        cursor += 3
        val trailer = cursor < raw.size && raw[cursor] == 0xff.toByte()
        val consumed = cursor + if (trailer) 1 else 0
        if (consumed != raw.size) return fail(CodecDiagnosticCode.TRAILING_DATA,
            "S1 BASIC has trailing raw data", consumed, raw.size - consumed)
        return CodecResult.Success(S1BasicDecoding(filename, LogicalByteSequence(logical.toByteArray()), cursor, trailer, mappings, type == 0x17))
    }

    /** Converts FF + line records + FF to cassette body; no tokens are rewritten. */
    public fun toTransferBody(program: ByteArray): CodecResult<LogicalByteSequence> {
        if (program.size < 2 || program.first() != 0xff.toByte() || program.last() != 0xff.toByte())
            return failure(CodecStage.PROGRAM_ENCODE, CodecDiagnosticCode.INVALID_HEADER, "S1 BASIC image must begin and end with FF")
        val body = program.copyOfRange(1, program.lastIndex)
        validateBody(body)?.let { return failure(CodecStage.PROGRAM_ENCODE, CodecDiagnosticCode.INVALID_HEADER,
            it.second, it.first, 0, CodecDataKind.LOGICAL_BYTES) }
        return CodecResult.Success(LogicalByteSequence(body))
    }

    public fun toProgramImage(body: LogicalByteSequence): CodecResult<LogicalByteSequence> {
        val bytes = body.copyBytes()
        validateBody(bytes)?.let { return failure(CodecStage.PROGRAM_DECODE, CodecDiagnosticCode.INVALID_HEADER,
            it.second, it.first, 0, CodecDataKind.LOGICAL_BYTES) }
        return CodecResult.Success(LogicalByteSequence(byteArrayOf(0xff.toByte()) + bytes + byteArrayOf(0xff.toByte())))
    }

    private fun validateBody(body: ByteArray): Pair<Int, String>? {
        if (body.isEmpty() || body.size > 65536) return 0 to "S1 BASIC body must contain 1..65536 bytes of line records"
        var cursor = 0
        while (cursor < body.size) {
            if (cursor + 3 > body.size) return cursor to "S1 BASIC line header is truncated"
            val number = (body[cursor].toInt() and 255) * 256 + (body[cursor + 1].toInt() and 255)
            val length = body[cursor + 2].toInt() and 255
            if (number !in 1..65279 || length == 0) return cursor to "S1 BASIC line number or length is invalid"
            if (cursor + 3 + length > body.size) return cursor to "S1 BASIC line is truncated"
            cursor += 3 + length
            if (body[cursor - 1] != 13.toByte()) return cursor - 1 to "S1 BASIC line must end with 0D"
        }
        return null
    }

    private fun checksum(bytes: ByteArray): Int {
        var sum = 0
        for (byte in bytes) {
            val value = byte.toInt() and 255
            sum += value and 15
            if (sum > 255) sum = (sum + 1) and 255
            sum = (sum + (value ushr 4)) and 255
        }
        return swap(sum)
    }
    private fun swap(value: Int): Int = (value ushr 4) or ((value and 15) shl 4)
    private fun checksumFailure(data: ByteArray, raw: ByteArray, offset: Int): CodecResult.Failure? {
        val expected = checksum(data)
        val actual = raw[offset].toInt() and 255
        if (actual == expected) return null
        return CodecResult.Failure(listOf(CodecDiagnostic(CodecDiagnosticSeverity.ERROR, CodecStage.TRANSFER_DECODE,
            CodecDiagnosticCode.CHECKSUM_MISMATCH, "S1 BASIC checksum mismatch",
            CodecDataRange(CodecDataKind.RAW_BYTES, offset, 1),
            mapOf("expected" to expected.toString(16).uppercase().padStart(2, '0'), "actual" to actual.toString(16).uppercase().padStart(2, '0')))))
    }
    private fun failure(stage: CodecStage, code: CodecDiagnosticCode, message: String, offset: Int = 0,
        length: Int = 0, kind: CodecDataKind = CodecDataKind.RAW_BYTES): CodecResult.Failure = CodecResult.Failure(listOf(
        CodecDiagnostic(CodecDiagnosticSeverity.ERROR, stage, code, message, CodecDataRange(kind, offset, length))))
}

public class S1BasicEncoding internal constructor(
    public val rawBytes: RawTransferByteSequence,
    public val filename: String,
    bodyMappings: List<CodecDataMapping>,
    public val passwordProtected: Boolean = false,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}

public class S1BasicDecoding internal constructor(
    public val filename: String,
    public val body: LogicalByteSequence,
    public val consumedRawBytes: Int,
    public val physicalTrailer: Boolean,
    bodyMappings: List<CodecDataMapping>,
    public val passwordProtected: Boolean = false,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}
