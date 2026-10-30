package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.runner.*
import kotlin.test.*

class DebuggerCaseSteppingTest {
    private fun program(value: Int = 0x10, handler: String = "LIA 1\nRTN") = """
        ORG 0
        LIA $value
        CASE1 0x020040
        CASE2
        DB 0x10
        DW FIRST
        DB 0x20
        DW SECOND
        DW DEFAULT
        ORG 0x20
        FIRST: $handler
        ORG 0x30
        SECOND: LIA 2
        RTN
        ORG 0x38
        DEFAULT: LIA 3
        RTN
        ORG 0x40
        AFTER: LIA 9
        JP AFTER
    """.trimIndent()

    private fun runner(source: String, limit: Long = 100000): DesktopEmulatorRunner {
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source))
        val internal = ByteArray(0x2000)
        assembled.image.segments.forEach { it.copyBytes().copyInto(internal, it.startAddress) }
        val id = MachineId("pc-1245")
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
            RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
        )))).session
        return DesktopEmulatorRunner(session, MonotonicClock { 0L }, debugStepInstructionLimit = limit).apply {
            setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        }
    }
    private fun finish(runner: DesktopEmulatorRunner) {
        repeat(100) { if (runner.state == RunnerState.RUNNING) runner.tick() }
        assertNotEquals(RunnerState.RUNNING, runner.state)
    }
    private fun accumulator(runner: DesktopEmulatorRunner) = runner.cpuSnapshot().copyInternalRam()[2].toInt() and 255

    @Test fun overCaseOneAndOutObservedCaseFrameFollowMatchingAndDefaultBranches() {
        for ((value, expected) in listOf(0x10 to 1, 0x20 to 2, 0x99 to 3)) {
            for (out in listOf(false, true)) {
                val runner = runner(program(value))
                runner.step()
                if (out) { runner.step(); assertTrue(runner.canStepOut); assertTrue(runner.stepOut()) }
                else assertTrue(runner.stepOver())
                finish(runner)
                assertEquals(0x40, runner.cpuSnapshot().programCounter)
                assertEquals(expected, accumulator(runner))
                assertEquals(if (out) DebuggerStepStatus.OUT_COMPLETE else DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
                assertFalse(runner.canStepOut)
                assertEquals(0x60, runner.cpuSnapshot().r)
            }
        }
    }

    @Test fun steppingOverCaseTwoDispatchesOneInstructionAndRetainsObservedReturn() {
        val runner = runner(program())
        runner.step(); runner.step() // LIA and CASE1
        assertTrue(runner.stepOver()); finish(runner)
        assertEquals(0x20, runner.cpuSnapshot().programCounter)
        assertEquals(DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
        assertTrue(runner.canStepOut)
        assertTrue(runner.stepOut()); finish(runner)
        assertEquals(0x40, runner.cpuSnapshot().programCounter)
    }

    @Test fun callOverAndOutTraverseNestedCaseAndCallsWithoutLosingOuterFrame() {
        val source = """
            ORG 0
            CALL OUTER
            LIA 9
            JP 3
            ORG 0x100
            OUTER: LIA 0
            CASE1 0x010110
            CASE2
            DB 0
            DW HANDLER
            DW HANDLER
            ORG 0x110
            RTN
            ORG 0x120
            HANDLER: CALL INNER
            RTN
            INNER: LIA 7
            RTN
        """.trimIndent()
        for (out in listOf(false, true)) {
            val runner = runner(source)
            if (out) { runner.step(); runner.stepOut() } else runner.stepOver()
            finish(runner)
            assertEquals(3, runner.cpuSnapshot().programCounter)
            assertEquals(7, accumulator(runner))
            assertFalse(runner.canStepOut)
            assertEquals(if (out) DebuggerStepStatus.OUT_COMPLETE else DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
        }
        val runner = runner(source)
        repeat(3) { runner.step() } // CALL, LIA, CASE1
        runner.stepOut(); finish(runner)
        assertEquals(0x110, runner.cpuSnapshot().programCounter)
        assertTrue(runner.canStepOut)
        runner.stepOut(); finish(runner)
        assertEquals(3, runner.cpuSnapshot().programCounter)
    }

    @Test fun nestedCasesWithIdenticalReturnPcWaitForTheCorrectFrame() {
        val runner = runner("""
            ORG 0
            CASE1 0x010040
            CASE2
            DB 0
            DW 0x20
            DW 0x20
            ORG 0x20
            CASE1 0x010040
            CASE2
            DB 0
            DW 0x30
            DW 0x30
            ORG 0x30
            RTN
            ORG 0x40
            RTN
        """.trimIndent())
        runner.setInstructionTraceEnabled(true)
        runner.stepOver(); finish(runner)
        assertEquals(DebuggerStepStatus.OVER_COMPLETE, runner.debugStepStatus)
        assertEquals(0x40, runner.cpuSnapshot().programCounter)
        assertEquals(2, runner.instructionTrace().count { it.instruction.bytes.first() == 0x37 })
        assertFalse(runner.canStepOut)
    }

    @Test fun breakpointWatchFaultStackDamageAndLimitKeepTheirExistingPriority() {
        val breakpoint = runner(program())
        breakpoint.step(); breakpoint.saveBreakpoint(null, 6)
        breakpoint.stepOver(); finish(breakpoint)
        assertEquals(DebuggerStopReason.Breakpoint(6), breakpoint.stopReason)
        assertEquals(DebuggerStepStatus.INTERRUPTED, breakpoint.debugStepStatus)
        assertTrue(breakpoint.canStepOut)
        breakpoint.stepOut(); finish(breakpoint)
        assertEquals(0x40, breakpoint.cpuSnapshot().programCounter)
        val watch = runner(program(handler = "LIDP 0xC000\nLIA 7\nSTD\nRTN"))
        watch.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE)
        watch.step(); watch.stepOver(); finish(watch)
        assertIs<DebuggerStopReason.MemoryAccessed>(watch.stopReason)
        assertEquals(DebuggerStepStatus.INTERRUPTED, watch.debugStepStatus)
        assertTrue(watch.canStepOut)
        watch.stepOut(); finish(watch)
        assertEquals(0x40, watch.cpuSnapshot().programCounter)
        val fault = runner(program(handler = "DB 0x3E"))
        fault.step(); fault.stepOver(); finish(fault)
        assertEquals(RunnerState.FAULTED, fault.state)
        assertFalse(fault.canStepOut)
        val damage = runner(program(handler = "LIP 0x5E\nLIA 0\nEXAM\nRTN"))
        damage.step(); damage.stepOver(); finish(damage)
        assertEquals(DebuggerStepStatus.FRAME_LOST, damage.debugStepStatus)
        val limited = runner(program(handler = "JP FIRST"), limit = 10)
        limited.step(); limited.stepOver(); finish(limited)
        assertEquals(DebuggerStepStatus.LIMIT, limited.debugStepStatus)
        assertTrue(limited.canStepOut)
    }

    @Test fun explicitReturnRomIsUsedAndChangedReturnBankIsRejected() {
        for ((returnPc, restore, expected) in listOf(Triple(0x100, false, true), Triple(0x4008, false, false), Triple(0x4008, true, true))) {
            val id = MachineId("pc-1360")
            val internal = ByteArray(0x2000).apply {
                byteArrayOf(0x79, 0x40, 0).copyInto(this)
                (byteArrayOf(0x10, 0x34, 0, 2, 1, 0x52) +
                    (if (restore) byteArrayOf(2, 0, 0x52) else byteArrayOf()) + byteArrayOf(0x37)).copyInto(this, 0x200)
            }
            val bank = ByteArray(0x4000).apply {
                byteArrayOf(0x7a, 1, (returnPc ushr 8).toByte(), returnPc.toByte(), 0x69, 0, 2, 0, 2, 0).copyInto(this)
            }
            val parts = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal)) +
                List(8) { RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, bank.copyOf()) }
            val runner = DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, parts))).session,
                MonotonicClock { 0L }).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
            runner.step(); runner.stepOver(); finish(runner)
            assertEquals(returnPc, runner.cpuSnapshot().programCounter)
            assertEquals(if (expected) DebuggerStepStatus.OVER_COMPLETE else DebuggerStepStatus.FRAME_LOST, runner.debugStepStatus)
        }
    }

    @Test fun fastUnobservedExecutionDoesNotInventCaseFrames() {
        val runner = runner(program())
        runner.run(); runner.tick(); runner.pause()
        assertFalse(runner.canStepOut)
        assertFalse(runner.stepOut())
        assertEquals(DebuggerStepStatus.NO_FRAME, runner.debugStepStatus)
    }
}
