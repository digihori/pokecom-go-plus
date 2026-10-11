package com.digihori.pgp.desktop.wav

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineBasicDialect
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.*
import com.digihori.pgp.core.source.machine.PgpMemoryDumpWriter
import com.digihori.pgp.desktop.basic.*
import com.digihori.pgp.desktop.machine.*

/** One execution snapshot. Partial results never become a loadable program. */
internal data class DesktopWavResult(
    val summary: String,
    val diagnostics: List<CodecDiagnostic> = emptyList(),
    val pcm: NormalizedPcmData? = null,
    val raw: RawTransferByteSequence? = null,
    val mappings: List<CodecDataMapping> = emptyList(),
    val syncPoints: List<SignalSyncPoint> = emptyList(),
    val payload: OldPayloadDecoding? = null,
    val programName: String? = null,
    val program: ByteArray? = null,
    val wav: WavByteSequence? = null,
    val content: String = "",
    val error: String? = null,
) {
    fun report(): String = buildString {
        appendLine(summary)
        error?.let { appendLine(it) }
        diagnostics.forEach { appendLine(it.describe()) }
    }
}

internal fun CodecDiagnostic.describe(): String = buildString {
    append("${severity.name} ${stage.name} / ${code.name}: $message")
    range?.let { append(" [${it.kind.name} ${it.offset}..${it.endExclusive}]") }
    if (details.isNotEmpty()) append(" ${details.entries.joinToString { "${it.key}=${it.value}" }}")
}

internal object DesktopWavOperation {
    fun encode(bytes: ByteArray, binary: Boolean, machine: MachineId, filename: String, type12: Boolean): DesktopWavResult {
        if (MachineCatalog.find(machine)?.basicDialect != MachineBasicDialect.OLD) {
            return DesktopWavResult("Unsupported target", error = "Only OLD WAV is currently implemented for ${machine.value}.")
        }
        val encoded = if (binary) {
            val image = when (val parsed = DesktopMemoryDumpLoader.parse(bytes)) {
                is DesktopMemoryDumpLoadResult.Failure -> return DesktopWavResult("Invalid memory dump", error = parsed.error.toString())
                is DesktopMemoryDumpLoadResult.Success -> parsed.image
            }
            val contiguous = when (val flattened = DesktopContiguousMemoryImage.flatten(image)) {
                is DesktopContiguousMemoryImageResult.Failure -> return DesktopWavResult("Invalid memory image", error = flattened.message)
                is DesktopContiguousMemoryImageResult.Success -> flattened
            }
            OldWavCodec.encodeBinary(filename, contiguous.startAddress, LogicalByteSequence(contiguous.bytes))
        } else {
            val compiled = when (val result = DesktopBasicLoader.compileProgram(bytes, machine)) {
                is DesktopBasicProgramCompileResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = result.error.toString())
                is DesktopBasicProgramCompileResult.Success -> result.bytes
            }
            val body = when (val adapted = DesktopOldBasicTransferAdapter.toTransferBody(compiled)) {
                is DesktopOldBasicTransferResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = adapted.message)
                is DesktopOldBasicTransferResult.Success -> adapted.bytes
            }
            OldWavCodec.encodeBasic(filename, LogicalByteSequence(body), type12)
        }
        return when (encoded) {
            is CodecResult.Failure -> DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> {
                val output = encoded.value
                val inspected = decode(output.wav.bytes, machine)
                inspected.copy(
                    summary = "Encoded for ${machine.value}\n${inspected.summary}",
                    diagnostics = encoded.diagnostics + inspected.diagnostics,
                    wav = output.wav.bytes,
                    // Creation exports the WAV; decoded program is only a preview.
                    program = null,
                    programName = null,
                )
            }
        }
    }

    fun decode(bytes: WavByteSequence, machine: MachineId): DesktopWavResult {
        val diagnostics = mutableListOf<CodecDiagnostic>()
        val wav = when (val read = PcmWavReader.read(bytes)) {
            is CodecResult.Failure -> return DesktopWavResult("WAV container failed", read.diagnostics)
            is CodecResult.Success -> read.value.also { diagnostics += read.diagnostics }
        }
        val pcm = when (val normalized = PcmNormalizer.normalize(wav)) {
            is CodecResult.Failure -> return DesktopWavResult("PCM normalization failed", diagnostics + normalized.diagnostics)
            is CodecResult.Success -> normalized.value.also { diagnostics += normalized.diagnostics }
        }
        val summary = "${wav.format.sampleRate} Hz / ${wav.format.channelCount} channel(s) / ${wav.format.bitsPerSample}-bit\n" +
            "${pcm.samples.size} samples / ${pcm.samples.size.toDouble() / pcm.sampleRate} seconds"
        val signal = when (val decoded = OldSignalDecoder.decode(pcm)) {
            is CodecResult.Failure -> return DesktopWavResult(summary + "\nOLD signal failed", diagnostics + decoded.diagnostics, pcm)
            is CodecResult.Success -> decoded.value.also { diagnostics += decoded.diagnostics }
        }
        val partial = DesktopWavResult(
            summary + "\n${signal.rawBytes.size} raw bytes / ${signal.syncPoints.size} nibble sync points",
            diagnostics.toList(), pcm, signal.rawBytes, signal.rawByteMappings, signal.syncPoints,
        )
        val payload = when (val decoded = OldPayloadDecoder.decode(signal.rawBytes)) {
            is CodecResult.Failure -> return partial.copy(diagnostics = diagnostics + decoded.diagnostics)
            is CodecResult.Success -> decoded.value.also { diagnostics += decoded.diagnostics }
        }
        val name = payload.filename.ifBlank { "program" }
        val validated = partial.copy(
            summary = partial.summary + "\nValidated OLD ${payload.type.name}, filename: ${payload.filename}\n" +
                "${payload.body.size} logical bytes" + (payload.startAddress?.let { " / ${hex(it)}..${hex(payload.endAddress!!)}" } ?: ""),
            diagnostics = diagnostics.toList(), payload = payload,
        )
        if (payload.type == OldPayloadType.BINARY) {
            val dump = PgpMemoryDumpWriter.write(payload.startAddress!!, payload.body.copyBytes())
            return validated.copy(programName = "$name.dmp", program = dump.encodeToByteArray(), content = dump)
        }
        if (MachineCatalog.find(machine)?.basicDialect != MachineBasicDialect.OLD) {
            return validated.copy(error = "Select an OLD target machine to interpret BASIC tokens.")
        }
        val adapted = when (val result = DesktopOldBasicTransferAdapter.fromTransferBody(payload.body.copyBytes())) {
            is DesktopOldBasicTransferResult.Failure -> return validated.copy(error = result.message)
            is DesktopOldBasicTransferResult.Success -> result.bytes
        }
        return when (val decoded = DesktopBasicLoader.detokenizeProgram(adapted, machine)) {
            is DesktopBasicProgramDecodeResult.Success -> validated.copy(programName = "$name.bas", program = decoded.utf8Bytes, content = decoded.utf8Bytes.decodeToString())
            is DesktopBasicProgramDecodeResult.Failure -> validated.copy(error = "BASIC at ${decoded.error.offset}: ${decoded.error.message}")
            is DesktopBasicProgramDecodeResult.GenericFailure -> validated.copy(error = "BASIC at ${decoded.offset}: ${decoded.message}")
        }
    }

    fun hex(value: Int): String = value.toString(16).uppercase().padStart(4, '0')
    fun rawText(raw: RawTransferByteSequence): String = raw.copyBytes().asList().chunked(16).mapIndexed { row, bytes ->
        "${hex(row * 16)}  " + bytes.joinToString(" ") { (it.toInt() and 255).toString(16).uppercase().padStart(2, '0') }
    }.joinToString("\n")
}
