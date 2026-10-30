package com.digihori.pgp.desktop.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.desktop.DisassemblyPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerRegistrationPanelTest {
    @Test fun bankScopedRegistrationsCanBeEditedAndClearedInBothLanguages() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = bankBreakpointRunner()
            val scene = ImageComposeScene(1100, 1500) {
                PgpStudioTheme { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings) }
            }
            try {
                scene.frames(); scene.edit(0, "4000"); scene.edit(3, "0")
                scene.click(strings.text("debug.registration.add")); scene.frames()
                scene.edit(3, "1"); scene.click(strings.text("debug.registration.add")); scene.frames()
                assertEquals(listOf(0, 1), runner.breakpointRegistrations().map { it.bank })
                assertTrue(scene.texts().contains(strings.text("debug.breakpoint.bankSummary", "1")))
                scene.click(strings.text("debug.registration.editBreakpoint", "4000 bank=0")); scene.frames()
                scene.edit(3, "8"); scene.click(strings.text("common.apply")); scene.frames()
                assertTrue(scene.texts().contains(DebuggerRegistrationError.BANK.display(strings)))
                assertEquals(listOf(0, 1), runner.breakpointRegistrations().map { it.bank })
                scene.edit(3, ""); scene.click(strings.text("common.apply")); scene.frames()
                assertTrue(scene.texts().contains(DebuggerRegistrationError.DUPLICATE_ADDRESS.display(strings)))
                scene.click(strings.text("debug.registration.deleteBreakpoint", "4000 bank=1")); scene.frames()
                scene.click(strings.text("common.apply")); scene.frames()
                assertNull(runner.breakpointRegistrations().single().bank)
                assertEquals(0, runner.cpuSnapshot().programCounter)
            } finally { scene.close() }
        }
    }

    @Test fun restoredSettingsDiscardOldEditIdsAndSynchronizeTraceControls() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val loaded = assertIs<com.digihori.pgp.desktop.rom.DesktopRomLoadResult.Success>(
                com.digihori.pgp.desktop.rom.DesktopRomLoader.loadPc1245LegacyImage(ByteArray(0x10000)))
            val runner = com.digihori.pgp.desktop.runner.DesktopEmulatorRunner(loaded.session, debugRomFingerprint = loaded.romFingerprint)
            runner.saveBreakpoint(null, 0xc000)
            val originalId = runner.breakpointRegistrations().single().id
            val saved = DebuggerSettingsFiles.capture(runner, null).copy(traceEnabled = true, traceCapacity = 16)
            var refresh by mutableStateOf(0)
            val scene = ImageComposeScene(1100, 1300) {
                refresh
                PgpStudioTheme {
                    androidx.compose.foundation.layout.Column {
                        DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings)
                        com.digihori.pgp.desktop.InstructionTracePanel(runner, 0, DesktopKeyboardInput(), strings, runner.debugSettingsRevision)
                    }
                }
            }
            try {
                scene.frames()
                scene.click(strings.text("debug.registration.editBreakpoint", "C000")); scene.frames()
                scene.edit(0, "FFFF") // Unapplied draft must not survive replacement of the old registration ID.
                DebuggerSettingsFiles.restore(runner, null, saved)
                refresh++; scene.frames()
                assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text("debug.registration.add") } })
                assertTrue(runner.breakpointRegistrations().single().id > originalId)
                assertEquals(0xc000, runner.breakpointRegistrations().single().address)
                assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "16" })
                assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text("debug.disable") } })
                assertFalse(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "FFFF" })
            } finally { scene.close() }
        }
    }

    @Test fun conditionalRulesEditClearAndRejectInvalidInputInBothLanguages() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            val scene = ImageComposeScene(900, 1100) {
                PgpStudioTheme { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings) }
            }
            try {
                scene.frames()
                scene.edit(0, "C000"); scene.edit(1, "A == 0x41"); scene.edit(2, "% 3")
                scene.click(strings.text("debug.registration.add")); scene.frames()
                val original = runner.breakpointRegistrations().single()
                assertEquals("A == 0x41", original.condition)
                assertEquals("% 3", original.hitCondition)
                scene.click(strings.text("debug.registration.toggleBreakpoint", "C000")); scene.frames()
                assertEquals(original.copy(enabled = false), runner.breakpointRegistrations().single())
                scene.click(strings.text("debug.registration.editBreakpoint", "C000")); scene.frames()
                assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "% 3" })
                scene.edit(1, "A == 256"); scene.click(strings.text("common.apply")); scene.frames()
                assertTrue(DebuggerRegistrationError.CONDITION.display(strings) in scene.texts())
                assertEquals(original.copy(enabled = false), runner.breakpointRegistrations().single())
                scene.edit(1, ""); scene.edit(2, "% 0"); scene.click(strings.text("common.apply")); scene.frames()
                assertTrue(DebuggerRegistrationError.HIT_CONDITION.display(strings) in scene.texts())
                scene.edit(2, ""); scene.click(strings.text("common.apply")); scene.frames()
                assertEquals("", runner.breakpointRegistrations().single().condition)
                assertEquals("", runner.breakpointRegistrations().single().hitCondition)
                assertEquals(original.id, runner.breakpointRegistrations().single().id)
            } finally { scene.close() }
        }
    }

    @Test fun breakpointAddEditToggleDeleteAndDuplicateErrorWorkInBothLanguages() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            val scene = ImageComposeScene(900, 1000) {
                PgpStudioTheme { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings) }
            }
            try {
                scene.frames(); scene.edit(0, "C000"); scene.click(strings.text("debug.registration.add")); scene.frames()
                val id = runner.breakpointRegistrations().single().id
                scene.click(strings.text("debug.registration.add")); scene.frames()
                assertTrue(scene.texts().contains(DebuggerRegistrationError.DUPLICATE_ADDRESS.display(strings)))
                assertEquals(1, runner.breakpointRegistrations().size)
                scene.click(strings.text("debug.registration.editBreakpoint", "C000")); scene.frames()
                scene.edit(0, "C001"); scene.click(strings.text("common.apply")); scene.frames()
                assertEquals(id, runner.breakpointRegistrations().single().id)
                assertEquals(0xc001, runner.breakpointRegistrations().single().address)
                scene.click(strings.text("debug.registration.toggleBreakpoint", "C001")); scene.frames()
                assertFalse(runner.breakpointRegistrations().single().enabled)
                scene.click(strings.text("debug.registration.deleteBreakpoint", "C001")); scene.frames()
                assertTrue(runner.breakpointRegistrations().isEmpty())
                assertEquals(0, runner.cpuSnapshot().programCounter)
            } finally { scene.close() }
        }
    }

    @Test fun multipleWatchesAddEditModeToggleAndDeleteInBothLanguages() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            val scene = ImageComposeScene(900, 1100) {
                PgpStudioTheme { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings, initialWatch = true) }
            }
            try {
                scene.frames(); scene.edit(0, "C000"); scene.edit(1, "C000")
                scene.click(strings.text("debug.registration.add")); scene.frames()
                val first = runner.watchRegistrations().single().id
                scene.edit(0, "C001"); scene.edit(1, "C001"); scene.click(strings.text("debug.registration.add")); scene.frames()
                assertEquals(2, runner.watchRegistrations().size)
                scene.click(strings.text("debug.registration.editWatch", first.toString())); scene.frames()
                scene.click(strings.text("debug.registration.kind.READ_WRITE"))
                scene.click(strings.text("common.apply")); scene.frames()
                assertEquals(DebuggerWatchKind.READ_WRITE, runner.watchRegistrations().first().kind)
                scene.click(strings.text("debug.registration.toggleWatch", first.toString())); scene.frames()
                assertFalse(runner.watchRegistrations().first().enabled)
                scene.click(strings.text("debug.registration.deleteWatch", first.toString())); scene.frames()
                assertEquals(1, runner.watchRegistrations().size)
                scene.edit(0, "C002"); scene.edit(1, "C001"); scene.click(strings.text("debug.registration.add")); scene.frames()
                assertTrue(scene.texts().contains(DebuggerRegistrationError.RANGE.display(strings)))
                assertEquals(1, runner.watchRegistrations().size)
            } finally { scene.close() }
        }
    }

    @Test fun runningDisablesManagementAndDisassemblyReenablesSameDisabledRegistration() = ui {
        val strings = StudioStrings(StudioLanguage.ENGLISH)
        val runner = debuggerTestRunner()
        val id = assertIs<DebuggerRegistrationResult.Success>(runner.saveBreakpoint(null, 0, false)).id
        runner.run()
        val scene = ImageComposeScene(900, 650) {
            PgpStudioTheme { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings) }
        }
        try {
            scene.frames()
            assertTrue(scene.button(strings.text("debug.registration.add")).config.contains(SemanticsProperties.Disabled))
            assertTrue(scene.button(strings.text("debug.registration.toggleBreakpoint", "0000")).config.contains(SemanticsProperties.Disabled))
        } finally { scene.close() }
        runner.pause()
        val disassembly = ImageComposeScene(900, 650) {
            PgpStudioTheme { DisassemblyPanel(runner, runner.cpuSnapshot(), DesktopKeyboardInput(), {}, strings) }
        }
        try {
            disassembly.frames(); disassembly.click(strings.text("debug.registration.enableBreakpoint", "0000")); disassembly.frames()
            assertEquals(id, runner.breakpointRegistrations().single().id)
            assertTrue(runner.breakpointRegistrations().single().enabled)
        } finally { disassembly.close() }
    }

    @Test fun editingDraftSurvivesTabsInBothLayoutsAndAllErrorCodesTranslate() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            for (error in DebuggerRegistrationError.entries) assertTrue(error.display(strings).isNotBlank())
        }
        for (width in listOf(700, 1200)) {
            val strings = StudioStrings(StudioLanguage.ENGLISH)
            val runner = debuggerTestRunner()
            val scene = ImageComposeScene(width, 1100) {
                PgpStudioTheme {
                    DebuggerLayout(0, false, { Text("controls") }, { Text("execution") }, { Text("memory") }, strings,
                        history = { Text("history") }, registrations = { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings) })
                }
            }
            try {
                scene.frames(); scene.click(strings.text("debug.registration.title")); scene.frames()
                scene.edit(0, "CAFE")
                scene.click(strings.text("debug.history.title")); scene.frames()
                scene.click(strings.text("debug.registration.title")); scene.frames()
                assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "CAFE" })
            } finally { scene.close() }
        }
    }
}

private fun ui(action: () -> Unit) = SwingUtilities.invokeAndWait(action)
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
    it.config.contains(SemanticsActions.OnClick) && (it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == label } ||
        it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().contains(label))
}
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.click(label: String) { assertTrue(button(label).config[SemanticsActions.OnClick].action!!.invoke()) }
@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.edit(index: Int, value: String) {
    assertTrue(nodes().filter { it.config.contains(SemanticsActions.SetText) }[index].config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)))
}
