package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.rom.MachineId
import java.nio.file.Files
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopProjectWorkspaceCreatorTest {
    @Test
    fun createsCombinedTemplateThatCanBeOpenedAndBuilt() {
        val root = Files.createTempDirectory("pgp-project-create-test")

        val created = assertIs<DesktopProjectCreateResult.Success>(
            DesktopProjectWorkspaceCreator.create(
                root.toFile(),
                "New project",
                MachineId("pc-1245"),
                DesktopProjectTemplate.BASIC_AND_MACHINE_CODE,
            ),
        )
        val projectRoot = root.resolve("New-project")
        val manifest = projectRoot.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        assertEquals(manifest.toFile(), created.manifestFile)
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace

        assertEquals(listOf(ProjectSourceType.BASIC, ProjectSourceType.MEMORY_DUMP),
            workspace.sources.map { it.definition.type })
        assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        assertTrue(projectRoot.resolve("src/main.bas").readText().contains("HELLO"))
        assertTrue(projectRoot.resolve("build").toFile().isDirectory)
    }

    @Test
    fun doesNotOverwriteExistingProjectFolder() {
        val parent = Files.createTempDirectory("pgp-project-existing-test")
        assertIs<DesktopProjectCreateResult.Success>(
            DesktopProjectWorkspaceCreator.create(
                parent.toFile(),
                "New project",
                MachineId("pc-1245"),
                DesktopProjectTemplate.BASIC,
            ),
        )
        assertIs<DesktopProjectCreateError.DestinationExists>(
            assertIs<DesktopProjectCreateResult.Failure>(
                DesktopProjectWorkspaceCreator.create(
                    parent.toFile(),
                    "New project",
                    MachineId("pc-1245"),
                    DesktopProjectTemplate.BASIC,
                ),
            ).error,
        )
        assertTrue(parent.resolve("New-project/src/main.bas").readText().contains("HELLO"))
    }

    @Test
    fun derivesPortableFolderNameFromDisplayName() {
        assertEquals("My-Game-Test", DesktopProjectWorkspaceCreator.folderName(" My Game/Test "))
    }
}
