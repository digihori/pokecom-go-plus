package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.core.source.machine.PgpMemoryDumpError
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object DesktopMemoryDumpLoader {
    fun parse(bytes: ByteArray): DesktopMemoryDumpLoadResult {
        val source = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrNull() ?: return DesktopMemoryDumpLoadResult.Failure(DesktopMemoryDumpLoadError.InvalidUtf8)

        // SC61 appends the historical DOS end-of-file marker. Only tolerate it at EOF.
        val trimmed = source.trimEnd()
        val normalized = if (trimmed.endsWith('\u001a')) trimmed.dropLast(1) else source
        return when (val parsed = PgpMemoryDumpParser.parse(normalized)) {
            is PgpMemoryDumpParseResult.Success -> DesktopMemoryDumpLoadResult.Success(parsed.image)
            is PgpMemoryDumpParseResult.Failure ->
                DesktopMemoryDumpLoadResult.Failure(DesktopMemoryDumpLoadError.Parse(parsed.error))
        }
    }
}

internal sealed interface DesktopMemoryDumpLoadResult {
    data class Success(val image: AddressedMemoryImage) : DesktopMemoryDumpLoadResult
    data class Failure(val error: DesktopMemoryDumpLoadError) : DesktopMemoryDumpLoadResult
}

internal sealed interface DesktopMemoryDumpLoadError {
    data object InvalidUtf8 : DesktopMemoryDumpLoadError
    data class Parse(val error: PgpMemoryDumpError) : DesktopMemoryDumpLoadError
}
