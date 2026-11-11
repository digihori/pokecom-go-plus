package com.digihori.pgp.desktop.application.execution

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.desktop.runner.*
import java.nio.file.Files
import java.util.Base64
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlinx.serialization.json.*
import kotlin.test.*

class BoundedExecutionServiceTest {
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun fixture(source: String = "ORG 0\nLIA 1\nJP 0\n", block: (BoundedExecutionService, PgpMcpToolAdapter, DesktopEmulatorRunner, () -> JsonObject, (DevelopmentReadState) -> Unit, DevelopmentReadState) -> Unit) {
        val root = Files.createTempDirectory("pgp-bounded-")
        try {
            val runner = boundedTestRunner(source)
            val project = GeneratedProjectFiles.create(root, "project", "Bounded", listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 END\n")))
            var state = DevelopmentReadState(runner = runner, debugService = DesktopDebugContextService(runner, "test-session"), project = project)
            val changes = DevelopmentChangeService({ state })
            val service = BoundedExecutionService({ state }, { changes.projectRevision() })
            val adapter = PgpMcpToolAdapter({ state.debugService }, boundedProvider = { service }, displayProvider = { DisplayReadService { state } })
            fun args() = buildJsonObject {
                put("sessionId", "test-session"); put("sessionRevision", runner.sessionRevision); put("projectRevision", changes.projectRevision())
                put("action", "continue"); put("maxInstructions", 7); put("maxCycles", 10000)
            }
            block(service, adapter, runner, ::args, { state = it }, state)
        } finally { root.toFile().deleteRecursively() }
    }
    @Test fun pc1251BoundedRunAnd120ColumnDisplayUseActualMachineAndRejectMismatchedProject() {
        val root = Files.createTempDirectory("pgp-pc1251-bounded-")
        try {
            val runner = boundedTestRunner("ORG 0\nLIDP 0xF800\nLIA 0x41\nSTD\nJP 0\n", "pc-1251")
            val project = GeneratedProjectFiles.create(root, "project", "PC1251", listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 END\n")), "pc-1251")
            var state = DevelopmentReadState(runner = runner, debugService = DesktopDebugContextService(runner, "1251-session"), project = project)
            val changes = DevelopmentChangeService({ state })
            val service = BoundedExecutionService({ state }, { changes.projectRevision() })
            val adapter = PgpMcpToolAdapter({ state.debugService }, boundedProvider = { service }, displayProvider = { DisplayReadService { state } })
            fun args(action: String = "continue", address: Int? = null) = buildJsonObject {
                put("sessionId", "1251-session"); put("sessionRevision", runner.sessionRevision); put("projectRevision", changes.projectRevision())
                put("action", action); put("maxInstructions", 3); put("maxCycles", 1000)
                address?.let { put("callAddress", it) }
            }
            val id = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
            assertEquals(0, runner.cpuSnapshot().programCounter)
            service.approve(); runner.tick(); service.observe()
            assertEquals("pc-1251", service.operation(id).string("machineId"))
            assertEquals("instructionLimit", service.operation(id).string("stopReason"))
            val display = DisplayReadService { state }.read("1251-session", runner.sessionRevision)
            val pixels = display.getValue("structuredContent").jsonObject
            assertEquals(120, pixels.getValue("dotColumns").jsonPrimitive.int)
            assertEquals(24, pixels.getValue("characterColumns").jsonPrimitive.int)
            assertEquals('1', pixels.getValue("pixels").jsonArray[0].jsonPrimitive.content[0])
            val png = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(display.getValue("content").jsonArray[1].jsonObject.string("data"))))
            assertEquals(882, png.width)
            error(adapter, BoundedExecutionMcp.PROPOSE, args("call", 0xf800), "INVALID_ARGUMENTS")
            error(adapter, BoundedExecutionMcp.PROPOSE, args("call", 0x9000), "INVALID_ARGUMENTS")
            val call = adapter.call(BoundedExecutionMcp.PROPOSE, args("call", 0xb800))
            assertEquals(JsonPrimitive(false), call["isError"]); service.reject()
            val other = GeneratedProjectFiles.create(root, "other", "Other", listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 END\n")))
            state = state.copy(project = other)
            error(adapter, BoundedExecutionMcp.PROPOSE, args(), "MATCHING_SUPPORTED_PROJECT_REQUIRED")
        } finally { root.toFile().deleteRecursively() }
    }

    @Test fun pc1251CallChecksHardwareRamInsteadOfExpandedCatalogDefaults() {
        val root = Files.createTempDirectory("pgp-pc1251-hardware-")
        try {
            val runner = boundedTestRunner(machineId = "pc-1251", configuration = EmulatorConfiguration(
                ramMode = RamMode.HARDWARE))
            val project = GeneratedProjectFiles.create(root, "project", "PC1251", listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 END\n")), "pc-1251")
            val state = DevelopmentReadState(runner = runner, debugService = DesktopDebugContextService(runner, "hardware-session"), project = project)
            val changes = DevelopmentChangeService({ state })
            val service = BoundedExecutionService({ state }, { changes.projectRevision() })
            val adapter = PgpMcpToolAdapter({ state.debugService }, boundedProvider = { service })
            fun args(address: Int) = buildJsonObject {
                put("sessionId", "hardware-session"); put("sessionRevision", runner.sessionRevision); put("projectRevision", changes.projectRevision())
                put("action", "call"); put("callAddress", address); put("maxInstructions", 1); put("maxCycles", 1000)
            }
            error(adapter, BoundedExecutionMcp.PROPOSE, args(0xa000), "CALL_ADDRESS_NOT_RAM")
            error(adapter, BoundedExecutionMcp.PROPOSE, args(0xb000), "CALL_ADDRESS_NOT_RAM")
            assertEquals(JsonPrimitive(false), adapter.call(BoundedExecutionMcp.PROPOSE, args(0xb800))["isError"])
            service.reject(); assertEquals(0, runner.cpuSnapshot().programCounter)
        } finally { root.toFile().deleteRecursively() }
    }

    private fun error(adapter: PgpMcpToolAdapter, name: String, args: JsonObject, code: String) {
        val response = adapter.call(name, args)
        assertEquals(JsonPrimitive(true), response["isError"])
        assertEquals(code, response.getValue("structuredContent").jsonObject.getValue("error").jsonObject.string("code"))
    }
    @Test fun proposalsDoNotExecuteUntilApprovalAndTerminalEvidenceSurvivesLaterSteps() = fixture { service, adapter, runner, args, _, _ ->
        val revision = runner.sessionRevision
        val queued = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject
        val id = queued.string("operationId")
        assertEquals(revision, runner.sessionRevision); assertEquals(RunnerState.PAUSED, runner.state)
        service.reject(); assertEquals("REJECTED", service.operation(id).string("status"))
        val next = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
        service.approve(); assertEquals("RUNNING", service.operation(next).string("status"))
        runner.tick(); service.observe()
        val result = service.operation(next)
        assertEquals("SUCCEEDED", result.string("status")); assertEquals("instructionLimit", result.string("stopReason"))
        assertEquals("notDetermined", result.string("programFinished")); assertEquals("7", result.string("executedInstructions"))
        val context = assertIs<DebugContextResult.Success>(DesktopDebugContextService(runner).capture(DebugContextRequest())).context
        val stop = Json.parseToJsonElement(DebugContextJsonWriter.write(context).decodeToString()).jsonObject.getValue("stop").jsonObject
        assertEquals("executionLimit", stop.string("kind")); assertEquals("instructionLimit", stop.string("limit"))
        runner.step(); assertEquals(result, service.operation(next))
    }
    @Test fun changedRevisionProjectOrModeRejectsApprovalAndServerCancellationStopsOnlyOwnedRun() = fixture { service, adapter, runner, args, _, state ->
        val id = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
        runner.step(); service.approve(); assertEquals("STALE_REVISION", service.operation(id).string("code"))
        val next = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
        state.project!!.sources.single().file.appendText("20 END\n"); service.approve()
        assertEquals("STALE_PROJECT", service.operation(next).string("code"))
        val running = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
        service.approve(); service.cancel()
        assertEquals("INTERRUPTED", service.operation(running).string("status")); assertEquals(RunnerState.PAUSED, runner.state)
        runner.run(); service.cancel(); assertEquals(RunnerState.RUNNING, runner.state); runner.pause()
        runner.setOperatingMode(OperatingMode.PROGRAM)
        error(adapter, BoundedExecutionMcp.PROPOSE, args(), "RUN_MODE_REQUIRED")
    }
    @Test fun projectSwitchInterruptsOldTargetAndDoesNotRunTheReplacementSession() = fixture { service, adapter, runner, args, set, state ->
        val id = adapter.call(BoundedExecutionMcp.PROPOSE, args()).getValue("structuredContent").jsonObject.string("operationId")
        service.approve(); set(state.copy(project = state.project!!.copy()))
        service.observe(); assertEquals("INTERRUPTED", service.operation(id).string("status"))
        assertEquals(RunnerState.PAUSED, runner.state)
    }
    @Test fun strictActionSchemaBuildsExplicitRunCallAndNumericKeysWithoutPathsOrPcWrites() = fixture { service, adapter, _, args, _, _ ->
        val base = args()
        for (extra in listOf("pc" to JsonPrimitive(49152), "approve" to JsonPrimitive(true), "keys" to JsonArray(emptyList())))
            error(adapter, BoundedExecutionMcp.PROPOSE, JsonObject(base + extra), "INVALID_ARGUMENTS")
        error(adapter, BoundedExecutionMcp.PROPOSE, JsonObject(base + ("maxInstructions" to JsonPrimitive("7"))), "INVALID_ARGUMENTS")
        error(adapter, BoundedExecutionMcp.PROPOSE, JsonObject(base + ("maxCycles" to JsonPrimitive(5000001))), "INVALID_ARGUMENTS")
        for (action in listOf("run", "call", "keys")) {
            val request = JsonObject(base + ("action" to JsonPrimitive(action)) + when (action) {
                "call" -> mapOf("callAddress" to JsonPrimitive(0xc000))
                "keys" -> mapOf("keys" to buildJsonArray { add("NUM_1"); add("ENTER") })
                else -> emptyMap()
            })
            val response = adapter.call(BoundedExecutionMcp.PROPOSE, request)
            assertEquals(JsonPrimitive(false), response["isError"])
            val keys = service.review()!!.keys
            if (action == "call") assertEquals("CLEAR C A L L SPACE NUM_4 NUM_9 NUM_1 NUM_5 NUM_2 ENTER", keys.joinToString(" ") { it.name })
            if (action == "run") assertEquals(listOf(PocketKey.CLEAR, PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER), keys)
            service.reject()
        }
        error(adapter, BoundedExecutionMcp.PROPOSE, JsonObject(base + mapOf("action" to JsonPrimitive("keys"), "keys" to buildJsonArray { add("MODE") })), "INVALID_ARGUMENTS")
    }
    @Test fun displayIsRevisionBoundReadOnlyAndReturnsPixelsAndAValidPngWithoutOcrOrRomBytes() = fixture { _, adapter, runner, _, _, _ ->
        fun request() = buildJsonObject { put("sessionId", "test-session"); put("sessionRevision", runner.sessionRevision) }
        val revision = runner.sessionRevision
        val result = adapter.call(BoundedExecutionMcp.DISPLAY, request())
        assertEquals(JsonPrimitive(false), result["isError"])
        val data = result.getValue("structuredContent").jsonObject
        assertEquals(7, data.getValue("pixels").jsonArray.size)
        assertTrue(data.getValue("pixels").jsonArray.all { it.jsonPrimitive.content.length == 80 })
        assertFalse(data.containsKey("text")); assertFalse(data.containsKey("rom"))
        val png = Base64.getDecoder().decode(result.getValue("content").jsonArray[1].jsonObject.string("data"))
        assertEquals(594, ImageIO.read(ByteArrayInputStream(png)).width)
        assertEquals(revision, runner.sessionRevision)
        val stale = request(); runner.step(); error(adapter, BoundedExecutionMcp.DISPLAY, stale, "STALE_REVISION")
        runner.run(); error(adapter, BoundedExecutionMcp.DISPLAY, request(), "SESSION_RUNNING"); runner.pause()
    }
    @Test fun visiblePixelsMatchLcdColumnsAndDisabledDisplayDoesNotExposeHiddenDots() {
        for (enabled in listOf(true, false)) fixture("ORG 0\nLIDP 0xF800\nLIA 0x7F\nSTD\nLIP 0x5F\n${if (enabled) "ORIM 1" else "ANIM 0xFE"}\nOUTC\nJP 0\n") { _, adapter, runner, _, _, _ ->
            runner.startBoundedExecution("draw", emptyList(), 6, 1000); runner.tick()
            assertEquals(0x7f, runner.memoryByte(0xf800))
            val response = adapter.call(BoundedExecutionMcp.DISPLAY, buildJsonObject { put("sessionId", "test-session"); put("sessionRevision", runner.sessionRevision) })
            val data = response.getValue("structuredContent").jsonObject
            assertEquals(JsonPrimitive(enabled), data["enabled"])
            assertTrue(data.getValue("pixels").jsonArray.all { row -> row.jsonPrimitive.content == (if (enabled) "1" else "0") + "0".repeat(79) })
        }
    }
}
