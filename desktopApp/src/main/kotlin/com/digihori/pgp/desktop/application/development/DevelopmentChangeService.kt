package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.project.DesktopProjectWorkspace
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardCopyOption.*
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.json.*

internal data class SourceReplacement(val oldText: String, val newText: String)
internal data class DevelopmentReview(
    val id: String, val kind: String, val projectName: String, val sourcePath: String?,
    val replacements: List<SourceReplacement>, val revision: String, val sourceId: String? = null,
)
internal data class DevelopmentOperation(val id: String, val kind: String, val status: String, val code: String? = null)

/** Application-thread proposal queue. Only Studio UI can approve a source write or build. */
internal class DevelopmentChangeService(
    private val stateProvider: () -> DevelopmentReadState,
    private val onChange: () -> Unit = {},
    private val onApplied: (String) -> Unit = {},
    private val onBuild: (String) -> Unit = {},
) {
    private var workspace: DesktopProjectWorkspace? = null
    private var epoch = UUID.randomUUID().toString()
    private var pending: DevelopmentReview? = null
    private var replacementText: String? = null
    private val operations = linkedMapOf<String, DevelopmentOperation>()
    private val revisions = mutableMapOf<String, String>()
    fun review(): DevelopmentReview? = pending
    fun history(): List<DevelopmentOperation> = operations.values.toList().asReversed()

    private data class Snapshot(val state: DevelopmentReadState, val revision: String, val bytes: Map<String, ByteArray>)
    private fun snapshot(ignoreBusy: Boolean = false): Snapshot {
        val state = stateProvider()
        val project = state.project ?: fail("NO_PROJECT")
        if (project !== workspace) { workspace = project; epoch = UUID.randomUUID().toString() }
        if (state.busy && !ignoreBusy) fail("BUSY")
        if (state.assembler.external) fail("BUILTIN_ONLY")
        if (project.sources.size > 64 || project.sources.map { it.definition.id }.distinct().size != project.sources.size) fail("LIMIT_EXCEEDED")
        val root = project.manifestFile.canonicalFile.parentFile.toPath()
        val paths = mutableSetOf<java.nio.file.Path>()
        fun read(file: java.io.File, limit: Int): ByteArray {
            val path = file.absoluteFile.toPath().normalize()
            val lexicalRoot = project.manifestFile.absoluteFile.parentFile.toPath().normalize()
            if (!path.startsWith(lexicalRoot)) fail("OUTSIDE_PROJECT")
            var lexical = lexicalRoot
            for (part in lexicalRoot.relativize(path)) {
                lexical = lexical.resolve(part)
                if (Files.isSymbolicLink(lexical)) fail("LINK_NOT_ALLOWED")
            }
            val relative = root.relativize(path.toRealPath())
            if (relative.startsWith("..") || relative.toString().isEmpty()) fail("OUTSIDE_PROJECT")
            // Reject links below the project root; canonical containment alone is insufficient for writes.
            var current = root
            for (part in root.relativize(path.toRealPath())) {
                current = current.resolve(part)
                if (Files.isSymbolicLink(current)) fail("LINK_NOT_ALLOWED")
            }
            if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, NOFOLLOW_LINKS)) fail("LINK_NOT_ALLOWED")
            if (!paths.add(path.toRealPath())) fail("DUPLICATE_PATH")
            val links = runCatching { Files.getAttribute(path, "unix:nlink", NOFOLLOW_LINKS) as Number }.getOrNull()
            if (links != null && links.toInt() > 1) fail("LINK_NOT_ALLOWED")
            return Files.newInputStream(path).use { input -> input.readNBytes(limit + 1).also { if (it.size > limit) fail("LIMIT_EXCEEDED") } }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        fun add(bytes: ByteArray) { digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); digest.update(bytes) }
        add(epoch.toByteArray()); add(state.assembler.toString().toByteArray())
        val layout = state.runner?.memoryLayout ?: com.digihori.pgp.core.api.MachineMemoryLayout(project.definition.machineId, state.fallbackRamMode)
        add(layout.machineId.value.toByteArray()); add(layout.pc1251MemoryMode.name.toByteArray())
        val manifest = read(project.manifestFile, 65536)
        val parsed = com.digihori.pgp.core.project.ProjectManifestReader.read(decode(manifest))
        if (parsed !is com.digihori.pgp.core.project.ProjectManifestReadResult.Success || parsed.project != project.definition) fail("PROJECT_REOPEN_REQUIRED")
        add(manifest)
        var total = 0
        val bytes = project.sources.associate { source ->
            if (source.definition.type !in setOf(ProjectSourceType.BASIC, ProjectSourceType.ASSEMBLY)) fail("TEXT_PROJECT_ONLY")
            val content = read(source.file, 65536)
            total += content.size
            if (total > 2 * 1024 * 1024) fail("LIMIT_EXCEEDED")
            decode(content) // A revision never authorizes rewriting invalid text.
            add(source.definition.id.toByteArray()); add(source.definition.path.toByteArray()); add(content)
            source.definition.id to content
        }
        return Snapshot(state, digest.digest().hex(), bytes)
    }

    /** Internal content stamp shared by Build/Load; not the metadata-only revision. */
    fun projectRevision(ignoreBusy: Boolean = false): String = snapshot(ignoreBusy).revision

    fun source(sourceId: String): JsonObject {
        val snapshot = snapshot()
        val content = snapshot.bytes[sourceId] ?: fail("SOURCE_NOT_FOUND")
        val source = snapshot.state.project!!.sources.single { it.definition.id == sourceId }
        return buildJsonObject {
            put("format", "pgp-source"); put("schemaVersion", 1); put("projectRevision", snapshot.revision)
            put("sourceId", sourceId); put("path", source.definition.path); put("sourceSha256", sha(content)); put("text", decode(content))
        }
    }

    fun propose(sourceId: String, revision: String, sourceSha256: String, edits: List<SourceReplacement>): JsonObject {
        if (pending != null || operations.values.any { it.status == "RUNNING" }) fail("BUSY")
        val snapshot = snapshot()
        if (snapshot.revision != revision) fail("STALE_PROJECT")
        val bytes = snapshot.bytes[sourceId] ?: fail("SOURCE_NOT_FOUND")
        if (sha(bytes) != sourceSha256) fail("STALE_SOURCE")
        if (edits.isEmpty() || edits.size > 16) fail("INVALID_PATCH")
        val original = decode(bytes)
        var text = original
        for (edit in edits) {
            if (edit.oldText.isEmpty() || edit.oldText == edit.newText || edit.oldText.length + edit.newText.length > 32768) fail("INVALID_PATCH")
            val at = text.indexOf(edit.oldText)
            if (at < 0 || text.indexOf(edit.oldText, at + 1) >= 0) fail("AMBIGUOUS_PATCH")
            text = text.replaceRange(at, at + edit.oldText.length, edit.newText)
        }
        if (text == original || text.toByteArray().size > 65536 || text.contains('\u0000')) fail("INVALID_PATCH")
        val source = snapshot.state.project!!.sources.single { it.definition.id == sourceId }
        replacementText = text
        return queue(DevelopmentReview(UUID.randomUUID().toString(), "PATCH", snapshot.state.project.definition.name,
            source.definition.path, edits.toList(), revision, source.definition.id))
    }

    fun proposeBuild(revision: String): JsonObject {
        if (pending != null || operations.values.any { it.status == "RUNNING" }) fail("BUSY")
        val snapshot = snapshot()
        if (snapshot.revision != revision) fail("STALE_PROJECT")
        validateBuildTargets(snapshot)
        return queue(DevelopmentReview(UUID.randomUUID().toString(), "BUILD", snapshot.state.project!!.definition.name, null, emptyList(), revision))
    }

    private fun validateBuildTargets(snapshot: Snapshot) {
        val project = snapshot.state.project!!
        val root = project.manifestFile.canonicalFile.parentFile.toPath()
        val inputs = (project.sources.map { it.file.canonicalFile.toPath() } + project.manifestFile.canonicalFile.toPath()).toSet()
        for (file in com.digihori.pgp.desktop.project.DesktopProjectBuildOutputWriter.outputFiles(project)) {
            val target = file.absoluteFile.toPath().normalize()
            val lexicalRoot = project.manifestFile.absoluteFile.parentFile.toPath().normalize()
            if (!target.startsWith(lexicalRoot)) fail("OUTSIDE_PROJECT")
            var path = lexicalRoot
            for (part in lexicalRoot.relativize(target)) {
                path = path.resolve(part)
                if (Files.isSymbolicLink(path)) fail("LINK_NOT_ALLOWED")
            }
            val canonical = file.canonicalFile.toPath()
            if (!canonical.startsWith(root) || canonical in inputs) fail("INVALID_BUILD_TARGET")
            if (Files.exists(target, NOFOLLOW_LINKS)) {
                if (!Files.isRegularFile(target, NOFOLLOW_LINKS)) fail("INVALID_BUILD_TARGET")
                val links = runCatching { Files.getAttribute(target, "unix:nlink", NOFOLLOW_LINKS) as Number }.getOrNull()
                if (links != null && links.toInt() > 1) fail("LINK_NOT_ALLOWED")
            }
        }
    }

    private fun queue(review: DevelopmentReview): JsonObject {
        pending = review
        operations[review.id] = DevelopmentOperation(review.id, review.kind, "PENDING_APPROVAL")
        revisions[review.id] = review.revision
        while (operations.size > 32) { val old = operations.keys.first(); operations.remove(old); revisions.remove(old) }
        onChange()
        return operation(review.id)
    }

    /** Invoked exclusively by the local Studio review button, never exposed as an MCP tool. */
    fun approve() {
        val review = pending ?: return
        try {
            val current = snapshot()
            if (current.revision != review.revision) fail("STALE_PROJECT")
            pending = null
            if (review.kind == "PATCH") {
                val project = current.state.project!!
                val source = project.sources.single { it.definition.id == review.sourceId }
                val target = source.file.toPath().toRealPath()
                val temp = Files.createTempFile(target.parent, ".pgp-patch-", ".tmp")
                try {
                    Files.write(temp, requireNotNull(replacementText).toByteArray())
                    runCatching { Files.setPosixFilePermissions(temp, Files.getPosixFilePermissions(target, NOFOLLOW_LINKS)) }
                    if (snapshot().revision != review.revision) fail("STALE_PROJECT")
                    Files.move(temp, target, ATOMIC_MOVE, REPLACE_EXISTING)
                } finally { Files.deleteIfExists(temp) }
                operations[review.id] = DevelopmentOperation(review.id, review.kind, "APPLIED")
                onApplied(source.definition.id)
            } else {
                validateBuildTargets(current)
                operations[review.id] = DevelopmentOperation(review.id, review.kind, "RUNNING")
                onBuild(review.id)
            }
        } catch (error: Exception) {
            pending = null
            operations[review.id] = DevelopmentOperation(review.id, review.kind, "FAILED", (error as? DevelopmentReadError)?.code ?: "OPERATION_FAILED")
        } finally { replacementText = null; onChange() }
    }

    fun reject() {
        val review = pending ?: return
        pending = null; replacementText = null
        operations[review.id] = DevelopmentOperation(review.id, review.kind, "REJECTED")
        onChange()
    }
    fun completeBuild(id: String, success: Boolean) {
        val op = operations[id] ?: return
        val current = runCatching { snapshot(ignoreBusy = true).revision == revisions[id] }.getOrDefault(false)
        operations[id] = op.copy(status = if (success && current) "SUCCEEDED" else "FAILED",
            code = if (!current) "STALE_PROJECT" else if (success) null else "BUILD_FAILED")
        onChange()
    }
    fun operation(id: String): JsonObject {
        val operation = operations[id] ?: fail("OPERATION_NOT_FOUND")
        return buildJsonObject {
            put("format", "pgp-development-operation"); put("schemaVersion", 1)
            put("operationId", operation.id); put("kind", operation.kind); put("status", operation.status)
            put("code", operation.code?.let(::JsonPrimitive) ?: JsonNull)
            put("approval", "Studio UI only"); put("loadsOrRuns", false)
        }
    }
    private fun decode(bytes: ByteArray): String = try {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString().also { if (it.contains('\u0000')) fail("INVALID_TEXT") }
    } catch (_: java.nio.charset.CharacterCodingException) { fail("INVALID_UTF8") }
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).hex()
    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
    private fun fail(code: String): Nothing = throw DevelopmentReadError(code)
}
