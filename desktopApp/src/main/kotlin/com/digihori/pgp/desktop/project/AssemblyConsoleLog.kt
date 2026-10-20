package com.digihori.pgp.desktop.project

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.uiText

@Composable
internal fun AssemblyConsoleLog(title: String, content: String) {
    val lines = remember(content) { content.lineSequence().toList() }
    val clipboard = LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = { clipboard.setText(AnnotatedString(content)) }) { Text(uiText("assembly.copyLog")) }
    }
    if (lines.size <= 8) {
        Text(content, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth())
    } else key(content) {
        val scroll = rememberLazyListState()
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                LazyColumn(state = scroll, modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                    contentPadding = PaddingValues(8.dp)) {
                    items(lines) { Text(it.ifEmpty { " " }, fontFamily = FontFamily.Monospace) }
                }
                VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}
