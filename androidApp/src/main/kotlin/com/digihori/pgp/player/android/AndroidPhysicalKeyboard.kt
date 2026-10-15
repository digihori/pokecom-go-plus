package com.digihori.pgp.player.android

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import com.digihori.pgp.core.api.PocketKey

/** Keycode-based input: no character conversion, IME injection, or generated Shift sequences. */
internal class PhysicalKeyboardState(
    private val press: (String, PocketKey) -> Unit,
    private val release: (String) -> Unit,
) {
    private data class Hold(
        val token: Pair<Int, Int>, val key: PocketKey, var remaining: Long,
        var released: Boolean = false, val chord: Boolean = false,
    )
    private val held = mutableMapOf<Pair<Int, Int>, Hold>()
    private val waiting = mutableListOf<Hold>()
    private val blocked = mutableSetOf<Pair<Int, Int>>()
    private var minimumHoldCycles = 0L
    private var minimumGapCycles = 0L
    private var gapRemaining = 0L
    private var downEvents = 0L
    private var upEvents = 0L
    private var deliveredPresses = 0L
    internal fun diagnosticState(): String =
        "receiving=$receiving down=$downEvents up=$upEvents delivered=$deliveredPresses " +
            "held=${held.size} queued=${waiting.size} blocked=${blocked.size} gap=$gapRemaining"
    private var receiving = false
    private var allowed = emptySet<PocketKey>()

    fun configure(receiving: Boolean, allowed: Set<PocketKey>, minimumHoldCycles: Long = 0,
                  minimumGapCycles: Long = 0) {
        require(minimumHoldCycles >= 0 && minimumGapCycles >= 0)
        if (this.receiving != receiving || this.allowed != allowed ||
            this.minimumHoldCycles != minimumHoldCycles || this.minimumGapCycles != minimumGapCycles) reset()
        this.receiving = receiving
        this.allowed = allowed
        this.minimumHoldCycles = minimumHoldCycles
        this.minimumGapCycles = minimumGapCycles
    }

    fun handle(device: Int, code: Int, down: Boolean, repeat: Int = 0): Boolean {
        val token = device to code
        val key = physicalPocketKey(code) ?: return false
        if (down) downEvents++ else upEvents++
        if (!down) {
            blocked.remove(token)
            val queued = waiting.lastOrNull { it.token == token && !it.released }
            val active = queued ?: held[token]?.takeUnless { it.released }
            if (active != null) {
                active.released = true
                if (queued == null && active.remaining == 0L) finish(active)
            }
            pump()
            return active != null || (receiving && key in allowed)
        }
        if (!receiving || key !in allowed) {
            blocked.add(token)
            return false
        }
        // A fresh down proves a new physical press even if focus loss swallowed the previous up.
        // Repeats after a reset must still wait for a release/new press.
        if (repeat == 0) blocked.remove(token)
        if (token in blocked) return true
        if (held[token]?.released == false || waiting.any { it.token == token && !it.released }) return true
        if (repeat > 0) { blocked.add(token); return true }
        val chord = held.values.any { !it.released } || waiting.any { !it.released }
        val hold = Hold(token, key, minimumHoldCycles, chord = chord)
        if (waiting.isNotEmpty() || gapRemaining > 0 || (held.isNotEmpty() && held.values.all { it.released })) {
            waiting += hold
        } else start(hold)
        pump()
        return true
    }

    /** Retain short physical taps until the ROM can scan them; never synthesize text. */
    fun advanceCycles(cycles: Long) {
        require(cycles >= 0)
        if (held.isEmpty()) gapRemaining = (gapRemaining - cycles).coerceAtLeast(0)
        held.values.toList().forEach { hold ->
            hold.remaining = (hold.remaining - cycles).coerceAtLeast(0)
            if (hold.released && hold.remaining == 0L) finish(hold)
        }
        pump()
    }

    private fun start(hold: Hold) {
        deliveredPresses++
        held[hold.token] = hold
        press(owner(hold.token), hold.key)
    }

    private fun finish(hold: Hold) {
        held.remove(hold.token)
        release(owner(hold.token))
        if (held.isEmpty()) gapRemaining = minimumGapCycles
    }

    private fun pump() {
        if (held.isNotEmpty() || gapRemaining > 0 || waiting.isEmpty()) return
        start(waiting.removeAt(0))
        while (waiting.firstOrNull()?.chord == true) start(waiting.removeAt(0))
    }

    fun reset(device: Int? = null) {
        held.values.filter { device == null || it.token.first == device }.toList().forEach { hold ->
            held.remove(hold.token)
            blocked.add(hold.token)
            release(owner(hold.token))
        }
        waiting.filter { device == null || it.token.first == device }.forEach { blocked.add(it.token) }
        waiting.removeAll { device == null || it.token.first == device }
        if (device != null) blocked.removeAll { it.first == device }
        if (held.isEmpty()) gapRemaining = 0
        // Do not start queued keys while releasing for a context change.
    }

    private fun owner(token: Pair<Int, Int>) = "keyboard:${token.first}:${token.second}"
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

internal fun physicalPocketKey(code: Int): PocketKey? = when (code) {
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
