package com.digihori.pgp.input

import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxKeyCatalog
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxModel
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

/** Face buttons use physical positions; printed A/B/X/Y labels vary by manufacturer. */
enum class GamepadButton(val label: String) {
    UP("↑"), DOWN("↓"), LEFT("←"), RIGHT("→"),
    SOUTH("A / 下"), EAST("B / 右"), WEST("X / 左"), NORTH("Y / 上"),
    L1("L1"), R1("R1"),
}

data class GamepadDevice(val id: String, val name: String)

/** Select one pad, never silently hand input to an already-connected second pad. */
class SingleGamepadSelection {
    var selectedId: String? = null
        private set
    private var knownIds = emptySet<String>()
    private var scanned = false

    fun update(devices: List<GamepadDevice>, requestedId: String? = null): String? {
        val ids = devices.map { it.id }.toSet()
        if (requestedId != null && requestedId in ids) selectedId = requestedId
        if (selectedId !in ids) selectedId = null
        val sole = ids.singleOrNull()
        if (selectedId == null && sole != null && (!scanned || sole !in knownIds)) selectedId = sole
        knownIds = ids
        scanned = true
        return selectedId
    }
}

data class GamepadSettings(
    val enabled: Boolean = false,
    val bindings: Map<GamepadButton, PocketKey> = emptyMap(),
) {
    fun withBinding(button: GamepadButton, key: PocketKey?): GamepadSettings =
        copy(bindings = bindings.toMutableMap().apply {
            if (key == null) remove(button) else put(button, key)
        })

    /** Small, platform-neutral preference payload; unknown values are safely unassigned. */
    fun preferenceValues(): Map<String, String> = buildMap {
        put("enabled", enabled.toString())
        GamepadButton.entries.forEach { put(it.name, bindings[it]?.name.orEmpty()) }
    }

    companion object {
        fun fromPreferences(read: (String) -> String?): GamepadSettings = GamepadSettings(
            enabled = read("enabled") == "true",
            bindings = GamepadButton.entries.mapNotNull { button ->
                PocketKey.entries.find { it.name == read(button.name) }?.let { button to it }
            }.toMap(),
        )
    }
}

/** Owns keys by input token, so releasing one input cannot release another input's hold. */
class HostKeyState(
    private val press: (PocketKey) -> InputResult,
    private val release: (PocketKey) -> InputResult,
) {
    private val owners = mutableMapOf<String, PocketKey>()

    fun isHeld(key: PocketKey): Boolean = key in owners.values

    fun press(owner: String, key: PocketKey): InputResult {
        if (owners[owner] == key) return InputResult.Accepted
        release(owner)
        val result = if (isHeld(key)) InputResult.Accepted else press(key)
        if (result is InputResult.Accepted) owners[owner] = key
        return result
    }

    fun release(owner: String): InputResult {
        val key = owners.remove(owner) ?: return InputResult.Accepted
        return if (isHeld(key)) InputResult.Accepted else release(key)
    }

    fun releaseAll() {
        val keys = owners.values.toSet()
        owners.clear()
        keys.forEach { release(it) }
    }
}

/** Single-device button dispatcher. Hosts supply snapshots on their session thread. */
class GamepadInput(
    private val press: (GamepadButton, PocketKey) -> Unit,
    private val release: (GamepadButton, PocketKey) -> Unit,
) {
    private var settings = GamepadSettings()
    private var deviceId: String? = null
    private var allowedKeys = emptySet<PocketKey>()
    private var receiving = false
    private var blocked = GamepadButton.entries.toSet()
    private val held = mutableMapOf<GamepadButton, PocketKey>()

    fun configure(settings: GamepadSettings, deviceId: String?, allowedKeys: Set<PocketKey>, receiving: Boolean) {
        if (this.settings == settings && this.deviceId == deviceId &&
            this.allowedKeys == allowedKeys && this.receiving == receiving) return
        reset()
        this.settings = settings.copy(bindings = settings.bindings.toMap())
        this.deviceId = deviceId
        this.allowedKeys = allowedKeys.toSet()
        this.receiving = receiving
    }

    /** After changes/focus loss, a held button must first be released before it can fire. */
    fun reset() {
        val previous = held.toMap()
        held.clear()
        previous.forEach { (button, key) -> release(button, key) }
        blocked = GamepadButton.entries.toSet()
    }

    fun update(id: String?, buttons: Set<GamepadButton>) {
        if (id != deviceId || id == null) return
        blocked = blocked.intersect(buttons)
        held.keys.filter { it !in buttons }.toList().forEach { button ->
            held.remove(button)?.let { release(button, it) }
        }
        if (!receiving || !settings.enabled) return
        buttons.filter { it !in blocked && it !in held }.forEach { button ->
            val key = settings.bindings[button]?.takeIf { it in allowedKeys } ?: return@forEach
            held[button] = key
            press(button, key)
        }
    }
}

/** Physical key choices matching the keyboard layouts exposed by the apps. */
object GamepadKeys {
    fun forMachine(machineId: MachineId): Set<PocketKey> {
        val common = PocketKey.entries.takeWhile { it != PocketKey.MODE }.toSet()
        return when (MachineCatalog.require(machineId).keyboardLayout) {
            MachineKeyboardLayout.PC_1245, MachineKeyboardLayout.PC_1251,
            MachineKeyboardLayout.PC_1261 -> common
            MachineKeyboardLayout.PC_1450, MachineKeyboardLayout.PC_1460, MachineKeyboardLayout.PC_1470U ->
                Pc14xxKeyCatalog.supportedKeys(checkNotNull(Pc14xxModel.fromMachineId(machineId)))
            MachineKeyboardLayout.PC_1401 -> common + PocketKey.entries.filter {
                it.ordinal in PocketKey.CAL.ordinal..PocketKey.MEMORY_ADD.ordinal
            } + setOf(PocketKey.LEFT_PAREN, PocketKey.RIGHT_PAREN, PocketKey.COMMA)
            MachineKeyboardLayout.PC_1350 -> common + setOf(
                PocketKey.MODE, PocketKey.KANA, PocketKey.INSERT, PocketKey.DELETE,
                PocketKey.LEFT_PAREN, PocketKey.RIGHT_PAREN, PocketKey.COLON,
                PocketKey.SEMICOLON, PocketKey.COMMA,
            )
            MachineKeyboardLayout.PC_1360 -> common + setOf(
                PocketKey.MODE, PocketKey.KANA, PocketKey.SMALL, PocketKey.INSERT,
                PocketKey.DELETE, PocketKey.COLON, PocketKey.SEMICOLON, PocketKey.COMMA,
            )
        }
    }

    fun label(key: PocketKey?): String = when (key) {
        null -> "未割当"
        PocketKey.LEFT -> "←"
        PocketKey.RIGHT -> "→"
        PocketKey.UP -> "↑"
        PocketKey.DOWN -> "↓"
        PocketKey.PLUS -> "+"
        PocketKey.MINUS -> "−"
        PocketKey.MULTIPLY -> "×"
        PocketKey.DIVIDE -> "÷"
        PocketKey.DOT -> "."
        PocketKey.EQUALS -> "="
        PocketKey.LEFT_PAREN -> "("
        PocketKey.RIGHT_PAREN -> ")"
        PocketKey.COLON -> ":"
        PocketKey.SEMICOLON -> ";"
        PocketKey.COMMA -> ","
        else -> key.name.removePrefix("NUM_")
    }
}
