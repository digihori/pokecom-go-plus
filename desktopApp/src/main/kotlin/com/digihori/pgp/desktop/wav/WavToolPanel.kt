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

internal data class WavToolInput(val file: File, val origin: String, val machine: MachineId, val request: Long)

@Composable
internal fun WavToolPanel(owner: Frame, request: WavToolInput?, closeRequested: Boolean, onCloseHandled: () -> Unit, onClose: () -> Unit) {
    var input by remember { mutableStateOf<WavToolInput?>(null) }
    var filename by remember { mutableStateOf("") }
    var format by remember { mutableStateOf<MachineBasicDialect?>(MachineBasicDialect.OLD) }
    var type12 by remember { mutableStateOf(false) }
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
    val settings = "${format?.name ?: "UNKNOWN"}/$filename/$type12/$decode"
    fun inputMachine() = MachineId(when (format) {
        MachineBasicDialect.S1 -> "pc-1350"
        MachineBasicDialect.S2 -> "pc-1360"
        else -> "pc-1245"
    })
    fun accept(value: WavToolInput) {
        input = value
        type12 = false
        filename = value.file.nameWithoutExtension
        format = MachineCatalog.find(value.machine)?.basicDialect
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
        val destination = choose(owner, "Save $name", name, FileDialog.SAVE) ?: return
        runCatching { destination.writeBytes(bytes) }.fold(
            onSuccess = { status = "Saved: ${destination.absolutePath}"; if (generated) unsaved = false },
            onFailure = { status = "Save failed: ${it.message}" },
        )
    }
    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("WAV Tool", style = MaterialTheme.typography.titleLarge)
        Text("S1/S2対応は開発中です。S1/S2のWAV変換にはpcwavツールを使用してください。")
        Text("Studio • OLD 02 / 12 / 62 • file conversion and analysis")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = {
                choose(owner, "Create WAV — open BASIC or memory dump", "*.bas;*.dmp")?.let {
                    if (it.extension.lowercase() !in setOf("bas", "dmp")) status = "Choose a .bas or addressed .dmp file."
                    else open(WavToolInput(it, "Existing file", inputMachine(), System.nanoTime()))
                }
            }) { Text("Create WAV…") }
            Button(enabled = !busy, onClick = {
                choose(owner, "Open WAV", "*.wav")?.let { open(WavToolInput(it, "Existing file", inputMachine(), System.nanoTime())) }
            }) { Text("Open WAV…") }
        }
        input?.let { source ->
            SelectionContainer { Text("${source.origin}\n${source.file.absolutePath}\nSaved file contents are used; external editor changes require another conversion.") }
            Text("${if (decode) "Decode / analyze" else "Encode"} • signal: OLD (S1/S2 not implemented) • hardware compatibility not verified")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MachineBasicDialect.entries.forEach { candidate ->
                    OutlinedButton(enabled = !busy, onClick = { format = candidate }) {
                        Text((if (format == candidate) "✓ " else "") + candidate.name +
                            (if (candidate != MachineBasicDialect.OLD) " (not implemented)" else ""))
                    }
                }
            }
            if (format != MachineBasicDialect.OLD) Text("${format?.name ?: "Unknown"} WAV is not implemented. Select OLD explicitly to use the OLD codec.")
            if (!decode) {
                OutlinedTextField(filename, { filename = it }, enabled = !busy, label = { Text("Pocket computer filename") }, singleLine = true)
                if (source.file.extension.equals("bas", true)) Row {
                    Checkbox(type12, { type12 = it }, enabled = !busy)
                    Text("BASIC type 12 (advanced; password entry not supported)")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy && format == MachineBasicDialect.OLD, onClick = {
                    val selectedMachine = inputMachine()
                    val selectedFilename = filename
                    val selectedType12 = type12
                    val selectedDecode = decode
                    val capturedSettings = settings
                    busy = true
                    job = scope.launch {
                        try {
                            val next = withContext(Dispatchers.Default) {
                                runCatching {
                                    val bytes = source.file.readBytes()
                                    if (selectedDecode) DesktopWavOperation.decode(WavByteSequence(bytes), selectedMachine)
                                    else DesktopWavOperation.encode(bytes, source.file.extension.equals("dmp", true), selectedMachine, selectedFilename, selectedType12)
                                }.getOrElse { DesktopWavResult("Operation failed", error = it.message ?: it.toString()) }
                            }
                            result = next
                            settingsAtRun = capturedSettings
                            unsaved = next.wav != null || next.program != null
                            status = if (next.error != null || next.diagnostics.any { it.severity == com.digihori.pgp.core.wav.CodecDiagnosticSeverity.ERROR })
                                "Stopped at an error. Available partial results are retained."
                            else "Conversion complete. Review the result before saving."
                        } finally { busy = false }
                    }
                }) { Text(if (busy) "Processing…" else if (decode) "Analyze / reanalyze" else "Convert") }
                if (busy) OutlinedButton(onClick = { job?.cancel(); status = "Cancelled; previous completed result retained." }) { Text("Cancel") }
            }
        } ?: Text("Create WAV from a .bas/.dmp file, or open WAV to decode and inspect it.")
        if (status.isNotBlank()) SelectionContainer { Text(status) }
        result?.let { output ->
            if (settings != settingsAtRun) Text("Settings changed — rerun required. Result below uses: $settingsAtRun")
            SelectionContainer { Text(output.summary) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                output.wav?.let { wav -> Button(onClick = { save("${output.payload?.filename?.ifBlank { "program" } ?: "program"}.wav", wav.copyBytes(), true) }) { Text("Save WAV…") } }
                output.program?.let { bytes -> Button(onClick = { save(output.programName!!, bytes, true) }) { Text("Save program…") } }
                output.raw?.let { raw -> OutlinedButton(onClick = { save("raw-transfer.bin", raw.copyBytes(), false) }) { Text("Export raw…") } }
                OutlinedButton(onClick = { save("wav-analysis.txt", ("Input: ${input?.file?.absolutePath}\nSettings: $settingsAtRun\n" + output.report()).encodeToByteArray(), false) }) { Text("Export report…") }
            }
            Text("Raw exports are analysis data, not a validated Program. BASIC/.dmp exports do not preserve all transfer metadata.")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Content", "Raw", "Waveform").forEach { label -> OutlinedButton(onClick = { view = label }) { Text(label) } }
            }
            when (view) {
                "Content" -> {
                    if (output.content.length > 16000) Text("Preview limited to 16000 characters; saved program contains the full result.")
                    SelectionContainer { Text(output.content.take(16000).ifBlank { "No program text available. Inspect raw and diagnostics." }, fontFamily = FontFamily.Monospace) }
                }
                "Raw" -> {
                    Text(if (output.payload == null) "Payload unvalidated" else "OLD payload validated")
                    output.raw?.let { raw ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(enabled = rawPage > 0, onClick = { rawPage-- }) { Text("Previous bytes") }
                            OutlinedButton(enabled = (rawPage + 1) * 256 < raw.size, onClick = { rawPage++ }) { Text("Next bytes") }
                        }
                        val bytes = remember(raw) { raw.copyBytes() }
                        val begin = rawPage * 256
                        val end = (begin + 256).coerceAtMost(bytes.size)
                        Text("Raw $begin..$end of ${bytes.size} • select a row to locate its waveform")
                        for (offset in begin until end step 16) {
                            val rowEnd = (offset + 16).coerceAtMost(end)
                            val mapping = output.mappings.firstOrNull { it.target.offset in offset until rowEnd }
                            Text(DesktopWavOperation.hex(offset) + "  " + bytes.copyOfRange(offset, rowEnd).joinToString(" ") {
                                (it.toInt() and 255).toString(16).uppercase().padStart(2, '0')
                            }, fontFamily = FontFamily.Monospace, modifier = Modifier.clickable {
                                mapping?.let { selectedSample = it.source.offset; view = "Waveform" }
                            })
                        }
                    }
                }
                "Waveform" -> WavWaveform(output, selectedSample, { selectedSample = it }, { byte -> rawPage = byte / 256; view = "Raw" })
            }
            Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
            output.error?.let { SelectionContainer { Text(it) } }
            if (output.diagnostics.isEmpty()) Text("No codec diagnostics")
            output.diagnostics.forEach { diagnostic ->
                Text(diagnostic.describe(), modifier = Modifier.clickable {
                    diagnostic.range?.let { range ->
                        when (range.kind) {
                            com.digihori.pgp.core.wav.CodecDataKind.PCM_SAMPLES -> { selectedSample = range.offset; view = "Waveform" }
                            com.digihori.pgp.core.wav.CodecDataKind.RAW_BYTES -> { rawPage = range.offset / 256; view = "Raw" }
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
        onDismissRequest = { pendingClose = false }, title = { Text("Close unsaved result?") },
        text = { Text("The current generated WAV/program has not been saved.") },
        confirmButton = { TextButton(onClick = { pendingClose = false; onClose() }) { Text("Close") } },
        dismissButton = { TextButton(onClick = { pendingClose = false }) { Text("Keep open") } },
    )
    if (pending != null) AlertDialog(
        onDismissRequest = { pending = null }, title = { Text("Replace unsaved result?") },
        text = { Text("The current generated WAV/program has not been saved.") },
        confirmButton = { TextButton(onClick = { pending?.let { accept(it) }; pending = null }) { Text("Replace") } },
        dismissButton = { TextButton(onClick = { pending = null }) { Text("Keep current") } },
    )
}

private fun choose(owner: Frame, title: String, suggested: String, mode: Int = FileDialog.LOAD): File? {
    val dialog = FileDialog(owner, title, mode).apply { file = suggested; isVisible = true }
    val directory = dialog.directory; val name = dialog.file
    dialog.dispose()
    EventQueue.invokeLater { owner.toFront(); owner.requestFocus(); owner.requestFocusInWindow() }
    return if (directory != null && name != null) File(directory, name) else null
}
