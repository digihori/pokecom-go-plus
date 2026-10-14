package com.digihori.pgp.desktop.debug

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.input.DesktopKeyboardInput
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.runner.RunnerState
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities

/** Synthetic session only; never starts Studio, a ROM library, preferences or native input. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun renderDebuggerStep4Previews(directory: File) {
    check(directory.isDirectory || directory.mkdirs())
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (scenario in listOf("breakpoints", "watches", "running", "empty")) {
            val strings = StudioStrings(language)
            val runner = debuggerTestRunner()
            if (scenario != "empty") {
                runner.saveBreakpoint(null, 0xc000)
                runner.saveBreakpoint(null, 0xc010, false)
                runner.saveWatch(null, 0xc000, 0xc00f, DebuggerWatchKind.CHANGE)
                runner.saveWatch(null, 0xc010, 0xc01f, DebuggerWatchKind.READ_WRITE, false)
            }
            if (scenario == "running") runner.run()
            for ((name, width, height) in listOf(PreviewRegistrationSize("wide", 1200, 900), PreviewRegistrationSize("narrow", 420, 800))) {
                val scene = ImageComposeScene(width, height) {
                    PgpStudioTheme {
                        Surface(Modifier.fillMaxSize()) {
                            DebuggerLayout(0, false, {
                                DebuggerControls(true, runner.state, null, null, {}, {}, {}, {}, {}, {}, strings)
                            }, { Text("CPU / Disassembly") }, { Text("Memory") }, strings,
                                registrations = { DebuggerRegistrationPanel(runner, DesktopKeyboardInput(), strings = strings, initialWatch = scenario == "watches") })
                        }
                    }
                }
                fun nodes(): List<SemanticsNode> {
                    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
                    return scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
                }
                var renderTime = 0L
                fun render() { repeat(4) { renderTime += 200_000_000L; scene.render(renderTime).close() } }
                fun click(key: String) {
                    val label = strings.text(key)
                    val node = nodes().first { it.config.contains(SemanticsActions.OnClick) &&
                        it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == label } }
                    check(node.config[SemanticsActions.OnClick].action!!.invoke())
                    render()
                }
                try {
                    render(); click("debug.registration.title")
                    scene.render(renderTime + 200_000_000L).use { image ->
                        image.encodeToData()?.use { data ->
                            File(directory, "${language.name.lowercase()}-$scenario-$name.png").writeBytes(data.bytes)
                        } ?: error("PNG encoding failed")
                    }
                } finally { scene.close() }
            }
        }
    }
}

private data class PreviewRegistrationSize(val name: String, val width: Int, val height: Int)
