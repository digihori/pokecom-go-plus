package com.digihori.pgp.player.android

import androidx.core.content.edit
import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.*
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.input.*

/** Activity/main-thread adapter. HAT is the digital d-pad; stick and trigger axes are ignored. */
internal class AndroidGamepadController(
    context: Context,
    press: (GamepadButton, PocketKey) -> Unit,
    release: (GamepadButton, PocketKey) -> Unit,
) : InputManager.InputDeviceListener {
    private val preferences = context.getSharedPreferences("gamepad", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(InputManager::class.java)
    private val input = GamepadInput(press, release)
    private val selection = SingleGamepadSelection()
    var settings by mutableStateOf(GamepadSettings.fromPreferences { preferences.getString(it, null) })
        private set
    var devices by mutableStateOf(emptyList<GamepadDevice>())
        private set
    var selectedId by mutableStateOf<String?>(null)
        private set
    var pressed by mutableStateOf(emptySet<GamepadButton>())
        private set
    var available by mutableStateOf(GamepadButton.entries.toSet())
        private set
    var status by mutableStateOf<String?>(null)
        private set
    private val keyButtons = mutableSetOf<GamepadButton>()
    private var hatButtons = emptySet<GamepadButton>()
    private var machineId: MachineId? = null
    private var receiving = false
    private var testing = false

    fun start() {
        manager.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        refreshDevices()
    }

    fun close() {
        manager.unregisterInputDeviceListener(this)
        input.reset()
    }

    fun configure(machineId: MachineId?, receiving: Boolean, testing: Boolean) {
        this.machineId = machineId
        this.receiving = receiving
        this.testing = testing
        input.configure(settings, selectedId, machineId?.let(GamepadKeys::forMachine).orEmpty(), receiving && !testing)
        input.update(selectedId, pressed)
    }

    fun reset() { input.reset(); input.update(selectedId, pressed) }

    fun select(id: String) {
        if (devices.none { it.id == id } || id == selectedId) return
        input.reset()
        selectedId = selection.update(devices, id)
        keyButtons.clear()
        hatButtons = emptySet()
        publishButtons()
        updateAvailable()
        configure(machineId, receiving, testing)
    }

    fun save(value: GamepadSettings) {
        // One complete editor transaction; disk persistence is asynchronous.
        preferences.edit {
            value.preferenceValues().forEach { (key, stored) -> putString(key, stored) }
        }
        settings = value.copy(bindings = value.bindings.toMap())
        configure(machineId, receiving, testing)
        status = "設定を保存しました"
    }

    fun onKey(event: KeyEvent): Boolean {
        val button = keyCodes[event.keyCode] ?: return false
        val device = event.device ?: return false
        if (!isController(device)) return false
        if (event.deviceId.toString() != selectedId) return true
        when (event.action) {
            KeyEvent.ACTION_DOWN -> keyButtons.add(button)
            KeyEvent.ACTION_UP -> keyButtons.remove(button)
            else -> return false
        }
        publishButtons()
        return testing || settings.enabled
    }

    fun onMotion(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        if (!isController(device) || event.action != MotionEvent.ACTION_MOVE) return false
        if (device.getMotionRange(MotionEvent.AXIS_HAT_X) == null &&
            device.getMotionRange(MotionEvent.AXIS_HAT_Y) == null) return false
        if (event.deviceId.toString() != selectedId) return true
        // Keep all historical d-pad transitions; do not turn analog stick motion into keys.
        for (index in 0 until event.historySize) {
            updateHat(event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_X, index),
                event.getHistoricalAxisValue(MotionEvent.AXIS_HAT_Y, index))
        }
        updateHat(event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
        return testing || settings.enabled
    }

    private fun updateHat(x: Float, y: Float) {
        hatButtons = buildSet {
            if (x < -.5f) add(GamepadButton.LEFT)
            if (x > .5f) add(GamepadButton.RIGHT)
            if (y < -.5f) add(GamepadButton.UP)
            if (y > .5f) add(GamepadButton.DOWN)
        }
        publishButtons()
    }

    private fun publishButtons() {
        pressed = keyButtons + hatButtons
        input.update(selectedId, pressed)
    }

    private fun refreshDevices() {
        val wasSelected = selectedId
        devices = manager.inputDeviceIds.map { manager.getInputDevice(it) }.filterNotNull()
            .filter(::isController).map { GamepadDevice(it.id.toString(), it.name) }
        selectedId = selection.update(devices)
        if (wasSelected != selectedId) {
            input.reset()
            keyButtons.clear()
            hatButtons = emptySet()
            publishButtons()
        }
        updateAvailable()
        configure(machineId, receiving, testing)
    }

    private fun updateAvailable() {
        val device = selectedId?.toIntOrNull()?.let(manager::getInputDevice)
        available = if (device == null) GamepadButton.entries.toSet() else {
            val codes = keyCodes.keys.toIntArray()
            val supported = device.hasKeys(*codes)
            buildSet {
                codes.forEachIndexed { index, code -> if (supported[index]) add(keyCodes.getValue(code)) }
                if (device.getMotionRange(MotionEvent.AXIS_HAT_X) != null) {
                    add(GamepadButton.LEFT); add(GamepadButton.RIGHT)
                }
                if (device.getMotionRange(MotionEvent.AXIS_HAT_Y) != null) {
                    add(GamepadButton.UP); add(GamepadButton.DOWN)
                }
            }
        }
    }

    override fun onInputDeviceAdded(deviceId: Int) = refreshDevices()
    override fun onInputDeviceChanged(deviceId: Int) = refreshDevices()
    override fun onInputDeviceRemoved(deviceId: Int) = refreshDevices()

    private companion object {
        fun isController(device: InputDevice): Boolean =
            device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

        val keyCodes = mapOf(
            KeyEvent.KEYCODE_DPAD_UP to GamepadButton.UP, KeyEvent.KEYCODE_DPAD_DOWN to GamepadButton.DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT to GamepadButton.LEFT, KeyEvent.KEYCODE_DPAD_RIGHT to GamepadButton.RIGHT,
            KeyEvent.KEYCODE_BUTTON_A to GamepadButton.SOUTH, KeyEvent.KEYCODE_BUTTON_B to GamepadButton.EAST,
            KeyEvent.KEYCODE_BUTTON_X to GamepadButton.WEST, KeyEvent.KEYCODE_BUTTON_Y to GamepadButton.NORTH,
            KeyEvent.KEYCODE_BUTTON_L1 to GamepadButton.L1, KeyEvent.KEYCODE_BUTTON_R1 to GamepadButton.R1,
        )
    }
}
