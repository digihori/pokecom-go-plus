package com.digihori.pgp.desktop.application.development

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
internal fun renderDevelopmentReviews(directory: File) {
    directory.mkdirs()
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(460, 1000)) {
            val strings = StudioStrings(language)
            val scene = ImageComposeScene(width, 780) {
                PgpStudioTheme {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DevelopmentReviewPanel(DevelopmentReview("test", "PATCH", "HELLO WORLD", "src/hello.asm",
                                listOf(SourceReplacement("HOLD_START: LIA 8\n    PUSH", "HOLD_START: LIA 4\n    PUSH")), "test"),
                                listOf(DevelopmentOperation("build-test", "BUILD", "FAILED", "BUILD_FAILED")), {}, {}, strings)
                        }
                    }
                }
            }
            try {
                repeat(4) { scene.render(it * 200_000_000L).close() }
                scene.render(900_000_000L).use { image ->
                    File(directory, "${language.name}-$width.png").writeBytes(image.encodeToData()!!.bytes)
                }
            } finally { scene.close() }
        }
    }
}
