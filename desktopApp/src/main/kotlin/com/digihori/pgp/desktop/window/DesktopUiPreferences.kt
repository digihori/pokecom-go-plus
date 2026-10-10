package com.digihori.pgp.desktop.window

import com.digihori.pgp.desktop.input.HostKeyInputMode
import java.util.prefs.Preferences

internal object DesktopUiPreferences {
    private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/ui")

    fun projectPaneWidth(defaultValue: Float): Float =
        preferences.getFloat("projectPaneWidthDp", defaultValue)

    fun saveProjectPaneWidth(value: Float) {
        preferences.putFloat("projectPaneWidthDp", value)
    }

    fun sourceInformationHeight(defaultValue: Float): Float =
        preferences.getFloat("sourceInformationHeightDp", defaultValue)

    fun saveSourceInformationHeight(value: Float) {
        preferences.putFloat("sourceInformationHeightDp", value)
    }

    fun hostKeyInputMode(): HostKeyInputMode = runCatching {
        HostKeyInputMode.valueOf(preferences.get("hostKeyInputMode", HostKeyInputMode.LOGICAL.name))
    }.getOrDefault(HostKeyInputMode.LOGICAL)

    fun saveHostKeyInputMode(value: HostKeyInputMode) {
        preferences.put("hostKeyInputMode", value.name)
    }
}
