package com.digihori.pgp.core.source.basic

import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizeError
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261BasicTokenizer
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicCodec
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicTokenizeResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.basic.BasicTextParseError
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser

/** Pure machine-selected BASIC conversion, shared by Studio and Player. */
public object BasicProgramCodec {
    fun compilePc1245Program(bytes: ByteArray): BasicProgramCompileResult {
        return compileProgram(bytes, Pc1245RomDefinition.MACHINE_ID)
    }

    fun compileProgram(bytes: ByteArray, machineId: MachineId): BasicProgramCompileResult {
        val text = decodeUtf8(bytes) ?: return BasicProgramCompileResult.Failure(
            BasicProgramCompileError.InvalidUtf8,
        )
        if (MachineCatalog.find(machineId) == null) return BasicProgramCompileResult.Failure(
            BasicProgramCompileError.UnsupportedMachine(machineId),
        )
        val document = when (val parsed = BasicTextParser.parse(withoutDescriptionHeader(text))) {
            is BasicTextParseResult.Failure -> return BasicProgramCompileResult.Failure(
                BasicProgramCompileError.Parse(parsed.error),
            )
            is BasicTextParseResult.Success -> parsed.document
        }
        return when (MachineCatalog.find(machineId)?.basicDialect) {
            com.digihori.pgp.core.api.MachineBasicDialect.S1 -> {
            val tokenizerResult = if (machineId == Pc1401RomDefinition.MACHINE_ID) {
                Pc1401BasicTokenizer.tokenize(document)
            } else if (machineId == Pc1261RomDefinition.MACHINE_ID) {
                Pc1261BasicTokenizer.tokenize(document)
            } else {
                Pc1350BasicTokenizer.tokenize(document)
            }
            when (val tokenized = tokenizerResult) {
                is Pc1350BasicTokenizeResult.Success -> BasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1350BasicTokenizeResult.Failure -> BasicProgramCompileResult.Failure(
                    BasicProgramCompileError.S1Tokenize(tokenized.error),
                )
            }}
            com.digihori.pgp.core.api.MachineBasicDialect.S2 -> when (val tokenized = Pc1360BasicCodec.tokenize(document)) {
                is Pc1360BasicTokenizeResult.Success -> BasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1360BasicTokenizeResult.Failure -> BasicProgramCompileResult.Failure(
                    BasicProgramCompileError.S2Tokenize(tokenized.error),
                )
            }
            else -> when (val tokenized = Pc1245BasicTokenizer.tokenize(document)) {
                is Pc1245BasicTokenizeResult.Success -> BasicProgramCompileResult.Success(tokenized.bytes)
                is Pc1245BasicTokenizeResult.Failure -> BasicProgramCompileResult.Failure(
                    BasicProgramCompileError.Tokenize(tokenized.error),
                )
            }
        }
    }

    fun detokenizePc1245Program(bytes: ByteArray): BasicProgramDecodeResult =
        when (val decoded = Pc1245BasicDetokenizer.detokenize(bytes)) {
            is Pc1245BasicDetokenizeResult.Success -> BasicProgramDecodeResult.Success(
                (decoded.source + "\n").encodeToByteArray(),
            )
            is Pc1245BasicDetokenizeResult.Failure -> BasicProgramDecodeResult.Failure(decoded.error)
        }

    fun detokenizeProgram(bytes: ByteArray, machineId: MachineId): BasicProgramDecodeResult =
        if (MachineCatalog.find(machineId) == null) BasicProgramDecodeResult.UnsupportedMachine(machineId)
        else if (machineId == Pc1401RomDefinition.MACHINE_ID) {
            when (val decoded = Pc1401BasicTokenizer.detokenize(bytes)) {
                is Pc1350BasicDetokenizeResult.Success -> BasicProgramDecodeResult.Success((decoded.source + "\n").encodeToByteArray())
                is Pc1350BasicDetokenizeResult.Failure -> BasicProgramDecodeResult.GenericFailure(decoded.error.offset, decoded.error.message)
            }
        } else if (machineId == Pc1261RomDefinition.MACHINE_ID) {
            when (val decoded = Pc1261BasicTokenizer.detokenize(bytes)) {
                is Pc1350BasicDetokenizeResult.Success -> BasicProgramDecodeResult.Success((decoded.source + "\n").encodeToByteArray())
                is Pc1350BasicDetokenizeResult.Failure -> BasicProgramDecodeResult.GenericFailure(decoded.error.offset, decoded.error.message)
            }
        } else if (MachineCatalog.find(machineId)?.basicDialect == com.digihori.pgp.core.api.MachineBasicDialect.S1) {
            when (val decoded = Pc1350BasicTokenizer.detokenize(bytes)) {
                is Pc1350BasicDetokenizeResult.Success -> BasicProgramDecodeResult.Success((decoded.source + "\n").encodeToByteArray())
                is Pc1350BasicDetokenizeResult.Failure -> BasicProgramDecodeResult.GenericFailure(decoded.error.offset, decoded.error.message)
            }
        } else if (MachineCatalog.find(machineId)?.basicDialect == com.digihori.pgp.core.api.MachineBasicDialect.S2) {
            when (val decoded = Pc1360BasicCodec.detokenize(bytes)) {
                is Pc1360BasicDetokenizeResult.Success -> BasicProgramDecodeResult.Success((decoded.source + "\n").encodeToByteArray())
                is Pc1360BasicDetokenizeResult.Failure -> BasicProgramDecodeResult.GenericFailure(decoded.error.offset, decoded.error.message)
            }
        } else detokenizePc1245Program(bytes)

    /** Studio's leading # description lines are comments, not BASIC statements. Keep line numbers. */
    private fun withoutDescriptionHeader(text: String): String {
        var inHeader = true
        return text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
            .split('\n').joinToString("\n") { line ->
                if (inHeader && line.trimStart().startsWith('#')) ""
                else { inHeader = false; line }
            }
    }

    private fun decodeUtf8(bytes: ByteArray): String? =
        runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
}

public sealed interface BasicProgramCompileResult {
    class Success(bytes: ByteArray) : BasicProgramCompileResult {
        val bytes: ByteArray = bytes.copyOf()
    }
    data class Failure(val error: BasicProgramCompileError) : BasicProgramCompileResult
}

public sealed interface BasicProgramCompileError {
    data object InvalidUtf8 : BasicProgramCompileError
    data class Parse(val error: BasicTextParseError) : BasicProgramCompileError
    data class Tokenize(val error: Pc1245BasicTokenizeError) : BasicProgramCompileError
    data class S1Tokenize(val error: com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeError) : BasicProgramCompileError
    data class S2Tokenize(val error: com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicTokenizeError) : BasicProgramCompileError
    data class UnsupportedMachine(val machineId: MachineId) : BasicProgramCompileError
}

public sealed interface BasicProgramDecodeResult {
    class Success(bytes: ByteArray) : BasicProgramDecodeResult {
        val utf8Bytes: ByteArray = bytes.copyOf()
    }
    data class Failure(val error: Pc1245BasicDetokenizeError) : BasicProgramDecodeResult
    data class UnsupportedMachine(val machineId: MachineId) : BasicProgramDecodeResult
    data class GenericFailure(val offset: Int, val message: String) : BasicProgramDecodeResult
}
