package com.digihori.pgp.desktop.application.development

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ProjectGenerationPanelTest {
    @Test fun destinationSelectionEnablesSeparateApprovalAndFullTextIsVisibleInBothLanguages() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(420, 800)) {
            val strings = StudioStrings(language)
            var destination by mutableStateOf<String?>(null)
            var approved = 0
            var rejected = 0
            val review = DevelopmentReview("generation", "CREATE_PROJECT", "Example", null, emptyList(), "no-project",
                generatedSources = listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 PRINT \"HELLO\"\n20 END\n")))
            val scene = ImageComposeScene(width, 780) {
                PgpStudioTheme { Surface {
                    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DevelopmentReviewPanel(review, emptyList(), { approved++ }, { rejected++ }, strings,
                            creationDestination = destination, onChooseCreationParent = { destination = "Selected parent/pgp-generated-example" })
                    }
                } }
            }
            try {
                var time = 0L
                fun render() { repeat(4) { time += 200_000_000; scene.render(time).close() } }
                fun nodes(): List<SemanticsNode> {
                    fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
                    return scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
                }
                fun button(key: String) = nodes().first { it.config.contains(SemanticsActions.OnClick) &&
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text(key) } }
                fun click(key: String) { assertTrue(button(key).config[SemanticsActions.OnClick].action!!.invoke()); render() }
                render()
                assertTrue(button("mcp.review.approve").config.contains(SemanticsProperties.Disabled))
                click("mcp.review.chooseParent"); assertEquals(0, approved)
                assertFalse(button("mcp.review.approve").config.contains(SemanticsProperties.Disabled))
                assertTrue(nodes().any { it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text.contains("10 PRINT") } })
                val output = File("build/reports/project-generation").apply { mkdirs() }
                scene.render(time + 200_000_000).use { image -> image.encodeToData()!!.use {
                    File(output, "${language.name.lowercase()}-$width.png").writeBytes(it.bytes)
                } }
                click("mcp.review.approve"); assertEquals(1, approved)
                click("mcp.review.reject"); assertEquals(1, rejected)
            } finally { scene.close() }
        }
    }
}
