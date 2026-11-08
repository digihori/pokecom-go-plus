package com.digihori.pgp.desktop.console

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioStrings
import java.awt.Cursor

@Composable
internal fun StudioConsolePanel(
    console: StudioConsoleState,
    height: Dp,
    onHeightDelta: (Dp) -> Unit,
    onHeightFinished: () -> Unit,
    onHide: () -> Unit,
    onCopy: (String) -> Unit,
    onInputFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    strings: StudioStrings = StudioStrings.system,
) {
    val density = LocalDensity.current
    val currentDelta by rememberUpdatedState(onHeightDelta)
    val currentFinished by rememberUpdatedState(onHeightFinished)
    val currentFocusChanged by rememberUpdatedState(onInputFocusChanged)
    DisposableEffect(Unit) { onDispose { currentFocusChanged(false) } }
    val scroll = rememberScrollState()
    var followTail by remember { mutableStateOf(true) }
    val text = console.text
    LaunchedEffect(scroll) {
        snapshotFlow { Triple(scroll.value, scroll.maxValue, scroll.isScrollInProgress) }.collect { (value, maximum, moving) ->
            if (moving) followTail = value >= maximum - 2
        }
    }
    LaunchedEffect(text, scroll.maxValue) {
        if (followTail) scroll.scrollTo(scroll.maxValue)
    }
    Surface(modifier.fillMaxWidth().height(height), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column {
            Box(Modifier.fillMaxWidth().height(8.dp)
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
                .pointerInput(density) {
                    detectDragGestures(onDragEnd = { currentFinished() }, onDragCancel = { currentFinished() }) { change, amount ->
                        change.consume()
                        currentDelta(with(density) { -amount.y.toDp() })
                    }
                }, contentAlignment = Alignment.Center) {
                HorizontalDivider()
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(strings.text("console.title"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(enabled = console.entries.isNotEmpty(), onClick = { onCopy(text) }) { Text(strings.text("common.copyAll")) }
                TextButton(enabled = console.entries.isNotEmpty(), onClick = { console.clear(); followTail = true }) {
                    Text(strings.text("console.clear"))
                }
                TextButton(onClick = onHide) { Text(strings.text("console.hide")) }
            }
            Box(Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp, end = 4.dp, bottom = 8.dp)) {
                BasicTextField(value = text, onValueChange = {}, readOnly = true,
                    modifier = Modifier.fillMaxSize().onFocusChanged { currentFocusChanged(it.isFocused) }
                        .verticalScroll(scroll).padding(end = 14.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant),
                    decorationBox = { field ->
                        if (text.isEmpty()) Text(strings.text("console.empty"), style = MaterialTheme.typography.bodySmall)
                        field()
                    })
                VerticalScrollbar(rememberScrollbarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}
