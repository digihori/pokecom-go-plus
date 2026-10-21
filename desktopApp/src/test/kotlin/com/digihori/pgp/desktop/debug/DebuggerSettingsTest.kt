package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.rom.*
import com.digihori.pgp.desktop.runner.*
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.*
import kotlin.test.*

class DebuggerSettingsTest {
    @Test fun roundTripUsesNewIdsZeroCountsAndCurrentWatchBaselinesWithoutSavingExecutionData() {
        val root = Files.createTempDirectory("pgp-debug-settings-").toFile()
        try {
            val runner = runner(byteArrayOf(0x10, 0xc0.toByte(), 0, 2, 7, 0x52, 0x79, 0, 0))
            val project = root.resolve("pgp-project.json").apply { writeText("manifest sentinel") }
            runner.saveBreakpoint(null, 5, condition = "A == 7", hitCondition = ">= 2")
            runner.saveBreakpoint(null, 0xc501, false, "MEM[0xC000] == 7", "% 3")
            runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE)
            runner.saveWatch(null, 0xc010, 0xc010, DebuggerWatchKind.READ_WRITE, false)
            runner.setInstructionTraceEnabled(true); runner.setInstructionTraceCapacity(16)
            runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
            runner.run(); runner.tick()
            assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason)
            assertEquals(1L, runner.breakpointRegistrations().first().hitCount)
            val revision = runner.sessionRevision
            val settings = DebuggerSettingsFiles.capture(runner, project)
            assertEquals(revision, runner.sessionRevision)
            val file = root.resolve("saved.pgdebug.json")
            DebuggerSettingsFiles.write(file, settings)
            val text = file.readText()
            assertFalse(root.absolutePath in text)
            val json = Json.parseToJsonElement(text).jsonObject
            assertEquals(setOf("format", "schemaVersion", "target", "breakpoints", "watches", "trace"), json.keys)
            assertTrue(json.getValue("breakpoints").jsonArray.all { "id" !in it.jsonObject && "hitCount" !in it.jsonObject })
            assertEquals(setOf("enabled", "capacity"), json.getValue("trace").jsonObject.keys)
            assertEquals(settings, DebuggerSettingsFiles.read(file))
            val oldIds = runner.breakpointRegistrations().map { it.id } + runner.watchRegistrations().map { it.id }
            load(runner, 0xc000, 9)
            val cpu = runner.cpuSnapshot()
            val trace = runner.instructionTrace()
            val stop = runner.stopReason
            DebuggerSettingsFiles.restore(runner, project, DebuggerSettingsFiles.read(file))
            assertTrue((runner.breakpointRegistrations().map { it.id } + runner.watchRegistrations().map { it.id }).all { it > oldIds.max() })
            assertTrue(runner.breakpointRegistrations().all { it.hitCount == 0L })
            assertEquals("A == 7", runner.breakpointRegistrations().first().condition)
            assertEquals(">= 2", runner.breakpointRegistrations().first().hitCondition)
            assertFalse(runner.breakpointRegistrations().last().enabled)
            assertEquals(16, runner.instructionTraceCapacity()); assertTrue(runner.isInstructionTraceEnabled())
            assertEquals(trace, runner.instructionTrace())
            assertEquals(stop, runner.stopReason)
            assertTrue(CpuSnapshotDifference.changed(cpu, runner.cpuSnapshot()).isEmpty())
            runner.step() // JP: current value 9 is the fresh baseline, rather than the old saved value 7.
            assertEquals(DebuggerStopReason.StepComplete, runner.stopReason)
            repeat(3) { runner.step() }
            val change = assertIs<DebuggerStopReason.MemoryChanged>(runner.stopReason).changes.single()
            assertEquals(9, change.before); assertEquals(7, change.after)
            assertEquals("manifest sentinel", project.readText())
            val second = runner()
            assertTrue(second.breakpoints().isEmpty())
            DebuggerSettingsFiles.restore(second, project, settings)
            assertEquals(2, second.breakpointRegistrations().size)
        } finally { root.deleteRecursively() }
    }

    @Test fun mismatchedTargetsRunningStateAndMissingRomIdNeverMutateSettings() {
        val root = Files.createTempDirectory("pgp-debug-target-").toFile()
        try {
            val runner = runner()
            runner.saveBreakpoint(null, 7)
            val project = root.resolve("pgp-project.json")
            val saved = DebuggerSettingsFiles.capture(runner, project)
            val before = runner.breakpointRegistrations(); val revision = runner.sessionRevision
            for (target in listOf(saved.target.copy(machineId = "pc-1251"), saved.target.copy(romSha256 = "b".repeat(64)),
                saved.target.copy(memoryConfiguration = "OTHER"), saved.target.copy(projectSha256 = null))) {
                error(DebuggerSettingsError.TARGET) { DebuggerSettingsFiles.restore(runner, project, saved.copy(target = target)) }
                assertEquals(before, runner.breakpointRegistrations()); assertEquals(revision, runner.sessionRevision)
            }
            error(DebuggerSettingsError.TARGET) { DebuggerSettingsFiles.restore(runner, root.resolve("other/pgp-project.json"), saved) }
            error(DebuggerSettingsError.TARGET) { DebuggerSettingsFiles.restore(runner, null, saved) }
            runner.run()
            val runningRevision = runner.sessionRevision
            error(DebuggerSettingsError.RUNNING) { DebuggerSettingsFiles.capture(runner, project) }
            error(DebuggerSettingsError.RUNNING) { DebuggerSettingsFiles.restore(runner, project, saved) }
            assertEquals(RunnerState.RUNNING, runner.state); assertEquals(runningRevision, runner.sessionRevision)
            error(DebuggerSettingsError.NO_ROM_ID) { DebuggerSettingsFiles.capture(DesktopEmulatorRunner(session()), null) }
        } finally { root.deleteRecursively() }
    }

    @Test fun invalidDefinitionsAndFailedObservationLeaveEveryLiveSettingAndRevisionUnchanged() {
        var unavailable = false
        val session = session()
        val runner = DesktopEmulatorRunner(session, debugRomFingerprint = fingerprint,
            memoryBytesProvider = { start, size -> if (unavailable) throw IllegalStateException("test") else session.memorySnapshot(start, size).copyBytes() })
        runner.saveBreakpoint(null, 7)
        runner.setInstructionTraceEnabled(true)
        val saved = DebuggerSettingsFiles.capture(runner, null)
        val revision = runner.sessionRevision
        val before = runner.breakpointRegistrations()
        val invalid = listOf(
            saved.copy(breakpoints = listOf(DebuggerBreakpointSetting(0x10000, true, "", ""))),
            saved.copy(breakpoints = listOf(DebuggerBreakpointSetting(1, true, "A = 1", ""))),
            saved.copy(breakpoints = listOf(DebuggerBreakpointSetting(1, true, "", "% 0"))),
            saved.copy(breakpoints = saved.breakpoints + saved.breakpoints),
            saved.copy(watches = listOf(DebuggerWatchSetting(2, 1, DebuggerWatchKind.READ, true))),
            saved.copy(watches = listOf(DebuggerWatchSetting(0xc000, 0xd000, DebuggerWatchKind.CHANGE, true))),
        )
        for (settings in invalid) {
            error(DebuggerSettingsError.REGISTRATIONS) { DebuggerSettingsFiles.restore(runner, null, settings) }
            assertEquals(before, runner.breakpointRegistrations()); assertEquals(revision, runner.sessionRevision)
        }
        error(DebuggerSettingsError.TRACE) { DebuggerSettingsFiles.restore(runner, null, saved.copy(traceCapacity = 0)) }
        unavailable = true
        error(DebuggerSettingsError.REGISTRATIONS) { DebuggerSettingsFiles.restore(runner, null,
            saved.copy(watches = listOf(DebuggerWatchSetting(0xc000, 0xc000, DebuggerWatchKind.CHANGE, true)), traceEnabled = false, traceCapacity = 16)) }
        assertEquals(before, runner.breakpointRegistrations()); assertEquals(revision, runner.sessionRevision)
        assertEquals(0L, runner.debugSettingsRevision)
        assertTrue(runner.isInstructionTraceEnabled()); assertTrue(runner.watchRegistrations().isEmpty())
    }

    @Test fun parserRejectsUnknownVersionsFieldsTypesOversizeFilesAndBadInputWithoutOverwritingFiles() {
        val root = Files.createTempDirectory("pgp-debug-invalid-").toFile()
        try {
            val settings = DebuggerSettingsFiles.capture(runner(), null)
            val objectValue = Json.parseToJsonElement(DebuggerSettingsFiles.encode(settings)).jsonObject
            error(DebuggerSettingsError.VERSION) { DebuggerSettingsFiles.decode(JsonObject(objectValue + ("schemaVersion" to JsonPrimitive(99))).toString()) }
            for (text in listOf("{}", "[]", "bad", JsonObject(objectValue + ("cpu" to JsonPrimitive(1))).toString(),
                JsonObject(objectValue + ("schemaVersion" to JsonPrimitive("1"))).toString())) {
                error(DebuggerSettingsError.FORMAT) { DebuggerSettingsFiles.decode(text) }
            }
            val oversized = root.resolve("large.pgdebug.json").apply { writeBytes(ByteArray(DebuggerSettingsFiles.MAX_BYTES + 1) { 32 }) }
            error(DebuggerSettingsError.FORMAT) { DebuggerSettingsFiles.read(oversized) }
            val file = root.resolve("existing.pgdebug.json").apply { writeText("sentinel") }
            error(DebuggerSettingsError.TRACE) { DebuggerSettingsFiles.write(file, settings.copy(traceCapacity = 0)) }
            assertEquals("sentinel", file.readText())
            val other = root.resolve("pgp-project.json").apply { writeText("manifest") }
            error(DebuggerSettingsError.FORMAT) { DebuggerSettingsFiles.write(other, settings) }
            assertEquals("manifest", other.readText())
            error(DebuggerSettingsError.IO) { DebuggerSettingsFiles.write(root.resolve("missing/other.pgdebug.json"), settings) }
            assertTrue(root.listFiles()!!.none { it.name.endsWith(".tmp") })
            DebuggerSettingsFiles.write(file, settings)
            assertEquals(settings, DebuggerSettingsFiles.read(file))
        } finally { root.deleteRecursively() }
    }

    @Test fun tracingSetupFailureDoesNotReplaceRegistrationsOrTraceSettings() {
        val delegate = session()
        val observed = mutableListOf<Boolean>()
        val faulting = object : EmulatorSession by delegate {
            override fun setMemoryAccessTracing(enabled: Boolean) {
                observed += enabled
                if (enabled) throw IllegalStateException("synthetic tracing failure")
                delegate.setMemoryAccessTracing(false)
            }
        }
        val runner = DesktopEmulatorRunner(faulting, debugRomFingerprint = fingerprint)
        runner.saveBreakpoint(null, 7)
        val saved = DebuggerSettingsFiles.capture(runner, null)
        val before = runner.breakpointRegistrations(); val revision = runner.sessionRevision
        error(DebuggerSettingsError.REGISTRATIONS) {
            DebuggerSettingsFiles.restore(runner, null, saved.copy(traceEnabled = true, traceCapacity = 16,
                watches = listOf(DebuggerWatchSetting(0xc000, 0xc000, DebuggerWatchKind.WRITE, true))))
        }
        assertEquals(listOf(true, false), observed)
        assertEquals(before, runner.breakpointRegistrations()); assertEquals(revision, runner.sessionRevision)
        assertFalse(runner.isInstructionTraceEnabled()); assertTrue(runner.watchRegistrations().isEmpty())
    }

    @Test fun restoringSmallerTraceCapacityTrimsExistingHistoryAndFaultStateIsNotReset() {
        val runner = runner(byteArrayOf(0x33, 0x79, 0, 0))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        runner.setInstructionTraceEnabled(true)
        runner.saveBreakpoint(null, 0, hitCondition = "== 20")
        runner.run(); runner.tick()
        val kept = runner.instructionTrace().takeLast(16)
        assertTrue(runner.instructionTrace().size > 16)
        val saved = DebuggerSettingsFiles.capture(runner, null).copy(breakpoints = emptyList(), traceEnabled = false, traceCapacity = 16)
        DebuggerSettingsFiles.restore(runner, null, saved)
        assertEquals(kept, runner.instructionTrace())
        assertFalse(runner.isInstructionTraceEnabled()); assertTrue(runner.breakpoints().isEmpty())
        val fault = runner(byteArrayOf(0x3e))
        fault.step()
        val reason = fault.stopReason
        val config = DebuggerSettingsFiles.capture(fault, null).copy(traceEnabled = true, traceCapacity = 16)
        DebuggerSettingsFiles.restore(fault, null, config)
        assertEquals(RunnerState.FAULTED, fault.state); assertEquals(reason, fault.stopReason)
        assertTrue(fault.isInstructionTraceEnabled())
    }

    @Test fun fingerprintIsStableAcrossComponentOrderAndCoversContentIdsAndRoles() {
        val a = RomComponent(RomComponentId("a"), RomRole.INTERNAL, byteArrayOf(1, 2))
        val b = RomComponent(RomComponentId("b"), RomRole.EXTERNAL, byteArrayOf(3))
        fun hash(components: List<RomComponent>) = DebuggerSettingsFiles.romFingerprint(RomSet(MachineId("pc-1245"), components))
        assertEquals(hash(listOf(a, b)), hash(listOf(b, a)))
        for (other in listOf(RomComponent(a.id, a.role, byteArrayOf(1, 3)), RomComponent(a.id, RomRole.EXTERNAL, a.copyBytes()),
            RomComponent(RomComponentId("c"), a.role, a.copyBytes()))) {
            assertNotEquals(hash(listOf(a, b)), hash(listOf(other, b)))
        }
        val image = ByteArray(0x10000)
        val first = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPc1245LegacyImage(image))
        image[0] = 7
        val second = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPc1245LegacyImage(image))
        assertNotNull(first.romFingerprint); assertNotEquals(first.romFingerprint, second.romFingerprint)
    }

    private fun error(code: DebuggerSettingsError, block: () -> Unit) { assertEquals(code, assertFailsWith<DebuggerSettingsException>(block = block).code) }
    private fun load(runner: DesktopEmulatorRunner, address: Int, value: Int) {
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            listOf(AddressedMemorySegmentData(address, byteArrayOf(value.toByte()))))).image
        assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
    }
    private val fingerprint = "a".repeat(64)
    private fun runner(program: ByteArray = byteArrayOf()) = DesktopEmulatorRunner(session(program), debugRomFingerprint = fingerprint)
    private fun session(program: ByteArray = byteArrayOf()): EmulatorSession {
        val id = MachineId("pc-1245")
        val rom = RomSet(id, listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000).also { program.copyInto(it) }),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x4000))))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
    }
}
