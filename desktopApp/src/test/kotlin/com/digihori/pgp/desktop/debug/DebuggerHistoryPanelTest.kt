package com.digihori.pgp.desktop.debug

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.runner.RunnerState
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

internal fun historyPanelFixture(state: RunnerState = RunnerState.PAUSED) = DebuggerHistorySnapshot(
    state, true,
    listOf(DesktopMemoryAccessHistoryEntry(42, 0x1234, MemoryAccess(MemoryAccessKind.READ, 0xc000, 0x41)),
        DesktopMemoryAccessHistoryEntry(43, 0x2345, MemoryAccess(MemoryAccessKind.WRITE, 0xc001, 0x41))),
    listOf(DesktopBankHistoryEntry(0, BankSwitchEvent(0, 1, 0xf9)), DesktopBankHistoryEntry(1, BankSwitchEvent(1, 2, 2))),
    MachineMemoryBanking.Banked(0x4000, 0x7fff, 4), 2,
)

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerHistoryPanelTest {
    @Test fun bothLanguagesFilterAndNavigateUsingRecordedAddressesWithoutAlteringSnapshot() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val snapshot = historyPanelFixture()
            var instruction: Int? = null
            var memory: Int? = null
            var refreshes = 0
            val scene = ImageComposeScene(900, 1100) {
                PgpStudioTheme { DebuggerHistoryPanel(snapshot, DesktopKeyboardInput(), { instruction = it }, { memory = it }, { refreshes++ }, strings) }
            }
            try {
                scene.frames()
                assertTrue(scene.texts().contains("#43 PC=2345 W:C001=41"))
                assertTrue(scene.texts().contains(strings.text("debug.history.dropped")))
                scene.click(strings.text("debug.history.filters")); scene.frames()
                scene.click(strings.text("debug.history.filter.READ")); scene.frames()
                assertTrue(scene.texts().contains("#42 PC=1234 R:C000=41"))
                assertFalse(scene.texts().contains("#43 PC=2345 W:C001=41"))
                scene.click(strings.text("debug.history.instruction", "1234"))
                scene.click(strings.text("debug.history.memory", "C000"))
                scene.click(strings.text("debug.refresh"))
                assertEquals(0x1234, instruction)
                assertEquals(0xc000, memory)
                assertEquals(1, refreshes)
                scene.edit(0, "C001"); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.history.empty")))
                scene.edit(0, "oops"); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.history.error.START")))
                assertFalse(scene.texts().any { it.startsWith("#42 PC=") })
                assertEquals(2, snapshot.accesses.size)
            } finally { scene.close() }
        }
    }

    @Test fun bankFilterAndCurrentWindowNavigationNeverSelectHistoricalBank() = ui {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val snapshot = historyPanelFixture()
            var address: Int? = null
            val scene = ImageComposeScene(700, 1100) {
                PgpStudioTheme { DebuggerHistoryPanel(snapshot, DesktopKeyboardInput(), {}, { address = it }, {}, strings) }
            }
            try {
                scene.frames(); scene.click(strings.text("debug.history.bank")); scene.frames()
                assertTrue(scene.texts().contains("#1 bank-1 → bank-2"))
                assertTrue(scene.texts().contains(strings.text("debug.history.selectorValue", "F9")))
                scene.click(strings.text("debug.history.filters")); scene.frames()
                scene.edit(0, "1"); scene.frames()
                assertTrue(scene.texts().contains("#0 bank-0 → bank-1"))
                assertTrue(scene.texts().contains("#1 bank-1 → bank-2"))
                scene.click(strings.text("debug.history.currentWindow", "4000"))
                assertEquals(0x4000, address)
                assertEquals(2, snapshot.selectedBank)
                scene.edit(0, "4"); scene.frames()
                assertTrue(scene.texts().contains(strings.text("debug.history.error.BANK", "3")))
                assertFalse(scene.texts().any { it.startsWith("#0 bank-") })
            } finally { scene.close() }
        }
    }

    @Test fun runningDisablesNavigationAndNonBankedMachineShowsUnsupported() = ui {
        val strings = StudioStrings(StudioLanguage.ENGLISH)
        val scene = ImageComposeScene(900, 1100) {
            PgpStudioTheme { DebuggerHistoryPanel(historyPanelFixture(RunnerState.RUNNING).copy(banking = null),
                DesktopKeyboardInput(), { error("instruction") }, { error("memory") }, {}, strings) }
        }
        try {
            scene.frames()
            assertTrue(scene.button(strings.text("debug.history.instruction", "2345")).config.contains(SemanticsProperties.Disabled))
            assertTrue(scene.button(strings.text("debug.history.memory", "C001")).config.contains(SemanticsProperties.Disabled))
            scene.click(strings.text("debug.history.bank")); scene.frames()
            assertTrue(scene.texts().contains(strings.text("debug.history.bankUnsupported")))
        } finally { scene.close() }
    }

    @Test fun historyNavigationLeavesTabInBothLayoutsAndFiltersSurviveReturning() = ui {
        for (width in listOf(700, 1200)) {
            val strings = StudioStrings(StudioLanguage.ENGLISH)
            var revision by mutableStateOf(0)
            var memory by mutableStateOf(false)
            var address by mutableStateOf(0)
            val scene = ImageComposeScene(width, 1100) {
                PgpStudioTheme {
                    DebuggerLayout(revision, memory, { Text("fixed controls") }, { Text("execution") }, { Text("memory $address") }, strings,
                        history = { DebuggerHistoryPanel(historyPanelFixture(), DesktopKeyboardInput(),
                            { memory = false; address = it; revision++ }, { memory = true; address = it; revision++ }, {}, strings) })
                }
            }
            try {
                scene.frames(); scene.click(strings.text("debug.history.title")); scene.frames()
                scene.click(strings.text("debug.history.filters")); scene.frames()
                scene.click(strings.text("debug.history.filter.WRITE")); scene.frames()
                scene.click(strings.text("debug.history.memory", "C001")); scene.frames()
                assertTrue(scene.texts().contains("memory ${0xc001}"))
                assertFalse(scene.texts().contains("#43 PC=2345 W:C001=41"))
                assertTrue(scene.texts().contains("fixed controls"))
                scene.click(strings.text("debug.history.title")); scene.frames()
                assertTrue(scene.texts().contains("#43 PC=2345 W:C001=41"))
                assertFalse(scene.texts().contains("#42 PC=1234 R:C000=41"))
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
