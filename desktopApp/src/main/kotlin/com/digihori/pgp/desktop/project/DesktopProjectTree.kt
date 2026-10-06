package com.digihori.pgp.desktop.project

import java.io.File

internal data class DesktopProjectTreeSnapshot(
    val tracked: List<DesktopProjectSource>,
    val untracked: List<DesktopProjectTreeFile>,
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
        )
    }

    private fun File.isAutomaticallyIgnored(): Boolean =
        name.startsWith('.') || name.endsWith('~') || name.endsWith(".tmp", ignoreCase = true)
}
