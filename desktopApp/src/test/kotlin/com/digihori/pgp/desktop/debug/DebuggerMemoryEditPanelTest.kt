package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.desktop.MemoryViewPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerMemoryEditPanelTest {
    @Test fun bothLanguagesPreviewCancelApplyAndRefreshMemoryWithoutExecutingCpu() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            val keyboard = DesktopKeyboardInput()
            var revision by mutableStateOf(runner.sessionRevision)
            val tracker = DebuggerComparisonTracker(0xc000)
            var comparison by mutableStateOf(tracker.reset(runner))
            var edits = 0
            val scene = ImageComposeScene(1100, 1400) {
                PgpStudioTheme {
                    Column {
                        DebuggerMemoryEditPanel(runner, keyboard, 0xc000, revision, {
                            edits++; revision = runner.sessionRevision; comparison = tracker.stopped(runner)
                        }, strings)
                        MemoryViewPanel(runner, keyboard, 0xc000, {}, strings, comparison = comparison, memoryRevision = revision)
                    }
                }
            }
            try {
                scene.frames()
                scene.edit(1, "GG"); scene.frames(); scene.click(strings.text("debug.edit.preview")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.edit.error.VALUE")))
                scene.edit(1, "41"); scene.frames(); scene.click(strings.text("debug.edit.preview")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.edit.change", "C000", "C000", "00", "41")))
                assertEquals(0, runner.memoryByte(0xc000))
                scene.click(strings.text("debug.edit.cancel")); scene.frames()
                assertEquals(0, runner.memoryByte(0xc000))
                scene.click(strings.text("debug.edit.preview")); scene.frames()
                val pc = runner.cpuSnapshot().programCounter
                scene.click(strings.text("debug.edit.apply")); scene.frames()
                assertEquals(1, edits)
                assertEquals(0x41, runner.memoryByte(0xc000))
                assertEquals(pc, runner.cpuSnapshot().programCounter)
                assertTrue(scene.texts().any { it.startsWith("C000: 41 00") })
                assertEquals(MemoryValueChange(0xc000, 0, 0x41), comparison.memoryChanges[0xc000])
                scene.edit(0, "F800"); scene.frames(); scene.click(strings.text("debug.edit.preview")); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.edit.error.RAM_ONLY")))
            } finally { scene.close() }
        }
    }

    @Test fun retainedApplyCallbackRejectsChangedSessionAndRunningDisablesPreview() = SwingUtilities.invokeAndWait {
        val strings = StudioStrings(StudioLanguage.ENGLISH)
        val runner = debuggerTestRunner()
        var revision by mutableStateOf(runner.sessionRevision)
        var edits = 0
        var start by mutableStateOf(0xc000)
        val scene = ImageComposeScene(900, 1000) {
            PgpStudioTheme { DebuggerMemoryEditPanel(runner, DesktopKeyboardInput(), start, revision, { edits++ }, strings) }
        }
        try {
            scene.frames(); scene.edit(1, "41"); scene.frames()
            scene.click(strings.text("debug.edit.preview")); scene.frames()
            runner.step()
            scene.click(strings.text("debug.edit.apply")); scene.frames()
            assertEquals(0, edits)
            assertEquals(0, runner.memoryByte(0xc000))
            assertTrue(scene.texts().contains(strings.text("debug.edit.error.STALE")))
            revision = runner.sessionRevision
            scene.click(strings.text("debug.edit.preview")); scene.frames()
            start = 0xc100; scene.frames()
            assertFalse(scene.texts().contains(strings.text("debug.edit.apply")))
            assertEquals(0, runner.memoryByte(0xc100))
            runner.run(); revision = runner.sessionRevision; scene.frames()
            assertTrue(scene.button(strings.text("debug.edit.preview")).config.contains(SemanticsProperties.Disabled))
        } finally { scene.close() }
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
