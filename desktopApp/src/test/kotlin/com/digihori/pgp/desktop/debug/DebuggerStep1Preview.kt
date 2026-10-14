package com.digihori.pgp.desktop.debug

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.*
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities

/** Only synthetic sessions and UI injection; no Studio startup or preferences. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun renderDebuggerStep1Previews(directory: File) {
    check(directory.isDirectory || directory.mkdirs())
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            for ((name, size) in mapOf("wide" to (1200 to 800), "narrow" to (420 to 650),
                "short" to (640 to 400), "memory" to (420 to 650), "fault" to (420 to 650), "running" to (420 to 650))) {
                val strings = StudioStrings(language)
                val runner = if (name == "fault") debuggerTestRunner(byteArrayOf(0x3e)) else
                    debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x5a, 0x52, 0x79, 0, 0))
                runner.setMemoryWatch(0xc000, 0xc000)
                repeat(if (name == "fault") 1 else 3) { runner.step() }
                if (name == "running") runner.run()
                val keyboard = DesktopKeyboardInput()
                val reason = runner.stopReason
                val state = runner.state
                val scene = ImageComposeScene(size.first, size.second) {
                    PgpStudioTheme {
                        Surface(Modifier.fillMaxSize()) {
                            var revision by remember { mutableStateOf(if (name == "memory") 1 else 0) }
                            var memory by remember { mutableStateOf(name == "memory") }
                            var navigation by remember { mutableStateOf<Pair<Int, Int?>?>(null) }
                            var memoryStart by remember { mutableStateOf(0xc000) }
                            DebuggerLayout(revision, memory, {
                                DebuggerControls(true, state, runner.cpuSnapshot(), reason, {}, {}, {},
                                    { memory = false; navigation = ++revision to null },
                                    { memory = false; navigation = ++revision to it },
                                    { memory = true; memoryStart = it; revision++ }, strings)
                            }, {
                                CpuRegisterPanel(runner.cpuSnapshot(), setOf(CpuField.PC), strings)
                                DisassemblyPanel(runner, runner.cpuSnapshot(), keyboard, {}, strings, navigation)
                                InstructionTracePanel(runner, 0, keyboard, strings)
                            }, {
                                MemoryViewPanel(runner, keyboard, memoryStart, { memoryStart = it }, strings)
                                MemoryMapPanel(MachineCatalog.require(runner.machineId).memoryRegions, { memoryStart = it.startAddress }, strings)
                                MemoryWatchPanel(runner, keyboard, strings)
                            }, strings)
                        }
                    }
                }
                try {
                    repeat(4) { scene.render(it * 100_000_000L).close() }
                    scene.render(500_000_000).use { image ->
                        image.encodeToData()?.use { data ->
                            File(directory, "${language.name.lowercase()}-$name.png").writeBytes(data.bytes)
                        } ?: error("PNG encoding failed")
                    }
                } finally { scene.close() }
            }
        }
    }
}
