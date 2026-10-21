package com.digihori.pgp.desktop.application.development

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import com.digihori.pgp.desktop.project.*
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalComposeUiApi::class)
class DevelopmentStateBindingTest {
    @Test fun sourceAndBuildChangesReachMcpWithoutAnyOtherUiRecomposition() {
        val root = Files.createTempDirectory("pgp-state-binding-").toFile()
        try {
            root.resolve("main.asm").writeText("ORG 0xC500\nRTN\n")
            val manifest = root.resolve("pgp-project.json")
            manifest.writeText("""{"format":"pgp-project","formatVersion":1,"name":"Binding test","machineId":"pc-1245","sources":[{"id":"main","type":"assembly","path":"main.asm"}]}""")
            val workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace
            SwingUtilities.invokeAndWait {
                val status = mutableStateOf("UP_TO_DATE")
                val busy = mutableStateOf(false)
                val changed = mutableStateOf(emptySet<String>())
                val errors = mutableStateOf(emptyList<DesktopProjectBuildError>())
                val published = AtomicReference<DevelopmentReadState?>()
                val service = DesktopDevelopmentReadService { requireNotNull(published.get()) }
                val scene = ImageComposeScene(32, 32) {
                    PublishDevelopmentReadState(published) {
                        DevelopmentReadState(project = workspace, buildStatus = status.value, busy = busy.value,
                            changedSourceIds = changed.value, buildErrors = errors.value)
                    }
                }
                var time = 0L
                fun render() { repeat(4) { time += 200_000_000L; scene.render(time).close() } }
                fun metadata() = service.context("metadata", null, 0, 32)
                try {
                    render()
                    val initialRevision = metadata()["metadataRevision"]
                    status.value = "SOURCE_CHANGED"; changed.value = setOf("main")
                    render()
                    assertEquals("SOURCE_CHANGED", metadata()["project"]!!.jsonObject["buildStatus"]!!.jsonPrimitive.content)
                    assertNotEquals(initialRevision, metadata()["metadataRevision"])
                    assertTrue(metadata()["sources"]!!.jsonArray.single().jsonObject["changedSinceStudioBaseline"]!!.jsonPrimitive.boolean)
                    busy.value = true; render()
                    assertTrue(metadata()["project"]!!.jsonObject["busy"]!!.jsonPrimitive.boolean)
                    errors.value = listOf(DesktopProjectBuildError.MemoryOverlap("main", "other", 0xc500))
                    status.value = "BUILD_FAILED"; busy.value = false; render()
                    assertEquals("MEMORY_OVERLAP", service.context("diagnostics", null, 0, 32)["diagnostics"]!!.jsonArray.single().jsonObject["code"]!!.jsonPrimitive.content)
                    errors.value = emptyList(); status.value = "BUILT_NOT_LOADED"; render()
                    assertEquals("BUILT_NOT_LOADED", metadata()["project"]!!.jsonObject["buildStatus"]!!.jsonPrimitive.content)
                    assertTrue(service.context("diagnostics", null, 0, 32)["diagnostics"]!!.jsonArray.isEmpty())
                } finally { scene.close() }
            }
        } finally { root.deleteRecursively() }
    }
}
