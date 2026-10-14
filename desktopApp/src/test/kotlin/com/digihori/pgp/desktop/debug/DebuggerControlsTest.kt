package com.digihori.pgp.desktop.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.DisassemblyPanel
import com.digihori.pgp.desktop.MemoryViewPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.runner.RunnerState
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerControlsTest {
    @Test fun stopLocationsDistinguishInstructionFromAccessAndDoNotGuessForPause() {
        assertEquals(DebuggerStopLocation(0x1234), DebuggerStopReason.Breakpoint(0x1234).location())
        assertEquals(DebuggerStopLocation(0x1234), DebuggerStopReason.RunToAddress(0x1234).location())
        assertEquals(DebuggerStopLocation(0x1234, 0xc010), DebuggerStopReason.MemoryChanged(0x1234,
            listOf(MemoryValueChange(0xc010, 1, 2))).location())
        assertEquals(DebuggerStopLocation(0x1234, 0xc020), DebuggerStopReason.MemoryAccessed(0x1234,
            listOf(MemoryAccess(MemoryAccessKind.READ, 0xc020, 2))).location())
        assertEquals(DebuggerStopLocation(0x1234), DebuggerStopReason.Fault(CoreFault.UnsupportedOpcode(0x1234, 0)).location())
        for (reason in listOf(DebuggerStopReason.Reset, DebuggerStopReason.UserPause, DebuggerStopReason.StepComplete)) {
            assertNull(reason.location())
        }
    }

    @Test fun controlsUseRunnerCallbacksAndNavigationDoesNotExecuteInEitherLanguage() = onUiThread {
        for (language in StudioLanguage.entries) {
            val runner = debuggerTestRunner()
            val strings = StudioStrings(language)
            var refresh by mutableStateOf(0)
            var instruction: Int? = null
            var memory: Int? = null
            val reason = DebuggerStopReason.MemoryAccessed(0x1234, listOf(MemoryAccess(MemoryAccessKind.WRITE, 0xc010, 0xff)))
            val scene = ImageComposeScene(700, 400) {
                refresh
                PgpStudioTheme {
                    DebuggerControls(true, runner.state, runner.cpuSnapshot(), reason,
                        { runner.run(); refresh++ }, { runner.pause(); refresh++ }, { runner.step(); refresh++ },
                        { instruction = -1 }, { instruction = it }, { memory = it }, strings)
                }
            }
            try {
                scene.frames()
                assertTrue(scene.button(strings.text("emulator.pause")).config.contains(SemanticsProperties.Disabled))
                val revision = runner.sessionRevision
                scene.click(strings.text("debug.showStopInstruction", "1234"))
                scene.click(strings.text("debug.showStopMemory", "C010"))
                assertEquals(0x1234, instruction)
                assertEquals(0xc010, memory)
                assertEquals(revision, runner.sessionRevision)
                scene.click(strings.text("emulator.run")); scene.frames()
                assertEquals(RunnerState.RUNNING, runner.state)
                assertTrue(scene.button(strings.text("emulator.step")).config.contains(SemanticsProperties.Disabled))
                scene.click(strings.text("emulator.pause")); scene.frames()
                assertEquals(RunnerState.PAUSED, runner.state)
                val pc = runner.cpuSnapshot().programCounter
                scene.click(strings.text("emulator.step")); scene.frames()
                assertEquals((pc + 2) and 0xffff, runner.cpuSnapshot().programCounter)
                assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
            } finally { scene.close() }
        }
    }

    @Test fun faultAndMissingSessionDisableExecution() = onUiThread {
        for (hasSession in listOf(false, true)) {
            val strings = StudioStrings(StudioLanguage.ENGLISH)
            val scene = ImageComposeScene(700, 400) {
                PgpStudioTheme {
                    DebuggerControls(hasSession, RunnerState.FAULTED, null, null,
                        { error("run") }, { error("pause") }, { error("step") }, {}, {}, {}, strings)
                }
            }
            try {
                scene.frames()
                for (key in listOf("emulator.run", "emulator.pause", "emulator.step", "debug.followPc")) {
                    assertTrue(scene.button(strings.text(key)).config.contains(SemanticsProperties.Disabled))
                }
            } finally { scene.close() }
        }
    }

    @Test fun navigationSelectsMemoryInNarrowWindowAndWideWindowShowsBothPanes() = onUiThread {
        for (width in listOf(420, 1200)) {
            val strings = StudioStrings(StudioLanguage.ENGLISH)
            var revision by mutableStateOf(0)
            var memory by mutableStateOf(false)
            val scene = ImageComposeScene(width, 650) {
                PgpStudioTheme {
                    DebuggerLayout(revision, memory, { Text("fixed controls") },
                        { Text("execution pane") }, { Text("memory pane") }, strings)
                }
            }
            try {
                scene.frames()
                assertTrue("execution pane" in scene.texts())
                assertEquals(width == 1200, "memory pane" in scene.texts())
                memory = true; revision++; scene.frames()
                assertTrue("memory pane" in scene.texts())
                assertTrue("fixed controls" in scene.texts())
                memory = false; revision++; scene.frames()
                assertTrue("execution pane" in scene.texts())
            } finally { scene.close() }
        }
    }

    @Test fun customDisassemblyDoesNotMarkFirstRowAsCurrentPcAndFollowRestoresIt() = onUiThread {
        val runner = debuggerTestRunner()
        var navigation by mutableStateOf<Pair<Int, Int?>?>(1 to 0x0100)
        val scene = ImageComposeScene(900, 650) {
            PgpStudioTheme {
                DisassemblyPanel(runner, runner.cpuSnapshot(), DesktopKeyboardInput(), {}, navigation = navigation)
            }
        }
        try {
            scene.frames()
            assertTrue(scene.texts().any { it.startsWith(" 0100") })
            assertFalse(scene.texts().any { it.startsWith(">0100") })
            navigation = 2 to null; scene.frames()
            assertTrue(scene.texts().any { it.startsWith(">0000") })
        } finally { scene.close() }
    }

    @Test fun paneSwitchRetainsManualFollowChoiceAndUnsubmittedAddress() = onUiThread {
        val runner = debuggerTestRunner()
        val strings = StudioStrings(StudioLanguage.ENGLISH)
        val scene = ImageComposeScene(700, 650) {
            PgpStudioTheme {
                DebuggerLayout(0, false, { Text("controls") }, {
                    DisassemblyPanel(runner, runner.cpuSnapshot(), DesktopKeyboardInput(), {}, strings, 1 to 0x0100)
                }, { Text("memory pane") }, strings)
            }
        }
        try {
            scene.frames()
            scene.click(strings.text("debug.followPc")); scene.frames()
            val input = scene.nodes().first { it.config.contains(SemanticsActions.SetText) }
            assertTrue(input.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("CAFE")))
            scene.click(strings.text("debug.memoryTab")); scene.frames()
            scene.click(strings.text("debug.executionTab")); scene.frames()
            assertTrue(scene.texts().any { it.startsWith(">0000") })
            assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "CAFE" })
        } finally { scene.close() }
    }

    @Test fun memoryRefreshesAtSameAddressWhenStoppedSnapshotChanges() = onUiThread {
        val runner = debuggerTestRunner()
        var snapshot by mutableStateOf(runner.cpuSnapshot())
        val scene = ImageComposeScene(900, 650) {
            PgpStudioTheme {
                MemoryViewPanel(runner, DesktopKeyboardInput(), 0xc000, {}, snapshotRevision = snapshot)
            }
        }
        try {
            scene.frames()
            assertTrue(scene.texts().any { it.startsWith("C000: 00") })
            val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
                listOf(AddressedMemorySegmentData(0xc000, byteArrayOf(0x5a))))).image
            assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
            snapshot = runner.cpuSnapshot(); scene.frames()
            assertTrue(scene.texts().any { it.startsWith("C000: 5A") })
        } finally { scene.close() }
    }

    @Test fun memoryPaneRetainsUnsubmittedAddressAcrossTabSwitch() = onUiThread {
        val runner = debuggerTestRunner()
        val strings = StudioStrings(StudioLanguage.ENGLISH)
        val scene = ImageComposeScene(700, 650) {
            PgpStudioTheme {
                DebuggerLayout(1, true, { Text("controls") }, { Text("execution") }, {
                    MemoryViewPanel(runner, DesktopKeyboardInput(), 0xc000, {}, strings)
                }, strings)
            }
        }
        try {
            scene.frames()
            val input = scene.nodes().first { it.config.contains(SemanticsActions.SetText) }
            assertTrue(input.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("C0F0")))
            scene.click(strings.text("debug.executionTab")); scene.frames()
            scene.click(strings.text("debug.memoryTab")); scene.frames()
            assertTrue(scene.nodes().any { it.config.getOrNull(SemanticsProperties.EditableText)?.text == "C0F0" })
        } finally { scene.close() }
    }

    @Test fun realCpuReadAndWriteMonitoringStopAfterInstructionAndResumeWithObservation() {
        for (kind in MemoryAccessKind.entries) {
            // LIDP C000; LDD / (LIA 5A; STD); JP 0000. Entirely synthetic ROM.
            val program = if (kind == MemoryAccessKind.READ) byteArrayOf(0x10, 0xc0.toByte(), 0, 0x57, 0x79, 0, 0)
                else byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x5a, 0x52, 0x79, 0, 0)
            val runner = debuggerTestRunner(program)
            runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
            runner.setDebugObservationEnabled(true)
            runner.setMemoryAccessWatch(0xc000, 0xc000, kind == MemoryAccessKind.READ, kind == MemoryAccessKind.WRITE)
            runner.run(); runner.tick()
            val reason = assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
            assertEquals(RunnerState.PAUSED, runner.state)
            assertEquals(if (kind == MemoryAccessKind.READ) 3 else 5, reason.instructionAddress)
            assertEquals(reason.instructionAddress + 1, runner.cpuSnapshot().programCounter)
            assertEquals(kind, reason.accesses.single().kind)
            assertEquals(0xc000, reason.location()?.memoryAddress)
            assertEquals(1, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
            runner.run(); runner.tick()
            assertEquals(RunnerState.PAUSED, runner.state)
            assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
            assertEquals(2, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
        }
    }
}

private fun onUiThread(action: () -> Unit) = SwingUtilities.invokeAndWait(action)

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.frames() {
    repeat(4) { render(System.nanoTime()).close() }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.nodes(): List<SemanticsNode> {
    fun descend(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descend)
    return semanticsOwners.flatMap { descend(it.rootSemanticsNode) }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.texts(): List<String> = nodes().flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text }

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.button(label: String): SemanticsNode = nodes().first {
    it.config.contains(SemanticsActions.OnClick) && it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == label }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun ImageComposeScene.click(label: String) { assertTrue(button(label).config[SemanticsActions.OnClick].action!!.invoke()) }
