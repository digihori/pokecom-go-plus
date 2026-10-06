package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopProjectTreeScannerTest {
    @Test
    fun classifiesManifestAndSourceDirectoryFiles() {
        val root = Files.createTempDirectory("pgp-project-tree-test")
        root.resolve("src/nested").createDirectories()
        root.resolve("src/main.bas").writeText("10 END\n")
        root.resolve("src/new.asm").writeText("NOP\n")
        root.resolve("src/nested/data.dmp").writeText("C000: 00\n")
        root.resolve("src/.editor.tmp").writeText("temporary")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(
            """
            {
              "format": "pgp-project",
              "formatVersion": 1,
              "name": "Tree test",
              "machineId": "pc-1245",
              "sources": [
                { "id": "main", "type": "basic", "path": "src/main.bas" },
                { "id": "missing", "type": "memory-dump", "path": "src/missing.dmp" }
              ]
            }
            """.trimIndent(),
        )
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace

        val tree = DesktopProjectTreeScanner.scan(workspace)

        assertEquals(listOf("src/main.bas"), tree.tracked.map { it.definition.path })
        assertEquals(
            listOf("src/nested/data.dmp", "src/new.asm"),
            tree.untracked.map { it.relativePath },
        )
    }

    @Test
    fun detectsFilesAddedAfterProjectWasOpened() {
        val root = Files.createTempDirectory("pgp-project-tree-update-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.bas").writeText("10 END\n")
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(
            """
            {
              "format": "pgp-project",
              "formatVersion": 1,
              "name": "Tree update test",
              "machineId": "pc-1245",
              "sources": [
                { "id": "main", "type": "basic", "path": "src/main.bas" }
              ]
            }
            """.trimIndent(),
        )
        val workspace = assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace

        assertEquals(0, DesktopProjectTreeScanner.scan(workspace).untracked.size)
        root.resolve("src/added.bas").writeText("20 END\n")
        assertEquals(
            listOf("src/added.bas"),
            DesktopProjectTreeScanner.scan(workspace).untracked.map { it.relativePath },
        )
    }
}
