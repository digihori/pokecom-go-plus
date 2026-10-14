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

@Composable
internal fun DesktopAssemblerSettingsPanel(layout: com.digihori.pgp.core.api.MachineMemoryLayout) {
    val initial = remember { DesktopUiPreferences.assemblerSettings() }
    var external by remember { mutableStateOf(initial.external) }
    var executable by remember { mutableStateOf(initial.executable) }
    var arguments by remember { mutableStateOf(initial.arguments) }
    var timeout by remember { mutableStateOf(initial.timeoutSeconds.toString()) }
    var source by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    fun settings(): DesktopAssemblerSettings {
        val seconds = timeout.toIntOrNull()
        require(seconds != null && seconds in 1..3600) { uiText("assembler.timeoutError") }
        return DesktopAssemblerSettings(external, executable, arguments, seconds).also {
            if (external) it.command(File("source.asm"))
        }
    }
    Text(uiText("assembler.scopeHelp"))
    AssemblerSelectionOptions(external, { external = it }, !running)
    OutlinedTextField(executable, { executable = it }, label = { Text(uiText("assembler.executable")) },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(arguments, { arguments = it }, label = { Text(uiText("assembler.arguments")) },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    Text(uiText("assembler.commandHelp"))
    OutlinedTextField(timeout, { timeout = it }, label = { Text(uiText("assembler.timeout")) },
        singleLine = true, enabled = !running)
    Button(enabled = !running, onClick = {
        runCatching { DesktopUiPreferences.saveAssemblerSettings(settings()) }
            .onSuccess { message = uiText("assembler.saved") }
            .onFailure { message = StudioStrings.system.projectOperationError(it.message.orEmpty()) }
    }) { Text(uiText("assembler.apply")) }
    Text(uiText("assembler.selectionHelp"))
    OutlinedTextField(source, { source = it }, label = { Text(uiText("assembler.testSource")) },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    Text(uiText("assembler.testHelp"))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !running, onClick = {
            val configured = runCatching { settings().also { it.command(File(source)) } }
            configured.onFailure { message = StudioStrings.system.projectOperationError(it.message.orEmpty()) }.onSuccess { config ->
                running = true
                message = uiText("assembler.running")
                job = scope.launch {
                    try {
                        message = when (val result = DesktopSc61Assembler.assemble(config, File(source), layout)) {
                            is DesktopSc61AssemblyResult.Success -> uiText("assembler.imported" , result.image.byteCount.toString(), result.exitCode.toString()) +
                                result.image.segments.joinToString("\n") { "%04X..%04X".format(it.startAddress, it.endAddress) } + "\n${result.log}"
                            is DesktopSc61AssemblyResult.Failure -> uiText("assembly.failureDetail", StudioStrings.system.projectOperationError(result.message)) + "\n" + result.log
                        }
                    } catch (cancelled: CancellationException) {
                        message = uiText("assembler.cancelled")
                        throw cancelled
                    } finally { running = false }
                }
            }
        }) { Text(uiText("assembler.test")) }
        OutlinedButton(enabled = running, onClick = { job?.cancel() }) { Text(uiText("assembler.cancel")) }
    }
    SelectionContainer {
        Text(message, modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()))
    }
}


@Composable
internal fun AssemblerSelectionOptions(
    external: Boolean, onExternalChange: (Boolean) -> Unit, enabled: Boolean,
    strings: StudioStrings = StudioStrings.system,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (isExternal in listOf(false, true)) {
            val label = strings.text(if (isExternal) "assembler.external" else "assembler.internal")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(external == isExternal, onClick = { onExternalChange(isExternal) }, enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = label })
                Text(label)
            }
        }
    }
}
