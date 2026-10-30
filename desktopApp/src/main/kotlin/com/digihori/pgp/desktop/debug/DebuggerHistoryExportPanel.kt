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

@Composable
internal fun DebuggerHistoryExportPanel(available: Boolean, stopped: Boolean, keyboard: DesktopKeyboardInput,
    notice: String?, onExport: (DebuggerExportLimits) -> Unit, strings: StudioStrings = StudioStrings.system) {
    var trace by rememberSaveable { mutableStateOf("256") }
    var accesses by rememberSaveable { mutableStateOf("256") }
    var banks by rememberSaveable { mutableStateOf("256") }
    var logs by rememberSaveable { mutableStateOf("256") }
    var invalid by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text("debug.export.title"), style = MaterialTheme.typography.titleSmall)
        Text(strings.text("debug.export.help"), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(trace, { trace = it; invalid = false }, label = { Text(strings.text("debug.export.trace")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboard))
            OutlinedTextField(accesses, { accesses = it; invalid = false }, label = { Text(strings.text("debug.export.accesses")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboard))
            OutlinedTextField(banks, { banks = it; invalid = false }, label = { Text(strings.text("debug.export.banks")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboard))
            OutlinedTextField(logs, { logs = it; invalid = false }, label = { Text(strings.text("debug.export.logs")) },
                singleLine = true, modifier = Modifier.width(160.dp).emulatorTextInput(keyboard))
        }
        Button(enabled = available && stopped, onClick = {
            fun count(text: String) = text.takeIf { it.matches(Regex("[0-9]{1,4}")) }?.toIntOrNull() ?: -1
            val limits = DebuggerExportLimits(count(trace), count(accesses), count(banks), count(logs))
            if (!limits.valid()) invalid = true else onExport(limits)
        }) { Text(strings.text("debug.export.save")) }
        if (invalid) Text(strings.text("debug.export.error.LIMIT"), color = MaterialTheme.colorScheme.error)
        if (available && !stopped) Text(strings.text("debug.export.error.STATE"))
        notice?.let { Text(it) }
    }
}
