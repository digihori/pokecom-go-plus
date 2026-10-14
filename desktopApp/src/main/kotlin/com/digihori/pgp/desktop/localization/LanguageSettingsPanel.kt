package com.digihori.pgp.desktop.localization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Saving is owned by the caller; strings keep the current process language. */
@Composable
internal fun LanguageSettingsPanel(
    selected: StudioLanguagePreference,
    onSelect: (StudioLanguagePreference) -> Unit,
    pendingRestart: Boolean,
    error: String? = null,
    strings: StudioStrings = StudioStrings.system,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.text("settings.languageHelp"))
        StudioLanguagePreference.entries.forEach { preference ->
            val label = strings.text("settings.languageOption.${preference.value}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == preference, onClick = { onSelect(preference) },
                    modifier = Modifier.semantics { contentDescription = label })
                Text(label)
            }
        }
        if (pendingRestart) Text(strings.text("settings.languagePending"), color = MaterialTheme.colorScheme.primary)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
