package com.digihori.pgp.desktop.window

import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.desktop.localization.LanguageMemoryPreferences
import java.util.prefs.BackingStoreException
import kotlin.test.*

class DesktopRamPreferencesTest {
    @Test fun defaultsToHardwareAndRestoresBothModesAcrossStoreInstances() {
        val preferences = LanguageMemoryPreferences()
        preferences.put("uiLanguage", "ja")
        assertEquals(Pc1251FamilyMemoryMode.HARDWARE, DesktopRamPreferences(preferences).load())
        for (mode in listOf(Pc1251FamilyMemoryMode.EXPANDED, Pc1251FamilyMemoryMode.HARDWARE)) {
            DesktopRamPreferences(preferences).save(mode)
            assertEquals(mode, DesktopRamPreferences(preferences).load())
            assertEquals("ja", preferences.get("uiLanguage", null))
        }
        preferences.put(DesktopRamPreferences.KEY, "future-mode")
        assertEquals(Pc1251FamilyMemoryMode.HARDWARE, DesktopRamPreferences(preferences).load())
    }

    @Test fun failedSavePreservesPreviousSelection() {
        for (previous in listOf(null, Pc1251FamilyMemoryMode.EXPANDED.name)) {
            val preferences = LanguageMemoryPreferences()
            if (previous != null) preferences.put(DesktopRamPreferences.KEY, previous)
            preferences.failFlush = true
            assertFailsWith<BackingStoreException> {
                DesktopRamPreferences(preferences).save(Pc1251FamilyMemoryMode.HARDWARE)
            }
            assertEquals(previous, preferences.get(DesktopRamPreferences.KEY, null))
        }
    }
}
