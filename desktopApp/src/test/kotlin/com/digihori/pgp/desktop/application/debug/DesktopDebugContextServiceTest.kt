package com.digihori.pgp.desktop.application.debug

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopDebugContextServiceTest {
    @Test
    fun capturesVersionedContextForExplicitRamRange() {
        val runner = runner()
        val service = DesktopDebugContextService(
            runner = runner,
            sessionId = "session-test",
            capturedAtProvider = { "2026-10-10T00:00:00Z" },
            executedCyclesProvider = { 123L },
        )

        assertEquals(1, service.capabilities().schemaVersion)
        assertEquals(false, service.capabilities().romBytesAllowed)

        val result = assertIs<DebugContextResult.Success>(
            service.capture(
                DebugContextRequest(
                    memoryRanges = listOf(DebugMemoryRangeRequest(0xc000, 4)),
                    disassemblyStartAddress = 0,
                    disassemblyInstructionCount = 2,
                ),
            ),
        )

        assertEquals("session-test", result.context.session.id)
        assertEquals("pc-1245", result.context.session.machineId)
        assertEquals("ram", result.context.memory.single().regionId)
        assertEquals(2, result.context.disassembly.size)

        val root = Json.parseToJsonElement(DebugContextJsonWriter.write(result.context).decodeToString()).jsonObject
        val jsonText = DebugContextJsonWriter.write(result.context).decodeToString()
        assertEquals(DebugContextV1.FORMAT, root.getValue("format").jsonPrimitive.content)
        assertEquals("1", root.getValue("schemaVersion").jsonPrimitive.content)
        assertEquals(4, root.getValue("memory").jsonArray.single().jsonObject.getValue("byteLength").jsonPrimitive.content.toInt())
        assertFalse(jsonText.contains("apiKey", ignoreCase = true))
        assertFalse(jsonText.contains("filePath", ignoreCase = true))
    }

    @Test
    fun rejectsRomBytesAndDoesNotPauseRunningSession() {
        val runner = runner()
        val service = DesktopDebugContextService(runner)

        assertIs<DebugContextError.RomBytesNotAllowed>(
            assertIs<DebugContextResult.Failure>(
                service.capture(DebugContextRequest(memoryRanges = listOf(DebugMemoryRangeRequest(0, 1)))),
            ).error,
        )

        runner.run()
        assertIs<DebugContextError.SessionRunning>(
            assertIs<DebugContextResult.Failure>(service.capture(DebugContextRequest())).error,
        )
        assertTrue(runner.state.name == "RUNNING")
    }

    @Test
    fun rejectsOversizedMemoryRequests() {
        val service = DesktopDebugContextService(runner())
        val result = assertIs<DebugContextResult.Failure>(
            service.capture(DebugContextRequest(memoryRanges = listOf(DebugMemoryRangeRequest(0xc000, 4097)))),
        )
        assertIs<DebugContextError.InvalidRequest>(result.error)
    }

    private fun runner(): DesktopEmulatorRunner {
        val romSet = RomSet(
            Pc1245RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
            ),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, romSet),
        ).session
        return DesktopEmulatorRunner(session)
    }
}
