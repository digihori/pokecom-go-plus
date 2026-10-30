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
class DebuggerLogpointPanelTest {
    @Test fun bothLanguagesRegisterEditToggleNavigateAndClearWithoutExecutingOnUiActions() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            var revision by mutableStateOf(runner.sessionRevision)
            var selected: Int? = null
            val scene = ImageComposeScene(1100, 1400) {
                PgpStudioTheme { DebuggerLogpointPanel(runner, DesktopKeyboardInput(), revision, { selected = it }, strings) }
            }
            try {
                scene.frames()
                scene.click(strings.text("debug.log.add")); scene.frames()
                assertEquals(1, runner.logpointRegistrations().size)
                assertTrue(runner.logpointEntries().isEmpty())
                scene.click(strings.text("debug.log.add")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.log.error.DUPLICATE")))
                scene.click(strings.text("debug.log.toggle", "0")); scene.frames()
                assertFalse(runner.logpointRegistrations().single().enabled)
                runner.step(); revision = runner.sessionRevision; scene.frames()
                assertTrue(runner.logpointEntries().isEmpty())
                scene.click(strings.text("debug.log.edit", "0")); scene.frames()
                scene.edit(1, "PC"); scene.edit(2, "A = 0"); scene.frames()
                scene.click(strings.text("debug.log.apply")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.log.error.CONDITION")))
                assertEquals("A", runner.logpointRegistrations().single().value)
                scene.edit(2, ""); scene.frames(); scene.click(strings.text("debug.log.apply")); scene.frames()
                assertEquals("PC", runner.logpointRegistrations().single().value)
                scene.click(strings.text("debug.log.toggle", "0")); scene.frames()
                runner.reset(); runner.step(); revision = runner.sessionRevision; scene.frames()
                assertTrue(scene.texts().any { it.contains("PC=0000 PC=0x0000") })
                val pc = runner.cpuSnapshot().programCounter
                val beforeRevision = runner.sessionRevision
                scene.click(strings.text("debug.log.location", "0000"))
                assertEquals(0, selected)
                assertEquals(pc, runner.cpuSnapshot().programCounter)
                assertEquals(beforeRevision, runner.sessionRevision)
                scene.click(strings.text("debug.log.clear")); scene.frames()
                assertTrue(runner.logpointEntries().isEmpty())
                assertEquals(1, runner.logpointRegistrations().size)
                scene.click(strings.text("debug.log.remove", "0")); scene.frames()
                assertTrue(runner.logpointRegistrations().isEmpty())
                runner.run(); revision = runner.sessionRevision; scene.frames()
                assertTrue(scene.button(strings.text("debug.log.add")).config.contains(SemanticsProperties.Disabled))
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
