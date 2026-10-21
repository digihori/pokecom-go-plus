package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

@Composable
internal fun DebuggerSettingsPanel(available: Boolean, paused: Boolean, scope: String, notice: String?,
    onSave: () -> Unit, onRestore: () -> Unit, strings: StudioStrings = StudioStrings.system) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text("debug.settings.title"), style = MaterialTheme.typography.titleSmall)
        Text(scope, style = MaterialTheme.typography.bodySmall)
        Text(strings.text("debug.settings.help"), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onSave, enabled = available && paused) { Text(strings.text("debug.settings.save")) }
            Button(onRestore, enabled = available && paused) { Text(strings.text("debug.settings.restore")) }
        }
        if (!available) Text(strings.text("debug.settings.unavailable"))
        else if (!paused) Text(strings.text("debug.settings.pause"))
        notice?.let { Text(it) }
    }
}
