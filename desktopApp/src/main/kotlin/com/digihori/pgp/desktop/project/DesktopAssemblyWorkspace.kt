package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyListingLine
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.debug.Sc61860AssemblySymbol
import com.digihori.pgp.core.debug.Sc61860Disassembler
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.source.DesktopSourceHeaderParser
import java.io.File
import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.api.MemoryImageLoadError
import com.digihori.pgp.core.api.MemoryPlacementValidator

internal data class DesktopAssemblySourceResult(
    val source: DesktopProjectSource,
    val image: AddressedMemoryImage,
    val symbols: List<Sc61860AssemblySymbol>,
    val listing: List<Sc61860AssemblyListingLine>,
    val consoleLog: String = "",
    val externalListing: String? = null,
    val symbolLocations: Map<String, DesktopSc61SourceLocation> = emptyMap(),
)

internal data class DesktopAssemblyDiagnostic(
    val source: DesktopProjectSource,
    val line: Int?,
    val message: String,
    val sourceText: String?,
    val file: File = source.file,
)

internal sealed interface DesktopAssemblyWorkspaceResult {
    data class Success(val sources: List<DesktopAssemblySourceResult>) : DesktopAssemblyWorkspaceResult
    data class Failure(val diagnostics: List<DesktopAssemblyDiagnostic>) : DesktopAssemblyWorkspaceResult
}

/** One assembly pipeline shared by the workspace preview and project Build & Load. */
internal object DesktopAssemblyWorkspaceCompiler {
    suspend fun compileConfigured(workspace: DesktopProjectWorkspace, layout: MachineMemoryLayout, settings: DesktopAssemblerSettings): DesktopAssemblyWorkspaceResult {
        if (!settings.external) return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { compile(workspace, layout, validatePlacement = false) }
        val assembled = mutableMapOf<String, Sc61860AssemblyResult>()
        val logs = mutableMapOf<String, String>()
        val externalListings = mutableMapOf<String, DesktopSc61Listing>()
        val externalDiagnostics = mutableMapOf<String, DesktopSc61SourceLocation>()
        for (source in workspace.sources.filter { it.definition.type == ProjectSourceType.ASSEMBLY }) {
            when (val result = DesktopSc61Assembler.assemble(settings, source.file, layout)) {
                is DesktopSc61AssemblyResult.Failure -> {
                    result.location?.let { externalDiagnostics[source.definition.id] = it }
                    assembled[source.definition.id] = Sc61860AssemblyResult.Failure(0, result.message + "\n" + result.log)
                }
                is DesktopSc61AssemblyResult.Success -> {
                    logs[source.definition.id] = result.log
                    result.sourceListing?.let { externalListings[source.definition.id] = it }
                    // Byte ownership is available even before an external source map is parsed.
                    assembled[source.definition.id] = Sc61860AssemblyResult.Success(result.image, result.sourceListing?.symbols.orEmpty(), result.sourceListing?.listing ?: result.image.segments.map {
                        Sc61860AssemblyListingLine(0, it.startAddress, it.copyBytes().map { byte -> byte.toInt() and 255 }, "; External DMP; source mapping unavailable")
                    })
                }
            }
        }
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            when (val result = compile(workspace, layout, assembled, validatePlacement = false)) {
                is DesktopAssemblyWorkspaceResult.Failure -> result.copy(diagnostics = result.diagnostics.map {
                    val location = externalDiagnostics[it.source.definition.id] ?: externalListings[it.source.definition.id]?.rows
                        ?.filter { row -> row.location != null && row.location.line == it.line && row.location.text == it.sourceText }
                        ?.mapNotNull { row -> row.location }?.distinct()?.singleOrNull()
                    if (location == null) it else it.copy(line = location.line, file = location.file, sourceText = location.text)
                })
                is DesktopAssemblyWorkspaceResult.Success -> result.copy(sources = result.sources.map {
                    val listing = externalListings[it.source.definition.id]
                    it.copy(consoleLog = logs[it.source.definition.id].orEmpty(), externalListing = listing?.raw,
                        symbolLocations = listing?.symbolLocations.orEmpty())
                })
            }
        }
    }

    fun compile(workspace: DesktopProjectWorkspace, layout: MachineMemoryLayout = MachineMemoryLayout(workspace.definition.machineId),
        preassembled: Map<String, Sc61860AssemblyResult>? = null, validatePlacement: Boolean = true): DesktopAssemblyWorkspaceResult {
        require(layout.machineId == workspace.definition.machineId)
        val results = mutableListOf<DesktopAssemblySourceResult>()
        val diagnostics = mutableListOf<DesktopAssemblyDiagnostic>()
        val owners = arrayOfNulls<Pair<DesktopProjectSource, Int>>(0x10000)
        workspace.sources.filter { it.definition.type == ProjectSourceType.ASSEMBLY }.forEach { source ->
            val text = runCatching { source.file.readText() }.getOrElse {
                diagnostics += DesktopAssemblyDiagnostic(source, null, it.message ?: it::class.simpleName.orEmpty(), null)
                return@forEach
            }
            val sourceText = DesktopSourceHeaderParser.parse(text).sourceText
            when (val assembled = preassembled?.getValue(source.definition.id) ?: Sc61860Assembler.assemble(sourceText)) {
                is Sc61860AssemblyResult.Failure -> diagnostics += DesktopAssemblyDiagnostic(
                    source,
                    assembled.line.takeIf { it > 0 },
                    assembled.message,
                    assembled.line.takeIf { it > 0 }?.let { sourceText.lineSequence().elementAtOrNull(it - 1) },
                )
                is Sc61860AssemblyResult.Success -> {
                    val issue = if (validatePlacement) MemoryPlacementValidator.validate(assembled.image, layout) else null
                    if (issue != null) {
                        val address = issue.error.address
                        val line = assembled.listing.firstOrNull { address >= it.address && address < it.address + it.bytes.size }
                        diagnostics += DesktopAssemblyDiagnostic(source, line?.line, issue.error.placementMessage(layout), line?.sourceText)
                    } else {
                        val conflict = assembled.image.segments.firstNotNullOfOrNull { segment ->
                            (segment.startAddress..segment.endAddress).firstNotNullOfOrNull { address ->
                                owners[if (validatePlacement) layout.locate(address).physicalAddress else address]?.let { previous ->
                                    val line = assembled.listing.firstOrNull { address >= it.address && address < it.address + it.bytes.size }
                                    DesktopAssemblyDiagnostic(source, line?.line?.takeIf { it > 0 },
                                        "0x${address.hex(4)} overlaps ${previous.first.definition.path} at 0x${previous.second.hex(4)} (same ${if (validatePlacement) "physical memory" else "address"}).", line?.sourceText)
                                }
                            }
                        }
                        if (conflict != null) diagnostics += conflict
                        else {
                            assembled.image.segments.forEach { segment -> (segment.startAddress..segment.endAddress).forEach { address ->
                                owners[if (validatePlacement) layout.locate(address).physicalAddress else address] = source to address
                            } }
                            results += DesktopAssemblySourceResult(
                                source, assembled.image, assembled.symbols, assembled.listing,
                            )
                        }
                    }
                }
            }
        }
        return if (diagnostics.isEmpty()) DesktopAssemblyWorkspaceResult.Success(results)
        else DesktopAssemblyWorkspaceResult.Failure(diagnostics)
    }
}

internal fun DesktopAssemblySourceResult.renderListing(): String = buildString {
    externalListing?.let { append(it); return@buildString }
    append("; ").append(source.definition.path).append('\n')
    if (consoleLog.isNotEmpty()) {
        append("; External assembler source listing is not imported yet.\n")
        return@buildString
    }
    listing.forEach { entry ->
        append(entry.address.hex(4)).append("  ")
        append(entry.bytes.joinToString(" ") { it.hex(2) }.padEnd(13))
        append("  ").append(entry.line.toString().padStart(5)).append("  ")
        append(entry.sourceText).append('\n')
    }
}

internal fun List<DesktopAssemblySourceResult>.renderMap(): String = buildString {
    this@renderMap.flatMap { result -> result.symbols.map {
        Triple(it.address, result.symbolLocations[it.name]?.file?.path ?: result.source.definition.path, it)
    } }
        .sortedWith(compareBy({ it.first }, { it.third.name }))
        .forEach { (address, path, symbol) ->
            if (symbol.kind == com.digihori.pgp.core.debug.Sc61860AssemblySymbolKind.CONSTANT) {
                append(symbol.name).append(" EQU ").append(symbol.value)
            } else append(address.hex(4)).append("  ").append(symbol.name)
            append("  ").append(path).append(':').append(if (symbol.line > 0) symbol.line.toString() else "?").append('\n')
        }
}

internal fun DesktopAssemblySourceResult.renderDisassembly(): String {
    // DMP rows are separate segments; adjacent rows still form one instruction stream.
    val groups = mutableListOf<MutableList<com.digihori.pgp.core.source.machine.AddressedMemorySegment>>()
    image.segments.sortedBy { it.startAddress }.forEach { segment ->
        val previous = groups.lastOrNull()
        if (previous != null && previous.last().endAddress + 1 == segment.startAddress) previous += segment
        else groups += mutableListOf(segment)
    }
    return groups.joinToString("\n") { group ->
        val start = group.first().startAddress
        val bytes = group.flatMap { it.copyBytes().toList() }
        Sc61860Disassembler.renderAssembly(start, group.last().endAddress) { address ->
            bytes[address - start].toInt() and 0xff
        }.trimEnd()
    }.plus("\n")
}

internal fun DesktopAssemblySourceResult.renderMemorySummary(layout: MachineMemoryLayout): String = image.segments.joinToString("\n") { segment ->
    val kinds = (segment.startAddress..segment.endAddress).map { layout.locate(it).kind }.distinct()
    val mirrors = (segment.startAddress..segment.endAddress).any { layout.locate(it).physicalAddress != it }
    "0x${segment.startAddress.hex(4)}..0x${segment.endAddress.hex(4)}  ${segment.size} bytes  ${kinds.joinToString()}" +
        if (mirrors) " (contains mirrors)" else ""
}

internal fun MachineMemoryLayout.renderMemoryLayout(): String = buildString {
    append(machineId.value)
    if (com.digihori.pgp.core.api.MachineCatalog.require(machineId).supportsConfigurableRam) append(" — ").append(pc1251MemoryMode)
    append('\n')
    var start = 0
    while (start <= 0xffff) {
        val region = locate(start)
        var end = start
        while (end < 0xffff && locate(end + 1).kind == region.kind && locate(end + 1).writable == region.writable) end++
        append(start.hex(4)).append("..").append(end.hex(4)).append("  ").append(region.kind)
            .append(if (region.writable) " (writable)" else " (read-only)").append('\n')
        start = end + 1
    }
    if (machineId.value == "pc-1360") append("4000..7FFF: ROM bank 0..7; bank output is not supported.\n")
    append("LCD/control writes have side effects. BASIC placement is checked again at load time.")
}

internal fun MemoryImageLoadError.placementMessage(layout: MachineMemoryLayout): String = when (this) {
    is MemoryImageLoadError.ReadOnlyAddress -> "0x${address.hex(4)} is ${layout.locate(address).kind} and cannot be loaded on ${layout.machineId.value}."
    is MemoryImageLoadError.MirrorOverlap -> "0x${address.hex(4)} and 0x${previousAddress.hex(4)} write the same physical address 0x${physicalAddress.hex(4)}."
    is MemoryImageLoadError.BasicOverlap -> "0x${address.hex(4)} overlaps BASIC text, pointer updates or old-text clearing."
}

internal object DesktopProjectBuildOutputWriter {
    /** A single assembly source owns the output name; combined projects retain program.*. */
    fun outputFiles(workspace: DesktopProjectWorkspace): List<File> {
        val assemblySources = workspace.sources.filter { it.definition.type == ProjectSourceType.ASSEMBLY }
        val baseName = assemblySources.singleOrNull()?.file?.nameWithoutExtension ?: "program"
        val directory = File(workspace.manifestFile.parentFile, "build")
        return listOf("dmp", "lst", "map").map { File(directory, "$baseName.$it") }
    }

    fun write(workspace: DesktopProjectWorkspace, artifact: DesktopProjectArtifact): File? {
        val image = artifact.memoryImage ?: return null
        val buildDirectory = File(workspace.manifestFile.parentFile, "build")
        check(buildDirectory.isDirectory || buildDirectory.mkdirs()) { "Could not create ${buildDirectory.path}" }
        val (dump, listing, map) = outputFiles(workspace)
        dump.writeBytes(com.digihori.pgp.desktop.machine.DesktopMemoryDumpWriter.write(image))
        if (artifact.assemblySources.isNotEmpty()) {
            listing.writeText(
                artifact.assemblySources.joinToString("\n") { it.renderListing().trimEnd() }.plus("\n"),
            )
            map.writeText(artifact.assemblySources.renderMap())
        }
        return dump
    }
}

private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
