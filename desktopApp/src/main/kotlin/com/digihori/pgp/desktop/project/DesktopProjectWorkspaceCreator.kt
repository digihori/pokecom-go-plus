package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectDefinition
import com.digihori.pgp.core.project.ProjectManifestWriter
import com.digihori.pgp.core.project.ProjectSource
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.rom.MachineId
import java.io.File

internal enum class DesktopProjectStarter(val displayName: String) {
    BASIC("BASIC"),
    MEMORY_DUMP("Machine code (.dmp)"),
    ASSEMBLY("Assembly (.asm)"),
}

internal sealed interface DesktopProjectCreateResult {
    data class Success(val manifestFile: File) : DesktopProjectCreateResult
    data class Failure(val error: DesktopProjectCreateError) : DesktopProjectCreateResult
}

internal sealed interface DesktopProjectCreateError {
    data class DestinationExists(val path: String) : DesktopProjectCreateError
    data class CouldNotCreate(val message: String) : DesktopProjectCreateError
}

internal object DesktopProjectWorkspaceCreator {
    fun create(
        parentDirectory: File,
        projectName: String,
        machineId: MachineId,
        starters: Set<DesktopProjectStarter>,
    ): DesktopProjectCreateResult {
        val baseFolderName = folderName(projectName)
        val destination = if (baseFolderName == DEFAULT_PROJECT_FOLDER_NAME) {
            nextAvailableDefaultDirectory(parentDirectory.absoluteFile, baseFolderName)
        } else {
            ProjectDestination(File(parentDirectory.absoluteFile, baseFolderName), projectName.trim())
        }
        val projectDirectory = destination.directory
        val manifestFile = File(projectDirectory, DesktopProjectWorkspaceLoader.DEFAULT_MANIFEST_NAME)
        if (projectDirectory.exists()) {
            return DesktopProjectCreateResult.Failure(
                DesktopProjectCreateError.DestinationExists(projectDirectory.path),
            )
        }
        require(starters.isNotEmpty()) { "At least one starter source is required" }
        val sourceFiles = starters.files(projectDirectory)

        val definition = ProjectDefinition(
            name = destination.projectName,
            machineId = machineId,
            sources = sourceFiles.map { it.definition },
        )
        val manifest = runCatching { ProjectManifestWriter.write(definition) }.getOrElse {
            return DesktopProjectCreateResult.Failure(
                DesktopProjectCreateError.CouldNotCreate(it.message ?: it::class.simpleName.orEmpty()),
            )
        }

        return runCatching {
            check(projectDirectory.mkdirs()) { "Could not create project folder: ${projectDirectory.path}" }
            val buildDirectory = File(projectDirectory, "build")
            check(buildDirectory.mkdir()) { "Could not create build folder: ${buildDirectory.path}" }
            sourceFiles.forEach { source ->
                source.file.parentFile.mkdirs()
                check(source.file.createNewFile()) { "File appeared while creating project: ${source.file.path}" }
                source.file.writeText(source.initialContent, Charsets.UTF_8)
            }
            check(manifestFile.createNewFile()) { "File appeared while creating project: ${manifestFile.path}" }
            manifestFile.writeText(manifest, Charsets.UTF_8)
            DesktopProjectCreateResult.Success(manifestFile)
        }.getOrElse { error ->
            projectDirectory.deleteRecursively()
            DesktopProjectCreateResult.Failure(
                DesktopProjectCreateError.CouldNotCreate(error.message ?: error::class.simpleName.orEmpty()),
            )
        }
    }

    internal fun folderName(projectName: String): String = projectName.trim()
        .replace(Regex("[<>:\"/\\\\|?*]"), "-")
        .replace(Regex("\\s+"), "-")
        .trim('.', '-', ' ')
        .ifEmpty { "pgp-project" }

    private fun nextAvailableDefaultDirectory(parentDirectory: File, baseFolderName: String): ProjectDestination {
        val base = File(parentDirectory, baseFolderName)
        if (!base.exists()) return ProjectDestination(base, DEFAULT_PROJECT_DISPLAY_NAME)
        var suffix = 2
        while (true) {
            val candidate = File(parentDirectory, "$baseFolderName-$suffix")
            if (!candidate.exists()) {
                return ProjectDestination(candidate, "$DEFAULT_PROJECT_DISPLAY_NAME $suffix")
            }
            suffix++
        }
    }

    private fun Set<DesktopProjectStarter>.files(root: File): List<SourceFile> = buildList {
        if (DesktopProjectStarter.BASIC in this@files) {
            add(
                SourceFile(
                    ProjectSource("main", ProjectSourceType.BASIC, "src/main.bas"),
                    File(root, "src/main.bas"),
                    "10 PRINT \"HELLO\"\n",
                ),
            )
        }
        if (DesktopProjectStarter.MEMORY_DUMP in this@files) {
            add(
                SourceFile(
                    ProjectSource("machine", ProjectSourceType.MEMORY_DUMP, "src/machine.dmp"),
                    File(root, "src/machine.dmp"),
                    "; Replace this starter byte with your machine-code data.\nC000: 00\n",
                ),
            )
        }
        if (DesktopProjectStarter.ASSEMBLY in this@files) {
            add(
                SourceFile(
                    ProjectSource("assembly", ProjectSourceType.ASSEMBLY, "src/main.asm"),
                    File(root, "src/main.asm"),
                    "; Sample SC61860 program. Edit or remove this example.\n\n" +
                        "ORG 0xC000\n\n" +
                        "START: LII 0x12\n" +
                        "       LIDP TABLE\n" +
                        "       JRP DONE\n" +
                        "TABLE: DB 0x01, &02, 3\n" +
                        "DONE:  RTN\n",
                ),
            )
        }
    }

    private data class SourceFile(
        val definition: ProjectSource,
        val file: File,
        val initialContent: String,
    )

    private data class ProjectDestination(
        val directory: File,
        val projectName: String,
    )

    private const val DEFAULT_PROJECT_FOLDER_NAME: String = "New-PGP-Project"
    private const val DEFAULT_PROJECT_DISPLAY_NAME: String = "New PGP Project"
}
