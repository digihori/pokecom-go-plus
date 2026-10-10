package com.digihori.pgp.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

private val placementStore = DesktopWindowPlacementStore()

@Composable
internal fun StudioWindow(
    id: String,
    onCloseRequest: () -> Unit,
    title: String,
    onPreviewKeyEvent: (KeyEvent) -> Boolean = { false },
    content: @Composable FrameWindowScope.() -> Unit,
) {
    val restored = androidx.compose.runtime.remember(id) {
        placementStore.restore(id)?.let { saved -> saved to placementStore.visibleBounds(saved) }
    }
    val state = rememberWindowState(
        placement = if (restored?.first?.maximized == true) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = restored?.second?.let { WindowPosition.Absolute(it.x.dp, it.y.dp) }
            ?: WindowPosition.PlatformDefault,
        size = restored?.second?.let { DpSize(it.width.dp, it.height.dp) } ?: DpSize(800.dp, 600.dp),
    )
    Window(
        onCloseRequest = onCloseRequest,
        onPreviewKeyEvent = onPreviewKeyEvent,
        title = title,
        state = state,
    ) {
        DisposableEffect(window, id) {
            val session = DesktopWindowPlacementSession(id, window, placementStore)
            val listener = object : ComponentAdapter() {
                override fun componentMoved(event: ComponentEvent?) = session.save()
                override fun componentResized(event: ComponentEvent?) = session.save()
            }
            window.addComponentListener(listener)
            onDispose {
                window.removeComponentListener(listener)
                session.save()
            }
        }
        content()
    }
}
