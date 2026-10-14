package com.digihori.pgp.desktop.machine

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

/** Display fields only; address parsing and memory access remain with the caller. */
@Composable
internal fun MemoryDumpRangeFields(
    start: String,
    end: String,
    onStart: (String) -> Unit,
    onEnd: (String) -> Unit,
    error: String?,
    strings: StudioStrings = StudioStrings.system,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.text("memory.rangeHelp"))
        OutlinedTextField(start, onStart, label = { Text(strings.text("memory.start")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(end, onEnd, label = { Text(strings.text("memory.end")) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
