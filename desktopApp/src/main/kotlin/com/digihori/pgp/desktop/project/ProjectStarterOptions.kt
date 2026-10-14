package com.digihori.pgp.desktop.project

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.digihori.pgp.desktop.localization.StudioStrings

/** Presentation only; starter identities and generated files remain language independent. */
@Composable
internal fun ProjectStarterOptions(
    selected: Set<DesktopProjectStarter>,
    onSelected: (Set<DesktopProjectStarter>) -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    DesktopProjectStarter.entries.forEach { starter ->
        val label = strings.text("project.starter.${starter.name}")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = starter in selected,
                modifier = Modifier.semantics { contentDescription = label },
                onCheckedChange = { checked -> onSelected(if (checked) selected + starter else selected - starter) })
            Text(label)
        }
    }
}
