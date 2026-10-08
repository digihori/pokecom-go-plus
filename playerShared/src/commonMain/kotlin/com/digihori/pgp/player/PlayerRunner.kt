package com.digihori.pgp.player

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
) {
    public companion object {
        public fun idle(): PlayerTick = PlayerTick(CycleBudget(0, 0, 0), null)
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

    private var previousTimeNanoseconds: Long? = null

    public fun run(nowNanoseconds: Long) {
        require(nowNanoseconds >= 0) { "Monotonic time must not be negative" }
        if (state != PlayerRunState.PAUSED) return
        planner.reset()
        previousTimeNanoseconds = nowNanoseconds
        state = PlayerRunState.RUNNING
    }

    public fun pause() {
        planner.reset()
        previousTimeNanoseconds = null
        if (state != PlayerRunState.FAULTED) state = PlayerRunState.PAUSED
    }

    public fun reset() {
        session.reset()
        planner.reset()
        previousTimeNanoseconds = null
        executionStatus = ExecutionStatus.Ready
        state = PlayerRunState.PAUSED
    }

    public fun tick(nowNanoseconds: Long): PlayerTick {
        require(nowNanoseconds >= 0) { "Monotonic time must not be negative" }
        if (state != PlayerRunState.RUNNING) return PlayerTick.idle()
        val previous = checkNotNull(previousTimeNanoseconds)
        require(nowNanoseconds >= previous) { "Monotonic clock moved backwards" }
        previousTimeNanoseconds = nowNanoseconds

        val budget = planner.plan(nowNanoseconds - previous)
        if (budget.cycles == 0L) return PlayerTick(budget, null)
        val result = session.runCycles(budget.cycles)
        executionStatus = result.status
        if (result.status is ExecutionStatus.Faulted) {
            state = PlayerRunState.FAULTED
            previousTimeNanoseconds = null
            planner.reset()
        }
        return PlayerTick(budget, result)
    }
}
