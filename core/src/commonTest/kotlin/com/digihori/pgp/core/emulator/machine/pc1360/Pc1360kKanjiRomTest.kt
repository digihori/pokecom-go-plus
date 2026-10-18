package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.*
import kotlin.test.*

class Pc1360kKanjiRomTest {
    private fun pattern(selector: Int, address: Int) = (address xor (address ushr 8) xor (selector * 0x5a)) and 255
    private fun imported() = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
        ByteArray(0x2000), ByteArray(0x20000), Pc1360RomDefinition.PC1360K_MACHINE_ID,
        List(4) { selector -> ByteArray(0x10000) { pattern(selector, it).toByte() } },
    )).romSet

    @Test
    fun bothReadInstructionsUseSelectorAndDpWithoutChangingOrdinaryMemoryOrBank() {
        val machine = Pc1360Machine(imported())
        listOf(0x56, 0x03, 0x6d, 0x54, 0x03, 0x6d).forEachIndexed { i, value -> machine.writeMemory(0xc000 + i, value) }
        machine.writeMemory(0xc100, 0xa5)
        for (selector in 0..3) for (bank in listOf(0, 7)) for (dp in listOf(0, 0x3fff, 0x4000, 0x7fff, 0x8000, 0xc100, 0xffff)) {
            machine.writeMemory(0x3600, selector); machine.writeMemory(0x3400, bank)
            machine.cpuState.apply { programCounter = 0xc000; dataPointer = dp; p = 0x20 }
            machine.setMemoryAccessTracing(true); machine.drainMemoryAccesses()
            assertNull(machine.step().stopReason)
            assertEquals(pattern(selector, dp), machine.cpuState.internalRam[2])
            assertEquals(0xc001, machine.cpuState.programCounter, "READ does not consume following LIB")
            val accesses = machine.drainMemoryAccesses()
            assertEquals(listOf(0xc000, dp), accesses.map { it.address })
            assertEquals(pattern(selector, dp), accesses.last().value)
            assertNull(machine.step().stopReason) // LIB 6D
            assertNull(machine.step().stopReason) // READM -> (P)
            assertEquals(pattern(selector, dp), machine.cpuState.internalRam[0x20])
            assertEquals(0xc004, machine.cpuState.programCounter)
            assertEquals(0xa5, machine.readMemory(0xc100))
            assertEquals(bank, machine.selectedRomBank)
            machine.setMemoryAccessTracing(false)
        }
        machine.writeMemory(0x3600, 4)
        machine.cpuState.apply { programCounter = 0xc000; dataPointer = 0xc100 }
        machine.step()
        assertEquals(0xff, machine.cpuState.internalRam[2], "Unmeasured selector values do not silently alias 0..3")
        machine.coldReset()
        machine.cpuState.apply { programCounter = 0xc000; dataPointer = 0xc100 }
        machine.writeMemory(0xc000, 0x56); machine.step()
        assertEquals(pattern(0, 0xc100), machine.cpuState.internalRam[2], "Reset retains firmware and resets selector")
    }

    @Test
    fun missingPairBadSizeWrongRoleAndStandardMachineAreRejected() {
        val rom = imported()
        val standardComponents = rom.components.filter { it.role != RomRole.KANJI }
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        fun rejected(components: List<RomComponent>, machineId: MachineId = id) {
            assertIs<CreateSessionResult.Failure>(EmulatorFactory.create(machineId, RomSet(machineId, components)))
        }
        rejected(standardComponents + rom.components.first { it.id == Pc1360RomDefinition.kanjiId(0) })
        rejected(rom.components.map { if (it.id == Pc1360RomDefinition.kanjiId(1)) RomComponent(it.id, RomRole.KANJI, ByteArray(1)) else it })
        rejected(rom.components.map { if (it.id == Pc1360RomDefinition.kanjiId(0)) RomComponent(it.id, RomRole.EXTERNAL, it.copyBytes()) else it })
        rejected(rom.components, Pc1360RomDefinition.MACHINE_ID)
        assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, standardComponents)))
        val partial = RomSet(id, standardComponents + rom.components.filter { it.id in listOf(Pc1360RomDefinition.kanjiId(0), Pc1360RomDefinition.kanjiId(1)) })
        val partialBus = Pc1360MemoryBus(partial)
        partialBus.write(0x3600, 1)
        assertEquals(pattern(1, 0xc100), partialBus.readProgramData(0, 0xc100).value)
        partialBus.write(0x3600, 2)
        assertEquals(0xff, partialBus.readProgramData(0, 0xc100).value)
        rejected(standardComponents + rom.components.filter { it.id in listOf(Pc1360RomDefinition.kanjiId(2), Pc1360RomDefinition.kanjiId(3)) })
        rejected(rom.components.filter { it.id != Pc1360RomDefinition.kanjiId(3) })
        assertIs<RomImportResult.Failure>(Pc1360LegacyRomImporter.importImages(ByteArray(8192), ByteArray(131072), id, listOf(ByteArray(65536))))
        assertIs<RomImportResult.Failure>(Pc1360LegacyRomImporter.importImages(ByteArray(8192), ByteArray(131072), id, List(2) { ByteArray(1) }))
    }
}
