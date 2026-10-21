package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.runner.RunnerState
import kotlin.test.*

class DebuggerBreakpointConditionTest {
    @Test fun conditionsAreBoundedUnsignedComparisonsWithNoExecutableExpressions() {
        val runner = debuggerTestRunner(byteArrayOf(2, 0x41))
        runner.step()
        val cpu = runner.cpuSnapshot()
        for (text in listOf("A == 65", "a == 0x41", "A == \$41", "A != 64", "A > 64", "A >= 65", "A < 66", "A <= 65", "PC == 2", "MEM[0xC000] == 255")) {
            assertTrue(assertNotNull(DebuggerBreakpointCondition.parse(text), text).matches({ cpu }, { 255 }), text)
        }
        for (text in listOf("A == 0", "A != 65", "A >= 66", "A > 65", "A < 65", "A <= 64", "MEM[49152] != 255")) {
            assertFalse(assertNotNull(DebuggerBreakpointCondition.parse(text)).matches({ cpu }, { 255 }), text)
        }
        for (text in listOf("A = 1", "A == -1", "A == 256", "PC == 65536", "C == 2", "MEM[65536] == 0", "MEM[0] == 256", "foo == 1", "A == 1 && B == 2", "run()", "A == 0; exit()", "A == 0xFFFFFFFF", "A == " + "0".repeat(129))) {
            assertNull(DebuggerBreakpointCondition.parse(text), text)
        }
        for (register in listOf("PC", "DP", "P", "Q", "R", "D", "C", "Z", "I", "J", "A", "B", "XL", "XH", "YL", "YH", "K", "L", "M", "N")) {
            assertTrue(assertNotNull(DebuggerBreakpointCondition.parse("$register >= 0")).matches({ cpu }, { error("unexpected memory read") }))
        }
        assertTrue(assertNotNull(DebuggerBreakpointCondition.parse("MEM[\$C000] == 7")).matches({ error("unexpected CPU snapshot") }, { 7 }))
    }

    @Test fun invalidEditsAreAtomicAndTogglesPreserveRulesAndCount() {
        val store = DebuggerRegistrationStore { _, _ -> byteArrayOf(7) }
        val runner = debuggerTestRunner()
        val id = success(store.saveBreakpoint(null, 0, true, "MEM[0xC000] == 7", ">= 2"))
        assertFalse(store.breakpointHit(0, runner::cpuSnapshot))
        assertTrue(store.breakpointHit(0, runner::cpuSnapshot))
        val before = store.breakpoints()
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.CONDITION), store.saveBreakpoint(id, 1, true, "A = 1"))
        assertEquals(before, store.breakpoints())
        for (text in listOf("0", "== 0", "% 0", ">= -1", "== 1000000001", "== 0x3", "< 3", "== 99999999999999999999999999")) {
            assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.HIT_CONDITION), store.saveBreakpoint(id, 0, true, hitCondition = text), text)
            assertEquals(before, store.breakpoints())
        }
        success(store.saveBreakpoint(id, 0, false))
        assertFalse(store.breakpointHit(0, runner::cpuSnapshot))
        success(store.saveBreakpoint(id, 0, true))
        assertEquals(before, store.breakpoints())
        success(store.saveBreakpoint(id, 0, true, hitCondition = "% 3"))
        assertEquals(0L, store.breakpoints().single().hitCount)
        assertFalse(store.breakpointHit(0, runner::cpuSnapshot))
        assertFalse(store.breakpointHit(0, runner::cpuSnapshot))
        assertTrue(store.breakpointHit(0, runner::cpuSnapshot))
        store.resetHitCounts()
        assertEquals(0L, store.breakpoints().single().hitCount)
        success(store.saveBreakpoint(id, 1, true, "", ""))
        assertEquals("", store.breakpoints().single().condition)
        assertTrue(store.breakpointHit(1, runner::cpuSnapshot))
    }

    @Test fun falseValueConditionsStillCountButHitFilterDoesNotReadSnapshotsOrMemory() {
        var reads = 0
        val store = DebuggerRegistrationStore { _, _ -> reads++; byteArrayOf(0) }
        success(store.saveBreakpoint(null, 0, true, "MEM[0] == 1", ">= 3"))
        repeat(2) { assertFalse(store.breakpointHit(0) { error("unexpected CPU read") }) }
        assertEquals(0, reads)
        assertFalse(store.breakpointHit(0) { error("unexpected CPU read") })
        assertEquals(1, reads)
        assertEquals(3L, store.breakpoints().single().hitCount)
    }

    // LIA 0; loop: INCA; JP loop. The breakpoint observes A before INCA.
    private fun loop() = debuggerTestRunner(byteArrayOf(2, 0, 0x42, 0x79, 0, 2)).apply {
        setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
    }

    @Test fun realRunnerStopsBeforeConditionalInstructionAndResumesWithoutDoubleCount() {
        val runner = loop()
        success(runner.saveBreakpoint(null, 2, condition = "A == 3"))
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(2), runner.stopReason)
        assertEquals(3, runner.cpuSnapshot().copyInternalRam()[2].toInt())
        assertEquals(4L, runner.breakpointRegistrations().single().hitCount)
        runner.runToAddress(3); runner.tick()
        assertEquals(DebuggerStopReason.RunToAddress(3), runner.stopReason)
        assertEquals(4, runner.cpuSnapshot().copyInternalRam()[2].toInt())
        assertEquals(4L, runner.breakpointRegistrations().single().hitCount)
        runner.reset()
        assertEquals(0L, runner.breakpointRegistrations().single().hitCount)
        assertEquals("A == 3", runner.breakpointRegistrations().single().condition)
    }

    @Test fun hitModesAndManualStepRunToAndFaultKeepExistingPriorities() {
        for ((rule, nextCount) in listOf("% 3" to 6L, ">= 3" to 4L)) {
            val runner = loop()
            success(runner.saveBreakpoint(null, 2, hitCondition = rule))
            runner.run(); runner.tick()
            assertEquals(3L, runner.breakpointRegistrations().single().hitCount)
            assertEquals(2, runner.cpuSnapshot().copyInternalRam()[2].toInt())
            runner.run(); runner.tick()
            assertEquals(nextCount, runner.breakpointRegistrations().single().hitCount)
            assertEquals(DebuggerStopReason.Breakpoint(2), runner.stopReason)
        }
        val exact = loop()
        success(exact.saveBreakpoint(null, 2, hitCondition = "== 3"))
        exact.step(); exact.step()
        assertEquals(0L, exact.breakpointRegistrations().single().hitCount)
        exact.run(); exact.tick()
        assertEquals(3L, exact.breakpointRegistrations().single().hitCount)
        exact.runToAddress(3); exact.tick()
        exact.run(); exact.tick()
        assertEquals(RunnerState.RUNNING, exact.state)
        assertTrue(exact.breakpointRegistrations().single().hitCount > 3)
        exact.pause()
        val priority = loop()
        success(priority.saveBreakpoint(null, 2, hitCondition = "== 1"))
        priority.runToAddress(2); priority.tick()
        assertEquals(DebuggerStopReason.RunToAddress(2), priority.stopReason)
        assertEquals(0L, priority.breakpointRegistrations().single().hitCount)
        val fault = debuggerTestRunner(byteArrayOf(0x3e)).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
        success(fault.saveBreakpoint(null, 0, condition = "PC != 0"))
        fault.run(); fault.tick()
        assertIs<DebuggerStopReason.Fault>(fault.stopReason)
    }

    @Test fun successfulLoadResetsCountsButFailedLoadPreservesThemAndReadOnlyAccessDoesNotCount() {
        val runner = loop()
        success(runner.saveBreakpoint(null, 2, hitCondition = ">= 2"))
        runner.run(); runner.tick()
        val before = runner.breakpointRegistrations()
        repeat(3) { assertEquals(before, runner.breakpointRegistrations()) }
        fun image(address: Int) = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            listOf(AddressedMemorySegmentData(address, byteArrayOf(7))))).image
        assertIs<MemoryImageLoadResult.Failure>(runner.loadMemoryImage(image(0)))
        assertEquals(before, runner.breakpointRegistrations())
        assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image(0xc500)))
        assertEquals(0L, runner.breakpointRegistrations().single().hitCount)
        assertEquals(">= 2", runner.breakpointRegistrations().single().hitCondition)
    }

    @Test fun conditionMemoryReadsDoNotBecomeCpuAccessesAndFalseConditionsAllowWatchStops() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 2, 7, 0x52, 0x79, 0, 5)).apply {
            setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
            setDebugObservationEnabled(true)
        }
        success(runner.saveBreakpoint(null, 5, condition = "MEM[0xC000] == 7"))
        success(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE))
        runner.run(); runner.tick()
        assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
        assertEquals(1L, runner.breakpointRegistrations().single().hitCount)
        assertEquals(1, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(5), runner.stopReason)
        assertEquals(2L, runner.breakpointRegistrations().single().hitCount)
        assertEquals(1, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
    }

    private fun success(result: DebuggerRegistrationResult) = assertIs<DebuggerRegistrationResult.Success>(result).id
}
