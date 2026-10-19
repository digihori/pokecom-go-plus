package com.digihori.pgp.player.android

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import com.digihori.pgp.core.api.PocketKey

/** Android key codes stay in the adapter; timing/ownership are shared with iOS. */
internal class PhysicalKeyboardState(press: (String, PocketKey) -> Unit, release: (String) -> Unit) {
    private var pc1350 = false
    private var pc1360 = false
    private var pc1360k = false
    private val input = com.digihori.pgp.input.PhysicalKeyInput(press, release) { physicalPocketKey(it, pc1350, pc1360, pc1360k) }
    fun configure(receiving: Boolean, allowed: Set<PocketKey>, minimumHoldCycles: Long = 0,
                  minimumGapCycles: Long = 0, pc1350: Boolean = false, pc1360: Boolean = false, pc1360k: Boolean = false) {
        if (this.pc1350 != pc1350 || this.pc1360 != pc1360 || this.pc1360k != pc1360k) input.reset()
        this.pc1350 = pc1350
        this.pc1360 = pc1360
        this.pc1360k = pc1360k
        input.configure(receiving, allowed, minimumHoldCycles, minimumGapCycles)
    }
    fun handle(device: Int, code: Int, down: Boolean, repeat: Int = 0) = input.handle(device, code, down, repeat)
    fun reset(device: Int? = null) = input.reset(device)
    fun advanceCycles(cycles: Long) = input.advanceCycles(cycles)
    fun diagnosticState() = input.diagnosticState()
}

internal class AndroidPhysicalKeyboard(context: Context, press: (String, PocketKey) -> Unit, release: (String) -> Unit) : InputManager.InputDeviceListener {
    val input = PhysicalKeyboardState(press, release)
    private var receivedEvents = 0L
    private var rejectedSource = 0L
    fun diagnosticState(): String = "events=$receivedEvents sourceRejected=$rejectedSource ${input.diagnosticState()}"
    private val manager = context.getSystemService(InputManager::class.java)
    fun start() = manager.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
    fun close() { manager.unregisterInputDeviceListener(this); input.reset() }
    fun onKey(event: KeyEvent): Boolean {
        receivedEvents++
        val sources = event.device?.sources ?: event.source
        if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK ||
            !event.isFromSource(InputDevice.SOURCE_KEYBOARD)) { rejectedSource++; return false }
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false
        return input.handle(event.deviceId, event.keyCode, event.action == KeyEvent.ACTION_DOWN, event.repeatCount)
    }
    override fun onInputDeviceAdded(deviceId: Int) = Unit
    override fun onInputDeviceChanged(deviceId: Int) { input.reset(deviceId) }
    override fun onInputDeviceRemoved(deviceId: Int) { input.reset(deviceId) }
}

internal fun physicalPocketKey(code: Int, pc1350: Boolean = false, pc1360: Boolean = false, pc1360k: Boolean = false): PocketKey? = when {
    (pc1350 || pc1360 || pc1360k) && code == KeyEvent.KEYCODE_F2 -> PocketKey.MODE
    (pc1350 || pc1360 || pc1360k) && code == KeyEvent.KEYCODE_F3 -> PocketKey.KANA
    (pc1350 || pc1360 || pc1360k) && code == KeyEvent.KEYCODE_INSERT -> PocketKey.INSERT
    (pc1350 || pc1360 || pc1360k) && code == KeyEvent.KEYCODE_FORWARD_DEL -> PocketKey.DELETE
    (pc1360 || pc1360k) && code == KeyEvent.KEYCODE_F4 -> PocketKey.SMALL
    pc1360k && code == KeyEvent.KEYCODE_F5 -> PocketKey.KANJI
    pc1360k && code == KeyEvent.KEYCODE_F6 -> PocketKey.HIRA_KATA
    pc1360k && code == KeyEvent.KEYCODE_F7 -> PocketKey.KANJI_FIX
    pc1360k && code == KeyEvent.KEYCODE_F8 -> PocketKey.KANJI_CONVERT
    pc1360k && code == KeyEvent.KEYCODE_F9 -> PocketKey.YJ
    else -> defaultPhysicalPocketKey(code)
}

private fun defaultPhysicalPocketKey(code: Int): PocketKey? = when (code) {
    in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z -> PocketKey.valueOf(('A' + code - KeyEvent.KEYCODE_A).toString())
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> PocketKey.valueOf("NUM_${code - KeyEvent.KEYCODE_0}")
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> PocketKey.valueOf("NUM_${code - KeyEvent.KEYCODE_NUMPAD_0}")
    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> PocketKey.SHIFT
    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_F1 -> PocketKey.DEF
    KeyEvent.KEYCODE_F2 -> PocketKey.CAL
    KeyEvent.KEYCODE_F3 -> PocketKey.BASIC
    KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN -> PocketKey.LEFT_PAREN
    KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN -> PocketKey.RIGHT_PAREN
    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BREAK -> PocketKey.BREAK
    KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> PocketKey.ENTER
    KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_CLEAR -> PocketKey.CLEAR
    KeyEvent.KEYCODE_SPACE -> PocketKey.SPACE
    KeyEvent.KEYCODE_DPAD_LEFT -> PocketKey.LEFT
    KeyEvent.KEYCODE_DPAD_RIGHT -> PocketKey.RIGHT
    KeyEvent.KEYCODE_DPAD_UP -> PocketKey.UP
    KeyEvent.KEYCODE_DPAD_DOWN -> PocketKey.DOWN
    KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> PocketKey.PLUS
    KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> PocketKey.MINUS
    KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_NUMPAD_MULTIPLY -> PocketKey.MULTIPLY
    KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_NUMPAD_DIVIDE -> PocketKey.DIVIDE
    KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_NUMPAD_DOT -> PocketKey.DOT
    KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_NUMPAD_EQUALS -> PocketKey.EQUALS
    KeyEvent.KEYCODE_COMMA -> PocketKey.COMMA
    KeyEvent.KEYCODE_SEMICOLON -> PocketKey.SEMICOLON
    else -> null
}
