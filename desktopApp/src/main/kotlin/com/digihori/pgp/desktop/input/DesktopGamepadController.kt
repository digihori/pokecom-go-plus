package com.digihori.pgp.desktop.input

import androidx.compose.runtime.*
import com.digihori.pgp.desktop.localization.uiText
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import com.digihori.pgp.input.*

/** Mutable UI state and emulator input live on EDT; only immutable snapshots cross Cocoa. */
internal class DesktopGamepadController {
    private val preferences = DesktopGamepadPreferences()
    private val backend = SdlGamepadBackend(unnamedDeviceName = uiText("controller.unnamedDevice"))
    var settings by mutableStateOf(runCatching { preferences.load() }.getOrDefault(GamepadSettings()))
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
    private var requestedId: String? = null
    private var runner: DesktopEmulatorRunner? = null
    private var inputEpoch = -1L
    private var receiving = false
    private val input = GamepadInput(
        press = { button, key -> runner?.pressGamepadKey(button, key) },
        release = { button, key -> runner?.releaseGamepadKey(button, key) },
    )

    fun setContext(target: DesktopEmulatorRunner?, receiving: Boolean) {
        if (runner !== target || inputEpoch != (target?.inputEpoch ?: -1L)) {
            input.reset() // Release through the old session before switching it.
            runner = target
            inputEpoch = target?.inputEpoch ?: -1L
        }
        this.receiving = receiving
        configure()
    }

    private fun configure() {
        input.configure(settings, selectedId, runner?.let { GamepadKeys.forMachine(it.machineId) }.orEmpty(),
            receiving && runner?.state == RunnerState.RUNNING)
    }

    fun select(id: String) {
        if (devices.none { it.id == id }) return
        requestedId = id
        input.reset()
    }

    fun save(value: GamepadSettings) {
        runCatching { preferences.save(value) }.onSuccess {
            settings = value.copy(bindings = value.bindings.toMap())
            configure()
            status = uiText("controller.saved")
        }.onFailure { status = uiText("controller.saveFailed", it.message) }
    }

    suspend fun poll() {
        val snapshot = backend.poll(requestedId)
        devices = snapshot.devices
        selectedId = snapshot.selectedId
        requestedId = selectedId
        pressed = snapshot.buttons
        available = if (selectedId == null) GamepadButton.entries.toSet() else snapshot.availableButtons
        configure()
        input.update(selectedId, snapshot.buttons)
    }

    fun unavailable(failure: Throwable) {
        input.reset()
        devices = emptyList()
        selectedId = null
        pressed = emptySet()
        status = uiText("controller.inputUnavailable", failure.message ?: failure::class.simpleName)
    }

    suspend fun close() {
        input.reset()
        backend.close()
    }
}
