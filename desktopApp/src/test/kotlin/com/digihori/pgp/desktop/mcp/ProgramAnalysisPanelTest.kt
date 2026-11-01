package com.digihori.pgp.desktop.mcp

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ProgramAnalysisPanelTest {
    @Test fun reviewAndExplicitPublishAreRequiredInBothLanguages() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(420, 800)) {
            val strings = StudioStrings(language)
            val service = ProgramAnalysisService()
            var metadata by mutableStateOf(service.metadata())
            var publications = 0
            val scene = ImageComposeScene(width, 780) {
                PgpStudioTheme { Surface {
                    ProgramAnalysisPanel(service, metadata, { "example.bas" to "10 CALL 50432\n20 END".encodeToByteArray() },
                        { service.publish(it); publications++; metadata = service.metadata() },
                        { service.clear(); metadata = service.metadata() }, strings)
                } }
            }
            try {
                var clock = 0L
                fun render() { repeat(4) { clock += 200_000_000; scene.render(clock).close() } }
                fun nodes(): List<SemanticsNode> {
                    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
                    return scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
                }
                fun buttons(key: String) = nodes().filter { it.config.contains(SemanticsActions.OnClick) &&
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text(key) } }
                fun click(key: String) { assertTrue(buttons(key).first().config[SemanticsActions.OnClick].action!!.invoke()); render() }
                render()
                assertTrue(buttons("analysis.publish").isEmpty())
                click("analysis.choose")
                assertEquals(0, publications)
                click("analysis.preview")
                assertEquals(0, publications)
                assertTrue(nodes().any { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text.contains("10 CALL 50432") } })
                val directory = File("build/reports/program-analysis").apply { mkdirs() }
                scene.render(clock + 200_000_000).use { image -> image.encodeToData()!!.use {
                    File(directory, "${language.name.lowercase()}-$width.png").writeBytes(it.bytes)
                } }
                click("analysis.publish")
                assertEquals(1, publications)
                assertTrue(buttons("analysis.publish").isEmpty())
                click("analysis.clear")
                assertTrue(service.metadata().toString().contains("\"available\":false"))
            } finally { scene.close() }
        }
    }
}
