package com.digihori.pgp.desktop.localization

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class LanguageSettingsPanelTest {
    @Test fun selectingEnglishSavesOnlyPreferenceAndKeepsJapaneseUntilRestart() {
        SwingUtilities.invokeAndWait {
            val preferences = LanguageMemoryPreferences()
            val store = StudioLanguagePreferences(preferences)
            val selected = mutableStateOf(store.load())
            val strings = StudioStrings(StudioLanguage.JAPANESE)
            val scene = ImageComposeScene(420, 500) {
                PgpStudioTheme {
                    LanguageSettingsPanel(selected.value, { store.save(it); selected.value = it },
                        selected.value != StudioLanguagePreference.OS_DEFAULT, strings = strings)
                }
            }
            fun nodes(): List<SemanticsNode> {
                fun descend(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descend)
                return scene.semanticsOwners.flatMap { descend(it.rootSemanticsNode) }
            }
            try {
                scene.render(0).close()
                val english = nodes().first { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("English") == true }
                assertTrue(english.config[SemanticsActions.OnClick].action!!.invoke())
                scene.render(100_000_000).close()
                assertEquals(StudioLanguagePreference.ENGLISH, store.load())
                assertEquals(StudioLanguagePreference.ENGLISH, selected.value)
                assertEquals(StudioLanguage.JAPANESE, strings.language)
                val texts = nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
                assertTrue(texts.contains(strings.text("settings.languagePending")))
                assertTrue(texts.contains(strings.text("settings.languageHelp")))
            } finally { scene.close() }
        }
    }
}
