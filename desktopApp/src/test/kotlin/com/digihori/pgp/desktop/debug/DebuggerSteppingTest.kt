package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.runner.*
import com.digihori.pgp.desktop.application.debug.*
import kotlin.test.*

class DebuggerSteppingTest {
    private val nested = """
        ORG 0
        CALL FIRST
        LIA 9
        JP 3
        FIRST: CALL SECOND
        LIA 7
        RTN
        SECOND: LIA 3
        RTN
    """.trimIndent()

    @Test fun overExecutesNestedCallsAndCalButOrdinaryInstructionOnlyStepsOnce() {
        for (instruction in listOf("CALL", "CAL")) {
            val runner = runner(nested.replace("CALL", instruction))
            runner.setInstructionTraceEnabled(true)
            assertTrue(runner.stepOver()); finish(runner)
            assertEquals(if (instruction == "CALL") 3 else 2, runner.cpuSnapshot().programCounter)
            assertEquals(7, accumulator(runner))
            assertEquals(DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
            assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
            assertFalse(runner.canStepOut)
            assertEquals(6, runner.instructionTrace().size)
            assertTrue(runner.stepOver()); finish(runner)
            assertEquals(9, accumulator(runner))
        }
    }

    @Test fun outUsesObservedFrameAndIgnoresNestedReturnWithSameLogicalAddress() {
        val runner = runner(nested)
        assertFalse(runner.canStepOut)
        val before = runner.cpuSnapshot().programCounter
        assertFalse(runner.stepOut())
        assertEquals(before, runner.cpuSnapshot().programCounter)
        assertEquals(DebuggerStepStatus.NO_FRAME, runner.debugStepStatus)
        runner.step()
        assertTrue(runner.canStepOut)
        runner.step() // The inner CALL.
        assertTrue(runner.stepOut()); finish(runner)
        assertEquals(3, accumulator(runner))
        assertEquals(DebuggerStepStatus.OUT_COMPLETE, runner.debugStepStatus)
        assertTrue(runner.canStepOut) // FIRST is still active.
        assertTrue(runner.stepOut()); finish(runner)
        assertEquals(3, runner.cpuSnapshot().programCounter)
        assertEquals(7, accumulator(runner))
        assertFalse(runner.canStepOut)

        val recursive = runner("""
            ORG 0
            LIA 3
            CALL RECURSE
            JP 5
            RECURSE: CPIA 0
            JRZP BASE
            DECA
            CALL RECURSE
            BASE: RTN
        """.trimIndent())
        recursive.step()
        assertTrue(recursive.stepOver()); finish(recursive)
        assertEquals(5, recursive.cpuSnapshot().programCounter)
        assertEquals(0, accumulator(recursive))
        assertEquals(DebuggerStepStatus.OVER_COMPLETE, recursive.debugStepStatus)
        assertFalse(recursive.canStepOut)
    }

    @Test fun breakpointWatchFaultAndManualPauseInterruptWithoutResumingOldGoal() {
        val breakpoint = runner(nested)
        val first = assemble(nested).symbols.first { it.name == "FIRST" }.address
        breakpoint.saveBreakpoint(null, first)
        breakpoint.stepOver(); finish(breakpoint)
        assertEquals(DebuggerStopReason.Breakpoint(first), breakpoint.stopReason)
        assertEquals(DebuggerStepStatus.INTERRUPTED, breakpoint.debugStepStatus)
        assertTrue(breakpoint.canStepOut)
        breakpoint.runToAddress(first + 3); finish(breakpoint)
        assertEquals(DebuggerStopReason.RunToAddress(first + 3), breakpoint.stopReason)
        assertEquals(DebuggerStepStatus.IDLE, breakpoint.debugStepStatus)

        val watch = runner("ORG 0\nCALL SUB\nJP 3\nSUB: LIDP 0xC000\nLIA 7\nSTD\nRTN")
        watch.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE)
        watch.stepOver(); finish(watch)
        assertIs<DebuggerStopReason.MemoryAccessed>(watch.stopReason)
        assertEquals(DebuggerStepStatus.INTERRUPTED, watch.debugStepStatus)
        assertTrue(watch.canStepOut)
        watch.stepOut(); finish(watch)
        assertEquals(3, watch.cpuSnapshot().programCounter)

        val fault = runner("ORG 0\nCALL SUB\nRTN\nSUB: DB 0x3E")
        fault.stepOver(); finish(fault, faulted = true)
        assertIs<DebuggerStopReason.Fault>(fault.stopReason)
        assertEquals(DebuggerStepStatus.INTERRUPTED, fault.debugStepStatus)
        assertFalse(fault.stepOver()); assertFalse(fault.stepOut())

        val cancelled = runner(nested)
        cancelled.stepOver(); cancelled.pause()
        assertEquals(DebuggerStepStatus.INTERRUPTED, cancelled.debugStepStatus)
        assertEquals(0, cancelled.cpuSnapshot().programCounter)
        cancelled.runToAddress(first); finish(cancelled)
        assertEquals(DebuggerStopReason.RunToAddress(first), cancelled.stopReason)
    }

    @Test fun boundedExecutionSpansTicksAndStopsOnInstructionOrCycleLimit() {
        val session = session("ORG 0\nCALL SUB\nJP 3\nSUB: JP SUB")
        val limited = DesktopEmulatorRunner(session, MonotonicClock { 0L }, debugStepInstructionLimit = 10).apply {
            setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        }
        limited.stepOver(); finish(limited)
        assertEquals(DebuggerStepStatus.LIMIT, limited.debugStepStatus)
        assertEquals(DebuggerStopReason.UserPause, limited.stopReason)
        assertTrue(limited.canStepOut)
        val cycles = DesktopEmulatorRunner(session("ORG 0\nCALL SUB\nJP 3\nSUB: WAIT 255\nRTN"),
            MonotonicClock { 0L }, debugStepCycleLimit = 20).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
        cycles.stepOver(); finish(cycles)
        assertEquals(DebuggerStepStatus.LIMIT, cycles.debugStepStatus)
        val multi = DesktopEmulatorRunner(session("ORG 0\nCALL SUB\nJP 3\nSUB: JP SUB"),
            MonotonicClock { 0L }, debugStepInstructionLimit = 2_000).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
        multi.stepOver(); multi.tick()
        assertEquals(RunnerState.RUNNING, multi.state)
        finish(multi)
        assertEquals(DebuggerStepStatus.LIMIT, multi.debugStepStatus)
    }

    @Test fun stackDamageUnsupportedCaseAndUnobservedExecutionNeverPretendToComplete() {
        for (source in listOf(
            "ORG 0\nCALL SUB\nJP 3\nSUB: LIP 0x5E\nLIA 0\nEXAM\nRTN",
            "ORG 0\nCALL SUB\nJP 3\nSUB: PUSH\nRTN",
        )) {
            val runner = runner(source)
            runner.stepOver(); finish(runner)
            assertEquals(DebuggerStepStatus.FRAME_LOST, runner.debugStepStatus)
            assertFalse(runner.canStepOut)
        }
        val cases = runner("ORG 0\nCALL SUB\nJP 3\nSUB: CASE1 0x000003\nCASE2")
        cases.stepOver(); finish(cases)
        assertEquals(DebuggerStepStatus.UNSUPPORTED_FLOW, cases.debugStepStatus)
        assertEquals(6, cases.cpuSnapshot().programCounter)
        val normal = runner(nested)
        normal.step()
        assertTrue(normal.canStepOut)
        normal.run(); normal.tick(); normal.pause() // Fast run loses observation continuity.
        assertFalse(normal.canStepOut)
        normal.reset()
        assertFalse(normal.canStepOut)
    }

    @Test fun balancedLocalPushPopWorksAndLoadInvalidatesObservedFrames() {
        val runner = runner("ORG 0\nCALL SUB\nJP 3\nSUB: LIA 7\nPUSH\nLIA 9\nPOP\nRTN")
        runner.stepOver(); finish(runner)
        assertEquals(DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
        assertEquals(7, accumulator(runner))
        runner.reset(); runner.step()
        assertTrue(runner.canStepOut)
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            listOf(AddressedMemorySegmentData(0xc500, byteArrayOf(7))))).image
        assertIs<ProgramImageLoadResult.Success>(runner.loadProgramImage(null, image))
        assertFalse(runner.canStepOut)
    }

    @Test fun failedLoadsPreserveObservedFramesAndSourceMetadata() {
        for (kind in listOf("basic", "memory", "combined")) {
            val runner = runner(nested)
            runner.step() // Observe the outer CALL before attempting a rejected load.
            runner.attachSourceMap(emptyList())
            val sourceMap = assertNotNull(runner.sourceMap)
            val pc = runner.cpuSnapshot().programCounter
            val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
                listOf(AddressedMemorySegmentData(0, byteArrayOf(7))))).image // ROM is not writable.
            when (kind) {
                "basic" -> assertIs<BasicProgramLoadResult.Failure>(runner.loadBasicProgram(byteArrayOf(0)))
                "memory" -> assertIs<MemoryImageLoadResult.Failure>(runner.loadMemoryImage(image))
                else -> assertIs<ProgramImageLoadResult.MemoryFailure>(runner.loadProgramImage(null, image))
            }
            assertSame(sourceMap, runner.sourceMap, kind)
            assertEquals(pc, runner.cpuSnapshot().programCounter, kind)
            assertTrue(runner.canStepOut, kind)
            assertTrue(runner.stepOut(), kind)
            finish(runner)
            assertEquals(DebuggerStepStatus.OUT_COMPLETE, runner.debugStepStatus, kind)
            assertEquals(3, runner.cpuSnapshot().programCounter, kind)
        }
    }

    @Test fun bankIdentityIsCheckedEvenWhenReturnedInstructionBytesMatch() {
        for (restore in listOf(false, true)) {
            val internal = assemble("ORG 0\nJP 0x4000\nORG 0x100\nLIDP 0x3400\nLIA 1\nSTD\n" +
                (if (restore) "LIA 0\nSTD\n" else "") + "RTN").let(::romBytes)
            val id = MachineId("pc-1360")
            val bank = ByteArray(0x4000).also { byteArrayOf(0x78, 1, 0, 0x33).copyInto(it) }
            val components = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal)) +
                List(8) { RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, bank.copyOf()) }
            val runner = DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, components))).session,
                MonotonicClock { 0L }).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
            runner.step()
            assertEquals(0x4000, runner.cpuSnapshot().programCounter)
            runner.stepOver(); finish(runner)
            assertEquals(0x4003, runner.cpuSnapshot().programCounter)
            assertEquals(if (restore) DebuggerStepStatus.OVER_COMPLETE else DebuggerStepStatus.FRAME_LOST, runner.debugStepStatus)
        }
    }

    @Test fun legacyDebugContextReportsExistingStepCompletionAndDoesNotExecuteOnRead() {
        val runner = runner(nested)
        runner.stepOver(); finish(runner)
        val revision = runner.sessionRevision
        val service = DesktopDebugContextService(runner, "stepping-test")
        val context = assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context
        assertEquals(DebugStopContext.StepComplete, context.stop)
        assertEquals(revision, runner.sessionRevision)
    }

    private fun finish(runner: DesktopEmulatorRunner, faulted: Boolean = false) {
        repeat(1_000) { if (runner.state == RunnerState.RUNNING) runner.tick() }
        assertEquals(if (faulted) RunnerState.FAULTED else RunnerState.PAUSED, runner.state)
    }
    private fun accumulator(runner: DesktopEmulatorRunner) = runner.cpuSnapshot().copyInternalRam()[2].toInt() and 255
    private fun assemble(source: String) = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source))
    private fun romBytes(assembled: Sc61860AssemblyResult.Success) = ByteArray(0x2000).also { bytes ->
        assembled.image.segments.forEach { it.copyBytes().copyInto(bytes, it.startAddress) }
    }
    private fun session(source: String): EmulatorSession {
        val id = MachineId("pc-1245")
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
            RomComponent(RomComponentId("internal"), RomRole.INTERNAL, romBytes(assemble(source))),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
        )))).session
    }
    private fun runner(source: String) = DesktopEmulatorRunner(session(source), MonotonicClock { 0L }).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
}
