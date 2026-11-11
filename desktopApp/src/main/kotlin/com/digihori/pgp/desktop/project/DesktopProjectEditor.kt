package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectManifestWriter
import com.digihori.pgp.core.project.ProjectSource
import com.digihori.pgp.core.project.ProjectSourceType
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.*

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

    /** Imports a batch as one manifest update; existing files and external originals stay intact. */
    fun addSources(workspace: DesktopProjectWorkspace, files: List<File>): Result<ProjectFileEdit> = runCatching {
        require(files.isNotEmpty()) { "Select an existing source file." }
        files.forEach { require(it.isFile) { "Select an existing source file." }; fileType(it.name) }
        var updated = workspace
        val copied = mutableListOf<File>()
        val paths = mutableListOf<String>()
        try {
            val directory = sourceDirectory(workspace)
            for (source in files) {
                var destination = File(directory, source.name)
                var suffix = 2
                while (destination.exists() || updated.sources.any { it.file.canonicalFile == destination.canonicalFile }) {
                    destination = File(directory, "${source.nameWithoutExtension}-${suffix++}.${source.extension}")
                }
                Files.newOutputStream(destination.toPath(), java.nio.file.StandardOpenOption.CREATE_NEW,
                    java.nio.file.StandardOpenOption.WRITE).use { output ->
                    copied += destination
                    source.inputStream().use { it.copyTo(output) }
                }
                val path = "src/${destination.name}"
                paths += path
                updated = register(updated, destination, path)
            }
            DesktopProjectManifestUpdater.writeManifest(workspace, ProjectManifestWriter.write(updated.definition))
        } catch (error: Exception) {
            copied.asReversed().forEach { file ->
                try { Files.deleteIfExists(file.toPath()) } catch (rollback: Exception) { error.addSuppressed(rollback) }
            }
            throw error
        }
        ProjectFileEdit(updated, paths)
    }

    fun addSource(workspace: DesktopProjectWorkspace, source: File): Result<DesktopProjectWorkspace> =
        addSources(workspace, listOf(source)).map { it.workspace }

    fun availableFileName(workspace: DesktopProjectWorkspace, extension: String): String {
        val directory = File(workspace.manifestFile.parentFile, "src")
        var name = "new.$extension"
        var suffix = 2
        while (fileNameTaken(workspace, File(directory, name))) {
            name = "new-${suffix++}.$extension"
        }
        return name
    }

    private fun fileNameTaken(workspace: DesktopProjectWorkspace, file: File): Boolean =
        file.exists() || workspace.sources.any { it.file.canonicalFile == file.canonicalFile }

    fun createFile(workspace: DesktopProjectWorkspace, name: String, other: Boolean = false): Result<ProjectFileEdit> = runCatching {
        val filename = validName(name)
        if (!other) fileType(filename)
        val destination = File(sourceDirectory(workspace), filename)
        require(!fileNameTaken(workspace, destination)) {
            "A file with that name already exists."
        }
        val path = "src/$filename"
        val updated = register(workspace, destination, path, allowUnknown = other)
        val content = if (other) "" else when (destination.extension.lowercase()) {
            "bas" -> "10 END\n"
            "asm" -> {
                val address = com.digihori.pgp.core.api.MachineMemoryLayout(workspace.definition.machineId).assemblyStartAddress
                "ORG 0x${address.toString(16).uppercase()}\nRTN\n"
            }
            "pgmacro" -> "# Studio macro\n"
            else -> ""
        }
        var created = false
        try {
            Files.newOutputStream(destination.toPath(), java.nio.file.StandardOpenOption.CREATE_NEW,
                java.nio.file.StandardOpenOption.WRITE).use { output ->
                created = true
                output.write(content.toByteArray(Charsets.UTF_8))
            }
            DesktopProjectManifestUpdater.writeManifest(workspace, ProjectManifestWriter.write(updated.definition))
        }
        catch (error: Exception) {
            if (created) try { Files.deleteIfExists(destination.toPath()) } catch (rollback: Exception) { error.addSuppressed(rollback) }
            throw error
        }
        ProjectFileEdit(updated, listOf(path))
    }

    /** Renames a file in place, retaining source identity, type and load address. */
    fun renameFile(workspace: DesktopProjectWorkspace, path: String, name: String): Result<ProjectFileEdit> = runCatching {
        val filename = validName(name)
        val root = workspace.manifestFile.parentFile.canonicalFile
        val source = File(root, path).absoluteFile.normalize()
        require(source.canonicalFile.toPath().startsWith(root.toPath()) && source.isFile &&
            (workspace.sources.any { it.definition.path == path } ||
                source.toPath().startsWith(File(root, "src").toPath()))) { "Select a file inside the project." }
        require(filename.substringAfterLast('.', "").equals(source.extension, ignoreCase = true)) {
            "Keep the existing file extension."
        }
        val destination = File(source.parentFile, filename)
        val newPath = destination.relativeTo(root).invariantSeparatorsPath
        if (destination == source) return@runCatching ProjectFileEdit(workspace, listOf(path))
        require(!destination.exists() && workspace.sources.none { it.definition.path == newPath }) {
            "A file with that name already exists."
        }
        val sources = workspace.sources.map {
            if (it.definition.path == path) it.copy(definition = it.definition.copy(path = newPath), file = destination) else it
        }
        val updated = workspace.copy(definition = workspace.definition.copy(sources = sources.map { it.definition }), sources = sources)
        Files.move(source.toPath(), destination.toPath())
        try { DesktopProjectManifestUpdater.writeManifest(workspace, ProjectManifestWriter.write(updated.definition)) }
        catch (error: Exception) {
            try { Files.move(destination.toPath(), source.toPath()) } catch (rollback: Exception) { error.addSuppressed(rollback) }
            throw error
        }
        ProjectFileEdit(updated, listOf(newPath))
    }

    /** Moves a file and its recovery metadata together; does not erase emulator memory. */
    fun trashFile(workspace: DesktopProjectWorkspace, path: String): Result<ProjectTrashEdit> = runCatching {
        val root = workspace.manifestFile.parentFile.canonicalFile
        val source = File(root, path).absoluteFile.normalize()
        require(path == source.relativeTo(root).invariantSeparatorsPath &&
            source.canonicalFile.toPath().startsWith(root.toPath()) && source.isFile &&
            !Files.isSymbolicLink(source.toPath()) &&
            (workspace.sources.any { it.definition.path == path } || source.toPath().startsWith(File(root, "src").toPath())) &&
            !source.toPath().startsWith(File(root, ".trash").toPath()) &&
            !source.canonicalFile.toPath().startsWith(File(root, ".trash").toPath()) && source != workspace.manifestFile.canonicalFile) {
            "Select a file inside the project."
        }
        val trash = File(root, ".trash")
        require(trash.canonicalFile == trash) { "The project trash must be inside the project." }
        Files.createDirectories(trash.toPath())
        val prefix = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-"))
        val entry = Files.createTempDirectory(trash.toPath(), prefix).toFile()
        val destination = File(entry, path)
        val remaining = workspace.sources.filterNot { it.definition.path == path }
        val removed = workspace.definition.sources.filter { it.path == path }
        val updated = workspace.copy(definition = workspace.definition.copy(sources = remaining.map { it.definition }), sources = remaining)
        var moved = false
        try {
            val original = Json.parseToJsonElement(ProjectManifestWriter.write(workspace.definition.copy(sources = removed)))
                .jsonObject.getValue("sources").jsonArray.singleOrNull() ?: JsonNull
            File(entry, "entry.json").writeText(buildJsonObject {
                put("originalPath", path)
                put("movedAt", java.time.Instant.now().toString())
                put("source", original)
            }.toString(), Charsets.UTF_8)
            Files.createDirectories(destination.parentFile.toPath())
            Files.move(source.toPath(), destination.toPath())
            moved = true
            DesktopProjectManifestUpdater.writeManifest(workspace, ProjectManifestWriter.write(updated.definition))
        } catch (error: Exception) {
            if (moved) {
                try { Files.move(destination.toPath(), source.toPath()); moved = false }
                catch (rollback: Exception) { error.addSuppressed(rollback) }
            }
            // Preserve the recovery folder if rollback failed: it may hold the only copy.
            if (!moved) try { entry.deleteRecursively() } catch (rollback: Exception) { error.addSuppressed(rollback) }
            throw error
        }
        ProjectTrashEdit(ProjectFileEdit(updated, emptyList()), destination)
    }

    private fun validName(name: String): String = name.trim().also {
        require(it.isNotEmpty() && !it.startsWith('.') && !it.endsWith('.') && !it.endsWith(' ') &&
            it.none { char -> char.isISOControl() || char in "<>:\"/\\|?*" }) { "Enter a valid file name without folders." }
    }

    private fun fileType(name: String, allowUnknown: Boolean = false): ProjectSourceType? = when (File(name).extension.lowercase()) {
        "bas" -> ProjectSourceType.BASIC
        "asm" -> ProjectSourceType.ASSEMBLY
        "dmp" -> ProjectSourceType.MEMORY_DUMP
        "pgmacro" -> null // Auxiliary file: visible in the tree, never part of a Build.
        else -> if (allowUnknown) null else error("Supported project files are .bas, .asm, .dmp and .pgmacro.")
    }

    private fun sourceDirectory(workspace: DesktopProjectWorkspace): File {
        val root = workspace.manifestFile.parentFile.canonicalFile
        val directory = File(root, "src")
        require(directory.canonicalFile.parentFile == root) { "The src directory must be inside the project." }
        Files.createDirectories(directory.toPath())
        return directory
    }

    private fun register(workspace: DesktopProjectWorkspace, file: File, path: String, allowUnknown: Boolean = false): DesktopProjectWorkspace {
        val type = fileType(file.name, allowUnknown) ?: return workspace
        val base = file.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "-")
            .trim('-', '_').let { if (it.firstOrNull()?.isLetter() == true) it else "source-$it" }
        val ids = workspace.sources.map { it.definition.id }.toSet()
        var id = base
        var suffix = 2
        while (id in ids) id = "$base-${suffix++}"
        val definition = ProjectSource(id, type, path)
        return workspace.copy(definition = workspace.definition.copy(sources = workspace.definition.sources + definition),
            sources = workspace.sources + DesktopProjectSource(definition, file))
    }
}

internal data class ProjectFileEdit(val workspace: DesktopProjectWorkspace, val paths: List<String>)
internal data class ProjectTrashEdit(val edit: ProjectFileEdit, val destination: File)
