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
                setOf(DesktopProjectStarter.BASIC, DesktopProjectStarter.MEMORY_DUMP),
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
                setOf(DesktopProjectStarter.BASIC),
            ),
        )
        assertIs<DesktopProjectCreateError.DestinationExists>(
            assertIs<DesktopProjectCreateResult.Failure>(
                DesktopProjectWorkspaceCreator.create(
                    parent.toFile(),
                    "New project",
                    MachineId("pc-1245"),
                    setOf(DesktopProjectStarter.BASIC),
                ),
            ).error,
        )
        assertTrue(parent.resolve("New-project/src/main.bas").readText().contains("HELLO"))
    }

    @Test
    fun numbersTheDefaultProjectFolderWithoutOverwritingExistingProjects() {
        val parent = Files.createTempDirectory("pgp-default-project-number-test")
        parent.resolve("New-PGP-Project").toFile().mkdir()
        parent.resolve("New-PGP-Project-2").toFile().mkdir()

        val created = assertIs<DesktopProjectCreateResult.Success>(
            DesktopProjectWorkspaceCreator.create(
                parent.toFile(),
                "New PGP Project",
                MachineId("pc-1245"),
                setOf(DesktopProjectStarter.BASIC),
            ),
        )

        assertEquals(
            parent.resolve("New-PGP-Project-3/pgp-project.json").toFile(),
            created.manifestFile,
        )
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(created.manifestFile),
        ).workspace
        assertEquals("New PGP Project 3", workspace.definition.name)
        assertTrue(parent.resolve("New-PGP-Project").toFile().isDirectory)
        assertTrue(parent.resolve("New-PGP-Project-2").toFile().isDirectory)
    }

    @Test
    fun derivesPortableFolderNameFromDisplayName() {
        assertEquals("My-Game-Test", DesktopProjectWorkspaceCreator.folderName(" My Game/Test "))
    }

    @Test
    fun createsAllSelectedStarterSourcesIncludingAssembly() {
        val root = Files.createTempDirectory("pgp-project-starters-test")

        val created = assertIs<DesktopProjectCreateResult.Success>(
            DesktopProjectWorkspaceCreator.create(
                root.toFile(),
                "All sources",
                MachineId("pc-1360"),
                DesktopProjectStarter.entries.toSet(),
            ),
        )
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(created.manifestFile),
        ).workspace

        assertEquals(
            listOf(ProjectSourceType.BASIC, ProjectSourceType.MEMORY_DUMP, ProjectSourceType.ASSEMBLY),
            workspace.sources.map { it.definition.type },
        )
        val assembly = root.resolve("All-sources/src/main.asm").readText()
        assertTrue(assembly.contains("Sample SC61860 program"))
        assertTrue(assembly.contains("START: LII 0x12"))
        assertTrue(assembly.contains("LIDP TABLE"))
        assertTrue(assembly.contains("JRP DONE"))
        assertTrue(assembly.contains("TABLE: DB 0x01, &02, 3"))
        assertTrue(assembly.contains("DONE:  RTN"))
    }
}
