package com.digihori.pgp.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.input.PocketKeyCap
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class SevenFixesUiTest {
    @Test fun longPatchKeepsApprovalAboveBoundedDiffAndRendersArrowLegends() = SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            var approved = 0
            var rejected = 0
            val review = DevelopmentReview("preview", "PATCH", "Example", "main.bas",
                listOf(SourceReplacement((1..150).joinToString("\n") { "$it PRINT OLD" }, (1..150).joinToString("\n") { "$it PRINT NEW" })), "revision")
            val scene = ImageComposeScene(900, 720) {
                PgpStudioTheme {
                    Surface {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PocketKeyButton(PocketKeyCap(PocketKey.STAT, "↕︎", "STAT", column = 0), null, Modifier.size(100.dp, 78.dp))
                                PocketKeyButton(PocketKeyCap(PocketKey.TITLE, "↕︎", "TITLE", column = 0), null, Modifier.size(100.dp, 78.dp), fitLabels = true)
                            }
                            DevelopmentReviewPanel(review, emptyList(), { approved++ }, { rejected++ }, strings)
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
                fun button(key: String) = nodes().first { it.config.contains(SemanticsActions.OnClick) &&
                    it.config.getOrNull(SemanticsProperties.Text).orEmpty().any { t -> t.text == strings.text(key) } }
                render()
                val top = button("mcp.review.approve").boundsInRoot.top
                assertTrue(top in 100f..400f)
                val diff = nodes().filter { it.config.contains(SemanticsActions.ScrollBy) }.minBy { it.boundsInRoot.height }
                assertTrue(diff.config[SemanticsActions.ScrollBy].action!!.invoke(0f, 500f))
                render()
                assertEquals(top, button("mcp.review.approve").boundsInRoot.top)
                assertTrue(button("mcp.review.approve").config[SemanticsActions.OnClick].action!!.invoke())
                assertTrue(button("mcp.review.reject").config[SemanticsActions.OnClick].action!!.invoke())
                assertEquals(1, approved); assertEquals(1, rejected)
                val directory = File("build/reports/seven-fixes").apply { mkdirs() }
                scene.render(time + 200_000_000).use { image ->
                    image.encodeToData()!!.use { File(directory, "${language.name.lowercase()}-review-arrows.png").writeBytes(it.bytes) }
                }
            } finally { scene.close() }
        }
    }
}
