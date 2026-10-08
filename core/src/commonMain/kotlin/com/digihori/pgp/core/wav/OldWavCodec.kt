package com.digihori.pgp.core.wav

/** End-to-end OLD transfer codec. Filesystem access remains the host application's responsibility. */
public object OldWavCodec {
    public fun encodeBasic(
        filename: String,
        body: LogicalByteSequence,
        passwordProtected: Boolean = false,
    ): CodecResult<OldWavEncoding> {
        val payload = when (val result = OldPayloadEncoder.encodeBasic(filename, body, passwordProtected)) {
            is CodecResult.Failure -> return result
            is CodecResult.Success -> result
        }
        return encodePayload(payload)
    }

    public fun encodeBinary(
        filename: String,
        startAddress: Int,
        body: LogicalByteSequence,
    ): CodecResult<OldWavEncoding> {
        val payload = when (val result = OldPayloadEncoder.encodeBinary(filename, startAddress, body)) {
            is CodecResult.Failure -> return result
            is CodecResult.Success -> result
        }
        return encodePayload(payload)
    }

    public fun decode(input: WavByteSequence): CodecResult<OldWavDecoding> {
        val diagnostics = mutableListOf<CodecDiagnostic>()

        val wav = when (val result = PcmWavReader.read(input)) {
            is CodecResult.Failure -> return result
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }
        val pcm = when (val result = PcmNormalizer.normalize(wav)) {
            is CodecResult.Failure -> return failureWith(diagnostics, result)
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }
        val signal = when (val result = OldSignalDecoder.decode(pcm)) {
            is CodecResult.Failure -> return failureWith(diagnostics, result)
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }
        val payload = when (val result = OldPayloadDecoder.decode(signal.rawBytes)) {
            is CodecResult.Failure -> return failureWith(diagnostics, result)
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }

        return CodecResult.Success(
            value = OldWavDecoding(
                wav = wav,
                pcm = pcm,
                signal = signal,
                payload = payload,
            ),
            diagnostics = diagnostics,
        )
    }

    private fun encodePayload(payload: CodecResult.Success<OldPayloadEncoding>): CodecResult<OldWavEncoding> {
        val diagnostics = payload.diagnostics.toMutableList()
        val signal = when (val result = OldSignalEncoder.encode(payload.value.payload)) {
            is CodecResult.Failure -> return failureWith(diagnostics, result)
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }
        val wav = when (val result = PcmWavWriter.writeOldSignal(signal)) {
            is CodecResult.Failure -> return failureWith(diagnostics, result)
            is CodecResult.Success -> result.value.also { diagnostics += result.diagnostics }
        }
        return CodecResult.Success(
            value = OldWavEncoding(
                payload = payload.value,
                signal = signal,
                wav = wav,
            ),
            diagnostics = diagnostics,
        )
    }

    private fun failureWith(
        previousDiagnostics: List<CodecDiagnostic>,
        failure: CodecResult.Failure,
    ): CodecResult.Failure = CodecResult.Failure(previousDiagnostics + failure.diagnostics)
}

/** Intermediate results retained so Studio can inspect or export any codec layer. */
public data class OldWavEncoding(
    public val payload: OldPayloadEncoding,
    public val signal: OldSignalEncoding,
    public val wav: WavEncoding,
)

/** Validated payload plus the container and signal details used to recover it. */
public data class OldWavDecoding(
    public val wav: WavPcmData,
    public val pcm: NormalizedPcmData,
    public val signal: OldSignalDecoding,
    public val payload: OldPayloadDecoding,
)
