package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.AudioSnapshot
import com.digihori.pgp.core.api.AudioPcmSnapshot
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.RunResult
import com.digihori.pgp.core.api.StepResult
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.MemoryAccessKind
import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.core.runtime.CycleBudget
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.KeyInputQueue
import com.digihori.pgp.core.runtime.KeyTransition
import com.digihori.pgp.core.runtime.SpeedRatio
import com.digihori.pgp.desktop.input.DesktopKeyInputSink
import com.digihori.pgp.desktop.input.DesktopCommandHistory
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.debug.DesktopInstructionTraceEntry
import com.digihori.pgp.desktop.debug.MemoryValueChange
import com.digihori.pgp.desktop.debug.DesktopMemoryAccessHistoryEntry
import com.digihori.pgp.desktop.debug.DesktopBankHistoryEntry
import com.digihori.pgp.core.debug.Sc61860InstructionDecoder

internal fun interface MonotonicClock {
    fun nowNanoseconds(): Long
}

internal object SystemMonotonicClock : MonotonicClock {
    override fun nowNanoseconds(): Long = System.nanoTime()
}

internal class DesktopEmulatorRunner(
    private val session: EmulatorSession,
    private val clock: MonotonicClock = SystemMonotonicClock,
    planner: CycleBudgetPlanner? = null,
    private val keyInputQueue: KeyInputQueue = defaultKeyInputQueue(session.machineId),
    private val commandHistory: DesktopCommandHistory = DesktopCommandHistory(),
    private val programCounterProvider: () -> Int = { session.cpuSnapshot().programCounter },
    private val traceEntryProvider: ((Long) -> DesktopInstructionTraceEntry)? = null,
    private val memoryBytesProvider: (Int, Int) -> ByteArray = { start, length ->
        session.memorySnapshot(start, length).copyBytes()
    },
) : DesktopKeyInputSink {
    override val machineId get() = session.machineId

    private val planner: CycleBudgetPlanner = planner ?: CycleBudgetPlanner(
        cyclesPerSecond = MachineCatalog.require(session.machineId).cyclesPerSecond,
    )
    var state: RunnerState = RunnerState.PAUSED
        private set

    var stopReason: DebuggerStopReason? = DebuggerStopReason.Reset
        private set

    var speed: SpeedRatio = SpeedRatio.NORMAL
        private set

    private var previousTimeNanoseconds: Long? = null
    private var operatingMode: OperatingMode = OperatingMode.RUN
    private val executionBreakpoints: MutableSet<Int> = mutableSetOf()
    private var breakpointToSkipOnce: Int? = null
    private var temporaryRunToAddress: Int? = null
    private var instructionTraceEnabled: Boolean = false
    private var instructionTraceCapacity: Int = DEFAULT_INSTRUCTION_TRACE_CAPACITY
    private val instructionTrace: ArrayDeque<DesktopInstructionTraceEntry> = ArrayDeque()
    private var nextTraceSequence: Long = 0
    private var memoryWatchRange: IntRange? = null
    private var memoryWatchBaseline: ByteArray? = null
    private var memoryAccessWatch: MemoryAccessWatch? = null
    private var debugObservationEnabled: Boolean = false
    private val memoryAccessHistory: ArrayDeque<DesktopMemoryAccessHistoryEntry> = ArrayDeque()
    private val bankHistory: ArrayDeque<DesktopBankHistoryEntry> = ArrayDeque()
    private var nextMemoryAccessSequence: Long = 0
    private var nextBankSequence: Long = 0
    private var lastStepMemoryAccesses: List<MemoryAccess> = emptyList()
    var sessionRevision: Long = 0
        private set

    fun run() {
        if (state != RunnerState.PAUSED) return
        breakpointToSkipOnce = when (val reason = stopReason) {
            is DebuggerStopReason.Breakpoint -> reason.address
            is DebuggerStopReason.RunToAddress -> reason.address
            else -> null
        }
        planner.reset()
        previousTimeNanoseconds = clock.nowNanoseconds()
        state = RunnerState.RUNNING
        stopReason = null
        sessionRevision++
    }

    fun pause() {
        val previousState = state
        val previousReason = stopReason
        applyTransition(keyInputQueue.cancel())
        planner.reset()
        previousTimeNanoseconds = null
        if (state != RunnerState.FAULTED) {
            if (state == RunnerState.RUNNING) stopReason = DebuggerStopReason.UserPause
            state = RunnerState.PAUSED
        }
        temporaryRunToAddress = null
        if (state != previousState || stopReason != previousReason) sessionRevision++
    }

    fun reset() {
        keyInputQueue.cancel()
        session.reset()
        planner.reset()
        previousTimeNanoseconds = null
        speed = SpeedRatio.NORMAL
        operatingMode = OperatingMode.RUN
        commandHistory.clearPendingInput()
        state = RunnerState.PAUSED
        stopReason = DebuggerStopReason.Reset
        breakpointToSkipOnce = null
        temporaryRunToAddress = null
        clearInstructionTrace()
        refreshMemoryWatchBaseline()
        session.drainMemoryAccesses()
        session.drainBankSwitchEvents()
        memoryAccessHistory.clear()
        bankHistory.clear()
        nextMemoryAccessSequence = 0
        nextBankSequence = 0
        sessionRevision++
    }

    fun toggleBreakpoint(address: Int): Boolean {
        val normalized = address and 0xffff
        return if (executionBreakpoints.remove(normalized)) false else {
            executionBreakpoints += normalized
            true
        }
    }

    fun breakpoints(): Set<Int> = executionBreakpoints.toSortedSet()

    fun setInstructionTraceEnabled(enabled: Boolean) {
        if (instructionTraceEnabled == enabled) return
        instructionTraceEnabled = enabled
        sessionRevision++
    }

    fun isInstructionTraceEnabled(): Boolean = instructionTraceEnabled

    fun instructionTrace(): List<DesktopInstructionTraceEntry> = instructionTrace.toList()

    fun instructionTraceCapacity(): Int = instructionTraceCapacity

    fun setDebugObservationEnabled(enabled: Boolean) {
        if (debugObservationEnabled == enabled) return
        debugObservationEnabled = enabled
        session.setMemoryAccessTracing(enabled || memoryAccessWatch != null)
        session.drainMemoryAccesses()
        lastStepMemoryAccesses = emptyList()
        sessionRevision++
    }

    fun isDebugObservationEnabled(): Boolean = debugObservationEnabled

    fun memoryAccessHistory(): List<DesktopMemoryAccessHistoryEntry> = memoryAccessHistory.toList()

    fun bankHistory(): List<DesktopBankHistoryEntry> = bankHistory.toList()

    fun setInstructionTraceCapacity(capacity: Int) {
        require(capacity in MIN_INSTRUCTION_TRACE_CAPACITY..MAX_INSTRUCTION_TRACE_CAPACITY)
        instructionTraceCapacity = capacity
        while (instructionTrace.size > capacity) instructionTrace.removeFirst()
        sessionRevision++
    }

    fun clearInstructionTrace() {
        if (instructionTrace.isEmpty() && nextTraceSequence == 0L) return
        instructionTrace.clear()
        nextTraceSequence = 0
        sessionRevision++
    }

    fun setMemoryWatch(startAddress: Int, endAddressInclusive: Int) {
        require(startAddress in 0..0xffff)
        require(endAddressInclusive in startAddress..0xffff)
        require(endAddressInclusive - startAddress + 1 <= MAX_MEMORY_WATCH_BYTES)
        memoryWatchRange = startAddress..endAddressInclusive
        refreshMemoryWatchBaseline()
    }

    fun clearMemoryWatch() {
        memoryWatchRange = null
        memoryWatchBaseline = null
    }

    fun memoryWatch(): IntRange? = memoryWatchRange

    fun setMemoryAccessWatch(startAddress: Int, endAddressInclusive: Int, reads: Boolean, writes: Boolean) {
        require(startAddress in 0..0xffff && endAddressInclusive in startAddress..0xffff)
        require(reads || writes)
        memoryAccessWatch = MemoryAccessWatch(startAddress..endAddressInclusive, reads, writes)
        session.drainMemoryAccesses()
        session.setMemoryAccessTracing(true)
    }

    fun clearMemoryAccessWatch() {
        memoryAccessWatch = null
        session.setMemoryAccessTracing(debugObservationEnabled)
        session.drainMemoryAccesses()
        lastStepMemoryAccesses = emptyList()
    }

    fun memoryAccessWatchRange(): IntRange? = memoryAccessWatch?.range

    fun runToAddress(address: Int) {
        if (state == RunnerState.FAULTED) return
        temporaryRunToAddress = address and 0xffff
        if (state == RunnerState.PAUSED) run()
    }

    fun setSpeed(speed: SpeedRatio) {
        if (this.speed == speed) return
        this.speed = speed
        planner.reset()
        if (state == RunnerState.RUNNING) {
            previousTimeNanoseconds = clock.nowNanoseconds()
        }
    }

    fun step(): StepResult {
        pause()
        val instructionAddress = if (memoryWatchRange != null || memoryAccessWatch != null) {
            programCounterProvider() and 0xffff
        } else 0
        val result = stepSession()
        val executionStatus = result.status
        if (executionStatus is ExecutionStatus.Faulted) {
            state = RunnerState.FAULTED
            stopReason = DebuggerStopReason.Fault(executionStatus.fault)
        } else {
            stopReason = detectMemoryAccesses(instructionAddress)
                ?: detectMemoryChanges(instructionAddress)
                ?: DebuggerStopReason.StepComplete
        }
        return result
    }

    fun displaySnapshot(): DisplaySnapshot = session.displaySnapshot()

    fun cpuSnapshot(): CpuSnapshot = session.cpuSnapshot()

    fun audioSnapshot(): AudioSnapshot = session.audioSnapshot()

    fun drainAudioSamples(): AudioPcmSnapshot = session.drainAudioSamples()

    fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult {
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadBasicProgram(program)
        sessionRevision++
        refreshMemoryWatchBaseline()
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun basicProgramSnapshot(): BasicProgramSnapshotResult = session.basicProgramSnapshot()

    fun loadMemoryImage(image: AddressedMemoryImage): MemoryImageLoadResult {
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadMemoryImage(image)
        sessionRevision++
        refreshMemoryWatchBaseline()
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot =
        session.memorySnapshot(startAddress, length)

    fun memoryByte(address: Int): Int =
        session.memorySnapshot(address and 0xffff, 1).copyBytes().single().toInt() and 0xff

    fun selectedRomBank(): Int? = session.selectedRomBank()

    fun resolveRomLocation(address: Int): PhysicalRomLocation? =
        session.resolveRomLocation(address and 0xffff)

    override fun pressKey(key: PocketKey): InputResult = session.pressKey(key).also { result ->
        if (result is InputResult.Accepted) {
            sessionRevision++
            if (operatingMode == OperatingMode.RUN && key != PocketKey.DEF) {
                commandHistory.recordUserKey(key)
            }
        }
    }

    override fun releaseKey(key: PocketKey): InputResult {
        val result = session.releaseKey(key)
        if (result is InputResult.Accepted) sessionRevision++
        return result
    }

    override fun enqueueKeySequence(keys: Iterable<PocketKey>) {
        keyInputQueue.enqueue(keys)
    }

    override fun enqueueUserKeySequence(keys: Iterable<PocketKey>) {
        val captured = keys.toList()
        if (operatingMode == OperatingMode.RUN) commandHistory.recordUserSequence(captured)
        keyInputQueue.enqueue(captured)
    }

    override fun recallPreviousCommand(): Boolean =
        operatingMode == OperatingMode.RUN && recall(commandHistory.previous())

    override fun recallNextCommand(): Boolean =
        operatingMode == OperatingMode.RUN && recall(commandHistory.next())

    /** Runs a complete generated key sequence without host-time pacing. */
    fun runKeySequenceImmediately(keys: Iterable<PocketKey>): RunResult {
        check(state != RunnerState.FAULTED) { "Cannot run input while the emulator is faulted" }
        val resumeAfterInput = state == RunnerState.RUNNING
        applyTransition(keyInputQueue.cancel())
        keyInputQueue.enqueue(keys)
        planner.reset()
        previousTimeNanoseconds = null

        var executedCycles = 0L
        var executedInstructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        applyTransition(keyInputQueue.start())
        while (!keyInputQueue.isIdle && status !is ExecutionStatus.Faulted) {
            val requestedCycles = keyInputQueue.limitCycles(Long.MAX_VALUE)
            val partialResult = runSessionCycles(requestedCycles)
            check(partialResult.executedCycles > 0) { "Session made no progress" }
            executedCycles += partialResult.executedCycles
            executedInstructions += partialResult.executedInstructions
            status = partialResult.status
            val transition = keyInputQueue.advance(partialResult.executedCycles)
            applyTransition(transition)

            // The ROM tokenizes and stores a BASIC line after ENTER. Unlike ordinary
            // key gaps, that work can take longer as the line and program grow.
            if (
                transition is KeyTransition.Release &&
                transition.key == PocketKey.ENTER &&
                status !is ExecutionStatus.Faulted
            ) {
                val settleResult = runSessionCycles(BASIC_LINE_SETTLE_CYCLES)
                executedCycles += settleResult.executedCycles
                executedInstructions += settleResult.executedInstructions
                status = settleResult.status
            }
        }

        if (status is ExecutionStatus.Faulted) {
            applyTransition(keyInputQueue.cancel())
            state = RunnerState.FAULTED
            stopReason = DebuggerStopReason.Fault(status.fault)
            temporaryRunToAddress = null
        } else if (resumeAfterInput) {
            previousTimeNanoseconds = clock.nowNanoseconds()
        } else {
            state = RunnerState.PAUSED
        }
        refreshMemoryWatchBaseline()
        return RunResult(executedCycles, executedInstructions, status)
    }

    fun setOperatingMode(mode: OperatingMode) {
        session.setOperatingMode(mode)
        if (operatingMode != mode) sessionRevision++
        if (operatingMode != mode) commandHistory.clearPendingInput()
        operatingMode = mode
    }

    internal fun commandHistorySize(): Int = commandHistory.size

    /** Minimum host-time hold used to keep a quick software-key tap visible to ROM key scanning. */
    fun minimumSoftwareKeyHoldMilliseconds(): Long {
        val definition = MachineCatalog.require(machineId)
        return (definition.automaticKeyHoldCycles * 1_000L + definition.cyclesPerSecond - 1L) /
            definition.cyclesPerSecond
    }

    fun tick(): RunnerTick {
        if (state != RunnerState.RUNNING) return RunnerTick.idle()

        val now = clock.nowNanoseconds()
        val previous = checkNotNull(previousTimeNanoseconds)
        require(now >= previous) { "Monotonic clock moved backwards" }
        previousTimeNanoseconds = now

        val budget = planner.plan(now - previous, speed)
        if (budget.cycles == 0L) return RunnerTick(budget, null)

        applyTransition(keyInputQueue.start())
        var cyclesRemaining = budget.cycles
        var executedCycles = 0L
        var executedInstructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        while (cyclesRemaining > 0 && status !is ExecutionStatus.Faulted) {
            val requestedCycles = keyInputQueue.limitCycles(cyclesRemaining)
            val partial = runUntilBreakpoint(requestedCycles)
            val partialResult = partial.result
            executedCycles += partialResult.executedCycles
            executedInstructions += partialResult.executedInstructions
            cyclesRemaining = (cyclesRemaining - partialResult.executedCycles).coerceAtLeast(0)
            status = partialResult.status
            if (partialResult.executedCycles > 0) {
                applyTransition(keyInputQueue.advance(partialResult.executedCycles))
            }
            if (partial.stopReason != null) {
                applyTransition(keyInputQueue.cancel())
                state = RunnerState.PAUSED
                previousTimeNanoseconds = null
                planner.reset()
                stopReason = partial.stopReason
                break
            }
            check(partialResult.executedCycles > 0) { "Session made no progress" }
        }
        val result = RunResult(executedCycles, executedInstructions, status)
        if (status is ExecutionStatus.Faulted) {
            applyTransition(keyInputQueue.cancel())
            state = RunnerState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
            stopReason = DebuggerStopReason.Fault(status.fault)
            temporaryRunToAddress = null
        }
        return RunnerTick(budget, result)
    }

    private fun runUntilBreakpoint(cycleBudget: Long): BreakpointRunResult {
        if (
            executionBreakpoints.isEmpty() && temporaryRunToAddress == null &&
            !instructionTraceEnabled && memoryWatchRange == null && memoryAccessWatch == null &&
            !debugObservationEnabled
        ) {
            val result = session.runCycles(cycleBudget)
            collectBankEvents()
            if (result.executedInstructions > 0) sessionRevision += result.executedInstructions
            return BreakpointRunResult(result)
        }
        var cycles = 0L
        var instructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        while (cycles < cycleBudget && status !is ExecutionStatus.Faulted) {
            val needsProgramCounter = temporaryRunToAddress != null ||
                executionBreakpoints.isNotEmpty() || memoryWatchRange != null || memoryAccessWatch != null
            val pc = if (needsProgramCounter) programCounterProvider() and 0xffff else 0
            if (temporaryRunToAddress != null || executionBreakpoints.isNotEmpty()) {
                if (pc == temporaryRunToAddress) {
                    temporaryRunToAddress = null
                    return BreakpointRunResult(
                        RunResult(cycles, instructions, status),
                        stopReason = DebuggerStopReason.RunToAddress(pc),
                    )
                }
                if (pc in executionBreakpoints && pc != breakpointToSkipOnce) {
                    return BreakpointRunResult(
                        RunResult(cycles, instructions, status),
                        stopReason = DebuggerStopReason.Breakpoint(pc),
                    )
                }
            }
            breakpointToSkipOnce = null
            val step = stepSession()
            cycles += step.cycles
            instructions++
            status = step.status
            val accessStop = detectMemoryAccesses(pc)
            if (accessStop != null) {
                return BreakpointRunResult(
                    RunResult(cycles, instructions, status),
                    stopReason = accessStop,
                )
            }
            val memoryStop = detectMemoryChanges(pc)
            if (memoryStop != null) {
                return BreakpointRunResult(
                    RunResult(cycles, instructions, status),
                    stopReason = memoryStop,
                )
            }
        }
        return BreakpointRunResult(RunResult(cycles, instructions, status))
    }

    private fun runSessionCycles(cycleBudget: Long): RunResult {
        if (!instructionTraceEnabled && !debugObservationEnabled) {
            val result = session.runCycles(cycleBudget)
            collectBankEvents()
            if (result.executedInstructions > 0) sessionRevision += result.executedInstructions
            return result
        }
        var cycles = 0L
        var instructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        while (cycles < cycleBudget && status !is ExecutionStatus.Faulted) {
            val step = stepSession()
            cycles += step.cycles
            instructions++
            status = step.status
        }
        return RunResult(cycles, instructions, status)
    }

    private fun stepSession(): StepResult {
        val instructionAddress = if (debugObservationEnabled || memoryAccessWatch != null) {
            programCounterProvider() and 0xffff
        } else {
            0
        }
        if (instructionTraceEnabled) {
            val entry = traceEntryProvider?.invoke(nextTraceSequence) ?: captureTraceEntry(nextTraceSequence)
            if (instructionTrace.size == instructionTraceCapacity) instructionTrace.removeFirst()
            instructionTrace.addLast(entry)
            nextTraceSequence++
        }
        val result = session.step()
        sessionRevision++
        lastStepMemoryAccesses = if (debugObservationEnabled || memoryAccessWatch != null) {
            session.drainMemoryAccesses()
        } else {
            emptyList()
        }
        if (debugObservationEnabled) {
            lastStepMemoryAccesses.forEach { access ->
                addBounded(
                    memoryAccessHistory,
                    DesktopMemoryAccessHistoryEntry(nextMemoryAccessSequence++, instructionAddress, access),
                    MAX_DEBUG_HISTORY_ENTRIES,
                )
            }
        }
        collectBankEvents()
        return result
    }

    private fun captureTraceEntry(sequence: Long): DesktopInstructionTraceEntry {
        val cpu = session.cpuSnapshot()
        return DesktopInstructionTraceEntry(
            sequence = sequence,
            dataPointer = cpu.dataPointer,
            p = cpu.p,
            q = cpu.q,
            r = cpu.r,
            d = cpu.d,
            carry = cpu.carry,
            zero = cpu.zero,
            instruction = Sc61860InstructionDecoder.decode(cpu.programCounter, ::memoryByte),
            romLocation = session.resolveRomLocation(cpu.programCounter),
        )
    }

    private fun refreshMemoryWatchBaseline() {
        val range = memoryWatchRange ?: return
        memoryWatchBaseline = memoryBytesProvider(range.first, range.last - range.first + 1)
    }

    private fun detectMemoryChanges(instructionAddress: Int): DebuggerStopReason.MemoryChanged? {
        val range = memoryWatchRange ?: return null
        val before = memoryWatchBaseline ?: return null
        val after = memoryBytesProvider(range.first, before.size)
        val changes = buildList {
            before.indices.forEach { index ->
                val oldValue = before[index].toInt() and 0xff
                val newValue = after[index].toInt() and 0xff
                if (oldValue != newValue) {
                    add(MemoryValueChange(range.first + index, oldValue, newValue))
                }
            }
        }
        memoryWatchBaseline = after
        return changes.takeIf { it.isNotEmpty() }?.let {
            DebuggerStopReason.MemoryChanged(instructionAddress, it)
        }
    }

    private fun detectMemoryAccesses(instructionAddress: Int): DebuggerStopReason.MemoryAccessed? {
        val watch = memoryAccessWatch ?: return null
        val matching = lastStepMemoryAccesses.filter { access ->
            access.address in watch.range && when (access.kind) {
                MemoryAccessKind.READ -> watch.reads
                MemoryAccessKind.WRITE -> watch.writes
            }
        }
        return matching.takeIf { it.isNotEmpty() }?.let {
            DebuggerStopReason.MemoryAccessed(instructionAddress, it)
        }
    }

    private fun collectBankEvents() {
        session.drainBankSwitchEvents().forEach { event ->
            addBounded(
                bankHistory,
                DesktopBankHistoryEntry(nextBankSequence++, event),
                MAX_DEBUG_HISTORY_ENTRIES,
            )
        }
    }

    private fun <T> addBounded(history: ArrayDeque<T>, entry: T, capacity: Int) {
        if (history.size == capacity) history.removeFirst()
        history.addLast(entry)
    }

    private fun applyTransition(transition: KeyTransition?) {
        when (transition) {
            is KeyTransition.Press -> session.pressKey(transition.key)
            is KeyTransition.Release -> session.releaseKey(transition.key)
            null -> Unit
        }
    }

    private fun recall(recall: com.digihori.pgp.desktop.input.CommandHistoryRecall?): Boolean {
        recall ?: return false
        if (recall.replaceCurrentInput) keyInputQueue.enqueue(listOf(PocketKey.CLEAR))
        if (recall.keys.isNotEmpty()) keyInputQueue.enqueue(recall.keys)
        return true
    }

    internal companion object {
        // Pokecom GO retained a released key for three 20 ms polling intervals.
        private fun defaultKeyInputQueue(machineId: com.digihori.pgp.core.rom.MachineId): KeyInputQueue {
            val definition = MachineCatalog.require(machineId)
            return KeyInputQueue(
                holdCycles = definition.automaticKeyHoldCycles,
                gapCycles = definition.automaticKeyGapCycles,
            )
        }

        const val BASIC_LINE_SETTLE_CYCLES: Long = 57_600 // 200 ms after ENTER.
        const val DEFAULT_INSTRUCTION_TRACE_CAPACITY: Int = 256
        const val MIN_INSTRUCTION_TRACE_CAPACITY: Int = 16
        const val MAX_INSTRUCTION_TRACE_CAPACITY: Int = 65_536
        const val MAX_MEMORY_WATCH_BYTES: Int = 4_096
        const val MAX_DEBUG_HISTORY_ENTRIES: Int = 4_096
    }
}

private data class MemoryAccessWatch(val range: IntRange, val reads: Boolean, val writes: Boolean)

private data class BreakpointRunResult(
    val result: RunResult,
    val stopReason: DebuggerStopReason? = null,
)

internal enum class RunnerState {
    PAUSED,
    RUNNING,
    FAULTED,
}

internal data class RunnerTick(
    val budget: CycleBudget,
    val runResult: RunResult?,
) {
    companion object {
        fun idle(): RunnerTick = RunnerTick(
            budget = CycleBudget(0, 0, 0),
            runResult = null,
        )
    }
}
