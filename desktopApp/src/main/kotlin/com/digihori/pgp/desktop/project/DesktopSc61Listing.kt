package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.debug.Sc61860AssemblyListingLine
import com.digihori.pgp.core.debug.Sc61860AssemblySymbol
import com.digihori.pgp.core.debug.Sc61860AssemblySymbolKind
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal data class DesktopSc61SourceLocation(val file: File, val line: Int, val text: String)
internal data class DesktopSc61ListedRow(
    val line: Int, val address: Int, val bytes: List<Int>, val cycles: String,
    val text: String, val location: DesktopSc61SourceLocation?,
)
internal data class DesktopSc61Listing(
    val raw: String,
    val rows: List<DesktopSc61ListedRow>,
    val symbols: List<Sc61860AssemblySymbol>,
    val symbolLocations: Map<String, DesktopSc61SourceLocation>,
) {
    val listing: List<Sc61860AssemblyListingLine> get() = rows.map {
        Sc61860AssemblyListingLine(it.location?.line ?: 0, it.address, it.bytes, it.location?.text ?: it.text)
    }
}

internal object DesktopSc61ListingParser {
    private val numbered = Regex("""^(\d{5}):([0-9A-Fa-f]{4})  (.*)$""")
    private val continuation = Regex("""^ {12}([0-9A-Fa-f]{2,8})[\s]*$""")
    private val symbol = Regex("""([A-Za-z_@][A-Za-z0-9_@]*)\s*:\s*([0-9A-Fa-f]{4})h""")
    private val declaration = Regex("""^\s*([A-Za-z_@][A-Za-z0-9_@]*)\s*:\s*(.*)$""")

    fun decode(bytes: ByteArray): String = runCatching {
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    }.getOrElse { java.nio.charset.Charset.forName("windows-31j").decode(ByteBuffer.wrap(bytes)).toString() }

    fun parse(text: String, source: File): DesktopSc61Listing {
        val resolver = sourceResolver(source)
        val rows = mutableListOf<DesktopSc61ListedRow>()
        val symbols = mutableListOf<Sc61860AssemblySymbol>()
        val locations = mutableMapOf<String, DesktopSc61SourceLocation>()
        val declarations = mutableMapOf<String, Pair<Int, Sc61860AssemblySymbolKind>>()
        var inMap = false
        text.lineSequence().forEach { original ->
            val line = original.trimEnd('\r', '\u001a')
            if (line.contains(" - MAP LIST OF [")) { inMap = true; return@forEach }
            if (inMap) {
                symbol.findAll(line).forEach { match ->
                    val name = match.groupValues[1]
                    val declared = declarations[name]
                    symbols += Sc61860AssemblySymbol(name, match.groupValues[2].toInt(16), declared?.first ?: 0,
                        declared?.second ?: Sc61860AssemblySymbolKind.LABEL)
                }
                return@forEach
            }
            val match = numbered.matchEntire(line)
            if (match == null) {
                val extra = continuation.matchEntire(line)
                if (extra != null && rows.isNotEmpty() && rows.last().bytes.isNotEmpty()) {
                    val previous = rows.removeAt(rows.lastIndex)
                    rows += previous.copy(bytes = previous.bytes + hexBytes(extra.groupValues[1]))
                }
                return@forEach
            }
            val body = match.groupValues[3]
            val code: String
            val cycles: String
            val content: String
            if ('\t' in body) {
                val fields = body.split('\t')
                code = fields.first().trim()
                val rest = fields.drop(1).filter { it.isNotBlank() }.toMutableList()
                cycles = if (rest.firstOrNull()?.matches(Regex("""[0-9]+(?:[/+~\-][0-9]+)*""")) == true) rest.removeAt(0) else ""
                content = rest.joinToString(" ").trim()
            } else {
                code = body.take(8).trim()
                cycles = body.drop(8).take(7).trim()
                content = body.drop(15).trim()
            }
            val number = match.groupValues[1].toInt()
            val location = resolver(number, content)
            rows += DesktopSc61ListedRow(number, match.groupValues[2].toInt(16), hexBytes(code), cycles, content, location)
            declaration.find(content)?.let {
                val name = it.groupValues[1]
                declarations[name] = (location?.line ?: 0) to
                    if (it.groupValues[2].startsWith("EQU", true)) Sc61860AssemblySymbolKind.CONSTANT else Sc61860AssemblySymbolKind.LABEL
                if (location != null) locations[name] = location
            }
        }
        return DesktopSc61Listing(text, rows, symbols, locations)
    }

    fun diagnostic(log: String, source: File): DesktopSc61SourceLocation? {
        val error = Regex("""(?m)^\s*LINE:(\d+)\s+ERROR CODE:\d+.*$""").find(log) ?: return null
        val line = error.groupValues[1].toIntOrNull() ?: return null
        val printed = log.substring(0, error.range.first).lineSequence().mapNotNull { numbered.matchEntire(it.trimEnd('\r')) }.lastOrNull()
        val body = printed?.groupValues?.get(3) ?: return null
        return sourceResolver(source)(line, body.drop(15).trim())
    }

    private fun hexBytes(text: String): List<Int> = if (text.length % 2 == 0 && text.isNotEmpty() && text.all { it.digitToIntOrNull(16) != null })
        text.chunked(2).map { it.toInt(16) } else emptyList()

    /** SC61 omits file names for INCLUDE rows. Navigate only when content identifies one source. */
    private fun sourceResolver(source: File): (Int, String) -> DesktopSc61SourceLocation? {
        val files = linkedMapOf<File, List<String>>()
        fun read(file: File) {
            val canonical = file.canonicalFile
            if (canonical in files || files.size >= 256 || !canonical.isFile || canonical.length() > 2 * 1024 * 1024) return
            val lines = decode(canonical.readBytes()).lines()
            files[canonical] = lines
            lines.forEach { line ->
                Regex("""(?i)^\s*(?:[A-Za-z_@][A-Za-z0-9_@]*\s*:)?\s*INCLUDE\s*\(([^)]+)\)""").find(line)?.let {
                    val path = File(it.groupValues[1].trim())
                    read(if (path.isAbsolute) path else File(source.absoluteFile.parentFile, path.path))
                }
            }
        }
        read(source)
        return { number, printed ->
            files.mapNotNull { (file, lines) -> lines.getOrNull(number - 1)?.takeIf { canonicalText(it) == canonicalText(printed) }
                ?.let { DesktopSc61SourceLocation(file, number, it) } }.singleOrNull()
        }
    }

    private fun canonicalText(text: String): String = buildString {
        var quote: Char? = null
        for (c in text) {
            if (quote != null) { append(c); if (c == quote) quote = null }
            else if (c == ';' || c == '#') break
            else if (c == '\'' || c == '"') { quote = c; append(c) }
            else if (!c.isWhitespace() && c != ':') append(c.uppercaseChar())
        }
    }
}
