package com.digihori.pgp.desktop.debug

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerHistoryExportPanelTest {
    @Test fun bothLanguagesValidateLimitsAndDoNotExportWhenUnavailableOrRunning() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            var available by mutableStateOf(true)
            var stopped by mutableStateOf(true)
            var captured: DebuggerExportLimits? = null
            val scene = ImageComposeScene(1000, 800) {
                PgpStudioTheme { DebuggerHistoryExportPanel(available, stopped, DesktopKeyboardInput(), null, { captured = it }, strings) }
            }
            try {
                scene.frames(); scene.edit(0, "4097"); scene.frames()
                scene.click(strings.text("debug.export.save")); scene.frames()
                assertNull(captured)
                assertTrue(scene.texts().contains(strings.text("debug.export.error.LIMIT")))
                scene.edit(0, "2"); scene.edit(1, "0"); scene.edit(2, "1"); scene.edit(3, "3"); scene.frames()
                scene.click(strings.text("debug.export.save")); scene.frames()
                assertEquals(DebuggerExportLimits(2, 0, 1, 3), captured)
                stopped = false; scene.frames()
                assertTrue(scene.button(strings.text("debug.export.save")).config.contains(SemanticsProperties.Disabled))
                assertTrue(scene.texts().contains(strings.text("debug.export.error.STATE")))
                stopped = true; available = false; scene.frames()
                assertTrue(scene.button(strings.text("debug.export.save")).config.contains(SemanticsProperties.Disabled))
            } finally { scene.close() }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.frames() { repeat(4) { render(System.nanoTime()).close() } }
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.nodes(): List<SemanticsNode> {
    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
    return semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
}
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.texts() = nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.button(label: String) = nodes().first {
    it.config.contains(SemanticsActions.OnClick) && it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == label }
}
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.click(label: String) { assertTrue(button(label).config[SemanticsActions.OnClick].action!!.invoke()) }
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.edit(index: Int, value: String) {
    assertTrue(nodes().filter { it.config.contains(SemanticsActions.SetText) }[index].config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)))
}
