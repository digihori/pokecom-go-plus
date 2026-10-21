package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.debug.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.desktop.runner.*
import kotlin.test.*
import kotlinx.serialization.json.*

class DebugChangeServiceTest {
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun expect(code: String, action: () -> Unit) = assertEquals(code, assertFailsWith<DevelopmentReadError>(block = action).code)
    private inner class Fixture(program: ByteArray = byteArrayOf(0x02, 0x41, 0x33)) {
        val runner = debuggerTestRunner(program)
        var state = DevelopmentReadState(runner = runner, debugService = DesktopDebugContextService(runner, "debug-review"))
        var notifications = 0
        val service = DebugChangeService({ state }, { notifications++ }, { it.pause() }, { it.step() })
        fun propose(name: String, parameters: JsonObject = buildJsonObject {}) = service.propose(
            "debug-review", if (name == "pause") null else runner.sessionRevision, DebugAction(name, parameters))
        fun approve(name: String, parameters: JsonObject = buildJsonObject {}): JsonObject {
            val proposed = propose(name, parameters); service.approve()
            return service.operation(proposed.string("operationId"))
        }
    }

    @Test fun conditionalBreakpointCreateUpdateDeleteRequireApprovalAndPreserveCpuAndRam() {
        val f = Fixture(); val runner = f.runner
        val cpu = runner.cpuSnapshot().programCounter
        val memory = runner.memorySnapshot(0xc000, 16).copyBytes()
        val revision = runner.sessionRevision
        val request = f.propose("save_breakpoint", buildJsonObject {
            put("address", 0xc500); put("enabled", true); put("condition", "A == 0x41"); put("hitCondition", ">= 2")
        })
        assertEquals("PENDING_APPROVAL", request.string("status")); assertTrue(runner.breakpointRegistrations().isEmpty())
        assertEquals(revision, runner.sessionRevision)
        f.service.approve()
        val operation = f.service.operation(request.string("operationId"))
        assertEquals("SUCCEEDED", operation.string("status"))
        val bp = runner.breakpointRegistrations().single()
        assertEquals("A == 0x41", bp.condition); assertEquals(">= 2", bp.hitCondition)
        assertEquals(bp.id, operation["registrationId"]!!.jsonPrimitive.long)
        assertTrue(runner.sessionRevision > revision)
        val changed = f.approve("save_breakpoint", buildJsonObject { put("id", bp.id); put("address", 0xc510); put("enabled", false) })
        assertEquals("SUCCEEDED", changed.string("status")); assertFalse(runner.breakpointRegistrations().single().enabled)
        assertEquals("A == 0x41", runner.breakpointRegistrations().single().condition)
        val rejected = f.propose("remove_breakpoint", buildJsonObject { put("id", bp.id) }); f.service.reject()
        assertEquals("REJECTED", f.service.operation(rejected.string("operationId")).string("status"))
        assertEquals(1, runner.breakpointRegistrations().size)
        assertEquals("SUCCEEDED", f.approve("remove_breakpoint", buildJsonObject { put("id", bp.id) }).string("status"))
        assertTrue(runner.breakpointRegistrations().isEmpty())
        assertEquals(cpu, runner.cpuSnapshot().programCounter); assertContentEquals(memory, runner.memorySnapshot(0xc000, 16).copyBytes())
    }

    @Test fun watchAndTraceUseCommonExecutionAndEachApprovedStepRunsExactlyOneInstruction() {
        val f = Fixture(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x42, 0x52, 0x33))
        val watch = f.approve("save_watch", buildJsonObject { put("startAddress", 0xc000); put("endAddress", 0xc000); put("kind", "write"); put("enabled", true) })
        assertEquals("SUCCEEDED", watch.string("status")); val id = watch["registrationId"]!!.jsonPrimitive.long
        assertEquals("SUCCEEDED", f.approve("trace", buildJsonObject { put("enabled", true); put("capacity", 16) }).string("status"))
        val before = f.runner.memoryByte(0xc000)
        for (expectedPc in listOf(3, 5, 6)) {
            val pending = f.propose("step")
            val oldPc = f.runner.cpuSnapshot().programCounter
            assertTrue(oldPc < expectedPc)
            f.service.approve()
            assertEquals("SUCCEEDED", f.service.operation(pending.string("operationId")).string("status"))
            assertEquals(expectedPc, f.runner.cpuSnapshot().programCounter)
            assertEquals(RunnerState.PAUSED, f.runner.state)
        }
        assertNotEquals(before, f.runner.memoryByte(0xc000)); assertEquals(0x42, f.runner.memoryByte(0xc000))
        assertEquals(3, f.runner.instructionTrace().size)
        assertIs<DebuggerStopReason.MemoryAccessed>(f.runner.stopReason)
        assertEquals("SUCCEEDED", f.approve("remove_watch", buildJsonObject { put("id", id) }).string("status"))
        assertTrue(f.runner.watchRegistrations().isEmpty())
    }

    @Test fun changedRevisionSessionResetAndLoadInvalidatePendingRequests() {
        val f = Fixture()
        val request = f.propose("save_breakpoint", buildJsonObject { put("address", 10); put("enabled", true) })
        f.runner.step(); f.service.approve()
        assertEquals("STALE_REVISION", f.service.operation(request.string("operationId")).string("code"))
        assertTrue(f.runner.breakpointRegistrations().isEmpty())
        val second = f.propose("step")
        f.state = f.state.copy(debugService = DesktopDebugContextService(f.runner, "different-session"))
        f.service.approve()
        assertEquals("STALE_SESSION", f.service.operation(second.string("operationId")).string("code"))
        f.state = f.state.copy(debugService = DesktopDebugContextService(f.runner, "debug-review"))
        f.runner.run()
        val pause = f.propose("pause")
        f.runner.reset(); f.runner.run(); f.service.approve()
        assertEquals("STALE_SESSION", f.service.operation(pause.string("operationId")).string("code"))
        assertEquals(RunnerState.RUNNING, f.runner.state)
        val loadPause = f.propose("pause")
        f.runner.loadBasicProgram(byteArrayOf()) // Even a failed load attempt invalidates a pending live pause.
        f.service.approve()
        assertEquals("STALE_SESSION", f.service.operation(loadPause.string("operationId")).string("code"))
    }

    @Test fun pauseTargetsTheSameLiveSessionWhileRevisionMayAdvance() {
        val f = Fixture()
        f.runner.run(); val revision = f.runner.sessionRevision
        val proposal = f.propose("pause")
        assertEquals(RunnerState.RUNNING, f.runner.state)
        f.runner.setInstructionTraceEnabled(true) // Live-session revision is not a frozen PC transaction.
        assertTrue(f.runner.sessionRevision > revision)
        f.service.approve()
        assertEquals("SUCCEEDED", f.service.operation(proposal.string("operationId")).string("status"))
        assertEquals(RunnerState.PAUSED, f.runner.state)
    }

    @Test fun malformedConditionsRangesKindsAndUnsupportedExecutionAreRejectedBeforeReview() {
        val f = Fixture()
        expect("CONDITION") { f.propose("save_breakpoint", buildJsonObject { put("address", 10); put("enabled", true); put("condition", "run(secret)") }) }
        expect("RANGE") { f.propose("save_watch", buildJsonObject { put("startAddress", 20); put("endAddress", 10); put("kind", "change"); put("enabled", true) }) }
        expect("WATCH_BYTES_LIMIT") { f.propose("save_watch", buildJsonObject { put("startAddress", 0); put("endAddress", 4096); put("kind", "change"); put("enabled", true) }) }
        expect("INVALID_ARGUMENTS") { f.propose("trace", buildJsonObject { put("enabled", true); put("capacity", 15) }) }
        expect("INVALID_ARGUMENTS") { f.propose("continue") }
        expect("NOT_FOUND") { f.propose("remove_watch", buildJsonObject { put("id", 999) }) }
        expect("STALE_SESSION") { f.service.propose("wrong", 0, DebugAction("step", buildJsonObject {})) }
        assertNull(f.service.review()); assertTrue(f.runner.breakpointRegistrations().isEmpty())
        f.runner.run()
        expect("SESSION_RUNNING") { f.propose("step") }
        f.runner.pause(); f.state = f.state.copy(busy = true)
        expect("BUSY") { f.propose("step") }
    }

    @Test fun authenticatedHttpProposalStatusAndFreshContextUseTheAwtDispatcher() {
        val f = Fixture()
        val credentialDirectory = java.nio.file.Files.createTempDirectory("pgp-debug-http-")
        val server = DesktopMcpServer(PgpMcpToolAdapter({ f.state.debugService }, AwtMcpApplicationDispatcher,
            debugChangesProvider = { f.service }), token = "debug-test-token", credentialStore = object : DesktopMcpCredentialStore {
                override fun publish(token: String) = DesktopMcpCredentialLocation(credentialDirectory.resolve("headers.json"), "unused-test-helper")
                override fun removeIfOwned(token: String) { }
            })
        try {
            val info = assertIs<DesktopMcpStartResult.Success>(server.start(0)).info
            val client = java.net.http.HttpClient.newHttpClient()
            fun call(name: String, args: JsonObject): JsonObject {
                val body = buildJsonObject { put("jsonrpc", "2.0"); put("id", 1); put("method", "tools/call")
                    put("params", buildJsonObject { put("name", name); put("arguments", args) }) }
                val request = java.net.http.HttpRequest.newBuilder(java.net.URI(info.url))
                    .header("Authorization", "Bearer debug-test-token").header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString())).build()
                val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                assertEquals(200, response.statusCode())
                return Json.parseToJsonElement(response.body()).jsonObject["result"]!!.jsonObject
            }
            val args = buildJsonObject { put("sessionId", "debug-review"); put("sessionRevision", 0); put("action", "step"); put("parameters", buildJsonObject {}) }
            val queued = call(DebugChangeMcp.PROPOSE, args)["structuredContent"]!!.jsonObject
            assertEquals("PENDING_APPROVAL", queued.string("status")); assertEquals(0, f.runner.cpuSnapshot().programCounter)
            javax.swing.SwingUtilities.invokeAndWait { f.service.approve() }
            val result = call(DebugChangeMcp.OPERATION, buildJsonObject { put("operationId", queued.string("operationId")) })["structuredContent"]!!.jsonObject
            assertEquals("SUCCEEDED", result.string("status"))
            val context = call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, buildJsonObject { put("disassemblyStartAddress", 2); put("disassemblyInstructionCount", 1); put("traceLimit", 0) })["structuredContent"]!!.jsonObject
            assertEquals(2, context["cpu"]!!.jsonObject["programCounter"]!!.jsonPrimitive.int)
            val stale = call(DebugChangeMcp.PROPOSE, args)["structuredContent"]!!.jsonObject
            assertEquals("STALE_REVISION", stale["error"]!!.jsonObject.string("code"))
        } finally { server.close(); credentialDirectory.toFile().deleteRecursively() }
    }

    @Test fun faultedStepAndReplacedRunnerAreRejectedAndHistoryStaysBounded() {
        val fault = Fixture(byteArrayOf(0x16))
        fault.runner.step()
        assertEquals(RunnerState.FAULTED, fault.runner.state)
        expect("SESSION_NOT_PAUSED") { fault.propose("step") }
        val f = Fixture()
        val request = f.propose("step")
        val other = debuggerTestRunner()
        f.state = f.state.copy(runner = other, debugService = DesktopDebugContextService(other, "debug-review"))
        f.service.approve()
        assertEquals("STALE_SESSION", f.service.operation(request.string("operationId")).string("code"))
        assertEquals(0, other.cpuSnapshot().programCounter)
        f.state = f.state.copy(runner = f.runner, debugService = DesktopDebugContextService(f.runner, "debug-review"))
        val first = f.propose("step"); f.service.reject()
        repeat(32) { f.propose("step"); f.service.reject() }
        assertEquals(32, f.service.history().size)
        expect("OPERATION_NOT_FOUND") { f.service.operation(first.string("operationId")) }
        assertEquals(0, f.runner.cpuSnapshot().programCounter)
    }

    @Test fun mcpValidatesTypesAndNeverOffersAnApprovalTool() {
        val f = Fixture()
        val adapter = PgpMcpToolAdapter({ f.state.debugService }, debugChangesProvider = { f.service })
        fun call(name: String, args: JsonObject) = adapter.call(name, args)
        val request = call(DebugChangeMcp.PROPOSE, buildJsonObject {
            put("sessionId", "debug-review"); put("sessionRevision", f.runner.sessionRevision); put("action", "step"); put("parameters", buildJsonObject {})
        })
        assertFalse(request["isError"]!!.jsonPrimitive.boolean)
        assertEquals(0, f.runner.cpuSnapshot().programCounter)
        assertTrue(call("pgp_approve_debug_action", buildJsonObject {})["isError"]!!.jsonPrimitive.boolean)
        f.service.reject()
        val malformed = call(DebugChangeMcp.PROPOSE, buildJsonObject {
            put("sessionId", "debug-review"); put("sessionRevision", "0"); put("action", "step"); put("parameters", buildJsonObject {})
        })
        assertEquals("INVALID_ARGUMENTS", malformed["structuredContent"]!!.jsonObject["error"]!!.jsonObject.string("code"))
        assertNull(f.service.review())
        val tools = adapter.tools().filter { it.jsonObject.string("name") == DebugChangeMcp.PROPOSE }.single().jsonObject
        assertFalse(tools["annotations"]!!.jsonObject["readOnlyHint"]!!.jsonPrimitive.boolean)
    }
}
