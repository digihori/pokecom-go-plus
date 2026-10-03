package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.AudioSnapshot
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.RunResult
import com.digihori.pgp.core.api.StepResult
import com.digihori.pgp.core.runtime.CycleBudget
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.SpeedRatio

internal fun interface MonotonicClock {
    fun nowNanoseconds(): Long
}

internal object SystemMonotonicClock : MonotonicClock {
    override fun nowNanoseconds(): Long = System.nanoTime()
}

internal class DesktopEmulatorRunner(
    private val session: EmulatorSession,
    private val clock: MonotonicClock = SystemMonotonicClock,
    private val planner: CycleBudgetPlanner = CycleBudgetPlanner(
        cyclesPerSecond = CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND,
    ),
) {
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
        planner.reset()
        previousTimeNanoseconds = null
        if (state != RunnerState.FAULTED) state = RunnerState.PAUSED
    }

    fun reset() {
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

    fun pressKey(key: PocketKey): InputResult = session.pressKey(key)

    fun releaseKey(key: PocketKey): InputResult = session.releaseKey(key)

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

        val result = session.runCycles(budget.cycles)
        if (result.status is ExecutionStatus.Faulted) {
            state = RunnerState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
        }
        return RunnerTick(budget, result)
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
