package com.digihori.pgp.desktop.localization

import java.util.Locale
import java.util.prefs.Preferences

/** Stable preference values; neither the UI label nor Locale is persisted. */
internal enum class StudioLanguagePreference(val value: String) {
    OS_DEFAULT("system"), JAPANESE("ja"), ENGLISH("en");

    fun resolve(osLocale: Locale): StudioLanguage = when (this) {
        OS_DEFAULT -> StudioLanguage.fromOsLocale(osLocale)
        JAPANESE -> StudioLanguage.JAPANESE
        ENGLISH -> StudioLanguage.ENGLISH
    }

    companion object {
        fun fromValue(value: String?): StudioLanguagePreference = entries.firstOrNull { it.value == value } ?: OS_DEFAULT
    }
}

/** Injected store: tests use in-memory Preferences and never the macOS preferences root. */
internal class StudioLanguagePreferences(private val preferences: Preferences) {
    fun load(): StudioLanguagePreference = StudioLanguagePreference.fromValue(preferences.get(KEY, null))

    fun save(value: StudioLanguagePreference) {
        val previous = preferences.get(KEY, null)
        try {
            preferences.put(KEY, value.value)
            preferences.flush()
        } catch (failure: Exception) {
            if (previous == null) preferences.remove(KEY) else preferences.put(KEY, previous)
            throw failure
        }
    }

    companion object { const val KEY = "uiLanguage" }
}
