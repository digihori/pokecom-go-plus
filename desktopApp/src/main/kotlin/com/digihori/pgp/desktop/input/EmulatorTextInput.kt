package com.digihori.pgp.desktop.input

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

/** Each editor owns its block until focus is lost or the editor is disposed. */
internal fun Modifier.emulatorTextInput(keyboard: DesktopKeyboardInput): Modifier = composed {
    val owner = remember { Any() }
    DisposableEffect(keyboard, owner) {
        onDispose { keyboard.setTextInputFocused(owner, false) }
    }
    onFocusChanged { keyboard.setTextInputFocused(owner, it.isFocused) }
}
