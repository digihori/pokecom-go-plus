package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.source.basic.*
import com.digihori.pgp.core.source.machine.*

public enum class PlayerProgramFormat { BASIC, MEMORY_DUMP }

public data class PlayerProgramRange(public val startAddress: Int, public val endAddress: Int)

/** Parsed input owns its bytes. Hosts supply a format explicitly; bin/hex are not supported. */
public class PlayerProgramInput internal constructor(
    public val machineId: MachineId,
    public val format: PlayerProgramFormat,
    program: ByteArray?,
    internal val image: AddressedMemoryImage?,
) {
    private val basic = program?.copyOf()
    internal fun copyProgram(): ByteArray? = basic?.copyOf()
}

public sealed interface PlayerProgramParseResult {
    public data class Success(public val input: PlayerProgramInput) : PlayerProgramParseResult
    public data class Failure(public val error: PlayerProgramError) : PlayerProgramParseResult
}

public sealed interface PlayerProgramError {
    public data object NotPaused : PlayerProgramError
    public data object WrongMachine : PlayerProgramError
    public data object InvalidUtf8 : PlayerProgramError
    public data object EmptyDump : PlayerProgramError
    public data object InvalidRange : PlayerProgramError
    public data class BasicCompile(public val error: BasicProgramCompileError) : PlayerProgramError
    public data class BasicMemory(public val error: BasicProgramMemoryError) : PlayerProgramError
    public data class BasicDecode(public val result: BasicProgramDecodeResult) : PlayerProgramError
    public data class DumpParse(public val error: PgpMemoryDumpError) : PlayerProgramError
    public data class Placement(public val error: MemoryImageLoadError) : PlayerProgramError
    public data class NonRam(public val address: Int) : PlayerProgramError
    public data class BasicOverlap(public val address: Int) : PlayerProgramError
}

public sealed interface PlayerProgramCheckResult {
    public data class Success(
        public val ranges: List<PlayerProgramRange>,
        public val byteCount: Int,
        public val replacesBasic: Boolean,
    ) : PlayerProgramCheckResult
    public data class Failure(public val error: PlayerProgramError) : PlayerProgramCheckResult
}

public sealed interface PlayerProgramApplyResult {
    public data object Success : PlayerProgramApplyResult
    public data class Failure(public val error: PlayerProgramError) : PlayerProgramApplyResult
}

public sealed interface PlayerProgramExportResult {
    public class Success(bytes: ByteArray) : PlayerProgramExportResult {
        private val content = bytes.copyOf()
        public fun copyBytes(): ByteArray = content.copyOf()
    }
    public data class Failure(public val error: PlayerProgramError) : PlayerProgramExportResult
}

public object PlayerProgramParser {
    public fun parse(bytes: ByteArray, format: PlayerProgramFormat, machineId: MachineId): PlayerProgramParseResult {
        if (format == PlayerProgramFormat.BASIC) {
            return when (val compiled = BasicProgramCodec.compileProgram(bytes, machineId)) {
                is BasicProgramCompileResult.Success -> PlayerProgramParseResult.Success(
                    PlayerProgramInput(machineId, format, compiled.bytes, null),
                )
                is BasicProgramCompileResult.Failure -> PlayerProgramParseResult.Failure(PlayerProgramError.BasicCompile(compiled.error))
            }
        }
        val text = runCatching { bytes.decodeToString(throwOnInvalidSequence = true) }.getOrNull()
            ?: return PlayerProgramParseResult.Failure(PlayerProgramError.InvalidUtf8)
        return when (val parsed = PgpMemoryDumpParser.parse(text)) {
            is PgpMemoryDumpParseResult.Failure -> PlayerProgramParseResult.Failure(PlayerProgramError.DumpParse(parsed.error))
            is PgpMemoryDumpParseResult.Success -> if (parsed.image.byteCount == 0) {
                PlayerProgramParseResult.Failure(PlayerProgramError.EmptyDump)
            } else PlayerProgramParseResult.Success(PlayerProgramInput(machineId, format, null, parsed.image))
        }
    }
}

/** No files, clocks, threads or UI. Host serializes calls and owns confirmation/key release. */
internal class PlayerProgramIo(private val session: EmulatorSession, private val runner: PlayerRunner) {
    fun check(input: PlayerProgramInput): PlayerProgramCheckResult {
        if (runner.state != PlayerRunState.PAUSED) return PlayerProgramCheckResult.Failure(PlayerProgramError.NotPaused)
        if (input.machineId != session.machineId) return PlayerProgramCheckResult.Failure(PlayerProgramError.WrongMachine)
        val memory = session.memorySnapshot(0, 0x10000).copyBytes()
        input.image?.let { image ->
            MemoryPlacementValidator.validate(image, session.memoryLayout)?.let {
                return PlayerProgramCheckResult.Failure(PlayerProgramError.Placement(it.error))
            }
            image.segments.forEach { segment ->
                for (address in segment.startAddress..segment.endAddress) {
                    if (session.memoryLayout.locate(address).kind != MachineMemoryKind.RAM)
                        return PlayerProgramCheckResult.Failure(PlayerProgramError.NonRam(address))
                }
            }
        }
        return when (val planned = ProgramImageLoader.plan(session.memoryLayout, input.copyProgram(), input.image) { memory[it].toInt() and 0xff }) {
            is ProgramImagePlanResult.BasicFailure -> PlayerProgramCheckResult.Failure(PlayerProgramError.BasicMemory(planned.error))
            is ProgramImagePlanResult.MemoryFailure -> PlayerProgramCheckResult.Failure(PlayerProgramError.Placement(planned.error))
            is ProgramImagePlanResult.Success -> {
                val ranges = if (input.image != null) input.image.segments.map { PlayerProgramRange(it.startAddress, it.endAddress) }
                    else planned.image?.segments?.filter { it.startAddress != session.memoryLayout.basicStartPointerAddress }
                        ?.map { PlayerProgramRange(it.startAddress, it.endAddress) }.orEmpty()
                PlayerProgramCheckResult.Success(ranges, input.copyProgram()?.size ?: input.image?.byteCount ?: 0, input.format == PlayerProgramFormat.BASIC)
            }
        }
    }

    fun apply(input: PlayerProgramInput): PlayerProgramApplyResult {
        val checked = check(input)
        if (checked is PlayerProgramCheckResult.Failure) return PlayerProgramApplyResult.Failure(checked.error)
        return when (val result = session.loadProgramImage(input.copyProgram(), input.image)) {
            is ProgramImageLoadResult.Success -> PlayerProgramApplyResult.Success
            is ProgramImageLoadResult.BasicFailure -> PlayerProgramApplyResult.Failure(PlayerProgramError.BasicMemory(result.error))
            is ProgramImageLoadResult.MemoryFailure -> PlayerProgramApplyResult.Failure(PlayerProgramError.Placement(result.error))
        }
    }

    fun exportBasic(): PlayerProgramExportResult {
        if (runner.state != PlayerRunState.PAUSED) return PlayerProgramExportResult.Failure(PlayerProgramError.NotPaused)
        return when (val snapshot = session.basicProgramSnapshot()) {
            is BasicProgramSnapshotResult.Failure -> PlayerProgramExportResult.Failure(PlayerProgramError.BasicMemory(snapshot.error))
            is BasicProgramSnapshotResult.Success -> when (val decoded = BasicProgramCodec.detokenizeProgram(snapshot.copyBytes(), session.machineId)) {
                is BasicProgramDecodeResult.Success -> PlayerProgramExportResult.Success(decoded.utf8Bytes)
                else -> PlayerProgramExportResult.Failure(PlayerProgramError.BasicDecode(decoded))
            }
        }
    }

    fun exportDump(start: Int, end: Int): PlayerProgramExportResult {
        if (runner.state != PlayerRunState.PAUSED) return PlayerProgramExportResult.Failure(PlayerProgramError.NotPaused)
        if (start !in 0..0xffff || end !in start..0xffff) return PlayerProgramExportResult.Failure(PlayerProgramError.InvalidRange)
        val owners = mutableMapOf<Int, Int>()
        for (address in start..end) {
            val location = session.memoryLayout.locate(address)
            if (location.kind != MachineMemoryKind.RAM || !location.writable) return PlayerProgramExportResult.Failure(PlayerProgramError.NonRam(address))
            val previous = owners.put(location.physicalAddress, address)
            if (previous != null) return PlayerProgramExportResult.Failure(
                PlayerProgramError.Placement(MemoryImageLoadError.MirrorOverlap(address, previous, location.physicalAddress, 0)),
            )
        }
        val bytes = session.memorySnapshot(start, end - start + 1).copyBytes()
        return PlayerProgramExportResult.Success(PgpMemoryDumpWriter.write(start, bytes).encodeToByteArray())
    }
}
