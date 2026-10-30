package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import kotlin.test.*

class DebuggerLogpointTest {
    private fun success(result: DebuggerLogpointResult) = assertIs<DebuggerLogpointResult.Success>(result).id
    private fun loop() = debuggerTestRunner(byteArrayOf(2, 0, 0x42, 0x79, 0, 2)).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }

    @Test fun conditionalLogsDoNotStopAndBreakpointAtSamePcDoesNotLogTwice() {
        val runner = loop()
        success(runner.saveLogpoint(null, 2, "A", "A >= 2", true))
        assertIs<DebuggerRegistrationResult.Success>(runner.saveBreakpoint(null, 3, condition = "A == 4"))
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(3), runner.stopReason)
        assertEquals(listOf(2, 3), runner.logpointEntries().map { it.value })
        assertEquals(listOf(2, 2), runner.logpointEntries().map { it.address })
        val same = loop()
        success(same.saveLogpoint(null, 2, "A", "", true))
        assertIs<DebuggerRegistrationResult.Success>(same.saveBreakpoint(null, 2))
        same.run(); same.tick()
        assertTrue(same.logpointEntries().isEmpty())
        same.run(); same.tick()
        assertEquals(listOf(0), same.logpointEntries().map { it.value })
        assertEquals(DebuggerStopReason.Breakpoint(2), same.stopReason)
    }

    @Test fun manualStepsRecordBeforeExecutionAndDoNotCreateCpuAccessEvents() {
        val runner = loop()
        runner.setDebugObservationEnabled(true)
        success(runner.saveLogpoint(null, 0, "MEM[0xC000]", "MEM[0xC000] == 0", true))
        val pc = runner.cpuSnapshot().programCounter
        val revision = runner.sessionRevision
        assertTrue(runner.logpointEntries().isEmpty())
        assertEquals(pc, runner.cpuSnapshot().programCounter)
        assertEquals(revision, runner.sessionRevision)
        runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        val entry = runner.logpointEntries().single()
        assertEquals(0, entry.value)
        assertEquals(runner.resolveRomLocation(0), entry.rom)
        assertTrue(runner.memoryAccessHistory().none { it.access.address == 0xc000 })
        assertTrue(runner.instructionTrace().isEmpty())
    }

    @Test fun resetAndSuccessfulLoadClearHistoryButKeepDefinitionsAndFailedLoadKeepsHistory() {
        val runner = loop()
        val id = success(runner.saveLogpoint(null, 0, "PC", "", true))
        runner.step()
        val history = runner.logpointEntries()
        fun image(address: Int) = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            listOf(AddressedMemorySegmentData(address, byteArrayOf(7))))).image
        assertIs<MemoryImageLoadResult.Failure>(runner.loadMemoryImage(image(0)))
        assertEquals(history, runner.logpointEntries())
        assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image(0xc000)))
        assertTrue(runner.logpointEntries().isEmpty())
        assertEquals(id, runner.logpointRegistrations().single().id)
        runner.reset(); runner.step()
        assertEquals(0L, runner.logpointEntries().single().sequence)
        runner.clearLogpointHistory()
        assertTrue(runner.logpointEntries().isEmpty())
        assertEquals(1, runner.logpointRegistrations().size)
        success(runner.saveLogpoint(id, 0, "PC", "", false))
        runner.reset(); runner.step()
        assertTrue(runner.logpointEntries().isEmpty())
        runner.run()
        assertEquals(DebuggerLogpointResult.Failure(DebuggerLogpointError.PAUSED_ONLY), runner.removeLogpoint(id))
    }

    @Test fun registrationValidationIsAtomicAndHistoryIsBoundedWithNoReadOnMiss() {
        val store = DebuggerLogpoints { it in 0xc000..0xc7ff }
        val id = success(store.save(null, 2, "A", "", true))
        val before = store.registrations()
        fun failure(error: DebuggerLogpointError, result: DebuggerLogpointResult) {
            assertEquals(DebuggerLogpointResult.Failure(error), result)
            assertEquals(before, store.registrations())
        }
        failure(DebuggerLogpointError.DUPLICATE, store.save(null, 2, "A", "", true))
        failure(DebuggerLogpointError.VALUE, store.save(id, 2, "A + 1", "", true))
        failure(DebuggerLogpointError.CONDITION, store.save(id, 2, "A", "A = 0", true))
        failure(DebuggerLogpointError.MEMORY, store.save(id, 2, "MEM[0x8000]", "", true))
        failure(DebuggerLogpointError.MEMORY, store.save(id, 2, "A", "MEM[0x8000] == 0", true))
        failure(DebuggerLogpointError.ADDRESS, store.save(id, 65536, "A", "", true))
        store.observe(3, { error("CPU read on nonmatching PC") }, { error("memory read on miss") }, { error("ROM read on miss") })
        val cpu = loop().cpuSnapshot()
        repeat(300) { store.observe(2, { cpu }, { error("unexpected memory read") }, { null }) }
        assertEquals(256, store.entries().size)
        assertEquals(44L, store.dropped)
        assertEquals(44L, store.entries().first().sequence)
        repeat(31) { success(store.save(null, 100 + it, "PC", "", false)) }
        assertEquals(DebuggerLogpointResult.Failure(DebuggerLogpointError.LIMIT), store.save(null, 200, "PC", "", false))
        store.clearHistory()
        assertEquals(0L, store.dropped)
        success(store.remove(id))
        assertFalse(store.hasEnabled)
    }

    @Test fun falseConditionAvoidsValueReadAndObservationFailureIsRecordedWithoutThrowing() {
        val store = DebuggerLogpoints { true }
        val id = success(store.save(null, 0, "MEM[0xC000]", "A == 7", true))
        val cpu = loop().cpuSnapshot()
        store.observe(0, { cpu }, { error("unexpected memory read") }, { null })
        assertTrue(store.entries().isEmpty())
        success(store.save(id, 0, "MEM[0xC000]", "", true))
        store.observe(0, { cpu }, { throw IllegalStateException("read failed") }, { null })
        assertTrue(store.entries().single().failed)
        assertNull(store.entries().single().value)
    }

    @Test fun enabledLoggingKeepsRunningAndFaultAndWatchStillWin() {
        val runner = loop()
        success(runner.saveLogpoint(null, 2, "A", "", true))
        runner.run(); runner.tick()
        assertEquals(RunnerState.RUNNING, runner.state)
        assertNull(runner.stopReason)
        assertEquals(256, runner.logpointEntries().size)
        assertTrue(runner.logpointDropped() > 0)
        val fault = debuggerTestRunner(byteArrayOf(0x3e)).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
        success(fault.saveLogpoint(null, 0, "PC", "", true))
        fault.run(); fault.tick()
        assertIs<DebuggerStopReason.Fault>(fault.stopReason)
        assertEquals(1, fault.logpointEntries().size)
        val watch = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 2, 7, 0x52))
        success(watch.saveLogpoint(null, 5, "A", "", true))
        assertIs<DebuggerRegistrationResult.Success>(watch.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE))
        repeat(3) { watch.step() }
        assertIs<DebuggerStopReason.MemoryAccessed>(watch.stopReason)
        assertEquals(7, watch.logpointEntries().single().value)
    }

    @Test fun disabledLogpointsKeepBatchExecutionAndEnabledLogpointsObserveInstructions() {
        val id = MachineId("pc-1245")
        val bytes = ByteArray(0x2000).apply { byteArrayOf(2, 0, 0x42, 0x79, 0, 2).copyInto(this) }
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
            RomComponent(RomComponentId("internal"), RomRole.INTERNAL, bytes),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
        )))).session
        var batches = 0
        var steps = 0
        val counted = object : EmulatorSession by session {
            override fun runCycles(cycleBudget: Long): RunResult { batches++; return session.runCycles(cycleBudget) }
            override fun step(): StepResult { steps++; return session.step() }
        }
        val runner = DesktopEmulatorRunner(counted).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
        val point = success(runner.saveLogpoint(null, 2, "A", "", false))
        runner.run(); runner.tick(); runner.pause()
        assertTrue(batches > 0)
        assertEquals(0, steps)
        val previousBatches = batches
        success(runner.saveLogpoint(point, 2, "A", "", true))
        runner.run(); runner.tick(); runner.pause()
        assertEquals(previousBatches, batches)
        assertTrue(steps > 0)
        assertTrue(runner.logpointEntries().isNotEmpty())
    }

    @Test fun physicalRomBankIsCapturedWithoutChangingSelection() {
        val id = MachineId("pc-1360")
        val internal = ByteArray(0x2000).apply { byteArrayOf(0x79, 0x40, 0).copyInto(this) }
        val parts = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal)) +
            List(8) { RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000)) }
        val runner = DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, parts))).session)
        success(runner.saveLogpoint(null, 0x4000, "PC", "", true))
        runner.step()
        val bank = runner.selectedRomBank()
        runner.step()
        assertEquals(0x4000, runner.logpointEntries().single().value)
        assertEquals(bank, runner.logpointEntries().single().rom?.bank)
        assertNotNull(runner.logpointEntries().single().rom)
        assertEquals(bank, runner.selectedRomBank())
    }
}
