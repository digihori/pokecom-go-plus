package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectManifestWriter
import com.digihori.pgp.core.project.ProjectSource
import com.digihori.pgp.core.project.ProjectSourceType
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal sealed interface DesktopProjectUpdateResult {
    data class Success(
        val addedCount: Int,
        val removedCount: Int,
        val skippedPaths: List<String>,
    ) : DesktopProjectUpdateResult
    data class CouldNotWrite(val message: String) : DesktopProjectUpdateResult
}

internal object DesktopProjectManifestUpdater {
    fun synchronize(
        workspace: DesktopProjectWorkspace,
        tree: DesktopProjectTreeSnapshot,
    ): DesktopProjectUpdateResult {
        val retained = workspace.sources.filter { it.file.isFile }.map { it.definition }
        val existingIds = retained.mapTo(mutableSetOf()) { it.id }
        val skippedPaths = mutableListOf<String>()
        val additions = tree.untracked.mapNotNull { file ->
            val type = when (file.file.extension.lowercase()) {
                "bas" -> ProjectSourceType.BASIC
                "dmp" -> ProjectSourceType.MEMORY_DUMP
                "asm" -> ProjectSourceType.ASSEMBLY
                else -> {
                    skippedPaths += file.relativePath
                    return@mapNotNull null
                }
            }
            ProjectSource(uniqueId(file, existingIds), type, file.relativePath)
        }
        val removedCount = workspace.definition.sources.size - retained.size
        val updatedSources = retained + additions
        if (removedCount == 0 && additions.isEmpty()) {
            return DesktopProjectUpdateResult.Success(0, 0, skippedPaths)
        }
        val updated = workspace.definition.copy(sources = updatedSources)
        val text = runCatching { ProjectManifestWriter.write(updated) }.getOrElse {
            return DesktopProjectUpdateResult.CouldNotWrite(it.message ?: it::class.simpleName.orEmpty())
        }
        return runCatching {
            writeManifest(workspace, text)
            DesktopProjectUpdateResult.Success(additions.size, removedCount, skippedPaths)
        }.getOrElse {
            DesktopProjectUpdateResult.CouldNotWrite(it.message ?: it::class.simpleName.orEmpty())
        }
    }

    private fun uniqueId(file: DesktopProjectTreeFile, existingIds: MutableSet<String>): String {
        val baseId = file.file.nameWithoutExtension
            .replace(Regex("[^a-zA-Z0-9_-]"), "-")
            .trim('-', '_')
            .let { if (it.firstOrNull()?.isLetter() == true) it else "source-$it" }
            .ifBlank { "source" }
        var id = baseId
        var suffix = 2
        while (id in existingIds) id = "$baseId-${suffix++}"
        existingIds += id
        return id
    }

    private fun writeManifest(workspace: DesktopProjectWorkspace, text: String) {
        val destination = workspace.manifestFile.toPath()
        val temporary = Files.createTempFile(destination.parent, ".pgp-project-", ".tmp")
        try {
            Files.writeString(temporary, text, Charsets.UTF_8)
            runCatching {
                Files.move(
                    temporary,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
