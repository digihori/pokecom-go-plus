package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.api.AudioSnapshot
import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.RunResult
import com.digihori.pgp.core.api.StepResult
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.SpeedRatio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DesktopEmulatorRunnerTest {
    @Test
    fun runsTheBudgetCalculatedFromMonotonicElapsedTime() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.run()
        clock.advance(50_000_000)
        val tick = runner.tick()

        assertEquals(14_400, tick.budget.cycles)
        assertEquals(listOf(14_400L), session.budgets)
        assertEquals(RunnerState.RUNNING, runner.state)
    }

    @Test
    fun pauseDropsElapsedTimeAndResumeStartsANewTimeline() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.run()
        clock.advance(10_000_000)
        runner.pause()
        clock.advance(5_000_000_000)
        assertNull(runner.tick().runResult)
        runner.run()
        clock.advance(10_000_000)
        runner.tick()

        assertEquals(listOf(2_880L), session.budgets)
    }

    @Test
    fun repeatedRunDoesNotRestartTheActiveTimeline() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.run()
        clock.advance(10_000_000)
        runner.run()
        clock.advance(10_000_000)
        runner.tick()

        assertEquals(listOf(5_760L), session.budgets)
    }

    @Test
    fun resetResetsTheSessionSpeedAndRunnerState() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.setSpeed(SpeedRatio.DOUBLE)
        runner.run()
        runner.reset()

        assertEquals(1, session.resetCount)
        assertEquals(SpeedRatio.NORMAL, runner.speed)
        assertEquals(RunnerState.PAUSED, runner.state)
    }

    @Test
    fun speedChangeDoesNotChargeTimeFromTheOldSpeed() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.run()
        clock.advance(1_000_000_000)
        runner.setSpeed(SpeedRatio.DOUBLE)
        clock.advance(10_000_000)
        runner.tick()

        assertEquals(listOf(5_760L), session.budgets)
    }

    @Test
    fun faultStopsFurtherExecutionUntilReset() {
        val session = FakeSession(faultOnRun = true)
        val clock = FakeClock()
        val runner = runner(session, clock)

        runner.run()
        clock.advance(10_000_000)
        runner.tick()
        runner.run()
        clock.advance(10_000_000)
        runner.tick()

        assertEquals(RunnerState.FAULTED, runner.state)
        assertEquals(1, session.budgets.size)
        runner.reset()
        assertEquals(RunnerState.PAUSED, runner.state)
    }

    @Test
    fun stepPausesAndPropagatesTheResult() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)
        runner.run()

        val result = runner.step()

        assertEquals(4, result.cycles)
        assertEquals(1, session.stepCount)
        assertEquals(RunnerState.PAUSED, runner.state)
    }

    @Test
    fun delegatesLogicalInputAndOperatingModeToTheSession() {
        val session = FakeSession()
        val runner = runner(session, FakeClock())

        runner.pressKey(PocketKey.A)
        runner.releaseKey(PocketKey.A)
        runner.setOperatingMode(OperatingMode.PROGRAM)

        assertEquals(listOf(PocketKey.A), session.pressedKeys)
        assertEquals(listOf(PocketKey.A), session.releasedKeys)
        assertEquals(OperatingMode.PROGRAM, session.recordedOperatingMode)
    }

    @Test
    fun rejectsABackwardsClock() {
        val clock = FakeClock(now = 100)
        val runner = runner(FakeSession(), clock)
        runner.run()
        clock.now = 99

        assertFailsWith<IllegalArgumentException> {
            runner.tick()
        }
    }

    private fun runner(session: FakeSession, clock: FakeClock): DesktopEmulatorRunner =
        DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(
                cyclesPerSecond = CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND,
                maximumCatchUpNanoseconds = 100_000_000,
            ),
        )

    private class FakeClock(var now: Long = 0) : MonotonicClock {
        override fun nowNanoseconds(): Long = now
        fun advance(nanoseconds: Long) { now += nanoseconds }
    }

    private class FakeSession(
        private val faultOnRun: Boolean = false,
    ) : EmulatorSession {
        override val machineId: MachineId = MachineId("pc-1245")
        val budgets = mutableListOf<Long>()
        var resetCount = 0
        var stepCount = 0
        val pressedKeys = mutableListOf<PocketKey>()
        val releasedKeys = mutableListOf<PocketKey>()
        var recordedOperatingMode = OperatingMode.RUN

        override fun reset() { resetCount++ }
        override fun step(): StepResult {
            stepCount++
            return StepResult(4, ExecutionStatus.Ready)
        }
        override fun runCycles(cycleBudget: Long): RunResult {
            budgets += cycleBudget
            val status = if (faultOnRun) {
                ExecutionStatus.Faulted(CoreFault.UnsupportedOpcode(0x1234, 0xff))
            } else {
                ExecutionStatus.Ready
            }
            return RunResult(cycleBudget, 1, status)
        }
        override fun pressKey(key: PocketKey): InputResult {
            pressedKeys += key
            return InputResult.Accepted
        }
        override fun releaseKey(key: PocketKey): InputResult {
            releasedKeys += key
            return InputResult.Accepted
        }
        override fun setOperatingMode(mode: OperatingMode) {
            recordedOperatingMode = mode
        }
        override fun cpuSnapshot(): CpuSnapshot = unsupported()
        override fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot = unsupported()
        override fun displaySnapshot(): DisplaySnapshot = unsupported()
        override fun audioSnapshot(): AudioSnapshot = unsupported()

        private fun <T> unsupported(): T = error("Not used by runner tests")
    }
}
