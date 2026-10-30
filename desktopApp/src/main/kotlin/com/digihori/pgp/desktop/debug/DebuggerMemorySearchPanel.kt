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
internal fun DebuggerMemorySearchPanel(runner: DesktopEmulatorRunner?, keyboardInput: DesktopKeyboardInput,
    revision: Long, onSelect: (Int) -> Unit, strings: StudioStrings = StudioStrings.system) {
    var start by rememberSaveable(runner) { mutableStateOf("0000") }
    var end by rememberSaveable(runner) { mutableStateOf("FFFF") }
    var query by rememberSaveable(runner) { mutableStateOf("") }
    var format by rememberSaveable(runner) { mutableStateOf(MemorySearchFormat.HEX) }
    var result by remember(runner) { mutableStateOf<DebuggerMemorySearch.Result?>(null) }
    var error by remember(runner) { mutableStateOf<String?>(null) }
    val paused = runner?.state == RunnerState.PAUSED
    val current = result?.let { paused && it.revision == revision && it.bank == runner.selectedRomBank() } == true
    fun clear() { result = null; error = null }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text("debug.search.title"), style = MaterialTheme.typography.titleSmall)
        Text(strings.text("debug.search.help"), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it; clear() }, label = { Text(strings.text("debug.search.start")) },
                singleLine = true, modifier = Modifier.width(150.dp).emulatorTextInput(keyboardInput))
            OutlinedTextField(end, { end = it; clear() }, label = { Text(strings.text("debug.search.end")) },
                singleLine = true, modifier = Modifier.width(150.dp).emulatorTextInput(keyboardInput))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MemorySearchFormat.entries.forEach { mode ->
                FilterChip(format == mode, { format = mode; clear() }, label = { Text(strings.text("debug.search.format.$mode")) })
            }
        }
        OutlinedTextField(query, { query = it; clear() }, label = { Text(strings.text("debug.search.value")) },
            singleLine = true, modifier = Modifier.fillMaxWidth().emulatorTextInput(keyboardInput))
        Button(enabled = paused, onClick = {
            clear()
            val first = DebuggerMemorySearch.address(start)
            val last = DebuggerMemorySearch.address(end)
            val bytes = DebuggerMemorySearch.pattern(query, format)
            when {
                first == null || last == null || first > last -> error = "debug.search.rangeError"
                bytes == null -> error = "debug.search.valueError"
                runner == null || runner.state != RunnerState.PAUSED -> error = "debug.search.pause"
                else -> runCatching { DebuggerMemorySearch.search(runner, first, last, bytes) }
                    .onSuccess { result = it }.onFailure { error = "debug.search.failed" }
            }
        }) { Text(strings.text("debug.search.run")) }
        if (!paused) Text(strings.text("debug.search.pause"))
        error?.let { Text(strings.text(it), color = MaterialTheme.colorScheme.error) }
        result?.let { found ->
            if (!current) Text(strings.text("debug.search.stale"))
            else {
                Text(strings.text("debug.search.result", found.addresses.size.toString(), found.skipped.toString(), found.bank?.toString() ?: "—"))
                if (found.truncated) Text(strings.text("debug.search.limit"))
                Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                    found.addresses.forEach { address ->
                        TextButton(onClick = {
                            if (runner?.state == RunnerState.PAUSED && runner.sessionRevision == found.revision && runner.selectedRomBank() == found.bank) onSelect(address)
                        }) { Text("0x" + address.toString(16).uppercase().padStart(4, '0')) }
                    }
                }
            }
        }
    }
}
