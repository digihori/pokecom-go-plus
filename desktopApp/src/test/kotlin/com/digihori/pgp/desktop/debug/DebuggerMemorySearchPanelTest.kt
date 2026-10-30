package com.digihori.pgp.desktop.debug

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.source.machine.*
import androidx.compose.foundation.layout.Column
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerMemorySearchPanelTest {
    @Test fun hostPauseAndCpuRefreshUpdateNestedPanelsWithoutEditingSearchText() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner(byteArrayOf(0x42, 0x43))
            val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(listOf(
                AddressedMemorySegmentData(0xc100, byteArrayOf(0xde.toByte(), 0xad.toByte(), 0xbe.toByte(), 0xef.toByte(), 0x34, 0x12, 0x12, 0x34))
            ))).image
            assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
            runner.run()
            var hostCpu by mutableStateOf(runner.cpuSnapshot())
            var hostState by mutableStateOf(runner.state)
            var selected: Int? = null
            val keyboard = DesktopKeyboardInput()
            val scene = ImageComposeScene(900, 1000) {
                // Match Main: an observable host snapshot must invalidate nested section content.
                val revision = rememberDebuggerSessionRevision(runner, hostCpu, hostState)
                PgpStudioTheme { SearchSection {
                    DebuggerMemorySearchPanel(runner, keyboard, revision, { selected = it }, strings)
                } }
            }
            try {
                scene.frames()
                scene.edit(0, "C100"); scene.edit(1, "C107"); scene.edit(2, "DE AD BE EF"); scene.frames()
                assertTrue(scene.button(strings.text("debug.search.run")).config.contains(SemanticsProperties.Disabled))
                runner.pause()
                hostState = runner.state // No field edit and no CPU refresh required to enable Search.
                scene.frames()
                assertFalse(scene.button(strings.text("debug.search.run")).config.contains(SemanticsProperties.Disabled))
                scene.click(strings.text("debug.search.run")); scene.frames()
                assertTrue("0xC100" in scene.texts())
                assertFalse(strings.text("debug.search.stale") in scene.texts())
                scene.click("0xC100"); assertEquals(0xc100, selected)
                runner.step(); hostCpu = runner.cpuSnapshot(); scene.frames()
                assertTrue(strings.text("debug.search.stale") in scene.texts())
                scene.click(strings.text("debug.search.run")); scene.frames()
                assertTrue("0xC100" in scene.texts())
                for ((format, start, end) in listOf(
                    Triple(MemorySearchFormat.U16_LE, "C104", "C105"),
                    Triple(MemorySearchFormat.U16_BE, "C106", "C107"),
                )) {
                    scene.edit(0, start); scene.edit(1, end); scene.edit(2, "4660"); scene.frames()
                    scene.click(strings.text("debug.search.format.$format")); scene.frames()
                    scene.click(strings.text("debug.search.run")); scene.frames()
                    assertTrue("0x$start" in scene.texts())
                    assertFalse(strings.text("debug.search.stale") in scene.texts())
                }
            } finally { scene.close() }
        }
    }

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

@Composable
private fun SearchSection(content: @Composable () -> Unit) { Column { content() } }

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
