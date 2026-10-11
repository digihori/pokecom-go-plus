package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectManifestWriter
import com.digihori.pgp.core.project.ProjectSource
import com.digihori.pgp.core.project.ProjectSourceType
import java.io.File
import java.nio.file.Files

internal object DesktopProjectEditor {
    /** Renames the entire project directory and persists the display name together. */
    fun rename(workspace: DesktopProjectWorkspace, name: String): Result<DesktopProjectWorkspace> = runCatching {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Project name must not be blank." }
        val root = workspace.manifestFile.parentFile.canonicalFile
        val parent = requireNotNull(root.parentFile) { "The filesystem root cannot be renamed." }
        val destination = File(parent, DesktopProjectWorkspaceCreator.folderName(trimmed))
        require(!destination.exists() || Files.isSameFile(root.toPath(), destination.toPath())) {
            "A folder named ${destination.name} already exists."
        }
        val updated = workspace.copy(
            manifestFile = File(destination, workspace.manifestFile.name),
            definition = workspace.definition.copy(name = trimmed),
            sources = workspace.sources.map { it.copy(file = File(destination, it.definition.path)) },
        )
        val text = ProjectManifestWriter.write(updated.definition)
        val moved = root.path != destination.path
        if (moved) Files.move(root.toPath(), destination.toPath())
        try {
            DesktopProjectManifestUpdater.writeManifest(updated, text)
        } catch (error: Exception) {
            if (moved) {
                try { Files.move(destination.toPath(), root.toPath()) }
                catch (rollback: Exception) { error.addSuppressed(rollback) }
            }
            throw error
        }
        updated
    }

    /** Copies only the selected file and registers it, without synchronizing unrelated files. */
    fun addSource(workspace: DesktopProjectWorkspace, source: File): Result<DesktopProjectWorkspace> = runCatching {
        require(source.isFile) { "Select an existing source file." }
        val type = when (source.extension.lowercase()) {
            "bas" -> ProjectSourceType.BASIC
            "asm" -> ProjectSourceType.ASSEMBLY
            "dmp" -> ProjectSourceType.MEMORY_DUMP
            else -> error("Supported source files are .bas, .asm and .dmp.")
        }
        val root = workspace.manifestFile.parentFile.canonicalFile
        val directory = File(root, "src")
        require(directory.canonicalFile.parentFile == root) { "The src directory must be inside the project." }
        Files.createDirectories(directory.toPath())
        val reserved = workspace.sources.map { it.file.canonicalPath }.toSet()
        var destination = File(directory, source.name)
        var suffix = 2
        while (destination.exists() || destination.canonicalPath in reserved) {
            destination = File(directory, "${source.nameWithoutExtension}-${suffix++}.${source.extension}")
        }
        val baseId = source.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "-")
            .trim('-', '_').let { if (it.firstOrNull()?.isLetter() == true) it else "source-$it" }
        val ids = workspace.definition.sources.map { it.id }.toSet()
        var id = baseId
        suffix = 2
        while (id in ids) id = "$baseId-${suffix++}"
        val definition = ProjectSource(id, type, "src/${destination.name}")
        val updated = workspace.copy(
            definition = workspace.definition.copy(sources = workspace.definition.sources + definition),
            sources = workspace.sources + DesktopProjectSource(definition, destination),
        )
        val text = ProjectManifestWriter.write(updated.definition)
        Files.copy(source.toPath(), destination.toPath())
        try {
            DesktopProjectManifestUpdater.writeManifest(workspace, text)
        } catch (error: Exception) {
            Files.deleteIfExists(destination.toPath())
            throw error
        }
        updated
    }
}
