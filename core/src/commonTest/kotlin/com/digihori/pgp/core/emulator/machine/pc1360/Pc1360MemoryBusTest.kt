package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class Pc1360MemoryBusTest {
    @Test
    fun legacyImporterSplitsEightPhysicalBanks() {
        val internal = ByteArray(0x2000) { 0x55 }
        val bankImage = ByteArray(0x20000) { (it / 0x4000).toByte() }
        val romSet = assertIs<RomImportResult.Success>(
            Pc1360LegacyRomImporter.importImages(internal, bankImage),
        ).romSet

        assertEquals(9, romSet.components.size)
        assertContentEquals(internal, romSet.component(Pc1360RomDefinition.INTERNAL_ID)!!.copyBytes())
        repeat(8) { bank ->
            assertEquals(bank, romSet.component(Pc1360RomDefinition.bankId(bank))!!.copyBytes()[0].toInt())
        }
    }

    @Test
    fun rejectsInvalidLegacyBankImageSize() {
        val failure = assertIs<RomImportResult.Failure>(
            Pc1360LegacyRomImporter.importImages(ByteArray(0x2000), ByteArray(42)),
        )
        assertEquals(RomImportError.InvalidImageSize(listOf(0x20000), 42), failure.error)
    }

    @Test
    fun selectsBanksWithLowThreeBitsAndReportsChanges() {
        val bus = Pc1360MemoryBus(romSet())
        assertEquals(0, bus.read(0x4000))
        bus.write(0x3400, 0x0b)
        assertEquals(3, bus.selectedBank)
        assertEquals(3, bus.read(0x4000))
        assertEquals(3, bus.read(0x7fff))
        assertEquals(listOf(com.digihori.pgp.core.api.BankSwitchEvent(0, 3, 0x0b)), bus.drainBankSwitchEvents())
        assertTrue(bus.drainBankSwitchEvents().isEmpty())
    }

    @Test
    fun protectsRomsAndAllowsControlAndRamWrites() {
        val bus = Pc1360MemoryBus(romSet())
        bus.write(0x0000, 0xaa)
        bus.write(0x4000, 0xbb)
        assertEquals(0x44, bus.read(0x0000))
        assertEquals(0, bus.read(0x4000))
        assertFalse(bus.isWritableAddress(0x1fff))
        assertFalse(bus.isWritableAddress(0x4000))
        assertTrue(bus.isWritableAddress(0x2000))
        assertTrue(bus.isWritableAddress(0xffff))
        bus.write(0x8000, 0xcc)
        assertEquals(0xcc, bus.read(0x8000))
    }

    @Test
    fun mapsPc1360DisplayAddresses() {
        val display = Pc1360Display()
        val bus = Pc1360MemoryBus(romSet(), display)
        Pc1360Display.VRAM_BLOCK_STARTS.forEachIndexed { index, address -> bus.write(address, index + 1) }
        bus.write(Pc1360Display.SYMBOL_ADDRESS, 0x5a)
        assertEquals(0x5a, display.symbolState())
        Pc1360Display.VRAM_BLOCK_STARTS.indices.forEach { index ->
            assertEquals(index + 1, display.copyVramColumns()[index * 30].toInt() and 0xff)
        }
    }

    private fun romSet() = assertIs<RomImportResult.Success>(
        Pc1360ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x44 },
            List(8) { bank -> ByteArray(0x4000) { bank.toByte() } },
        ),
    ).romSet
}
