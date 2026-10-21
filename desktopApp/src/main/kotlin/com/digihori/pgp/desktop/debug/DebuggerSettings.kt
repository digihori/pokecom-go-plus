package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.serialization.json.*

internal data class DebuggerSettingsTarget(val machineId: String, val romSha256: String, val memoryConfiguration: String, val projectSha256: String?)
internal data class DebuggerBreakpointSetting(val address: Int, val enabled: Boolean, val condition: String, val hitCondition: String)
internal data class DebuggerWatchSetting(val start: Int, val end: Int, val kind: DebuggerWatchKind, val enabled: Boolean)
internal data class DebuggerSavedSettings(val target: DebuggerSettingsTarget, val breakpoints: List<DebuggerBreakpointSetting>,
    val watches: List<DebuggerWatchSetting>, val traceEnabled: Boolean, val traceCapacity: Int)
internal enum class DebuggerSettingsError { RUNNING, NO_ROM_ID, TARGET, FORMAT, VERSION, REGISTRATIONS, TRACE, IO, CHANGED }
internal sealed interface DebuggerSettingsApplyResult {
    data object Success : DebuggerSettingsApplyResult
    data class Failure(val code: DebuggerSettingsError) : DebuggerSettingsApplyResult
}
internal class DebuggerSettingsException(val code: DebuggerSettingsError) : Exception(code.name)

/** Explicit files only. Never reads or writes Preferences, ROM libraries, or project manifests. */
internal object DebuggerSettingsFiles {
    const val MAX_BYTES = 256 * 1024
    private val hashPattern = Regex("[0-9a-f]{64}")
    private fun fail(code: DebuggerSettingsError): Nothing = throw DebuggerSettingsException(code)
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun romFingerprint(rom: RomSet): String {
        // Length-prefixed component metadata plus content: independent of ZIP packaging and source path.
        val digest = MessageDigest.getInstance("SHA-256")
        rom.components.sortedBy { it.id.value }.forEach { component ->
            val bytes = component.copyBytes()
            for (value in listOf(component.id.value, component.role.value, bytes.size.toString())) {
                val encoded = value.encodeToByteArray()
                digest.update(java.nio.ByteBuffer.allocate(4).putInt(encoded.size).array()); digest.update(encoded)
            }
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun target(runner: DesktopEmulatorRunner, project: File?): DebuggerSettingsTarget {
        val rom = runner.debugRomFingerprint?.takeIf { hashPattern.matches(it) } ?: fail(DebuggerSettingsError.NO_ROM_ID)
        return DebuggerSettingsTarget(runner.machineId.value, rom, runner.memoryLayout.pc1251MemoryMode.name,
            project?.let { hash(it.canonicalPath.encodeToByteArray()) })
    }

    fun capture(runner: DesktopEmulatorRunner, project: File?): DebuggerSavedSettings {
        if (runner.state == RunnerState.RUNNING) fail(DebuggerSettingsError.RUNNING)
        return DebuggerSavedSettings(target(runner, project), runner.breakpointRegistrations().map {
            DebuggerBreakpointSetting(it.address, it.enabled, it.condition, it.hitCondition)
        }, runner.watchRegistrations().map { DebuggerWatchSetting(it.range.first, it.range.last, it.kind, it.enabled) },
            runner.isInstructionTraceEnabled(), runner.instructionTraceCapacity())
    }

    fun restore(runner: DesktopEmulatorRunner, project: File?, settings: DebuggerSavedSettings) {
        if (runner.state == RunnerState.RUNNING) fail(DebuggerSettingsError.RUNNING)
        if (settings.target != target(runner, project)) fail(DebuggerSettingsError.TARGET)
        validate(settings)
        val applied = runner.restoreDebugSettings(settings)
        if (applied is DebuggerSettingsApplyResult.Failure) fail(applied.code)
    }

    fun encode(settings: DebuggerSavedSettings): String {
        validate(settings)
        val json = buildJsonObject {
            put("format", "pgp-debug-settings"); put("schemaVersion", 1)
            put("target", buildJsonObject {
                put("machineId", settings.target.machineId); put("romSha256", settings.target.romSha256)
                put("memoryConfiguration", settings.target.memoryConfiguration)
                put("projectSha256", settings.target.projectSha256?.let(::JsonPrimitive) ?: JsonNull)
            })
            put("breakpoints", buildJsonArray { settings.breakpoints.forEach { bp -> add(buildJsonObject {
                put("address", bp.address); put("enabled", bp.enabled); put("condition", bp.condition); put("hitCondition", bp.hitCondition)
            }) } })
            put("watches", buildJsonArray { settings.watches.forEach { watch -> add(buildJsonObject {
                put("start", watch.start); put("end", watch.end); put("kind", watch.kind.name); put("enabled", watch.enabled)
            }) } })
            put("trace", buildJsonObject { put("enabled", settings.traceEnabled); put("capacity", settings.traceCapacity) })
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json)
    }

    fun decode(text: String): DebuggerSavedSettings = try {
        if (text.encodeToByteArray().size > MAX_BYTES) fail(DebuggerSettingsError.FORMAT)
        fun JsonObject.keysExactly(vararg keys: String) { if (this.keys != keys.toSet()) fail(DebuggerSettingsError.FORMAT) }
        fun JsonObject.string(key: String): String = this.getValue(key).jsonPrimitive.let {
            if (!it.isString) fail(DebuggerSettingsError.FORMAT); it.content
        }
        fun JsonObject.integer(key: String): Int = this.getValue(key).jsonPrimitive.let {
            if (it.isString) fail(DebuggerSettingsError.FORMAT); it.int
        }
        fun JsonObject.boolean(key: String): Boolean = this.getValue(key).jsonPrimitive.let {
            if (it.isString) fail(DebuggerSettingsError.FORMAT); it.boolean
        }
        val root = Json.parseToJsonElement(text).jsonObject
        if (root.string("format") != "pgp-debug-settings") fail(DebuggerSettingsError.FORMAT)
        if (root.integer("schemaVersion") != 1) fail(DebuggerSettingsError.VERSION)
        root.keysExactly("format", "schemaVersion", "target", "breakpoints", "watches", "trace")
        val target = root.getValue("target").jsonObject
        target.keysExactly("machineId", "romSha256", "memoryConfiguration", "projectSha256")
        val project = if (target.getValue("projectSha256") == JsonNull) null else target.string("projectSha256")
        val points = root.getValue("breakpoints").jsonArray
        val watches = root.getValue("watches").jsonArray
        if (points.size > DebuggerRegistrationStore.MAX_BREAKPOINTS || watches.size > DebuggerRegistrationStore.MAX_WATCHES) fail(DebuggerSettingsError.REGISTRATIONS)
        val trace = root.getValue("trace").jsonObject
        trace.keysExactly("enabled", "capacity")
        DebuggerSavedSettings(DebuggerSettingsTarget(target.string("machineId"), target.string("romSha256"), target.string("memoryConfiguration"), project),
            points.map { element -> element.jsonObject.let {
                it.keysExactly("address", "enabled", "condition", "hitCondition")
                DebuggerBreakpointSetting(it.integer("address"), it.boolean("enabled"), it.string("condition"), it.string("hitCondition"))
            } }, watches.map { element -> element.jsonObject.let {
                it.keysExactly("start", "end", "kind", "enabled")
                DebuggerWatchSetting(it.integer("start"), it.integer("end"), DebuggerWatchKind.valueOf(it.string("kind")), it.boolean("enabled"))
            } }, trace.boolean("enabled"), trace.integer("capacity")).also(::validate)
    } catch (error: DebuggerSettingsException) { throw error }
    catch (_: Exception) { fail(DebuggerSettingsError.FORMAT) }

    private fun validate(settings: DebuggerSavedSettings) {
        if (!hashPattern.matches(settings.target.romSha256) || settings.target.projectSha256?.let { !hashPattern.matches(it) } == true ||
            settings.target.machineId.length !in 1..64 || settings.target.memoryConfiguration.length !in 1..64) fail(DebuggerSettingsError.FORMAT)
        if (settings.traceCapacity !in DesktopEmulatorRunner.MIN_INSTRUCTION_TRACE_CAPACITY..DesktopEmulatorRunner.MAX_INSTRUCTION_TRACE_CAPACITY) fail(DebuggerSettingsError.TRACE)
        val store = DebuggerRegistrationStore { _, length -> ByteArray(length) }
        if (settings.breakpoints.size > DebuggerRegistrationStore.MAX_BREAKPOINTS || settings.watches.size > DebuggerRegistrationStore.MAX_WATCHES) fail(DebuggerSettingsError.REGISTRATIONS)
        settings.breakpoints.forEach {
            if (it.condition.length > 128 || it.hitCondition.length > 32 || store.saveBreakpoint(null, it.address, it.enabled, it.condition, it.hitCondition) is DebuggerRegistrationResult.Failure) fail(DebuggerSettingsError.REGISTRATIONS)
        }
        settings.watches.forEach {
            if (store.saveWatch(null, it.start, it.end, it.kind, it.enabled) is DebuggerRegistrationResult.Failure) fail(DebuggerSettingsError.REGISTRATIONS)
        }
    }

    fun read(file: File): DebuggerSavedSettings = try {
        val bytes = file.inputStream().use { it.readNBytes(MAX_BYTES + 1) }
        if (bytes.size > MAX_BYTES) fail(DebuggerSettingsError.FORMAT)
        decode(bytes.decodeToString(throwOnInvalidSequence = true))
    } catch (error: DebuggerSettingsException) { throw error }
    catch (_: Exception) { fail(DebuggerSettingsError.IO) }

    fun write(file: File, settings: DebuggerSavedSettings) {
        val text = encode(settings)
        if (text.encodeToByteArray().size > MAX_BYTES) fail(DebuggerSettingsError.FORMAT)
        if (!file.name.endsWith(".pgdebug.json", ignoreCase = true)) fail(DebuggerSettingsError.FORMAT)
        try {
            val path = file.absoluteFile.toPath()
            val temporary = Files.createTempFile(path.parent, ".pgp-debug-", ".tmp")
            try {
                Files.writeString(temporary, text)
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } finally { Files.deleteIfExists(temporary) }
        } catch (_: Exception) { fail(DebuggerSettingsError.IO) }
    }
}
