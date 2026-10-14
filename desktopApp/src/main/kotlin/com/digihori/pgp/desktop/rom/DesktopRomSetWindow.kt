package com.digihori.pgp.desktop.rom

import com.digihori.pgp.desktop.localization.uiText
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.localization.romSetCreationError
import com.digihori.pgp.desktop.localization.romSetInputIssue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
    StudioWindow(id = "create-rom-set", title = uiText("romSet.title"), onCloseRequest = { if (!busy) onClose() }) {
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
                result.onSuccess { created = it }.onFailure { error = StudioStrings.system.romSetCreationError(it) }
                busy = false
            }
        }
        PgpStudioTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(uiText("romSet.title"), style = MaterialTheme.typography.headlineSmall)
                    Text(uiText("romSet.help"))
                    Box {
                        OutlinedButton(enabled = !busy, onClick = { machineMenu = true }) { Text(uiText("romSet.machine" , definition.displayName)) }
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
                    RomFileField(uiText("romSet.internal"), internal, !busy,
                        internal?.let { DesktopRomSetCreator.inputIssue(it, 8192)?.let(StudioStrings.system::romSetInputIssue) }) {
                        chooseRomSetFile(window, uiText("romSet.selectInternal"), "internal.bin")?.let { internal = it; changed() }
                    }
                    val banking = definition.memoryBanking as? com.digihori.pgp.core.api.MachineMemoryBanking.Banked
                    val banked = banking != null
                    RomFileField(if (banked) uiText("romSet.externalBanked" , (externalSize / 1024).toString(), banking.bankCount.toString()) else uiText("romSet.external" , (externalSize / 1024).toString(), externalSize.toString()),
                        external, !busy, external?.let { DesktopRomSetCreator.inputIssue(it, externalSize)?.let(StudioStrings.system::romSetInputIssue) }) {
                        chooseRomSetFile(window, uiText("romSet.selectExternal"), if (banked) "${machineId.value.replace("-", "").removeSuffix("u")}bank.bin" else "external.bin")?.let { external = it; changed() }
                    }
                    if (banked) Text(uiText("romSet.bankHelp" , (banking.bankCount - 1).toString()), style = MaterialTheme.typography.bodySmall)
                    RomFileField(uiText("romSet.destination"), destination, !busy) {
                        chooseRomSetFile(window, uiText("romSet.save"), destination?.name ?: "${machineId.value}.pgrom", FileDialog.SAVE,
                            destination?.parentFile)?.let { destination = DesktopRomSetCreator.destination(it); changed() }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    created?.let { Text(uiText("romSet.created" , it.absolutePath), color = MaterialTheme.colorScheme.primary) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(enabled = !busy && created == null && destination != null &&
                            DesktopRomSetCreator.inputError(internal, 8192) == null &&
                            DesktopRomSetCreator.inputError(external, externalSize) == null,
                            onClick = { if (destination?.exists() == true) overwriteConfirmation = true else create(false) }) {
                            Text(if (busy) uiText("romSet.creating") else uiText("romSet.create"))
                        }
                        created?.let { file ->
                            OutlinedButton(onClick = { if (onRegister(file, machineId)) onClose() else error = uiText("romSet.registrationFailed") }) {
                                Text(uiText("romSet.register"))
                            }
                        }
                        TextButton(enabled = !busy, onClick = onClose) { Text(uiText("romSet.close")) }
                    }
                }
                if (overwriteConfirmation) AlertDialog(
                    onDismissRequest = { overwriteConfirmation = false },
                    title = { Text(uiText("romSet.replaceConfirm")) },
                    text = { Text(destination?.absolutePath.orEmpty()) },
                    confirmButton = { TextButton(onClick = { overwriteConfirmation = false; create(true) }) { Text(uiText("romSet.replace")) } },
                    dismissButton = { TextButton(onClick = { overwriteConfirmation = false }) { Text(uiText("romSet.cancel")) } },
                )
            }
        }
    }
}

@Composable
internal fun RomFileField(label: String, file: File?, enabled: Boolean, error: String? = null, strings: StudioStrings = StudioStrings.system, onSelect: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = onSelect, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = strings.text("romSet.fileDescription", label, file?.absolutePath ?: strings.text("romSet.select"))
        }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(file?.absolutePath ?: strings.text("romSet.select"), modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text(strings.text("romSet.browse"))
            }
        }
        if (file != null) Text(error ?: strings.text("romSet.selected"), color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
