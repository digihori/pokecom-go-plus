package com.digihori.pgp.player.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.*

internal enum class PlayerHomeLayout(val id: String, val label: String) {
    TILES("tiles", "タイル"), LIST("list", "リスト");

    companion object {
        fun fromId(id: String?): PlayerHomeLayout = entries.firstOrNull { it.id == id } ?: TILES
    }
}

@Composable
internal fun PlayerHomeScreen(
    state: PlayerScreenState,
    entries: List<PlayerRomEntry>?,
    onPlay: (MachineId) -> Unit,
    onImportRom: (MachineId) -> Unit,
    importEnabled: Boolean,
    layout: PlayerHomeLayout,
    onLayoutChange: (PlayerHomeLayout) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(top = PlayerTitleBarHeight, start = 24.dp, end = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("機種を選択", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            PlayerHomeLayout.entries.forEach { value ->
                FilterChip(selected = layout == value, onClick = { onLayoutChange(value) },
                    label = { Text(value.label) })
            }
        }
        LazyVerticalGrid(
            columns = if (layout == PlayerHomeLayout.TILES) GridCells.Adaptive(200.dp) else GridCells.Fixed(1),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(PlayerMachineCatalog.definitions, key = { it.id.value }) { machine ->
                MachineCard(machine, entries?.find { it.machineId == machine.id }, entries != null,
                    state is PlayerScreenState.Emulator && state.machineId == machine.id,
                    layout, importEnabled, onPlay, onImportRom)
            }
            if (state is PlayerScreenState.Failure) item(span = { GridItemSpan(maxLineSpan) }) {
                Text("選択中のROMを復元できませんでした。ROM管理で確認するか、別の機種を選択してください。")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Playerとホームを同じ機種で往復する場合はRAMを維持します。実行中なら自動再開し、明示停止は維持します。")
                    Text("別の機種へ切り替える場合は、確認後に起動し直します。RAMとロード済みプログラムは引き継ぎません。")
                }
            }
        }
    }
}

@Composable
private fun MachineCard(
    machine: MachineDefinition,
    entry: PlayerRomEntry?,
    loaded: Boolean,
    selected: Boolean,
    layout: PlayerHomeLayout,
    importEnabled: Boolean,
    onPlay: (MachineId) -> Unit,
    onImportRom: (MachineId) -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = if (selected) 5.dp else 2.dp) {
        if (layout == PlayerHomeLayout.TILES) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MachineSummary(machine, entry, loaded, selected)
                MachineAction(machine.id, entry, loaded, importEnabled, onPlay, onImportRom, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { MachineSummary(machine, entry, loaded, selected) }
                MachineAction(machine.id, entry, loaded, importEnabled, onPlay, onImportRom)
            }
        }
    }
}

@Composable
private fun MachineSummary(machine: MachineDefinition, entry: PlayerRomEntry?, loaded: Boolean, selected: Boolean) {
    Text((if (selected) "*" else "") + machine.displayName, style = MaterialTheme.typography.titleMedium)
    Text(when {
        !loaded -> "ROMを確認しています…"
        entry?.installed == true -> "登録済み"
        else -> "ROM未登録"
    }, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun MachineAction(id: MachineId, entry: PlayerRomEntry?, loaded: Boolean, importEnabled: Boolean,
                          onPlay: (MachineId) -> Unit, onImportRom: (MachineId) -> Unit, modifier: Modifier = Modifier) {
    if (entry?.installed == true) {
        Button(onClick = { onPlay(id) }, modifier = modifier) { Text("プレイ") }
    } else {
        Button(onClick = { onImportRom(id) }, enabled = loaded && importEnabled, modifier = modifier) { Text("ROMを登録") }
    }
}
