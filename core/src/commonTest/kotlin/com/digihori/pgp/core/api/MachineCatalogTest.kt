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
            listOf("pc-1245", "pc-1250", "pc-1251", "pc-1255", "pc-1350", "pc-1360"),
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
        assertEquals(1, pc1245.characterRows)
        assertEquals(setOf(OperatingMode.RUN, OperatingMode.PROGRAM), pc1245.supportedOperatingModes)
        assertTrue(!pc1245.supportsConfigurableRam)

        assertEquals(MachineFamily.PC_1251, pc1255.family)
        assertEquals(24, pc1255.characterColumns)
        assertTrue(OperatingMode.RESERVE in pc1255.supportedOperatingModes)
        assertTrue(pc1255.supportsConfigurableRam)
        assertEquals(38_400L, pc1255.automaticKeyHoldCycles)
    }

    @Test
    fun describesDebuggerMemoryRegionsForEachMachine() {
        MachineCatalog.definitions.forEach { definition ->
            assertTrue(definition.memoryRegions.any { it.kind == MachineMemoryRegionKind.ROM })
            assertTrue(definition.memoryRegions.any { it.kind == MachineMemoryRegionKind.RAM })
            assertTrue(definition.memoryRegions.any { it.kind == MachineMemoryRegionKind.DISPLAY })
        }

        val pc1251 = MachineCatalog.require(MachineId("pc-1251"))
        val ram = pc1251.memoryRegions.single { it.id == "ram" }
        val ramMirror = pc1251.memoryRegions.single { it.startAddress == 0xb000 }
        assertEquals(0xb800, ram.startAddress)
        assertEquals(0xc7ff, ram.endAddressInclusive)
        assertEquals("ram", ramMirror.mirrorsRegionId)

        val pc1255 = MachineCatalog.require(MachineId("pc-1255"))
        assertEquals(0xa000, pc1255.memoryRegions.single { it.id == "ram" }.startAddress)
        assertTrue(pc1255.memoryRegions.any { it.startAddress == 0x8000 && it.endAddressInclusive == 0x9fff })

        val pc1350 = MachineCatalog.require(MachineId("pc-1350"))
        assertEquals(MachineGeneration.S1, pc1350.generation)
        assertEquals(25, pc1350.characterColumns)
        assertEquals(4, pc1350.characterRows)
        assertEquals(0x8000, pc1350.memoryRegions.single { it.id == "external-rom" }.startAddress)

        val pc1360 = MachineCatalog.require(MachineId("pc-1360"))
        assertEquals(MachineGeneration.S2, pc1360.generation)
        assertEquals(MachineBasicDialect.S2, pc1360.basicDialect)
        assertEquals(MachineMemoryBanking.Banked(0x4000, 0x7fff, 8), pc1360.memoryBanking)
    }

    @Test
    fun returnsNullForUnknownMachine() {
        assertNull(MachineCatalog.find(MachineId("pc-9999")))
    }
}
