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
import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.core.runtime.CycleBudget
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.KeyInputQueue
import com.digihori.pgp.core.runtime.KeyTransition
import com.digihori.pgp.core.runtime.SpeedRatio
import com.digihori.pgp.desktop.input.DesktopKeyInputSink
import com.digihori.pgp.desktop.input.DesktopCommandHistory
import com.digihori.pgp.input.HostKeyState
import com.digihori.pgp.input.GamepadButton
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.debug.DebuggerSettingsApplyResult
import com.digihori.pgp.desktop.debug.DebuggerSettingsError
import com.digihori.pgp.desktop.debug.DebuggerCallTracker
import com.digihori.pgp.desktop.debug.DebuggerStepStatus
import com.digihori.pgp.desktop.debug.DebuggerRegistrationStore
import com.digihori.pgp.desktop.debug.DebuggerRegistrationResult
import com.digihori.pgp.desktop.debug.DebuggerRegistrationError
import com.digihori.pgp.desktop.debug.DebuggerBreakpoint
import com.digihori.pgp.desktop.debug.DebuggerWatch
import com.digihori.pgp.desktop.debug.DebuggerWatchKind
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.debug.DesktopInstructionTraceEntry
import com.digihori.pgp.desktop.debug.DesktopMemoryAccessHistoryEntry
import com.digihori.pgp.desktop.debug.DesktopBankHistoryEntry
import com.digihori.pgp.core.debug.Sc61860InstructionDecoder
import com.digihori.pgp.desktop.debug.DebuggerMemoryEditError
import com.digihori.pgp.desktop.debug.DebuggerMemoryEditPreview
import com.digihori.pgp.desktop.debug.DebuggerMemoryEditResult

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
    private val debugStepInstructionLimit: Long = 100_000L,
    private val debugStepCycleLimit: Long = 2_000_000L,
    val debugRomFingerprint: String? = null,
) : DesktopKeyInputSink {
    init { require(debugStepInstructionLimit > 0 && debugStepCycleLimit > 0) }
    override val machineId get() = session.machineId
    val memoryLayout get() = session.memoryLayout
    private val hostKeys = HostKeyState(session::pressKey, session::releaseKey)
    var inputEpoch: Long = 0
        private set

    private val planner: CycleBudgetPlanner = planner ?: CycleBudgetPlanner(
        cyclesPerSecond = MachineCatalog.require(session.machineId).cyclesPerSecond,
    )
    var state: RunnerState = RunnerState.PAUSED
        private set

    var stopReason: DebuggerStopReason? = DebuggerStopReason.Reset
        private set

    var speed: SpeedRatio = SpeedRatio.NORMAL
        private set

    var unlimited: Boolean = false
        private set

    fun setExecutionSpeed(value: DesktopExecutionSpeed) {
        val changed = unlimited != (value == DesktopExecutionSpeed.UNLIMITED)
        unlimited = value == DesktopExecutionSpeed.UNLIMITED
        setSpeed(value.ratio)
        if (changed) {
            planner.reset()
            if (state == RunnerState.RUNNING) previousTimeNanoseconds = clock.nowNanoseconds()
        }
    }

    private var previousTimeNanoseconds: Long? = null
    private var requestedOperatingMode: OperatingMode = OperatingMode.RUN
    private val operatingMode: OperatingMode get() = session.currentOperatingMode() ?: requestedOperatingMode
    private var registrations = DebuggerRegistrationStore(readBytes = memoryBytesProvider)
    private val executionBreakpoints: Set<Int> get() = registrations.enabledBreakpointAddresses
    var registrationRevision: Long = 0
        private set
    private var breakpointToSkipOnce: Int? = null
    private var temporaryRunToAddress: Int? = null
    private val callTracker = DebuggerCallTracker()
    private data class StepPlan(val out: Boolean, var frame: Long?, val single: Boolean = false,
        var instructions: Long = 0, var cycles: Long = 0)
    private var stepPlan: StepPlan? = null
    var debugStepStatus: DebuggerStepStatus = DebuggerStepStatus.IDLE
        private set
    val canStepOut: Boolean get() = state == RunnerState.PAUSED && callTracker.top != null

    fun stepOver(): Boolean = startDebugStep(out = false)
    fun stepOut(): Boolean = startDebugStep(out = true)

    private fun startDebugStep(out: Boolean): Boolean {
        if (state != RunnerState.PAUSED) return false
        if (out && callTracker.top == null) { debugStepStatus = DebuggerStepStatus.NO_FRAME; return false }
        val pc = session.cpuSnapshot().programCounter
        val single = !out && !DebuggerCallTracker.isCall(memoryByte(pc))
        val frame = if (out) callTracker.top?.id else null
        run()
        stepPlan = StepPlan(out, frame, single)
        debugStepStatus = if (out) DebuggerStepStatus.OUT_RUNNING else DebuggerStepStatus.OVER_RUNNING
        // Like Step Into, the starting instruction is executed even when it has a breakpoint.
        breakpointToSkipOnce = pc
        return true
    }

    private fun interruptDebugStep() {
        if (stepPlan != null) { stepPlan = null; debugStepStatus = DebuggerStepStatus.INTERRUPTED }
    }
    private var instructionTraceEnabled: Boolean = false
    private var instructionTraceCapacity: Int = DEFAULT_INSTRUCTION_TRACE_CAPACITY
    private val instructionTrace: ArrayDeque<DesktopInstructionTraceEntry> = ArrayDeque()
    private var nextTraceSequence: Long = 0
    private val hasMemoryWatch: Boolean get() = registrations.hasChangeWatch
    private val hasMemoryAccessWatch: Boolean get() = registrations.hasAccessWatch
    private var debugObservationEnabled: Boolean = false
    private val memoryAccessHistory: ArrayDeque<DesktopMemoryAccessHistoryEntry> = ArrayDeque()
    private val bankHistory: ArrayDeque<DesktopBankHistoryEntry> = ArrayDeque()
    private var nextMemoryAccessSequence: Long = 0
    private var nextBankSequence: Long = 0
    private var lastStepMemoryAccesses: List<MemoryAccess> = emptyList()
    var debugTargetGeneration: Long = 0
        private set

    fun signalDebugSettingsChange() { debugSettingsRevision++ }

    var sessionRevision: Long = 0
        private set

    fun run() {
        if (state != RunnerState.PAUSED) return
        debugStepStatus = DebuggerStepStatus.IDLE
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
        interruptDebugStep()
        val previousState = state
        val previousReason = stopReason
        applyTransition(keyInputQueue.cancel())
        hostKeys.releaseAll()
        inputEpoch++
        planner.reset()
        previousTimeNanoseconds = null
        if (state != RunnerState.FAULTED) {
            if (state == RunnerState.RUNNING) stopReason = DebuggerStopReason.UserPause
            state = RunnerState.PAUSED
        }
        temporaryRunToAddress = null
        if (state != previousState || stopReason != previousReason) sessionRevision++
    }

    var sourceMap: com.digihori.pgp.desktop.project.DesktopDebugSourceMap? = null
        private set

    fun attachSourceMap(sources: List<com.digihori.pgp.desktop.project.DesktopAssemblySourceResult>) {
        sourceMap = com.digihori.pgp.desktop.project.DesktopDebugSourceMap.create(sources, ::resolveRomLocation)
    }

    fun reset() {
        debugTargetGeneration++
        stepPlan = null
        debugStepStatus = DebuggerStepStatus.IDLE
        callTracker.clear()
        sourceMap = null
        keyInputQueue.cancel()
        hostKeys.releaseAll()
        inputEpoch++
        session.reset()
        registrations.resetHitCounts()
        planner.reset()
        previousTimeNanoseconds = null
        requestedOperatingMode = OperatingMode.RUN
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

    var debugSettingsRevision: Long = 0
        private set

    /** Prepare all definitions and current-value baselines before replacing live registrations. */
    fun restoreDebugSettings(settings: com.digihori.pgp.desktop.debug.DebuggerSavedSettings): DebuggerSettingsApplyResult {
        if (state == RunnerState.RUNNING) return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.RUNNING)
        if (settings.traceCapacity !in MIN_INSTRUCTION_TRACE_CAPACITY..MAX_INSTRUCTION_TRACE_CAPACITY)
            return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.TRACE)
        if (settings.target.machineId != machineId.value || settings.target.romSha256 != debugRomFingerprint ||
            settings.target.memoryConfiguration != memoryLayout.pc1251MemoryMode.name)
            return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.TARGET)
        val candidate = DebuggerRegistrationStore(registrations.nextRegistrationId, memoryBytesProvider)
        for (entry in settings.breakpoints) {
            val result = candidate.saveBreakpoint(null, entry.address, entry.enabled, entry.condition, entry.hitCondition)
            if (result is DebuggerRegistrationResult.Failure) return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.REGISTRATIONS)
        }
        for (entry in settings.watches) {
            val result = candidate.saveWatch(null, entry.start, entry.end, entry.kind, entry.enabled)
            if (result is DebuggerRegistrationResult.Failure) return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.REGISTRATIONS)
        }
        val oldTracing = debugObservationEnabled || hasMemoryAccessWatch
        val newTracing = debugObservationEnabled || candidate.hasAccessWatch
        if (oldTracing != newTracing) {
            try { session.setMemoryAccessTracing(newTracing) }
            catch (_: Exception) {
                runCatching { session.setMemoryAccessTracing(oldTracing) }
                return DebuggerSettingsApplyResult.Failure(DebuggerSettingsError.REGISTRATIONS)
            }
        }
        registrations = candidate
        instructionTraceEnabled = settings.traceEnabled
        instructionTraceCapacity = settings.traceCapacity
        while (instructionTrace.size > instructionTraceCapacity) instructionTrace.removeFirst()
        temporaryRunToAddress = null
        breakpointToSkipOnce = null
        debugSettingsRevision++
        registrationRevision++
        sessionRevision++
        return DebuggerSettingsApplyResult.Success
    }

    fun breakpointRegistrations(): List<DebuggerBreakpoint> = registrations.breakpoints()
    fun watchRegistrations(): List<DebuggerWatch> = registrations.watches()

    fun saveBreakpoint(id: Long?, address: Int, enabled: Boolean = true, condition: String? = null, hitCondition: String? = null): DebuggerRegistrationResult =
        registrationChanged(registrations.saveBreakpoint(id, address, enabled, condition, hitCondition))

    fun removeBreakpoint(id: Long): DebuggerRegistrationResult = registrationChanged(registrations.removeBreakpoint(id))

    fun setBreakpointEnabled(id: Long, enabled: Boolean): DebuggerRegistrationResult {
        val entry = breakpointRegistrations().firstOrNull { it.id == id }
            ?: return DebuggerRegistrationResult.Failure(DebuggerRegistrationError.NOT_FOUND)
        return saveBreakpoint(id, entry.address, enabled)
    }

    fun saveWatch(id: Long?, start: Int, end: Int, kind: DebuggerWatchKind, enabled: Boolean = true): DebuggerRegistrationResult {
        val tracedBefore = debugObservationEnabled || hasMemoryAccessWatch
        val result = registrationChanged(registrations.saveWatch(id, start, end, kind, enabled))
        if (result is DebuggerRegistrationResult.Success) updateAccessTracing(tracedBefore)
        return result
    }

    fun removeWatch(id: Long): DebuggerRegistrationResult {
        val tracedBefore = debugObservationEnabled || hasMemoryAccessWatch
        val result = registrationChanged(registrations.removeWatch(id))
        if (result is DebuggerRegistrationResult.Success) updateAccessTracing(tracedBefore)
        return result
    }

    fun setWatchEnabled(id: Long, enabled: Boolean): DebuggerRegistrationResult {
        val entry = watchRegistrations().firstOrNull { it.id == id }
            ?: return DebuggerRegistrationResult.Failure(DebuggerRegistrationError.NOT_FOUND)
        return saveWatch(id, entry.range.first, entry.range.last, entry.kind, enabled)
    }

    private fun registrationChanged(result: DebuggerRegistrationResult): DebuggerRegistrationResult {
        if (result is DebuggerRegistrationResult.Success) { registrationRevision++; sessionRevision++ }
        return result
    }

    private fun updateAccessTracing(previous: Boolean) {
        val enabled = debugObservationEnabled || hasMemoryAccessWatch
        if (previous == enabled) return
        session.setMemoryAccessTracing(enabled)
        session.drainMemoryAccesses()
        lastStepMemoryAccesses = emptyList()
    }

    fun toggleBreakpoint(address: Int): Boolean {
        val normalized = address and 0xffff
        val old = breakpointRegistrations().firstOrNull { it.address == normalized }
        if (old?.enabled == true) { removeBreakpoint(old.id); return false }
        return saveBreakpoint(old?.id, normalized, true) is DebuggerRegistrationResult.Success
    }

    /** Legacy active-address view, also used by execution checks. */
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
        session.setMemoryAccessTracing(enabled || hasMemoryAccessWatch)
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

    /** Compatibility setters replace their own category; the UI uses saveWatch for multiple entries. */
    fun setMemoryWatch(startAddress: Int, endAddressInclusive: Int) {
        require(startAddress in 0..0xffff && endAddressInclusive in startAddress..0xffff)
        require(endAddressInclusive - startAddress + 1 <= MAX_MEMORY_WATCH_BYTES)
        clearMemoryWatch()
        check(saveWatch(null, startAddress, endAddressInclusive, DebuggerWatchKind.CHANGE) is DebuggerRegistrationResult.Success)
    }

    fun clearMemoryWatch() {
        watchRegistrations().filter { it.kind == DebuggerWatchKind.CHANGE }.forEach { removeWatch(it.id) }
    }

    fun memoryWatch(): IntRange? = watchRegistrations().firstOrNull { it.enabled && it.kind == DebuggerWatchKind.CHANGE }?.range

    fun setMemoryAccessWatch(startAddress: Int, endAddressInclusive: Int, reads: Boolean, writes: Boolean) {
        require(startAddress in 0..0xffff && endAddressInclusive in startAddress..0xffff)
        require(reads || writes)
        clearMemoryAccessWatch()
        val kind = if (reads && writes) DebuggerWatchKind.READ_WRITE else if (reads) DebuggerWatchKind.READ else DebuggerWatchKind.WRITE
        check(saveWatch(null, startAddress, endAddressInclusive, kind) is DebuggerRegistrationResult.Success)
    }

    fun clearMemoryAccessWatch() {
        watchRegistrations().filter { it.kind != DebuggerWatchKind.CHANGE }.forEach { removeWatch(it.id) }
    }

    fun memoryAccessWatchRange(): IntRange? = watchRegistrations().firstOrNull { it.enabled && it.kind != DebuggerWatchKind.CHANGE }?.range

    fun runToAddress(address: Int) {
        if (state == RunnerState.FAULTED) return
        interruptDebugStep()
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
        debugStepStatus = DebuggerStepStatus.IDLE
        val instructionAddress = if (hasMemoryWatch || hasMemoryAccessWatch) {
            programCounterProvider() and 0xffff
        } else 0
        val result = stepSession()
        val executionStatus = result.status
        if (executionStatus is ExecutionStatus.Faulted) {
            state = RunnerState.FAULTED
            stopReason = DebuggerStopReason.Fault(executionStatus.fault)
        } else {
            stopReason = detectWatches(instructionAddress) ?: DebuggerStopReason.StepComplete
        }
        return result
    }

    fun displaySnapshot(): DisplaySnapshot = session.displaySnapshot()

    fun cpuSnapshot(): CpuSnapshot = session.cpuSnapshot()

    fun audioSnapshot(): AudioSnapshot = session.audioSnapshot()

    fun drainAudioSamples(): AudioPcmSnapshot = session.drainAudioSamples()

    fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult {
        debugTargetGeneration++
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadBasicProgram(program)
        if (result is BasicProgramLoadResult.Success) {
            registrations.resetHitCounts()
            callTracker.clear()
            sourceMap = null
            refreshMemoryWatchBaseline()
        }
        sessionRevision++
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun loadProgramImage(program: ByteArray?, image: AddressedMemoryImage?): com.digihori.pgp.core.api.ProgramImageLoadResult {
        debugTargetGeneration++
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadProgramImage(program, image)
        if (result is com.digihori.pgp.core.api.ProgramImageLoadResult.Success) {
            sourceMap = null
            registrations.resetHitCounts()
            callTracker.clear()
            sessionRevision++
            refreshMemoryWatchBaseline()
        }
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun basicProgramSnapshot(): BasicProgramSnapshotResult = session.basicProgramSnapshot()

    fun loadMemoryImage(image: AddressedMemoryImage): MemoryImageLoadResult {
        debugTargetGeneration++
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadMemoryImage(image)
        if (result is MemoryImageLoadResult.Success) {
            registrations.resetHitCounts()
            callTracker.clear()
            sourceMap = null
            refreshMemoryWatchBaseline()
        }
        sessionRevision++
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot =
        session.memorySnapshot(startAddress, length)

    private val memoryEditSessionId = java.util.UUID.randomUUID().toString()

    fun previewMemoryEdit(address: Int, value: Int): DebuggerMemoryEditResult {
        fun failure(error: DebuggerMemoryEditError) = DebuggerMemoryEditResult.Failure(error)
        if (state != RunnerState.PAUSED) return failure(DebuggerMemoryEditError.PAUSED_ONLY)
        if (address !in 0..0xffff) return failure(DebuggerMemoryEditError.ADDRESS)
        if (value !in 0..255) return failure(DebuggerMemoryEditError.VALUE)
        val location = memoryLayout.locate(address)
        if (location.kind != com.digihori.pgp.core.api.MachineMemoryKind.RAM || !location.writable)
            return failure(DebuggerMemoryEditError.RAM_ONLY)
        val before = try { memoryByte(address) } catch (_: Exception) { return failure(DebuggerMemoryEditError.READ) }
        if (before == value) return failure(DebuggerMemoryEditError.UNCHANGED)
        return DebuggerMemoryEditResult.Success(DebuggerMemoryEditPreview(address, location.physicalAddress, before, value, sessionRevision, memoryEditSessionId))
    }

    /** Revalidate the preview before a one-byte host load. Rejected edits never pause or mutate the session. */
    fun applyMemoryEdit(edit: DebuggerMemoryEditPreview): DebuggerMemoryEditResult {
        if (state != RunnerState.PAUSED) return DebuggerMemoryEditResult.Failure(DebuggerMemoryEditError.PAUSED_ONLY)
        if (edit.sessionId != memoryEditSessionId || edit.revision != sessionRevision) return DebuggerMemoryEditResult.Failure(DebuggerMemoryEditError.STALE)
        val checked = previewMemoryEdit(edit.address, edit.after)
        if (checked !is DebuggerMemoryEditResult.Success) return checked
        if (checked.edit != edit) return DebuggerMemoryEditResult.Failure(DebuggerMemoryEditError.STALE)
        val image = (com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory.create(listOf(
            com.digihori.pgp.core.source.machine.AddressedMemorySegmentData(edit.address, byteArrayOf(edit.after.toByte())),
        )) as com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult.Success).image
        val result = session.loadMemoryImage(image)
        if (result !is MemoryImageLoadResult.Success) return DebuggerMemoryEditResult.Failure(DebuggerMemoryEditError.WRITE)
        registrations.rebaseEditedByte(edit.after) { address ->
            val location = memoryLayout.locate(address)
            location.kind == com.digihori.pgp.core.api.MachineMemoryKind.RAM && location.physicalAddress == edit.physicalAddress
        }
        debugTargetGeneration++
        sessionRevision++
        sourceMap = null
        callTracker.clear()
        stepPlan = null
        debugStepStatus = DebuggerStepStatus.IDLE
        temporaryRunToAddress = null
        breakpointToSkipOnce = null
        return DebuggerMemoryEditResult.Success(edit)
    }

    fun memoryByte(address: Int): Int =
        session.memorySnapshot(address and 0xffff, 1).copyBytes().single().toInt() and 0xff

    fun selectedRomBank(): Int? = session.selectedRomBank()

    fun resolveRomLocation(address: Int): PhysicalRomLocation? =
        session.resolveRomLocation(address and 0xffff)

    override fun pressKey(key: PocketKey): InputResult = pressHostKey(key, "screen:${key.name}")

    override fun pressHostKey(key: PocketKey, owner: String): InputResult {
        val wasHeld = hostKeys.isHeld(key)
        return hostKeys.press(owner, key).also { result ->
            if (result is InputResult.Accepted) {
                sessionRevision++
                if (!wasHeld && operatingMode == OperatingMode.RUN && key != PocketKey.DEF) {
                    commandHistory.recordUserKey(key)
                }
            }
        }
    }

    override fun releaseKey(key: PocketKey): InputResult = releaseHostKey(key, "screen:${key.name}")

    override fun releaseHostKey(key: PocketKey, owner: String): InputResult {
        val result = hostKeys.release(owner)
        if (result is InputResult.Accepted) sessionRevision++
        return result
    }

    fun pressGamepadKey(button: GamepadButton, key: PocketKey) {
        if (hostKeys.press("gamepad:${button.name}", key) is InputResult.Accepted) {
            commandHistory.clearPendingInput()
            sessionRevision++
        }
    }

    fun releaseGamepadKey(button: GamepadButton, key: PocketKey) {
        releaseHostKey(key, "gamepad:${button.name}")
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
        interruptDebugStep()
        check(state != RunnerState.FAULTED) { "Cannot run input while the emulator is faulted" }
        val resumeAfterInput = state == RunnerState.RUNNING
        applyTransition(keyInputQueue.cancel())
        keyInputQueue.enqueue(keys)
        planner.reset()
        previousTimeNanoseconds = null

        var executedCycles = 0L
        var executedInstructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        if (!session.isOperatingModeTransitionPending()) applyTransition(keyInputQueue.start())
        while ((!keyInputQueue.isIdle || session.isOperatingModeTransitionPending()) && status !is ExecutionStatus.Faulted) {
            if (!session.isOperatingModeTransitionPending()) applyTransition(keyInputQueue.start())
            val requestedCycles = if (session.isOperatingModeTransitionPending()) 4_096L else keyInputQueue.limitCycles(Long.MAX_VALUE)
            val partialResult = runSessionCycles(requestedCycles)
            check(partialResult.executedCycles > 0 || status is ExecutionStatus.Faulted) { "Session made no progress" }
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
            interruptDebugStep()
            callTracker.clear()
        } else if (resumeAfterInput) {
            previousTimeNanoseconds = clock.nowNanoseconds()
        } else {
            state = RunnerState.PAUSED
        }
        refreshMemoryWatchBaseline()
        return RunResult(executedCycles, executedInstructions, status)
    }

    fun currentOperatingMode(): OperatingMode = operatingMode

    fun setOperatingMode(mode: OperatingMode) {
        session.setOperatingMode(mode)
        if (operatingMode != mode) { sessionRevision++; debugTargetGeneration++ }
        if (operatingMode != mode) {
            applyTransition(keyInputQueue.cancel())
            hostKeys.releaseAll()
            inputEpoch++
            commandHistory.clearPendingInput()
        }
        requestedOperatingMode = mode
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

        val budget = if (unlimited) CycleBudget(4_096L, 0L, 0L) else planner.plan(now - previous, speed)
        if (budget.cycles == 0L) return RunnerTick(budget, null)

        if (!session.isOperatingModeTransitionPending()) applyTransition(keyInputQueue.start())
        var cyclesRemaining = budget.cycles
        var executedCycles = 0L
        var executedInstructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        while (cyclesRemaining > 0 && status !is ExecutionStatus.Faulted) {
            if (!session.isOperatingModeTransitionPending()) applyTransition(keyInputQueue.start())
            val requestedCycles = if (session.isOperatingModeTransitionPending()) minOf(4_096L, cyclesRemaining) else keyInputQueue.limitCycles(cyclesRemaining)
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
                interruptDebugStep()
                applyTransition(keyInputQueue.cancel())
                state = RunnerState.PAUSED
                previousTimeNanoseconds = null
                planner.reset()
                stopReason = partial.stopReason
                break
            }
            check(partialResult.executedCycles > 0 || status is ExecutionStatus.Faulted) { "Session made no progress" }
        }
        val result = RunResult(executedCycles, executedInstructions, status)
        if (status is ExecutionStatus.Faulted) {
            applyTransition(keyInputQueue.cancel())
            state = RunnerState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
            stopReason = DebuggerStopReason.Fault(status.fault)
            temporaryRunToAddress = null
            interruptDebugStep()
            callTracker.clear()
        }
        return RunnerTick(budget, result)
    }

    private fun runUntilBreakpoint(cycleBudget: Long): BreakpointRunResult {
        if (
            executionBreakpoints.isEmpty() && temporaryRunToAddress == null &&
            !instructionTraceEnabled && !hasMemoryWatch && !hasMemoryAccessWatch &&
            !debugObservationEnabled && stepPlan == null
        ) {
            callTracker.clear()
            val result = session.runCycles(cycleBudget)
            collectBankEvents()
            if (result.executedInstructions > 0) sessionRevision += result.executedInstructions
            return BreakpointRunResult(result)
        }
        var cycles = 0L
        var instructions = 0L
        var status: ExecutionStatus = ExecutionStatus.Ready
        while (cycles < cycleBudget && status !is ExecutionStatus.Faulted) {
            val needsProgramCounter = stepPlan != null || temporaryRunToAddress != null ||
                executionBreakpoints.isNotEmpty() || hasMemoryWatch || hasMemoryAccessWatch
            val pc = if (needsProgramCounter) programCounterProvider() and 0xffff else 0
            if (temporaryRunToAddress != null || executionBreakpoints.isNotEmpty()) {
                if (pc == temporaryRunToAddress) {
                    temporaryRunToAddress = null
                    return BreakpointRunResult(
                        RunResult(cycles, instructions, status),
                        stopReason = DebuggerStopReason.RunToAddress(pc),
                    )
                }
                if (pc in executionBreakpoints && pc != breakpointToSkipOnce && registrations.breakpointHit(pc, session::cpuSnapshot)) {
                    sessionRevision++
                    return BreakpointRunResult(
                        RunResult(cycles, instructions, status),
                        stopReason = DebuggerStopReason.Breakpoint(pc),
                    )
                }
            }
            val plan = stepPlan
            if (plan != null && !plan.single && memoryByte(pc) in listOf(0x7a, 0x69)) {
                stepPlan = null
                debugStepStatus = DebuggerStepStatus.UNSUPPORTED_FLOW
                return BreakpointRunResult(RunResult(cycles, instructions, status), DebuggerStopReason.UserPause)
            }
            breakpointToSkipOnce = null
            val step = stepSession()
            cycles += step.cycles
            instructions++
            status = step.status
            val watchStop = detectWatches(pc)
            if (watchStop != null) {
                return BreakpointRunResult(RunResult(cycles, instructions, status), stopReason = watchStop)
            }
            if (plan != null && status !is ExecutionStatus.Faulted) {
                plan.instructions++
                plan.cycles += step.cycles
                val result = when {
                    !plan.single && lastCallEvent.lost -> DebuggerStepStatus.FRAME_LOST
                    plan.single || (plan.frame != null && lastCallEvent.returned == plan.frame) ->
                        if (plan.out) DebuggerStepStatus.OUT_COMPLETE else DebuggerStepStatus.OVER_COMPLETE
                    plan.instructions >= debugStepInstructionLimit || plan.cycles >= debugStepCycleLimit -> DebuggerStepStatus.LIMIT
                    else -> null
                }
                if (plan.frame == null && !plan.single) plan.frame = lastCallEvent.entered
                if (result != null) {
                    stepPlan = null
                    debugStepStatus = result
                    return BreakpointRunResult(RunResult(cycles, instructions, status),
                        if (result == DebuggerStepStatus.OVER_COMPLETE || result == DebuggerStepStatus.OUT_COMPLETE)
                            DebuggerStopReason.StepComplete else DebuggerStopReason.UserPause)
                }
            }
        }
        return BreakpointRunResult(RunResult(cycles, instructions, status))
    }

    private fun runSessionCycles(cycleBudget: Long): RunResult {
        if (!instructionTraceEnabled && !debugObservationEnabled) {
            callTracker.clear()
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

    private var lastCallEvent = DebuggerCallTracker.Event()

    private fun stepSession(): StepResult {
        val beforeCall = session.cpuSnapshot()
        val opcode = memoryByte(beforeCall.programCounter)
        val callInstruction = if (DebuggerCallTracker.isCall(opcode)) Sc61860InstructionDecoder.decode(beforeCall.programCounter, ::memoryByte) else null
        val returnRom = callInstruction?.let { session.resolveRomLocation(it.nextAddress) }
        val instructionAddress = if (debugObservationEnabled || hasMemoryAccessWatch) {
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
        lastCallEvent = if (result.status is ExecutionStatus.Faulted) {
            callTracker.clear()
            DebuggerCallTracker.Event(lost = true)
        } else callTracker.observe(beforeCall, session.cpuSnapshot(), opcode, callInstruction?.targetAddress,
            returnRom, session::resolveRomLocation)
        sessionRevision++
        lastStepMemoryAccesses = if (debugObservationEnabled || hasMemoryAccessWatch) {
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

    private fun refreshMemoryWatchBaseline() = registrations.refreshBaselines()

    private fun detectWatches(instructionAddress: Int): DebuggerStopReason? {
        val accesses = registrations.accesses(lastStepMemoryAccesses)
        // Refresh every change baseline even when an access watch wins the stop reason.
        val changes = registrations.changes()
        return if (accesses.isNotEmpty()) DebuggerStopReason.MemoryAccessed(instructionAddress, accesses)
            else if (changes.isNotEmpty()) DebuggerStopReason.MemoryChanged(instructionAddress, changes) else null
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
            is KeyTransition.Press -> hostKeys.press("queue", transition.key)
            is KeyTransition.Release -> hostKeys.release("queue")
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
