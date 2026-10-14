package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings

/** Labels stay readable in a narrow dialog; address/count parsing stays with the caller. */
@Composable
internal fun DebugContextFieldPair(
    first: String, second: String,
    onFirstChange: (String) -> Unit, onSecondChange: (String) -> Unit,
    firstLabelKey: String, secondLabelKey: String,
    strings: StudioStrings = StudioStrings.system,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 480.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(first, onFirstChange, label = { Text(strings.text(firstLabelKey)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(second, onSecondChange, label = { Text(strings.text(secondLabelKey)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(first, onFirstChange, label = { Text(strings.text(firstLabelKey)) },
                    singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(second, onSecondChange, label = { Text(strings.text(secondLabelKey)) },
                    singleLine = true, modifier = Modifier.weight(1f))
            }
        }
    }
}
