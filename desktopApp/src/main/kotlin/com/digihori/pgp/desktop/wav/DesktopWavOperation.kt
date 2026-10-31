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
    val compileError: DesktopBasicProgramCompileError? = null,
    val memoryError: DesktopMemoryDumpLoadError? = null,
    val targetMachine: String? = null,
    val s1Payload: S1BinaryDecoding? = null,
    val hexProgram: ByteArray? = null,
    val s1BasicPayload: S1BasicDecoding? = null,
    val s2BasicPayload: S2BasicDecoding? = null,
    val signalDialect: MachineBasicDialect = MachineBasicDialect.OLD,
    val reportNotes: List<String> = emptyList(),
    val detectedFormat: String? = null,
    val decodeCandidates: List<DesktopWavResult> = emptyList(),
) {
    val validated: Boolean get() = payload != null || s1Payload != null || s1BasicPayload != null || s2BasicPayload != null
    val transferFilename: String get() = (s2BasicPayload?.filename ?: s1BasicPayload?.filename ?: s1Payload?.filename ?: payload?.filename).orEmpty().ifBlank { "program" }
    fun report(): String = buildString {
        appendLine(summary)
        reportNotes.forEach { appendLine(it) }
        error?.let { appendLine(it) }
        diagnostics.forEach { appendLine(it.describe()) }
        decodeCandidates.forEach { candidate -> appendLine("Candidate ${candidate.signalDialect}: ${candidate.summary}"); candidate.diagnostics.forEach { appendLine(it.describe()) } }
    }
}

internal fun CodecDiagnostic.describe(): String = buildString {
    append("${severity.name} ${stage.name} / ${code.name}: $message")
    range?.let { append(" [${it.kind.name} ${it.offset}..${it.endExclusive}]") }
    if (details.isNotEmpty()) append(" ${details.entries.joinToString { "${it.key}=${it.value}" }}")
}

internal object DesktopWavOperation {
    fun supported(dialect: MachineBasicDialect?, decode: Boolean, binary: Boolean): Boolean = dialect != null

    fun encode(bytes: ByteArray, binary: Boolean, machine: MachineId, filename: String, type12: Boolean): DesktopWavResult {
        val dialect = MachineCatalog.find(machine)?.basicDialect
        val transferMachine = canonicalMachine(dialect)
        if (!binary && dialect in setOf(MachineBasicDialect.S1, MachineBasicDialect.S2)) return encodeModernBasic(bytes, transferMachine, filename, type12)
        if (binary && dialect in setOf(MachineBasicDialect.S1, MachineBasicDialect.S2)) return encodeS1(bytes, machine, filename)
        if (dialect != MachineBasicDialect.OLD) {
            return DesktopWavResult("Unsupported target", error = "Unsupported WAV target machine.")
        }
        val encoded = if (binary) {
            val image = when (val parsed = DesktopMemoryDumpLoader.parse(runCatching { DesktopWavBinaryFiles.asDump(bytes) }.getOrElse { return DesktopWavResult("Invalid memory dump", error = it.message) })) {
                is DesktopMemoryDumpLoadResult.Failure -> return DesktopWavResult("Invalid memory dump", error = parsed.error.toString(), memoryError = parsed.error)
                is DesktopMemoryDumpLoadResult.Success -> parsed.image
            }
            val contiguous = when (val flattened = DesktopContiguousMemoryImage.flatten(image)) {
                is DesktopContiguousMemoryImageResult.Failure -> return DesktopWavResult("Invalid memory image", error = flattened.message)
                is DesktopContiguousMemoryImageResult.Success -> flattened
            }
            OldWavCodec.encodeBinary(filename, contiguous.startAddress, LogicalByteSequence(contiguous.bytes))
        } else {
            val compiled = when (val result = DesktopBasicLoader.compileProgram(bytes, canonicalMachine(MachineCatalog.find(machine)?.basicDialect))) {
                is DesktopBasicProgramCompileResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = result.error.toString(), compileError = result.error, targetMachine = machine.value)
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
                    summary = "Encoded for ${MachineCatalog.find(machine)?.basicDialect?.name ?: "UNKNOWN"}\n${inspected.summary}",
                    diagnostics = encoded.diagnostics + inspected.diagnostics,
                    wav = output.wav.bytes,
                    // Creation exports the WAV; decoded program is only a preview.
                    program = null,
                    programName = null,
                )
            }
        }
    }

    private fun canonicalMachine(dialect: MachineBasicDialect?): MachineId = MachineId(when (dialect) {
        MachineBasicDialect.S1 -> "pc-1350"
        MachineBasicDialect.S2 -> "pc-1360"
        else -> "pc-1245"
    })

    /** The machine argument is retained for callers; Decode always detects the transfer format. */
    fun decode(bytes: WavByteSequence, machine: MachineId): DesktopWavResult = decode(bytes)

    fun decode(bytes: WavByteSequence): DesktopWavResult {
        val candidates = listOf(MachineBasicDialect.OLD, MachineBasicDialect.S1).map { dialect ->
            decodeInternal(bytes, canonicalMachine(dialect)).let { result ->
                result.copy(signalDialect = if (result.s2BasicPayload != null ||
                    result.raw?.let { it.size > 0 && it.unsignedByteAt(0) in setOf(0x27, 0x37) } == true) MachineBasicDialect.S2 else dialect)
            }
        }
        val valid = candidates.filter { it.validated }
        val result = if (valid.size == 1) {
            val selected = valid.single()
            val label = when {
                selected.payload != null -> "OLD ${selected.payload.type.name}"
                selected.s1Payload != null -> "S1/S2 BINARY"
                selected.s2BasicPayload != null -> "S2 ${selected.s2BasicPayload.type.name}"
                selected.s1BasicPayload?.passwordProtected == true -> "S1 PASSWORD_PROTECTED_BASIC"
                else -> "S1 BASIC"
            }
            selected.copy(detectedFormat = label, summary = "Detected format: $label\n${selected.summary}")
        } else {
            val best = candidates.maxBy { candidate ->
                val first = candidate.raw?.let { if (it.size > 0) it.unsignedByteAt(0) else -1 }
                val known = if (candidate.signalDialect == MachineBasicDialect.OLD) first in setOf(2, 0x12, 0x62) else first in setOf(7, 0x17, 0x27, 0x37, 0x67)
                (if (known) { if (candidate.signalDialect == MachineBasicDialect.OLD) 1000000 else 2000000 } else 0) +
                    (if (candidate.diagnostics.any { it.code == CodecDiagnosticCode.CHECKSUM_MISMATCH }) 4000000 else 0) + (candidate.raw?.size ?: 0)
            }
            best.copy(summary = "Transfer format could not be determined\n${best.summary}", decodeCandidates = candidates,
                diagnostics = best.diagnostics + CodecDiagnostic(CodecDiagnosticSeverity.ERROR, CodecStage.TRANSFER_DECODE,
                    CodecDiagnosticCode.UNSUPPORTED_FORMAT, if (valid.size > 1) "Multiple transfer formats passed validation" else "No transfer format passed validation"))
        }
        val damaged = result.error != null || result.diagnostics.any {
            it.severity == CodecDiagnosticSeverity.ERROR || (it.stage == CodecStage.SIGNAL_DECODE && it.severity == CodecDiagnosticSeverity.WARNING)
        }
        return if (damaged) result.copy(program = null, programName = null, hexProgram = null) else result
    }

    private fun decodeInternal(bytes: WavByteSequence, machine: MachineId): DesktopWavResult {
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
        if (MachineCatalog.find(machine)?.basicDialect in setOf(MachineBasicDialect.S1, MachineBasicDialect.S2)) {
            return decodeS1(pcm, summary, diagnostics, machine)
        }
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

    private fun encodeModernBasic(bytes: ByteArray, machine: MachineId, filename: String, protectedBasic: Boolean): DesktopWavResult {
        val transferFilename = filename.take(7)
        val program = when (val compiled = DesktopBasicLoader.compileProgram(bytes, canonicalMachine(MachineCatalog.find(machine)?.basicDialect))) {
            is DesktopBasicProgramCompileResult.Failure -> return DesktopWavResult("BASIC conversion failed", error = compiled.error.toString(), compileError = compiled.error, targetMachine = machine.value)
            is DesktopBasicProgramCompileResult.Success -> compiled.bytes
        }
        val s2 = MachineCatalog.require(machine).basicDialect == MachineBasicDialect.S2
        val body = when (val adapted = if (s2) S2BasicCodec.toTransferBody(program) else S1BasicCodec.toTransferBody(program)) {
            is CodecResult.Failure -> return DesktopWavResult("BASIC conversion failed", adapted.diagnostics)
            is CodecResult.Success -> adapted.value
        }
        val raw = if (s2) {
            when (val encoded = S2BasicCodec.encode(transferFilename, body,
                if (protectedBasic) S2BasicType.PASSWORD_PROTECTED_BASIC else S2BasicType.BASIC)) {
                is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
                is CodecResult.Success -> encoded.value.rawBytes
            }
        } else {
            when (val encoded = S1BasicCodec.encode(transferFilename, body, protectedBasic)) {
                is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
                is CodecResult.Success -> encoded.value.rawBytes
            }
        }
        // BASIC adds a physical FF after FF FF checksum. It is not part of the payload.
        val signal = when (val encoded = S1SignalEncoder.encode(RawTransferByteSequence(raw.copyBytes() + byteArrayOf(0xff.toByte())))) {
            is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> encoded.value
        }
        val wav = when (val encoded = PcmWavWriter.write8BitMono(signal.sampleRate, signal.samples)) {
            is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> encoded.value.bytes
        }
        val inspected = decode(wav, machine)
        return inspected.copy(summary = "Encoded for ${MachineCatalog.find(machine)?.basicDialect?.name ?: "UNKNOWN"}\n${inspected.summary}", wav = wav,
            program = null, programName = null, hexProgram = null,
            reportNotes = if (filename.length > 7) listOf("Transfer filename shortened to seven characters: $filename -> $transferFilename.") else emptyList())
    }

    private fun decodeS1Basic(partial: DesktopWavResult, machine: MachineId): DesktopWavResult {
        val payload = when (val decoded = S1BasicCodec.decode(requireNotNull(partial.raw))) {
            is CodecResult.Failure -> return partial.copy(diagnostics = partial.diagnostics + decoded.diagnostics)
            is CodecResult.Success -> decoded.value
        }
        val validated = partial.copy(summary = partial.summary + "\nValidated S1 ${if (payload.passwordProtected) "PASSWORD_PROTECTED_BASIC" else "BASIC"}, filename: ${payload.filename}\n${payload.body.size} logical bytes",
            s1BasicPayload = payload)
        if (MachineCatalog.find(machine)?.basicDialect != MachineBasicDialect.S1)
            return validated.copy(error = "Select an S1 target machine to interpret BASIC tokens.")
        val image = when (val adapted = S1BasicCodec.toProgramImage(payload.body)) {
            is CodecResult.Failure -> return validated.copy(diagnostics = validated.diagnostics + adapted.diagnostics)
            is CodecResult.Success -> adapted.value.copyBytes()
        }
        return when (val decoded = DesktopBasicLoader.detokenizeProgram(image, machine)) {
            is DesktopBasicProgramDecodeResult.Success -> {
                val recompiled = DesktopBasicLoader.compileProgram(decoded.utf8Bytes, machine)
                if (recompiled !is DesktopBasicProgramCompileResult.Success || !recompiled.bytes.contentEquals(image)) {
                    return validated.copy(content = decoded.utf8Bytes.decodeToString(), diagnostics = validated.diagnostics + CodecDiagnostic(
                        CodecDiagnosticSeverity.ERROR, CodecStage.PROGRAM_DECODE, CodecDiagnosticCode.UNDEFINED_BYTE,
                        "S1 BASIC text cannot preserve all bytes for the selected machine",
                        CodecDataRange(CodecDataKind.LOGICAL_BYTES, 0, payload.body.size)))
                }
                val damaged = partial.diagnostics.any { it.stage == CodecStage.SIGNAL_DECODE && it.severity != CodecDiagnosticSeverity.INFO }
                validated.copy(content = decoded.utf8Bytes.decodeToString(),
                    programName = if (damaged) null else "${payload.filename.ifBlank { "program" }}.bas",
                    program = if (damaged) null else decoded.utf8Bytes)
            }
            is DesktopBasicProgramDecodeResult.Failure -> validated.copy(error = "BASIC at ${decoded.error.offset}: ${decoded.error.message}")
            is DesktopBasicProgramDecodeResult.GenericFailure -> validated.copy(error = "BASIC at ${decoded.offset}: ${decoded.message}")
        }
    }

    private fun decodeS2Basic(partial: DesktopWavResult, machine: MachineId): DesktopWavResult {
        val payload = when (val decoded = S2BasicCodec.decode(requireNotNull(partial.raw))) {
            is CodecResult.Failure -> return partial.copy(diagnostics = partial.diagnostics + decoded.diagnostics)
            is CodecResult.Success -> decoded.value
        }
        val validated = partial.copy(summary = partial.summary + "\nValidated S2 ${payload.type.name}, filename: ${payload.filename}\n${payload.body.size} logical bytes",
            s2BasicPayload = payload)
        if (MachineCatalog.find(machine)?.basicDialect != MachineBasicDialect.S2)
            return validated.copy(error = "Select an S2 target machine to interpret BASIC tokens.")
        val image = when (val adapted = S2BasicCodec.toProgramImage(payload.body)) {
            is CodecResult.Failure -> return validated.copy(diagnostics = validated.diagnostics + adapted.diagnostics)
            is CodecResult.Success -> adapted.value.copyBytes()
        }
        return when (val decoded = DesktopBasicLoader.detokenizeProgram(image, machine)) {
            is DesktopBasicProgramDecodeResult.Success -> {
                val recompiled = DesktopBasicLoader.compileProgram(decoded.utf8Bytes, machine)
                if (recompiled !is DesktopBasicProgramCompileResult.Success || !recompiled.bytes.contentEquals(image)) {
                    return validated.copy(content = decoded.utf8Bytes.decodeToString(), diagnostics = validated.diagnostics + CodecDiagnostic(
                        CodecDiagnosticSeverity.ERROR, CodecStage.PROGRAM_DECODE, CodecDiagnosticCode.UNDEFINED_BYTE,
                        "S2 BASIC text cannot preserve all bytes for the selected machine",
                        CodecDataRange(CodecDataKind.LOGICAL_BYTES, 0, payload.body.size)))
                }
                val damaged = partial.diagnostics.any { it.stage == CodecStage.SIGNAL_DECODE && it.severity != CodecDiagnosticSeverity.INFO }
                validated.copy(content = decoded.utf8Bytes.decodeToString(),
                    programName = if (damaged) null else "${payload.filename.ifBlank { "program" }}.bas",
                    program = if (damaged) null else decoded.utf8Bytes)
            }
            is DesktopBasicProgramDecodeResult.Failure -> validated.copy(error = "BASIC at ${decoded.error.offset}: ${decoded.error.message}")
            is DesktopBasicProgramDecodeResult.GenericFailure -> validated.copy(error = "BASIC at ${decoded.offset}: ${decoded.message}")
        }
    }

    private fun encodeS1(bytes: ByteArray, machine: MachineId, filename: String): DesktopWavResult {
        val source = runCatching { DesktopWavBinaryFiles.asDump(bytes) }.getOrElse {
            return DesktopWavResult("Invalid memory dump", error = it.message)
        }
        val image = when (val parsed = DesktopMemoryDumpLoader.parse(source)) {
            is DesktopMemoryDumpLoadResult.Failure -> return DesktopWavResult("Invalid memory dump", error = parsed.error.toString(), memoryError = parsed.error)
            is DesktopMemoryDumpLoadResult.Success -> parsed.image
        }
        val contiguous = when (val flat = DesktopContiguousMemoryImage.flatten(image)) {
            is DesktopContiguousMemoryImageResult.Failure -> return DesktopWavResult("Invalid memory image", error = flat.message)
            is DesktopContiguousMemoryImageResult.Success -> flat
        }
        val payload = when (val encoded = S1BinaryCodec.encode(filename, contiguous.startAddress, LogicalByteSequence(contiguous.bytes))) {
            is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> encoded.value
        }
        val signal = when (val encoded = S1SignalEncoder.encode(payload.rawBytes)) {
            is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> encoded.value
        }
        val wav = when (val encoded = PcmWavWriter.write8BitMono(signal.sampleRate, signal.samples)) {
            is CodecResult.Failure -> return DesktopWavResult("Encode failed", encoded.diagnostics)
            is CodecResult.Success -> encoded.value.bytes
        }
        val inspected = decode(wav, machine)
        return inspected.copy(summary = "Encoded for ${MachineCatalog.find(machine)?.basicDialect?.name ?: "UNKNOWN"}\n${inspected.summary}", wav = wav,
            program = null, programName = null, hexProgram = null)
    }

    private fun decodeS1(pcm: NormalizedPcmData, summary: String, prior: List<CodecDiagnostic>, machine: MachineId): DesktopWavResult {
        val diagnostics = prior.toMutableList()
        val signal = when (val decoded = S1SignalDecoder.decode(pcm)) {
            is CodecResult.Failure -> return DesktopWavResult(summary + "\nS1/S2 signal failed", diagnostics + decoded.diagnostics, pcm)
            is CodecResult.Success -> decoded.value.also { diagnostics += decoded.diagnostics }
        }
        val partial = DesktopWavResult(summary + "\n${signal.rawBytes.size} raw bytes / ${signal.syncPoints.size} nibble sync points",
            diagnostics.toList(), pcm, signal.rawBytes, signal.rawByteMappings, signal.syncPoints)
        if (signal.rawBytes.unsignedByteAt(0) in setOf(7, 0x17)) return decodeS1Basic(partial, canonicalMachine(MachineBasicDialect.S1))
        if (signal.rawBytes.unsignedByteAt(0) in setOf(0x27, 0x37)) return decodeS2Basic(partial, canonicalMachine(MachineBasicDialect.S2))
        val payload = when (val decoded = S1BinaryCodec.decode(signal.rawBytes)) {
            is CodecResult.Failure -> return partial.copy(diagnostics = diagnostics + decoded.diagnostics)
            is CodecResult.Success -> decoded.value.also { diagnostics += decoded.diagnostics }
        }
        val dump = PgpMemoryDumpWriter.write(payload.startAddress, payload.body.copyBytes())
        // Signal damage may hide bytes/records; keep the validated preview but do not offer a program export.
        val signalDamaged = diagnostics.any { it.stage == CodecStage.SIGNAL_DECODE && it.severity != CodecDiagnosticSeverity.INFO }
        return partial.copy(summary = partial.summary + "\nValidated S1 BINARY, filename: ${payload.filename}\n" +
            "${payload.body.size} logical bytes / ${hex(payload.startAddress)}..${hex(payload.endAddress)}" +
                "\nReserved metadata: " + payload.copyReservedMetadata().joinToString(" ") { (it.toInt() and 255).toString(16).uppercase().padStart(2, '0') },
            diagnostics = diagnostics.toList(), s1Payload = payload, content = dump,
            programName = if (signalDamaged) null else "${payload.filename.ifBlank { "program" }}.dmp",
            program = if (signalDamaged) null else dump.encodeToByteArray(),
            hexProgram = if (signalDamaged) null else DesktopWavBinaryFiles.writeHex(payload.startAddress, payload.body.copyBytes()))
    }

    fun hex(value: Int): String = value.toString(16).uppercase().padStart(4, '0')
    fun rawText(raw: RawTransferByteSequence): String = raw.copyBytes().asList().chunked(16).mapIndexed { row, bytes ->
        "${hex(row * 16)}  " + bytes.joinToString(" ") { (it.toInt() and 255).toString(16).uppercase().padStart(2, '0') }
    }.joinToString("\n")
}
