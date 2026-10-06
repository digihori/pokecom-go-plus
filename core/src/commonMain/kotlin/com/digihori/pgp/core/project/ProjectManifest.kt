package com.digihori.pgp.core.project

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** A platform-independent description of one Pokecom GO Studio project. */
public data class ProjectDefinition(
    public val name: String,
    public val machineId: MachineId,
    public val sources: List<ProjectSource>,
)

public data class ProjectSource(
    public val id: String,
    public val type: ProjectSourceType,
    public val path: String,
    public val loadAddress: Int? = null,
)

@Serializable
public enum class ProjectSourceType {
    @SerialName("basic")
    BASIC,
    @SerialName("memory-dump")
    MEMORY_DUMP,
    @SerialName("raw-binary")
    RAW_BINARY,
    @SerialName("assembly")
    ASSEMBLY,
}

public sealed interface ProjectManifestReadResult {
    public data class Success(public val project: ProjectDefinition) : ProjectManifestReadResult
    public data class Failure(public val errors: List<ProjectManifestError>) : ProjectManifestReadResult
}

public sealed interface ProjectManifestError {
    public data class InvalidJson(public val message: String) : ProjectManifestError
    public data class UnsupportedFormat(public val actual: String) : ProjectManifestError
    public data class UnsupportedVersion(public val actual: Int) : ProjectManifestError
    public data object BlankName : ProjectManifestError
    public data class UnsupportedMachine(public val actual: String) : ProjectManifestError
    public data class InvalidSourceId(public val sourceIndex: Int, public val actual: String) : ProjectManifestError
    public data class DuplicateSourceId(public val id: String) : ProjectManifestError
    public data class InvalidSourcePath(public val sourceId: String, public val actual: String) : ProjectManifestError
    public data class DuplicateSourcePath(public val path: String) : ProjectManifestError
    public data class InvalidLoadAddress(public val sourceId: String, public val actual: String) : ProjectManifestError
    public data class MissingLoadAddress(public val sourceId: String) : ProjectManifestError
    public data class UnexpectedLoadAddress(public val sourceId: String) : ProjectManifestError
}

/** Reads the versioned `pgp-project.json` format without accessing the filesystem. */
public object ProjectManifestReader {
    public const val FORMAT: String = "pgp-project"
    public const val VERSION: Int = 1

    private val json: Json = Json {
        ignoreUnknownKeys = false
    }

    public fun read(source: String): ProjectManifestReadResult {
        val document = try {
            json.decodeFromString<ProjectManifestDocument>(source)
        } catch (error: SerializationException) {
            return ProjectManifestReadResult.Failure(
                listOf(ProjectManifestError.InvalidJson(error.message ?: "Invalid JSON")),
            )
        } catch (error: IllegalArgumentException) {
            return ProjectManifestReadResult.Failure(
                listOf(ProjectManifestError.InvalidJson(error.message ?: "Invalid JSON")),
            )
        }

        val errors = mutableListOf<ProjectManifestError>()
        if (document.format != FORMAT) errors += ProjectManifestError.UnsupportedFormat(document.format)
        if (document.formatVersion != VERSION) {
            errors += ProjectManifestError.UnsupportedVersion(document.formatVersion)
        }
        if (document.name.isBlank()) errors += ProjectManifestError.BlankName

        val machineId = runCatching { MachineId(document.machineId) }.getOrNull()
        if (machineId == null || MachineCatalog.find(machineId) == null) {
            errors += ProjectManifestError.UnsupportedMachine(document.machineId)
        }

        document.sources.groupingBy { it.id }.eachCount()
            .filterValues { it > 1 }.keys
            .forEach { errors += ProjectManifestError.DuplicateSourceId(it) }
        document.sources.groupingBy { it.path }.eachCount()
            .filterValues { it > 1 }.keys
            .forEach { errors += ProjectManifestError.DuplicateSourcePath(it) }

        val projectSources = document.sources.mapIndexedNotNull { index, item ->
            if (!item.id.matches(SOURCE_ID_PATTERN)) {
                errors += ProjectManifestError.InvalidSourceId(index, item.id)
            }
            if (!item.path.isSafeRelativePath()) {
                errors += ProjectManifestError.InvalidSourcePath(item.id, item.path)
            }
            val loadAddress = item.loadAddress?.let { value ->
                parseAddress(value) ?: run {
                    errors += ProjectManifestError.InvalidLoadAddress(item.id, value)
                    null
                }
            }
            when {
                item.type == ProjectSourceType.RAW_BINARY && item.loadAddress == null ->
                    errors += ProjectManifestError.MissingLoadAddress(item.id)
                item.type != ProjectSourceType.RAW_BINARY && item.loadAddress != null ->
                    errors += ProjectManifestError.UnexpectedLoadAddress(item.id)
            }
            if (
                !item.id.matches(SOURCE_ID_PATTERN) ||
                !item.path.isSafeRelativePath() ||
                (item.loadAddress != null && loadAddress == null)
            ) {
                null
            } else {
                ProjectSource(item.id, item.type, item.path, loadAddress)
            }
        }

        if (errors.isNotEmpty() || machineId == null) {
            return ProjectManifestReadResult.Failure(errors)
        }
        return ProjectManifestReadResult.Success(
            ProjectDefinition(document.name.trim(), machineId, projectSources),
        )
    }

    private fun parseAddress(value: String): Int? {
        val trimmed = value.trim()
        val digits = when {
            trimmed.startsWith("0x", ignoreCase = true) -> trimmed.drop(2)
            trimmed.startsWith("&") -> trimmed.drop(1)
            else -> trimmed
        }
        if (digits.isEmpty() || !digits.all { it.isHexDigit() }) return null
        return digits.toIntOrNull(16)?.takeIf { it in 0..0xffff }
    }

    private fun String.isSafeRelativePath(): Boolean =
        isNotBlank() &&
            !startsWith('/') &&
            !WINDOWS_ABSOLUTE_PATH.matches(this) &&
            '\\' !in this &&
            split('/').none { it.isEmpty() || it == "." || it == ".." }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private val SOURCE_ID_PATTERN: Regex = Regex("[a-zA-Z][a-zA-Z0-9_-]*")
    private val WINDOWS_ABSOLUTE_PATH: Regex = Regex("[a-zA-Z]:.*")
}

/** Writes the canonical, human-editable representation of a validated project model. */
public object ProjectManifestWriter {
    private val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    public fun write(project: ProjectDefinition): String {
        require(project.name.isNotBlank()) { "Project name must not be blank" }
        require(MachineCatalog.find(project.machineId) != null) { "Unsupported project machine" }
        val document = ProjectManifestDocument(
            format = ProjectManifestReader.FORMAT,
            formatVersion = ProjectManifestReader.VERSION,
            name = project.name,
            machineId = project.machineId.value,
            sources = project.sources.map { source ->
                ProjectSourceDocument(
                    id = source.id,
                    type = source.type,
                    path = source.path,
                    loadAddress = source.loadAddress?.let { "0x" + it.toString(16).uppercase().padStart(4, '0') },
                )
            },
        )
        val encoded = json.encodeToString(document) + "\n"
        require(ProjectManifestReader.read(encoded) is ProjectManifestReadResult.Success) {
            "Project contains values that cannot be written to a manifest"
        }
        return encoded
    }
}

@Serializable
private data class ProjectManifestDocument(
    val format: String,
    val formatVersion: Int,
    val name: String,
    val machineId: String,
    val sources: List<ProjectSourceDocument>,
)

@Serializable
private data class ProjectSourceDocument(
    val id: String,
    val type: ProjectSourceType,
    val path: String,
    val loadAddress: String? = null,
)
