package com.digihori.pgp.desktop.project

import com.digihori.pgp.desktop.localization.uiText
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.localization.projectOperationError

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.window.DesktopUiPreferences
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private data class AssemblerProfileDraft(
    val executable: String, val arguments: String, val timeout: String,
    val successExitCode: String, val source: String,
) {
    fun settings(tool: DesktopAssemblerTool): DesktopAssemblerSettings {
        val seconds = timeout.toIntOrNull()
        require(seconds != null && seconds in 1..3600) { "Timeout must be 1..3600 seconds" }
        val code = successExitCode.toIntOrNull()
        require(code != null && code in 0..255) { "Success exit code must be 0..255" }
        return DesktopAssemblerSettings(true, executable, arguments, seconds, tool, code, source)
    }
}

@Composable
internal fun DesktopAssemblerSettingsPanel(
    layout: com.digihori.pgp.core.api.MachineMemoryLayout,
    onSettingsChanged: (DesktopAssemblerSettings) -> Unit = {},
    store: DesktopAssemblerPreferences = DesktopUiPreferences.assemblerStore(),
    strings: StudioStrings = StudioStrings.system,
) {
    val initial = remember { store.load() }
    var active by remember { mutableStateOf(initial) }
    var editing by remember { mutableStateOf<DesktopAssemblerTool?>(if (initial.external) initial.tool else null) }
    var drafts by remember { mutableStateOf(DesktopAssemblerTool.entries.associateWith { tool ->
        val p = store.profile(tool)
        AssemblerProfileDraft(p.executable, p.arguments, p.timeoutSeconds.toString(), p.successExitCode.toString(), p.testSource)
    }) }
    var message by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    fun refreshActive() {
        active = store.load()
        onSettingsChanged(active)
    }
    Text(strings.text("assembler.activeSelection"), style = MaterialTheme.typography.titleSmall)
    fun select(tool: DesktopAssemblerTool?) {
        runCatching {
            tool?.let { store.profile(it).command(File("source.asm")) }
            store.select(tool)
        }.onSuccess { refreshActive(); message = strings.text("assembler.activeChanged") }
            .onFailure { message = strings.projectOperationError(it.message.orEmpty()) }
    }
    AssemblerSelectionOptions(active.external, { if (!it) select(null) }, !running,
        strings = strings, tool = active.tool, onToolChange = { select(it) })
    Text(strings.text("assembler.current", active.assemblerLabel(strings)), style = MaterialTheme.typography.bodySmall)
    Text(strings.text("assembler.profileHelp"))
    val tabs = listOf(null) + DesktopAssemblerTool.entries
    TabRow(selectedTabIndex = tabs.indexOf(editing)) {
        tabs.forEach { tool ->
            Tab(selected = editing == tool, enabled = !running, onClick = { editing = tool; message = "" },
                text = { Text(strings.text(when (tool) {
                    null -> "assembler.internal"
                    DesktopAssemblerTool.SC61 -> "assembler.sc61"
                    DesktopAssemblerTool.YASM -> "assembler.yasm"
                    DesktopAssemblerTool.GENERIC -> "assembler.genericTab"
                })) })
        }
    }
    val tool = editing
    if (tool == null) {
        Text(strings.text("assembler.internalHelp"))
    } else {
        val draft = drafts.getValue(tool)
        fun update(value: AssemblerProfileDraft) { drafts = drafts + (tool to value) }
        OutlinedTextField(draft.executable, { update(draft.copy(executable = it)) }, label = { Text(strings.text("assembler.executable")) },
            singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(draft.arguments, { update(draft.copy(arguments = it)) }, label = { Text(strings.text("assembler.arguments")) },
            singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
        Text(strings.text("assembler.commandHelp"))
        if (tool == DesktopAssemblerTool.YASM) Text(strings.text("assembler.yasmHelp"))
        if (tool == DesktopAssemblerTool.GENERIC) {
            OutlinedTextField(draft.successExitCode, { update(draft.copy(successExitCode = it)) },
                label = { Text(strings.text("assembler.successExitCode")) }, singleLine = true, enabled = !running)
            Text(strings.text("assembler.genericHelp"))
        }
        OutlinedTextField(draft.timeout, { update(draft.copy(timeout = it)) }, label = { Text(strings.text("assembler.timeout")) },
            singleLine = true, enabled = !running)
        OutlinedTextField(draft.source, { update(draft.copy(source = it)) }, label = { Text(strings.text("assembler.testSource")) },
            singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !running, onClick = {
                runCatching {
                    val value = draft.settings(tool)
                    if (active.external && active.tool == tool) value.command(File("source.asm"))
                    store.saveProfile(value)
                }.onSuccess { refreshActive(); message = strings.text("assembler.profileSaved") }
                    .onFailure { message = strings.projectOperationError(it.message.orEmpty()) }
            }) { Text(strings.text("assembler.saveProfile")) }
            OutlinedButton(enabled = !running, onClick = {
                runCatching { draft.settings(tool).also { it.command(File(draft.source)) } }
                    .onFailure { message = strings.projectOperationError(it.message.orEmpty()) }
                    .onSuccess { config ->
                        running = true
                        message = strings.text("assembler.running")
                        job = scope.launch {
                            try {
                                message = when (val result = DesktopConfiguredExternalAssembler.assemble(config, File(draft.source), layout)) {
                                    is DesktopExternalAssemblyResult.Success -> strings.text("assembler.imported", result.image.byteCount.toString(), result.exitCode.toString()) +
                                        result.image.segments.joinToString("\n") { "%04X..%04X".format(it.startAddress, it.endAddress) } + "\n${result.log}"
                                    is DesktopExternalAssemblyResult.Failure -> strings.text("assembly.failureDetail", strings.projectOperationError(result.message)) + "\n" + result.log
                                }
                            } catch (cancelled: CancellationException) { message = strings.text("assembler.cancelled"); throw cancelled }
                            finally { running = false }
                        }
                    }
            }) { Text(strings.text("assembler.test")) }
            if (running) OutlinedButton(onClick = { job?.cancel() }) { Text(strings.text("assembler.cancel")) }
        }
        Text(strings.text("assembler.testHelp"))
    }
    SelectionContainer {
        Text(message, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
    }
}

internal fun DesktopAssemblerSettings.assemblerLabel(strings: StudioStrings = StudioStrings.system): String =
    strings.text(if (!external) "assembler.internal" else when (tool) {
        DesktopAssemblerTool.SC61 -> "assembler.sc61"
        DesktopAssemblerTool.YASM -> "assembler.yasm"
        DesktopAssemblerTool.GENERIC -> "assembler.genericTab"
    })


@Composable
internal fun AssemblerSelectionOptions(
    external: Boolean, onExternalChange: (Boolean) -> Unit, enabled: Boolean,
    strings: StudioStrings = StudioStrings.system,
    tool: DesktopAssemblerTool = DesktopAssemblerTool.SC61,
    onToolChange: (DesktopAssemblerTool) -> Unit = {},
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (candidate in listOf(null) + DesktopAssemblerTool.entries) {
            val label = strings.text(when (candidate) {
                null -> "assembler.internal"
                DesktopAssemblerTool.SC61 -> "assembler.sc61"
                DesktopAssemblerTool.YASM -> "assembler.yasm"
                DesktopAssemblerTool.GENERIC -> "assembler.generic"
            })
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(if (candidate == null) !external else external && tool == candidate,
                    onClick = { onExternalChange(candidate != null); candidate?.let(onToolChange) }, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = label })
                Text(label)
            }
        }
    }
}
