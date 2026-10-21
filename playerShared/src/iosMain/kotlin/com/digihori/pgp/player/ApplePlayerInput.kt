package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.input.*

internal fun appleGamepadSettings(enabled: Boolean, buttons: List<String>, keys: List<String>): GamepadSettings =
    GamepadSettings.fromPreferences { id ->
        if (id == "enabled") enabled.toString() else keys.getOrNull(buttons.indexOf(id))
    }

/** Swift uses string IDs; the canonical choices, validation and device selection remain shared. */
public object AppleInputCatalog {
    public val buttons: List<String> get() = GamepadButton.entries.map { it.name }
    public val keys: List<String> get() = AppleMachineCatalog.machines.flatMap { it.keys }.distinct()
    public fun buttonLabel(name: String): String = GamepadButton.entries.find { it.name == name }?.label ?: name
    public fun keyLabel(name: String?): String = GamepadKeys.label(PocketKey.entries.find { it.name == name })
    public fun normalizedBindings(buttons: List<String>, keys: List<String>): Map<String, String> =
        appleGamepadSettings(false, buttons, keys).preferenceValues().filterKeys { it != "enabled" }
    public fun keyForCode(code: Int): String? = applePhysicalKey(code)?.name
    public fun keyForMachine(code: Int, machineId: String): String? = applePhysicalKeyForMachine(code, machineId)?.name
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

/** Machine-specific physical positions; the common mapping for existing machines is unchanged. */
internal fun applePhysicalKeyForMachine(code: Int, machineId: String): PocketKey? =
    if (machineId in listOf("pc-1350", "pc-1360", "pc-1360k")) when (code) {
        59 -> PocketKey.MODE // F2
        60 -> PocketKey.KANA // F3
        61 -> if (machineId in listOf("pc-1360", "pc-1360k")) PocketKey.SMALL else null
        62 -> if (machineId == "pc-1360k") PocketKey.KANJI else null
        63 -> if (machineId == "pc-1360k") PocketKey.HIRA_KATA else null
        64 -> if (machineId == "pc-1360k") PocketKey.KANJI_FIX else null
        65 -> if (machineId == "pc-1360k") PocketKey.KANJI_CONVERT else null
        66 -> if (machineId == "pc-1360k") PocketKey.YJ else null
        73 -> PocketKey.INSERT
        76 -> PocketKey.DELETE
        54 -> PocketKey.COMMA
        51 -> PocketKey.SEMICOLON
        182 -> PocketKey.LEFT_PAREN
        183 -> PocketKey.RIGHT_PAREN
        else -> applePhysicalKey(code)
    } else if (machineId in listOf("pc-1401", "pc-1402", "pc-1450", "pc-1460", "pc-1470u")) when (code) {
        59 -> PocketKey.CAL
        60 -> PocketKey.BASIC
        61 -> if (machineId in listOf("pc-1450", "pc-1460", "pc-1470u")) PocketKey.KANA else null
        62 -> if (machineId in listOf("pc-1450", "pc-1460", "pc-1470u")) PocketKey.SMALL else null
        63 -> if (machineId in listOf("pc-1450", "pc-1460", "pc-1470u")) PocketKey.DAKUTEN else null
        64 -> if (machineId in listOf("pc-1450", "pc-1460", "pc-1470u")) PocketKey.LONG_VOWEL else null
        65 -> when (machineId) {
            "pc-1470u" -> PocketKey.SHIFT
            "pc-1450", "pc-1460" -> PocketKey.LOCK
            else -> null
        }
        42 -> if (machineId == "pc-1470u") PocketKey.BACKSPACE else PocketKey.CLEAR
        76 -> if (machineId == "pc-1470u") PocketKey.DELETE else PocketKey.CLEAR
        73 -> if (machineId == "pc-1470u") PocketKey.INSERT else null
        54 -> PocketKey.COMMA
        51 -> PocketKey.SEMICOLON
        182 -> PocketKey.LEFT_PAREN
        183 -> PocketKey.RIGHT_PAREN
        else -> applePhysicalKey(code)
    } else applePhysicalKey(code)
