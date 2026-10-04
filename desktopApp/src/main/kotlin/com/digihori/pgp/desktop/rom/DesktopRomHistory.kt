package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.rom.MachineId
import java.io.File
import java.util.prefs.Preferences

internal class DesktopRomHistory(
    private val preferences: PreferenceStore = JavaPreferenceStore(),
) {
    fun lastSelection(): DesktopRomSelection? {
        val path = preferences.get(PATH_KEY)?.takeIf(String::isNotBlank) ?: return null
        val machineId = preferences.get(MACHINE_ID_KEY)
            ?.takeIf(String::isNotBlank)
            ?.let { runCatching { MachineId(it) }.getOrNull() }
            ?: return null
        return DesktopRomSelection(File(path), machineId)
    }

    fun remember(file: File, machineId: MachineId) {
        preferences.put(PATH_KEY, file.absoluteFile.path)
        preferences.put(MACHINE_ID_KEY, machineId.value)
    }

    fun clear() {
        preferences.remove(PATH_KEY)
        preferences.remove(MACHINE_ID_KEY)
    }

    private companion object {
        const val PATH_KEY = "lastRomPath"
        const val MACHINE_ID_KEY = "lastRomMachineId"
    }
}

internal data class DesktopRomSelection(
    val file: File,
    val machineId: MachineId,
)

internal interface PreferenceStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

private class JavaPreferenceStore : PreferenceStore {
    private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop")

    override fun get(key: String): String? = runCatching { preferences.get(key, null) }.getOrNull()

    override fun put(key: String, value: String) {
        runCatching { preferences.put(key, value) }
    }

    override fun remove(key: String) {
        runCatching { preferences.remove(key) }
    }
}
