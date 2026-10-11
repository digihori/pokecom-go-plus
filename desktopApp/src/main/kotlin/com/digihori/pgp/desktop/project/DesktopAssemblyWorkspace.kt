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
)

internal data class DesktopAssemblyDiagnostic(
    val source: DesktopProjectSource,
    val line: Int?,
    val message: String,
    val sourceText: String?,
)

internal sealed interface DesktopAssemblyWorkspaceResult {
    data class Success(val sources: List<DesktopAssemblySourceResult>) : DesktopAssemblyWorkspaceResult
    data class Failure(val diagnostics: List<DesktopAssemblyDiagnostic>) : DesktopAssemblyWorkspaceResult
}

/** One assembly pipeline shared by the workspace preview and project Build & Load. */
internal object DesktopAssemblyWorkspaceCompiler {
    fun compile(workspace: DesktopProjectWorkspace, layout: MachineMemoryLayout = MachineMemoryLayout(workspace.definition.machineId)): DesktopAssemblyWorkspaceResult {
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
            when (val assembled = Sc61860Assembler.assemble(sourceText)) {
                is Sc61860AssemblyResult.Failure -> diagnostics += DesktopAssemblyDiagnostic(
                    source,
                    assembled.line.takeIf { it > 0 },
                    assembled.message,
                    assembled.line.takeIf { it > 0 }?.let { sourceText.lineSequence().elementAtOrNull(it - 1) },
                )
                is Sc61860AssemblyResult.Success -> {
                    val issue = MemoryPlacementValidator.validate(assembled.image, layout)
                    if (issue != null) {
                        val address = issue.error.address
                        val line = assembled.listing.firstOrNull { address >= it.address && address < it.address + it.bytes.size }
                        diagnostics += DesktopAssemblyDiagnostic(source, line?.line, issue.error.placementMessage(layout), line?.sourceText)
                    } else {
                        val conflict = assembled.listing.firstNotNullOfOrNull { line ->
                            line.bytes.indices.firstNotNullOfOrNull { offset ->
                                val address = line.address + offset
                                owners[layout.locate(address).physicalAddress]?.let { previous ->
                                    DesktopAssemblyDiagnostic(source, line.line,
                                        "0x${address.hex(4)} overlaps ${previous.first.definition.path} at 0x${previous.second.hex(4)} (same physical memory).", line.sourceText)
                                }
                            }
                        }
                        if (conflict != null) diagnostics += conflict
                        else {
                            assembled.listing.forEach { line -> line.bytes.indices.forEach { offset ->
                                val address = line.address + offset
                                owners[layout.locate(address).physicalAddress] = source to address
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
    append("; ").append(source.definition.path).append('\n')
    listing.forEach { entry ->
        append(entry.address.hex(4)).append("  ")
        append(entry.bytes.joinToString(" ") { it.hex(2) }.padEnd(13))
        append("  ").append(entry.line.toString().padStart(5)).append("  ")
        append(entry.sourceText).append('\n')
    }
}

internal fun List<DesktopAssemblySourceResult>.renderMap(): String = buildString {
    this@renderMap.flatMap { result -> result.symbols.map { Triple(it.address, result.source.definition.path, it) } }
        .sortedWith(compareBy({ it.first }, { it.third.name }))
        .forEach { (address, path, symbol) ->
            if (symbol.kind == com.digihori.pgp.core.debug.Sc61860AssemblySymbolKind.CONSTANT) {
                append(symbol.name).append(" EQU ").append(symbol.value)
            } else append(address.hex(4)).append("  ").append(symbol.name)
            append("  ").append(path).append(':').append(symbol.line).append('\n')
        }
}

internal fun DesktopAssemblySourceResult.renderDisassembly(): String = image.segments.joinToString("\n") { segment ->
    val bytes = segment.copyBytes()
    Sc61860Disassembler.renderAssembly(segment.startAddress, segment.endAddress) { address ->
        bytes[address - segment.startAddress].toInt() and 0xff
    }.trimEnd()
}.plus("\n")

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
