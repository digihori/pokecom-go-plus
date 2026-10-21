package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.desktop.debug.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.mcp.PgpMcpToolAdapter
import com.digihori.pgp.desktop.runner.RunnerState
import kotlinx.serialization.json.*
import kotlin.test.*

class DebugRegistrationsTest {
    @Test fun conditionalRegistrationsKeepVersionOneJsonShapeAndReadsAreSideEffectFree() {
        val runner = debuggerTestRunner()
        assertIs<DebuggerRegistrationResult.Success>(runner.saveBreakpoint(null, 0, condition = "A == 7", hitCondition = "% 3"))
        val service = DesktopDebugContextService(runner, "conditional-test")
        val revision = runner.sessionRevision
        val before = runner.breakpointRegistrations()
        val snapshot = assertIs<DebugRegistrationsResult.Success>(service.registrations()).snapshot
        val json = snapshot.toJson()
        assertEquals(1, json.getValue("schemaVersion").jsonPrimitive.int)
        assertEquals(setOf("id", "address", "enabled"), json.getValue("breakpoints").jsonArray.single().jsonObject.keys)
        assertEquals("A == 7", snapshot.breakpoints.single().condition)
        assertEquals(before, runner.breakpointRegistrations())
        assertEquals(revision, runner.sessionRevision)
    }

    @Test fun capturesEveryKindDisabledEntriesAndStableSessionWithoutMutatingExecution() {
        val runner = debuggerTestRunner()
        runner.saveBreakpoint(null, 0xc001, false)
        runner.saveBreakpoint(null, 0xc000)
        DebuggerWatchKind.entries.forEachIndexed { i, kind -> runner.saveWatch(null, 0xc000 + i, 0xc010 + i, kind, i != 1) }
        val service = DesktopDebugContextService(runner, "session-test", { "fixed" })
        val before = DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context)
        val revision = runner.sessionRevision
        val snapshot = assertIs<DebugRegistrationsResult.Success>(service.registrations()).snapshot
        assertEquals("session-test", snapshot.session.id)
        assertEquals(revision, snapshot.session.revision)
        assertEquals(listOf(0xc000, 0xc001), snapshot.breakpoints.map { it.address })
        assertFalse(snapshot.breakpoints.last().enabled)
        val json = snapshot.toJson()
        assertEquals(listOf("change", "read", "write", "readWrite"), json.getValue("watches").jsonArray.map { it.jsonObject.getValue("kind").jsonPrimitive.content })
        assertEquals("logicalAllBanks", json.getValue("addressScope").jsonPrimitive.content)
        assertEquals(revision, runner.sessionRevision)
        assertContentEquals(before, DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context))
        runner.setBreakpointEnabled(snapshot.breakpoints.last().id, true)
        assertFalse(snapshot.breakpoints.last().enabled)
        val updated = assertIs<DebugRegistrationsResult.Success>(service.registrations()).snapshot
        assertTrue(updated.session.revision > revision)
        runner.reset()
        assertEquals(snapshot.breakpoints.map { it.id }, assertIs<DebugRegistrationsResult.Success>(service.registrations()).snapshot.breakpoints.map { it.id })
    }

    @Test fun toolReturnsRegistrationSchemaRejectsArgumentsAndDoesNotPause() {
        val runner = debuggerTestRunner()
        val service = DesktopDebugContextService(runner, "tool-session")
        val adapter = PgpMcpToolAdapter({ service })
        fun call(args: JsonObject = buildJsonObject {}) = adapter.call(PgpMcpToolAdapter.GET_DEBUG_REGISTRATIONS, args)
        val result = call()
        assertEquals("false", result.getValue("isError").jsonPrimitive.content)
        val body = result.getValue("structuredContent").jsonObject
        assertEquals("pgp-debug-registrations", body.getValue("format").jsonPrimitive.content)
        assertEquals("1", body.getValue("schemaVersion").jsonPrimitive.content)
        assertTrue(body.getValue("breakpoints").jsonArray.isEmpty())
        assertTrue(body.getValue("watches").jsonArray.isEmpty())
        fun code(result: JsonObject) = result.getValue("structuredContent").jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content
        assertEquals("INVALID_ARGUMENTS", code(call(buildJsonObject { put("enabled", true) })))
        runner.run()
        val revision = runner.sessionRevision
        assertEquals("SESSION_RUNNING", code(call()))
        assertEquals(RunnerState.RUNNING, runner.state)
        assertEquals(revision, runner.sessionRevision)
        assertEquals("NO_ACTIVE_SESSION", code(PgpMcpToolAdapter({ null }).call(PgpMcpToolAdapter.GET_DEBUG_REGISTRATIONS, buildJsonObject {})))
        runner.pause()
        assertEquals("false", call().getValue("isError").jsonPrimitive.content)
    }

    @Test fun faultedSessionCanReadRegistrations() {
        val runner = debuggerTestRunner(byteArrayOf(0x3e))
        runner.step()
        assertEquals(RunnerState.FAULTED, runner.state)
        assertEquals("faulted", assertIs<DebugRegistrationsResult.Success>(DesktopDebugContextService(runner).registrations()).snapshot.session.runState)
    }
}
