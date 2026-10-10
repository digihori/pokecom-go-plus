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
    fun assemblesAssemblySourceIntoTheProjectArtifact() {
        val root = Files.createTempDirectory("pgp-project-assembly-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xC000\nLII 0x12\nRTN\n")
        val workspace = openWorkspace(
            root,
            """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""",
        )

        val artifact = assertIs<DesktopProjectBuildResult.Success>(
            DesktopProjectBuilder.build(workspace),
        ).artifact
        val segment = checkNotNull(artifact.memoryImage).segments.single()
        assertEquals(0xc000, segment.startAddress)
        assertContentEquals(byteArrayOf(0x00, 0x12, 0x37), segment.copyBytes())
    }

    @Test
    fun ignoresStudioSourceHeaderAndPreservesAssemblyLineNumbers() {
        val root = Files.createTempDirectory("pgp-project-assembly-header-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("# Demo\n# CALL &C000\nORG 0xC000\nJRP 0xD000\n")
        val workspace = openWorkspace(
            root,
            """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""",
        )

        val failure = assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace))
        val error = assertIs<DesktopProjectBuildError.Assembly>(failure.errors.single())
        assertEquals(4, error.line)
    }

    @Test
    fun combinesMultiplePc1360AssemblySourcesIntoOneArtifact() {
        val root = Files.createTempDirectory("pgp-project-pc1360-multi-assembly-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0x2000\nLII 0x12\nRTN\n")
        root.resolve("src/helper.asm").writeText("ORG 0x2100\nLII 0x34\nRTN\n")
        val workspace = openWorkspace(
            root,
            """
            { "id": "main", "type": "assembly", "path": "src/main.asm" },
            { "id": "helper", "type": "assembly", "path": "src/helper.asm" }
            """.trimIndent(),
            machineId = "pc-1360",
        )

        val artifact = assertIs<DesktopProjectBuildResult.Success>(
            DesktopProjectBuilder.build(workspace),
        ).artifact
        val segments = checkNotNull(artifact.memoryImage).segments

        assertEquals(listOf(0x2000, 0x2100), segments.map { it.startAddress })
        assertContentEquals(byteArrayOf(0x00, 0x12, 0x37), segments[0].copyBytes())
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x37), segments[1].copyBytes())
    }

    @Test
    fun reportsAssemblyErrorWithSourceLine() {
        val root = Files.createTempDirectory("pgp-project-assembly-error-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xC000\nJRP 0xD000\n")
        val workspace = openWorkspace(
            root,
            """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""",
        )

        val failure = assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace))
        val error = assertIs<DesktopProjectBuildError.Assembly>(failure.errors.single())
        assertEquals(2, error.line)
    }

    @Test
    fun writesDumpListingAndMapFromTheSameSuccessfulBuild() {
        val root = Files.createTempDirectory("pgp-project-assembly-output-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xC000\nSTART: LII 0x12\nRTN\n")
        val workspace = openWorkspace(
            root,
            """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""",
        )
        val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))

        val dump = checkNotNull(DesktopProjectBuildOutputWriter.write(workspace, built.artifact))

        assertEquals(root.resolve("build/program.dmp").toFile(), dump)
        assertTrue(dump.readText().contains("C000 00 12 37"))
        assertTrue(root.resolve("build/program.lst").toFile().readText().contains("START: LII 0x12"))
        assertTrue(root.resolve("build/program.map").toFile().readText().contains("C000  START  src/main.asm:2"))
    }

    private fun openWorkspace(
        root: java.nio.file.Path,
        sources: String,
        machineId: String = "pc-1245",
    ): DesktopProjectWorkspace {
        val manifest = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        manifest.writeText(
            """
            {
              "format": "pgp-project",
              "formatVersion": 1,
              "name": "Build test",
              "machineId": "$machineId",
              "sources": [$sources]
            }
            """.trimIndent(),
        )
        return assertIs<DesktopProjectOpenResult.Success>(
            DesktopProjectWorkspaceLoader.open(manifest.toFile()),
        ).workspace
    }
}
