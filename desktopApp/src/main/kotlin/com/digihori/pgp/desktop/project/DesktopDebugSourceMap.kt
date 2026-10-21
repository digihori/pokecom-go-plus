package com.digihori.pgp.desktop.project

import java.io.File
import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.core.debug.Sc61860AssemblySymbolKind

/** Loaded build metadata, never inferred from an arbitrary current workspace build. */
internal class DesktopDebugSourceMap private constructor(
    private val rows: List<Row>,
    private val snapshots: Map<File, String>,
) {
    internal data class Row(
        val address: Int,
        val bytes: List<Int>,
        val location: DesktopSc61SourceLocation,
        val labels: List<String>,
        val physical: List<PhysicalRomLocation?>,
    )

    fun sourceSnapshots(): Map<File, String> = snapshots.toMap()

    fun sourcesUnchanged(): Boolean = snapshots.isNotEmpty() && snapshots.all { (file, text) ->
        runCatching { file.readText() == text }.getOrDefault(false)
    }

    fun resolve(address: Int, read: (Int) -> Int, physical: (Int) -> PhysicalRomLocation?): Row? {
        val row = rows.filter { address >= it.address && address < it.address + it.bytes.size }.singleOrNull() ?: return null
        // Check the entire listing row, including every physical ROM component/bank/offset.
        return row.takeIf { it.bytes.indices.all { i ->
            read(it.address + i) == it.bytes[i] && physical(it.address + i) == it.physical[i]
        } }
    }

    companion object {
        /** Freeze source and SC61 INCLUDE dependencies before invoking the external process. */
        fun captureInputs(source: File): Map<File, String> {
            val inputs = linkedMapOf<File, String>()
            val include = Regex("""(?i)^\s*(?:[A-Za-z_@][A-Za-z0-9_@]*\s*:)?\s*INCLUDE\s*\(([^)]+)\)""")
            fun capture(file: File) {
                val canonical = file.canonicalFile
                if (canonical in inputs || inputs.size >= 256 || !canonical.isFile || canonical.length() > 2 * 1024 * 1024) return
                val bytes = canonical.readBytes()
                inputs[canonical] = bytes.toString(Charsets.UTF_8)
                DesktopSc61ListingParser.decode(bytes).lineSequence().forEach { line ->
                    include.find(line)?.let {
                        val path = File(it.groupValues[1].trim())
                        capture(if (path.isAbsolute) path else File(source.absoluteFile.parentFile, path.path))
                    }
                }
            }
            runCatching { capture(source) }.onFailure { return emptyMap() }
            return inputs
        }

        fun create(sources: List<DesktopAssemblySourceResult>, physical: (Int) -> PhysicalRomLocation?): DesktopDebugSourceMap {
            val rows = sources.flatMap { source -> source.listing.mapNotNull { line ->
                if (line.line <= 0 || line.bytes.isEmpty() || line.address + line.bytes.size > 0x10000) return@mapNotNull null
                val location = source.sourceLocations[line.address]
                    ?: if (source.externalListing == null && source.consoleLog.isEmpty())
                        DesktopSc61SourceLocation(source.source.file, line.line, line.sourceText) else return@mapNotNull null
                if (location.file !in source.sourceSnapshots && location.file.canonicalFile !in source.sourceSnapshots) return@mapNotNull null
                Row(line.address, line.bytes.toList(), location,
                    source.symbols.filter { it.kind == Sc61860AssemblySymbolKind.LABEL && it.address == line.address }.map { it.name },
                    line.bytes.indices.map { physical(line.address + it) })
            } }
            return DesktopDebugSourceMap(rows, sources.flatMap { it.sourceSnapshots.entries }.associate { it.toPair() })
        }
    }
}
