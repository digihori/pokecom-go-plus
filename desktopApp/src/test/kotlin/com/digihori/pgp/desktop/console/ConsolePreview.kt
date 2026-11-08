package com.digihori.pgp.desktop.console

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.desktop.localization.StudioLanguage
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import java.time.LocalTime
import javax.swing.SwingUtilities
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
internal fun renderConsolePreviews(directory: File) {
    directory.mkdirs()
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) for (width in listOf(520, 1000)) {
            val strings = StudioStrings(language)
            val console = StudioConsoleState(true, clock = { LocalTime.of(12, 34, 56) })
            console.append(strings.text("console.buildSource", "src/basic1.bas"))
            console.append(strings.text("console.loadedTarget", "src/basic1.bas"))
            console.append("macro.pgmacro:8  cmd \"RUN\"\nROM ERROR", ConsoleLevel.ERROR)
            val scene = ImageComposeScene(width, 320) {
                PgpStudioTheme { Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StudioConsolePanel(console, 240.dp, {}, {}, {}, {}, strings = strings,
                            macroBusy = true, pauseMessage = if (width == 520) strings.text("macro.pauseMessage", "確認して再開") else null)
                        Text(strings.text("console.loadedTarget", "src/basic1.bas"), style = MaterialTheme.typography.bodySmall)
                    }
                } }
            }
            try {
                scene.render()
                File(directory, "${language.name.lowercase()}-$width.png").writeBytes(
                    scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
            } finally { scene.close() }
        }
    }
}
