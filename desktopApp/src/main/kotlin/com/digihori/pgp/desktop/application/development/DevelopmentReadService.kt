package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860InstructionSet
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.application.debug.DebugContextService
import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.runner.*
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.serialization.json.*

internal data class DevelopmentReadState(
    val runner: DesktopEmulatorRunner? = null,
    val debugService: DebugContextService? = null,
    val project: DesktopProjectWorkspace? = null,
    val assembler: DesktopAssemblerSettings = DesktopAssemblerSettings(),
    val assembly: List<DesktopAssemblySourceResult> = emptyList(),
    val diagnostics: List<DesktopAssemblyDiagnostic> = emptyList(),
    val buildErrors: List<DesktopProjectBuildError> = emptyList(),
    val buildStatus: String = "NOT_BUILT",
    val changedSourceIds: Set<String> = emptySet(),
    val busy: Boolean = false,
)
internal class DevelopmentReadError(val code: String) : Exception(code)
internal interface DevelopmentReadService {
    fun machineSpec(machineId: String?): JsonObject
    fun context(section: String, sourceId: String?, offset: Int, limit: Int): JsonObject
}

/** Application-thread reads of existing Studio/Core models; no build, tool execution or arbitrary-path API. */
internal class DesktopDevelopmentReadService(private val stateProvider: () -> DevelopmentReadState) : DevelopmentReadService {
    private val instanceId = java.util.UUID.randomUUID().toString()
    private var observedState: DevelopmentReadState? = null
    private var metadataRevision = 0L
    override fun machineSpec(machineId: String?): JsonObject {
        val state = stateProvider()
        val id = if (machineId != null) runCatching { MachineId(machineId) }.getOrNull() ?: throw DevelopmentReadError("UNSUPPORTED_MACHINE")
            else state.runner?.machineId ?: throw DevelopmentReadError("NO_ACTIVE_SESSION")
        val definition = MachineCatalog.find(id) ?: throw DevelopmentReadError("UNSUPPORTED_MACHINE")
        val active = state.runner?.takeIf { it.machineId == id }
        val layout = active?.memoryLayout ?: MachineMemoryLayout(id)
        return bounded(buildJsonObject {
            put("format", "pgp-machine-spec"); put("schemaVersion", 1)
            put("machineId", id.value); put("displayName", definition.displayName)
            put("definitionSource", "PGP Core MachineCatalog and instruction metadata; not an independent hardware specification")
            put("family", definition.family.name); put("generation", definition.generation.name)
            put("cpu", "SC61860"); put("cyclesPerSecond", definition.cyclesPerSecond)
            put("basicDialect", definition.basicDialect.name)
            put("display", buildJsonObject {
                put("characterColumns", definition.characterColumns); put("characterRows", definition.characterRows)
                put("directDrawing", if (id.value == "pc-1245") buildJsonObject {
                    put("source", "PGP Core Pc1245Display and displaySnapshot")
                    put("characterWidth", 5); put("dotRows", 7); put("dotColumns", 80)
                    put("encoding", "one byte per logical column; bit 0 is the top row; bits 0..6 are pixels")
                    put("columnAddressFormula", "column < 60 ? 0xF800 + column : 0xF868 + (79 - column)")
                    put("symbolAddressesExcluded", buildJsonArray { add(0xf83c); add(0xf83d) })
                    put("lcdMustBeEnabled", true)
                    put("enableSequence", "LIP 0x5F; ORIM 0x01; OUTC (preserves other control bits)")
                    put("firmwareMayOverwriteDisplay", true)
                    put("firmwareCallingConventionVerified", false)
                    put("exampleCodeAndDataEndInclusive", 0xc5ff)
                    put("exampleMemoryGuidance", "PC-1245 HELLO example keeps code/data below 0xC600; BASIC firmware writes upper RAM workspace, not only program pointers")
                } else JsonNull)
            })
            put("keyboard", buildJsonObject {
                put("layoutId", definition.keyboardLayout.name)
                put("automaticHoldCycles", definition.automaticKeyHoldCycles); put("automaticGapCycles", definition.automaticKeyGapCycles)
            })
            put("operatingModes", buildJsonArray { definition.supportedOperatingModes.forEach { add(JsonPrimitive(it.name)) } })
            put("memoryRegions", buildJsonArray { definition.memoryRegions.forEach { region -> add(buildJsonObject {
                put("id", region.id); put("kind", region.kind.name); put("startAddress", region.startAddress)
                put("endAddressInclusive", region.endAddressInclusive); put("mirrorsRegionId", region.mirrorsRegionId?.let(::JsonPrimitive) ?: JsonNull)
            }) } })
            put("memoryLayout", buildJsonObject {
                put("scope", if (active != null) "activeSession" else "catalogDefaults")
                put("ramConfiguration", layout.pc1251MemoryMode.name)
                put("assemblyStartAddress", layout.assemblyStartAddress); put("defaultBasicStartAddress", layout.defaultBasicStartAddress)
                put("catalogRegionsAreDescriptive", true)
            })
            put("banking", when (val banks = definition.memoryBanking) {
                MachineMemoryBanking.None -> JsonNull
                is MachineMemoryBanking.Banked -> buildJsonObject {
                    put("windowStart", banks.windowStart); put("windowEndInclusive", banks.windowEndInclusive); put("bankCount", banks.bankCount)
                }
            })
            put("instructions", buildJsonArray { (0..255).mapNotNull(Sc61860InstructionSet::find).forEach { instruction -> add(buildJsonObject {
                put("opcode", instruction.opcode); put("mnemonic", instruction.mnemonic); put("length", instruction.length)
                put("operandEncoding", instruction.operandEncoding.name); put("flow", instruction.flow.name); put("encodable", instruction.encodable)
            }) } })
            put("notProvided", buildJsonArray { listOf("ROM routine calling conventions", "LCD pixel encoding except PC-1245 directDrawing", "key read ABI", "independent real-hardware timing validation").forEach { add(JsonPrimitive(it)) } })
        })
    }

    override fun context(section: String, sourceId: String?, offset: Int, limit: Int): JsonObject {
        if (section !in setOf("metadata", "diagnostics", "assembly", "debug") || offset !in 0..1_000_000 || limit !in 1..128 ||
            (section == "assembly") != (sourceId != null)) throw DevelopmentReadError("INVALID_ARGUMENTS")
        val state = stateProvider()
        if (section == "debug" && state.runner == null) throw DevelopmentReadError("NO_ACTIVE_SESSION")
        if (section == "debug" && state.runner?.state == RunnerState.RUNNING) throw DevelopmentReadError("SESSION_RUNNING")
        val workspace = state.project
        val root = workspace?.manifestFile?.canonicalFile?.parentFile
        fun relative(file: File): String? = relative(root, file)
        fun validSource(result: DesktopAssemblySourceResult): Boolean = workspace?.sources.orEmpty().any {
            it.definition.id == result.source.definition.id && it.file.canonicalFile == result.source.file.canonicalFile
        } && relative(result.source.file) != null
        val projectId = workspace?.let { digest(it.manifestFile.canonicalPath) }
        val assembler = buildJsonObject {
            put("mode", if (state.assembler.external) "external" else "builtin")
            put("tool", if (state.assembler.external) state.assembler.tool.name else "SC61860_BUILTIN")
            put("timeoutSeconds", state.assembler.timeoutSeconds)
            put("configured", !state.assembler.external || state.assembler.executable.isNotBlank())
            put("listingSupported", !state.assembler.external || state.assembler.tool == DesktopAssemblerTool.SC61)
            put("commandAndLogsOmitted", true)
        }
        // Observe immutable UI metadata; this is not a file-content transaction token.
        if (state != observedState) { observedState = state; metadataRevision++ }
        val revision = "$instanceId:$metadataRevision"
        return bounded(buildJsonObject {
            put("format", "pgp-development-context"); put("schemaVersion", 1); put("section", section)
            put("session", state.debugService?.sessionIdentity()?.let { session -> buildJsonObject {
                put("id", session.id); put("revision", session.revision); put("machineId", session.machineId); put("runState", session.runState)
            } } ?: JsonNull)
            put("project", workspace?.let { buildJsonObject {
                put("id", projectId); put("metadataRevision", revision); put("revisionScope", "observedMetadataNotSourceContent")
                put("name", it.definition.name.take(256)); put("machineId", it.definition.machineId.value)
                put("buildStatus", state.buildStatus); put("busy", state.busy)
            } } ?: JsonNull)
            put("metadataRevision", revision)
            put("offset", offset); put("limit", limit)
            when (section) {
                "metadata" -> {
                    put("assembler", assembler)
                    put("romSha256", state.runner?.debugRomFingerprint?.let(::JsonPrimitive) ?: JsonNull)
                    val sources = workspace?.sources.orEmpty()
                    page(sources.size, offset, limit)
                    put("sources", buildJsonArray { sources.drop(offset).take(limit).forEach { source -> add(buildJsonObject {
                        if (source.definition.id.length > 128) throw DevelopmentReadError("LIMIT_EXCEEDED")
                        put("id", source.definition.id); put("path", relative(source.file)?.let(::JsonPrimitive) ?: JsonNull)
                        put("type", source.definition.type.name); put("changedSinceStudioBaseline", source.definition.id in state.changedSourceIds)
                        put("loadAddress", source.definition.loadAddress?.let(::JsonPrimitive) ?: JsonNull)
                    }) } })
                }
                "diagnostics" -> {
                    put("buildFailureDetailsAvailable", state.buildStatus != "BUILD_FAILED" || state.buildErrors.isNotEmpty() || state.diagnostics.isNotEmpty())
                    if (workspace != null && state.buildErrors.isNotEmpty()) {
                        page(state.buildErrors.size, offset, limit)
                        put("diagnostics", JsonArray(state.buildErrors.drop(offset).take(limit).map { buildDiagnostic(it, state) }))
                    } else {
                        val diagnostics = if (workspace == null) emptyList() else state.diagnostics.filter { diagnostic ->
                            workspace.sources.any { it.definition.id == diagnostic.source.definition.id && it.file.canonicalFile == diagnostic.source.file.canonicalFile }
                        }
                        page(diagnostics.size, offset, limit)
                        put("diagnostics", JsonArray(diagnostics.drop(offset).take(limit).map { assemblyDiagnostic(it, state, root) }))
                    }
                    put("basis", "lastReportedResult; may be stale after source changes")
                }
                "assembly" -> {
                    val declared = workspace?.sources?.firstOrNull { it.definition.id == sourceId } ?: throw DevelopmentReadError("SOURCE_NOT_FOUND")
                    if (relative(declared.file) == null) throw DevelopmentReadError("OUTSIDE_PROJECT")
                    val assembled = state.assembly.filter { it.source.definition.id == sourceId && validSource(it) }.singleOrNull()
                    val budget = ReadBudget(root)
                    put("sourceId", sourceId); put("basis", "lastSuccessfulAssemblyPreview; not proof of a loaded build")
                    put("freshness", assembled?.let { budget.freshness(it.sourceSnapshots) } ?: "unavailable")
                    put("symbolCount", assembled?.symbols?.size ?: 0); put("listingCount", assembled?.listing?.size ?: 0)
                    put("symbols", buildJsonArray { assembled?.symbols.orEmpty().drop(offset).take(limit).forEach { symbol -> add(buildJsonObject {
                        put("name", if (symbol.name.length <= 128) JsonPrimitive(symbol.name) else JsonNull)
                        put("nameOmitted", symbol.name.length > 128); put("value", symbol.address); put("kind", symbol.kind.name)
                        val location = assembled?.symbolLocations?.get(symbol.name)
                        val path = if (assembled?.externalListing != null) location?.file?.let(::relative) else relative(declared.file)
                        put("path", path?.let(::JsonPrimitive) ?: JsonNull)
                        put("line", if (path != null) symbol.line.takeIf { it > 0 }?.let(::JsonPrimitive) ?: JsonNull else JsonNull)
                    }) } })
                    put("listing", buildJsonArray { assembled?.listing.orEmpty().drop(offset).take(limit).forEach { line -> add(buildJsonObject {
                        val location = assembled?.sourceLocations?.get(line.address)
                        val file = if (assembled?.externalListing != null) location?.file else declared.file
                        val path = file?.let(::relative)
                        put("address", line.address); put("byteCount", line.bytes.size)
                        put("path", path?.let(::JsonPrimitive) ?: JsonNull)
                        put("line", if (path != null && line.line > 0) JsonPrimitive(line.line) else JsonNull)
                        put("sourceTextOmitted", true)
                    }) } })
                    put("nextSymbolOffset", next(offset, limit, assembled?.symbols?.size ?: 0))
                    put("nextListingOffset", next(offset, limit, assembled?.listing?.size ?: 0))
                    put("loadedLocation", loadedLocation(state, root, budget))
                }
                "debug" -> {
                    val runner = checkNotNull(state.runner)
                    val points = runner.breakpointRegistrations(); val watches = runner.watchRegistrations()
                    put("addressScope", "logicalAllBanks")
                    put("hitCounting", "Enabled address encounters before instruction; false value conditions count; manual Step and resume skip do not count")
                    put("breakpointCount", points.size); put("watchCount", watches.size)
                    put("breakpoints", buildJsonArray { points.drop(offset).take(limit).forEach { point -> add(buildJsonObject {
                        put("id", point.id); put("address", point.address); put("enabled", point.enabled)
                        put("condition", point.condition); put("hitCondition", point.hitCondition); put("hitCount", point.hitCount)
                    }) } })
                    put("watches", buildJsonArray { watches.drop(offset).take(limit).forEach { watch -> add(buildJsonObject {
                        put("id", watch.id); put("startAddress", watch.range.first); put("endAddressInclusive", watch.range.last)
                        put("kind", watch.kind.name); put("enabled", watch.enabled)
                    }) } })
                    put("nextBreakpointOffset", next(offset, limit, points.size)); put("nextWatchOffset", next(offset, limit, watches.size))
                    put("trace", buildJsonObject { put("enabled", runner.isInstructionTraceEnabled()); put("capacity", runner.instructionTraceCapacity()) })
                    put("step", buildJsonObject { put("status", runner.debugStepStatus.name); put("canStepOut", runner.canStepOut) })
                }
            }
        })
    }

    private fun assemblyDiagnostic(value: DesktopAssemblyDiagnostic, state: DevelopmentReadState, root: File?): JsonObject = buildJsonObject {
        put("code", "ASSEMBLY_ERROR"); put("severity", "error"); put("sourceId", value.source.definition.id.take(128))
        val path = relative(root, value.file)
        put("path", path?.let(::JsonPrimitive) ?: JsonNull)
        put("line", if (path != null) value.line?.let(::JsonPrimitive) ?: JsonNull else JsonNull)
        val readable = path != null && !value.externalTool && !state.assembler.external && value.consoleLog.isBlank()
        put("message", if (readable) value.message.take(4096).replace(Regex("""(?i)(?:[A-Z]:[\\/]|/)[^\s\"'<>]*"""), "<path>").take(512)
            else "Assembly failed; inspect Studio diagnostics. External output and paths are omitted.")
        put("rawOutputOmitted", true)
    }

    private fun buildDiagnostic(error: DesktopProjectBuildError, state: DevelopmentReadState): JsonObject {
        if (error is DesktopProjectBuildError.Assembly) {
            state.diagnostics.firstOrNull { it.source.definition.id == error.sourceId && it.line == error.line }?.let {
                return assemblyDiagnostic(it, state, state.project?.manifestFile?.canonicalFile?.parentFile)
            }
        }
        return buildJsonObject {
            val sourceId = when (error) {
                is DesktopProjectBuildError.CouldNotReadSource -> error.sourceId
                is DesktopProjectBuildError.BasicCompile -> error.sourceId
                is DesktopProjectBuildError.MemoryDumpParse -> error.sourceId
                is DesktopProjectBuildError.EmptyRawBinary -> error.sourceId
                is DesktopProjectBuildError.InvalidRawBinaryRange -> error.sourceId
                is DesktopProjectBuildError.MemoryOverlap -> error.sourceId
                is DesktopProjectBuildError.Assembly -> error.sourceId
                is DesktopProjectBuildError.Placement -> error.sourceId
                else -> null
            }
            put("code", when (error) {
                is DesktopProjectBuildError.MultipleBasicSources -> "MULTIPLE_BASIC_SOURCES"
                is DesktopProjectBuildError.CouldNotReadSource -> "COULD_NOT_READ_SOURCE"
                is DesktopProjectBuildError.BasicCompile -> "BASIC_COMPILE"
                is DesktopProjectBuildError.MemoryDumpParse -> "MEMORY_DUMP_PARSE"
                is DesktopProjectBuildError.EmptyRawBinary -> "EMPTY_RAW_BINARY"
                is DesktopProjectBuildError.InvalidRawBinaryRange -> "INVALID_RAW_BINARY_RANGE"
                is DesktopProjectBuildError.MemoryOverlap -> "MEMORY_OVERLAP"
                is DesktopProjectBuildError.Assembly -> "ASSEMBLY_ERROR"
                is DesktopProjectBuildError.Placement -> "MEMORY_PLACEMENT"
                is DesktopProjectBuildError.BasicPlacement -> "BASIC_PLACEMENT"
            })
            put("severity", "error"); put("sourceId", sourceId?.take(128)?.let(::JsonPrimitive) ?: JsonNull)
            put("line", (error as? DesktopProjectBuildError.Assembly)?.line?.takeIf { it > 0 }?.let(::JsonPrimitive) ?: JsonNull)
            put("address", when (error) {
                is DesktopProjectBuildError.MemoryOverlap -> JsonPrimitive(error.address)
                is DesktopProjectBuildError.InvalidRawBinaryRange -> JsonPrimitive(error.startAddress)
                is DesktopProjectBuildError.Placement -> JsonPrimitive(error.error.address)
                else -> JsonNull
            })
            put("message", "Build failed; inspect the typed error code and Studio diagnostics. Raw tool output is omitted.")
        }
    }

    private fun loadedLocation(state: DevelopmentReadState, root: File?, budget: ReadBudget): JsonObject = buildJsonObject {
        put("basis", "activeSessionLoadedBuild; independent of requested preview source")
        val runner = state.runner
        val map = runner?.sourceMap
        if (runner == null || map == null) { put("status", "unavailable"); return@buildJsonObject }
        if (runner.state == RunnerState.RUNNING) { put("status", "sessionRunning"); return@buildJsonObject }
        val freshness = budget.freshness(map.sourceSnapshots())
        if (freshness != "current") { put("status", freshness); return@buildJsonObject }
        val pc = runner.cpuSnapshot().programCounter
        val row = map.resolve(pc, runner::memoryByte, runner::resolveRomLocation)
        val path = row?.location?.file?.let { relative(root, it) }
        if (row == null || path == null) { put("status", "unmappedOrBytesOrPhysicalLocationMismatch"); return@buildJsonObject }
        put("status", "matchedLoadedBuild"); put("address", pc); put("path", path); put("line", row.location.line)
        put("rom", runner.resolveRomLocation(pc)?.let { rom -> buildJsonObject {
            put("component", rom.componentId.value); put("bank", rom.bank?.let(::JsonPrimitive) ?: JsonNull); put("offset", rom.offset)
        } } ?: JsonNull)
        put("labelsTruncated", row.labels.size > 128 || row.labels.any { it.length > 128 })
        put("labels", buildJsonArray { row.labels.take(128).forEach { if (it.length <= 128) add(JsonPrimitive(it)) } })
    }

    /** A strict project-root boundary and a shared 2 MiB inspection budget per request. */
    private class ReadBudget(private val root: File?) {
        private var remaining = 2 * 1024 * 1024
        private val cache = mutableMapOf<File, ByteArray?>()
        fun freshness(snapshots: Map<File, String>): String {
            if (snapshots.isEmpty()) return "unavailable"
            for ((file, text) in snapshots) {
                if (relative(root, file) == null) return "outsideProjectUnverified"
                val canonical = file.canonicalFile
                if (text.contains('\uFFFD')) return "encodingUnverified"
                if (text.length > 2 * 1024 * 1024) return "inspectionLimit"
                val bytes = if (cache.containsKey(canonical)) cache[canonical] else {
                    if (!canonical.isFile) return "staleOrMissing"
                    if (canonical.length() > remaining) return "inspectionLimit"
                    val read = runCatching { canonical.inputStream().use { it.readNBytes(remaining + 1) } }.getOrNull()
                    if (read != null && read.size > remaining) return "inspectionLimit"
                    remaining -= read?.size ?: 0
                    cache[canonical] = read
                    read
                } ?: return "staleOrMissing"
                val decoded = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull() ?: return "encodingUnverified"
                if (decoded != text) return "staleOrMissing"
            }
            return "current"
        }
    }

    companion object {
        private fun relative(root: File?, file: File): String? = runCatching {
            val base = root?.toPath()?.toRealPath() ?: return null
            // File.canonicalFile does not reliably follow Windows symlinks.
            // Resolve the existing ancestor through NIO, retaining missing suffixes
            // so diagnostics can still name not-yet-created project files.
            var existing = file.toPath().toAbsolutePath()
            val missing = mutableListOf<Path>()
            while (!Files.exists(existing, NOFOLLOW_LINKS)) {
                missing.add(existing.fileName ?: return null)
                existing = existing.parent ?: return null
            }
            var path = existing.toRealPath()
            for (part in missing.asReversed()) path = path.resolve(part)
            path = path.normalize()
            if (!path.startsWith(base)) null else base.relativize(path).toString().replace(File.separatorChar, '/').takeIf { it.length <= 512 }
        }.getOrNull()
        private fun digest(text: String): String = MessageDigest.getInstance("SHA-256").digest(text.encodeToByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        private fun next(offset: Int, limit: Int, total: Int): JsonElement = if (offset + limit < total) JsonPrimitive(offset + limit) else JsonNull
        private fun JsonObjectBuilder.page(total: Int, offset: Int, limit: Int) { put("total", total); put("nextOffset", next(offset, limit, total)) }
        private fun bounded(value: JsonObject): JsonObject {
            if (value.toString().encodeToByteArray().size > 128 * 1024) throw DevelopmentReadError("RESPONSE_TOO_LARGE")
            return value
        }
    }
}
