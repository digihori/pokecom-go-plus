package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1251CoreComponentsTest {
    @Test
    fun importsSeparatePhysicalRomDumps() {
        val internal = ByteArray(0x2000) { 0x11 }
        val external = ByteArray(0x4000) { 0x22 }

        val result = assertIs<RomImportResult.Success>(Pc1251ComponentRomImporter.importImages(internal, external))

        assertEquals(Pc1251RomDefinition.MACHINE_ID, result.romSet.machineId)
        assertContentEquals(internal, result.romSet.component(Pc1251RomDefinition.INTERNAL_ID)?.copyBytes())
        assertContentEquals(external, result.romSet.component(Pc1251RomDefinition.EXTERNAL_ID)?.copyBytes())
    }

    @Test
    fun mapsOnlyThePhysicalRamAndLcdRanges() {
        val bus = Pc1251MemoryBus(romSet())

        bus.write(0xb000, 0x12)
        bus.write(0xc7ff, 0x34)
        bus.write(0xa000, 0x56)
        bus.write(0xf800, 0x78)

        assertEquals(0x12, bus.read(0xb000))
        assertEquals(0x12, bus.read(0xb800))
        assertEquals(0x34, bus.read(0xc7ff))
        assertEquals(0x56, bus.read(0xa000))
        assertEquals(0x78, bus.read(0xf800))
    }

    @Test
    fun mapsTheScratchRamAliasesUsedByMachineLanguageSoundRoutines() {
        val bus = Pc1251MemoryBus(romSet())

        bus.write(0x8000, 0x7f)
        assertEquals(0x7f, bus.read(0x8000))
        assertEquals(0x7f, bus.read(0xa000))

        bus.write(0xd000, 0x42)
        assertEquals(0x42, bus.read(0xc000))
        assertEquals(0x42, bus.read(0xd000))
    }

    @Test
    fun mapsTheTwoLcdHalvesIntoTwentyFourCharacters() {
        val display = Pc1251Display()

        display.writeMemory(0xf800, 0x01)
        display.writeMemory(0xf83b, 0x02)
        display.writeMemory(0xf87b, 0x04)
        display.writeMemory(0xf840, 0x08)

        val columns = display.copyDotColumns()
        assertEquals(120, columns.size)
        assertEquals(0x01, columns[0].toInt())
        assertEquals(0x02, columns[59].toInt())
        assertEquals(0x04, columns[60].toInt())
        assertEquals(0x08, columns[119].toInt())
    }

    private fun romSet(): RomSet = RomSet(
        Pc1251RomDefinition.MACHINE_ID,
        listOf(
            RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000)),
            RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
        ),
    )
}
