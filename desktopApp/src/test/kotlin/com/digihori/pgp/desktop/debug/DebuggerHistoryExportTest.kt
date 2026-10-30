package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import java.nio.file.Files
import kotlinx.serialization.json.*
import kotlin.test.*

class DebuggerHistoryExportTest {
    private fun recorded() = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 2, 7, 0x52)).apply {
        setInstructionTraceEnabled(true); setDebugObservationEnabled(true)
        assertIs<DebuggerLogpointResult.Success>(saveLogpoint(null, 0, "PC", "", true))
        repeat(3) { step() }
    }
    private fun capture(runner: DesktopEmulatorRunner, limits: DebuggerExportLimits = DebuggerExportLimits()) =
        Json.parseToJsonElement(DebuggerHistoryExport.capture(runner, limits, "2026-10-30T00:00:00Z").decodeToString()).jsonObject
    private fun section(root: JsonObject, name: String) = root.getValue(name).jsonObject
    private fun entries(root: JsonObject, name: String) = section(root, name).getValue("entries").jsonArray
    private fun error(code: DebuggerExportError, block: () -> Unit) =
        assertEquals(code, assertFailsWith<DebuggerExportException>(block = block).code)

    @Test fun capturesIndependentVersionedSectionsNewestLimitsAndStopWithoutChangingRunner() {
        val runner = recorded()
        val revision = runner.sessionRevision
        val trace = runner.instructionTrace()
        val accesses = runner.memoryAccessHistory()
        val logs = runner.logpointEntries()
        val root = capture(runner, DebuggerExportLimits(trace = 2, accesses = 1, banks = 0, logs = 1))
        assertEquals("pgp-debug-history", root.getValue("format").jsonPrimitive.content)
        assertEquals(1, root.getValue("formatVersion").jsonPrimitive.int)
        assertEquals("pc-1245", root.getValue("machineId").jsonPrimitive.content)
        assertEquals("STEP_COMPLETE", root.getValue("stop").jsonObject.getValue("code").jsonPrimitive.content)
        assertEquals("0006", root.getValue("pc").jsonPrimitive.content)
        assertEquals(3, section(root, "trace").getValue("retainedBeforeExport").jsonPrimitive.int)
        assertEquals(1, section(root, "trace").getValue("omittedByLimit").jsonPrimitive.int)
        assertEquals(listOf("0003", "0005"), entries(root, "trace").map { it.jsonObject.getValue("address").jsonPrimitive.content })
        assertEquals("0207", entries(root, "trace").first().jsonObject.getValue("bytes").jsonPrimitive.content)
        assertEquals(1, entries(root, "accesses").size)
        assertEquals("C000", entries(root, "accesses").single().jsonObject.getValue("address").jsonPrimitive.content)
        assertEquals("WRITE", entries(root, "accesses").single().jsonObject.getValue("kind").jsonPrimitive.content)
        assertEquals("0000", entries(root, "logs").single().jsonObject.getValue("value").jsonPrimitive.content)
        assertFalse(root.containsKey("memory")); assertFalse(root.containsKey("cpu"))
        assertFalse(root.getValue("bankEventsLinkedToInstructions").jsonPrimitive.boolean)
        assertEquals(revision, runner.sessionRevision)
        assertEquals(trace, runner.instructionTrace()); assertEquals(accesses, runner.memoryAccessHistory()); assertEquals(logs, runner.logpointEntries())
        val empty = capture(runner, DebuggerExportLimits(0, 0, 0, 0))
        for (name in listOf("trace", "accesses", "banks", "logs")) assertTrue(entries(empty, name).isEmpty())
    }

    @Test fun rejectsInvalidLimitsAndRunningWithoutPausingAndAllowsFaulted() {
        val runner = recorded()
        error(DebuggerExportError.LIMIT) { capture(runner, DebuggerExportLimits(trace = 4097)) }
        error(DebuggerExportError.LIMIT) { capture(runner, DebuggerExportLimits(logs = 257)) }
        error(DebuggerExportError.LIMIT) { capture(runner, DebuggerExportLimits(accesses = -1)) }
        runner.run()
        val revision = runner.sessionRevision
        error(DebuggerExportError.STATE) { capture(runner) }
        assertEquals(revision, runner.sessionRevision)
        val fault = debuggerTestRunner(byteArrayOf(0x3e)); fault.step()
        assertEquals("FAULTED", capture(fault).getValue("state").jsonPrimitive.content)
    }

    @Test fun chooserCancellationAndChangedOrReplacedSessionsNeverWrite() {
        val directory = Files.createTempDirectory("pgp-export-").toFile()
        try {
            val destination = directory.resolve("history.pgtrace.json").apply { writeText("original") }
            val runner = recorded()
            val revision = runner.sessionRevision
            assertFalse(DebuggerHistoryExport.save({ runner }, DebuggerExportLimits(), "time") { null })
            assertEquals(revision, runner.sessionRevision)
            error(DebuggerExportError.CHANGED) {
                DebuggerHistoryExport.save({ runner }, DebuggerExportLimits(), "time") { runner.reset(); destination }
            }
            var current = runner
            error(DebuggerExportError.CHANGED) {
                DebuggerHistoryExport.save({ current }, DebuggerExportLimits(), "time") { current = recorded(); destination }
            }
            assertEquals("original", destination.readText())
            assertTrue(DebuggerHistoryExport.save({ current }, DebuggerExportLimits(), "time") { destination })
            assertEquals("pgp-debug-history", Json.parseToJsonElement(destination.readText()).jsonObject.getValue("format").jsonPrimitive.content)
        } finally { directory.deleteRecursively() }
    }

    @Test fun atomicFailureAndInvalidOutputsPreserveDestinationAndCleanTemporaryFile() {
        val directory = Files.createTempDirectory("pgp-export-write-").toFile()
        try {
            val destination = directory.resolve("history.pgtrace.json").apply { writeText("original") }
            val bytes = DebuggerHistoryExport.capture(recorded(), DebuggerExportLimits(), "time")
            error(DebuggerExportError.IO) { DebuggerHistoryExport.write(destination, bytes) { _, _ -> throw java.io.IOException("move failed") } }
            assertEquals("original", destination.readText())
            assertEquals(listOf(destination.name), directory.listFiles()!!.map { it.name })
            error(DebuggerExportError.FORMAT) { DebuggerHistoryExport.write(directory.resolve("wrong.json"), bytes) }
            error(DebuggerExportError.SIZE) { DebuggerHistoryExport.write(destination, ByteArray(DebuggerHistoryExport.MAX_BYTES + 1)) }
            assertEquals("original", destination.readText())
        } finally { directory.deleteRecursively() }
    }

    @Test fun exportsPhysicalRomAndBankSwitchEventsWithoutInventingInstructionLinks() {
        val id = MachineId("pc-1360")
        val internal = ByteArray(0x2000).apply { byteArrayOf(0x10, 0x34, 0, 2, 1, 0x52).copyInto(this) }
        val parts = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal)) +
            List(8) { RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000)) }
        val runner = DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, parts))).session)
        runner.setDebugObservationEnabled(true); runner.setInstructionTraceEnabled(true)
        repeat(3) { runner.step() }
        val root = capture(runner)
        assertTrue(entries(root, "trace").first().jsonObject.containsKey("romComponent"))
        assertEquals(1, root.getValue("selectedRomBank").jsonPrimitive.int)
        assertEquals(1, entries(root, "banks").last().jsonObject.getValue("selectedBank").jsonPrimitive.int)
        assertEquals("01", entries(root, "banks").last().jsonObject.getValue("selectorValue").jsonPrimitive.content)
    }
}
