package com.digihori.pgp.core.wav

/** Encodes logical OLD BASIC or binary bodies into raw transfer payloads. */
public object OldPayloadEncoder {
    public fun encodeBasic(
        filename: String,
        body: LogicalByteSequence,
        passwordProtected: Boolean = false,
    ): CodecResult<OldPayloadEncoding> {
        val logicalBody = body.copyBytes()
        val terminatorOffset = logicalBody.indexOfFirst { (it.toInt() and 0xff) == BASIC_TERMINATOR }
        if (terminatorOffset < 0) {
            return failure(
                message = "OLD BASIC body must end with F0",
                code = CodecDiagnosticCode.MISSING_TERMINATOR,
            )
        }
        if (terminatorOffset != logicalBody.lastIndex) {
            return failure(
                message = "OLD BASIC body contains data after its F0 terminator",
                code = CodecDiagnosticCode.TRAILING_DATA,
                range = CodecDataRange(
                    CodecDataKind.LOGICAL_BYTES,
                    terminatorOffset + 1,
                    logicalBody.size - terminatorOffset - 1,
                ),
            )
        }
        val type = if (passwordProtected) {
            OldPayloadType.PASSWORD_PROTECTED_BASIC
        } else {
            OldPayloadType.BASIC
        }
        val normalizedName = normalizeFilename(filename)
        val bodyRaw = OldTransferChecksum.encodeBody(body).copyBytes()
        val bodyStart = TYPE_SIZE + NAME_SECTION_SIZE
        val raw = ByteArray(bodyStart + bodyRaw.size)

        raw[0] = type.rawValue.toByte()
        writeNameSection(raw, TYPE_SIZE, normalizedName.rawBytes)
        bodyRaw.copyInto(raw, bodyStart)

        return CodecResult.Success(
            OldPayloadEncoding(
                payload = OldTransferPayload(type, RawTransferByteSequence(raw)),
                normalizedFilename = normalizedName.value,
                bodyMappings = bodyMappings(body.size, bodyStart),
            ),
        )
    }

    public fun encodeBinary(
        filename: String,
        startAddress: Int,
        body: LogicalByteSequence,
    ): CodecResult<OldPayloadEncoding> {
        if (body.size == 0) {
            return failure("OLD binary body must not be empty")
        }
        if (startAddress !in 0..0xffff) {
            return failure("OLD binary start address must be between 0000 and FFFF")
        }
        val endOffset = body.size - 1
        if (endOffset > 0xffff) {
            return failure("OLD binary body exceeds the 16-bit end offset")
        }
        if (startAddress.toLong() + endOffset.toLong() > 0xffffL) {
            return failure("OLD binary body exceeds the 16-bit address space")
        }

        val normalizedName = normalizeFilename(filename)
        val bodyRaw = OldTransferChecksum.encodeBody(body).copyBytes()
        val bodyStart = TYPE_SIZE + NAME_SECTION_SIZE + METADATA_SECTION_SIZE
        val raw = ByteArray(bodyStart + bodyRaw.size)

        raw[0] = OldPayloadType.BINARY.rawValue.toByte()
        writeNameSection(raw, TYPE_SIZE, normalizedName.rawBytes)
        writeMetadataSection(raw, TYPE_SIZE + NAME_SECTION_SIZE, startAddress, endOffset)
        bodyRaw.copyInto(raw, bodyStart)

        return CodecResult.Success(
            OldPayloadEncoding(
                payload = OldTransferPayload(OldPayloadType.BINARY, RawTransferByteSequence(raw)),
                normalizedFilename = normalizedName.value,
                startAddress = startAddress,
                endOffset = endOffset,
                bodyMappings = bodyMappings(body.size, bodyStart),
            ),
        )
    }

    private fun writeNameSection(target: ByteArray, offset: Int, filenameRaw: ByteArray) {
        val nameBlock = ByteArray(NAME_BLOCK_SIZE)
        filenameRaw.copyInto(nameBlock)
        nameBlock[NAME_BYTES] = NAME_TERMINATOR.toByte()
        nameBlock.copyInto(target, offset)
        target[offset + NAME_BLOCK_SIZE] = OldTransferChecksum.nibbleSwap(
            OldTransferChecksum.calculate(LogicalByteSequence(nameBlock)),
        ).toByte()
    }

    private fun writeMetadataSection(target: ByteArray, offset: Int, startAddress: Int, endOffset: Int) {
        val metadata = byteArrayOf(
            0x00,
            0x00,
            0x00,
            0x60,
            (startAddress ushr 8).toByte(),
            startAddress.toByte(),
            (endOffset ushr 8).toByte(),
            endOffset.toByte(),
        )
        metadata.copyInto(target, offset)
        target[offset + METADATA_SIZE] = OldTransferChecksum.nibbleSwap(
            OldTransferChecksum.calculate(LogicalByteSequence(metadata)),
        ).toByte()
    }

    private fun normalizeFilename(filename: String): NormalizedFilename {
        val uppercase = filename.uppercase()
        val extensionSeparator = uppercase.lastIndexOf('.')
        val withoutExtension = if (extensionSeparator >= 0 && extensionSeparator < uppercase.lastIndex) {
            uppercase.substring(0, extensionSeparator)
        } else {
            uppercase
        }
        val value = withoutExtension.filter { it in 'A'..'Z' || it in '0'..'9' }.take(NAME_BYTES)
        val reversedCodes = value.reversed().map(::filenameCharacterCode)
        val raw = ByteArray(NAME_BYTES)
        reversedCodes.forEachIndexed { index, code ->
            raw[NAME_BYTES - reversedCodes.size + index] = code.toByte()
        }
        return NormalizedFilename(value, raw)
    }

    private fun filenameCharacterCode(character: Char): Int = when (character) {
        in '0'..'9' -> 0x40 + (character - '0')
        in 'A'..'Z' -> 0x51 + (character - 'A')
        else -> error("Filename must be normalized before character encoding")
    }

    private fun bodyMappings(logicalSize: Int, rawBodyStart: Int): List<CodecDataMapping> = buildList {
        var logicalOffset = 0
        while (logicalOffset < logicalSize) {
            val length = minOf(OldTransferChecksum.CHUNK_SIZE, logicalSize - logicalOffset)
            add(
                CodecDataMapping(
                    source = CodecDataRange(CodecDataKind.LOGICAL_BYTES, logicalOffset, length),
                    target = CodecDataRange(
                        CodecDataKind.RAW_BYTES,
                        rawBodyStart + logicalOffset + logicalOffset / OldTransferChecksum.CHUNK_SIZE,
                        length,
                    ),
                    role = "body-data",
                ),
            )
            logicalOffset += length
        }
    }

    private fun failure(
        message: String,
        code: CodecDiagnosticCode = CodecDiagnosticCode.INVALID_ARGUMENT,
        range: CodecDataRange? = null,
    ): CodecResult.Failure = CodecResult.Failure(
        listOf(
            CodecDiagnostic(
                severity = CodecDiagnosticSeverity.ERROR,
                stage = CodecStage.TRANSFER_ENCODE,
                code = code,
                message = message,
                range = range,
            ),
        ),
    )

    private data class NormalizedFilename(val value: String, val rawBytes: ByteArray)

    private const val TYPE_SIZE: Int = 1
    private const val NAME_BYTES: Int = 7
    private const val NAME_BLOCK_SIZE: Int = 8
    private const val NAME_SECTION_SIZE: Int = 9
    private const val NAME_TERMINATOR: Int = 0xf5
    private const val METADATA_SIZE: Int = 8
    private const val METADATA_SECTION_SIZE: Int = 9
    private const val BASIC_TERMINATOR: Int = 0xf0
}

/** Metadata retained beside an encoded payload for UI and analysis consumers. */
public class OldPayloadEncoding internal constructor(
    public val payload: OldTransferPayload,
    public val normalizedFilename: String,
    public val startAddress: Int? = null,
    public val endOffset: Int? = null,
    bodyMappings: List<CodecDataMapping>,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}
