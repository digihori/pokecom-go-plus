package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectSourceType
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopProjectWorkspaceTest {
    @Test
    fun opensProjectAndResolvesAllSourcesRelativeToManifest() {
        val root = Files.createTempDirectory("pgp-project-test")
        val sourceDirectory = Files.createDirectories(root.resolve("src"))
        sourceDirectory.resolve("main.bas").writeText("10 PRINT \"HELLO\"\n")
        sourceDirectory.resolve("routine.dmp").writeText("C000: 00 01\n")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(projectJson())

        val opened = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        )

        assertEquals("Sample", opened.workspace.definition.name)
        assertEquals(2, opened.workspace.sources.size)
        assertEquals(ProjectSourceType.BASIC, opened.workspace.sources.first().definition.type)
        assertEquals(sourceDirectory.resolve("main.bas").toFile(), opened.workspace.sources.first().file)
    }

    @Test
    fun opensProjectWithUnavailableSourcesSoBuildCanReportThem() {
        val root = Files.createTempDirectory("pgp-project-missing-test")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(projectJson())

        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace

        val tree = DesktopProjectTreeScanner.scan(workspace)
        assertTrue(tree.tracked.isEmpty())
    }

    @Test
    fun detectsExternalModificationDeletionAndRecreation() {
        val root = Files.createTempDirectory("pgp-project-watch-test")
        val sourceDirectory = Files.createDirectories(root.resolve("src"))
        val basic = sourceDirectory.resolve("main.bas")
        basic.writeText("10 PRINT \"A\"\n")
        sourceDirectory.resolve("routine.dmp").writeText("C000: 00\n")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(projectJson())
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace
        val tracker = DesktopProjectChangeTracker(workspace)

        assertTrue(tracker.scan().isEmpty())
        basic.writeText("10 PRINT \"B\"\n")
        assertEquals(DesktopProjectChangeKind.MODIFIED, tracker.scan().single().kind)
        Files.delete(basic)
        assertEquals(DesktopProjectChangeKind.DELETED, tracker.scan().single().kind)
        basic.writeText("10 PRINT \"C\"\n")
        assertEquals(DesktopProjectChangeKind.CREATED, tracker.scan().single().kind)
    }

    private fun projectJson(): String =
        """
        {
          "format": "pgp-project",
          "formatVersion": 1,
          "name": "Sample",
          "machineId": "pc-1245",
          "sources": [
            { "id": "main", "type": "basic", "path": "src/main.bas" },
            { "id": "routine", "type": "memory-dump", "path": "src/routine.dmp" }
          ]
        }
        """.trimIndent()
}
