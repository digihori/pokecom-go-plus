package com.digihori.pgp.desktop.input

import com.digihori.pgp.input.GamepadSettings
import java.util.prefs.Preferences

internal class DesktopGamepadPreferences(
    private val preferences: Preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/gamepad"),
) {
    fun load(): GamepadSettings = GamepadSettings.fromPreferences { preferences.get(it, null) }

    fun save(settings: GamepadSettings) {
        settings.preferenceValues().forEach { (key, value) -> preferences.put(key, value) }
        preferences.flush()
    }
}
