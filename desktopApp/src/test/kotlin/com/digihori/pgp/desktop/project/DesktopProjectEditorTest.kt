package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.test.*

class DesktopProjectEditorTest {
    @Test fun renameMovesProjectContentsAndPersistsNameWhileBlankNameIsRejected() {
        val parent = Files.createTempDirectory("pgp-rename-test").toFile()
        val root = java.io.File(parent, "original").apply { mkdirs() }
        val workspace = workspace(root)
        val withSource = DesktopProjectEditor.addSource(workspace,
            java.io.File(parent, "main.bas").apply { writeText("10 END\n") }).getOrThrow()
        java.io.File(root, "build").mkdirs()
        java.io.File(root, "build/output.txt").writeText("artifact")
        val updated = DesktopProjectEditor.rename(withSource, " New name ").getOrThrow()
        assertEquals("New name", updated.definition.name)
        assertEquals(java.io.File(parent.canonicalFile, "New-name/pgp-project.json"), updated.manifestFile)
        assertFalse(root.exists())
        assertEquals("10 END\n", updated.sources.single().file.readText())
        assertEquals("artifact", java.io.File(parent, "New-name/build/output.txt").readText())
        assertEquals("New name", reopen(updated).definition.name)
        val bytes = updated.manifestFile.readBytes()
        assertTrue(DesktopProjectEditor.rename(updated, "  ").isFailure)
        assertContentEquals(bytes, updated.manifestFile.readBytes())
    }

    @Test fun destinationConflictLeavesOriginalProjectIntact() {
        val parent = Files.createTempDirectory("pgp-rename-conflict-test").toFile()
        val root = java.io.File(parent, "original").apply { mkdirs() }
        val workspace = workspace(root)
        val occupied = java.io.File(parent, "Taken").apply { mkdirs() }
        val before = workspace.manifestFile.readBytes()
        assertTrue(DesktopProjectEditor.rename(workspace, "Taken").isFailure)
        assertContentEquals(before, workspace.manifestFile.readBytes())
        assertTrue(occupied.exists())
    }

    @Test fun importPreservesOriginalAndExistingFileAndRegistersOnlySelectedSource() {
        val root = Files.createTempDirectory("pgp-import-test").toFile()
        val workspace = workspace(root)
        val directory = java.io.File(root, "src").apply { mkdirs() }
        java.io.File(directory, "extra.asm").writeText("old")
        java.io.File(directory, "unrelated.asm").writeText("NOP")
        val external = Files.createTempDirectory("pgp-external-test").resolve("extra.asm").toFile()
        external.writeText("NOP\n")
        val updated = DesktopProjectEditor.addSource(workspace, external).getOrThrow()
        assertEquals("NOP\n", external.readText())
        assertEquals("old", java.io.File(directory, "extra.asm").readText())
        assertEquals(listOf("src/extra-2.asm"), reopen(updated).sources.map { it.definition.path })
        assertEquals("NOP\n", updated.sources.single().file.readText())
        val before = updated.manifestFile.readBytes()
        val invalid = java.io.File(external.parentFile, "image.png").apply { writeText("image") }
        assertTrue(DesktopProjectEditor.addSource(updated, invalid).isFailure)
        assertContentEquals(before, updated.manifestFile.readBytes())
    }

    private fun workspace(root: java.io.File): DesktopProjectWorkspace {
        val manifest = java.io.File(root, "pgp-project.json")
        manifest.writeText("""{"format":"pgp-project","formatVersion":1,"name":"Original","machineId":"pc-1401","sources":[]}""")
        return assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace
    }
    private fun reopen(workspace: DesktopProjectWorkspace) =
        assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(workspace.manifestFile)).workspace
}
