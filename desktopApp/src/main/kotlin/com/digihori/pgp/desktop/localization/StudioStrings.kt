package com.digihori.pgp.desktop.localization

import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

enum class StudioLanguage(val locale: Locale) {
    ENGLISH(Locale.ENGLISH), JAPANESE(Locale.JAPANESE);

    companion object {
        fun fromOsLocale(locale: Locale): StudioLanguage =
            if (locale.language == "ja") JAPANESE else ENGLISH
    }
}

/** UI-only locale. Never changes the process locale or persistent data. */
class StudioStrings(
    val language: StudioLanguage,
    internal val bundleName: String = "com.digihori.pgp.desktop.localization.Messages",
) {
    private val bundle = ResourceBundle.getBundle(
        bundleName, language.locale,
        ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES),
    )

    fun text(key: String, vararg arguments: Any?): String {
        val formatter = MessageFormat(bundle.getString(key), language.locale)
        require(arguments.size == formatter.formatsByArgumentIndex.size) {
            "UI message $key expects ${formatter.formatsByArgumentIndex.size} arguments, received ${arguments.size}"
        }
        return formatter.format(arguments)
    }

    companion object {
        private var resolved = false
        internal var startupPreference: StudioLanguagePreference = StudioLanguagePreference.OS_DEFAULT
            private set

        // Called by Desktop startup only, before any screen resolves the UI language.
        internal fun configureForStartup(preference: StudioLanguagePreference) {
            check(!resolved) { "Studio UI language has already been resolved" }
            startupPreference = preference
        }

        // Frozen for this process. Saving a preference does not change live UI strings.
        val system: StudioStrings by lazy {
            resolved = true
            StudioStrings(startupPreference.resolve(Locale.getDefault(Locale.Category.DISPLAY)))
        }
    }
}

internal fun uiText(key: String, vararg arguments: Any?): String =
    StudioStrings.system.text(key, *arguments)
