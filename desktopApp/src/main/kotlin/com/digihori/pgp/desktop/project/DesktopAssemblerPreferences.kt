package com.digihori.pgp.desktop.project

import java.util.prefs.Preferences

/** Tool profiles and the active choice are persisted independently. */
internal class DesktopAssemblerPreferences(private val preferences: Preferences) {
    private fun legacyTool() = runCatching { DesktopAssemblerTool.valueOf(preferences.get("assemblerTool", "SC61")) }
        .getOrDefault(DesktopAssemblerTool.SC61)

    fun load(): DesktopAssemblerSettings {
        val active = preferences.get("assemblerActive", null)
        val external = if (active == null) preferences.getBoolean("assemblerExternal", false) else active != "INTERNAL"
        val tool = if (!external || active == null) legacyTool() else
            runCatching { DesktopAssemblerTool.valueOf(active) }.getOrDefault(legacyTool())
        return profile(tool).copy(external = external)
    }

    fun profile(tool: DesktopAssemblerTool): DesktopAssemblerSettings {
        val prefix = "assemblerProfile.${tool.name}."
        val migrated = preferences.get(prefix + "executable", null) != null
        val legacy = !migrated && tool == legacyTool()
        fun text(key: String, old: String, fallback: String) =
            if (migrated) preferences.get(prefix + key, fallback) else if (legacy) preferences.get(old, fallback) else fallback
        fun number(key: String, old: String, fallback: Int) =
            if (migrated) preferences.getInt(prefix + key, fallback) else if (legacy) preferences.getInt(old, fallback) else fallback
        return DesktopAssemblerSettings(
            external = true, tool = tool,
            executable = text("executable", "assemblerExecutable", ""),
            arguments = text("arguments", "assemblerArguments", "{source}"),
            timeoutSeconds = number("timeout", "assemblerTimeout", 60).coerceIn(1, 3600),
            successExitCode = number("successExitCode", "assemblerSuccessExitCode", 0).coerceIn(0, 255),
            testSource = text("testSource", "assemblerTestSource", ""),
        )
    }

    fun saveProfile(value: DesktopAssemblerSettings) {
        val prefix = "assemblerProfile.${value.tool.name}."
        preferences.put(prefix + "executable", value.executable.trim())
        preferences.put(prefix + "arguments", value.arguments)
        preferences.putInt(prefix + "timeout", value.timeoutSeconds)
        preferences.putInt(prefix + "successExitCode", value.successExitCode)
        preferences.put(prefix + "testSource", value.testSource)
    }

    fun select(tool: DesktopAssemblerTool?) {
        // Capture the legacy profile before changing its identity.
        saveProfile(profile(legacyTool()))
        preferences.put("assemblerActive", tool?.name ?: "INTERNAL")
    }

    fun save(value: DesktopAssemblerSettings) {
        saveProfile(value)
        select(if (value.external) value.tool else null)
    }
}
