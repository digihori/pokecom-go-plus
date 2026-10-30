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
class DebuggerMemorySearchPanelTest {
    @Test fun bothLanguagesValidateSearchNavigateAndRejectStaleResults() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner(byteArrayOf(0x42, 0x43))
            var revision by mutableStateOf(runner.sessionRevision)
            var selected: Int? = null
            val scene = ImageComposeScene(900, 1000) {
                PgpStudioTheme { DebuggerMemorySearchPanel(runner, DesktopKeyboardInput(), revision, { selected = it }, strings) }
            }
            try {
                scene.frames()
                scene.edit(0, "0000"); scene.edit(1, "0001"); scene.edit(2, "42 43")
                scene.frames(); scene.click(strings.text("debug.search.run")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.search.result", "1", "0", "—")))
                scene.click("0x0000")
                assertEquals(0, selected)
                assertEquals(revision, runner.sessionRevision)
                selected = null
                runner.step() // A retained callback must also refuse stale navigation before repaint.
                scene.click("0x0000")
                assertNull(selected)
                revision = runner.sessionRevision
                scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.search.stale")))
                assertFalse(scene.texts().contains("0x0000"))
                scene.edit(0, "FFFF"); scene.frames(); scene.click(strings.text("debug.search.run")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.search.rangeError")))
                scene.edit(0, "0000"); scene.edit(2, "GG"); scene.frames()
                scene.click(strings.text("debug.search.run")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.search.valueError")))
                runner.run(); revision = runner.sessionRevision; scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.search.pause")))
                assertTrue(scene.button(strings.text("debug.search.run")).config.contains(SemanticsProperties.Disabled))
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
