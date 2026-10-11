package com.digihori.pgp.desktop.rom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.window.StudioWindow
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DesktopRomSetWindow(initialMachineId: MachineId, onClose: () -> Unit, onRegister: (File, MachineId) -> Boolean) {
    var busy by remember { mutableStateOf(false) }
    StudioWindow(id = "create-rom-set", title = "Create ROM Set", onCloseRequest = { if (!busy) onClose() }) {
        var machineId by remember { mutableStateOf(initialMachineId) }
        var internal by remember { mutableStateOf<File?>(null) }
        var external by remember { mutableStateOf<File?>(null) }
        var destination by remember { mutableStateOf<File?>(null) }
        var created by remember { mutableStateOf<File?>(null) }
        var error by remember { mutableStateOf<String?>(null) }
        var overwriteConfirmation by remember { mutableStateOf(false) }
        var machineMenu by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val definition = MachineCatalog.require(machineId)
        val externalSize = DesktopRomSetCreator.externalSize(machineId)
        fun changed() { created = null; error = null }
        fun create(overwrite: Boolean) {
            val selectedInternal = internal ?: return
            val selectedExternal = external ?: return
            val selectedDestination = destination ?: return
            val selectedMachine = machineId
            busy = true
            error = null
            created = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { DesktopRomSetCreator.create(selectedMachine, selectedInternal, selectedExternal, selectedDestination, overwrite) }
                }
                result.onSuccess { created = it }.onFailure { error = it.message ?: "Could not create the ROM set." }
                busy = false
            }
        }
        PgpStudioTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Create ROM Set", style = MaterialTheme.typography.headlineSmall)
                    Text("Select the ROM dumps and destination below, then press Create. Original ROM files are kept. The .pgrom package includes a manifest and SHA-256 checksums.")
                    Box {
                        OutlinedButton(enabled = !busy, onClick = { machineMenu = true }) { Text("Machine: ${definition.displayName} ▾") }
                        DropdownMenu(expanded = machineMenu, onDismissRequest = { machineMenu = false }) {
                            MachineCatalog.definitions.forEach { machine ->
                                DropdownMenuItem(text = { Text(machine.displayName) }, onClick = {
                                    machineMenu = false
                                    if (machineId != machine.id) {
                                        machineId = machine.id
                                        internal = null; external = null; destination = null; changed()
                                    }
                                })
                            }
                        }
                    }
                    RomFileField("Internal ROM — 8 KiB (8192 bytes)", internal, !busy,
                        internal?.let { DesktopRomSetCreator.inputError(it, 8192) }) {
                        chooseRomSetFile(window, "Select internal ROM", "internal.bin")?.let { internal = it; changed() }
                    }
                    val banked = definition.family == MachineFamily.PC_1360
                    RomFileField(if (banked) "External ROM — 128 KiB (8 banks × 16 KiB)" else "External ROM — ${externalSize / 1024} KiB ($externalSize bytes)",
                        external, !busy, external?.let { DesktopRomSetCreator.inputError(it, externalSize) }) {
                        chooseRomSetFile(window, "Select external ROM", if (banked) "pc1360bank.bin" else "external.bin")?.let { external = it; changed() }
                    }
                    if (banked) Text("Choose a combined bank file containing bank 0 through bank 7 in order.", style = MaterialTheme.typography.bodySmall)
                    RomFileField("Destination folder and file name (.pgrom)", destination, !busy) {
                        chooseRomSetFile(window, "Save ROM Set", destination?.name ?: "${machineId.value}.pgrom", FileDialog.SAVE,
                            destination?.parentFile)?.let { destination = DesktopRomSetCreator.destination(it); changed() }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    created?.let { Text("Created: ${it.absolutePath}", color = MaterialTheme.colorScheme.primary) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(enabled = !busy && created == null && destination != null &&
                            DesktopRomSetCreator.inputError(internal, 8192) == null &&
                            DesktopRomSetCreator.inputError(external, externalSize) == null,
                            onClick = { if (destination?.exists() == true) overwriteConfirmation = true else create(false) }) {
                            Text(if (busy) "Creating…" else "Create")
                        }
                        created?.let { file ->
                            OutlinedButton(onClick = { if (onRegister(file, machineId)) onClose() else error = "Registration failed. See the Studio error message." }) {
                                Text("Register in ROM Library")
                            }
                        }
                        TextButton(enabled = !busy, onClick = onClose) { Text("Close") }
                    }
                }
                if (overwriteConfirmation) AlertDialog(
                    onDismissRequest = { overwriteConfirmation = false },
                    title = { Text("Replace existing ROM set?") },
                    text = { Text(destination?.absolutePath.orEmpty()) },
                    confirmButton = { TextButton(onClick = { overwriteConfirmation = false; create(true) }) { Text("Replace") } },
                    dismissButton = { TextButton(onClick = { overwriteConfirmation = false }) { Text("Cancel") } },
                )
            }
        }
    }
}

@Composable
private fun RomFileField(label: String, file: File?, enabled: Boolean, error: String? = null, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = onSelect, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(file?.absolutePath ?: "Select…", modifier = Modifier.weight(1f))
                Text("Browse…")
            }
        }
        if (file != null) Text(error ?: "Selected", color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall)
    }
}

private fun chooseRomSetFile(owner: Frame, title: String, suggestedName: String, mode: Int = FileDialog.LOAD, folder: File? = null): File? {
    val dialog = FileDialog(owner, title, mode)
    return try {
        dialog.file = suggestedName
        folder?.let { dialog.directory = it.absolutePath }
        dialog.isVisible = true
        val name = dialog.file
        val directory = dialog.directory
        if (name == null || directory == null) null else File(directory, name)
    } finally { dialog.dispose(); owner.toFront(); owner.requestFocus() }
}
