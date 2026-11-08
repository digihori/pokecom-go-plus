package com.digihori.pgp.desktop.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
class ProjectFileTreeUiTest {
    @Test fun rightClickSelectsAndActsOnClickedFileRatherThanPreviousSelection() {
        SwingUtilities.invokeAndWait {
            for (language in StudioLanguage.entries) {
                val strings = StudioStrings(language)
                val files = listOf(
                    ProjectFileEntry("src/one.bas", File("one.bas"), ProjectSourceType.BASIC, true),
                    ProjectFileEntry("src/two.asm", File("two.asm"), ProjectSourceType.ASSEMBLY, true),
                    ProjectFileEntry("src/run.pgmacro", File("run.pgmacro"), null, false),
                )
                var selected by mutableStateOf<String?>("src/one.bas")
                var action: Pair<ProjectFileAction, String>? = null
                val scene = ImageComposeScene(700, 440) {
                    PgpStudioTheme { Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp)) {
                            ProjectFileTree(projectFileNodes(files), selected, true, true, { selected = it },
                                { kind, file -> action = kind to file.path }, strings)
                        }
                    } }
                }
                fun frames() { repeat(4) { scene.render(System.nanoTime()).close() } }
                fun nodes(): List<SemanticsNode> {
                    fun descend(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::descend)
                    return scene.semanticsOwners.flatMap { descend(it.rootSemanticsNode) }
                }
                try {
                    frames()
                    val row = nodes().first {
                        it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(strings.text("project.selectPath", "src/two.asm")) == true
                    }
                    val point = row.boundsInRoot.center
                    scene.sendPointerEvent(PointerEventType.Press, point, buttons = PointerButtons(isSecondaryPressed = true), button = PointerButton.Secondary)
                    scene.sendPointerEvent(PointerEventType.Release, point, buttons = PointerButtons(), button = PointerButton.Secondary)
                    frames()
                    assertEquals("src/two.asm", selected)
                    val rename = nodes().first {
                        it.config.contains(SemanticsActions.OnClick) &&
                            it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text("project.contextRename") }
                    }
                    val directory = File("build/reports/project-ui").apply { mkdirs() }
                    val picture = scene.render(System.nanoTime())
                    try { File(directory, "${language.name.lowercase()}-context-menu.png").writeBytes(picture.encodeToData(EncodedImageFormat.PNG)!!.bytes) }
                    finally { picture.close() }
                    assertTrue(rename.config[SemanticsActions.OnClick].action!!.invoke())
                    assertEquals(ProjectFileAction.RENAME to "src/two.asm", action)
                } finally { scene.close() }
            }
        }
    }
}
