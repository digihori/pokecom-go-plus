package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.*
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities

/** Independent rendering with generated ROM bytes; never invokes Studio main. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun renderDebuggerStep2Previews(directory: File) {
    check(directory.isDirectory || directory.mkdirs())
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (scenario in listOf("step", "stop", "bank", "reset", "running")) {
            val strings = StudioStrings(language)
            val program = byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52, 0x03, 0x42, 0x02, 0x5a, 0x52)
            val runner = if (scenario == "bank") bankRunner("pc-1360", 8, byteArrayOf(0x10, 0x34, 0, 0x02, 1, 0x52)) else debuggerTestRunner(program)
            val address = if (scenario == "bank") 0x4000 else 0xc000
            val tracker = DebuggerComparisonTracker(address)
            tracker.reset(runner)
            if (scenario == "stop") {
                runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
                tracker.running(runner); runner.runToAddress(11); runner.tick(); tracker.stopped(runner)
            } else if (scenario == "bank") {
                repeat(3) { tracker.preparingStep(runner); runner.step(); tracker.stepped(runner) }
            } else {
                repeat(6) { tracker.preparingStep(runner); runner.step(); tracker.stepped(runner) }
                if (scenario == "reset") { runner.reset(); tracker.reset(runner) }
                if (scenario == "running") { tracker.running(runner); runner.run() }
            }
            val frame = tracker.frame
            val cpu = runner.cpuSnapshot()
            for ((name, width, height, memorySelected) in listOf(
                PreviewSize("wide", 1200, 850, false), PreviewSize("cpu", 420, 700, false), PreviewSize("memory", 420, 700, true))) {
                val keyboard = DesktopKeyboardInput()
                val scene = ImageComposeScene(width, height) {
                    PgpStudioTheme {
                        Surface(Modifier.fillMaxSize()) {
                            DebuggerLayout(if (memorySelected) 1 else 0, memorySelected, {
                                DebuggerControls(true, runner.state, cpu, runner.stopReason, {}, {}, {}, {}, {}, {}, strings)
                            }, {
                                CpuRegisterPanel(cpu, CpuSnapshotDifference.changed(frame.previousCpu, cpu), strings, frame.previousCpu, frame.cpuBasis)
                                DisassemblyPanel(runner, cpu, keyboard, {}, strings)
                            }, {
                                MemoryViewPanel(runner, keyboard, address, {}, strings, cpu, comparison = frame)
                            }, strings)
                        }
                    }
                }
                try {
                    repeat(4) { scene.render(it * 100_000_000L).close() }
                    scene.render(500_000_000).use { image ->
                        image.encodeToData()?.use { data ->
                            File(directory, "${language.name.lowercase()}-$scenario-$name.png").writeBytes(data.bytes)
                        } ?: error("PNG encoding failed")
                    }
                } finally { scene.close() }
            }
        }
    }
}

private data class PreviewSize(val name: String, val width: Int, val height: Int, val memorySelected: Boolean)
