package com.digihori.pgp.desktop.localization

import com.digihori.pgp.controller.ui.ControllerStrings
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.input.*
import com.digihori.pgp.desktop.codexMcpConfiguration
import com.digihori.pgp.desktop.application.debug.DesktopDebugContextService
import com.digihori.pgp.desktop.mcp.*
import kotlinx.serialization.json.*
import kotlin.test.*

class StudioMcpControllerStringsTest {
    private val en = StudioStrings(StudioLanguage.ENGLISH)
    private val ja = StudioStrings(StudioLanguage.JAPANESE)

    @Test fun requestDescriptionChangesOnlyResultLabel() {
        assertEquals("No authenticated requests yet.", en.mcpRequestDescription(null))
        for (tool in listOf(null, "pgp_get_debug_context")) for (succeeded in listOf(false, true)) {
            val request = DesktopMcpRequestSummary("2026-10-14T00:00:00Z", "tools/call", tool, succeeded)
            val original = request.copy()
            val target = tool?.let { " / $it" }.orEmpty()
            assertEquals("${request.capturedAt} — tools/call$target — ${if (succeeded) "success" else "failed"}", en.mcpRequestDescription(request))
            val translated = ja.mcpRequestDescription(request)
            assertTrue(translated.contains("tools/call"))
            assertTrue(translated.contains(request.capturedAt))
            if (tool != null) assertTrue(translated.contains(tool))
            assertTrue(translated.endsWith(if (succeeded) "成功" else "失敗"))
            assertEquals(original, request)
        }
    }

    @Test fun startupErrorsAndCredentialValuesAreUiOnly() {
        for (message in listOf("The filesystem does not support secure POSIX permissions or Windows ACLs",
            "MCP credential file permissions are not owner-only", "Could not verify MCP credential file access",
            "MCP credential file ACL is not owner-only", "Cannot determine the user home directory",
            "LOCALAPPDATA is not available", "Could not start MCP server")) {
            assertTrue(en.mcpStartError(message).contains(message))
            assertNotEquals(en.mcpStartError(message), ja.mcpStartError(message))
        }
        val raw = "Address already in use: /日本語/{path}/1.25"
        assertTrue(ja.mcpStartError(raw).contains(raw))
        val info = DesktopMcpServerInfo("http://127.0.0.1:8765/mcp", "synthetic-preview-token", 8765,
            "/bin/cat '/example/日本語/headers.json'", "/example/日本語/headers.json")
        val configuration = codexMcpConfiguration(info)
        for (strings in listOf(en, ja)) {
            assertTrue(strings.text("mcp.url", info.url).contains(info.url))
            assertTrue(strings.text("mcp.token", info.token).contains(info.token))
            assertTrue(strings.text("mcp.credential", info.credentialPath).contains(info.credentialPath))
            assertEquals(configuration, codexMcpConfiguration(info))
        }
        assertTrue(configuration.contains("http_headers_helper"))
        assertTrue(configuration.contains("pgp_get_capabilities"))
        assertTrue(configuration.contains("pgp_get_debug_context"))
        assertFalse(configuration.contains(info.token))
    }

    @Test fun protocolToolsSchemasAndResponsesStayIdentical() {
        val service = DesktopDebugContextService(debuggerTestRunner(), sessionId = "localization-test",
            capturedAtProvider = { "2026-10-14T00:00:00Z" }, executedCyclesProvider = { 123L })
        val adapter = PgpMcpToolAdapter({ service })
        val args = buildJsonObject { putJsonArray("memoryRanges") { addJsonObject { put("startAddress", 49152); put("length", 4) } } }
        val tools = adapter.tools().toString()
        val success = adapter.call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, args).toString()
        val invalid = adapter.call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, buildJsonObject { put("unexpected", 1) }).toString()
        for (strings in listOf(en, ja)) {
            strings.mcpRequestDescription(DesktopMcpRequestSummary("2026-10-14T00:00:00Z", "tools/call", PgpMcpToolAdapter.GET_DEBUG_CONTEXT, true))
            strings.mcpStartError("MCP credential file ACL is not owner-only")
            assertEquals(tools, adapter.tools().toString())
            assertEquals(success, adapter.call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, args).toString())
            assertEquals(invalid, adapter.call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, buildJsonObject { put("unexpected", 1) }).toString())
        }
        assertTrue(invalid.contains("INVALID_ARGUMENTS"))
        assertTrue(success.contains("pc-1245"))
    }

    @Test fun sharedJapaneseDefaultsAndSavedBindingsRemainCompatible() {
        val defaults = ControllerStrings()
        val japanese = ja.controllerStrings()
        assertEquals(defaults.title, japanese.title)
        assertEquals(defaults.enable, japanese.enable)
        assertEquals(defaults.instructions, japanese.instructions)
        assertEquals(defaults.positions, japanese.positions)
        assertEquals(defaults.saveHelp, japanese.saveHelp)
        assertEquals(defaults.pressed, japanese.pressed)
        assertEquals(defaults.released, japanese.released)
        assertEquals(defaults.unavailableShort, japanese.unavailableShort)
        assertEquals("N/A", en.controllerStrings().unavailableShort)
        assertEquals("Unavailable", en.controllerStrings().unavailable)
        for (button in GamepadButton.entries) assertEquals(defaults.buttonLabel(button), japanese.buttonLabel(button))
        val settings = GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.ENTER, GamepadButton.LEFT to PocketKey.NUM_4))
        val values = settings.preferenceValues()
        val keys = PocketKey.entries.associateWith { GamepadKeys.label(it) }
        for (strings in listOf(en, ja)) {
            val labels = strings.controllerStrings()
            labels.assignment(labels.buttonLabel(GamepadButton.SOUTH))
            assertTrue(labels.pressed.isNotBlank())
            assertTrue(labels.released.isNotBlank())
            assertEquals(values, settings.preferenceValues())
            assertEquals(settings, GamepadSettings.fromPreferences { values[it] })
            assertEquals(keys, PocketKey.entries.associateWith { GamepadKeys.label(it) })
        }
    }
}
