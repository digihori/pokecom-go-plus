package com.digihori.pgp.player.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.player.PlayerExecutionSpeed

@Composable
internal fun PlayerPlaybackSettings(
    section: String,
    soundEnabled: Boolean,
    speed: PlayerExecutionSpeed,
    audioStatus: String?,
    onSound: (Boolean) -> Unit,
    onSpeed: (PlayerExecutionSpeed) -> Unit,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) { Text("戻る") }
                if (section == "sound") {
                    Text("サウンド", style = MaterialTheme.typography.headlineSmall)
                    Row {
                        Text("ポケコンの音を再生", Modifier.weight(1f))
                        Switch(checked = soundEnabled, onCheckedChange = onSound)
                    }
                    Text("全機種共通です。音量は端末のメディア音量で調整してください。ホーム・停止中・背景では再生しません。")
                    Text("他のアプリによる音声の使用やヘッドホンの切断で停止した場合は、CPUの再開またはOFF→ONで再試行できます。")
                    audioStatus?.let { Text(it) }
                } else {
                    Text("CPU速度", style = MaterialTheme.typography.headlineSmall)
                    Text("全機種共通です。各機種の標準速度に対する倍率です。音の高さとテンポも変わります。")
                    PlayerExecutionSpeed.entries.forEach { value ->
                        TextButton(onClick = { onSpeed(value) }) {
                            Text("${if (value == speed) "●" else "○"} ${if (value == PlayerExecutionSpeed.NORMAL) "標準（1.0倍）" else "${value.id}倍"}")
                        }
                    }
                }
            }
        }
    }
}
