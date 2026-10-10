package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.input.GamepadButton
import com.digihori.pgp.input.GamepadKeys
import com.digihori.pgp.input.GamepadSettings
import java.util.prefs.AbstractPreferences
import kotlin.test.*

class DesktopGamepadPreferencesTest {
    @Test
    fun saveReplacesOneSettingAndLoadsItInANewStore() {
        val node = MemoryPreferences()
        val store = DesktopGamepadPreferences(node)
        assertEquals(GamepadSettings(), store.load())
        store.save(GamepadSettings(true, mapOf(GamepadButton.LEFT to PocketKey.NUM_4)))
        assertEquals(PocketKey.NUM_4, DesktopGamepadPreferences(node).load().bindings[GamepadButton.LEFT])
        val replacement = GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.ENTER))
        store.save(replacement)
        assertEquals(replacement, DesktopGamepadPreferences(node).load())
    }

    @Test
    fun selectableKeysMatchTheExistingMachineKeyboards() {
        val layouts = mapOf(
            "pc-1245" to Pc1245KeyboardLayout.rows,
            "pc-1251" to Pc1251KeyboardLayout.rows,
            "pc-1261" to Pc1261KeyboardLayout.rows,
            "pc-1350" to Pc1350KeyboardLayout.rows,
            "pc-1360" to Pc1360KeyboardLayout.rows,
        )
        layouts.forEach { (machine, rows) ->
            assertEquals(rows.flatten().map { it.key }.toSet(), GamepadKeys.forMachine(MachineId(machine)))
        }
    }

    private class MemoryPreferences : AbstractPreferences(null, "") {
        private val values = mutableMapOf<String, String>()
        override fun putSpi(key: String, value: String) { values[key] = value }
        override fun getSpi(key: String): String? = values[key]
        override fun removeSpi(key: String) { values.remove(key) }
        override fun keysSpi(): Array<String> = values.keys.toTypedArray()
        override fun childrenNamesSpi(): Array<String> = emptyArray()
        override fun childSpi(name: String): AbstractPreferences = error("No children")
        override fun removeNodeSpi() { values.clear() }
        override fun flushSpi() = Unit
        override fun syncSpi() = Unit
    }
}
