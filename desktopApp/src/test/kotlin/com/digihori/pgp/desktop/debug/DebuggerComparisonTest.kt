package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import kotlin.test.*

class DebuggerComparisonTest {
    @Test fun stepComparesImmediateBeforeAndAfterIncludingIndividualInternalRegisters() {
        val runner = debuggerTestRunner(byteArrayOf(0x02, 0x41, 0x03, 0x22)) // LIA 41; LIB 22
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        tracker.preparingStep(runner); runner.step()
        val first = tracker.stepped(runner)
        assertEquals(DebuggerComparisonBasis.STEP, first.cpuBasis)
        val rows = DebuggerRegisterModel.rows(runner.cpuSnapshot(), first.previousCpu).flatten()
        assertEquals("00", rows.single { it.name == "A" }.previous)
        assertEquals("41", rows.single { it.name == "A" }.value)
        assertTrue(rows.single { it.name == "A" }.changed)
        assertFalse(rows.single { it.name == "B" }.changed)
        tracker.preparingStep(runner); runner.step()
        val second = DebuggerRegisterModel.rows(runner.cpuSnapshot(), tracker.stepped(runner).previousCpu).flatten()
        assertFalse(second.single { it.name == "A" }.changed)
        assertTrue(second.single { it.name == "B" }.changed)
    }

    @Test fun memoryHighlightsNetChangeAndSameValueWriteIsNotADelta() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52, 0x52))
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        repeat(2) { tracker.preparingStep(runner); runner.step(); tracker.stepped(runner) }
        tracker.preparingStep(runner); runner.step()
        assertEquals(MemoryValueChange(0xc000, 0, 0x41), tracker.stepped(runner).memoryChanges[0xc000])
        tracker.preparingStep(runner); runner.step()
        assertTrue(tracker.stepped(runner).memoryChanges.isEmpty())
    }

    @Test fun continueComparesWithPreviousStopAndFurtherStopAdvancesBaseline() {
        val runner = debuggerTestRunner(byteArrayOf(0x02, 0x41, 0x03, 0x22, 0x02, 0x42))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        runner.toggleBreakpoint(4)
        assertEquals(DebuggerComparisonBasis.RUNNING, tracker.running(runner).cpuBasis)
        runner.run(); runner.tick()
        assertEquals(RunnerState.PAUSED, runner.state)
        val first = tracker.stopped(runner)
        assertEquals(DebuggerComparisonBasis.STOP, first.cpuBasis)
        assertEquals(0, first.previousCpu?.programCounter)
        runner.runToAddress(6); runner.tick()
        val second = tracker.stopped(runner)
        assertEquals(4, second.previousCpu?.programCounter)
        val cells = DebuggerRegisterModel.rows(runner.cpuSnapshot(), second.previousCpu).flatten()
        assertEquals("41", cells.single { it.name == "A" }.previous)
        assertEquals("42", cells.single { it.name == "A" }.value)
        assertFalse(cells.single { it.name == "B" }.changed)
    }

    @Test fun runThenPauseUsesStoppedBaselineAndObservationNeverChangesRunnerRevision() {
        val runner = debuggerTestRunner(byteArrayOf(0x02, 0x41))
        val tracker = DebuggerComparisonTracker(0xc000)
        val revision = runner.sessionRevision
        tracker.reset(runner); tracker.running(runner); tracker.selectMemory(runner, 0xc100)
        assertEquals(revision, runner.sessionRevision)
        assertEquals(RunnerState.PAUSED, runner.state)
        runner.run(); runner.pause()
        val frame = tracker.stopped(runner)
        assertEquals(DebuggerComparisonBasis.STOP, frame.cpuBasis)
        assertTrue(DebuggerRegisterModel.rows(runner.cpuSnapshot(), frame.previousCpu).flatten().none { it.changed })
    }

    @Test fun resetAndDifferentSessionClearBothBaselines() {
        val first = debuggerTestRunner(byteArrayOf(0x02, 0x41))
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(first); tracker.preparingStep(first); first.step(); tracker.stepped(first)
        first.reset()
        assertNull(tracker.reset(first).previousCpu)
        assertTrue(tracker.frame.memoryChanges.isEmpty())
        assertEquals(DebuggerComparisonBasis.NONE, tracker.frame.cpuBasis)
        val second = debuggerTestRunner(byteArrayOf(0x03, 0x22))
        assertEquals(DebuggerComparisonBasis.NONE, tracker.selectMemory(second, 0xc000).cpuBasis)
        tracker.preparingStep(second); second.step()
        val frame = tracker.stepped(second)
        assertEquals(0, frame.previousCpu?.programCounter)
        assertEquals("00", DebuggerRegisterModel.rows(second.cpuSnapshot(), frame.previousCpu).flatten().single { it.name == "A" }.value)
    }

    @Test fun pageSwitchDoesNotInventEarlierValuesAndRunningSwitchWaitsForNewBaseline() {
        val runner = debuggerTestRunner()
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        load(runner, 0xc100, 0x41)
        val selected = tracker.selectMemory(runner, 0xc100)
        assertEquals(DebuggerComparisonBasis.NONE, selected.memoryBasis)
        assertTrue(selected.memoryChanges.isEmpty())
        load(runner, 0xc100, 0x42)
        assertEquals(MemoryValueChange(0xc100, 0x41, 0x42), tracker.stopped(runner).memoryChanges[0xc100])
        tracker.running(runner); runner.run()
        tracker.selectMemory(runner, 0xc200)
        runner.pause()
        assertEquals(DebuggerComparisonBasis.NONE, tracker.stopped(runner).memoryBasis)
        assertTrue(tracker.frame.memoryChanges.isEmpty())
    }

    @Test fun memoryCaptureWrapsAtFFFF() {
        val runner = debuggerTestRunner()
        val tracker = DebuggerComparisonTracker(0xfff0)
        tracker.reset(runner)
        tracker.preparingStep(runner); runner.step()
        val frame = tracker.stepped(runner)
        assertEquals(0xfff0, frame.memoryStart)
        assertTrue(frame.memoryChanges.isEmpty())
    }

    @Test fun bankSwitchSkipsDifferentPhysicalRomOnAllThreeBankedMachines() {
        for ((id, banks, selector) in listOf(Triple("pc-1360", 8, 0x3400), Triple("pc-1460", 4, 0x3c00), Triple("pc-1470u", 8, 0x3400))) {
            val runner = bankRunner(id, banks)
            val tracker = DebuggerComparisonTracker(0x4000)
            tracker.reset(runner)
            load(runner, selector, 1)
            val revision = runner.sessionRevision
            val frame = tracker.stopped(runner)
            assertTrue(frame.memoryChanges.isEmpty(), id)
            assertEquals(256, frame.excludedMemoryBytes, id)
            assertEquals(revision, runner.sessionRevision)
            // Reading comparisons did not drain the bank event; the Runner still records it on Step.
            runner.step()
            assertEquals(1, runner.bankHistory().size, id)
            tracker.selectMemory(runner, 0xc000)
            load(runner, selector, 0)
            load(runner, 0xc000, 0x41)
            assertEquals(MemoryValueChange(0xc000, 0, 0x41), tracker.stopped(runner).memoryChanges[0xc000], id)
        }
    }

    @Test fun hostLoadCanRebaseWithoutChangingExecutionAndFaultIsAStop() {
        val runner = debuggerTestRunner(byteArrayOf(0x3e))
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        load(runner, 0xc000, 0x41)
        assertTrue(tracker.reset(runner).memoryChanges.isEmpty())
        val revision = runner.sessionRevision
        tracker.preparingStep(runner)
        assertEquals(revision, runner.sessionRevision)
        runner.step()
        val frame = tracker.stepped(runner)
        assertEquals(RunnerState.FAULTED, runner.state)
        assertEquals(DebuggerComparisonBasis.STEP, frame.cpuBasis)
        assertEquals(0, frame.previousCpu?.programCounter)
    }

    @Test fun snapshotComparisonDoesNotConsumeOrCreateCpuWatchAndTraceEvents() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x57))
        runner.setDebugObservationEnabled(true)
        runner.setInstructionTraceEnabled(true)
        runner.setMemoryAccessWatch(0xc000, 0xc000, reads = true, writes = false)
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        repeat(2) { tracker.preparingStep(runner); runner.step(); tracker.stepped(runner) }
        assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
        val history = runner.memoryAccessHistory()
        val trace = runner.instructionTrace()
        assertEquals(5, history.size)
        assertEquals(2, trace.size)
        tracker.stopped(runner)
        assertEquals(history, runner.memoryAccessHistory())
        assertEquals(trace, runner.instructionTrace())
        tracker.preparingStep(runner); runner.step(); tracker.stepped(runner)
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        assertEquals(7, runner.memoryAccessHistory().size)
    }
}

internal fun bankRunner(id: String, count: Int, program: ByteArray = byteArrayOf()): DesktopEmulatorRunner {
    val machineId = MachineId(id)
    val components = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000).also { program.copyInto(it) })) +
        List(count) { bank -> RomComponent(RomComponentId("bank-$bank"), RomRole.EXTERNAL, ByteArray(0x4000) { bank.toByte() }) }
    return DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machineId, RomSet(machineId, components))).session)
}

private fun load(runner: DesktopEmulatorRunner, address: Int, value: Int) {
    val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
        listOf(AddressedMemorySegmentData(address, byteArrayOf(value.toByte()))))).image
    assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
}
