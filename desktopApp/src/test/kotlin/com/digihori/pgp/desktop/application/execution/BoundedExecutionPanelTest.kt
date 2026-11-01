package com.digihori.pgp.desktop.application.execution

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BoundedExecutionPanelTest {
    @Test fun inputAndLimitsAreReviewableBeforeApprovalInBothLanguages() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(420, 800)) {
            val strings = StudioStrings(language)
            var approvals = 0; var rejections = 0
            val review = BoundedExecutionReview("test", "session", 1, "content-revision", "Generated program", "keys",
                listOf(PocketKey.NUM_1, PocketKey.ENTER), 100000, 500000)
            val scene = ImageComposeScene(width, 780) {
                PgpStudioTheme { Surface { Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BoundedExecutionReviewPanel(review, emptyList(), { approvals++ }, { rejections++ }, strings)
                } } }
            }
            try {
                var time = 0L
                repeat(4) { time += 200_000_000; scene.render(time).close() }
                fun visit(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::visit)
                val nodes = scene.semanticsOwners.flatMap { visit(it.rootSemanticsNode) }
                val text = nodes.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.joinToString { it.text }
                assertTrue("NUM_1 → ENTER" in text); assertTrue("100000" in text && "500000" in text)
                assertEquals(0, approvals)
                fun click(key: String) {
                    val node = nodes.first { it.config.contains(SemanticsActions.OnClick) && it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { text -> text.text == strings.text(key) } }
                    assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
                }
                click("mcp.review.approve"); assertEquals(1, approvals)
                click("mcp.review.reject"); assertEquals(1, rejections)
                val directory = File("build/reports/bounded-execution").apply { mkdirs() }
                scene.render(time + 200_000_000).use { image -> image.encodeToData()!!.use { File(directory, "${language.name.lowercase()}-$width.png").writeBytes(it.bytes) } }
            } finally { scene.close() }
        }
    }
}
