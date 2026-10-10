package com.digihori.pgp.core.emulator.machine.pc1261

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Pc1261MachineTest {
    @Test
    fun flatImporterDiscardsLegacyRamArea() {
        val image = ByteArray(0x10000) { (it ushr 12).toByte() }
        val imported = assertIs<RomImportResult.Success>(Pc1261FlatRomImporter.importImage(image))
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1261RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        assertEquals(0, session.memorySnapshot(0x2000, 1).copyBytes()[0].toInt())
        assertEquals(image[0x8000], session.memorySnapshot(0x8000, 1).copyBytes()[0])
    }

    @Test
    fun displayWritesUseFourBlocksAndTwoCharacterRows() {
        val displayState = Pc1261Display()
        val writes = listOf(0x2000, 0x2800, 0x2040, 0x2840)
        writes.forEach { displayState.writeMemory(it, 0x01) }
        val session = session()
        writes.forEach { session.memorySnapshot(it, 1) }
        val columns = displayState.copyColumns()
        val display = session.displaySnapshot()
        assertEquals(24, display.characterColumns)
        assertEquals(2, display.characterRows)
        assertEquals(14, display.dotRows)
        assertEquals(4, columns.count { it.toInt() != 0 })
    }

    @Test
    fun displayAndRamDecodeMatchPc1261AddressWiring() {
        val display = Pc1261Display()
        val imported = assertIs<RomImportResult.Success>(
            Pc1261ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)),
        )
        val bus = Pc1261MemoryBus(imported.romSet, display)

        bus.write(0x3000, 0x41)
        assertEquals(0x41, bus.read(0x2000))
        assertEquals(0x41, bus.read(0x3000))
        assertEquals(0x41, display.copyColumns()[0].toInt() and 0xff)

        bus.write(0x6800, 0x7f)
        assertEquals(0, bus.read(0x6800), "addresses above the PC-1261 RAM range are read-only")
    }

    @Test
    fun modeSwitchIsReportedAsLcdStatus() {
        val imported = assertIs<RomImportResult.Success>(
            Pc1261ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)),
        )
        val machine = Pc1261Machine(imported.romSet)
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1261RomDefinition.MACHINE_ID, imported.romSet),
        ).session

        machine.keyboardState.setOperatingMode(OperatingMode.PROGRAM)
        assertEquals(0x02, machine.keyboardState.readInputB(0x08))
        assertEquals(0x08, machine.keyboardState.readInputB(0x02))
        machine.keyboardState.setOperatingMode(OperatingMode.RESERVE)
        assertEquals(0x01, machine.keyboardState.readInputB(0x08))
        assertEquals(0x08, machine.keyboardState.readInputB(0x01))

        session.setOperatingMode(OperatingMode.RESERVE)
        assertTrue(DisplaySymbol.RESERVE in session.displaySnapshot().symbols)
    }

    private fun session() = assertIs<CreateSessionResult.Success>(
        EmulatorFactory.create(
            Pc1261RomDefinition.MACHINE_ID,
            (Pc1261ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)) as RomImportResult.Success).romSet,
        ),
    ).session
}
