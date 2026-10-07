package com.digihori.pgp.desktop.basic

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineKeyboardLayout
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomBasicInput
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizeError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomBasicInput
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicCodec
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicTokenizeResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.basic.BasicTextParseError
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

internal object DesktopBasicLoader {
    fun compilePc1245RomInput(bytes: ByteArray): DesktopBasicLoadResult {
        return compileRomInput(bytes, Pc1245RomDefinition.MACHINE_ID)
    }

    fun compileRomInput(bytes: ByteArray, machineId: MachineId): DesktopBasicLoadResult {
        val text = decodeUtf8(bytes) ?: run {
            return DesktopBasicLoadResult.Failure(DesktopBasicLoadError.InvalidUtf8)
        }

        val document = when (val parsed = BasicTextParser.parse(text)) {
            is BasicTextParseResult.Failure -> return DesktopBasicLoadResult.Failure(
                DesktopBasicLoadError.Parse(parsed.error),
            )
            is BasicTextParseResult.Success -> parsed.document
        }
        val compiled = when (MachineCatalog.find(machineId)?.keyboardLayout) {
            MachineKeyboardLayout.PC_1251 -> Pc1251RomBasicInput.compile(document)
            MachineKeyboardLayout.PC_1245, null -> Pc1245RomBasicInput.compile(document)
            MachineKeyboardLayout.PC_1350, MachineKeyboardLayout.PC_1360 -> return DesktopBasicLoadResult.Failure(
                DesktopBasicLoadError.UnsupportedMachine(machineId),
            )
        }
        return when (compiled) {
            is Pc1245RomInputResult.Failure -> DesktopBasicLoadResult.Failure(
                DesktopBasicLoadError.UnsupportedRomInput(compiled.error),
            )
            is Pc1245RomInputResult.Success -> DesktopBasicLoadResult.Success(compiled.keys)
        }
    }

    fun compilePc1245Program(bytes: ByteArray): DesktopBasicProgramCompileResult {
        return compileProgram(bytes, Pc1245RomDefinition.MACHINE_ID)
    }

    fun compileProgram(bytes: ByteArray, machineId: MachineId): DesktopBasicProgramCompileResult {
        val text = decodeUtf8(bytes) ?: return DesktopBasicProgramCompileResult.Failure(
            DesktopBasicProgramCompileError.InvalidUtf8,
        )
        val document = when (val parsed = BasicTextParser.parse(text)) {
            is BasicTextParseResult.Failure -> return DesktopBasicProgramCompileResult.Failure(
                DesktopBasicProgramCompileError.Parse(parsed.error),
            )
            is BasicTextParseResult.Success -> parsed.document
        }
        return when (MachineCatalog.find(machineId)?.basicDialect) {
            com.digihori.pgp.core.api.MachineBasicDialect.S1 -> {
            when (val tokenized = Pc1350BasicTokenizer.tokenize(document)) {
                is Pc1350BasicTokenizeResult.Success -> DesktopBasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1350BasicTokenizeResult.Failure -> DesktopBasicProgramCompileResult.Failure(
                    DesktopBasicProgramCompileError.S1Tokenize(tokenized.error),
                )
            }}
            com.digihori.pgp.core.api.MachineBasicDialect.S2 -> when (val tokenized = Pc1360BasicCodec.tokenize(document)) {
                is Pc1360BasicTokenizeResult.Success -> DesktopBasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1360BasicTokenizeResult.Failure -> DesktopBasicProgramCompileResult.Failure(
                    DesktopBasicProgramCompileError.S2Tokenize(tokenized.error),
                )
            }
            else -> when (val tokenized = Pc1245BasicTokenizer.tokenize(document)) {
                is Pc1245BasicTokenizeResult.Success -> DesktopBasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1245BasicTokenizeResult.Failure -> DesktopBasicProgramCompileResult.Failure(
                    DesktopBasicProgramCompileError.Tokenize(tokenized.error),
                )
            }
        }
    }

    fun detokenizePc1245Program(bytes: ByteArray): DesktopBasicProgramDecodeResult =
        when (val decoded = Pc1245BasicDetokenizer.detokenize(bytes)) {
            is Pc1245BasicDetokenizeResult.Success -> DesktopBasicProgramDecodeResult.Success(
                (decoded.source + "\n").encodeToByteArray(),
            )
            is Pc1245BasicDetokenizeResult.Failure -> DesktopBasicProgramDecodeResult.Failure(decoded.error)
        }

    fun detokenizeProgram(bytes: ByteArray, machineId: MachineId): DesktopBasicProgramDecodeResult =
        if (MachineCatalog.find(machineId)?.basicDialect == com.digihori.pgp.core.api.MachineBasicDialect.S2) {
            when (val decoded = Pc1360BasicCodec.detokenize(bytes)) {
                is Pc1360BasicDetokenizeResult.Success -> DesktopBasicProgramDecodeResult.Success((decoded.source + "\n").encodeToByteArray())
                is Pc1360BasicDetokenizeResult.Failure -> DesktopBasicProgramDecodeResult.GenericFailure(decoded.error.offset, decoded.error.message)
            }
        } else detokenizePc1245Program(bytes)

    private fun decodeUtf8(bytes: ByteArray): String? = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()
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
    data class UnsupportedMachine(val machineId: MachineId) : DesktopBasicLoadError
}

internal sealed interface DesktopBasicProgramCompileResult {
    class Success(bytes: ByteArray) : DesktopBasicProgramCompileResult {
        val bytes: ByteArray = bytes.copyOf()
    }
    data class Failure(val error: DesktopBasicProgramCompileError) : DesktopBasicProgramCompileResult
}

internal sealed interface DesktopBasicProgramCompileError {
    data object InvalidUtf8 : DesktopBasicProgramCompileError
    data class Parse(val error: BasicTextParseError) : DesktopBasicProgramCompileError
    data class Tokenize(val error: Pc1245BasicTokenizeError) : DesktopBasicProgramCompileError
    data class S1Tokenize(val error: com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeError) : DesktopBasicProgramCompileError
    data class S2Tokenize(val error: com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicTokenizeError) : DesktopBasicProgramCompileError
    data class UnsupportedMachine(val machineId: MachineId) : DesktopBasicProgramCompileError
}

internal sealed interface DesktopBasicProgramDecodeResult {
    class Success(bytes: ByteArray) : DesktopBasicProgramDecodeResult {
        val utf8Bytes: ByteArray = bytes.copyOf()
    }
    data class Failure(val error: Pc1245BasicDetokenizeError) : DesktopBasicProgramDecodeResult
    data class GenericFailure(val offset: Int, val message: String) : DesktopBasicProgramDecodeResult
}
