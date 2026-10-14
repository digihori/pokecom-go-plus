package com.digihori.pgp.desktop.debug

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.digihori.pgp.desktop.CpuRegisterPanel
import com.digihori.pgp.desktop.MemoryViewPanel
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class DebuggerValueHighlightsTest {
    @Test fun individualRegisterValuesShowOldToNewAndDoNotHighlightUnchangedNeighbours() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner(byteArrayOf(0x02, 0x41))
            val previous = runner.cpuSnapshot()
            runner.step()
            val current = runner.cpuSnapshot()
            val scene = ImageComposeScene(900, 350) {
                PgpStudioTheme { CpuRegisterPanel(current, CpuSnapshotDifference.changed(previous, current), strings,
                    previous, DebuggerComparisonBasis.STEP) }
            }
            try {
                render(scene)
                val row = texts(scene).single { it.text.startsWith("I=") }
                val changed = row.text.indexOf("A=41") + 2
                val unchanged = row.text.indexOf("B=00") + 2
                assertTrue(row.spanStyles.any { it.start == changed && it.end == changed + 2 &&
                    it.item.fontWeight == FontWeight.Bold && it.item.textDecoration == TextDecoration.Underline })
                assertFalse(row.spanStyles.any { unchanged in it.start until it.end })
                assertTrue(texts(scene).any { it.text.contains("A: 00 → 41") })
                assertTrue(texts(scene).any { it.text == strings.text("debug.diff.basis.STEP") })
            } finally { scene.close() }
        }
    }

    @Test fun memoryByteAndAsciiShareHighlightAndAccessibleBeforeAfterWithoutChangingHexText() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val line = DesktopMemoryViewModel.build(0xc000, 1) { if (it == 0xc001) 0x41 else 0 }.single()
            val scene = ImageComposeScene(900, 100) {
                PgpStudioTheme { DebuggerMemoryLine(line, mapOf(0xc001 to MemoryValueChange(0xc001, 0, 0x41)), strings) }
            }
            try {
                render(scene)
                val row = texts(scene).single()
                assertEquals("C000: ${line.hexText}  |${line.asciiText}|", row.text)
                assertEquals(2, row.spanStyles.size)
                assertEquals("41", row.text.substring(row.spanStyles[0].start, row.spanStyles[0].end))
                assertEquals("A", row.text.substring(row.spanStyles[1].start, row.spanStyles[1].end))
                assertTrue(nodes(scene).any { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().any { label -> label.contains("C001: 00 → 41") } })
            } finally { scene.close() }
        }
    }

    @Test fun runningAndRebasedPageHaveNoStaleMemoryHighlights() = SwingUtilities.invokeAndWait {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52))
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        repeat(3) { tracker.preparingStep(runner); runner.step(); tracker.stepped(runner) }
        val oldFrame = tracker.frame
        runner.run()
        val cpuScene = ImageComposeScene(900, 300) {
            PgpStudioTheme { CpuRegisterPanel(runner.cpuSnapshot(), emptySet(),
                previousSnapshot = oldFrame.previousCpu, comparisonBasis = DebuggerComparisonBasis.RUNNING) }
        }
        try {
            render(cpuScene)
            assertTrue(texts(cpuScene).filter { it.text.startsWith("PC=") || it.text.startsWith("I=") || it.text.startsWith("K=") }.all { it.spanStyles.isEmpty() })
        } finally { cpuScene.close() }
        val scene = ImageComposeScene(900, 650) {
            PgpStudioTheme { MemoryViewPanel(runner, DesktopKeyboardInput(), 0xc000, {}, comparison = oldFrame) }
        }
        try {
            render(scene)
            assertTrue(texts(scene).filter { it.text.startsWith("C0") }.all { it.spanStyles.isEmpty() })
        } finally { scene.close() }
        runner.pause()
        val selected = tracker.selectMemory(runner, 0xc100)
        val second = ImageComposeScene(900, 650) {
            PgpStudioTheme { MemoryViewPanel(runner, DesktopKeyboardInput(), 0xc100, {}, comparison = selected) }
        }
        try {
            render(second)
            assertTrue(texts(second).filter { it.text.startsWith("C1") }.all { it.spanStyles.isEmpty() })
        } finally { second.close() }
    }

    @Test fun staleAfterValueDoesNotHighlightRefreshedMemory() = SwingUtilities.invokeAndWait {
        val line = DesktopMemoryViewModel.build(0xc000, 1) { 0x42 }.single()
        val scene = ImageComposeScene(900, 100) {
            PgpStudioTheme { DebuggerMemoryLine(line, mapOf(0xc000 to MemoryValueChange(0xc000, 0, 0x41)), StudioStrings(StudioLanguage.ENGLISH)) }
        }
        try { render(scene); assertTrue(texts(scene).single().spanStyles.isEmpty()) } finally { scene.close() }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun render(scene: ImageComposeScene) { repeat(4) { scene.render(it * 100_000_000L).close() } }
@OptIn(ExperimentalComposeUiApi::class)
private fun nodes(scene: ImageComposeScene): List<SemanticsNode> {
    fun descend(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descend)
    return scene.semanticsOwners.flatMap { descend(it.rootSemanticsNode) }
}
@OptIn(ExperimentalComposeUiApi::class)
private fun texts(scene: ImageComposeScene): List<AnnotatedString> = nodes(scene).flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }
