package com.digihori.pgp.desktop.localization

import java.util.Locale
import java.util.prefs.AbstractPreferences
import java.util.prefs.BackingStoreException
import kotlin.test.*

internal class LanguageMemoryPreferences : AbstractPreferences(null, "") {
    val values = mutableMapOf<String, String>()
    var failFlush = false
    override fun putSpi(key: String, value: String) { values[key] = value }
    override fun getSpi(key: String): String? = values[key]
    override fun removeSpi(key: String) { values.remove(key) }
    override fun keysSpi(): Array<String> = values.keys.toTypedArray()
    override fun childrenNamesSpi(): Array<String> = emptyArray()
    override fun childSpi(name: String): AbstractPreferences = error("No children")
    override fun removeNodeSpi() { values.clear() }
    override fun flushSpi() { if (failFlush) throw BackingStoreException("synthetic flush failure") }
    override fun syncSpi() = Unit
}

class StudioLanguagePreferencesTest {
    @Test fun settingsRoundTripAndUnknownValuesUseOsDefaultWithoutChangingOtherKeys() {
        val preferences = LanguageMemoryPreferences()
        preferences.values.putAll(mapOf("executionSpeed" to "LEVEL_3", "assemblerArguments" to "{source}", "externalEditorCommand" to "code --goto {file}:{line}"))
        val original = preferences.values.toMap()
        val store = StudioLanguagePreferences(preferences)
        assertEquals(StudioLanguagePreference.OS_DEFAULT, store.load())
        for (preference in StudioLanguagePreference.entries) {
            store.save(preference)
            assertEquals(preference, StudioLanguagePreferences(preferences).load())
            assertEquals(preference.value, preferences.values[StudioLanguagePreferences.KEY])
            assertEquals(original, preferences.values.filterKeys { it != StudioLanguagePreferences.KEY })
        }
        preferences.values[StudioLanguagePreferences.KEY] = "future-language"
        assertEquals(StudioLanguagePreference.OS_DEFAULT, store.load())
        assertEquals(StudioLanguage.JAPANESE, store.load().resolve(Locale.JAPANESE))
        assertEquals(StudioLanguage.ENGLISH, store.load().resolve(Locale.FRENCH))
    }

    @Test fun saveFailureRestoresPreviousPreference() {
        for (previous in listOf(null, "ja")) {
            val preferences = LanguageMemoryPreferences()
            if (previous != null) preferences.values[StudioLanguagePreferences.KEY] = previous
            preferences.failFlush = true
            assertFailsWith<BackingStoreException> { StudioLanguagePreferences(preferences).save(StudioLanguagePreference.ENGLISH) }
            assertEquals(previous, preferences.values[StudioLanguagePreferences.KEY])
        }
    }
}
