package com.digihori.pgp.desktop.project

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.localization.StudioLanguage
import com.digihori.pgp.desktop.localization.StudioStrings
import com.digihori.pgp.desktop.theme.PgpStudioTheme
import java.io.File
import javax.swing.SwingUtilities
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
internal fun renderAssemblerProfilePreviews(directory: File) {
    directory.mkdirs()
    SwingUtilities.invokeAndWait {
        for (language in StudioLanguage.entries) {
            val store = DesktopAssemblerPreferences(MemoryAssemblerPreferences())
            store.save(DesktopAssemblerSettings(true, "perl", "\"/path/to/yasm.pl\" {source}",
                tool = DesktopAssemblerTool.YASM, testSource = "/path/to/source.asm"))
            val scene = ImageComposeScene(760, 1200) {
                PgpStudioTheme { Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DesktopAssemblerSettingsPanel(MachineMemoryLayout(MachineId("pc-1350")),
                            store = store, strings = StudioStrings(language))
                        AssemblyConsoleLog("YASM log", (1..1000).joinToString("\n") { "label_$it  4000 : 37" })
                        Text("Build & Load")
                    }
                } }
            }
            scene.render()
            File(directory, "${language.name.lowercase()}-assembler-profiles.png").writeBytes(
                scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
            scene.close()
        }
    }
}
