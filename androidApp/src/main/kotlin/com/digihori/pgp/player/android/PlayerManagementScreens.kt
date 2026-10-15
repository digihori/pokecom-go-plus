package com.digihori.pgp.player.android

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId

@Composable
internal fun RomManagementScreen(manager: AndroidRomManager, current: MachineId?) {
    val state = manager.state ?: return
    BackHandler {
        when (state) {
            is RomManagerState.Confirm -> state.onCancel()
            is RomManagerState.Message -> manager.open()
            is RomManagerState.ListRoms -> manager.dismiss()
            is RomManagerState.Busy -> Unit
        }
    }
    ManagementSurface {
        Text("ROM管理", style = MaterialTheme.typography.headlineSmall)
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
                    TextButton(onClick = state.onCancel) { Text("キャンセル") }
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var document by remember(section) { mutableStateOf<String?>(null) }
    var linkError by remember(section) { mutableStateOf<String?>(null) }
    fun back() { if (document != null) document = null else onClose() }
    fun openLink(url: String) {
        linkError = null
        runCatching { uriHandler.openUri(url) }.onFailure {
            linkError = "外部リンクを開けませんでした。ブラウザの設定を確認してください。"
        }
    }
    BackHandler { back() }
    val asset = document ?: when (section) {
        "Playerとは" -> "PLAYER_HELP.md"
        "Studioについて" -> "PLAYER_STUDIO_GUIDE.md"
        "プライバシーポリシー" -> "PLAYER_PRIVACY_POLICY.md"
        else -> null
    }
    val body by produceState<String?>(null, asset) {
        value = null
        if (asset != null) value = withContext(Dispatchers.IO) {
            runCatching { context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .getOrElse { "同梱文書を読み込めませんでした。" }
        }
    }
    val licenses by produceState<List<String>>(emptyList(), section) {
        if (section == "バージョン・ライセンス") value = withContext(Dispatchers.IO) {
            context.assets.list("licenses").orEmpty().sorted().toList()
        }
    }
    ManagementSurface {
        TextButton(onClick = { back() }) { Text("戻る") }
        Text(document?.substringAfterLast('/') ?: section, style = MaterialTheme.typography.headlineSmall)
        if (asset != null) {
            if (body == null) Text("読み込んでいます…")
            else if (document == null) InformationDocument(body.orEmpty())
            else Text(body.orEmpty())
        } else when (section) {
            "バージョン・ライセンス", "バージョン" -> {
                Text("Pokecom GO Player\n$version\nTechnical Preview（早期評価版）")
                Text("アプリ本体はMIT Licenseです。ROMは含まれていません。第三者ソフトウェアの通知とライセンスは同梱文書を参照してください。")
                OutlinedButton(onClick = { document = "LICENSE" }) { Text("MIT License") }
                OutlinedButton(onClick = { document = "THIRD_PARTY_NOTICES.md" }) { Text("第三者ソフトウェアの通知") }
                licenses.forEach { file ->
                    TextButton(onClick = { document = "licenses/$file" }) { Text(file) }
                }
            }
        }
        if (section == "Studioについて" && document == null) {
            OutlinedButton(onClick = { openLink("https://github.com/digihori/pokecom-go-plus/releases") }) { Text("Studioの配布ページ（外部）") }
            TextButton(onClick = { openLink("https://github.com/digihori/pokecom-go-plus") }) { Text("プロジェクト・使い方（外部）") }
        }
        linkError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        TextButton(onClick = { back() }) { Text("戻る") }
    }
}

/** The bundled guides use headings and paragraphs; legal source text stays verbatim. */
@Composable
private fun InformationDocument(text: String) {
    text.split(Regex("\\n\\s*\\n")).forEach { paragraph ->
        val block = paragraph.trim()
        if (block.isNotEmpty()) {
            val heading = block.lineSequence().count() == 1
            val style = when {
                heading && block.startsWith("## ") -> MaterialTheme.typography.titleLarge
                heading && block.startsWith("# ") -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.bodyLarge
            }
            Text(
                text = if (heading) block.removePrefix("## ").removePrefix("# ") else block,
                style = if (heading && block.startsWith("#")) style.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else style,
                color = if (heading && block.startsWith("#")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
