package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomBasicInput
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.source.basic.BasicTextParseError
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object DesktopBasicLoader {
    fun compilePc1245RomInput(bytes: ByteArray): DesktopBasicLoadResult {
        val text = runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        }.getOrElse {
            return DesktopBasicLoadResult.Failure(DesktopBasicLoadError.InvalidUtf8)
        }

        val document = when (val parsed = BasicTextParser.parse(text)) {
            is BasicTextParseResult.Failure -> return DesktopBasicLoadResult.Failure(
                DesktopBasicLoadError.Parse(parsed.error),
            )
            is BasicTextParseResult.Success -> parsed.document
        }
        return when (val compiled = Pc1245RomBasicInput.compile(document)) {
            is Pc1245RomInputResult.Failure -> DesktopBasicLoadResult.Failure(
                DesktopBasicLoadError.UnsupportedRomInput(compiled.error),
            )
            is Pc1245RomInputResult.Success -> DesktopBasicLoadResult.Success(compiled.keys)
        }
    }
}

internal sealed interface DesktopBasicLoadResult {
    class Success(keys: List<PocketKey>) : DesktopBasicLoadResult {
        val keys: List<PocketKey> = keys.toList()
    }
    data class Failure(val error: DesktopBasicLoadError) : DesktopBasicLoadResult
}

internal sealed interface DesktopBasicLoadError {
    data object InvalidUtf8 : DesktopBasicLoadError
    data class Parse(val error: BasicTextParseError) : DesktopBasicLoadError
    data class UnsupportedRomInput(val error: Pc1245RomInputError) : DesktopBasicLoadError
}
