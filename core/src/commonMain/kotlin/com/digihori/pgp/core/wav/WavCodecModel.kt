package com.digihori.pgp.core.wav

/** OLD transfer payload types as they appear in the raw byte stream. */
public enum class OldPayloadType(public val rawValue: Int) {
    BASIC(0x02),
    PASSWORD_PROTECTED_BASIC(0x12),
    BINARY(0x62),
    ;

    public companion object {
        public fun fromRawValue(rawValue: Int): OldPayloadType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

/** Identifies the coordinate space used by a codec range. */
public enum class CodecDataKind {
    WAV_BYTES,
    PCM_SAMPLES,
    RAW_BYTES,
    LOGICAL_BYTES,
    TEXT_CHARACTERS,
}

/** A half-open range in one codec coordinate space. */
public data class CodecDataRange(
    public val kind: CodecDataKind,
    public val offset: Int,
    public val length: Int,
) {
    public val endExclusive: Int = checkedEndExclusive(offset, length)

    public operator fun contains(position: Int): Boolean = position in offset until endExclusive

    private companion object {
        private fun checkedEndExclusive(offset: Int, length: Int): Int {
            require(offset >= 0) { "Codec range offset must not be negative" }
            require(length >= 0) { "Codec range length must not be negative" }
            val end = offset.toLong() + length.toLong()
            require(end <= Int.MAX_VALUE) { "Codec range end exceeds the supported size" }
            return end.toInt()
        }
    }
}

/** Mapping retained for byte-level and text-level analysis views. */
public data class CodecDataMapping(
    public val source: CodecDataRange,
    public val target: CodecDataRange,
    public val role: String,
) {
    init {
        require(role.isNotBlank()) { "Codec mapping role must not be blank" }
    }
}

public enum class CodecDiagnosticSeverity {
    INFO,
    WARNING,
    ERROR,
}

public enum class CodecStage {
    WAV_CONTAINER,
    PCM_NORMALIZATION,
    SIGNAL_ENCODE,
    SIGNAL_DECODE,
    TRANSFER_ENCODE,
    TRANSFER_DECODE,
    PROGRAM_ENCODE,
    PROGRAM_DECODE,
}

public enum class CodecDiagnosticCode {
    INVALID_ARGUMENT,
    UNSUPPORTED_FORMAT,
    TRUNCATED_INPUT,
    INVALID_HEADER,
    CHECKSUM_MISMATCH,
    SYNCHRONIZATION_LOST,
    UNDEFINED_BYTE,
    MISSING_TERMINATOR,
    TRAILING_DATA,
}

/** A user-facing codec diagnostic with an optional precise input/output range. */
public class CodecDiagnostic(
    public val severity: CodecDiagnosticSeverity,
    public val stage: CodecStage,
    public val code: CodecDiagnosticCode,
    public val message: String,
    public val range: CodecDataRange? = null,
    details: Map<String, String> = emptyMap(),
) {
    public val details: Map<String, String> = details.toMap()

    init {
        require(message.isNotBlank()) { "Codec diagnostic message must not be blank" }
        require(details.keys.none(String::isBlank)) { "Codec diagnostic detail keys must not be blank" }
    }
}

/** Result type shared by the transfer, signal, and WAV container codecs. */
public sealed interface CodecResult<out T> {
    public val diagnostics: List<CodecDiagnostic>

    public class Success<T>(
        public val value: T,
        diagnostics: List<CodecDiagnostic> = emptyList(),
    ) : CodecResult<T> {
        override val diagnostics: List<CodecDiagnostic> = diagnostics.toList()

        init {
            require(diagnostics.none { it.severity == CodecDiagnosticSeverity.ERROR }) {
                "A successful codec result must not contain error diagnostics"
            }
        }
    }

    public class Failure(
        diagnostics: List<CodecDiagnostic>,
    ) : CodecResult<Nothing> {
        override val diagnostics: List<CodecDiagnostic> = diagnostics.toList()

        init {
            require(diagnostics.any { it.severity == CodecDiagnosticSeverity.ERROR }) {
                "A failed codec result must contain at least one error diagnostic"
            }
        }
    }
}

/** Immutable logical program bytes before transfer-format transformations. */
public class LogicalByteSequence(bytes: ByteArray) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int get() = content.size

    public fun copyBytes(): ByteArray = content.copyOf()
}

/** Immutable raw bytes recovered from or written to the transfer signal. */
public class RawTransferByteSequence(bytes: ByteArray) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int get() = content.size

    public fun copyBytes(): ByteArray = content.copyOf()

    public fun unsignedByteAt(index: Int): Int = content[index].toInt() and 0xff
}

/** Immutable mono signed PCM samples used at the signal-codec boundary. */
public class PcmSampleSequence(samples: ShortArray) {
    private val content: ShortArray = samples.copyOf()

    public val size: Int get() = content.size

    public fun copySamples(): ShortArray = content.copyOf()
}

/** Immutable complete RIFF/WAVE file bytes. */
public class WavByteSequence(bytes: ByteArray) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int get() = content.size

    public fun copyBytes(): ByteArray = content.copyOf()
}

/** A typed OLD payload. Full header and checksum validation belongs to the transfer decoder. */
public class OldTransferPayload(
    public val type: OldPayloadType,
    rawBytes: RawTransferByteSequence,
) {
    private val content: ByteArray = rawBytes.copyBytes()

    public val size: Int get() = content.size

    init {
        require(content.isNotEmpty()) { "OLD payload must contain a type byte" }
        require((content[0].toInt() and 0xff) == type.rawValue) {
            "OLD payload type does not match its first raw byte"
        }
    }

    public fun copyRawBytes(): ByteArray = content.copyOf()
}

public enum class WavAudioEncoding(public val formatCode: Int) {
    PCM(1),
}

/** PCM format after parsing the WAV fmt chunk and before mono normalization. */
public data class WavPcmFormat(
    public val sampleRate: Int,
    public val channelCount: Int,
    public val bitsPerSample: Int,
    public val encoding: WavAudioEncoding = WavAudioEncoding.PCM,
) {
    init {
        require(sampleRate > 0) { "WAV sample rate must be positive" }
        require(channelCount > 0) { "WAV channel count must be positive" }
        require(bitsPerSample > 0) { "WAV bits per sample must be positive" }
    }
}
