package com.digihori.pgp.desktop.rom

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.localization.StudioStrings

/** Presentation only; the caller owns library access and actions. */
@Composable
internal fun RomLibraryRow(
    machineId: MachineId,
    installed: Boolean,
    sourceName: String?,
    onRegister: () -> Unit,
    onRemove: () -> Unit,
    strings: StudioStrings = StudioStrings.system,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(DesktopRomRegistrationCatalog.label(machineId, strings))
            Text(if (installed) sourceName?.let { strings.text("rom.verifiedSource", it) }
                ?: strings.text("rom.verified") else strings.text("rom.missing"))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onRegister, modifier = Modifier.semantics {
                contentDescription = strings.text("rom.actionDescription", DesktopRomRegistrationCatalog.label(machineId, strings),
                    strings.text(if (installed) "rom.replaceAction" else "rom.registerAction"))
            }) {
                Text(strings.text(if (installed) "rom.replaceAction" else "rom.registerAction"))
            }
            if (installed) TextButton(onClick = onRemove, modifier = Modifier.semantics {
                contentDescription = strings.text("rom.actionDescription", DesktopRomRegistrationCatalog.label(machineId, strings), strings.text("rom.removeAction"))
            }) { Text(strings.text("rom.removeAction")) }
        }
    }
}
