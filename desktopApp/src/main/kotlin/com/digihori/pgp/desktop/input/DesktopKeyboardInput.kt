package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245CharacterInput

internal interface DesktopKeyInputSink {
    fun pressKey(key: PocketKey): InputResult
    fun releaseKey(key: PocketKey): InputResult
    fun enqueueKeySequence(keys: Iterable<PocketKey>)
}

/** Window-level physical keyboard dispatcher, independent of Compose focus ownership. */
internal class DesktopKeyboardInput {
    private var sink: DesktopKeyInputSink? = null
    private val activeInputs: MutableMap<Long, ActiveInput> = mutableMapOf()

    fun attach(sink: DesktopKeyInputSink?) {
        val previousSink = this.sink
        if (previousSink !== sink) {
            if (previousSink != null) {
                activeInputs.values.forEach { input ->
                    if (input is ActiveInput.Direct) previousSink.releaseKey(input.key)
                }
            }
            activeInputs.clear()
            this.sink = sink
        }
    }

    fun handle(event: KeyEvent): Boolean = handle(
        key = event.key,
        utf16CodePoint = event.utf16CodePoint,
        type = event.type,
        isCtrlPressed = event.isCtrlPressed,
        isMetaPressed = event.isMetaPressed,
    )

    internal fun handle(
        key: androidx.compose.ui.input.key.Key,
        utf16CodePoint: Int,
        type: KeyEventType,
        isCtrlPressed: Boolean = false,
        isMetaPressed: Boolean = false,
    ): Boolean {
        val activeSink = sink ?: return false
        if (isCtrlPressed || isMetaPressed) return false
        val physicalKey = key.keyCode
        return when (type) {
            KeyEventType.KeyDown -> {
                if (physicalKey in activeInputs) return true
                val character = utf16CodePoint
                    .takeIf { it in Char.MIN_VALUE.code..Char.MAX_VALUE.code }
                    ?.toChar()
                val sequence = character?.let(Pc1245CharacterInput::keySequence)
                if (sequence != null) {
                    activeInputs[physicalKey] = ActiveInput.QueuedCharacter
                    activeSink.enqueueKeySequence(sequence)
                    return true
                }
                val pocketKey = DesktopKeyMapper.map(key) ?: return false
                activeInputs[physicalKey] = ActiveInput.Direct(pocketKey)
                activeSink.pressKey(pocketKey)
                true
            }
            KeyEventType.KeyUp -> {
                when (val input = activeInputs.remove(physicalKey) ?: return false) {
                    is ActiveInput.Direct -> activeSink.releaseKey(input.key)
                    ActiveInput.QueuedCharacter -> Unit
                }
                true
            }
            else -> false
        }
    }

    private sealed interface ActiveInput {
        data class Direct(val key: PocketKey) : ActiveInput
        data object QueuedCharacter : ActiveInput
    }
}
