package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
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
internal fun DebuggerMemoryEditPanel(runner: DesktopEmulatorRunner?, keyboardInput: DesktopKeyboardInput,
    startAddress: Int, revision: Long, onEdited: () -> Unit, strings: StudioStrings = StudioStrings.system) {
    fun hex(value: Int, width: Int) = value.toString(16).uppercase().padStart(width, '0')
    var address by rememberSaveable(runner, startAddress) { mutableStateOf(hex(startAddress, 4)) }
    var value by rememberSaveable(runner) { mutableStateOf("") }
    var preview by remember(runner) { mutableStateOf<DebuggerMemoryEditPreview?>(null) }
    var applied by remember(runner) { mutableStateOf<DebuggerMemoryEditPreview?>(null) }
    var error by remember(runner) { mutableStateOf<DebuggerMemoryEditError?>(null) }
    val paused = runner?.state == RunnerState.PAUSED
    val current = paused && preview?.revision == revision
    fun clear() { preview = null; error = null }
    LaunchedEffect(startAddress) { clear() }
    fun summary(edit: DebuggerMemoryEditPreview) = strings.text("debug.edit.change", hex(edit.address, 4), hex(edit.physicalAddress, 4), hex(edit.before, 2), hex(edit.after, 2))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text("debug.edit.title"), style = MaterialTheme.typography.titleSmall)
        Text(strings.text("debug.edit.help"), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(address, { address = it; clear() }, label = { Text(strings.text("debug.edit.address")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboardInput))
            OutlinedTextField(value, { value = it; clear() }, label = { Text(strings.text("debug.edit.value")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboardInput))
        }
        Button(enabled = paused, onClick = {
            clear()
            val parsedAddress = DebuggerMemorySearch.address(address)
            val parsedValue = value.trim().takeIf { it.matches(Regex("[0-9a-fA-F]{2}")) }?.toIntOrNull(16)
            when {
                parsedAddress == null -> error = DebuggerMemoryEditError.ADDRESS
                parsedValue == null -> error = DebuggerMemoryEditError.VALUE
                runner != null -> when (val result = runner.previewMemoryEdit(parsedAddress, parsedValue)) {
                    is DebuggerMemoryEditResult.Success -> preview = result.edit
                    is DebuggerMemoryEditResult.Failure -> error = result.error
                }
            }
        }) { Text(strings.text("debug.edit.preview")) }
        preview?.let { edit ->
            Text(summary(edit))
            if (!current) Text(strings.text("debug.edit.error.STALE"))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = current, onClick = {
                    if (preview != edit || DebuggerMemorySearch.address(address) != edit.address ||
                        value.trim().toIntOrNull(16) != edit.after) {
                        clear(); error = DebuggerMemoryEditError.STALE
                        return@Button
                    }
                    when (val result = runner?.applyMemoryEdit(edit)) {
                        is DebuggerMemoryEditResult.Success -> { applied = result.edit; clear(); onEdited() }
                        is DebuggerMemoryEditResult.Failure -> { clear(); error = result.error }
                        null -> { clear(); error = DebuggerMemoryEditError.STALE }
                    }
                }) { Text(strings.text("debug.edit.apply")) }
                TextButton(onClick = { clear() }) { Text(strings.text("debug.edit.cancel")) }
            }
        }
        if (!paused) Text(strings.text("debug.edit.error.PAUSED_ONLY"))
        error?.let { Text(strings.text("debug.edit.error.$it"), color = MaterialTheme.colorScheme.error) }
        applied?.let { Text(strings.text("debug.edit.applied", summary(it))) }
    }
}
