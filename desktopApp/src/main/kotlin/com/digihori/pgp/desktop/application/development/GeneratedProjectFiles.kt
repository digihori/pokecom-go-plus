package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.project.*
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.project.*
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*

internal data class GeneratedSource(val id: String, val type: ProjectSourceType, val text: String) {
    val definition: ProjectSource get() = ProjectSource(id, type, "src/$id.${if (type == ProjectSourceType.BASIC) "bas" else "asm"}")
}

/** Writes only reviewed, server-named files. No input path or recursive directory deletion. */
internal object GeneratedProjectFiles {
    val MACHINES = listOf("pc-1245", "pc-1251")
    fun validate(sources: List<GeneratedSource>) {
        if (sources.size !in 1..8 || sources.map { it.id }.distinct().size != sources.size) fail("INVALID_SOURCES")
        if (sources.sumOf { it.text.toByteArray(Charsets.UTF_8).size } > 32768) fail("LIMIT_EXCEEDED")
        for (source in sources) {
            if (!source.id.matches(Regex("[a-z][a-z0-9_-]{0,31}")) ||
                source.id.matches(Regex("con|prn|aux|nul|com[1-9]|lpt[1-9]"))) fail("INVALID_SOURCE_ID")
            if (source.type !in setOf(ProjectSourceType.BASIC, ProjectSourceType.ASSEMBLY)) fail("TEXT_PROJECT_ONLY")
            if (source.text.isBlank() || source.text.any { it < ' ' && it !in "\t\r\n" } ||
                source.text.any { it.isSurrogate() } && !validSurrogates(source.text)) fail("INVALID_TEXT")
        }
    }

    private fun validSurrogates(text: String): Boolean {
        var at = 0
        while (at < text.length) {
            val char = text[at++]
            if (char.isHighSurrogate()) { if (at == text.length || !text[at++].isLowSurrogate()) return false }
            else if (char.isLowSurrogate()) return false
        }
        return true
    }

    fun create(parent: Path, folder: String, name: String, sources: List<GeneratedSource>, machineId: String = "pc-1245"): DesktopProjectWorkspace {
        validate(sources)
        if (machineId !in MACHINES) fail("UNSUPPORTED_GENERATION_MACHINE")
        if (!Files.isDirectory(parent, NOFOLLOW_LINKS) || Files.isSymbolicLink(parent)) fail("DESTINATION_CHANGED")
        val root = parent.resolve(folder)
        if (Files.exists(root, NOFOLLOW_LINKS)) fail("DESTINATION_EXISTS")
        val definition = ProjectDefinition(name, MachineId(machineId), sources.map { it.definition })
        val manifest = ProjectManifestWriter.write(definition)
        val created = mutableListOf<Path>()
        try {
            Files.createDirectory(root); created.add(root)
            for (directory in listOf(root.resolve("src"), root.resolve("build"))) {
                Files.createDirectory(directory); created.add(directory)
            }
            for (source in sources) {
                val path = root.resolve(source.definition.path)
                writeNew(path, source.text, created)
            }
            val path = root.resolve(DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
            writeNew(path, manifest, created)
            return workspace(path.toFile(), definition)
        } catch (error: Exception) {
            // Only remove entries created by this operation; never recurse into unrelated content.
            created.asReversed().forEach { runCatching { Files.deleteIfExists(it) } }
            throw error
        }
    }

    fun checkAddition(project: DesktopProjectWorkspace, source: GeneratedSource) {
        validate(listOf(source))
        if (project.definition.machineId.value !in MACHINES) fail("UNSUPPORTED_GENERATION_MACHINE")
        if (project.sources.size >= 64) fail("LIMIT_EXCEEDED")
        if (project.sources.any { it.definition.id.equals(source.id, true) || it.definition.path.equals(source.definition.path, true) }) fail("SOURCE_EXISTS")
        val root = project.manifestFile.parentFile.toPath().toRealPath()
        val directory = root.resolve("src")
        if (Files.exists(directory, NOFOLLOW_LINKS) && (!Files.isDirectory(directory, NOFOLLOW_LINKS) || Files.isSymbolicLink(directory))) fail("LINK_NOT_ALLOWED")
        if (Files.exists(root.resolve(source.definition.path), NOFOLLOW_LINKS)) fail("SOURCE_EXISTS")
    }

    fun add(project: DesktopProjectWorkspace, source: GeneratedSource, checkRevision: () -> Unit): DesktopProjectWorkspace {
        checkAddition(project, source)
        val root = project.manifestFile.parentFile.toPath().toRealPath()
        val directory = root.resolve("src")
        val target = root.resolve(source.definition.path)
        val definition = project.definition.copy(sources = project.definition.sources + source.definition)
        val text = ProjectManifestWriter.write(definition)
        val created = mutableListOf<Path>()
        var temporary: Path? = null
        var committed = false
        try {
            if (!Files.exists(directory, NOFOLLOW_LINKS)) { Files.createDirectory(directory); created.add(directory) }
            writeNew(target, source.text, created)
            temporary = Files.createTempFile(root, ".pgp-generated-", ".tmp")
            Files.writeString(temporary, text)
            checkRevision()
            if (Files.isSymbolicLink(directory) || target.toRealPath().parent != directory ||
                Files.isSymbolicLink(target) || !Files.isRegularFile(target, NOFOLLOW_LINKS) ||
                !Files.newInputStream(target, NOFOLLOW_LINKS).use { it.readNBytes(32769) }.contentEquals(source.text.toByteArray(Charsets.UTF_8))) fail("DESTINATION_CHANGED")
            Files.move(temporary, project.manifestFile.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
            committed = true
            return workspace(project.manifestFile, definition)
        } finally {
            temporary?.let { Files.deleteIfExists(it) }
            if (!committed) created.asReversed().forEach { runCatching { Files.deleteIfExists(it) } }
        }
    }

    private fun writeNew(path: Path, text: String, created: MutableList<Path>) {
        Files.newOutputStream(path, CREATE_NEW, WRITE, NOFOLLOW_LINKS).use {
            created.add(path)
            it.write(text.toByteArray(Charsets.UTF_8))
        }
    }
    private fun workspace(file: File, definition: ProjectDefinition) = DesktopProjectWorkspace(file, definition,
        definition.sources.map { DesktopProjectSource(it, File(file.parentFile, it.path)) })
    private fun fail(code: String): Nothing = throw DevelopmentReadError(code)
}
