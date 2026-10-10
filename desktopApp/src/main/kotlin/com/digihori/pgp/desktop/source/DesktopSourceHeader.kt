package com.digihori.pgp.desktop.source

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal data class DesktopSourceHeader(
    val description: String,
    val sourceText: String,
)

/** Extracts Studio documentation from the consecutive `#` lines at the start of a UTF-8 source. */
internal object DesktopSourceHeaderParser {
    fun parse(bytes: ByteArray): DesktopSourceHeader? = decodeUtf8(bytes)?.let(::parse)

    fun parse(source: String): DesktopSourceHeader {
        val normalized = source.removePrefix("\uFEFF")
        val lines = normalized.split('\n')
        val description = mutableListOf<String>()
        var inHeader = true
        val body = lines.map { rawLine ->
            val line = rawLine.removeSuffix("\r")
            if (inHeader && line.trimStart().startsWith('#')) {
                description += line.trimStart().removePrefix("#").removePrefix(" ")
                ""
            } else {
                inHeader = false
                rawLine
            }
        }.joinToString("\n")
        return DesktopSourceHeader(description.joinToString("\n").trim(), body)
    }

    private fun decodeUtf8(bytes: ByteArray): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()
}
