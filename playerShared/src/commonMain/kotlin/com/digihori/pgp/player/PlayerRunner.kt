package com.digihori.pgp.player

import com.digihori.pgp.core.api.AudioPcmSnapshot
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.RunResult
import com.digihori.pgp.core.runtime.CycleBudget
import com.digihori.pgp.core.runtime.CycleBudgetPlanner

public enum class PlayerRunState {
    PAUSED,
    RUNNING,
    FAULTED,
}

public data class PlayerTick(
    public val budget: CycleBudget,
    public val runResult: RunResult?,
    /** Consumed on the session thread on every executed tick, even if the host is muted. */
    public val audio: AudioPcmSnapshot? = null,
    public val speed: PlayerExecutionSpeed = PlayerExecutionSpeed.NORMAL,
) {
    public companion object {
        public fun idle(speed: PlayerExecutionSpeed = PlayerExecutionSpeed.NORMAL): PlayerTick =
            PlayerTick(CycleBudget(0, 0, 0), null, speed = speed)
    }
}

/**
 * Minimal host-driven runner shared by Player applications.
 *
 * It does not read a clock, launch a coroutine, create a thread, or synchronize calls. The platform
 * host supplies monotonic timestamps and serializes all access to this runner and its session.
 */
public class PlayerRunner(
    private val session: EmulatorSession,
    private val planner: CycleBudgetPlanner = CycleBudgetPlanner(
        MachineCatalog.require(session.machineId).cyclesPerSecond,
    ),
) {
    public val machineId get() = session.machineId

    public var state: PlayerRunState = PlayerRunState.PAUSED
        private set

    public var executionStatus: ExecutionStatus = ExecutionStatus.Ready
        private set

    public var speed: PlayerExecutionSpeed = PlayerExecutionSpeed.NORMAL
        private set

    /** Change at a supplied host-time boundary; never reset RAM or resume a paused CPU. */
    public fun setSpeed(value: PlayerExecutionSpeed, nowNanoseconds: Long) {
        require(nowNanoseconds >= 0)
        if (speed == value) return
        previousTimeNanoseconds?.let { require(nowNanoseconds >= it) }
        speed = value
        planner.reset()
        cycleOvershoot = 0
        if (state == PlayerRunState.RUNNING) previousTimeNanoseconds = nowNanoseconds
    }

    private var previousTimeNanoseconds: Long? = null
    // Instructions complete atomically. Charge their excess to later ticks, so PCM and CPU
    // time do not run progressively ahead of the host clock (notably with WAIT instructions).
    private var cycleOvershoot: Long = 0

    public fun run(nowNanoseconds: Long) {
        require(nowNanoseconds >= 0) { "Monotonic time must not be negative" }
        if (state != PlayerRunState.PAUSED) return
        planner.reset()
        cycleOvershoot = 0
        previousTimeNanoseconds = nowNanoseconds
        state = PlayerRunState.RUNNING
    }

    public fun pause() {
        planner.reset()
        cycleOvershoot = 0
        previousTimeNanoseconds = null
        if (state != PlayerRunState.FAULTED) state = PlayerRunState.PAUSED
    }

    public fun reset() {
        session.reset()
        planner.reset()
        cycleOvershoot = 0
        previousTimeNanoseconds = null
        executionStatus = ExecutionStatus.Ready
        state = PlayerRunState.PAUSED
    }

    public fun tick(nowNanoseconds: Long): PlayerTick {
        require(nowNanoseconds >= 0) { "Monotonic time must not be negative" }
        if (state != PlayerRunState.RUNNING) return PlayerTick.idle(speed)
        val previous = checkNotNull(previousTimeNanoseconds)
        require(nowNanoseconds >= previous) { "Monotonic clock moved backwards" }
        previousTimeNanoseconds = nowNanoseconds

        val budget = planner.plan(nowNanoseconds - previous, speed.ratio)
        if (budget.cycles == 0L) return PlayerTick(budget, null, speed = speed)
        if (cycleOvershoot >= budget.cycles) {
            cycleOvershoot -= budget.cycles
            return PlayerTick(budget, null, speed = speed)
        }
        val requestedCycles = budget.cycles - cycleOvershoot
        val result = session.runCycles(requestedCycles)
        cycleOvershoot = maxOf(0, result.executedCycles - requestedCycles)
        executionStatus = result.status
        if (result.status is ExecutionStatus.Faulted) {
            state = PlayerRunState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
            cycleOvershoot = 0
        }
        return PlayerTick(budget, result, session.drainAudioSamples(), speed)
    }
}
