package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.input.*

internal fun appleGamepadSettings(enabled: Boolean, buttons: List<String>, keys: List<String>): GamepadSettings =
    GamepadSettings.fromPreferences { id ->
        if (id == "enabled") enabled.toString() else keys.getOrNull(buttons.indexOf(id))
    }

/** Swift uses string IDs; the canonical choices, validation and device selection remain shared. */
public object AppleInputCatalog {
    public val buttons: List<String> get() = GamepadButton.entries.map { it.name }
    public val keys: List<String> get() = GamepadKeys.forMachine(MachineId("pc-1245")).map { it.name }
    public fun buttonLabel(name: String): String = GamepadButton.entries.find { it.name == name }?.label ?: name
    public fun keyLabel(name: String?): String = GamepadKeys.label(PocketKey.entries.find { it.name == name })
    public fun normalizedBindings(buttons: List<String>, keys: List<String>): Map<String, String> =
        appleGamepadSettings(false, buttons, keys).preferenceValues().filterKeys { it != "enabled" }
    public fun keyForCode(code: Int): String? = applePhysicalKey(code)?.name
}

public class AppleGamepadSelection {
    private val selection = SingleGamepadSelection()
    public fun update(ids: List<String>, requestedId: String?): String? =
        selection.update(ids.map { GamepadDevice(it, it) }, requestedId)
}

/** USB HID usages used by Apple GCKeyCode/UIKeyboardHIDUsage, never generated text/IME. */
internal fun applePhysicalKey(code: Int): PocketKey? = when (code) {
    in 4..29 -> PocketKey.valueOf(('A' + code - 4).toString())
    in 30..38 -> PocketKey.valueOf("NUM_${code - 29}")
    39, 98 -> PocketKey.NUM_0
    in 89..97 -> PocketKey.valueOf("NUM_${code - 88}")
    225, 229 -> PocketKey.SHIFT
    224, 58 -> PocketKey.DEF
    41, 72 -> PocketKey.BREAK
    40, 88 -> PocketKey.ENTER
    42, 76, 156 -> PocketKey.CLEAR
    44 -> PocketKey.SPACE
    80 -> PocketKey.LEFT
    79 -> PocketKey.RIGHT
    82 -> PocketKey.UP
    81 -> PocketKey.DOWN
    87 -> PocketKey.PLUS
    45, 86 -> PocketKey.MINUS
    85 -> PocketKey.MULTIPLY
    56, 84 -> PocketKey.DIVIDE
    55, 99 -> PocketKey.DOT
    46, 103 -> PocketKey.EQUALS
    else -> null
}
