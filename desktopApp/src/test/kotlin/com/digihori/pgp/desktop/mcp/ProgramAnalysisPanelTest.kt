package com.digihori.pgp.desktop.mcp

import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import kotlinx.serialization.json.*
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ProgramAnalysisPanelTest {
    @Test fun reviewAndExplicitPublishAreRequiredInBothLanguages() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(420, 800)) {
            val strings = StudioStrings(language)
            val service = ProgramAnalysisService()
            val adapter = PgpMcpToolAdapter({ null }, analysisProvider = { service })
            fun read(args: JsonObject = buildJsonObject {}): JsonObject =
                adapter.call(ProgramAnalysisMcp.GET, args).getValue("structuredContent").jsonObject
            val fixture = generateSequence(File(".").absoluteFile) { it.parentFile }
                .map { File(it, "test-data/basic/ai/pc1251-numeric-labels/src/main.bas") }
                .first { it.isFile }
            val source = fixture.readText()
            var metadata by mutableStateOf(service.metadata())
            var publications = 0
            val scene = ImageComposeScene(width, 780) {
                PgpStudioTheme { Surface {
                    ProgramAnalysisPanel(service, metadata, { fixture.name to fixture.readBytes() },
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
                fun buttons(label: String) = nodes().filter { it.config.contains(SemanticsActions.OnClick) &&
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == label } }
                fun click(label: String) { assertTrue(buttons(label).first().config[SemanticsActions.OnClick].action!!.invoke()); render() }
                render()
                assertTrue(buttons(strings.text("analysis.publish")).isEmpty())
                click("PC-1251")
                click(strings.text("analysis.choose"))
                assertEquals(0, publications)
                click(strings.text("analysis.preview"))
                assertEquals(0, publications)
                assertTrue(nodes().any { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text.contains(source.lineSequence().first()) } })
                val directory = File("build/reports/program-analysis").apply { mkdirs() }
                scene.render(clock + 200_000_000).use { image -> image.encodeToData()!!.use {
                    File(directory, "pc1251-${language.name.lowercase()}-$width.png").writeBytes(it.bytes)
                } }
                click(strings.text("analysis.publish"))
                assertEquals(1, publications)
                val target = read().getValue("target").jsonObject
                assertEquals(JsonPrimitive("pc-1251"), target["machineId"])
                val page = read(buildJsonObject {
                    put("section", "source")
                    put("targetId", target.getValue("targetId"))
                    put("revision", target.getValue("revision"))
                })
                assertEquals(source.lines(), page.getValue("lines").jsonArray.map {
                    it.jsonObject.getValue("text").jsonPrimitive.content
                })
                assertTrue(buttons(strings.text("analysis.publish")).isEmpty())
                click(strings.text("analysis.clear"))
                assertTrue(service.metadata().toString().contains("\"available\":false"))
            } finally { scene.close() }
        }
    }
}
