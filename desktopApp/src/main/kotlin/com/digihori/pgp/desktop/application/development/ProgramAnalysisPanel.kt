package com.digihori.pgp.desktop.application.development

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** File selection stays on the host. Only an explicitly reviewed in-memory copy is published. */
@Composable
internal fun ProgramAnalysisPanel(
    service: ProgramAnalysisService,
    metadata: JsonObject,
    onChoose: () -> Pair<String, ByteArray>?,
    onPublish: (AnalysisProgram) -> Unit,
    onClear: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    var kind by remember { mutableStateOf(AnalysisProgramKind.BASIC) }
    var selected by remember { mutableStateOf<Pair<String, ByteArray>?>(null) }
    var loadAddress by remember { mutableStateOf("0xC000") }
    var entryAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<AnalysisProgram?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    fun address(text: String, optional: Boolean): Int? {
        if (optional && text.isBlank()) return null
        val normalized = text.trim().removePrefix("0x").removePrefix("0X")
        return normalized.toIntOrNull(16)?.takeIf { it in 0..65535 } ?: throw DevelopmentReadError("INVALID_ARGUMENTS")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(strings.text("analysis.heading"), style = MaterialTheme.typography.headlineSmall)
        Text(strings.text("analysis.scope"))
        Text(strings.text("analysis.machine"))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalysisProgramKind.entries.forEach { value ->
                FilterChip(selected = kind == value, onClick = { kind = value; preview = null; error = null }, label = { Text(value.name) })
            }
        }
        TextButton(onClick = {
            error = null; preview = null
            try { onChoose()?.let { selected = it } } catch (_: Exception) { error = "ANALYSIS_READ_FAILED" }
        }) { Text(strings.text("analysis.choose")) }
        selected?.let { Text(strings.text("analysis.selected", it.first, it.second.size)) }
        if (kind == AnalysisProgramKind.RAW) OutlinedTextField(loadAddress, { loadAddress = it; preview = null },
            label = { Text(strings.text("analysis.loadAddress")) }, singleLine = true)
        if (kind in setOf(AnalysisProgramKind.RAW, AnalysisProgramKind.DMP)) OutlinedTextField(entryAddress, { entryAddress = it; preview = null },
            label = { Text(strings.text("analysis.entryAddress")) }, singleLine = true)
        OutlinedTextField(notes, { if (it.length <= 2048) { notes = it; preview = null } }, label = { Text(strings.text("analysis.notes")) }, modifier = Modifier.fillMaxWidth())
        Button(enabled = selected != null, onClick = {
            error = null; preview = null
            try {
                val file = checkNotNull(selected)
                preview = service.prepare(file.first, file.second, kind, "pc-1245",
                    if (kind == AnalysisProgramKind.RAW) address(loadAddress, false) else null,
                    if (kind in setOf(AnalysisProgramKind.DMP, AnalysisProgramKind.RAW)) address(entryAddress, true) else null, notes)
            } catch (e: DevelopmentReadError) { error = e.code }
              catch (_: Exception) { error = "ANALYSIS_READ_FAILED" }
        }) { Text(strings.text("analysis.preview")) }
        error?.let { Text(strings.text("analysis.error", it), color = MaterialTheme.colorScheme.error) }
        preview?.let { program ->
            Text(strings.text("analysis.review"), style = MaterialTheme.typography.titleMedium)
            Text(strings.text("analysis.summary", program.name, program.kind.name, program.inputBytes))
            Text(strings.text("analysis.entry", program.entryAddress?.let { "0x${it.toString(16).uppercase().padStart(4, '0')}" } ?: strings.text("analysis.unknown")))
            SelectionContainer { Text("SHA-256: ${program.inputSha256}", fontFamily = FontFamily.Monospace) }
            program.source?.let { source ->
                Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    SelectionContainer { Text(source.take(8192), fontFamily = FontFamily.Monospace) }
                }
                if (source.length > 8192) Text(strings.text("analysis.previewTruncated"))
            }
            if (program.segments.isNotEmpty()) {
                Text(strings.text("analysis.binaryPreview"))
                Column(Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                  program.segments.take(8).forEach { segment ->
                    val bytes = segment.copyBytes().take(128).joinToString(" ") { (it.toInt() and 0xff).toString(16).uppercase().padStart(2, '0') }
                    SelectionContainer { Text("0x${segment.start.toString(16).uppercase().padStart(4, '0')}: $bytes", fontFamily = FontFamily.Monospace) }
                  }
                }
            }
            Button(onClick = { onPublish(program); preview = null; selected = null }) { Text(strings.text("analysis.publish")) }
        }
        HorizontalDivider()
        Text(strings.text("analysis.current"), style = MaterialTheme.typography.titleMedium)
        val target = metadata["target"]?.takeIf { it != JsonNull }?.jsonObject
        if (target == null) Text(strings.text("analysis.none")) else {
            Text(strings.text("analysis.summary", target.getValue("name").jsonPrimitive.content,
                target.getValue("kind").jsonPrimitive.content, target.getValue("inputBytes").jsonPrimitive.content))
            SelectionContainer { Text(strings.text("analysis.targetId", target.getValue("targetId").jsonPrimitive.content), fontFamily = FontFamily.Monospace) }
        }
        TextButton(onClick = onClear) { Text(strings.text("analysis.clear")) }
    }
}
