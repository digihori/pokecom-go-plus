package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.project.*
import com.digihori.pgp.core.rom.MachineId
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopQuickAssemblyTest {
    @Test fun standaloneMatchesWorkspaceAndBuildWithoutSaving() = runBlocking {
        val root = Files.createTempDirectory("pgp-quick-").toFile()
        try {
            val file = root.resolve("main.asm")
            file.writeText("# Demo\n# CALL &C000\nORG 0xC000\nSTART: LII 7\nRTN\n")
            val layout = MachineMemoryLayout(MachineId("pc-1360"))
            val settings = DesktopAssemblerSettings()
            val source = ProjectSource("main", ProjectSourceType.ASSEMBLY, file.name)
            val workspace = DesktopProjectWorkspace(root.resolve("pgp-project.json"),
                ProjectDefinition("test", layout.machineId, listOf(source)), listOf(DesktopProjectSource(source, file)))
            val quick = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compileFile(file, layout, settings)).sources.single()
            val preview = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compileConfigured(workspace, layout, settings))
            val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, layout, compiledAssembly = preview))
            assertContentEquals(quick.image.segments.single().copyBytes(), preview.sources.single().image.segments.single().copyBytes())
            assertContentEquals(quick.image.segments.single().copyBytes(), built.artifact.memoryImage!!.segments.single().copyBytes())
            assertEquals(preview.sources.single().listing, quick.listing)
            assertEquals(preview.sources.single().symbols, quick.symbols)
            assertEquals(4, quick.listing.first().line)
            assertEquals(listOf("main.asm"), root.list()!!.toList())
            file.writeText("# Demo\n# CALL &C000\nORG 0xC000\nJRP 0xD000\n")
            val failure = assertIs<DesktopAssemblyWorkspaceResult.Failure>(DesktopAssemblyWorkspaceCompiler.compileFile(file, layout, settings))
            assertEquals(4, failure.diagnostics.single().line)
            assertEquals(file.absoluteFile, failure.diagnostics.single().file)
        } finally { root.deleteRecursively() }
    }
}
