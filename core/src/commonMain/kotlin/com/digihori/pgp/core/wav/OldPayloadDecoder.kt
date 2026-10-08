package com.digihori.pgp.core.wav

/** Decodes and validates OLD raw transfer payloads. */
public object OldPayloadDecoder {
    public fun decode(rawBytes: RawTransferByteSequence): CodecResult<OldPayloadDecoding> {
        val raw = rawBytes.copyBytes()
        if (raw.size < BASIC_BODY_START) {
            return failure(
                code = CodecDiagnosticCode.TRUNCATED_INPUT,
                message = "OLD payload is shorter than its type and filename section",
                range = CodecDataRange(CodecDataKind.RAW_BYTES, 0, raw.size),
            )
        }

        val typeValue = unsigned(raw[TYPE_OFFSET])
        val type = OldPayloadType.fromRawValue(typeValue) ?: return failure(
            code = CodecDiagnosticCode.UNSUPPORTED_FORMAT,
            message = "Unknown OLD payload type ${typeValue.hexByte()}",
            range = CodecDataRange(CodecDataKind.RAW_BYTES, TYPE_OFFSET, 1),
        )
        if (unsigned(raw[NAME_TERMINATOR_OFFSET]) != NAME_TERMINATOR) {
            return failure(
                code = CodecDiagnosticCode.INVALID_HEADER,
                message = "OLD filename block is missing its F5 terminator",
                range = CodecDataRange(CodecDataKind.RAW_BYTES, NAME_TERMINATOR_OFFSET, 1),
            )
        }
        checksumFailure(
            raw = raw,
            dataOffset = NAME_OFFSET,
            checksumOffset = NAME_CHECKSUM_OFFSET,
            label = "filename",
        )?.let { return it }

        val diagnostics = mutableListOf<CodecDiagnostic>()
        val filename = decodeFilename(raw, diagnostics)

        return when (type) {
            OldPayloadType.BASIC,
            OldPayloadType.PASSWORD_PROTECTED_BASIC,
            -> decodeBasic(raw, type, filename, diagnostics)
            OldPayloadType.BINARY -> decodeBinary(raw, filename, diagnostics)
        }
    }

    private fun decodeBasic(
        raw: ByteArray,
        type: OldPayloadType,
        filename: String,
        diagnostics: MutableList<CodecDiagnostic>,
    ): CodecResult<OldPayloadDecoding> {
        val bodyResult = decodeBody(
            raw = raw,
            rawStart = BASIC_BODY_START,
            expectedLogicalSize = null,
        )
        val body = when (bodyResult) {
            is BodyResult.Failure -> return CodecResult.Failure(bodyResult.diagnostics)
            is BodyResult.Success -> bodyResult.value
        }
        if (!body.terminated) {
            return failure(
                code = CodecDiagnosticCode.MISSING_TERMINATOR,
                message = "OLD BASIC body is missing its F0 terminator",
                range = CodecDataRange(
                    CodecDataKind.RAW_BYTES,
                    BASIC_BODY_START,
                    raw.size - BASIC_BODY_START,
                ),
            )
        }
        appendTrailingDataDiagnostic(raw, body.consumedRawBytes, diagnostics)

        return CodecResult.Success(
            value = OldPayloadDecoding(
                type = type,
                filename = filename,
                body = LogicalByteSequence(body.logicalBytes),
                consumedRawBytes = body.consumedRawBytes,
                bodyMappings = body.mappings,
            ),
            diagnostics = diagnostics,
        )
    }

    private fun decodeBinary(
        raw: ByteArray,
        filename: String,
        diagnostics: MutableList<CodecDiagnostic>,
    ): CodecResult<OldPayloadDecoding> {
        if (raw.size < BINARY_BODY_START) {
            return failure(
                code = CodecDiagnosticCode.TRUNCATED_INPUT,
                message = "OLD binary payload is missing metadata",
                range = CodecDataRange(
                    CodecDataKind.RAW_BYTES,
                    BASIC_BODY_START,
                    raw.size - BASIC_BODY_START,
                ),
            )
        }
        checksumFailure(
            raw = raw,
            dataOffset = METADATA_OFFSET,
            checksumOffset = METADATA_CHECKSUM_OFFSET,
            label = "binary metadata",
        )?.let { return it }

        val reserved = raw.copyOfRange(METADATA_OFFSET, METADATA_OFFSET + RESERVED_SIZE)
        val startAddress = unsigned(raw[METADATA_OFFSET + 4]) shl 8 or unsigned(raw[METADATA_OFFSET + 5])
        val endOffset = unsigned(raw[METADATA_OFFSET + 6]) shl 8 or unsigned(raw[METADATA_OFFSET + 7])
        val endAddress = startAddress.toLong() + endOffset.toLong()
        if (endAddress > 0xffffL) {
            return failure(
                code = CodecDiagnosticCode.INVALID_HEADER,
                message = "OLD binary metadata exceeds the 16-bit address space",
                range = CodecDataRange(CodecDataKind.RAW_BYTES, METADATA_OFFSET + 4, 4),
            )
        }
        if (!reserved.contentEquals(EXPECTED_RESERVED)) {
            diagnostics += CodecDiagnostic(
                severity = CodecDiagnosticSeverity.WARNING,
                stage = CodecStage.TRANSFER_DECODE,
                code = CodecDiagnosticCode.INVALID_HEADER,
                message = "OLD binary reserved metadata differs from 00 00 00 60",
                range = CodecDataRange(CodecDataKind.RAW_BYTES, METADATA_OFFSET, 4),
            )
        }

        val bodyResult = decodeBody(
            raw = raw,
            rawStart = BINARY_BODY_START,
            expectedLogicalSize = endOffset + 1,
        )
        val body = when (bodyResult) {
            is BodyResult.Failure -> return CodecResult.Failure(bodyResult.diagnostics)
            is BodyResult.Success -> bodyResult.value
        }
        appendTrailingDataDiagnostic(raw, body.consumedRawBytes, diagnostics)

        return CodecResult.Success(
            value = OldPayloadDecoding(
                type = OldPayloadType.BINARY,
                filename = filename,
                body = LogicalByteSequence(body.logicalBytes),
                consumedRawBytes = body.consumedRawBytes,
                startAddress = startAddress,
                endOffset = endOffset,
                reservedMetadata = reserved,
                bodyMappings = body.mappings,
            ),
            diagnostics = diagnostics,
        )
    }

    private fun decodeBody(
        raw: ByteArray,
        rawStart: Int,
        expectedLogicalSize: Int?,
    ): BodyResult {
        val logical = mutableListOf<Byte>()
        val mappings = mutableListOf<CodecDataMapping>()
        val accumulator = OldChecksumAccumulator()
        var rawOffset = rawStart
        var terminated = false

        while (expectedLogicalSize == null || logical.size < expectedLogicalSize) {
            if (rawOffset >= raw.size) break
            if (logical.size % OldTransferChecksum.RESET_INTERVAL == 0) accumulator.reset()
            val logicalChunkStart = logical.size
            val rawChunkStart = rawOffset
            val maximumChunkLength = if (expectedLogicalSize == null) {
                OldTransferChecksum.CHUNK_SIZE
            } else {
                minOf(OldTransferChecksum.CHUNK_SIZE, expectedLogicalSize - logical.size)
            }
            var chunkLength = 0

            while (chunkLength < maximumChunkLength && rawOffset < raw.size) {
                val logicalValue = OldTransferChecksum.nibbleSwap(unsigned(raw[rawOffset]))
                accumulator.addLogicalByte(logicalValue)
                logical += logicalValue.toByte()
                rawOffset++
                chunkLength++
                if (expectedLogicalSize == null && logicalValue == BASIC_TERMINATOR) {
                    terminated = true
                    break
                }
            }

            if (chunkLength > 0) {
                mappings += CodecDataMapping(
                    source = CodecDataRange(CodecDataKind.RAW_BYTES, rawChunkStart, chunkLength),
                    target = CodecDataRange(CodecDataKind.LOGICAL_BYTES, logicalChunkStart, chunkLength),
                    role = "body-data",
                )
            }

            val completedChunk = chunkLength == OldTransferChecksum.CHUNK_SIZE
            if (completedChunk) {
                if (rawOffset >= raw.size) {
                    return bodyFailure(
                        code = CodecDiagnosticCode.TRUNCATED_INPUT,
                        message = "OLD body is missing a checksum byte",
                        offset = rawOffset,
                        availableSize = raw.size,
                    )
                }
                val actual = OldTransferChecksum.nibbleSwap(unsigned(raw[rawOffset]))
                if (actual != accumulator.value) {
                    return checksumBodyFailure(rawOffset, actual, accumulator.value)
                }
                rawOffset++
            }

            if (terminated) break
            if (chunkLength < maximumChunkLength) break
        }

        if (expectedLogicalSize != null && logical.size < expectedLogicalSize) {
            return bodyFailure(
                code = CodecDiagnosticCode.TRUNCATED_INPUT,
                message = "OLD binary body is shorter than its declared length",
                offset = rawOffset,
                availableSize = raw.size,
            )
        }
        return BodyResult.Success(
            DecodedBody(
                logicalBytes = logical.toByteArray(),
                consumedRawBytes = rawOffset,
                terminated = terminated,
                mappings = mappings,
            ),
        )
    }

    private fun decodeFilename(raw: ByteArray, diagnostics: MutableList<CodecDiagnostic>): String {
        val characters = mutableListOf<Char>()
        repeat(NAME_BYTES) { index ->
            val rawOffset = NAME_OFFSET + index
            when (val value = unsigned(raw[rawOffset])) {
                0x00 -> Unit
                in 0x40..0x49 -> characters += ('0'.code + value - 0x40).toChar()
                in 0x51..0x6a -> characters += ('A'.code + value - 0x51).toChar()
                else -> {
                    characters += '?'
                    diagnostics += CodecDiagnostic(
                        severity = CodecDiagnosticSeverity.WARNING,
                        stage = CodecStage.TRANSFER_DECODE,
                        code = CodecDiagnosticCode.UNDEFINED_BYTE,
                        message = "Undefined OLD filename byte ${value.hexByte()}",
                        range = CodecDataRange(CodecDataKind.RAW_BYTES, rawOffset, 1),
                    )
                }
            }
        }
        return characters.asReversed().joinToString("")
    }

    private fun checksumFailure(
        raw: ByteArray,
        dataOffset: Int,
        checksumOffset: Int,
        label: String,
    ): CodecResult.Failure? {
        val data = raw.copyOfRange(dataOffset, checksumOffset)
        val expected = OldTransferChecksum.calculate(LogicalByteSequence(data))
        val actual = OldTransferChecksum.nibbleSwap(unsigned(raw[checksumOffset]))
        if (actual == expected) return null
        return failure(
            code = CodecDiagnosticCode.CHECKSUM_MISMATCH,
            message = "OLD $label checksum mismatch",
            range = CodecDataRange(CodecDataKind.RAW_BYTES, checksumOffset, 1),
            details = checksumDetails(expected, actual),
        )
    }

    private fun appendTrailingDataDiagnostic(
        raw: ByteArray,
        consumedRawBytes: Int,
        diagnostics: MutableList<CodecDiagnostic>,
    ) {
        if (consumedRawBytes >= raw.size) return
        diagnostics += CodecDiagnostic(
            severity = CodecDiagnosticSeverity.WARNING,
            stage = CodecStage.TRANSFER_DECODE,
            code = CodecDiagnosticCode.TRAILING_DATA,
            message = "OLD payload has ${raw.size - consumedRawBytes} trailing raw byte(s)",
            range = CodecDataRange(
                CodecDataKind.RAW_BYTES,
                consumedRawBytes,
                raw.size - consumedRawBytes,
            ),
        )
    }

    private fun checksumBodyFailure(offset: Int, actual: Int, expected: Int): BodyResult.Failure =
        BodyResult.Failure(
            listOf(
                diagnostic(
                    code = CodecDiagnosticCode.CHECKSUM_MISMATCH,
                    message = "OLD body checksum mismatch",
                    range = CodecDataRange(CodecDataKind.RAW_BYTES, offset, 1),
                    details = checksumDetails(expected, actual),
                ),
            ),
        )

    private fun bodyFailure(
        code: CodecDiagnosticCode,
        message: String,
        offset: Int,
        availableSize: Int,
    ): BodyResult.Failure = BodyResult.Failure(
        listOf(
            diagnostic(
                code = code,
                message = message,
                range = CodecDataRange(
                    CodecDataKind.RAW_BYTES,
                    minOf(offset, availableSize),
                    0,
                ),
            ),
        ),
    )

    private fun failure(
        code: CodecDiagnosticCode,
        message: String,
        range: CodecDataRange? = null,
        details: Map<String, String> = emptyMap(),
    ): CodecResult.Failure = CodecResult.Failure(
        listOf(diagnostic(code, message, range, details)),
    )

    private fun diagnostic(
        code: CodecDiagnosticCode,
        message: String,
        range: CodecDataRange? = null,
        details: Map<String, String> = emptyMap(),
    ): CodecDiagnostic = CodecDiagnostic(
        severity = CodecDiagnosticSeverity.ERROR,
        stage = CodecStage.TRANSFER_DECODE,
        code = code,
        message = message,
        range = range,
        details = details,
    )

    private fun checksumDetails(expected: Int, actual: Int): Map<String, String> = mapOf(
        "expected" to expected.hexByte(),
        "actual" to actual.hexByte(),
    )

    private fun unsigned(value: Byte): Int = value.toInt() and 0xff
    private fun Int.hexByte(): String = toString(16).uppercase().padStart(2, '0')

    private sealed interface BodyResult {
        data class Success(val value: DecodedBody) : BodyResult
        data class Failure(val diagnostics: List<CodecDiagnostic>) : BodyResult
    }

    private data class DecodedBody(
        val logicalBytes: ByteArray,
        val consumedRawBytes: Int,
        val terminated: Boolean,
        val mappings: List<CodecDataMapping>,
    )

    private const val TYPE_OFFSET: Int = 0
    private const val NAME_OFFSET: Int = 1
    private const val NAME_BYTES: Int = 7
    private const val NAME_TERMINATOR_OFFSET: Int = 8
    private const val NAME_CHECKSUM_OFFSET: Int = 9
    private const val BASIC_BODY_START: Int = 10
    private const val NAME_TERMINATOR: Int = 0xf5
    private const val BASIC_TERMINATOR: Int = 0xf0
    private const val METADATA_OFFSET: Int = 10
    private const val METADATA_SIZE: Int = 8
    private const val RESERVED_SIZE: Int = 4
    private const val METADATA_CHECKSUM_OFFSET: Int = 18
    private const val BINARY_BODY_START: Int = 19
    private val EXPECTED_RESERVED: ByteArray = byteArrayOf(0x00, 0x00, 0x00, 0x60)
}

/** Validated logical content and metadata decoded from one OLD payload. */
public class OldPayloadDecoding internal constructor(
    public val type: OldPayloadType,
    public val filename: String,
    public val body: LogicalByteSequence,
    public val consumedRawBytes: Int,
    public val startAddress: Int? = null,
    public val endOffset: Int? = null,
    reservedMetadata: ByteArray = ByteArray(0),
    bodyMappings: List<CodecDataMapping>,
) {
    private val reservedContent: ByteArray = reservedMetadata.copyOf()
    public val endAddress: Int? = startAddress?.let { it + requireNotNull(endOffset) }
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
    public val passwordProtected: Boolean = type == OldPayloadType.PASSWORD_PROTECTED_BASIC

    public fun copyReservedMetadata(): ByteArray = reservedContent.copyOf()
}
