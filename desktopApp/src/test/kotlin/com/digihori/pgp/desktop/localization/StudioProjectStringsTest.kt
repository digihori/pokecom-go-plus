package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.project.ProjectManifestError
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.message
import com.digihori.pgp.desktop.project.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class StudioProjectStringsTest {
    @Test fun typedManifestAndBuildErrorsKeepExternalValues() {
        val source = "source-id"
        val path = "src/日本語/folder with spaces/main.bas"
        val errors = listOf(
            ProjectManifestError.InvalidJson("original JSON diagnostic"),
            ProjectManifestError.UnsupportedFormat("pgp-project"), ProjectManifestError.UnsupportedVersion(99),
            ProjectManifestError.BlankName, ProjectManifestError.UnsupportedMachine("pc-unknown"),
            ProjectManifestError.InvalidSourceId(0, source), ProjectManifestError.DuplicateSourceId(source),
            ProjectManifestError.InvalidSourcePath(source, path), ProjectManifestError.DuplicateSourcePath(path),
            ProjectManifestError.InvalidLoadAddress(source, "0x10000"),
            ProjectManifestError.MissingLoadAddress(source), ProjectManifestError.UnexpectedLoadAddress(source),
        )
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            val text = DesktopProjectOpenError.InvalidManifest(errors).message(strings)
            for (value in listOf(path, source, "pc-unknown", "99", "0x10000", "loadAddress", "original JSON diagnostic")) {
                assertTrue(text.contains(value), text)
            }
            assertTrue(DesktopProjectCreateError.DestinationExists(path).message(strings).endsWith(path))
            val overlap = DesktopProjectBuildError.MemoryOverlap(source, "previous", 0xC123).message("PC-1245", strings)
            assertTrue(overlap.contains("0xC123"))
            assertTrue(overlap.contains(source))
            val assembly = DesktopProjectBuildError.Assembly(source, 42, "SC61 assembly failed (exit code -1)").message("PC-1245", strings)
            assertTrue(assembly.contains("42"))
            assertTrue(assembly.contains("-1"))
        }
    }

    @Test fun knownOperationErrorsTranslateAndOriginalLogsRemainOpaque() {
        val strings = StudioStrings(StudioLanguage.JAPANESE)
        assertEquals("日本語という名前のフォルダーは既に存在します。", strings.projectOperationError("A folder named 日本語 already exists."))
        assertEquals("SC61のアセンブルに失敗しました (終了コード 2)", strings.projectOperationError("SC61 assembly failed (exit code 2)"))
        val log = "LINE:42 ERROR CODE:7\nLIDP 0xC000\n/folder with spaces/input.asm"
        assertEquals(log, strings.projectOperationError(log))
        assertTrue(strings.projectOperationError("Supported source files are .bas, .asm and .dmp.").contains(".asm"))
    }

    @Test fun sourcesManifestAndBuildOutputsRemainIdenticalAcrossUiLanguages() {
        val root = Files.createTempDirectory("pgp-localization-project").toFile()
        try {
            val results = StudioLanguage.entries.map { language ->
                val strings = StudioStrings(language)
                strings.text("project.createTitle")
                val parent = File(root, language.name).apply { mkdirs() }
                val created = assertIs<DesktopProjectCreateResult.Success>(DesktopProjectWorkspaceCreator.create(
                    parent, "New PGP Project", MachineId("pc-1245"), setOf(DesktopProjectStarter.ASSEMBLY)))
                val workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(created.manifestFile)).workspace
                val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
                DesktopProjectBuildOutputWriter.write(workspace, built.artifact)
                built.artifact.assemblySources.forEach { it.renderMemorySummary(MachineMemoryLayout(workspace.definition.machineId), strings) }
                strings.text("project.updated", "1", "0", "")
                assertEquals("New PGP Project", workspace.definition.name)
                assertEquals("New-PGP-Project", created.manifestFile.parentFile.name)
                (listOf(created.manifestFile) + workspace.sources.map { it.file } + DesktopProjectBuildOutputWriter.outputFiles(workspace))
                    .associate { it.relativeTo(created.manifestFile.parentFile).path to it.readBytes() }
            }
            assertEquals(results[0].keys, results[1].keys)
            for (path in results[0].keys) assertContentEquals(results[0][path], results[1][path], path)
        } finally { root.deleteRecursively() }
    }

    @Test fun memoryLayoutChangesOnlyDisplayDescriptions() {
        val layout = MachineMemoryLayout(MachineId("pc-1360"))
        val original = layout.renderMemoryLayout()
        for (language in StudioLanguage.entries) {
            val shown = layout.renderMemoryLayout(StudioStrings(language))
            assertTrue(shown.contains("4000..7FFF"))
            assertTrue(shown.contains("pc-1360"))
            assertEquals(original, layout.renderMemoryLayout())
        }
    }
}
