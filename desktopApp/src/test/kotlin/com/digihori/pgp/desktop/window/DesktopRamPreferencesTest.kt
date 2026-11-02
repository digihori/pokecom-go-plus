package com.digihori.pgp.desktop.window

import com.digihori.pgp.core.api.RamMode
import com.digihori.pgp.desktop.localization.LanguageMemoryPreferences
import java.util.prefs.BackingStoreException
import kotlin.test.*

class DesktopRamPreferencesTest {
    @Test fun defaultsToHardwareAndRestoresBothModesAcrossStoreInstances() {
        val preferences = LanguageMemoryPreferences()
        preferences.put("uiLanguage", "ja")
        assertEquals(RamMode.HARDWARE, DesktopRamPreferences(preferences).load())
        for (mode in listOf(RamMode.UNRESTRICTED, RamMode.HARDWARE)) {
            DesktopRamPreferences(preferences).save(mode)
            assertEquals(mode, DesktopRamPreferences(preferences).load())
            assertEquals("ja", preferences.get("uiLanguage", null))
        }
        preferences.put(DesktopRamPreferences.KEY, "future-mode")
        assertEquals(RamMode.HARDWARE, DesktopRamPreferences(preferences).load())
    }

    @Test fun migratesLegacy1251AndKeepsFamiliesIndependent() {
        val preferences = LanguageMemoryPreferences()
        preferences.put(DesktopRamPreferences.LEGACY_KEY, "EXPANDED")
        val store = DesktopRamPreferences(preferences)
        assertEquals(RamMode.UNRESTRICTED, store.load())
        assertEquals(RamMode.HARDWARE, store.load(com.digihori.pgp.core.api.MachineFamily.PC_1261))
        store.save(RamMode.UNRESTRICTED, com.digihori.pgp.core.api.MachineFamily.PC_1261)
        store.save(RamMode.HARDWARE)
        val fresh = DesktopRamPreferences(preferences)
        assertEquals(RamMode.HARDWARE, fresh.load())
        assertEquals(RamMode.UNRESTRICTED, fresh.load(com.digihori.pgp.core.api.MachineFamily.PC_1261))
        assertEquals(RamMode.HARDWARE, fresh.load(com.digihori.pgp.core.api.MachineFamily.PC_1401))
        assertEquals("EXPANDED", preferences.get(DesktopRamPreferences.LEGACY_KEY, null))
    }

    @Test fun failedSavePreservesPreviousSelection() {
        for (previous in listOf(null, RamMode.UNRESTRICTED.name)) {
            val preferences = LanguageMemoryPreferences()
            if (previous != null) preferences.put(DesktopRamPreferences.KEY, previous)
            preferences.failFlush = true
            assertFailsWith<BackingStoreException> {
                DesktopRamPreferences(preferences).save(RamMode.HARDWARE)
            }
            assertEquals(previous, preferences.get(DesktopRamPreferences.KEY, null))
        }
    }
}
