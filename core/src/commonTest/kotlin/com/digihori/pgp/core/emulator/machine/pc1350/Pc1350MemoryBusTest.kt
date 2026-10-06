package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Pc1350MemoryBusTest {
    @Test
    fun installsRomsProtectsThemAndAllowsRamWrites() {
        val internal = ByteArray(0x2000) { 0x11 }
        val external = ByteArray(0x8000) { 0x22 }
        val result = assertIs<RomImportResult.Success>(Pc1350ComponentRomImporter.importImages(internal, external))
        val bus = Pc1350MemoryBus(result.romSet)

        assertEquals(0x11, bus.read(0x0000))
        assertEquals(0x11, bus.read(0x1fff))
        assertEquals(0x22, bus.read(0x8000))
        assertEquals(0x22, bus.read(0xffff))
        bus.write(0x0000, 0xaa)
        bus.write(0x8000, 0xbb)
        assertEquals(0x11, bus.read(0x0000))
        assertEquals(0x22, bus.read(0x8000))

        assertTrue(bus.isWritableAddress(0x2000))
        assertTrue(bus.isWritableAddress(0x7fff))
        assertFalse(bus.isWritableAddress(0x1fff))
        assertFalse(bus.isWritableAddress(0x8000))
        bus.write(0x2345, 0x9a)
        assertEquals(0x9a, bus.read(0x2345))
    }

    @Test
    fun mapsAllDiscontinuousVramBlocksAndSymbolByte() {
        val display = Pc1350Display()
        val bus = Pc1350MemoryBus(romSet(), display)

        Pc1350Display.VRAM_BLOCK_STARTS.forEachIndexed { index, address ->
            bus.write(address, index + 1)
        }
        bus.write(Pc1350Display.SYMBOL_ADDRESS, 0xa5)

        val columns = display.copyVramColumns()
        Pc1350Display.VRAM_BLOCK_STARTS.indices.forEach { index ->
            assertEquals(index + 1, columns[index * Pc1350Display.VRAM_BLOCK_SIZE].toInt() and 0xff)
        }
        assertEquals(0xa5, display.symbolState())
    }

    @Test
    fun convertsFourVramLinesIntoOneLogicalDotPlane() {
        val display = Pc1350Display()
        val bus = Pc1350MemoryBus(romSet(), display)

        // First column of each 150-column display line, with a different vertical bit.
        bus.write(0x7000, 0x01)
        bus.write(0x7040, 0x02)
        bus.write(0x701e, 0x04)
        bus.write(0x705e, 0x80)

        val dots = display.copyDots()
        assertEquals(Pc1350Display.DOT_COLUMNS * Pc1350Display.DOT_ROWS, dots.size)
        assertEquals(1, dots[0 * Pc1350Display.DOT_COLUMNS + 0])
        assertEquals(1, dots[9 * Pc1350Display.DOT_COLUMNS + 0])
        assertEquals(1, dots[18 * Pc1350Display.DOT_COLUMNS + 0])
        assertEquals(1, dots[31 * Pc1350Display.DOT_COLUMNS + 0])
        assertEquals(0, dots[8 * Pc1350Display.DOT_COLUMNS + 0])
        assertEquals(0, dots[24 * Pc1350Display.DOT_COLUMNS + 0])
    }

    @Test
    fun flatImporterExtractsOnlyPhysicalRomAreas() {
        val image = ByteArray(0x10000)
        image.fill(0x11, 0x0000, 0x2000)
        image.fill(0x66, 0x2000, 0x8000)
        image.fill(0x22, 0x8000, 0x10000)

        val result = assertIs<RomImportResult.Success>(Pc1350FlatRomImporter.importImage(image))
        val bus = Pc1350MemoryBus(result.romSet)
        assertEquals(0x11, bus.read(0x0000))
        assertEquals(0x00, bus.read(0x2000))
        assertEquals(0x22, bus.read(0x8000))
    }

    private fun romSet() = assertIs<RomImportResult.Success>(
        Pc1350ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)),
    ).romSet
}
