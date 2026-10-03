package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner

/** Window-level physical keyboard dispatcher, independent of Compose focus ownership. */
internal class DesktopKeyboardInput {
    private var runner: DesktopEmulatorRunner? = null
    private val pressedKeys: MutableMap<Long, PocketKey> = mutableMapOf()

    fun attach(runner: DesktopEmulatorRunner?) {
        val previousRunner = this.runner
        if (previousRunner !== runner) {
            if (previousRunner != null) {
                pressedKeys.values.forEach { previousRunner.releaseKey(it) }
            }
            pressedKeys.clear()
            this.runner = runner
        }
    }

    fun handle(event: KeyEvent): Boolean {
        val activeRunner = runner ?: return false
        val physicalKey = event.key.keyCode
        return when (event.type) {
            KeyEventType.KeyDown -> {
                val key = DesktopKeyMapper.map(event.key, event.utf16CodePoint) ?: return false
                if (pressedKeys.putIfAbsent(physicalKey, key) == null) {
                    activeRunner.pressKey(key)
                }
                true
            }
            KeyEventType.KeyUp -> {
                val key = pressedKeys.remove(physicalKey) ?: return false
                activeRunner.releaseKey(key)
                true
            }
            else -> false
        }
    }
}
