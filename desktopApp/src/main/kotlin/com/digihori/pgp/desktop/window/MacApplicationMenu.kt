package com.digihori.pgp.desktop.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.Desktop
import java.awt.EventQueue

/** macOS already supplies the application menu; connect it instead of adding a second one. */
@Composable
internal fun useMacApplicationMenu(
    onAbout: () -> Unit,
    onSettings: () -> Unit,
    onQuit: () -> Unit,
): Boolean {
    val about = rememberUpdatedState(onAbout)
    val settings = rememberUpdatedState(onSettings)
    val quit = rememberUpdatedState(onQuit)
    val desktop = remember {
        if (System.getProperty("os.name").startsWith("Mac", ignoreCase = true) && Desktop.isDesktopSupported()) {
            Desktop.getDesktop().takeIf {
                it.isSupported(Desktop.Action.APP_ABOUT) &&
                    it.isSupported(Desktop.Action.APP_PREFERENCES) &&
                    it.isSupported(Desktop.Action.APP_QUIT_HANDLER)
            }
        } else null
    }
    DisposableEffect(desktop) {
        if (desktop != null) {
            desktop.setAboutHandler { EventQueue.invokeLater { about.value() } }
            desktop.setPreferencesHandler { EventQueue.invokeLater { settings.value() } }
            desktop.setQuitHandler { _, response ->
                // Compose owns shutdown and disposal, including emulator/audio/MCP cleanup.
                response.cancelQuit()
                EventQueue.invokeLater { quit.value() }
            }
        }
        onDispose {
            desktop?.setAboutHandler(null)
            desktop?.setPreferencesHandler(null)
            desktop?.setQuitHandler(null)
        }
    }
    return desktop != null
}
