package com.digihori.pgp.desktop.application.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.*
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import kotlinx.serialization.json.*

@OptIn(ExperimentalComposeUiApi::class)
internal fun renderDebugReviews(directory: File) {
    directory.mkdirs()
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(460, 1000)) for (action in listOf("save_breakpoint", "pause", "step", "trace")) {
            val strings = StudioStrings(language)
            val review = DebugReview("synthetic", DebugAction(action, buildJsonObject {}), "synthetic-session", 42, "pc-1245",
                if (action == "save_breakpoint") "—" else if (action == "pause") "RUNNING, PC=0xC500" else "PAUSED, PC=0xC500",
                when (action) {
                    "save_breakpoint" -> "address = 0xC500\nenabled = true\ncondition = A == 0x41\nhitCondition = >= 2"
                    "step" -> "1 instruction at PC=0xC500 (may write RAM / I/O)"
                    "trace" -> "enabled = true\ncapacity = 256"
                    else -> "PAUSED (same live Session; PC may advance until approval)"
                })
            val scene = ImageComposeScene(width, 950) {
                PgpStudioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DebugReviewPanel(review, listOf(DebugOperation("prior", "step", "SUCCEEDED", "synthetic-session", 40, 41)), {}, {}, strings)
                        }
                    }
                }
            }
            try {
                repeat(4) { scene.render(it * 200_000_000L).close() }
                scene.render(900_000_000L).use { image -> File(directory, "${language.name}-$width-$action.png").writeBytes(image.encodeToData()!!.bytes) }
            } finally { scene.close() }
        }
    }
}
