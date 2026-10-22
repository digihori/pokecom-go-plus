package com.digihori.pgp.player.android

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler

/** Public language chooser, reachable before any ROM is installed. */
@Composable
internal fun PlayerSetupGuide() {
    val uriHandler = LocalUriHandler.current
    var error by remember { mutableStateOf(false) }
    Column {
        Text("ROMは同梱していません。準備・登録の手順はこちら。")
        OutlinedButton(onClick = {
            error = false
            runCatching {
                uriHandler.openUri("https://digihori.github.io/pokecom-go-plus/player/")
            }.onFailure { error = true }
        }) {
            Text("はじめに・ROMの準備方法 / Setup guide（外部）")
        }
        if (error) {
            Text("外部リンクを開けませんでした。ブラウザの設定を確認してください。",
                color = MaterialTheme.colorScheme.error)
        }
    }
}
