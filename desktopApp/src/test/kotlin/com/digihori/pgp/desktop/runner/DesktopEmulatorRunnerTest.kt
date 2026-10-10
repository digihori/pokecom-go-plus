package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.api.AudioSnapshot
import com.digihori.pgp.core.api.AudioPcmSnapshot
import com.digihori.pgp.core.api.BasicProgramLoadResult
import com.digihori.pgp.core.api.BasicProgramMemoryError
import com.digihori.pgp.core.api.BasicProgramSnapshotResult
import com.digihori.pgp.core.api.CoreFault
import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.ExecutionStatus
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.MemoryAccessKind
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.RunResult
import com.digihori.pgp.core.api.StepResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.runtime.CycleBudgetPlanner
import com.digihori.pgp.core.runtime.KeyInputQueue
import com.digihori.pgp.core.runtime.SpeedRatio
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.debug.DesktopInstructionTraceEntry
import com.digihori.pgp.core.debug.Sc61860DecodedInstruction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopEmulatorRunnerTest {
    @Test
    fun debugObservationRetainsMemoryAccessesWithoutConsumingWatchEventsTwice() {
        val access = MemoryAccess(MemoryAccessKind.WRITE, 0xc123, 0x5a)
        val session = FakeSession(memoryAccessOnStep = access)
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = FakeClock(),
            programCounterProvider = { 0x2345 },
        )
        runner.setDebugObservationEnabled(true)
        runner.setMemoryAccessWatch(0xc000, 0xcfff, reads = false, writes = true)

        runner.step()

        val history = runner.memoryAccessHistory().single()
        assertEquals(0x2345, history.instructionAddress)
        assertEquals(access, history.access)
        val reason = assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
        assertEquals(listOf(access), reason.accesses)
        assertTrue(runner.sessionRevision > 0)
    }

    @Test
    fun exposesMachineSpecificMinimumSoftwareKeyHold() {
        assertEquals(60L, runner(FakeSession(), FakeClock()).minimumSoftwareKeyHoldMilliseconds())
        assertEquals(
            200L,
            DesktopEmulatorRunner(
                FakeSession(machineId = Pc1251RomDefinition.MACHINE_ID),
                FakeClock(),
            ).minimumSoftwareKeyHoldMilliseconds(),
        )
    }

    @Test
    fun pc1251UsesIts192KilohertzCpuClockByDefault() {
        val clock = FakeClock()
        val session = FakeSession(machineId = com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition.MACHINE_ID)
        val runner = DesktopEmulatorRunner(session, clock)

        runner.run()
        clock.advance(1_000_000_000)
        val tick = runner.tick()

        assertEquals(19_200L, tick.budget.cycles)
        assertEquals(19_200L, tick.runResult?.executedCycles)
    }
    @Test
    fun basicProgramLoadRestoresThePreviousRunningState() {
        val runningSession = FakeSession()
        val runningRunner = runner(runningSession, FakeClock())
        runningRunner.run()

        runningRunner.loadBasicProgram(byteArrayOf(0xff.toByte(), 0xff.toByte()))

        assertEquals(RunnerState.RUNNING, runningRunner.state)

        val pausedRunner = runner(FakeSession(), FakeClock())
        pausedRunner.loadBasicProgram(byteArrayOf(0xff.toByte(), 0xff.toByte()))
        assertEquals(RunnerState.PAUSED, pausedRunner.state)
    }

    @Test
    fun failedBasicProgramLoadAlsoRestoresRunningStateAndReturnsTheReason() {
        val expected = BasicProgramLoadResult.Failure(
            BasicProgramMemoryError.ProgramTooLarge(size = 2000, capacity = 1761),
        )
        val session = FakeSession(basicLoadResult = expected)
        val runner = runner(session, FakeClock())
        runner.run()

        val actual = runner.loadBasicProgram(ByteArray(2000))

        assertEquals(expected, actual)
        assertEquals(RunnerState.RUNNING, runner.state)
    }

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
        assertEquals(DebuggerStopReason.Reset, runner.stopReason)
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
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
    }

    @Test
    fun recordsUserPauseAndClearsStopReasonWhenRunning() {
        val runner = runner(FakeSession(), FakeClock())

        runner.run()
        assertNull(runner.stopReason)
        runner.pause()

        assertEquals(DebuggerStopReason.UserPause, runner.stopReason)
    }

    @Test
    fun stopsBeforeExecutingBreakpointAndCanContinuePastIt() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            programCounterProvider = { 0x1000 + session.stepCount },
        )
        runner.toggleBreakpoint(0x1002)

        runner.run()
        clock.advance(1_000_000)
        val stopped = runner.tick()

        assertEquals(2, session.stepCount)
        assertEquals(8, stopped.runResult?.executedCycles)
        assertEquals(RunnerState.PAUSED, runner.state)
        assertEquals(DebuggerStopReason.Breakpoint(0x1002), runner.stopReason)

        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        assertEquals(RunnerState.RUNNING, runner.state)
        assertEquals(null, runner.stopReason)
        assertTrue(session.stepCount > 2)
    }

    @Test
    fun runToAddressStopsOnceWithoutCreatingPersistentBreakpoint() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            programCounterProvider = { 0x2000 + session.stepCount },
        )

        runner.runToAddress(0x2003)
        clock.advance(1_000_000)
        runner.tick()

        assertEquals(3, session.stepCount)
        assertEquals(RunnerState.PAUSED, runner.state)
        assertEquals(DebuggerStopReason.RunToAddress(0x2003), runner.stopReason)
        assertTrue(runner.breakpoints().isEmpty())

        runner.run()
        clock.advance(1_000_000)
        runner.tick()
        assertEquals(RunnerState.RUNNING, runner.state)
    }

    @Test
    fun instructionTraceIsOptInAndRetainsTheNewest256Instructions() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            traceEntryProvider = { sequence -> traceEntry(sequence) },
        )

        runner.run()
        clock.advance(1_000_000)
        runner.tick()
        assertTrue(runner.instructionTrace().isEmpty())
        assertEquals(listOf(288L), session.budgets)

        runner.setInstructionTraceEnabled(true)
        clock.advance(10_000_000)
        runner.tick()

        val trace = runner.instructionTrace()
        assertEquals(256, trace.size)
        assertEquals(464L, trace.first().sequence)
        assertEquals(719L, trace.last().sequence)
        assertEquals(720, session.stepCount)

        runner.clearInstructionTrace()
        assertTrue(runner.instructionTrace().isEmpty())
    }

    @Test
    fun instructionTraceCapacityCanChangeAndTrimsTheOldestEntries() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            traceEntryProvider = { sequence -> traceEntry(sequence) },
        )
        runner.setInstructionTraceCapacity(16)
        runner.setInstructionTraceEnabled(true)
        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        assertEquals(16, runner.instructionTraceCapacity())
        assertEquals(16, runner.instructionTrace().size)
        assertEquals(56L, runner.instructionTrace().first().sequence)
        assertEquals(71L, runner.instructionTrace().last().sequence)
    }

    @Test
    fun stopsAfterTheInstructionThatChangesWatchedMemory() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            programCounterProvider = { 0x1234 },
            memoryBytesProvider = { _, length ->
                ByteArray(length) { if (session.stepCount == 0) 0 else 0x7f }
            },
        )
        runner.setMemoryWatch(0xc000, 0xc003)

        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        assertEquals(RunnerState.PAUSED, runner.state)
        val reason = assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason)
        assertEquals(0x1234, reason.instructionAddress)
        assertEquals(4, reason.changes.size)
        assertEquals(0xc000, reason.changes.first().address)
        assertEquals(0, reason.changes.first().before)
        assertEquals(0x7f, reason.changes.first().after)
        assertEquals(1, session.stepCount)
    }

    @Test
    fun stopsOnSameValueWriteAccessWithoutRequiringAValueChange() {
        val session = FakeSession(
            memoryAccessOnStep = MemoryAccess(MemoryAccessKind.WRITE, 0xc123, 0),
        )
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            programCounterProvider = { 0x4567 },
        )
        runner.setMemoryAccessWatch(0xc100, 0xc1ff, reads = false, writes = true)

        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        val reason = assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
        assertEquals(0x4567, reason.instructionAddress)
        assertEquals(0xc123, reason.accesses.single().address)
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
    fun recordsDirectRunModeCommandAndRecallsItWithoutEnter() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            keyInputQueue = KeyInputQueue(holdCycles = 10, gapCycles = 5),
        )
        listOf(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER).forEach { key ->
            runner.pressKey(key)
            runner.releaseKey(key)
        }
        session.inputEvents.clear()

        assertEquals(1, runner.commandHistorySize())
        assertEquals(true, runner.recallPreviousCommand())
        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        assertEquals(
            listOf("press:R", "release:R", "press:U", "release:U", "press:N", "release:N"),
            session.inputEvents,
        )
        assertEquals(1, runner.commandHistorySize())
    }

    @Test
    fun doesNotRecordProgramModeInputAsCommandHistory() {
        val runner = runner(FakeSession(), FakeClock())
        runner.setOperatingMode(OperatingMode.PROGRAM)

        runner.pressKey(PocketKey.R)
        runner.releaseKey(PocketKey.R)
        runner.pressKey(PocketKey.ENTER)
        runner.releaseKey(PocketKey.ENTER)

        assertEquals(0, runner.commandHistorySize())
        assertEquals(false, runner.recallPreviousCommand())
    }


    @Test
    fun runsQueuedKeySequenceAtCycleBoundaries() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(
                cyclesPerSecond = CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND,
                maximumCatchUpNanoseconds = 100_000_000,
            ),
            keyInputQueue = KeyInputQueue(holdCycles = 10, gapCycles = 5),
        )

        runner.enqueueKeySequence(listOf(PocketKey.SHIFT, PocketKey.Q))
        runner.run()
        clock.advance(50_000_000)
        val tick = runner.tick()

        assertEquals(14_400, tick.runResult?.executedCycles)
        assertEquals(listOf(10L, 5L, 10L, 14_375L), session.budgets)
        assertEquals(
            listOf("press:SHIFT", "release:SHIFT", "press:Q", "release:Q"),
            session.inputEvents,
        )
    }

    @Test
    fun pauseCancelsQueuedInputAndReleasesItsActiveKey() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = runner(session, clock)
        runner.enqueueKeySequence(listOf(PocketKey.SHIFT, PocketKey.Q))
        runner.run()
        clock.advance(1_000_000)
        runner.tick()

        runner.pause()

        assertEquals(listOf("press:SHIFT", "release:SHIFT"), session.inputEvents)
    }

    @Test
    fun runsGeneratedInputImmediatelyAndResumesThePreviousRunningState() {
        val session = FakeSession()
        val clock = FakeClock()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = clock,
            planner = CycleBudgetPlanner(CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND),
            keyInputQueue = KeyInputQueue(holdCycles = 10, gapCycles = 5),
        )
        runner.run()

        val result = runner.runKeySequenceImmediately(listOf(PocketKey.SHIFT, PocketKey.Q))

        assertEquals(25, result.executedCycles)
        assertEquals(listOf(10L, 5L, 10L), session.budgets)
        assertEquals(
            listOf("press:SHIFT", "release:SHIFT", "press:Q", "release:Q"),
            session.inputEvents,
        )
        assertEquals(RunnerState.RUNNING, runner.state)
    }

    @Test
    fun immediateInputLeavesAPausedRunnerPaused() {
        val runner = DesktopEmulatorRunner(
            session = FakeSession(),
            clock = FakeClock(),
            keyInputQueue = KeyInputQueue(holdCycles = 10, gapCycles = 5),
        )

        runner.runKeySequenceImmediately(listOf(PocketKey.A))

        assertEquals(RunnerState.PAUSED, runner.state)
    }

    @Test
    fun usesThePc1251FamilyGeneratedKeyInterval() {
        Pc1251FamilyModel.entries.forEach { model ->
            val session = FakeSession(machineId = model.machineId)
            val runner = DesktopEmulatorRunner(session = session, clock = FakeClock())

            runner.runKeySequenceImmediately(listOf(PocketKey.NUM_1, PocketKey.NUM_0))

            assertEquals(listOf(38_400L, 19_200L, 38_400L), session.budgets, model.name)
        }
    }

    @Test
    fun immediateInputWaitsForTheRomToStoreEachBasicLineAfterEnter() {
        val session = FakeSession()
        val runner = DesktopEmulatorRunner(
            session = session,
            clock = FakeClock(),
            keyInputQueue = KeyInputQueue(holdCycles = 10, gapCycles = 5),
        )

        val result = runner.runKeySequenceImmediately(
            listOf(PocketKey.A, PocketKey.ENTER, PocketKey.B),
        )

        assertEquals(57_640, result.executedCycles)
        assertEquals(listOf(10L, 5L, 10L, 57_600L, 5L, 10L), session.budgets)
        assertEquals(
            listOf(
                "press:A", "release:A",
                "press:ENTER", "release:ENTER",
                "press:B", "release:B",
            ),
            session.inputEvents,
        )
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

    private fun traceEntry(sequence: Long): DesktopInstructionTraceEntry =
        DesktopInstructionTraceEntry(
            sequence = sequence,
            dataPointer = 0xc000,
            p = 0,
            q = 0,
            r = 0,
            d = 0,
            carry = false,
            zero = false,
            instruction = Sc61860DecodedInstruction(
                address = sequence.toInt() and 0xffff,
                definition = null,
                bytes = listOf(0),
                operandValue = null,
                targetAddress = null,
            ),
        )

    private class FakeClock(var now: Long = 0) : MonotonicClock {
        override fun nowNanoseconds(): Long = now
        fun advance(nanoseconds: Long) { now += nanoseconds }
    }

    private class FakeSession(
        private val faultOnRun: Boolean = false,
        private val basicLoadResult: BasicProgramLoadResult? = null,
        override val machineId: MachineId = MachineId("pc-1245"),
        private val memoryAccessOnStep: MemoryAccess? = null,
    ) : EmulatorSession {
        val budgets = mutableListOf<Long>()
        var resetCount = 0
        var stepCount = 0
        val pressedKeys = mutableListOf<PocketKey>()
        val releasedKeys = mutableListOf<PocketKey>()
        val inputEvents = mutableListOf<String>()
        var recordedOperatingMode = OperatingMode.RUN
        var memoryTracing = false
        val memoryAccesses = mutableListOf<MemoryAccess>()

        override fun reset() { resetCount++ }
        override fun step(): StepResult {
            stepCount++
            if (memoryTracing) memoryAccessOnStep?.let(memoryAccesses::add)
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
            inputEvents += "press:$key"
            return InputResult.Accepted
        }
        override fun releaseKey(key: PocketKey): InputResult {
            releasedKeys += key
            inputEvents += "release:$key"
            return InputResult.Accepted
        }
        override fun setOperatingMode(mode: OperatingMode) {
            recordedOperatingMode = mode
        }
        override fun cpuSnapshot(): CpuSnapshot = unsupported()
        override fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot = unsupported()
        override fun displaySnapshot(): DisplaySnapshot = unsupported()
        override fun audioSnapshot(): AudioSnapshot = unsupported()
        override fun drainAudioSamples(): AudioPcmSnapshot = unsupported()
        override fun loadBasicProgram(program: ByteArray): BasicProgramLoadResult =
            basicLoadResult ?: BasicProgramLoadResult.Success(0xc000, 0xc000 + program.lastIndex, program.size)
        override fun basicProgramSnapshot(): BasicProgramSnapshotResult = unsupported()
        override fun loadMemoryImage(image: AddressedMemoryImage): MemoryImageLoadResult = unsupported()
        override fun setMemoryAccessTracing(enabled: Boolean) { memoryTracing = enabled }
        override fun drainMemoryAccesses(): List<MemoryAccess> = memoryAccesses.toList().also { memoryAccesses.clear() }

        private fun <T> unsupported(): T = error("Not used by runner tests")
    }
}
