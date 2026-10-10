package com.digihori.pgp.desktop.project

import java.io.File
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.source.DesktopSourceHeaderParser

internal data class DesktopProjectTreeSnapshot(
    val tracked: List<DesktopProjectSource>,
    val untracked: List<DesktopProjectTreeFile>,
    val sourceDescriptions: Map<String, String> = emptyMap(),
)

internal data class DesktopProjectTreeFile(
    val relativePath: String,
    val file: File,
)

/** Compares the manifest with the current contents of the project's `src` directory. */
internal object DesktopProjectTreeScanner {
    fun scan(workspace: DesktopProjectWorkspace): DesktopProjectTreeSnapshot {
        val projectRoot = workspace.manifestFile.parentFile.absoluteFile.normalize()
        val trackedByPath = workspace.sources.associateBy { it.file.absoluteFile.normalize().path }
        val tracked = workspace.sources.filter { it.file.isFile }
        val untracked = mutableListOf<DesktopProjectTreeFile>()
        val sourceRoot = File(projectRoot, "src")

        if (sourceRoot.isDirectory) {
            sourceRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val normalized = file.absoluteFile.normalize()
                    if (trackedByPath.containsKey(normalized.path)) return@forEach
                    val entry = DesktopProjectTreeFile(
                        relativePath = normalized.relativeTo(projectRoot).invariantSeparatorsPath,
                        file = normalized,
                    )
                    if (!file.isAutomaticallyIgnored()) untracked += entry
                }
        }

        return DesktopProjectTreeSnapshot(
            tracked = tracked.sortedBy { it.definition.path },
            untracked = untracked.sortedBy { it.relativePath },
            // File equality alone cannot notify Compose of changes to an existing file.
            // Include immutable header text, also covering saves with unchanged timestamps.
            sourceDescriptions = buildMap {
                tracked.filter { it.definition.type != ProjectSourceType.RAW_BINARY }.forEach {
                    put(it.definition.path, description(it.file))
                }
                untracked.filter { it.file.extension.lowercase() in setOf("bas", "asm", "dmp") }.forEach {
                    put(it.relativePath, description(it.file))
                }
            },
        )
    }

    private fun description(file: File): String = runCatching {
        DesktopSourceHeaderParser.parse(file.readBytes())?.description.orEmpty()
    }.getOrDefault("")

    private fun File.isAutomaticallyIgnored(): Boolean =
        name.startsWith('.') || name.endsWith('~') || name.endsWith(".tmp", ignoreCase = true)
}
