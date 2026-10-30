package com.digihori.pgp.desktop.debug

import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.mcp.*
import kotlinx.serialization.json.*
import kotlin.test.*

class DebuggerBankBreakpointMcpTest {
    @Test fun bankCreationPreservationAndClearingRequireApprovalAndReadBackMatches() {
        val runner = bankBreakpointRunner()
        val debug = DesktopDebugContextService(runner, "bank-mcp")
        val state = DevelopmentReadState(runner = runner, debugService = debug)
        val development = DesktopDevelopmentReadService { state }
        val changes = DebugChangeService({ state }, {}, { it.pause() }, { it.step() })
        val adapter = PgpMcpToolAdapter({ debug }, developmentProvider = { development }, debugChangesProvider = { changes })
        fun propose(parameters: JsonObject) = adapter.call(DebugChangeMcp.PROPOSE, buildJsonObject {
            put("sessionId", "bank-mcp"); put("sessionRevision", runner.sessionRevision)
            put("action", "save_breakpoint"); put("parameters", parameters)
        })
        fun parameters(id: Long? = null, bank: JsonElement? = null) = buildJsonObject {
            id?.let { put("id", it) }; put("address", 0x4000); put("enabled", true)
            bank?.let { put("bank", it) }
        }
        val request = propose(parameters(bank = JsonPrimitive(1)))
        assertFalse(request.getValue("isError").jsonPrimitive.boolean)
        assertTrue(runner.breakpointRegistrations().isEmpty())
        assertTrue(changes.review()!!.after.contains("bank = 1"))
        changes.approve()
        val point = runner.breakpointRegistrations().single()
        assertEquals(1, point.bank)
        val result = adapter.call(PgpMcpToolAdapter.GET_DEBUG_REGISTRATIONS, buildJsonObject {})
        assertEquals(1, result.getValue("structuredContent").jsonObject.getValue("breakpoints").jsonArray.single().jsonObject.getValue("bank").jsonPrimitive.int)
        val context = development.context("debug", null, 0, 10)
        assertEquals(1, context.getValue("breakpoints").jsonArray.single().jsonObject.getValue("bank").jsonPrimitive.int)
        assertFalse(propose(parameters(point.id)).getValue("isError").jsonPrimitive.boolean)
        assertTrue(changes.review()!!.after.contains("bank = 1"))
        changes.approve()
        assertEquals(1, runner.breakpointRegistrations().single().bank)
        assertFalse(propose(parameters(point.id, JsonNull)).getValue("isError").jsonPrimitive.boolean)
        changes.approve()
        assertNull(runner.breakpointRegistrations().single().bank)
        for (bad in listOf(JsonPrimitive("1"), JsonPrimitive(1.5), JsonPrimitive(-1), JsonPrimitive(8))) {
            assertTrue(propose(parameters(point.id, bad)).getValue("isError").jsonPrimitive.boolean)
            assertNull(changes.review())
            assertNull(runner.breakpointRegistrations().single().bank)
        }
        val schema = DebugChangeMcp.tools().first().getValue("inputSchema").jsonObject
        val variant = schema.getValue("oneOf").jsonArray.first().jsonObject
        assertTrue(variant.getValue("properties").jsonObject.getValue("parameters").jsonObject.getValue("properties").jsonObject.containsKey("bank"))
    }
}
