package com.digihori.pgp.desktop.debug

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.desktop.MemoryViewPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

/** Combined panels in a synthetic session: no Studio preferences, ROM library, credentials or native input backend. */
@OptIn(ExperimentalComposeUiApi::class)
class DebuggerReleaseAcceptanceTest {
    @Test fun bothLanguagesAndSizesKeepNavigationInputsAndKeyboardOwnershipConsistent() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(420, 1200)) {
            val strings = StudioStrings(language)
            val runner = bankBreakpointRunner()
            val keyboard = DesktopKeyboardInput().apply { attach(runner) }
            val edit = assertIs<DebuggerMemoryEditResult.Success>(runner.previewMemoryEdit(0xc000, 0x41)).edit
            assertIs<DebuggerMemoryEditResult.Success>(runner.applyMemoryEdit(edit))
            runner.saveBreakpoint(null, 0x4000, bank = 0)
            runner.saveBreakpoint(null, 0x4000, bank = 1)
            runner.saveLogpoint(null, 0, "A", "", true)
            var navigation by mutableIntStateOf(1)
            var memorySelected by mutableStateOf(true)
            var address by mutableIntStateOf(0xc000)
            var revision by mutableLongStateOf(runner.sessionRevision)
            var exports = 0
            val scene = ImageComposeScene(width, if (width == 420) 800 else 900) {
                PgpStudioTheme {
                    Surface {
                        DebuggerLayout(navigation, memorySelected, {
                            DebuggerControls(true, runner.state, runner.cpuSnapshot(), runner.stopReason, {}, {}, {}, {}, {}, {}, strings,
                                onStepOver = {}, onStepOut = {}, canStepOut = runner.canStepOut, stepStatus = runner.debugStepStatus)
                        }, { Text("Execution") }, {
                            MemoryViewPanel(runner, keyboard, address, { address = it }, strings, memoryRevision = revision)
                            DebuggerMemoryEditPanel(runner, keyboard, address, revision, { revision = runner.sessionRevision }, strings)
                            DebuggerMemorySearchPanel(runner, keyboard, revision, {
                                address = it; memorySelected = true; navigation++
                            }, strings)
                        }, strings, history = {
                            DebuggerHistoryExportPanel(true, true, keyboard, null, { exports++ }, strings)
                            DebuggerHistoryPanel(captureDebuggerHistory(runner), keyboard, {}, {}, {}, strings)
                        }, registrations = {
                            DebuggerRegistrationPanel(runner, keyboard, strings = strings)
                            DebuggerLogpointPanel(runner, keyboard, revision, {}, strings)
                        })
                    }
                }
            }
            try {
                scene.frames()
                scene.save(language, width, "memory")
                assertTrue(scene.texts().any { it.startsWith("C000: 41") })
                val headerTop = scene.button(strings.text("emulator.run")).boundsInRoot.top
                // Six fields: Memory View address, editor address/value, search start/end/value.
                scene.edit(3, "C000"); scene.edit(4, "C00F"); scene.edit(5, "41")
                scene.focusField(5); scene.frames()
                assertFalse(keyboard.isEnabled)
                assertTrue(scene.fields()[5].boundsInRoot.height > 20f, "Focused search field must be visible")
                val beforeInput = runner.sessionRevision
                assertFalse(keyboard.handle(Key.A, 'A'.code, KeyEventType.KeyDown))
                assertEquals(beforeInput, runner.sessionRevision)
                scene.click(strings.text("debug.search.run")); scene.frames()
                val scroll = scene.nodes().filter { it.config.contains(SemanticsActions.ScrollBy) }.maxBy { it.boundsInRoot.height }
                assertTrue(scroll.config[SemanticsActions.ScrollBy].action!!.invoke(0f, 10000f))
                scene.frames()
                assertTrue(scene.button("0xC000").boundsInRoot.height > 0f, "Search result must be visible before navigation")
                scene.save(language, width, "search")
                scene.click("0xC000"); scene.frames()
                assertTrue(keyboard.isEnabled, "Navigation must clear text focus before returning to memory")
                assertEquals(headerTop, scene.button(strings.text("emulator.run")).boundsInRoot.top)
                val firstRow = scene.nodes().first { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { t -> t.text.startsWith("C000: 41") } }
                assertTrue(firstRow.boundsInRoot.height > 0f, "Search navigation must bring Memory View into the viewport")
                scene.click(strings.text("debug.registration.title")); scene.frames()
                assertTrue(keyboard.isEnabled, "Disposing the search field must release emulator keyboard ownership")
                assertTrue(scene.texts().contains(strings.text("debug.breakpoint.bankSummary", "1")))
                scene.focusField(3); scene.frames()
                scene.save(language, width, "registrations")
                scene.focusField(6); scene.frames()
                scene.save(language, width, "logpoints")
                assertFalse(keyboard.isEnabled)
                scene.click(strings.text("debug.history.title")); scene.frames()
                assertTrue(keyboard.isEnabled)
                scene.edit(0, "2"); scene.edit(1, "0"); scene.frames()
                scene.click(strings.text("debug.export.save")); scene.frames()
                assertEquals(1, exports)
                scene.save(language, width, "history")
                scene.click(strings.text(if (width < 1000) "debug.registration.memoryTab" else "debug.history.liveTab")); scene.frames()
                assertEquals("C000", scene.fields()[3].config[SemanticsProperties.EditableText].text)
                assertEquals("C00F", scene.fields()[4].config[SemanticsProperties.EditableText].text)
                assertEquals("41", scene.fields()[5].config[SemanticsProperties.EditableText].text)
                assertEquals(0, runner.cpuSnapshot().programCounter)
                assertEquals(0x41, runner.memoryByte(0xc000))
                assertEquals(beforeInput, runner.sessionRevision)
            } finally { scene.close() }
            assertTrue(keyboard.isEnabled)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.frames() { repeat(8) { acceptanceTime += 200_000_000L; render(acceptanceTime).close() } }
private var acceptanceTime = 0L
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.nodes(): List<SemanticsNode> {
    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
    return semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
}
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.fields() = nodes().filter { it.config.contains(SemanticsActions.SetText) }
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
    assertTrue(fields()[index].config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)))
}
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.focusField(index: Int) { assertTrue(fields()[index].config[SemanticsActions.RequestFocus].action!!.invoke()) }
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.save(language: StudioLanguage, width: Int, scenario: String) {
    val file = File("build/reports/debugger-release/${language.name.lowercase()}-$width-$scenario.png")
    file.parentFile.mkdirs()
    acceptanceTime += 200_000_000L
    render(acceptanceTime).use { bitmap -> bitmap.encodeToData()!!.use { file.writeBytes(it.bytes) } }
}
