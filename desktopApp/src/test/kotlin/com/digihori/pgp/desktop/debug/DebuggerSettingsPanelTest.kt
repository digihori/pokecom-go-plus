package com.digihori.pgp.desktop.debug

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerSettingsPanelTest {
    @Test fun explicitSaveRestoreButtonsScopeAndPauseStatesWorkInBothLanguages() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            var paused by mutableStateOf(true)
            var available by mutableStateOf(true)
            var saved = 0; var restored = 0
            val scene = ImageComposeScene(900, 500) {
                PgpStudioTheme {
                    DebuggerSettingsPanel(available, paused, strings.text("debug.settings.scope", "pc-1245", "Test"), null,
                        { saved++ }, { restored++ }, strings)
                }
            }
            try {
                fun render() { repeat(4) { scene.render(System.nanoTime()).close() } }
                fun nodes(): List<SemanticsNode> {
                    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
                    return scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
                }
                fun button(key: String) = nodes().first { it.config.contains(SemanticsActions.OnClick) &&
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text(key) } }
                render()
                assertTrue(button("debug.settings.save").config[SemanticsActions.OnClick].action!!.invoke())
                assertTrue(button("debug.settings.restore").config[SemanticsActions.OnClick].action!!.invoke())
                assertEquals(1, saved); assertEquals(1, restored)
                paused = false; render()
                assertTrue(button("debug.settings.save").config.contains(SemanticsProperties.Disabled))
                assertTrue(button("debug.settings.restore").config.contains(SemanticsProperties.Disabled))
                paused = true; available = false; render()
                assertTrue(button("debug.settings.save").config.contains(SemanticsProperties.Disabled))
                assertTrue(button("debug.settings.restore").config.contains(SemanticsProperties.Disabled))
                for (error in DebuggerSettingsError.entries) assertTrue(strings.text("debug.settings.error.${error.name}").isNotBlank())
            } finally { scene.close() }
        }
    }
}
