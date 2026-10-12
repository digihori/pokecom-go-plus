package com.digihori.pgp.desktop.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
        require(seconds != null && seconds in 1..3600) { "Timeout must be 1..3600 seconds" }
        return DesktopAssemblerSettings(external, executable, arguments, seconds).also {
            if (external) it.command(File("source.asm"))
        }
    }
    Row {
        RadioButton(!external, onClick = { external = false }, enabled = !running)
        Text("Internal")
        RadioButton(external, onClick = { external = true }, enabled = !running)
        Text("External")
    }
    OutlinedTextField(executable, { executable = it }, label = { Text("Executable path (SC61)") },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(arguments, { arguments = it }, label = { Text("Arguments") },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    Text("SC61: {source}. Quote arguments containing spaces. Runs in the source folder.")
    OutlinedTextField(timeout, { timeout = it }, label = { Text("Timeout in seconds") },
        singleLine = true, enabled = !running)
    Button(enabled = !running, onClick = {
        runCatching { DesktopUiPreferences.saveAssemblerSettings(settings()) }
            .onSuccess { message = "Assembler settings saved." }
            .onFailure { message = it.message.orEmpty() }
    }) { Text("Apply assembler setting") }
    Text("The saved selection is used by Workspace, Build / Build & Load and Quick Assemble. External mode currently supports SC61 DMP output.")
    OutlinedTextField(source, { source = it }, label = { Text("Test source file path") },
        singleLine = true, enabled = !running, modifier = Modifier.fillMaxWidth())
    Text("SC61 output is isolated. This test imports the DMP without loading it; machine placement is checked when loading.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !running, onClick = {
            val configured = runCatching { settings().also { it.command(File(source)) } }
            configured.onFailure { message = it.message.orEmpty() }.onSuccess { config ->
                running = true
                message = "Running external assembler…"
                job = scope.launch {
                    try {
                        message = when (val result = DesktopSc61Assembler.assemble(config, File(source), layout)) {
                            is DesktopSc61AssemblyResult.Success -> "DMP imported: ${result.image.byteCount} bytes (exit code ${result.exitCode})\n" +
                                result.image.segments.joinToString("\n") { "%04X..%04X".format(it.startAddress, it.endAddress) } + "\n${result.log}"
                            is DesktopSc61AssemblyResult.Failure -> "${result.message}\n${result.log}"
                        }
                    } catch (cancelled: CancellationException) {
                        message = "Assembler cancelled."
                        throw cancelled
                    } finally { running = false }
                }
            }
        }) { Text("Run connection test") }
        OutlinedButton(enabled = running, onClick = { job?.cancel() }) { Text("Cancel") }
    }
    SelectionContainer {
        Text(message, modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()))
    }
}
