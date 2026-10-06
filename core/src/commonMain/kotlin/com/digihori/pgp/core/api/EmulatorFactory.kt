package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.cpu.Sc61860StopReason
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Machine
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Display
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Buzzer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251Machine
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251MemoryImageLoadResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251Display
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350Display
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350Machine
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet

public object EmulatorFactory {
    public fun supportedMachineIds(): List<MachineId> = MachineCatalog.definitions.map(MachineDefinition::id)

    public fun create(
        machineId: MachineId,
        romSet: RomSet,
        configuration: EmulatorConfiguration = EmulatorConfiguration(),
    ): CreateSessionResult {
        if (machineId !in supportedMachineIds()) {
            return CreateSessionResult.Failure(CreateSessionError.UnsupportedMachine(machineId))
        }
        if (romSet.machineId != machineId) {
            return CreateSessionResult.Failure(
                CreateSessionError.MachineIdMismatch(expected = machineId, actual = romSet.machineId),
            )
        }

        return try {
            CreateSessionResult.Success(
                when (MachineCatalog.require(machineId).family) {
                    MachineFamily.PC_1245 -> Pc1245EmulatorSession(Pc1245Machine(romSet))
                    MachineFamily.PC_1251 -> {
                        val model = requireNotNull(Pc1251FamilyModel.fromMachineId(machineId))
                        Pc1251EmulatorSession(
                            machineId,
                            Pc1251Machine(romSet, model, configuration.pc1251FamilyMemoryMode),
                        )
                    }
                    MachineFamily.PC_1350 -> Pc1350EmulatorSession(Pc1350Machine(romSet))
                },
            )
        } catch (error: IllegalArgumentException) {
            CreateSessionResult.Failure(
                CreateSessionError.InvalidRomSet(error.message ?: "Invalid ROM set"),
            )
        }
    }
}

private class Pc1350EmulatorSession(
    private val machine: Pc1350Machine,
) : EmulatorSession {
    override val machineId: MachineId = Pc1350RomDefinition.MACHINE_ID
    private var status: ExecutionStatus = ExecutionStatus.Ready

    override fun reset() { machine.coldReset(); status = ExecutionStatus.Ready }
    override fun step(): StepResult {
        val result = machine.step()
        status = result.stopReason.toExecutionStatus()
        return StepResult(result.cycles, status)
    }
    override fun runCycles(cycleBudget: Long): RunResult {
        val result = machine.runCycles(cycleBudget)
        status = result.stopReason.toExecutionStatus()
        return RunResult(result.executedCycles, result.executedInstructions, status)
    }
    override fun pressKey(key: PocketKey): InputResult =
        if (machine.keyboardState.press(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)
    override fun releaseKey(key: PocketKey): InputResult =
        if (machine.keyboardState.release(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)
    override fun setOperatingMode(mode: OperatingMode) = Unit
    override fun cpuSnapshot(): CpuSnapshot {
        val state = machine.cpuState
        return CpuSnapshot(
            state.programCounter, state.currentProgramCounter, state.opcode, state.dataPointer,
            state.p, state.q, state.r, state.d, state.alu, state.carry, state.zero, state.xInput,
            state.powerOn, state.ia, state.ib, state.fo, state.control, state.testPort,
            ByteArray(state.internalRam.size) { state.internalRam[it].toByte() },
        )
    }
    override fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot {
        require(startAddress in 0..0xffff)
        require(length >= 0 && length <= 0x10000 - startAddress)
        return MemorySnapshot(startAddress, ByteArray(length) { machine.readMemory(startAddress + it).toByte() })
    }
    override fun displaySnapshot(): DisplaySnapshot {
        val display = machine.displayState
        val state = display.symbolState()
        return DisplaySnapshot(
            characterColumns = Pc1350Display.CHARACTER_COLUMNS,
            characterWidth = Pc1350Display.CHARACTER_WIDTH,
            dotRows = Pc1350Display.DOT_ROWS,
            characterRows = Pc1350Display.CHARACTER_ROWS,
            symbols = buildList {
                if (state and 0x01 != 0) add(DisplaySymbol.SHIFT)
                if (state and 0x02 != 0) add(DisplaySymbol.DEF)
                if (state and 0x10 != 0) add(DisplaySymbol.RUN)
                if (state and 0x20 != 0) add(DisplaySymbol.PRO)
                if (state and 0x40 != 0) add(DisplaySymbol.KANA)
                if (state and 0x80 != 0) add(DisplaySymbol.SMALL)
            },
            enabled = true,
            revision = display.revision,
            dots = display.copyDots(),
        )
    }
    override fun audioSnapshot(): AudioSnapshot = AudioSnapshot(machine.buzzerState.frequencyHz, machine.buzzerState.revision)
    override fun drainAudioSamples(): AudioPcmSnapshot =
        AudioPcmSnapshot(Pc1245Buzzer.SAMPLE_RATE, machine.buzzerState.drainPcm())
    override fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult =
        machine.loadBasicProgram(program).toLoadResult()
    override fun basicProgramSnapshot(): BasicProgramSnapshotResult =
        machine.basicProgram().toSnapshotResult()
    override fun loadMemoryImage(image: com.digihori.pgp.core.source.machine.AddressedMemoryImage): MemoryImageLoadResult {
        val invalid = image.segments.firstNotNullOfOrNull { segment ->
            (segment.startAddress..segment.endAddress).firstOrNull { it !in 0x2000..0x7fff }
                ?.let { MemoryImageLoadError.ReadOnlyAddress(it, segment.sourceLine) }
        }
        if (invalid != null) return MemoryImageLoadResult.Failure(invalid)
        image.segments.forEach { segment ->
            segment.copyBytes().forEachIndexed { offset, byte ->
                machine.writeMemory(segment.startAddress + offset, byte.toInt() and 0xff)
            }
        }
        return MemoryImageLoadResult.Success(image.segments.size, image.byteCount)
    }
    override fun setMemoryAccessTracing(enabled: Boolean) = machine.setMemoryAccessTracing(enabled)
    override fun drainMemoryAccesses(): List<MemoryAccess> = machine.drainMemoryAccesses()
}

public data class EmulatorConfiguration(
    public val pc1251FamilyMemoryMode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED,
)

private class Pc1251EmulatorSession(
    override val machineId: MachineId,
    private val machine: Pc1251Machine,
) : EmulatorSession {
    private var status: ExecutionStatus = ExecutionStatus.Ready

    override fun reset() { machine.coldReset(); status = ExecutionStatus.Ready }
    override fun step(): StepResult {
        val result = machine.step()
        status = result.stopReason.toExecutionStatus()
        return StepResult(result.cycles, status)
    }
    override fun runCycles(cycleBudget: Long): RunResult {
        val result = machine.runCycles(cycleBudget)
        status = result.stopReason.toExecutionStatus()
        return RunResult(result.executedCycles, result.executedInstructions, status)
    }
    override fun pressKey(key: PocketKey): InputResult =
        if (machine.keyboardState.press(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)
    override fun releaseKey(key: PocketKey): InputResult =
        if (machine.keyboardState.release(key)) InputResult.Accepted else InputResult.UnsupportedKey(key)
    override fun setOperatingMode(mode: OperatingMode) = machine.keyboardState.setOperatingMode(mode)
    override fun cpuSnapshot(): CpuSnapshot {
        val state = machine.cpuState
        return CpuSnapshot(
            state.programCounter, state.currentProgramCounter, state.opcode, state.dataPointer,
            state.p, state.q, state.r, state.d, state.alu, state.carry, state.zero, state.xInput,
            state.powerOn, state.ia, state.ib, state.fo, state.control, state.testPort,
            ByteArray(state.internalRam.size) { state.internalRam[it].toByte() },
        )
    }
    override fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot {
        require(startAddress in 0..0xffff)
        require(length >= 0 && length <= 0x10000 - startAddress)
        return MemorySnapshot(startAddress, ByteArray(length) { machine.readMemory(startAddress + it).toByte() })
    }
    override fun displaySnapshot(): DisplaySnapshot {
        val display = machine.displayState
        val columns = display.copyDotColumns()
        val dots = ByteArray(Pc1251Display.DOT_COLUMN_COUNT * Pc1251Display.DOT_ROWS)
        for (column in columns.indices) {
            val bits = columns[column].toInt() and 0xff
            for (row in 0 until Pc1251Display.DOT_ROWS) {
                if (bits and (1 shl row) != 0) dots[row * Pc1251Display.DOT_COLUMN_COUNT + column] = 1
            }
        }
        val symbols = buildList {
            val s0 = display.symbolState0()
            val s1 = display.symbolState1()
            if (s1 and 0x01 != 0) add(DisplaySymbol.BUSY)
            if (s0 and 0x02 != 0) add(DisplaySymbol.P)
            if (s0 and 0x01 != 0) add(DisplaySymbol.DEF)
            if (s0 and 0x08 != 0) add(DisplaySymbol.DE)
            if (s0 and 0x04 != 0) add(DisplaySymbol.G)
            if (s1 and 0x04 != 0) add(DisplaySymbol.RAD)
            if (s1 and 0x02 != 0) add(DisplaySymbol.SHIFT)
            add(
                when (machine.keyboardState.operatingMode) {
                    OperatingMode.RUN -> DisplaySymbol.RUN
                    OperatingMode.PROGRAM -> DisplaySymbol.PRO
                    OperatingMode.RESERVE -> DisplaySymbol.RESERVE
                },
            )
        }
        return DisplaySnapshot(
            characterColumns = 24,
            characterWidth = 5,
            dotRows = 7,
            characterRows = 1,
            symbols = symbols,
            enabled = display.enabled,
            revision = display.revision + machine.keyboardState.modeRevision,
            dots = dots,
        )
    }
    override fun audioSnapshot(): AudioSnapshot = AudioSnapshot(
        machine.buzzerState.frequencyHz,
        machine.buzzerState.revision,
    )
    override fun drainAudioSamples(): AudioPcmSnapshot = AudioPcmSnapshot(
        Pc1245Buzzer.SAMPLE_RATE,
        machine.buzzerState.drainPcm(),
    )
    override fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult =
        machine.loadBasicProgram(program).toLoadResult()
    override fun basicProgramSnapshot(): BasicProgramSnapshotResult =
        machine.basicProgram().toSnapshotResult()
    override fun loadMemoryImage(image: com.digihori.pgp.core.source.machine.AddressedMemoryImage): MemoryImageLoadResult =
        when (val result = machine.loadMemoryImage(image)) {
            is Pc1251MemoryImageLoadResult.Success ->
                MemoryImageLoadResult.Success(result.segmentCount, result.byteCount)
            is Pc1251MemoryImageLoadResult.ReadOnlyAddress ->
                MemoryImageLoadResult.Failure(MemoryImageLoadError.ReadOnlyAddress(result.address, result.sourceLine))
        }
    override fun setMemoryAccessTracing(enabled: Boolean) = machine.setMemoryAccessTracing(enabled)
    override fun drainMemoryAccesses(): List<MemoryAccess> = machine.drainMemoryAccesses()
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
            characterRows = 1,
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

    override fun drainAudioSamples(): AudioPcmSnapshot = AudioPcmSnapshot(
        sampleRate = Pc1245Buzzer.SAMPLE_RATE,
        samples = machine.buzzerState.drainPcm(),
    )

    override fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult =
        when (val result = machine.loadBasicProgram(program)) {
            is Pc1245BasicMemoryResult.Success -> BasicProgramLoadResult.Success(
                result.start,
                result.end,
                result.bytes.size,
            )
            is Pc1245BasicMemoryResult.InvalidPointers -> BasicProgramLoadResult.Failure(
                BasicProgramMemoryError.InvalidPointer(result.start, result.end),
            )
            is Pc1245BasicMemoryResult.TooLarge -> BasicProgramLoadResult.Failure(
                BasicProgramMemoryError.ProgramTooLarge(result.size, result.capacity),
            )
            is Pc1245BasicMemoryResult.InvalidProgram -> BasicProgramLoadResult.Failure(
                BasicProgramMemoryError.InvalidProgram(result.offset, result.reason),
            )
        }

    override fun basicProgramSnapshot(): BasicProgramSnapshotResult =
        when (val result = machine.basicProgram()) {
            is Pc1245BasicMemoryResult.Success -> BasicProgramSnapshotResult.Success(
                result.start,
                result.end,
                result.bytes,
            )
            is Pc1245BasicMemoryResult.InvalidPointers -> BasicProgramSnapshotResult.Failure(
                BasicProgramMemoryError.InvalidPointer(result.start, result.end),
            )
            is Pc1245BasicMemoryResult.TooLarge -> error("Program extraction cannot exceed capacity")
            is Pc1245BasicMemoryResult.InvalidProgram -> BasicProgramSnapshotResult.Failure(
                BasicProgramMemoryError.InvalidProgram(result.offset, result.reason),
            )
        }

    override fun loadMemoryImage(image: com.digihori.pgp.core.source.machine.AddressedMemoryImage): MemoryImageLoadResult =
        when (val result = machine.loadMemoryImage(image)) {
            is com.digihori.pgp.core.emulator.machine.pc1245.Pc1245MemoryImageLoadResult.Success ->
                MemoryImageLoadResult.Success(result.segmentCount, result.byteCount)
            is com.digihori.pgp.core.emulator.machine.pc1245.Pc1245MemoryImageLoadResult.ReadOnlyAddress ->
                MemoryImageLoadResult.Failure(MemoryImageLoadError.ReadOnlyAddress(result.address, result.sourceLine))
        }
    override fun setMemoryAccessTracing(enabled: Boolean) = machine.setMemoryAccessTracing(enabled)
    override fun drainMemoryAccesses(): List<MemoryAccess> = machine.drainMemoryAccesses()
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

private fun Pc1245BasicMemoryResult.toLoadResult(): BasicProgramLoadResult = when (this) {
    is Pc1245BasicMemoryResult.Success -> BasicProgramLoadResult.Success(start, end, bytes.size)
    is Pc1245BasicMemoryResult.InvalidPointers ->
        BasicProgramLoadResult.Failure(BasicProgramMemoryError.InvalidPointer(start, end))
    is Pc1245BasicMemoryResult.TooLarge ->
        BasicProgramLoadResult.Failure(BasicProgramMemoryError.ProgramTooLarge(size, capacity))
    is Pc1245BasicMemoryResult.InvalidProgram ->
        BasicProgramLoadResult.Failure(BasicProgramMemoryError.InvalidProgram(offset, reason))
}

private fun Pc1245BasicMemoryResult.toSnapshotResult(): BasicProgramSnapshotResult = when (this) {
    is Pc1245BasicMemoryResult.Success -> BasicProgramSnapshotResult.Success(start, end, bytes)
    is Pc1245BasicMemoryResult.InvalidPointers ->
        BasicProgramSnapshotResult.Failure(BasicProgramMemoryError.InvalidPointer(start, end))
    is Pc1245BasicMemoryResult.TooLarge -> error("Program extraction cannot exceed capacity")
    is Pc1245BasicMemoryResult.InvalidProgram ->
        BasicProgramSnapshotResult.Failure(BasicProgramMemoryError.InvalidProgram(offset, reason))
}
