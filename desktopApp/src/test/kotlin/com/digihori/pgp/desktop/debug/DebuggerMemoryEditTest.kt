package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import kotlin.test.*

class DebuggerMemoryEditTest {
    private fun preview(runner: DesktopEmulatorRunner, address: Int = 0xc000, value: Int = 0x41) =
        assertIs<DebuggerMemoryEditResult.Success>(runner.previewMemoryEdit(address, value)).edit
    private fun failure(error: DebuggerMemoryEditError, result: DebuggerMemoryEditResult) =
        assertEquals(DebuggerMemoryEditResult.Failure(error), result)

    @Test fun previewIsReadOnlyAndSingleByteEditUpdatesAliasesAndComparisonWithoutCpuEvents() {
        val runner = debuggerTestRunner()
        runner.setDebugObservationEnabled(true)
        runner.setInstructionTraceEnabled(true)
        val tracker = DebuggerComparisonTracker(0xc000)
        tracker.reset(runner)
        val revision = runner.sessionRevision
        val generation = runner.debugTargetGeneration
        val cpu = runner.cpuSnapshot()
        val edit = preview(runner, 0xd000)
        assertEquals(0xc000, edit.physicalAddress)
        assertEquals(0, runner.memoryByte(0xc000))
        assertEquals(revision, runner.sessionRevision)
        assertIs<DebuggerMemoryEditResult.Success>(runner.applyMemoryEdit(edit))
        assertEquals(0x41, runner.memoryByte(0xc000))
        assertEquals(0x41, runner.memoryByte(0xd000))
        assertEquals(0, runner.memoryByte(0xc001))
        assertEquals(cpu.programCounter, runner.cpuSnapshot().programCounter)
        assertContentEquals(cpu.copyInternalRam(), runner.cpuSnapshot().copyInternalRam())
        assertEquals(revision + 1, runner.sessionRevision)
        assertEquals(generation + 1, runner.debugTargetGeneration)
        assertTrue(runner.instructionTrace().isEmpty())
        assertTrue(runner.memoryAccessHistory().isEmpty())
        assertEquals(MemoryValueChange(0xc000, 0, 0x41), tracker.stopped(runner).memoryChanges[0xc000])
    }

    @Test fun invalidStaleAndRunningEditsDoNotChangeMemoryOrDebugState() {
        val runner = debuggerTestRunner()
        runner.attachSourceMap(emptyList())
        val sourceMap = runner.sourceMap
        val revision = runner.sessionRevision
        val generation = runner.debugTargetGeneration
        for (address in listOf(-1, 65536)) failure(DebuggerMemoryEditError.ADDRESS, runner.previewMemoryEdit(address, 1))
        for (value in listOf(-1, 256)) failure(DebuggerMemoryEditError.VALUE, runner.previewMemoryEdit(0xc000, value))
        for (address in listOf(0, 0x8000, 0xf800, 0xe800))
            failure(DebuggerMemoryEditError.RAM_ONLY, runner.previewMemoryEdit(address, 1))
        failure(DebuggerMemoryEditError.UNCHANGED, runner.previewMemoryEdit(0xc000, 0))
        val edit = preview(runner)
        failure(DebuggerMemoryEditError.STALE, runner.applyMemoryEdit(edit.copy(before = 5)))
        failure(DebuggerMemoryEditError.STALE, runner.applyMemoryEdit(edit.copy(physicalAddress = 0xc001)))
        failure(DebuggerMemoryEditError.STALE, debuggerTestRunner().applyMemoryEdit(edit))
        assertEquals(revision, runner.sessionRevision)
        assertEquals(generation, runner.debugTargetGeneration)
        assertSame(sourceMap, runner.sourceMap)
        runner.run()
        val runningRevision = runner.sessionRevision
        failure(DebuggerMemoryEditError.PAUSED_ONLY, runner.applyMemoryEdit(edit))
        assertEquals(runningRevision, runner.sessionRevision)
        runner.pause()
        failure(DebuggerMemoryEditError.STALE, runner.applyMemoryEdit(edit))
        val beforeReset = preview(runner)
        runner.reset()
        failure(DebuggerMemoryEditError.STALE, runner.applyMemoryEdit(beforeReset))
        assertEquals(0, runner.memoryByte(0xc000))
        val fault = debuggerTestRunner(byteArrayOf(0x3e)); fault.step()
        failure(DebuggerMemoryEditError.PAUSED_ONLY, fault.previewMemoryEdit(0xc000, 1))
    }

    @Test fun editRebasesOnlyEditedPhysicalByteAndLaterCpuWriteStillTriggersWatches() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 2, 9, 0x52)) // LIDP C000; LIA 9; STD
        assertIs<DebuggerRegistrationResult.Success>(runner.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.CHANGE))
        assertIs<DebuggerRegistrationResult.Success>(runner.saveWatch(null, 0xd000, 0xd001, DebuggerWatchKind.CHANGE))
        assertIs<DebuggerRegistrationResult.Success>(runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.WRITE))
        val watches = runner.watchRegistrations()
        assertIs<DebuggerMemoryEditResult.Success>(runner.applyMemoryEdit(preview(runner)))
        runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
        runner.step()
        assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason)
        assertEquals(9, runner.memoryByte(0xc000))
        assertEquals(watches, runner.watchRegistrations())
        // Test change-only aliases as well; an access stop has higher priority.
        runner.clearMemoryAccessWatch()
        assertIs<DebuggerMemoryEditResult.Success>(runner.applyMemoryEdit(preview(runner, value = 7)))
        assertEquals(7, runner.memoryByte(0xd000))
        runner.step()
        assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
    }

    @Test fun editingInvalidatesFramesAndSourceButPreservesBreakpointCountsAndDefinitions() {
        val runner = debuggerTestRunner(byteArrayOf(0x78, 0, 6, 0x79, 0, 3, 0x37))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        assertIs<DebuggerRegistrationResult.Success>(runner.saveBreakpoint(null, 0))
        runner.run(); runner.tick()
        val registrations = runner.breakpointRegistrations()
        assertEquals(1L, registrations.single().hitCount)
        runner.step()
        assertTrue(runner.canStepOut)
        runner.attachSourceMap(emptyList())
        assertIs<DebuggerMemoryEditResult.Success>(runner.applyMemoryEdit(preview(runner)))
        assertNull(runner.sourceMap)
        assertFalse(runner.canStepOut)
        assertEquals(registrations, runner.breakpointRegistrations())
    }

    @Test fun rebasingEditedByteDoesNotHideOtherPendingChanges() {
        val bytes = byteArrayOf(0, 0)
        val store = DebuggerRegistrationStore { _, _ -> bytes.copyOf() }
        assertIs<DebuggerRegistrationResult.Success>(store.saveWatch(null, 0xc000, 0xc001, DebuggerWatchKind.CHANGE, true))
        bytes[0] = 7; bytes[1] = 9
        store.rebaseEditedByte(7) { it == 0xc000 }
        assertEquals(listOf(MemoryValueChange(0xc001, 0, 9)), store.changes())
    }

    @Test fun rejectedCoreWritePreservesAllStateAndPreviewCanBeRetried() {
        val id = MachineId("pc-1245")
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
            RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000)),
        )))).session
        val rejected = object : EmulatorSession by session {
            override fun loadMemoryImage(image: AddressedMemoryImage) = MemoryImageLoadResult.Failure(MemoryImageLoadError.ReadOnlyAddress(0xc000, 0))
        }
        val runner = DesktopEmulatorRunner(rejected)
        runner.attachSourceMap(emptyList())
        val source = runner.sourceMap
        val edit = preview(runner)
        val generation = runner.debugTargetGeneration
        repeat(2) { failure(DebuggerMemoryEditError.WRITE, runner.applyMemoryEdit(edit)) }
        assertSame(source, runner.sourceMap)
        assertEquals(edit.revision, runner.sessionRevision)
        assertEquals(generation, runner.debugTargetGeneration)
        assertEquals(0, runner.memoryByte(0xc000))
    }
}
