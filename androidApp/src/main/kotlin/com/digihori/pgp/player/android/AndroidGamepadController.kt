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

/** Activity/main-thread adapter. Combines digital d-pad and stick directions. */
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
    private var stickButtons = emptySet<GamepadButton>()
    private val sticks = GamepadAxes()
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
        stickButtons = emptySet()
        sticks.reset()
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
        val joystick = event.isFromSource(InputDevice.SOURCE_JOYSTICK)
        if (!joystick && device.getMotionRange(MotionEvent.AXIS_HAT_X) == null &&
            device.getMotionRange(MotionEvent.AXIS_HAT_Y) == null) return false
        if (event.deviceId.toString() != selectedId) return true
        val (rightX, rightY) = rightAxes(device)
        val hasHat = device.getMotionRange(MotionEvent.AXIS_HAT_X, event.source) != null ||
            device.getMotionRange(MotionEvent.AXIS_HAT_Y, event.source) != null
        val ranges = if (joystick) listOf(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, rightX, rightY)
            .map { stickRange(device, it) } else emptyList()
        val leftTrigger = triggerRange(device, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE)
        val rightTrigger = triggerRange(device, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS)
        fun sample(read: (Int) -> Float) {
            if (hasHat) {
                val x = read(MotionEvent.AXIS_HAT_X)
                val y = read(MotionEvent.AXIS_HAT_Y)
                hatButtons = buildSet {
                    if (x < -.5f) add(GamepadButton.LEFT)
                    if (x > .5f) add(GamepadButton.RIGHT)
                    if (y < -.5f) add(GamepadButton.UP)
                    if (y > .5f) add(GamepadButton.DOWN)
                }
            }
            if (joystick) {
                fun normalized(index: Int): Float {
                    val range = ranges[index] ?: return 0f
                    val value = read(range.axis)
                    return value / if (value < 0f) -range.min else range.max
                }
                fun trigger(range: InputDevice.MotionRange?): Float =
                    if (range == null) 0f else (read(range.axis) - range.min) / (range.max - range.min)
                stickButtons = sticks.update(normalized(0), normalized(1), normalized(2), normalized(3),
                    trigger(leftTrigger), trigger(rightTrigger))
            }
            publishButtons()
        }
        for (index in 0 until event.historySize) {
            sample { event.getHistoricalAxisValue(it, index) }
        }
        sample { event.getAxisValue(it) }
        return testing || settings.enabled
    }

    private fun triggerRange(device: InputDevice, primary: Int, alternate: Int): InputDevice.MotionRange? =
        device.getMotionRange(primary, InputDevice.SOURCE_JOYSTICK)
            ?: device.getMotionRange(alternate, InputDevice.SOURCE_JOYSTICK)

    private fun stickRange(device: InputDevice, axis: Int): InputDevice.MotionRange? =
        device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK)?.takeIf { it.min < 0f && it.max > 0f }

    private fun rightAxes(device: InputDevice): Pair<Int, Int> =
        if (stickRange(device, MotionEvent.AXIS_Z) != null &&
            stickRange(device, MotionEvent.AXIS_RZ) != null)
            MotionEvent.AXIS_Z to MotionEvent.AXIS_RZ
        else MotionEvent.AXIS_RX to MotionEvent.AXIS_RY

    private fun publishButtons() {
        pressed = keyButtons + hatButtons + stickButtons
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
            stickButtons = emptySet()
            sticks.reset()
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
                if (device.getMotionRange(MotionEvent.AXIS_HAT_X) != null || stickRange(device, MotionEvent.AXIS_X) != null) {
                    add(GamepadButton.LEFT); add(GamepadButton.RIGHT)
                }
                if (device.getMotionRange(MotionEvent.AXIS_HAT_Y) != null || stickRange(device, MotionEvent.AXIS_Y) != null) {
                    add(GamepadButton.UP); add(GamepadButton.DOWN)
                }
                if (triggerRange(device, MotionEvent.AXIS_LTRIGGER, MotionEvent.AXIS_BRAKE) != null) add(GamepadButton.L2)
                if (triggerRange(device, MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_GAS) != null) add(GamepadButton.R2)
                val (rightX, rightY) = rightAxes(device)
                if (stickRange(device, rightX) != null) {
                    add(GamepadButton.RIGHT_STICK_LEFT)
                    add(GamepadButton.RIGHT_STICK_RIGHT)
                }
                if (stickRange(device, rightY) != null) {
                    add(GamepadButton.RIGHT_STICK_UP)
                    add(GamepadButton.RIGHT_STICK_DOWN)
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
            KeyEvent.KEYCODE_BUTTON_L2 to GamepadButton.L2, KeyEvent.KEYCODE_BUTTON_R2 to GamepadButton.R2,
            KeyEvent.KEYCODE_BUTTON_L1 to GamepadButton.L1, KeyEvent.KEYCODE_BUTTON_R1 to GamepadButton.R1,
        )
    }
}
