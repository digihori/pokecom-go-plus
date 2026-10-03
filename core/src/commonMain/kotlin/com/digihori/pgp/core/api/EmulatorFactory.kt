package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.cpu.Sc61860StopReason
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Machine
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Display
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet

public object EmulatorFactory {
    public fun supportedMachineIds(): List<MachineId> = listOf(Pc1245RomDefinition.MACHINE_ID)

    public fun create(machineId: MachineId, romSet: RomSet): CreateSessionResult {
        if (machineId != Pc1245RomDefinition.MACHINE_ID) {
            return CreateSessionResult.Failure(CreateSessionError.UnsupportedMachine(machineId))
        }
        if (romSet.machineId != machineId) {
            return CreateSessionResult.Failure(
                CreateSessionError.MachineIdMismatch(expected = machineId, actual = romSet.machineId),
            )
        }

        return try {
            CreateSessionResult.Success(Pc1245EmulatorSession(Pc1245Machine(romSet)))
        } catch (error: IllegalArgumentException) {
            CreateSessionResult.Failure(
                CreateSessionError.InvalidRomSet(error.message ?: "Invalid ROM set"),
            )
        }
    }
}

public sealed interface CreateSessionResult {
    public data class Success(public val session: EmulatorSession) : CreateSessionResult
    public data class Failure(public val error: CreateSessionError) : CreateSessionResult
}

public sealed interface CreateSessionError {
    public data class UnsupportedMachine(public val machineId: MachineId) : CreateSessionError
    public data class MachineIdMismatch(
        public val expected: MachineId,
        public val actual: MachineId,
    ) : CreateSessionError

    public data class InvalidRomSet(public val reason: String) : CreateSessionError
}

private class Pc1245EmulatorSession(
    private val machine: Pc1245Machine,
) : EmulatorSession {
    override val machineId: MachineId = Pc1245RomDefinition.MACHINE_ID

    private var status: ExecutionStatus = ExecutionStatus.Ready

    override fun reset() {
        machine.coldReset()
        status = ExecutionStatus.Ready
    }

    override fun step(): StepResult {
        val result = machine.step()
        status = result.stopReason.toExecutionStatus()
        return StepResult(cycles = result.cycles, status = status)
    }

    override fun runCycles(cycleBudget: Long): RunResult {
        val result = machine.runCycles(cycleBudget)
        status = result.stopReason.toExecutionStatus()
        return RunResult(
            executedCycles = result.executedCycles,
            executedInstructions = result.executedInstructions,
            status = status,
        )
    }

    override fun pressKey(key: PocketKey): InputResult =
        if (machine.keyboardState.press(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)

    override fun releaseKey(key: PocketKey): InputResult =
        if (machine.keyboardState.release(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)

    override fun setOperatingMode(mode: OperatingMode) {
        machine.keyboardState.setOperatingMode(mode)
    }

    override fun cpuSnapshot(): CpuSnapshot {
        val state = machine.cpuState
        return CpuSnapshot(
            programCounter = state.programCounter,
            currentProgramCounter = state.currentProgramCounter,
            opcode = state.opcode,
            dataPointer = state.dataPointer,
            p = state.p,
            q = state.q,
            r = state.r,
            d = state.d,
            alu = state.alu,
            carry = state.carry,
            zero = state.zero,
            xInput = state.xInput,
            powerOn = state.powerOn,
            ia = state.ia,
            ib = state.ib,
            fo = state.fo,
            control = state.control,
            testPort = state.testPort,
            internalRam = ByteArray(state.internalRam.size) { state.internalRam[it].toByte() },
        )
    }

    override fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot {
        require(startAddress in 0..0xffff) { "Start address must be a 16-bit value" }
        require(length >= 0) { "Length must not be negative" }
        require(length <= 0x10000 - startAddress) { "Memory range exceeds the 16-bit address space" }

        return MemorySnapshot(
            startAddress = startAddress,
            bytes = ByteArray(length) { offset -> machine.readMemory(startAddress + offset).toByte() },
        )
    }

    override fun displaySnapshot(): DisplaySnapshot {
        val display = machine.displayState
        val sourceColumns = display.copyDotColumns()
        val dotColumns = Pc1245Display.DOT_COLUMN_COUNT
        val dots = ByteArray(dotColumns * Pc1245Display.DOT_ROWS)

        for (column in 0 until dotColumns) {
            val columnBits = sourceColumns[column].toInt() and 0xff
            for (row in 0 until Pc1245Display.DOT_ROWS) {
                if (columnBits and (1 shl row) != 0) {
                    dots[row * dotColumns + column] = 1
                }
            }
        }

        return DisplaySnapshot(
            characterColumns = Pc1245Display.CHARACTER_COLUMNS,
            characterWidth = Pc1245Display.CHARACTER_WIDTH,
            dotRows = Pc1245Display.DOT_ROWS,
            symbols = activeDisplaySymbols(
                state0 = display.symbolState0(),
                state1 = display.symbolState1(),
                operatingMode = machine.keyboardState.operatingMode,
            ),
            enabled = display.enabled,
            revision = display.revision + machine.keyboardState.modeRevision,
            dots = dots,
        )
    }

    override fun audioSnapshot(): AudioSnapshot = AudioSnapshot(
        frequencyHz = machine.buzzerState.frequencyHz,
        revision = machine.buzzerState.revision,
    )
}

private fun activeDisplaySymbols(
    state0: Int,
    state1: Int,
    operatingMode: OperatingMode,
): List<DisplaySymbol> = buildList {
    if (state1 and 0x01 != 0) add(DisplaySymbol.BUSY)
    if (state0 and 0x02 != 0) add(DisplaySymbol.P)
    if (state0 and 0x01 != 0) add(DisplaySymbol.DEF)
    if (state0 and 0x08 != 0) add(DisplaySymbol.DE)
    if (state0 and 0x04 != 0) add(DisplaySymbol.G)
    if (state1 and 0x04 != 0) add(DisplaySymbol.RAD)
    if (state1 and 0x02 != 0) add(DisplaySymbol.SHIFT)
    add(if (operatingMode == OperatingMode.PROGRAM) DisplaySymbol.PRO else DisplaySymbol.RUN)
}

private fun Sc61860StopReason?.toExecutionStatus(): ExecutionStatus = when (this) {
    null -> ExecutionStatus.Ready
    is Sc61860StopReason.UnsupportedOpcode -> ExecutionStatus.Faulted(
        CoreFault.UnsupportedOpcode(address = address, opcode = opcode),
    )
}
