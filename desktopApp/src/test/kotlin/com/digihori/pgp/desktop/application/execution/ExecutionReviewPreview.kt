package com.digihori.pgp.desktop.application.execution

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

@OptIn(ExperimentalComposeUiApi::class)
internal fun renderExecutionReviews(directory: File) {
    directory.mkdirs()
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(460, 1000)) for (action in listOf("load", "continue")) {
            val strings = StudioStrings(language)
            val review = ExecutionReview("synthetic", action, "synthetic-session", 42, "pc-1245", "HELLO WORLD", "synthetic-project-revision",
                if (action == "load") "synthetic-build" else null, 0x1f6d, if (action == "load") 15 else 0,
                if (action == "load") listOf(ExecutionWriteRange(0xc000, 15), ExecutionWriteRange(0xc500, 87), ExecutionWriteRange(0xc590, 60), ExecutionWriteRange(0xc6e1, 4)) else emptyList(), if (action == "load") 166 else 0)
            val scene = ImageComposeScene(width, 1000) {
                PgpStudioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExecutionReviewPanel(review, listOf(ExecutionOperation(review.copy(id = "previous"), "REJECTED")), {}, {}, strings)
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
