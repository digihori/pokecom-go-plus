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
    fun compile(workspace: DesktopProjectWorkspace): DesktopAssemblyWorkspaceResult {
        val results = mutableListOf<DesktopAssemblySourceResult>()
        val diagnostics = mutableListOf<DesktopAssemblyDiagnostic>()
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
                is Sc61860AssemblyResult.Success -> results += DesktopAssemblySourceResult(
                    source,
                    assembled.image,
                    assembled.symbols,
                    assembled.listing,
                )
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
            append(address.hex(4)).append("  ").append(symbol.name)
                .append("  ").append(path).append(':').append(symbol.line).append('\n')
        }
}

internal fun DesktopAssemblySourceResult.renderDisassembly(): String = image.segments.joinToString("\n") { segment ->
    val bytes = segment.copyBytes()
    Sc61860Disassembler.renderAssembly(segment.startAddress, segment.endAddress) { address ->
        bytes[address - segment.startAddress].toInt() and 0xff
    }.trimEnd()
}.plus("\n")

internal fun DesktopAssemblySourceResult.renderMemorySummary(): String = image.segments.joinToString("\n") { segment ->
    "0x${segment.startAddress.hex(4)}..0x${segment.endAddress.hex(4)}  ${segment.size} bytes"
}

internal object DesktopProjectBuildOutputWriter {
    fun write(workspace: DesktopProjectWorkspace, artifact: DesktopProjectArtifact): File? {
        val image = artifact.memoryImage ?: return null
        val buildDirectory = File(workspace.manifestFile.parentFile, "build")
        check(buildDirectory.isDirectory || buildDirectory.mkdirs()) { "Could not create ${buildDirectory.path}" }
        val dump = File(buildDirectory, "program.dmp")
        dump.writeBytes(com.digihori.pgp.desktop.machine.DesktopMemoryDumpWriter.write(image))
        if (artifact.assemblySources.isNotEmpty()) {
            File(buildDirectory, "program.lst").writeText(
                artifact.assemblySources.joinToString("\n") { it.renderListing().trimEnd() }.plus("\n"),
            )
            File(buildDirectory, "program.map").writeText(artifact.assemblySources.renderMap())
        }
        return dump
    }
}

private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
