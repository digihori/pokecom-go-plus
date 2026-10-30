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
import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.MonotonicClock

class DesktopProjectBuilderTest {
    @Test fun buildsBasicForSelectedMachineRegardlessOfManifestMachine() {
        val root = Files.createTempDirectory("pgp-cross-machine-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.bas").writeText("10 PRINT \"HELLO\"\n20 END\n")
            val workspace = openWorkspace(root, """{ "id": "main", "type": "basic", "path": "src/main.bas" }""", "pc-1245")
            val target = MachineId("pc-1261")
            val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, MachineMemoryLayout(target), validateForLoad = true))
            assertEquals(target, built.artifact.targetMachineId)
            val expected = assertIs<com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult.Success>(
                com.digihori.pgp.desktop.basic.DesktopBasicLoader.compileProgram(workspace.sources.single().file.readBytes(), target))
            assertContentEquals(expected.bytes, built.artifact.copyBasicProgram())
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun crossMachineAssemblyStillChecksTargetRamBeforeLoad() {
        val root = Files.createTempDirectory("pgp-cross-placement-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText("ORG 0xC500\nRTN\n")
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""", "pc-1245")
            val layout = MachineMemoryLayout(MachineId("pc-1261"))
            val assembly = DesktopAssemblyWorkspaceCompiler.compile(workspace, layout, validatePlacement = false)
            assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = assembly))
            assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, layout, validateForLoad = true, compiledAssembly = assembly))
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun buildsUnmappedPc1261OutputButRejectsLoadWithoutPartialWrites() = kotlinx.coroutines.runBlocking {
        val root = Files.createTempDirectory("pgp-build-unmapped-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText("ORG $4000\nDB $37\nORG $6800\nDB \$FF\n")
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""", "pc-1261")
            val layout = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId)
            val compiled = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, DesktopAssemblerSettings())
            val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = compiled))
            val dump = DesktopProjectBuildOutputWriter.write(workspace, built.artifact)!!
            assertTrue(dump.readText().contains("6800 FF"))
            val id = workspace.definition.machineId
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x8000)),
            )))).session
            val before = session.memorySnapshot(0x4000, 1).copyBytes()
            val runner = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            val failure = assertIs<DesktopProjectApplyResult.MemoryFailure>(DesktopProjectArtifactLoader.load(runner, built.artifact))
            assertEquals(0x6800, failure.error.address)
            assertContentEquals(before, session.memorySnapshot(0x4000, 1).copyBytes())
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun disassemblesAcrossAdjacentDumpRowsButKeepsAddressGaps() {
        val root = Files.createTempDirectory("pgp-dump-disassembly-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText("ORG $4000\nRTN\n")
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""", "pc-1350")
            fun disassemble(dump: String): String {
                val image = assertIs<com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult.Success>(
                    com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader.parse(dump.encodeToByteArray())).image
                return DesktopAssemblySourceResult(workspace.sources.single(), image, emptyList(), emptyList()).renderDisassembly()
            }
            val joined = disassemble("4000:333333333333337A\n4008:014000\n")
            assertTrue(joined.contains("CASE1 0x014000"), joined)
            val gap = disassemble("4000:333333333333337A\n4010:014000\n")
            assertTrue(gap.contains("DB 0x7A"), gap)
            assertTrue(gap.contains("ORG 0x4010"), gap)
        } finally { root.toFile().deleteRecursively() }
    }
    private fun externalSettings(): DesktopAssemblerSettings {
        val classpath = listOf(Sc61ProcessFixture::class.java, Unit::class.java).joinToString(java.io.File.pathSeparator) {
            java.io.File(it.protectionDomain.codeSource.location.toURI()).absolutePath
        }
        return DesktopAssemblerSettings(true, java.io.File(System.getProperty("java.home"), "bin/java").path,
            "-cp \"$classpath\" ${Sc61ProcessFixture::class.java.name} {source}")
    }

    @Test fun buildsExternalOutputAndRejectsExternalFailureWithoutOverwritingBuild() = kotlinx.coroutines.runBlocking {
        val root = Files.createTempDirectory("pgp-external-build-")
        try {
            root.resolve("src").createDirectories()
            val source = root.resolve("src/main.asm").apply { writeText("external-only syntax") }
            root.resolve("src/data.bin").writeBytes(byteArrayOf(0x42))
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" }""", "pc-1350")
            val layout = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId)
            val assembly = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, externalSettings())
            val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = assembly))
            assertContentEquals(byteArrayOf(0x42), built.artifact.memoryImage!!.segments.single().copyBytes())
            assertTrue(built.artifact.assemblySources.single().consoleLog.contains("NO ERRORS FOUND"))
            val machine = workspace.definition.machineId
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, RomSet(machine, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x8000)),
            )))).session
            val runner = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            assertIs<DesktopProjectApplyResult.Success>(DesktopProjectArtifactLoader.load(runner, built.artifact))
            assertContentEquals(byteArrayOf(0x42), session.memorySnapshot(0x4000, 1).copyBytes())
            val output = DesktopProjectBuildOutputWriter.write(workspace, built.artifact)!!
            assertTrue(java.io.File(output.parentFile, "main.lst").readText().contains("ASSEMBLY LIST OF"))
            assertTrue(java.io.File(output.parentFile, "main.map").readText().contains("RAW"))
            val bytes = output.readBytes()
            source.writeText("fail")
            val failure = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, externalSettings())
            assertEquals(1, assertIs<DesktopAssemblyWorkspaceResult.Failure>(failure).diagnostics.single().line)
            assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = failure))
            assertContentEquals(bytes, output.readBytes())
            source.writeText("ORG $4000\nDB $37")
            assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, DesktopAssemblerSettings()))
            Unit
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun rejectsOverlapBetweenExternalAssemblyAndOtherInputs() = kotlinx.coroutines.runBlocking {
        val root = Files.createTempDirectory("pgp-external-overlap-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText("external-only syntax")
            root.resolve("src/data.bin").writeBytes(byteArrayOf(0x42))
            root.resolve("src/other.dmp").writeText("4000:37")
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" },
                { "id": "other", "type": "memory-dump", "path": "src/other.dmp" }""", "pc-1350")
            val layout = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId)
            val assembly = DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, externalSettings())
            assertTrue(assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = assembly)).errors.any {
                it is DesktopProjectBuildError.MemoryOverlap
            })
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun rejectsOverlapBetweenExternalAssemblySources() = kotlinx.coroutines.runBlocking {
        val root = Files.createTempDirectory("pgp-external-source-overlap-")
        try {
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText("external-only syntax")
            root.resolve("src/other.asm").writeText("external-only syntax")
            root.resolve("src/data.bin").writeBytes(byteArrayOf(0x42))
            val workspace = openWorkspace(root, """{ "id": "main", "type": "assembly", "path": "src/main.asm" },
                { "id": "other", "type": "assembly", "path": "src/other.asm" }""", "pc-1350")
            val layout = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId)
            val result = assertIs<DesktopAssemblyWorkspaceResult.Failure>(DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, externalSettings()))
            assertTrue(result.diagnostics.single().message.contains("overlaps"))
        } finally { root.toFile().deleteRecursively() }
    }
    @Test
    fun buildsBasicDumpAndRawBinaryAsOneArtifact() {
        val root = Files.createTempDirectory("pgp-project-build-test")
        root.resolve("src").createDirectories()
        root.resolve("src/main.bas").writeText("10 PRINT \"HELLO\"\n")
        root.resolve("src/routine.dmp").writeText("C500: 01 02\n")
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
              "loadAddress": "0xC540"
            }
            """.trimIndent(),
        )

        val artifact = assertIs<DesktopProjectBuildResult.Success>(
            DesktopProjectBuilder.build(workspace),
        ).artifact

        assertTrue(checkNotNull(artifact.copyBasicProgram()).isNotEmpty())
        val segments = checkNotNull(artifact.memoryImage).segments
        assertEquals(listOf(0xc500, 0xc540), segments.map { it.startAddress })
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

        assertEquals(root.resolve("build/main.dmp").toFile(), dump)
        assertTrue(dump.readText().contains("C000 00 12 37"))
        assertTrue(root.resolve("build/main.lst").toFile().readText().contains("START: LII 0x12"))
        assertTrue(root.resolve("build/main.map").toFile().readText().contains("C000  START  src/main.asm:2"))
    }

    @Test
    fun permitsBuildButLoadPreflightRejectsBasicOverlapAndPhysicalAliases() {
        for ((machine, source) in listOf(
            "pc-1245" to "C000: AA\n", "pc-1245" to "C6E1: AA\n",
            "pc-1245" to "C100: AA\nD100: BB\n", "pc-1401" to "6000: AA\n7000: BB\n",
        )) {
            val root = Files.createTempDirectory("pgp-build-placement")
            root.resolve("src").createDirectories()
            root.resolve("src/main.bas").writeText("10 PRINT \"HELLO\"\n")
            root.resolve("src/data.dmp").writeText(source)
            val workspace = openWorkspace(root, """
                { "id": "basic", "type": "basic", "path": "src/main.bas" },
                { "id": "data", "type": "memory-dump", "path": "src/data.dmp" }
            """.trimIndent(), machine)
            assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
            val error = assertIs<DesktopProjectBuildError.Placement>(
                assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, validateForLoad = true)).errors.single(),
            )
            assertEquals("data", error.sourceId)
            if (source.contains("D100") || source.contains("7000")) assertIs<com.digihori.pgp.core.api.MemoryImageLoadError.MirrorOverlap>(error.error)
            else assertIs<com.digihori.pgp.core.api.MemoryImageLoadError.BasicOverlap>(error.error)
        }
    }

    @Test
    fun workspaceReportsRomPlacementAtTheActualSourceLineAndDoesNotOverwriteOutputs() {
        val root = Files.createTempDirectory("pgp-workspace-placement")
        root.resolve("src").createDirectories()
        val source = root.resolve("src/main.asm")
        source.writeText("# Demo\nORG 0x3800\nSTART: RTN\n")
        val workspace = openWorkspace(root, """{ "id": "asm", "type": "assembly", "path": "src/main.asm" }""", "pc-1401")
        val success = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        DesktopProjectBuildOutputWriter.write(workspace, success.artifact)
        val outputs = listOf("main.dmp", "main.lst", "main.map").associateWith { root.resolve("build/$it").toFile().readBytes() }
        source.writeText("# Demo\nORG 0xC000\nSTART: RTN\n")
        val diagnostic = assertIs<DesktopAssemblyWorkspaceResult.Failure>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).diagnostics.single()
        assertEquals(3, diagnostic.line)
        assertTrue(diagnostic.message.contains("ROM"))
        assertTrue(diagnostic.message.contains("pc-1401"))
        assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, validateForLoad = true))
        assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        outputs.forEach { (name, bytes) -> assertContentEquals(bytes, root.resolve("build/$name").toFile().readBytes()) }
    }

    @Test
    fun workspaceRejectsCrossSourceAliasesAndShowsMemoryKinds() {
        val root = Files.createTempDirectory("pgp-workspace-alias")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xC100\nRTN\n")
        root.resolve("src/helper.asm").writeText("ORG 0xD100\nRTN\n")
        val workspace = openWorkspace(root, """
            { "id": "main", "type": "assembly", "path": "src/main.asm" },
            { "id": "helper", "type": "assembly", "path": "src/helper.asm" }
        """.trimIndent())
        val error = assertIs<DesktopAssemblyWorkspaceResult.Failure>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).diagnostics.single()
        assertEquals(2, error.line)
        assertTrue(error.message.contains("src/main.asm"))
        root.resolve("src/helper.asm").writeText("ORG 0xF800\nDB 1\n")
        val compiled = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace))
        val layout = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId)
        assertTrue(compiled.sources[0].renderMemorySummary(layout).contains("RAM"))
        assertTrue(compiled.sources[1].renderMemorySummary(layout).contains("LCD"))
    }

    @Test
    fun loadPreflightChecksTheSelectedHardwareRamMode() {
        val root = Files.createTempDirectory("pgp-build-ram-mode")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xA000\nRTN\n")
        val workspace = openWorkspace(root, """{ "id": "asm", "type": "assembly", "path": "src/main.asm" }""", "pc-1250")
        assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace,
            com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId, com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode.HARDWARE),
            validateForLoad = true,
        ))
    }

    @Test
    fun buildsExtendedAssemblyDataAndRendersTypedConstantsFromTheSameResult() {
        val root = Files.createTempDirectory("pgp-extended-assembly")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("""
            # Demo
            BASE EQU 0xC500
            VALUE EQU -1
            ORG BASE
            START: LIA LOW(TABLE)
            DW TABLE
            DB "A;B", '\xFF'
            DS 2, 0xAA
            ALIGN 16
            TABLE: RTN
        """.trimIndent())
        val workspace = openWorkspace(root, """{ "id": "asm", "type": "assembly", "path": "src/main.asm" }""")
        val preview = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).sources.single()
        val artifact = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace)).artifact
        assertContentEquals(preview.image.segments.single().copyBytes(), checkNotNull(artifact.memoryImage).segments.single().copyBytes())
        assertEquals(17, preview.image.byteCount)
        assertTrue(listOf(preview).renderMap().contains("BASE EQU 50432"))
        assertTrue(listOf(preview).renderMap().contains("VALUE EQU -1"))
        assertTrue(listOf(preview).renderMap().contains("C500  START"))
        assertTrue(preview.renderListing().contains("DS 2, 0xAA"))
        DesktopProjectBuildOutputWriter.write(workspace, artifact)
        assertTrue(root.resolve("build/main.map").toFile().readText().contains("VALUE EQU -1"))
    }

    @Test
    fun reservationsAndAlignmentAreIncludedInPlacementAndOverlapChecks() {
        for (source in listOf(
            "ORG 0x47FF\nDS 2", "ORG 0x47FF\nALIGN 4096",
            "ORG 0x6000\nDS 257", "ORG 0x3800\nDS 2\nORG 0x3801\nDB 1",
        )) {
            val root = Files.createTempDirectory("pgp-extended-placement")
            root.resolve("src").createDirectories()
            root.resolve("src/main.asm").writeText(source)
            val workspace = openWorkspace(root, """{ "id": "asm", "type": "assembly", "path": "src/main.asm" }""", "pc-1401")
            assertIs<DesktopAssemblyWorkspaceResult.Failure>(DesktopAssemblyWorkspaceCompiler.compile(workspace), source)
            assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, validateForLoad = true), source)
        }
        val root = Files.createTempDirectory("pgp-ds-basic-overlap")
        root.resolve("src").createDirectories()
        root.resolve("src/main.asm").writeText("ORG 0xC000\nDS 16")
        root.resolve("src/main.bas").writeText("10 PRINT \"HELLO\"")
        val workspace = openWorkspace(root, """
            { "id": "asm", "type": "assembly", "path": "src/main.asm" },
            { "id": "basic", "type": "basic", "path": "src/main.bas" }
        """.trimIndent())
        assertIs<com.digihori.pgp.core.api.MemoryImageLoadError.BasicOverlap>(
            assertIs<DesktopProjectBuildError.Placement>(assertIs<DesktopProjectBuildResult.Failure>(DesktopProjectBuilder.build(workspace, validateForLoad = true)).errors.single()).error,
        )
    }

    @Test
    fun outputNamesFollowSingleAssemblySourceAndPreviewDoesNotWriteFiles() {
        val root = Files.createTempDirectory("pgp-output-names")
        root.resolve("src").createDirectories()
        root.resolve("src/pacman.asm").writeText("ORG 0xC500\nSTART: RTN\n")
        val workspace = openWorkspace(root, """{ "id": "asm", "type": "assembly", "path": "src/pacman.asm" }""")
        val files = DesktopProjectBuildOutputWriter.outputFiles(workspace)
        assertEquals(listOf("pacman.dmp", "pacman.lst", "pacman.map"), files.map { it.name })
        assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace))
        assertTrue(files.none { it.exists() })
        val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        assertEquals(files.first(), DesktopProjectBuildOutputWriter.write(workspace, built.artifact))
        assertTrue(files.all { it.isFile })
        assertTrue(files[1].readText().contains("START: RTN"))
        assertTrue(files[2].readText().contains("START"))
        val multiple = workspace.copy(sources = workspace.sources + workspace.sources.single().copy(
            file = root.resolve("src/other.asm").toFile(),
        ))
        assertEquals(listOf("program.dmp", "program.lst", "program.map"),
            DesktopProjectBuildOutputWriter.outputFiles(multiple).map { it.name })
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
