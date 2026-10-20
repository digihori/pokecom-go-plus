package com.digihori.pgp.core.wav

public enum class S2BasicType(public val rawValue: Int) {
    BASIC(0x27), PASSWORD_PROTECTED_BASIC(0x37),
}

/** S2 uses the same line-record/chunk/tail envelope as S1, with a distinct retained type. */
public object S2BasicCodec {
    public fun encode(filename: String, body: LogicalByteSequence, type: S2BasicType = S2BasicType.BASIC): CodecResult<S2BasicEncoding> =
        when (val encoded = S1BasicCodec.encodeTyped(filename, body, type.rawValue)) {
            is CodecResult.Failure -> failure(encoded)
            is CodecResult.Success -> CodecResult.Success(S2BasicEncoding(encoded.value.rawBytes, filename, type, encoded.value.bodyMappings))
        }

    public fun decode(raw: RawTransferByteSequence): CodecResult<S2BasicDecoding> {
        val type = S2BasicType.entries.firstOrNull { raw.size > 0 && it.rawValue == raw.unsignedByteAt(0) }
            ?: return CodecResult.Failure(listOf(CodecDiagnostic(CodecDiagnosticSeverity.ERROR, CodecStage.TRANSFER_DECODE,
                CodecDiagnosticCode.UNSUPPORTED_FORMAT, "Expected S2 BASIC type 27 or 37", CodecDataRange(CodecDataKind.RAW_BYTES, 0, minOf(1, raw.size)))))
        return when (val decoded = S1BasicCodec.decodeTyped(raw, type.rawValue)) {
            is CodecResult.Failure -> failure(decoded)
            is CodecResult.Success -> decoded.value.let { CodecResult.Success(S2BasicDecoding(it.filename, it.body, type,
                it.consumedRawBytes, it.physicalTrailer, it.bodyMappings)) }
        }
    }

    public fun toTransferBody(program: ByteArray): CodecResult<LogicalByteSequence> =
        adapt(S1BasicCodec.toTransferBody(program))
    public fun toProgramImage(body: LogicalByteSequence): CodecResult<LogicalByteSequence> =
        adapt(S1BasicCodec.toProgramImage(body))
    private fun <T> adapt(result: CodecResult<T>): CodecResult<T> = when (result) {
        is CodecResult.Success -> result
        is CodecResult.Failure -> failure(result)
    }
    private fun failure(result: CodecResult.Failure): CodecResult.Failure = CodecResult.Failure(result.diagnostics.map {
        CodecDiagnostic(it.severity, it.stage, it.code, it.message.replace("S1 BASIC", "S2 BASIC"), it.range, it.details)
    })
}

public class S2BasicEncoding internal constructor(
    public val rawBytes: RawTransferByteSequence,
    public val filename: String,
    public val type: S2BasicType,
    bodyMappings: List<CodecDataMapping>,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}
public class S2BasicDecoding internal constructor(
    public val filename: String,
    public val body: LogicalByteSequence,
    public val type: S2BasicType,
    public val consumedRawBytes: Int,
    public val physicalTrailer: Boolean,
    bodyMappings: List<CodecDataMapping>,
) {
    public val bodyMappings: List<CodecDataMapping> = bodyMappings.toList()
}
