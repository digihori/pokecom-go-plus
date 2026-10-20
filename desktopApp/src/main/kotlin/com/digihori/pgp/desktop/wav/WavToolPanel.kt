package com.digihori.pgp.desktop.wav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.digihori.pgp.desktop.localization.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineBasicDialect
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.wav.WavByteSequence
import java.awt.Frame
import java.awt.FileDialog
import java.awt.EventQueue
import java.io.File
import kotlinx.coroutines.*

internal data class WavToolInput(val file: File, val origin: String, val machine: MachineId, val request: Long, val fromProject: Boolean = false)

@Composable
internal fun WavToolPanel(owner: Frame?, request: WavToolInput?, closeRequested: Boolean, onCloseHandled: () -> Unit, onClose: () -> Unit, strings: StudioStrings = StudioStrings.system) {
    var input by remember { mutableStateOf<WavToolInput?>(null) }
    var filename by remember { mutableStateOf("") }
    var format by remember { mutableStateOf<MachineBasicDialect?>(MachineBasicDialect.OLD) }
    var targetMachine by remember { mutableStateOf(MachineId("pc-1245")) }
    var encodeKind by remember { mutableStateOf("basic") }
    var decode by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<DesktopWavResult?>(null) }
    var settingsAtRun by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var unsaved by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<WavToolInput?>(null) }
    var pendingClose by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf("Content") }
    var selectedSample by remember(result) { mutableStateOf(0) }
    var rawPage by remember(result) { mutableStateOf(0) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val settings = if (decode) "AUTO-DECODE" else "${format?.name ?: "UNKNOWN"}/${targetMachine.value}/$filename/$encodeKind"
    fun inputMachine() = if (MachineCatalog.find(targetMachine)?.basicDialect == format) targetMachine else MachineId(when (format) {
        MachineBasicDialect.S1 -> "pc-1350"
        MachineBasicDialect.S2 -> "pc-1360"
        else -> "pc-1245"
    })
    fun accept(value: WavToolInput) {
        input = value
        targetMachine = value.machine
        filename = value.file.nameWithoutExtension
        if (value.fromProject) {
            val defaults = projectWavDefaults(value.machine, value.file.extension)
            format = defaults.first
            encodeKind = defaults.second
        } else {
            format = MachineCatalog.find(value.machine)?.basicDialect
            if (format == MachineBasicDialect.S2 && encodeKind == "binary") encodeKind = "basic"
        }
        decode = value.file.extension.equals("wav", true)
        result = null
        status = ""
        unsaved = false
    }
    fun open(value: WavToolInput) {
        job?.cancel()
        busy = false
        if (unsaved) pending = value else accept(value)
    }
    LaunchedEffect(request?.request) { request?.let { open(it) } }
    LaunchedEffect(closeRequested) {
        if (closeRequested) {
            onCloseHandled()
            if (unsaved) pendingClose = true else onClose()
        }
    }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }
    fun save(name: String, bytes: ByteArray, generated: Boolean) {
        val destination = choose(owner, strings.text("wav.saveTitle", name), name, FileDialog.SAVE) ?: return
        runCatching { destination.writeBytes(bytes) }.fold(
            onSuccess = { status = strings.text("wav.saved", destination.absolutePath); if (generated) unsaved = false },
            onFailure = { status = strings.text("wav.saveFailed", it.message.orEmpty()) },
        )
    }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(strings.text("wav.title"), style = MaterialTheme.typography.titleLarge)
        Text(strings.text("wav.future"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = {
                choose(owner, strings.text("wav.chooseCreate"), "*.bas;*.dmp;*.hex;*.ihx")?.let {
                    if (it.extension.lowercase() !in setOf("bas", "dmp", "hex", "ihx")) status = strings.text("wav.chooseInput")
                    else open(WavToolInput(it, strings.text("wav.origin"), inputMachine(), System.nanoTime()))
                }
            }) { Text(strings.text("wav.create")) }
            Button(enabled = !busy, onClick = {
                choose(owner, strings.text("wav.chooseOpen"), "*.wav")?.let { open(WavToolInput(it, strings.text("wav.origin"), inputMachine(), System.nanoTime())) }
            }) { Text(strings.text("wav.open")) }
        }
        input?.let { source ->
            SelectionContainer { Text(strings.text("wav.source", source.origin, source.file.absolutePath)) }
            val binaryInput = encodeKind == "binary"
            val matchingInput = decode || if (binaryInput) source.file.extension.lowercase() in setOf("dmp", "hex", "ihx") else source.file.extension.equals("bas", true)
            val supported = decode || DesktopWavOperation.supported(format, decode, binaryInput)
            if (!decode) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MachineBasicDialect.entries.forEach { candidate ->
                    OutlinedButton(enabled = !busy, onClick = { if (format != candidate) encodeKind = "basic"; format = candidate; targetMachine = inputMachine() }) {
                        Text((if (format == candidate) "✓ " else "") + candidate.name)
                    }
                }
            }
            if (!supported) Text(strings.text("wav.unsupported", format?.name ?: strings.text("wav.unknown")))
            if (!decode) {
                Text(strings.text("wav.encodeFormat"), style = MaterialTheme.typography.titleMedium)
                (if (format == MachineBasicDialect.S2) listOf("basic", "protected") else listOf("basic", "protected", "binary")).forEach { kind ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = !busy) { encodeKind = kind }) {
                        RadioButton(encodeKind == kind, { encodeKind = kind }, enabled = !busy)
                        Text(strings.text("wav.kind.$kind"))
                    }
                }
                if (!matchingInput) Text(strings.text(if (binaryInput) "wav.needBinaryInput" else "wav.needBasicInput"))
                OutlinedTextField(filename, { filename = it }, enabled = !busy, label = { Text(strings.text("wav.filename")) }, singleLine = true)

            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy && supported && matchingInput, onClick = {
                    val selectedMachine = inputMachine()
                    val selectedFilename = filename
                    val selectedType12 = encodeKind == "protected"
                    val selectedDecode = decode
                    val capturedSettings = settings
                    busy = true
                    job = scope.launch {
                        try {
                            val next = withContext(Dispatchers.Default) {
                                runCatching {
                                    val bytes = source.file.readBytes()
                                    if (selectedDecode) DesktopWavOperation.decode(WavByteSequence(bytes))
                                    else DesktopWavOperation.encode(bytes, binaryInput, selectedMachine, selectedFilename, selectedType12)
                                }.getOrElse { DesktopWavResult("Operation failed", error = it.message ?: it.toString()) }
                            }
                            result = next
                            settingsAtRun = capturedSettings
                            unsaved = next.wav != null || next.program != null
                            status = if (next.error != null || next.diagnostics.any { it.severity == com.digihori.pgp.core.wav.CodecDiagnosticSeverity.ERROR })
                                strings.text("wav.stopped")
                            else strings.text("wav.complete")
                        } finally { busy = false }
                    }
                }) { Text(if (busy) strings.text("wav.processing") else if (decode) strings.text("wav.analyze") else strings.text("wav.convert")) }
                if (busy) OutlinedButton(onClick = { job?.cancel(); status = strings.text("wav.cancelled") }) { Text(strings.text("wav.cancel")) }
            }
        } ?: Text(strings.text("wav.empty"))
        if (status.isNotBlank()) SelectionContainer { Text(status) }
        result?.let { output ->
            if (settings != settingsAtRun) Text(strings.text("wav.changed", settingsAtRun))
            SelectionContainer { Text(strings.wavSummary(output.summary)) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                output.wav?.let { wav -> Button(onClick = { save("${output.transferFilename}.wav", wav.copyBytes(), true) }) { Text(strings.text("wav.saveWav")) } }
                output.program?.let { bytes -> Button(onClick = { save(output.programName!!, bytes, true) }) { Text(strings.text("wav.saveProgram")) } }
                output.hexProgram?.let { bytes -> OutlinedButton(onClick = { save("${output.transferFilename}.hex", bytes, true) }) { Text(strings.text("wav.saveHex")) } }
                output.raw?.let { raw -> OutlinedButton(onClick = { save("raw-transfer.bin", raw.copyBytes(), false) }) { Text(strings.text("wav.exportRaw")) } }
                OutlinedButton(onClick = { save("wav-analysis.txt", ("Input: ${input?.file?.absolutePath}\nSettings: $settingsAtRun\n" + output.report()).encodeToByteArray(), false) }) { Text(strings.text("wav.exportReport")) }
            }
            Text(strings.text("wav.exportHelp"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Content", "Raw", "Waveform").forEach { label -> OutlinedButton(onClick = { view = label }) { Text(strings.text("wav.tab.$label")) } }
            }
            when (view) {
                "Content" -> {
                    if (output.content.length > 16000) Text(strings.text("wav.previewLimit"))
                    SelectionContainer { Text(output.content.take(16000).ifBlank { strings.text("wav.noProgram") }, fontFamily = FontFamily.Monospace) }
                }
                "Raw" -> {
                    Text(if (!output.validated) strings.text("wav.unvalidated") else strings.text("wav.validated"))
                    output.raw?.let { raw ->
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = rawPage > 0, onClick = { rawPage-- }) { Text(strings.text("wav.previous")) }
                            OutlinedButton(enabled = (rawPage + 1) * 256 < raw.size, onClick = { rawPage++ }) { Text(strings.text("wav.next")) }
                        }
                        val bytes = remember(raw) { raw.copyBytes() }
                        val begin = rawPage * 256
                        val end = (begin + 256).coerceAtMost(bytes.size)
                        Text(strings.text("wav.rawPage", begin.toString(), end.toString(), bytes.size.toString()))
                        for (offset in begin until end step 16) {
                            val rowEnd = (offset + 16).coerceAtMost(end)
                            val mapping = output.mappings.firstOrNull { it.target.offset in offset until rowEnd }
                            Text(DesktopWavOperation.hex(offset) + "  " + bytes.copyOfRange(offset, rowEnd).joinToString(" ") {
                                (it.toInt() and 255).toString(16).uppercase().padStart(2, '0')
                            }, fontFamily = FontFamily.Monospace, modifier = Modifier.semantics { contentDescription = strings.text("wav.rawRow", offset.toString(), (rowEnd - 1).toString()) }.clickable {
                                mapping?.let { selectedSample = it.source.offset; view = "Waveform" }
                            })
                        }
                    }
                }
                "Waveform" -> WavWaveform(output, selectedSample, { selectedSample = it }, { byte -> rawPage = byte / 256; view = "Raw" }, strings)
            }
            Text(strings.text("wav.diagnostics"), style = MaterialTheme.typography.titleMedium)
            output.error?.let { SelectionContainer { Text(output.uiError(strings)) } }
            if (output.diagnostics.isEmpty()) Text(strings.text("wav.noDiagnostics"))
            output.diagnostics.forEach { diagnostic ->
                Text(diagnostic.uiDescription(strings), modifier = Modifier.semantics { contentDescription = strings.text("wav.diagnosticAction", diagnostic.code.name) }.clickable {
                    diagnostic.range?.let { range ->
                        when (range.kind) {
                            com.digihori.pgp.core.wav.CodecDataKind.PCM_SAMPLES -> { selectedSample = range.offset.coerceIn(0, ((output.pcm?.samples?.size ?: 1) - 1).coerceAtLeast(0)); view = "Waveform" }
                            com.digihori.pgp.core.wav.CodecDataKind.RAW_BYTES -> { rawPage = rawPageFor(range.offset, output.raw?.size ?: 0); view = "Raw" }
                            else -> Unit
                        }
                    }
                })
            }
        }
    }
    VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(androidx.compose.ui.Alignment.CenterEnd).fillMaxHeight())
    }
    if (pendingClose) AlertDialog(
        onDismissRequest = { pendingClose = false }, title = { Text(strings.text("wav.closeUnsaved")) },
        text = { Text(strings.text("wav.unsaved")) },
        confirmButton = { TextButton(onClick = { pendingClose = false; onClose() }) { Text(strings.text("wav.close")) } },
        dismissButton = { TextButton(onClick = { pendingClose = false }) { Text(strings.text("wav.keepOpen")) } },
    )
    if (pending != null) AlertDialog(
        onDismissRequest = { pending = null }, title = { Text(strings.text("wav.replaceUnsaved")) },
        text = { Text(strings.text("wav.unsaved")) },
        confirmButton = { TextButton(onClick = { pending?.let { accept(it) }; pending = null }) { Text(strings.text("wav.replace")) } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text(strings.text("wav.keepCurrent")) } },
    )
}

private fun choose(owner: Frame?, title: String, suggested: String, mode: Int = FileDialog.LOAD): File? {
    val dialog = FileDialog(owner, title, mode).apply { file = suggested; isVisible = true }
    val directory = dialog.directory; val name = dialog.file
    dialog.dispose()
    EventQueue.invokeLater { owner?.let { it.toFront(); it.requestFocus(); it.requestFocusInWindow() } }
    return if (directory != null && name != null) File(directory, name) else null
}

internal fun rawPageFor(offset: Int, size: Int): Int = offset.coerceIn(0, (size - 1).coerceAtLeast(0)) / 256

internal fun projectWavDefaults(machine: MachineId, extension: String): Pair<MachineBasicDialect?, String> {
    val dialect = MachineCatalog.find(machine)?.basicDialect
    val binary = extension.lowercase() in setOf("dmp", "hex", "ihx")
    // S2 binary uses the S1 transfer format; the S2 selector offers BASIC only.
    return (if (binary && dialect == MachineBasicDialect.S2) MachineBasicDialect.S1 else dialect) to
        (if (binary) "binary" else "basic")
}
