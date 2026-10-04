package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopRomHistoryTest {
    @Test
    fun remembersTheAbsolutePathAndMachineForLegacyImages() {
        val preferences = MemoryPreferenceStore()
        val history = DesktopRomHistory(preferences)
        val file = File("roms/pc1251.bin")

        history.remember(file, Pc1251RomDefinition.MACHINE_ID)

        assertEquals(
            DesktopRomSelection(file.absoluteFile, Pc1251RomDefinition.MACHINE_ID),
            history.lastSelection(),
        )
    }

    @Test
    fun ignoresIncompleteOrInvalidSavedSettings() {
        val preferences = MemoryPreferenceStore()
        val history = DesktopRomHistory(preferences)

        preferences.put("lastRomPath", "/tmp/example.pgrom")
        assertNull(history.lastSelection())
        preferences.put("lastRomMachineId", "INVALID ID")
        assertNull(history.lastSelection())
    }

    @Test
    fun clearsTheSavedSelection() {
        val history = DesktopRomHistory(MemoryPreferenceStore())
        history.remember(File("example.pgrom"), Pc1251RomDefinition.MACHINE_ID)

        history.clear()

        assertNull(history.lastSelection())
    }

    private class MemoryPreferenceStore : PreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun get(key: String): String? = values[key]
        override fun put(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }
}
