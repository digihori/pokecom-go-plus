package com.digihori.pgp.desktop.runner

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.debug.*
import kotlin.test.*

internal fun boundedTestRunner(source: String = "ORG 0\nLIA 1\nJP 0\n", machineId: String = "pc-1245", configuration: EmulatorConfiguration = EmulatorConfiguration()): DesktopEmulatorRunner {
    val bytes = ByteArray(0x2000)
    assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source)).image.segments.forEach { it.copyBytes().copyInto(bytes, it.startAddress) }
    val id = MachineId(machineId)
    return DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
        RomComponent(RomComponentId("internal"), RomRole.INTERNAL, bytes),
        RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
    )), configuration)).session, MonotonicClock { 0L })
}

class BoundedExecutionTest {
    private fun finish(runner: DesktopEmulatorRunner): BoundedExecutionResult {
        repeat(2000) { if (runner.state == RunnerState.RUNNING) runner.tick() }
        assertNotEquals(RunnerState.RUNNING, runner.state)
        return runner.boundedExecution("test")!!
    }
    @Test fun instructionLimitStopsExactlyEvenWithNoElapsedHostTimeAndNoDebugTracing() {
        val runner = boundedTestRunner()
        runner.startBoundedExecution("test", emptyList(), 7, 10000)
        val result = finish(runner)
        assertEquals(7, result.instructions); assertEquals("instructionLimit", result.reason)
        assertEquals(DebuggerStopReason.ExecutionLimit("instructionLimit"), runner.stopReason)
        val revision = runner.sessionRevision; runner.tick(); assertEquals(revision, runner.sessionRevision)
        runner.step(); assertEquals(result, runner.boundedExecution("test"))
    }
    @Test fun cycleLimitCrossesOnlyOneInstructionAndLeavesNoAutomaticKeyHeldOrQueued() {
        val runner = boundedTestRunner("ORG 0\nLIDP 0xC100\nJP 0\n")
        runner.startBoundedExecution("test", listOf(PocketKey.NUM_1, PocketKey.ENTER), 100, 1)
        val result = finish(runner)
        assertEquals(1, result.instructions); assertEquals("cycleLimit", result.reason)
        assertTrue(result.cycles > 1); assertFalse(result.inputComplete); assertTrue(runner.canStartBoundedInput())
        runner.startBoundedExecution("next", emptyList(), 1, 100)
        runner.tick(); assertTrue(runner.boundedExecution("next")!!.inputComplete)
    }
    @Test fun pc1245DiscardsNextKeyWhenStoppedInTheExtendedEnterGap() {
        val runner = boundedTestRunner()
        val keys = listOf(PocketKey.ENTER, PocketKey.NUM_1)
        runner.startBoundedExecution("test", keys, 100_000, 35_000)
        val stopped = finish(runner)
        assertEquals("cycleLimit", stopped.reason)
        assertFalse(stopped.inputComplete)
        assertTrue(runner.canStartBoundedInput())

        runner.startBoundedExecution("test", emptyList(), 100_000, 60_000)
        assertTrue(finish(runner).inputComplete)

        runner.startBoundedExecution("test", keys, 100_000, 45_000)
        assertFalse(finish(runner).inputComplete)
        runner.startBoundedExecution("test", keys, 100_000, 60_000)
        assertTrue(finish(runner).inputComplete)
    }
    @Test fun pc1251RecoveryCountsAgainstTheNewBudgetAndDiscardsUnsentKeys() {
        val runner = boundedTestRunner(machineId = "pc-1251")
        val keys = listOf(PocketKey.NUM_4)
        runner.startBoundedExecution("test", keys, 100_000, 1)
        assertFalse(finish(runner).inputComplete)

        // Without a recovery gap this budget suffices to send one complete tap.
        runner.startBoundedExecution("test", keys, 100_000, 45_000)
        val retry = finish(runner)
        assertEquals("cycleLimit", retry.reason)
        assertFalse(retry.inputComplete)
        assertTrue(runner.canStartBoundedInput())

        runner.startBoundedExecution("test", emptyList(), 100_000, 19_200)
        assertTrue(finish(runner).inputComplete)
        runner.startBoundedExecution("test", keys, 100_000, 45_000)
        assertTrue(finish(runner).inputComplete)
    }
    @Test fun completedKeyEmissionIsRecordedWithoutClaimingThatFirmwareHandledIt() {
        val runner = boundedTestRunner()
        runner.startBoundedExecution("test", listOf(PocketKey.NUM_1, PocketKey.ENTER), 100000, 100000)
        val result = finish(runner)
        assertTrue(result.inputComplete); assertTrue(result.cycles >= 100000)
        assertTrue(runner.canStartBoundedInput())
    }
    @Test fun breakpointAndWatchTakePrecedenceAtTheObservedStop() {
        val runner = boundedTestRunner()
        runner.saveBreakpoint(null, 2)
        runner.startBoundedExecution("test", emptyList(), 1000, 10000)
        assertEquals("breakpoint", finish(runner).reason); assertEquals(2, runner.cpuSnapshot().programCounter)
        val watch = boundedTestRunner("ORG 0\nLIDP 0xC100\nLIA 0x41\nSTD\nJP 0\n")
        watch.saveWatch(null, 0xc100, 0xc100, DebuggerWatchKind.CHANGE)
        watch.startBoundedExecution("test", emptyList(), 3, 10000)
        assertEquals("watch", finish(watch).reason); assertEquals(0x41, watch.memoryByte(0xc100))
    }
    @Test fun faultsAndManualInterruptionsRemainDistinctFromBudgetStops() {
        val fault = boundedTestRunner("ORG 0\nDB 0x3E\n")
        fault.startBoundedExecution("test", emptyList(), 1, 100)
        assertEquals("fault", finish(fault).reason); assertEquals(RunnerState.FAULTED, fault.state)
        for (action in listOf<(DesktopEmulatorRunner) -> Unit>({ it.pause() }, { it.reset() }, { it.setOperatingMode(OperatingMode.PROGRAM) },
            { it.pressHostKey(PocketKey.A, "user"); it.releaseHostKey(PocketKey.A, "user") })) {
            val runner = boundedTestRunner()
            runner.startBoundedExecution("test", listOf(PocketKey.NUM_1), 1000000, 5000000)
            runner.tick(); action(runner)
            assertEquals("interrupted", runner.boundedExecution("test")!!.reason)
            assertEquals(RunnerState.PAUSED, runner.state)
        }
    }
    @Test fun existingInputAndOutOfRangeBudgetsCannotStartANewRun() {
        val runner = boundedTestRunner()
        runner.enqueueKeySequence(listOf(PocketKey.A))
        assertFails { runner.startBoundedExecution("test", emptyList(), 1, 1) }
        assertEquals(RunnerState.PAUSED, runner.state)
        assertFails { boundedTestRunner().startBoundedExecution("test", emptyList(), 1000001, 1) }
    }
    @Test fun manualLoadInterruptsWithoutResumingAnUnlimitedRun() {
        val runner = boundedTestRunner()
        runner.startBoundedExecution("test", emptyList(), 1000000, 5000000); runner.tick()
        val image = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble("ORG 0xC100\nDB 0x12\n")).image
        assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
        assertEquals(RunnerState.PAUSED, runner.state); assertEquals("interrupted", runner.boundedExecution("test")!!.reason)
        val revision = runner.sessionRevision; runner.tick(); assertEquals(revision, runner.sessionRevision)
    }
}
