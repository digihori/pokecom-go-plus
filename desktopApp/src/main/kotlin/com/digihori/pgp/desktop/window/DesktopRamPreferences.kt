package com.digihori.pgp.desktop.window

import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import java.util.prefs.Preferences

internal class DesktopRamPreferences(private val preferences: Preferences) {
    fun load(): Pc1251FamilyMemoryMode = runCatching {
        Pc1251FamilyMemoryMode.valueOf(preferences.get(KEY, Pc1251FamilyMemoryMode.HARDWARE.name))
    }.getOrDefault(Pc1251FamilyMemoryMode.HARDWARE)

    fun save(value: Pc1251FamilyMemoryMode) {
        val previous = preferences.get(KEY, null)
        try {
            preferences.put(KEY, value.name)
            preferences.flush()
        } catch (failure: Exception) {
            if (previous == null) preferences.remove(KEY) else preferences.put(KEY, previous)
            throw failure
        }
    }

    companion object { const val KEY = "pc1251FamilyMemoryMode" }
}
