package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import kotlin.test.*

class DebuggerRegistrationsTest {
    @Test fun breakpointCrudRejectsDuplicatesAtomicallyAndIdsRemainStable() {
        val runner = debuggerTestRunner()
        val id = success(runner.saveBreakpoint(null, 0x1234))
        val other = success(runner.saveBreakpoint(null, 0x2345, false))
        val revision = runner.sessionRevision
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.DUPLICATE_ADDRESS), runner.saveBreakpoint(id, 0x2345))
        assertEquals(revision, runner.sessionRevision)
        assertEquals(0x1234, runner.breakpointRegistrations().first { it.id == id }.address)
        success(runner.setBreakpointEnabled(id, false))
        assertTrue(runner.breakpoints().isEmpty())
        assertTrue(runner.toggleBreakpoint(0x1234))
        assertEquals(id, runner.breakpointRegistrations().first { it.address == 0x1234 }.id)
        assertEquals(setOf(0x1234), runner.breakpoints())
        success(runner.saveBreakpoint(id, 0x3456, true))
        success(runner.removeBreakpoint(other))
        assertEquals(setOf(0x3456), runner.breakpoints())
        assertEquals(RunnerState.PAUSED, runner.state)
        assertEquals(0, runner.cpuSnapshot().programCounter)
    }

    @Test fun disabledBreakpointDoesNotStopAndEnabledOneResumesPastCurrentPc() {
        val runner = debuggerTestRunner(byteArrayOf(0x02, 0x41, 0x03, 0x42, 0x79, 0, 0))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        val id = success(runner.saveBreakpoint(null, 2, false))
        runner.runToAddress(4); runner.tick()
        assertEquals(DebuggerStopReason.RunToAddress(4), runner.stopReason)
        success(runner.setBreakpointEnabled(id, true))
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(2), runner.stopReason)
        runner.runToAddress(4); runner.tick()
        assertEquals(DebuggerStopReason.RunToAddress(4), runner.stopReason)
        assertEquals(2, runner.breakpointRegistrations().single().address)
    }

    @Test fun multipleChangeWatchesStopIndependentlyAndSupportDisableAndModeEdit() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52,
            0x10, 0xc0.toByte(), 0x10, 0x02, 0x42, 0x52, 0x57, 0x79, 0, 0))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        val first = success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE))
        val second = success(runner.saveWatch(null, 0xc010, 0xc010, DebuggerWatchKind.CHANGE))
        runner.run(); runner.tick()
        assertEquals(0xc000, assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason).changes.single().address)
        success(runner.setWatchEnabled(first, false))
        runner.run(); runner.tick()
        assertEquals(0xc010, assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason).changes.single().address)
        success(runner.saveWatch(second, 0xc010, 0xc010, DebuggerWatchKind.READ))
        runner.run(); runner.tick()
        assertEquals(MemoryAccessKind.READ, assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason).accesses.single().kind)
        assertEquals(13, runner.cpuSnapshot().programCounter)
        success(runner.removeWatch(second))
        assertFalse(runner.watchRegistrations().single().enabled)
    }

    @Test fun overlappingRulesDoNotDuplicateEventsAndAccessStopAdvancesChangeBaselines() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52, 0x57))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        runner.setDebugObservationEnabled(true)
        runner.setInstructionTraceEnabled(true)
        success(runner.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.CHANGE))
        success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE))
        val write = success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE))
        val both = success(runner.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.READ_WRITE))
        runner.run(); runner.tick()
        assertEquals(1, assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason).accesses.size)
        assertEquals(1, runner.memoryAccessHistory().count { it.access.kind == MemoryAccessKind.WRITE })
        success(runner.setWatchEnabled(write, false)); success(runner.setWatchEnabled(both, false))
        runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        assertEquals(4, runner.instructionTrace().size)
        assertEquals(2, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
    }

    @Test fun enabledChangeBudgetAndObservationFailureLeaveRegistrationUnchanged() {
        val runner = debuggerTestRunner()
        val first = success(runner.saveWatch(null, 0xc000, 0xcfff, DebuggerWatchKind.CHANGE, false))
        success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE))
        val entries = runner.watchRegistrations()
        val revision = runner.sessionRevision
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.WATCH_BYTES_LIMIT), runner.setWatchEnabled(first, true))
        assertEquals(entries, runner.watchRegistrations())
        assertEquals(revision, runner.sessionRevision)
        val store = DebuggerRegistrationStore { _, _ -> error("unavailable") }
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.OBSERVATION_FAILED), store.saveWatch(null, 0, 0, DebuggerWatchKind.CHANGE, true))
        assertTrue(store.watches().isEmpty())
        assertEquals(0L, success(store.saveWatch(null, 0, 0, DebuggerWatchKind.CHANGE, false)))
    }

    @Test fun overlappingChangeRangesYieldOneChangePerAddressAndDisabledBaselineIsFreshOnEnable() {
        val memory = ByteArray(0x10000)
        val store = DebuggerRegistrationStore { start, length -> memory.copyOfRange(start, start + length) }
        val first = success(store.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.CHANGE, true))
        success(store.saveWatch(null, 0xc001, 0xc002, DebuggerWatchKind.CHANGE, true))
        memory[0xc000] = 0x41; memory[0xc001] = 0x42; memory[0xc002] = 0x43
        assertEquals(listOf(0xc000, 0xc001, 0xc002), store.changes().map { it.address })
        assertTrue(store.changes().isEmpty())
        success(store.saveWatch(first, 0xc000, 0xc001, DebuggerWatchKind.CHANGE, false))
        memory[0xc000] = 0x44
        success(store.saveWatch(first, 0xc000, 0xc001, DebuggerWatchKind.CHANGE, true))
        assertTrue(store.changes().isEmpty())
        val accesses = listOf(MemoryAccess(MemoryAccessKind.WRITE, 0xc000, 0x44), MemoryAccess(MemoryAccessKind.WRITE, 0xc000, 0x44))
        success(store.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE, true))
        success(store.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.READ_WRITE, true))
        assertEquals(accesses, store.accesses(accesses))
    }

    @Test fun resetPreservesDefinitionsAndRebasesEveryChangeWatchWhileNewSessionIsEmpty() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52))
        success(runner.saveBreakpoint(null, 5, false))
        success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE))
        success(runner.saveWatch(null, 0xc010, 0xc010, DebuggerWatchKind.READ, false))
        repeat(3) { runner.step() }
        assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason)
        val points = runner.breakpointRegistrations(); val watches = runner.watchRegistrations()
        runner.reset(); runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        assertEquals(points, runner.breakpointRegistrations()); assertEquals(watches, runner.watchRegistrations())
        assertTrue(debuggerTestRunner().watchRegistrations().isEmpty())
    }

    @Test fun legacySettersReplaceOnlyTheirCategoryAndClearLeavesOthersIntact() {
        val runner = debuggerTestRunner()
        success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE))
        success(runner.saveWatch(null, 0xc001, 0xc001, DebuggerWatchKind.CHANGE))
        success(runner.saveWatch(null, 0xc002, 0xc002, DebuggerWatchKind.READ))
        runner.setMemoryWatch(0xc010, 0xc011)
        assertEquals(2, runner.watchRegistrations().size)
        assertEquals(0xc010..0xc011, runner.memoryWatch())
        runner.setMemoryAccessWatch(0xc020, 0xc021, reads = false, writes = true)
        assertEquals(2, runner.watchRegistrations().size)
        runner.clearMemoryWatch()
        assertEquals(DebuggerWatchKind.WRITE, runner.watchRegistrations().single().kind)
        runner.clearMemoryAccessWatch(); assertTrue(runner.watchRegistrations().isEmpty())
    }

    @Test fun limitsAndInvalidEditsAreTypedAndAtomic() {
        val runner = debuggerTestRunner()
        repeat(DebuggerRegistrationStore.MAX_BREAKPOINTS) { success(runner.saveBreakpoint(null, it, false)) }
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.BREAKPOINT_LIMIT), runner.saveBreakpoint(null, 0x1234))
        repeat(DebuggerRegistrationStore.MAX_WATCHES) { success(runner.saveWatch(null, it, it, DebuggerWatchKind.READ, false)) }
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.WATCH_LIMIT), runner.saveWatch(null, 0, 0, DebuggerWatchKind.WRITE))
        val revision = runner.sessionRevision
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.ADDRESS), runner.saveBreakpoint(null, 0x10000))
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.RANGE), runner.saveWatch(null, 2, 1, DebuggerWatchKind.READ))
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.NOT_FOUND), runner.removeWatch(9999))
        assertEquals(revision, runner.sessionRevision)
    }

    @Test fun faultWinsOverAccessWatchAndMcpKeepsExistingStopSchema() {
        val runner = debuggerTestRunner(byteArrayOf(0x3e))
        success(runner.saveWatch(null, 0, 0, DebuggerWatchKind.READ))
        success(runner.saveWatch(null, 0, 1, DebuggerWatchKind.READ_WRITE))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED); runner.run(); runner.tick()
        assertEquals(RunnerState.FAULTED, runner.state)
        assertIs<DebuggerStopReason.Fault>(runner.stopReason)
        val service = DesktopDebugContextService(runner, "registration-test", { "2026-10-14T00:00:00Z" })
        val context = assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context
        assertEquals(1, DebugContextV1.SCHEMA_VERSION)
        assertEquals("fault", context.stop?.kind)
        val before = DebugContextJsonWriter.write(context)
        runner.watchRegistrations(); runner.breakpointRegistrations()
        assertContentEquals(before, DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context))
    }
}

private fun success(result: DebuggerRegistrationResult) = assertIs<DebuggerRegistrationResult.Success>(result).id
