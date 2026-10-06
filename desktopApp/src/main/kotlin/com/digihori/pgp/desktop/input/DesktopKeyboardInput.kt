package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245CharacterInput
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251CharacterInput
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350CharacterInput

internal interface DesktopKeyInputSink {
    val machineId: MachineId
    fun pressKey(key: PocketKey): InputResult
    fun releaseKey(key: PocketKey): InputResult
    fun enqueueKeySequence(keys: Iterable<PocketKey>)
    fun enqueueUserKeySequence(keys: Iterable<PocketKey>) = enqueueKeySequence(keys)
    fun recallPreviousCommand(): Boolean = false
    fun recallNextCommand(): Boolean = false
}

/** Window-level physical keyboard dispatcher, independent of Compose focus ownership. */
internal class DesktopKeyboardInput {
    private var sink: DesktopKeyInputSink? = null
    private var enabled: Boolean = true
    private val activeInputs: MutableMap<Long, ActiveInput> = mutableMapOf()

    fun attach(sink: DesktopKeyInputSink?) {
        val previousSink = this.sink
        if (previousSink !== sink) {
            clearActiveInputs(previousSink)
            this.sink = sink
        }
    }

    fun clearActiveInputs() {
        clearActiveInputs(sink)
    }

    fun setEnabled(value: Boolean) {
        if (enabled && !value) clearActiveInputs()
        enabled = value
    }

    fun handle(event: KeyEvent): Boolean = handle(
        key = event.key,
        utf16CodePoint = event.utf16CodePoint,
        type = event.type,
        isCtrlPressed = event.isCtrlPressed,
        isMetaPressed = event.isMetaPressed,
        isAltPressed = event.isAltPressed,
    )

    internal fun handle(
        key: androidx.compose.ui.input.key.Key,
        utf16CodePoint: Int,
        type: KeyEventType,
        isCtrlPressed: Boolean = false,
        isMetaPressed: Boolean = false,
        isAltPressed: Boolean = false,
    ): Boolean {
        if (!enabled) return false
        val activeSink = sink ?: return false
        if (isAltPressed && (key == androidx.compose.ui.input.key.Key.DirectionUp ||
                key == androidx.compose.ui.input.key.Key.DirectionDown)) {
            return when (type) {
                KeyEventType.KeyDown -> if (key == androidx.compose.ui.input.key.Key.DirectionUp) {
                    activeSink.recallPreviousCommand()
                } else {
                    activeSink.recallNextCommand()
                }
                KeyEventType.KeyUp -> true
                else -> false
            }
        }
        if (isCtrlPressed || isMetaPressed) return false
        val physicalKey = key.keyCode
        return when (type) {
            KeyEventType.KeyDown -> {
                if (physicalKey in activeInputs) return true
                val character = utf16CodePoint
                    .takeIf { it in Char.MIN_VALUE.code..Char.MAX_VALUE.code }
                    ?.toChar()
                val sequence = character?.let { activeSink.keySequence(it) }
                val directKey = DesktopKeyMapper.map(key)
                if (directKey != null && (sequence == null || sequence == listOf(directKey))) {
                    activeInputs[physicalKey] = ActiveInput.Direct(directKey)
                    activeSink.pressKey(directKey)
                    return true
                }
                if (sequence != null) {
                    activeInputs[physicalKey] = ActiveInput.QueuedCharacter
                    activeSink.enqueueUserKeySequence(sequence)
                    return true
                }
                false
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

    private fun DesktopKeyInputSink.keySequence(character: Char): List<PocketKey>? =
        when (MachineCatalog.find(machineId)?.keyboardLayout) {
            MachineKeyboardLayout.PC_1251 -> Pc1251CharacterInput.keySequence(character)
            MachineKeyboardLayout.PC_1245, null -> Pc1245CharacterInput.keySequence(character)
            MachineKeyboardLayout.PC_1350 -> Pc1350CharacterInput.keySequence(character)
        }

    private sealed interface ActiveInput {
        data class Direct(val key: PocketKey) : ActiveInput
        data object QueuedCharacter : ActiveInput
    }

    private fun clearActiveInputs(target: DesktopKeyInputSink?) {
        if (target != null) {
            activeInputs.values.forEach { input ->
                if (input is ActiveInput.Direct) target.releaseKey(input.key)
            }
        }
        activeInputs.clear()
    }
}
