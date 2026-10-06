package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopProjectBuilderTest {
    @Test
    fun buildsBasicDumpAndRawBinaryAsOneArtifact() {
        val root = Files.createTempDirectory("pgp-project-build-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.bas").writeText("10 PRINT \"HELLO\"\n")
        root.resolve("src/routine.dmp").writeText("C000: 01 02\n")
        root.resolve("src/data.bin").writeBytes(byteArrayOf(3, 4))
        val workspace = openWorkspace(
            root,
            """
            { "id": "main", "type": "basic", "path": "src/main.bas" },
            { "id": "routine", "type": "memory-dump", "path": "src/routine.dmp" },
            {
              "id": "data",
              "type": "raw-binary",
              "path": "src/data.bin",
              "loadAddress": "0xC100"
            }
            """.trimIndent(),
        )

        val artifact = assertIs<DesktopProjectBuildResult.Success>(
            DesktopProjectBuilder.build(workspace),
        ).artifact

        assertTrue(checkNotNull(artifact.copyBasicProgram()).isNotEmpty())
        val segments = checkNotNull(artifact.memoryImage).segments
        assertEquals(listOf(0xc000, 0xc100), segments.map { it.startAddress })
        assertContentEquals(byteArrayOf(3, 4), segments[1].copyBytes())
    }

    @Test
    fun rejectsOverlapBeforeCreatingArtifact() {
        val root = Files.createTempDirectory("pgp-project-overlap-test")
        root.resolve("src").createDirectories()
        root.resolve("src/first.dmp").writeText("C000: 01 02\n")
        root.resolve("src/second.dmp").writeText("C001: 03\n")
        val workspace = openWorkspace(
            root,
            """
            { "id": "first", "type": "memory-dump", "path": "src/first.dmp" },
            { "id": "second", "type": "memory-dump", "path": "src/second.dmp" }
            """.trimIndent(),
        )

        val failure = assertIs<DesktopProjectBuildResult.Failure>(
            DesktopProjectBuilder.build(workspace),
        )
        val overlap = assertIs<DesktopProjectBuildError.MemoryOverlap>(failure.errors.single())
        assertEquals("second", overlap.sourceId)
        assertEquals("first", overlap.previousSourceId)
        assertEquals(0xc001, overlap.address)
    }

    @Test
    fun reportsAssemblyAsReservedButNotImplemented() {
        val root = Files.createTempDirectory("pgp-project-assembly-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("NOP\n")
        val workspace = openWorkspace(
            root,
            """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""",
        )

        val failure = assertIs<DesktopProjectBuildResult.Failure>(
            DesktopProjectBuilder.build(workspace),
        )
        assertIs<DesktopProjectBuildError.AssemblyNotImplemented>(failure.errors.single())
    }

    private fun openWorkspace(root: java.nio.file.Path, sources: String): DesktopProjectWorkspace {
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(
            """
            {
              "format": "pgp-project",
              "formatVersion": 1,
              "name": "Build test",
              "machineId": "pc-1245",
              "sources": [$sources]
            }
            """.trimIndent(),
        )
        return assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace
    }
}
