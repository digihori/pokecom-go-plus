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
import com.digihori.pgp.core.runtime.CycleBudget
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.KeyInputQueue
import com.digihori.pgp.core.runtime.KeyTransition
import com.digihori.pgp.core.runtime.SpeedRatio
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.desktop.input.DesktopKeyInputSink
import com.digihori.pgp.core.source.machine.AddressedMemoryImage

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
    private val keyInputQueue: KeyInputQueue = KeyInputQueue(
        holdCycles = KEY_HOLD_CYCLES,
        gapCycles = KEY_GAP_CYCLES,
    ),
) : DesktopKeyInputSink {
    private val planner: CycleBudgetPlanner = planner ?: CycleBudgetPlanner(
        cyclesPerSecond = when (session.machineId) {
            Pc1251RomDefinition.MACHINE_ID -> CycleBudgetPlanner.PC1251_CYCLES_PER_SECOND
            else -> CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND
        },
    )
    var state: RunnerState = RunnerState.PAUSED
        private set

    var speed: SpeedRatio = SpeedRatio.NORMAL
        private set

    private var previousTimeNanoseconds: Long? = null

    fun run() {
        if (state != RunnerState.PAUSED) return
        planner.reset()
        previousTimeNanoseconds = clock.nowNanoseconds()
        state = RunnerState.RUNNING
    }

    fun pause() {
        applyTransition(keyInputQueue.cancel())
        planner.reset()
        previousTimeNanoseconds = null
        if (state != RunnerState.FAULTED) state = RunnerState.PAUSED
    }

    fun reset() {
        keyInputQueue.cancel()
        session.reset()
        planner.reset()
        previousTimeNanoseconds = null
        speed = SpeedRatio.NORMAL
        state = RunnerState.PAUSED
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
        val result = session.step()
        if (result.status is ExecutionStatus.Faulted) state = RunnerState.FAULTED
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
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun basicProgramSnapshot(): BasicProgramSnapshotResult = session.basicProgramSnapshot()

    fun loadMemoryImage(image: AddressedMemoryImage): MemoryImageLoadResult {
        val resumeAfterLoad = state == RunnerState.RUNNING
        pause()
        val result = session.loadMemoryImage(image)
        if (resumeAfterLoad && state != RunnerState.FAULTED) run()
        return result
    }

    fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot =
        session.memorySnapshot(startAddress, length)

    override fun pressKey(key: PocketKey): InputResult = session.pressKey(key)

    override fun releaseKey(key: PocketKey): InputResult = session.releaseKey(key)

    override fun enqueueKeySequence(keys: Iterable<PocketKey>) {
        keyInputQueue.enqueue(keys)
    }

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
            val partialResult = session.runCycles(requestedCycles)
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
                val settleResult = session.runCycles(BASIC_LINE_SETTLE_CYCLES)
                executedCycles += settleResult.executedCycles
                executedInstructions += settleResult.executedInstructions
                status = settleResult.status
            }
        }

        if (status is ExecutionStatus.Faulted) {
            applyTransition(keyInputQueue.cancel())
            state = RunnerState.FAULTED
        } else if (resumeAfterInput) {
            previousTimeNanoseconds = clock.nowNanoseconds()
        } else {
            state = RunnerState.PAUSED
        }
        return RunResult(executedCycles, executedInstructions, status)
    }

    fun setOperatingMode(mode: OperatingMode) {
        session.setOperatingMode(mode)
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
            val partialResult = session.runCycles(requestedCycles)
            check(partialResult.executedCycles > 0) { "Session made no progress" }
            executedCycles += partialResult.executedCycles
            executedInstructions += partialResult.executedInstructions
            cyclesRemaining = (cyclesRemaining - partialResult.executedCycles).coerceAtLeast(0)
            status = partialResult.status
            applyTransition(keyInputQueue.advance(partialResult.executedCycles))
        }
        val result = RunResult(executedCycles, executedInstructions, status)
        if (status is ExecutionStatus.Faulted) {
            applyTransition(keyInputQueue.cancel())
            state = RunnerState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
        }
        return RunnerTick(budget, result)
    }

    private fun applyTransition(transition: KeyTransition?) {
        when (transition) {
            is KeyTransition.Press -> session.pressKey(transition.key)
            is KeyTransition.Release -> session.releaseKey(transition.key)
            null -> Unit
        }
    }

    private companion object {
        // Pokecom GO retained a released key for three 20 ms polling intervals.
        const val KEY_HOLD_CYCLES: Long = 17_280 // 60 ms at the PC-1245 288 kHz clock.
        const val KEY_GAP_CYCLES: Long = 5_760 // 20 ms at the PC-1245 288 kHz clock.
        const val BASIC_LINE_SETTLE_CYCLES: Long = 57_600 // 200 ms after ENTER.
    }
}

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
