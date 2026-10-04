package com.digihori.pgp.core.api

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MachineCatalogTest {
    @Test
    fun exposesStableDefinitionsInUiOrder() {
        assertEquals(MachineId("pc-1245"), MachineCatalog.defaultDefinition.id)
        assertEquals(
            listOf("pc-1245", "pc-1250", "pc-1251", "pc-1255"),
            MachineCatalog.definitions.map { it.id.value },
        )
        assertEquals(
            MachineCatalog.definitions.size,
            MachineCatalog.definitions.map { it.id }.toSet().size,
            "machine IDs must be unique",
        )
        assertEquals(
            Pc1251FamilyModel.entries.map { it.machineId }.toSet(),
            MachineCatalog.definitions.filter { it.family == MachineFamily.PC_1251 }.map { it.id }.toSet(),
        )
    }

    @Test
    fun describesFamilyCapabilitiesAndTiming() {
        val pc1245 = MachineCatalog.require(MachineId("pc-1245"))
        val pc1255 = MachineCatalog.require(MachineId("pc-1255"))

        assertEquals(MachineFamily.PC_1245, pc1245.family)
        assertEquals(MachineGeneration.OLD, pc1245.generation)
        assertEquals(MachineMemoryBanking.None, pc1245.memoryBanking)
        assertEquals(16, pc1245.characterColumns)
        assertEquals(setOf(OperatingMode.RUN, OperatingMode.PROGRAM), pc1245.supportedOperatingModes)
        assertTrue(!pc1245.supportsConfigurableRam)

        assertEquals(MachineFamily.PC_1251, pc1255.family)
        assertEquals(24, pc1255.characterColumns)
        assertTrue(OperatingMode.RESERVE in pc1255.supportedOperatingModes)
        assertTrue(pc1255.supportsConfigurableRam)
        assertEquals(38_400L, pc1255.automaticKeyHoldCycles)
    }

    @Test
    fun returnsNullForUnknownMachine() {
        assertNull(MachineCatalog.find(MachineId("pc-9999")))
    }
}
