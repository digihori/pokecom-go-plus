package com.digihori.pgp.player.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId

@Composable
internal fun RomManagementScreen(manager: AndroidRomManager, current: MachineId?) {
    val state = manager.state ?: return
    BackHandler(enabled = state !is RomManagerState.Busy) { manager.dismiss() }
    ManagementSurface {
        Text("機種・ROM", style = MaterialTheme.typography.headlineSmall)
        when (state) {
            is RomManagerState.ListRoms -> {
                Text("機種ごとにROMを1セット保存します。切替・置換時にRAMは引き継ぎません。")
                state.entries.forEach { entry ->
                    Text(MachineCatalog.require(entry.machineId).displayName + if (entry.machineId == current) "（使用中）" else "",
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (entry.installed) "登録済み · ${entry.sourceName.orEmpty().substringAfterLast('/')}" else "未登録")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { manager.importRom(entry.machineId) }) { Text(if (entry.installed) "置換" else "取込み") }
                        if (entry.installed) {
                            OutlinedButton(onClick = { manager.use(entry.machineId) }, enabled = entry.machineId != current) { Text("使用する") }
                            TextButton(onClick = { manager.delete(entry.machineId) }) { Text("削除") }
                        }
                    }
                }
                TextButton(onClick = manager::dismiss) { Text("戻る") }
            }
            is RomManagerState.Busy -> { Text(state.text); CircularProgressIndicator() }
            is RomManagerState.Confirm -> {
                Text(state.text)
                Row {
                    TextButton(onClick = manager::open) { Text("キャンセル") }
                    Button(onClick = state.action) { Text("実行") }
                }
            }
            is RomManagerState.Message -> {
                Text(state.text)
                TextButton(onClick = manager::open) { Text("ROM一覧へ") }
                TextButton(onClick = manager::dismiss) { Text("閉じる") }
            }
        }
    }
}

@Composable
internal fun PlayerInformationScreen(section: String, version: String, onClose: () -> Unit) {
    BackHandler { onClose() }
    ManagementSurface {
        Text(section, style = MaterialTheme.typography.headlineSmall)
        Text(when (section) {
            "バージョン" -> "Pokecom GO Player\n$version"
            "プライバシーポリシー" -> "ここにプライバシーポリシーを表示します。"
            else -> "ここにPokecom GO Studioの案内を表示します。"
        })
        TextButton(onClick = onClose) { Text("戻る") }
    }
}

@Composable
private fun ManagementSurface(content: @Composable ColumnScope.() -> Unit) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize().clickable {}) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}
