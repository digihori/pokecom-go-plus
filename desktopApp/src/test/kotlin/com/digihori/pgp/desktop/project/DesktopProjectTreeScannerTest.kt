package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class DesktopProjectTreeScannerTest {
    @Test
    fun refreshesExistingSourceDescriptionEvenWithUnchangedTimestampAndSize() {
        val root = Files.createTempDirectory("pgp-project-description-test")
        try {
            root.resolve("src").createDirectories()
            val source = root.resolve("src/main.bas")
            source.writeText("# Before\n10 END\n")
            val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
            manifest.writeText("""
                {"format":"pgp-project","formatVersion":1,"name":"Description test",
                 "machineId":"pc-1245","sources":[
                    {"id":"main","type":"basic","path":"src/main.bas"}]}
            """.trimIndent())
            val workspace = assertIs<DesktopProjectOpenResult.Success>(
                DesktopProjectWorkspaceLoader.open(manifest.toFile())).workspace
            val before = DesktopProjectTreeScanner.scan(workspace)
            val timestamp = Files.getLastModifiedTime(source)
            source.writeText("# After!\n10 END\n")
            Files.setLastModifiedTime(source, timestamp)
            val after = DesktopProjectTreeScanner.scan(workspace)
            assertEquals(before.tracked, after.tracked)
            assertNotEquals(before, after)
            assertEquals("Before", before.sourceDescriptions["src/main.bas"])
            assertEquals("After!", after.sourceDescriptions["src/main.bas"])
            source.writeText("10 END\n")
            assertEquals("", DesktopProjectTreeScanner.scan(workspace).sourceDescriptions["src/main.bas"])
        } finally {
            root.toFile().deleteRecursively()
        }
    }

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
