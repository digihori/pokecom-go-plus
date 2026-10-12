package com.digihori.pgp.desktop.window

import com.digihori.pgp.desktop.input.HostKeyInputMode
import java.util.prefs.Preferences
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed

internal object DesktopUiPreferences {
    private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/ui")

    fun executionSpeed(): DesktopExecutionSpeed = runCatching {
        DesktopExecutionSpeed.valueOf(preferences.get("executionSpeed", DesktopExecutionSpeed.LEVEL_3.name))
    }.getOrDefault(DesktopExecutionSpeed.LEVEL_3)

    fun saveExecutionSpeed(value: DesktopExecutionSpeed) {
        preferences.put("executionSpeed", value.name)
    }

    // Absence preserves legacy PGP_EDITOR behavior; an empty value explicitly uses the OS editor.
    fun externalEditorCommand(): String? = preferences.get("externalEditorCommand", null)

    fun saveExternalEditorCommand(value: String) {
        preferences.put("externalEditorCommand", value.trim())
    }

    fun assemblerSettings() = com.digihori.pgp.desktop.project.DesktopAssemblerSettings(
        external = preferences.getBoolean("assemblerExternal", false),
        executable = preferences.get("assemblerExecutable", ""),
        arguments = preferences.get("assemblerArguments", "{source}"),
        timeoutSeconds = preferences.getInt("assemblerTimeout", 60).coerceIn(1, 3600),
    )

    fun saveAssemblerSettings(value: com.digihori.pgp.desktop.project.DesktopAssemblerSettings) {
        preferences.putBoolean("assemblerExternal", value.external)
        preferences.put("assemblerExecutable", value.executable.trim())
        preferences.put("assemblerArguments", value.arguments)
        preferences.putInt("assemblerTimeout", value.timeoutSeconds)
    }

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
