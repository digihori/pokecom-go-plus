package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopDebuggerCheckpointWriterTest {
    @Test
    fun writesVersionedMachineReadableCheckpoint() {
        val romSet = RomSet(
            Pc1245RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000) { 0x33 }),
                RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
            ),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, romSet),
        ).session

        val bytes = DesktopDebuggerCheckpointWriter.write(
            machineId = session.machineId.value,
            capturedAt = "2026-10-06T00:00:00Z",
            executedCycles = 123,
            cpu = session.cpuSnapshot(),
            memory = session.memorySnapshot(0xc000, 4),
            trace = emptyList(),
            stopReason = "Reset",
        )
        val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject

        assertEquals("pgp-debug-checkpoint", root.getValue("format").jsonPrimitive.content)
        assertEquals("1", root.getValue("formatVersion").jsonPrimitive.content)
        assertEquals("pc-1245", root.getValue("machineId").jsonPrimitive.content)
        assertEquals("C000", root.getValue("memory").jsonObject.getValue("startAddress").jsonPrimitive.content)
        assertEquals("00000000", root.getValue("memory").jsonObject.getValue("bytes").jsonPrimitive.content)
    }
}
