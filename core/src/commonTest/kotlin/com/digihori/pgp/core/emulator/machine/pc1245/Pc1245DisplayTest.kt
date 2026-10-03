package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Pc1245DisplayTest {
    @Test
    fun mapsBothLcdMemoryRangesIntoEightyColumns() {
        val display = Pc1245Display()

        display.writeMemory(0xf800, 0x01)
        display.writeMemory(0xf83b, 0x02)
        display.writeMemory(0xf868, 0x04)
        display.writeMemory(0xf87b, 0x08)

        val columns = display.copyDotColumns()
        assertEquals(0x01, columns[0].toInt())
        assertEquals(0x02, columns[59].toInt())
        assertEquals(0x08, columns[60].toInt())
        assertEquals(0x04, columns[79].toInt())
        assertEquals(4, display.revision)
    }

    @Test
    fun tracksSymbolBytesAndOnlyRevisesForChanges() {
        val display = Pc1245Display()

        display.writeMemory(0xf83c, 0x0f)
        display.writeMemory(0xf83d, 0x07)
        display.writeMemory(0xf83d, 0x07)

        assertEquals(0x0f, display.symbolState0())
        assertEquals(0x07, display.symbolState1())
        assertEquals(2, display.revision)
    }

    @Test
    fun resetClearsLogicalStateAndRestoresEnabledDisplay() {
        val display = Pc1245Display()
        display.writeMemory(0xf800, 0x7f)
        display.writeMemory(0xf83c, 0x01)
        display.setEnabled(false)

        display.reset()

        assertContentEquals(ByteArray(80), display.copyDotColumns())
        assertEquals(0, display.symbolState0())
        assertEquals(0, display.symbolState1())
        assertTrue(display.enabled)
        assertEquals(4, display.revision)

        display.reset()
        assertEquals(4, display.revision)
    }

    @Test
    fun ignoresMemoryOutsideTheLcdRanges() {
        val display = Pc1245Display()

        display.writeMemory(0xf840, 0x7f)

        assertFalse(display.copyDotColumns().any { it.toInt() != 0 })
        assertEquals(0, display.revision)
    }
}
