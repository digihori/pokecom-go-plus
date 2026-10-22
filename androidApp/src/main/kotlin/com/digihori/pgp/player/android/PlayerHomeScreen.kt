package com.digihori.pgp.player.android

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.*
import kotlinx.coroutines.flow.first

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
    gridState: LazyGridState,
    homeModels: Map<MachineId, MachineId>,
    onModelChange: (MachineId) -> Unit,
) {
    val machines = PlayerMachineCatalog.homeDefinitions
    val selectedMachineId = (state as? PlayerScreenState.Emulator)?.machineId
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(selectedMachineId, layout, gridState, viewportSize) {
        val selectedRowId = selectedMachineId?.let(PlayerMachineCatalog::romId)
        val index = machines.indexOfFirst { it.id == selectedRowId }
        if (index < 0 || viewportSize == IntSize.Zero) return@LaunchedEffect
        // Wait for measurement before checking a restored scroll position.
        val info = snapshotFlow { gridState.layoutInfo }
            .first { it.viewportSize == viewportSize && it.visibleItemsInfo.isNotEmpty() }
        val selected = info.visibleItemsInfo.firstOrNull { it.key == selectedRowId?.value }
        val fullyVisible = selected != null && selected.offset.y >= info.viewportStartOffset &&
            selected.offset.y + selected.size.height <= info.viewportEndOffset
        if (!fullyVisible) gridState.scrollToItem(index)
    }
    Column(Modifier.fillMaxSize().padding(top = PlayerTitleBarHeight, start = 24.dp, end = 24.dp)) {
        PlayerSetupGuide()
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("機種を選択", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            PlayerHomeLayout.entries.forEach { value ->
                FilterChip(selected = layout == value, onClick = { onLayoutChange(value) },
                    label = { Text(value.label) })
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                state = gridState,
                columns = if (layout == PlayerHomeLayout.TILES) GridCells.Adaptive(200.dp) else GridCells.Fixed(1),
                modifier = Modifier.fillMaxSize().onSizeChanged { viewportSize = it },
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(machines, key = { it.id.value }) { machine ->
                    val model = PlayerMachineCatalog.selectedModel(machine.id, homeModels)
                    MachineCard(com.digihori.pgp.core.api.MachineCatalog.require(model),
                        entries?.find { it.machineId == machine.id }, entries != null,
                        selectedMachineId, layout, importEnabled, onPlay, onImportRom, onModelChange)
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
            HomeScrollbar(gridState, machines.size, Modifier.align(Alignment.CenterEnd).offset(x = 12.dp)
                .fillMaxHeight().width(4.dp).padding(vertical = 4.dp))
        }
    }
}

/** Persistent position indicator while the grid overflows; stays clear of card touch targets. */
@Composable
private fun HomeScrollbar(gridState: LazyGridState, machineCount: Int, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        if (!gridState.canScrollForward && !gridState.canScrollBackward) return@Canvas
        val info = gridState.layoutInfo
        val rows = info.visibleItemsInfo.filter { it.row >= 0 }.groupBy { it.row }
        val first = rows.minByOrNull { it.key } ?: return@Canvas
        val last = rows.maxByOrNull { it.key } ?: return@Canvas
        // Machine cards share rows; the explanatory footer/error each occupies a full row.
        val columns = info.maxSpan.coerceAtLeast(1)
        val totalRows = ((machineCount + columns - 1) / columns +
            (info.totalItemsCount - machineCount).coerceAtLeast(0)).toFloat()
        fun rowPosition(row: Map.Entry<Int, List<LazyGridItemInfo>>, edge: Int): Float {
            val top = row.value.minOf { it.offset.y }
            val height = row.value.maxOf { it.size.height } + info.mainAxisItemSpacing
            return row.key + ((edge - top).toFloat() / height.coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        val start = rowPosition(first, info.viewportStartOffset)
        val visible = (rowPosition(last, info.viewportEndOffset) - start).coerceAtLeast(0f)
        val fraction = (visible / totalRows.coerceAtLeast(1f)).coerceIn(0f, 0.99f)
        val thumbHeight = (size.height * fraction)
            .coerceIn(24.dp.toPx().coerceAtMost(size.height), size.height)
        val progress = when {
            !gridState.canScrollBackward -> 0f
            !gridState.canScrollForward -> 1f
            else -> (start / (totalRows - visible).coerceAtLeast(0.01f)).coerceIn(0f, 1f)
        }
        val top = (size.height - thumbHeight) * progress
        val corner = CornerRadius(size.width / 2f)
        drawRoundRect(color.copy(alpha = 0.12f), cornerRadius = corner)
        drawRoundRect(color.copy(alpha = 0.65f), topLeft = Offset(0f, top),
            size = Size(size.width, thumbHeight), cornerRadius = corner)
    }
}

@Composable
private fun MachineCard(
    machine: MachineDefinition,
    entry: PlayerRomEntry?,
    loaded: Boolean,
    activeMachine: MachineId?,
    layout: PlayerHomeLayout,
    importEnabled: Boolean,
    onPlay: (MachineId) -> Unit,
    onImportRom: (MachineId) -> Unit,
    onModelChange: (MachineId) -> Unit,
) {
    val selected = activeMachine?.let(PlayerMachineCatalog::romId) == PlayerMachineCatalog.romId(machine.id)
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = if (selected) 5.dp else 2.dp) {
        if (layout == PlayerHomeLayout.TILES) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MachineSummary(machine, entry, loaded, activeMachine, onModelChange)
                MachineAction(machine.id, entry, loaded, importEnabled, onPlay, onImportRom, Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) { MachineSummary(machine, entry, loaded, activeMachine, onModelChange) }
                MachineAction(machine.id, entry, loaded, importEnabled, onPlay, onImportRom)
            }
        }
    }
}

@Composable
private fun MachineSummary(machine: MachineDefinition, entry: PlayerRomEntry?, loaded: Boolean,
                           activeMachine: MachineId?, onModelChange: (MachineId) -> Unit) {
    val models = PlayerMachineCatalog.models(machine.id)
    val label = (if (activeMachine == machine.id) "*" else "") + machine.displayName
    if (models.size > 1) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(0.dp)) {
                Text("$label ▾", style = MaterialTheme.typography.titleMedium)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                models.forEach { model ->
                    DropdownMenuItem(text = { Text(model.displayName) }, onClick = {
                        expanded = false
                        onModelChange(model.id)
                    })
                }
            }
        }
    } else Text(label, style = MaterialTheme.typography.titleMedium)
    if (activeMachine != null && activeMachine != machine.id &&
        PlayerMachineCatalog.romId(activeMachine) == PlayerMachineCatalog.romId(machine.id)) {
        Text("起動中：${com.digihori.pgp.core.api.MachineCatalog.require(activeMachine).displayName}",
            style = MaterialTheme.typography.bodySmall)
    }
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
        Button(onClick = { onPlay(id) }, modifier = modifier.semantics { contentDescription = "${id.value.uppercase()}でプレイ" }) { Text("プレイ") }
    } else {
        Button(onClick = { onImportRom(id) }, enabled = loaded && importEnabled, modifier = modifier) { Text("ROMを登録") }
    }
}
