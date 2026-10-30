package com.digihori.pgp.desktop.window

import com.digihori.pgp.desktop.input.HostKeyInputMode
import java.util.prefs.Preferences
import com.digihori.pgp.desktop.localization.StudioLanguagePreference
import com.digihori.pgp.desktop.localization.StudioLanguagePreferences
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed

internal object DesktopUiPreferences {
    private val preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/ui")

    fun ramMode() = DesktopRamPreferences(preferences).load()

    fun saveRamMode(value: com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode) {
        DesktopRamPreferences(preferences).save(value)
    }

    fun languagePreference(): StudioLanguagePreference = runCatching {
        StudioLanguagePreferences(preferences).load()
    }.getOrDefault(StudioLanguagePreference.OS_DEFAULT)

    fun saveLanguagePreference(value: StudioLanguagePreference) {
        StudioLanguagePreferences(preferences).save(value)
    }

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

    fun assemblerStore() = com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences)

    fun assemblerSettings() = com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences).load()

    fun saveAssemblerSettings(value: com.digihori.pgp.desktop.project.DesktopAssemblerSettings) {
        com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences).save(value)
    }

    fun assemblerProfile(tool: com.digihori.pgp.desktop.project.DesktopAssemblerTool) =
        com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences).profile(tool)

    fun saveAssemblerProfile(value: com.digihori.pgp.desktop.project.DesktopAssemblerSettings) {
        com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences).saveProfile(value)
    }

    fun selectAssembler(tool: com.digihori.pgp.desktop.project.DesktopAssemblerTool?) {
        com.digihori.pgp.desktop.project.DesktopAssemblerPreferences(preferences).select(tool)
    }

    fun projectPaneWidth(defaultValue: Float): Float =
        preferences.getFloat("projectPaneWidthDp", defaultValue)

    fun saveProjectPaneWidth(value: Float) {
        preferences.putFloat("projectPaneWidthDp", value)
    }

    fun projectControlsHeight(defaultValue: Float): Float =
        preferences.getFloat("projectControlsHeightDp", defaultValue)

    fun saveProjectControlsHeight(value: Float) {
        preferences.putFloat("projectControlsHeightDp", value)
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
