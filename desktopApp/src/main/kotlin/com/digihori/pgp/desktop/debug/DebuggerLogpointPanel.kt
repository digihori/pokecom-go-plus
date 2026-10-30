package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.input.emulatorTextInput
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

@Composable
internal fun DebuggerLogpointPanel(runner: DesktopEmulatorRunner?, keyboard: DesktopKeyboardInput,
    revision: Long, onSelect: (Int) -> Unit, strings: StudioStrings = StudioStrings.system) {
    var id by rememberSaveable(runner) { mutableStateOf<Long?>(null) }
    var address by rememberSaveable(runner) { mutableStateOf("0000") }
    var value by rememberSaveable(runner) { mutableStateOf("A") }
    var condition by rememberSaveable(runner) { mutableStateOf("") }
    var enabled by rememberSaveable(runner) { mutableStateOf(true) }
    var refresh by remember(runner) { mutableIntStateOf(0) }
    var error by remember(runner) { mutableStateOf<DebuggerLogpointError?>(null) }
    val paused = runner?.state == RunnerState.PAUSED
    val definitions = remember(runner, revision, refresh) { runner?.logpointRegistrations().orEmpty() }
    val entries = remember(runner, revision, refresh) { runner?.logpointEntries().orEmpty().asReversed() }
    fun accept(result: DebuggerLogpointResult?) {
        error = (result as? DebuggerLogpointResult.Failure)?.error
        if (result is DebuggerLogpointResult.Success) { id = null; refresh++ }
    }
    fun hex(n: Int) = n.toString(16).uppercase().padStart(4, '0')
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text("debug.log.title"), style = MaterialTheme.typography.titleSmall)
        Text(strings.text("debug.log.help"), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(address, { address = it; error = null }, label = { Text(strings.text("debug.log.address")) },
                modifier = Modifier.width(160.dp).emulatorTextInput(keyboard), singleLine = true)
            OutlinedTextField(value, { value = it; error = null }, label = { Text(strings.text("debug.log.value")) },
                modifier = Modifier.width(220.dp).emulatorTextInput(keyboard), singleLine = true)
        }
        OutlinedTextField(condition, { condition = it; error = null }, label = { Text(strings.text("debug.log.condition")) },
            modifier = Modifier.fillMaxWidth().emulatorTextInput(keyboard), singleLine = true)
        Row {
            Checkbox(enabled, { enabled = it })
            Text(strings.text("debug.log.enabled"))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = paused, onClick = {
                val pc = DebuggerMemorySearch.address(address)
                if (pc == null) error = DebuggerLogpointError.ADDRESS
                else accept(runner?.saveLogpoint(id, pc, value, condition, enabled))
            }) { Text(strings.text(if (id == null) "debug.log.add" else "debug.log.apply")) }
            if (id != null) TextButton(onClick = { id = null; error = null }) { Text(strings.text("debug.log.cancel")) }
        }
        if (!paused) Text(strings.text("debug.log.error.PAUSED_ONLY"))
        error?.let { Text(strings.text("debug.log.error.$it"), color = MaterialTheme.colorScheme.error) }
        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            definitions.forEach { point ->
                Text("#${point.id} ${hex(point.address)} ${point.value} ${point.condition} " + strings.text(if (point.enabled) "debug.log.on" else "debug.log.off"))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(enabled = paused, onClick = {
                        id = point.id; address = hex(point.address); value = point.value; condition = point.condition; enabled = point.enabled; error = null
                    }) { Text(strings.text("debug.log.edit", point.id.toString())) }
                    TextButton(enabled = paused, onClick = { accept(runner?.saveLogpoint(point.id, point.address, point.value, point.condition, !point.enabled)) }) {
                        Text(strings.text("debug.log.toggle", point.id.toString()))
                    }
                    TextButton(enabled = paused, onClick = { accept(runner?.removeLogpoint(point.id)) }) { Text(strings.text("debug.log.remove", point.id.toString())) }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = runner != null, onClick = { refresh++ }) { Text(strings.text("debug.log.refresh")) }
            Button(enabled = paused, onClick = { runner?.clearLogpointHistory(); refresh++ }) { Text(strings.text("debug.log.clear")) }
        }
        Text(strings.text("debug.log.count", entries.size.toString(), (runner?.logpointDropped() ?: 0).toString()))
        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            entries.forEach { entry ->
                Text("#${entry.sequence} LP#${entry.id} PC=${hex(entry.address)} ${entry.operand}=" +
                    (entry.value?.let { "0x${hex(it)}" } ?: strings.text("debug.log.readFailed")) +
                    (entry.rom?.let { " [${it.componentId.value} bank=${it.bank ?: "—"} offset=${hex(it.offset)}]" } ?: ""))
                TextButton(enabled = paused, onClick = { if (runner?.state == RunnerState.PAUSED) onSelect(entry.address) }) {
                    Text(strings.text("debug.log.location", hex(entry.address)))
                }
            }
        }
    }
}
