package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.test.*

class DesktopProjectEditorTest {
    @Test fun createsMultipleBasicAndAuxiliaryMacroWithoutBuildingMacro() {
        val root = Files.createTempDirectory("pgp-create-files-").toFile()
        try {
            var current = workspace(root)
            for (name in listOf("one.bas", "two.bas", "setup.pgmacro")) {
                current = DesktopProjectEditor.createFile(current, name).getOrThrow().workspace
            }
            assertEquals(listOf("src/one.bas", "src/two.bas"), reopen(current).sources.map { it.definition.path })
            assertEquals(listOf("src/setup.pgmacro"), DesktopProjectTreeScanner.scan(current).untracked.map { it.relativePath })
            val before = current.manifestFile.readBytes()
            assertTrue(DesktopProjectEditor.createFile(current, "one.bas").isFailure)
            assertTrue(DesktopProjectEditor.createFile(current, "../escape.bas").isFailure)
            assertContentEquals(before, current.manifestFile.readBytes())
            assertEquals("10 END\n", java.io.File(root, "src/one.bas").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun batchImportKeepsDuplicateNamesUniqueAndLeavesMacrosOutOfManifest() {
        val root = Files.createTempDirectory("pgp-batch-import-").toFile()
        val external = Files.createTempDirectory("pgp-batch-external-").toFile()
        try {
            val basic = java.io.File(external, "main.bas").apply { writeText("10 END\n") }
            val macro = java.io.File(external, "run.pgmacro").apply { writeText("cmd \"RUN\"\n") }
            val edit = DesktopProjectEditor.addSources(workspace(root), listOf(basic, basic, macro)).getOrThrow()
            assertEquals(listOf("src/main.bas", "src/main-2.bas", "src/run.pgmacro"), edit.paths)
            assertEquals(2, reopen(edit.workspace).sources.size)
            assertEquals(2, edit.workspace.sources.map { it.definition.id }.toSet().size)
            assertEquals("10 END\n", basic.readText())
        } finally { root.deleteRecursively(); external.deleteRecursively() }
    }

    @Test fun renamePreservesIdentityAndRejectsConflictsAndExtensionChanges() {
        val root = Files.createTempDirectory("pgp-file-rename-").toFile()
        try {
            val initial = DesktopProjectEditor.createFile(workspace(root), "main.bas").getOrThrow().workspace
            val originalId = initial.sources.single().definition.id
            val edit = DesktopProjectEditor.renameFile(initial, "src/main.bas", "日本語.bas").getOrThrow()
            assertEquals(originalId, reopen(edit.workspace).sources.single().definition.id)
            assertEquals("src/日本語.bas", reopen(edit.workspace).sources.single().definition.path)
            assertFalse(java.io.File(root, "src/main.bas").exists())
            val before = edit.workspace.manifestFile.readBytes()
            java.io.File(root, "src/taken.bas").writeText("keep")
            for (name in listOf("taken.bas", "bad.asm", "../escape.bas")) {
                assertTrue(DesktopProjectEditor.renameFile(edit.workspace, "src/日本語.bas", name).isFailure)
            }
            assertContentEquals(before, edit.workspace.manifestFile.readBytes())
            assertEquals("keep", java.io.File(root, "src/taken.bas").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun manifestFailureRollsBackCreateImportAndRename() {
        val root = Files.createTempDirectory("pgp-file-rollback-").toFile()
        val external = Files.createTempDirectory("pgp-file-rollback-external-").toFile()
        try {
            val initial = DesktopProjectEditor.createFile(workspace(root), "main.bas").getOrThrow().workspace
            val blocked = java.io.File(root, "blocked").apply { mkdir(); resolve("keep").writeText("keep") }
            val broken = initial.copy(manifestFile = blocked)
            assertTrue(DesktopProjectEditor.createFile(broken, "new.bas").isFailure)
            assertFalse(java.io.File(root, "src/new.bas").exists())
            val first = java.io.File(external, "a.bas").apply { writeText("10 END\n") }
            val second = java.io.File(external, "b.bas").apply { writeText("20 END\n") }
            assertTrue(DesktopProjectEditor.addSources(broken, listOf(first, second)).isFailure)
            assertFalse(java.io.File(root, "src/a.bas").exists())
            assertFalse(java.io.File(root, "src/b.bas").exists())
            assertTrue(DesktopProjectEditor.renameFile(broken, "src/main.bas", "renamed.bas").isFailure)
            assertTrue(java.io.File(root, "src/main.bas").isFile)
            assertFalse(java.io.File(root, "src/renamed.bas").exists())
            assertEquals("src/main.bas", reopen(initial).sources.single().definition.path)
        } finally { root.deleteRecursively(); external.deleteRecursively() }
    }

    @Test fun auxiliaryMacroRenameStaysOutsideBuildSources() {
        val root = Files.createTempDirectory("pgp-macro-rename-").toFile()
        try {
            val created = DesktopProjectEditor.createFile(workspace(root), "run.pgmacro").getOrThrow()
            val renamed = DesktopProjectEditor.renameFile(created.workspace, created.paths.single(), "start.pgmacro").getOrThrow()
            assertTrue(reopen(renamed.workspace).sources.isEmpty())
            assertTrue(java.io.File(root, "src/start.pgmacro").isFile)
        } finally { root.deleteRecursively() }
    }

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
