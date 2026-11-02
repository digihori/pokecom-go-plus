package com.digihori.pgp.desktop.window

import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.api.RamMode
import java.util.prefs.Preferences

internal class DesktopRamPreferences(private val preferences: Preferences) {
    fun load(family: MachineFamily = MachineFamily.PC_1251): RamMode = runCatching {
        val stored = preferences.get(key(family), null)
        val legacy = if (stored == null && family == MachineFamily.PC_1251) preferences.get(LEGACY_KEY, null) else null
        when (stored ?: legacy) {
            "UNRESTRICTED", "EXPANDED" -> RamMode.UNRESTRICTED
            else -> RamMode.HARDWARE
        }
    }.getOrDefault(RamMode.HARDWARE)

    fun save(value: RamMode, family: MachineFamily = MachineFamily.PC_1251) {
        val key = key(family)
        val previous = preferences.get(key, null)
        try {
            preferences.put(key, value.name)
            preferences.flush()
        } catch (failure: Exception) {
            if (previous == null) preferences.remove(key) else preferences.put(key, previous)
            throw failure
        }
    }

    companion object {
        val FAMILIES = listOf(MachineFamily.PC_1251, MachineFamily.PC_1261, MachineFamily.PC_1401)
        const val LEGACY_KEY = "pc1251FamilyMemoryMode"
        const val KEY = "ramMode.PC_1251"
        fun key(family: MachineFamily): String {
            require(family in FAMILIES)
            return "ramMode.${family.name}"
        }
    }
}
