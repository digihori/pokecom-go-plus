package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineMemoryRegion
import com.digihori.pgp.core.api.MachineMemoryRegionKind
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.core.debug.Sc61860InstructionFormatter
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.debug.DesktopDisassemblyModel
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState
import java.time.Instant
import java.util.UUID

internal interface DebugContextService {
    fun capabilities(): DebugContextCapabilities
    fun capture(request: DebugContextRequest): DebugContextResult
}

internal class DesktopDebugContextService(
    private val runner: DesktopEmulatorRunner,
    private val sessionId: String = UUID.randomUUID().toString(),
    private val capturedAtProvider: () -> String = { Instant.now().toString() },
    private val executedCyclesProvider: () -> Long? = { null },
) : DebugContextService {
    override fun capabilities(): DebugContextCapabilities = DebugContextCapabilities(
        format = DebugContextV1.FORMAT,
        schemaVersion = DebugContextV1.SCHEMA_VERSION,
        machineId = runner.machineId.value,
        maxMemoryRanges = MAX_MEMORY_RANGES,
        maxMemoryBytes = MAX_MEMORY_BYTES,
        maxDisassemblyInstructions = MAX_DISASSEMBLY_INSTRUCTIONS,
        maxHistoryEntries = MAX_HISTORY_ENTRIES,
        memoryAccessHistorySupported = true,
        bankHistorySupported = runner.selectedRomBank() != null,
        romBytesAllowed = false,
    )

    override fun capture(request: DebugContextRequest): DebugContextResult {
        if (runner.state == RunnerState.RUNNING) return DebugContextResult.Failure(DebugContextError.SessionRunning)
        validate(request)?.let { return DebugContextResult.Failure(it) }

        val definition = MachineCatalog.require(runner.machineId)
        val forbiddenRange = request.memoryRanges.firstOrNull { range ->
            (range.startAddress until range.startAddress + range.length).any { address ->
                definition.memoryRegions.any { region ->
                    address in region.startAddress..region.endAddressInclusive && region.isRom(definition.memoryRegions)
                }
            }
        }
        if (forbiddenRange != null) {
            return DebugContextResult.Failure(
                DebugContextError.RomBytesNotAllowed(forbiddenRange.startAddress, forbiddenRange.length),
            )
        }

        val cpu = runner.cpuSnapshot()
        val memory = request.memoryRanges.map { range ->
            val snapshot = runner.memorySnapshot(range.startAddress, range.length)
            DebugMemoryContext(
                startAddress = snapshot.startAddress,
                byteLength = snapshot.size,
                regionId = definition.memoryRegions.firstOrNull {
                    range.startAddress >= it.startAddress &&
                        range.startAddress + range.length - 1 <= it.endAddressInclusive
                }?.id,
                data = snapshot.copyBytes().hex(),
            )
        }
        val disassembly = request.disassemblyStartAddress?.let { start ->
            DesktopDisassemblyModel.build(start, request.disassemblyInstructionCount, runner::memoryByte).map { line ->
                val instruction = line.instruction
                DebugDisassemblyContext(
                    address = instruction.address,
                    bytes = instruction.bytes,
                    mnemonic = instruction.definition?.mnemonic,
                    operandValue = instruction.operandValue,
                    targetAddress = instruction.targetAddress,
                    text = line.instructionText,
                    romLocation = runner.resolveRomLocation(instruction.address).toContext(),
                )
            }
        }.orEmpty()

        val accessHistory = runner.memoryAccessHistory()
        val selectedAccessHistory = accessHistory.takeLast(request.memoryAccessHistoryLimit)
        val bankHistory = runner.bankHistory()
        val selectedBankHistory = bankHistory.takeLast(request.bankHistoryLimit)
        val trace = runner.instructionTrace()
        val selectedTrace = trace.takeLast(request.traceLimit)

        return DebugContextResult.Success(
            DebugContextV1(
                capturedAt = capturedAtProvider(),
                session = DebugSessionContext(
                    id = sessionId,
                    revision = runner.sessionRevision,
                    machineId = runner.machineId.value,
                    runState = runner.state.name.lowercase(),
                    executedCycles = executedCyclesProvider(),
                ),
                stop = runner.stopReason.toContext(),
                cpu = DebugCpuContext(
                    programCounter = cpu.programCounter,
                    currentProgramCounter = cpu.currentProgramCounter,
                    opcode = cpu.opcode,
                    dataPointer = cpu.dataPointer,
                    p = cpu.p,
                    q = cpu.q,
                    r = cpu.r,
                    d = cpu.d,
                    alu = cpu.alu,
                    carry = cpu.carry,
                    zero = cpu.zero,
                    xInput = cpu.xInput,
                    powerOn = cpu.powerOn,
                    ia = cpu.ia,
                    ib = cpu.ib,
                    fo = cpu.fo,
                    control = cpu.control,
                    testPort = cpu.testPort,
                    internalRamHex = cpu.copyInternalRam().hex(),
                    internalRamByteLength = cpu.internalRamSize,
                ),
                rom = DebugRomContext(
                    selectedBank = runner.selectedRomBank(),
                    currentLocation = runner.resolveRomLocation(cpu.programCounter).toContext(),
                ),
                memory = memory,
                disassembly = disassembly,
                memoryAccessHistory = DebugMemoryAccessHistoryContext(
                    enabled = runner.isDebugObservationEnabled(),
                    truncated = accessHistory.size > selectedAccessHistory.size ||
                        (accessHistory.firstOrNull()?.sequence ?: 0) > 0,
                    entries = selectedAccessHistory.map { entry ->
                        entry.access.toContext(entry.sequence, entry.instructionAddress)
                    },
                ),
                bankHistory = DebugBankHistoryContext(
                    supported = runner.selectedRomBank() != null,
                    truncated = bankHistory.size > selectedBankHistory.size ||
                        (bankHistory.firstOrNull()?.sequence ?: 0) > 0,
                    entries = selectedBankHistory.map { entry ->
                        DebugBankHistoryEntryContext(
                            sequence = entry.sequence,
                            previousBank = entry.event.previousBank,
                            selectedBank = entry.event.selectedBank,
                            selectorValue = entry.event.selectorValue,
                        )
                    },
                ),
                trace = DebugTraceContext(
                    enabled = runner.isInstructionTraceEnabled(),
                    capacity = runner.instructionTraceCapacity(),
                    truncated = trace.size > selectedTrace.size || (trace.firstOrNull()?.sequence ?: 0) > 0,
                    entries = selectedTrace.map { entry ->
                        DebugTraceEntryContext(
                            sequence = entry.sequence,
                            address = entry.instruction.address,
                            bytes = entry.instruction.bytes,
                            text = Sc61860InstructionFormatter.format(entry.instruction),
                            romLocation = entry.romLocation.toContext(),
                            dataPointer = entry.dataPointer,
                            p = entry.p,
                            q = entry.q,
                            r = entry.r,
                            d = entry.d,
                            carry = entry.carry,
                            zero = entry.zero,
                        )
                    },
                ),
            ),
        )
    }

    private fun validate(request: DebugContextRequest): DebugContextError.InvalidRequest? {
        if (request.memoryRanges.size > MAX_MEMORY_RANGES) {
            return DebugContextError.InvalidRequest("At most $MAX_MEMORY_RANGES memory ranges may be requested")
        }
        if (request.memoryRanges.any {
                it.startAddress !in 0..0xffff || it.length <= 0 || it.length > 0x10000 - it.startAddress
            }
        ) {
            return DebugContextError.InvalidRequest("Memory ranges must be non-empty and remain inside the 16-bit address space")
        }
        if (request.memoryRanges.sumOf { it.length.toLong() } > MAX_MEMORY_BYTES) {
            return DebugContextError.InvalidRequest("At most $MAX_MEMORY_BYTES memory bytes may be requested")
        }
        if (request.disassemblyStartAddress != null && request.disassemblyStartAddress !in 0..0xffff) {
            return DebugContextError.InvalidRequest("Disassembly start address must be a 16-bit address")
        }
        if (request.disassemblyInstructionCount !in 0..MAX_DISASSEMBLY_INSTRUCTIONS) {
            return DebugContextError.InvalidRequest("Disassembly instruction count is out of range")
        }
        if (request.disassemblyStartAddress == null && request.disassemblyInstructionCount != 0) {
            return DebugContextError.InvalidRequest("Disassembly start address is required when instructions are requested")
        }
        if (request.memoryAccessHistoryLimit !in 0..MAX_HISTORY_ENTRIES ||
            request.bankHistoryLimit !in 0..MAX_HISTORY_ENTRIES || request.traceLimit !in 0..MAX_HISTORY_ENTRIES
        ) {
            return DebugContextError.InvalidRequest("History and trace limits must be between 0 and $MAX_HISTORY_ENTRIES")
        }
        return null
    }

    private fun MachineMemoryRegion.isRom(regions: List<MachineMemoryRegion>): Boolean = when (kind) {
        MachineMemoryRegionKind.ROM -> true
        MachineMemoryRegionKind.MIRROR -> regions.firstOrNull { it.id == mirrorsRegionId }?.isRom(regions) == true
        else -> false
    }

    private fun DebuggerStopReason?.toContext(): DebugStopContext? = when (this) {
        null -> null
        DebuggerStopReason.Reset -> DebugStopContext.Reset
        DebuggerStopReason.UserPause -> DebugStopContext.UserPause
        DebuggerStopReason.StepComplete -> DebugStopContext.StepComplete
        is DebuggerStopReason.Breakpoint -> DebugStopContext.Breakpoint(address)
        is DebuggerStopReason.RunToAddress -> DebugStopContext.RunToAddress(address)
        is DebuggerStopReason.MemoryChanged -> DebugStopContext.MemoryChanged(
            instructionAddress,
            changes.map { DebugMemoryChangeContext(it.address, it.before, it.after) },
        )
        is DebuggerStopReason.MemoryAccessed -> DebugStopContext.MemoryAccessed(
            instructionAddress,
            accesses.map { it.toContext(null, instructionAddress) },
        )
        is DebuggerStopReason.Fault -> when (val value = fault) {
            is CoreFault.UnsupportedOpcode -> DebugStopContext.Fault(
                faultKind = "unsupportedOpcode",
                address = value.address,
                opcode = value.opcode,
            )
        }
    }

    private fun MemoryAccess.toContext(sequence: Long?, instructionAddress: Int?) = DebugMemoryAccessContext(
        sequence = sequence,
        instructionAddress = instructionAddress,
        kind = kind,
        address = address,
        value = value,
    )

    private fun PhysicalRomLocation?.toContext(): DebugRomLocationContext? = this?.let {
        DebugRomLocationContext(componentId.value, bank, offset)
    }

    private fun ByteArray.hex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    companion object {
        const val MAX_MEMORY_RANGES: Int = 8
        const val MAX_MEMORY_BYTES: Int = 4_096
        const val MAX_DISASSEMBLY_INSTRUCTIONS: Int = 256
        const val MAX_HISTORY_ENTRIES: Int = 4_096
    }
}
