package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectDefinition
import com.digihori.pgp.core.project.ProjectManifestError
import com.digihori.pgp.core.project.ProjectManifestReadResult
import com.digihori.pgp.core.project.ProjectManifestReader
import com.digihori.pgp.core.project.ProjectSource
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

internal data class DesktopProjectWorkspace(
    val manifestFile: File,
    val definition: ProjectDefinition,
    val sources: List<DesktopProjectSource>,
)

internal data class DesktopProjectSource(
    val definition: ProjectSource,
    val file: File,
)

internal sealed interface DesktopProjectOpenResult {
    data class Success(val workspace: DesktopProjectWorkspace) : DesktopProjectOpenResult
    data class Failure(val errors: List<DesktopProjectOpenError>) : DesktopProjectOpenResult
}

internal sealed interface DesktopProjectOpenError {
    data class CouldNotReadManifest(val message: String) : DesktopProjectOpenError
    data object InvalidUtf8 : DesktopProjectOpenError
    data class InvalidManifest(val errors: List<ProjectManifestError>) : DesktopProjectOpenError
}

internal object DesktopProjectWorkspaceLoader {
    const val DEFAULT_MANIFEST_NAME: String = "pgp-project.json"

    fun open(manifestFile: File): DesktopProjectOpenResult {
        val bytes = runCatching { manifestFile.readBytes() }.getOrElse {
            return DesktopProjectOpenResult.Failure(
                listOf(DesktopProjectOpenError.CouldNotReadManifest(it.message ?: it::class.simpleName.orEmpty())),
            )
        }
        val text = decodeUtf8(bytes)
            ?: return DesktopProjectOpenResult.Failure(listOf(DesktopProjectOpenError.InvalidUtf8))
        val definition = when (val read = ProjectManifestReader.read(text)) {
            is ProjectManifestReadResult.Success -> read.project
            is ProjectManifestReadResult.Failure -> return DesktopProjectOpenResult.Failure(
                listOf(DesktopProjectOpenError.InvalidManifest(read.errors)),
            )
        }

        val root = manifestFile.absoluteFile.parentFile.toPath().normalize().toFile()
        val sources = definition.sources.map { source ->
            DesktopProjectSource(source, File(root, source.path).toPath().normalize().toFile())
        }
        return DesktopProjectOpenResult.Success(
            DesktopProjectWorkspace(manifestFile.absoluteFile, definition, sources),
        )
    }

    private fun decodeUtf8(bytes: ByteArray): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()
}

internal enum class DesktopProjectChangeKind {
    CREATED,
    MODIFIED,
    DELETED,
}

internal data class DesktopProjectSourceChange(
    val source: DesktopProjectSource,
    val kind: DesktopProjectChangeKind,
)

/**
 * Cross-platform polling tracker for files edited by another process.
 *
 * Content hashes avoid relying only on timestamp precision, which differs between filesystems.
 * The caller controls polling frequency and thread ownership.
 */
internal class DesktopProjectChangeTracker(workspace: DesktopProjectWorkspace) {
    private val sources: List<DesktopProjectSource> = workspace.sources.toList()
    private val fingerprints: MutableMap<String, SourceFingerprint> = sources.associate { source ->
        source.definition.id to source.file.fingerprint()
    }.toMutableMap()

    fun scan(): List<DesktopProjectSourceChange> = sources.mapNotNull { source ->
        val previous = fingerprints.getValue(source.definition.id)
        val current = source.file.fingerprint()
        fingerprints[source.definition.id] = current
        if (current == previous) return@mapNotNull null
        val kind = when {
            !previous.exists && current.exists -> DesktopProjectChangeKind.CREATED
            previous.exists && !current.exists -> DesktopProjectChangeKind.DELETED
            else -> DesktopProjectChangeKind.MODIFIED
        }
        DesktopProjectSourceChange(source, kind)
    }

    private fun File.fingerprint(): SourceFingerprint {
        if (!isFile) return SourceFingerprint(exists = false, sha256 = "")
        val digest = MessageDigest.getInstance("SHA-256").digest(readBytes())
        return SourceFingerprint(
            exists = true,
            sha256 = digest.joinToString(separator = "") { "%02x".format(it) },
        )
    }

    private data class SourceFingerprint(
        val exists: Boolean,
        val sha256: String,
    )
}
