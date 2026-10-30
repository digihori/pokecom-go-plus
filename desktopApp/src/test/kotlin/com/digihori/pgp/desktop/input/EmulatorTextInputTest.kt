package com.digihori.pgp.desktop.input

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class EmulatorTextInputTest {
    @Test fun focusTransferAndEditorDisposalRestoreInputWithoutOverridingModalBlock() = SwingUtilities.invokeAndWait {
        val keyboard = DesktopKeyboardInput()
        var visible by mutableStateOf(true)
        val scene = ImageComposeScene(600, 400) {
            PgpStudioTheme {
                Column {
                    if (visible) {
                        OutlinedTextField("First", {}, modifier = Modifier.emulatorTextInput(keyboard))
                        OutlinedTextField("Second", {}, modifier = Modifier.emulatorTextInput(keyboard))
                    }
                }
            }
        }
        try {
            var time = 0L
            fun render() { repeat(4) { time += 200_000_000; scene.render(time).close() } }
            fun nodes(): List<SemanticsNode> {
                fun visit(n: SemanticsNode): List<SemanticsNode> = listOf(n) + n.children.flatMap(::visit)
                return scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
            }
            fun focus(text: String) {
                val node = nodes().first { it.config.getOrNull(SemanticsProperties.EditableText)?.text == text }
                assertTrue(node.config[SemanticsActions.RequestFocus].action!!.invoke())
                render()
            }
            render()
            focus("First"); assertFalse(keyboard.isEnabled)
            focus("Second"); assertFalse(keyboard.isEnabled)
            keyboard.setModalInputBlocked(true)
            visible = false; render()
            assertFalse(keyboard.isEnabled)
            keyboard.setModalInputBlocked(false)
            assertTrue(keyboard.isEnabled)
        } finally { scene.close() }
    }
}
