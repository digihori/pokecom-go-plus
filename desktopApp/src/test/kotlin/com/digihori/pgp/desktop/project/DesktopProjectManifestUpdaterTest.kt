package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopProjectManifestUpdaterTest {
    @Test
    fun synchronizesAddedAndRemovedSourcesWithManifest() {
        val root = Files.createTempDirectory("pgp-project-add-source-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.bas").writeText("10 END\n")
        root.resolve("src/extra.bas").writeText("20 END\n")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(
            """
            {
              "format": "pgp-project",
              "formatVersion": 1,
              "name": "Add source test",
              "machineId": "pc-1245",
              "sources": [
                { "id": "main", "type": "basic", "path": "src/main.bas" },
                { "id": "old", "type": "memory-dump", "path": "src/old.dmp" }
              ]
            }
            """.trimIndent(),
        )
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace
        val tree = DesktopProjectTreeScanner.scan(workspace)

        val result = assertIs<DesktopProjectUpdateResult.Success>(
            DesktopProjectManifestUpdater.synchronize(workspace, tree),
        )
        assertEquals(1, result.addedCount)
        assertEquals(1, result.removedCount)

        val reopened = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace
        assertEquals(listOf("src/main.bas", "src/extra.bas"), reopened.sources.map { it.definition.path })
    }
}
