package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1350MachineTest {
    @Test
    fun resetConnectsCpuMemoryAndDisplay() {
        val romSet = assertIs<RomImportResult.Success>(
            Pc1350ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)),
        ).romSet
        val machine = Pc1350Machine(romSet)

        assertEquals(0, machine.cpuState.programCounter)
        machine.writeMemory(0x7000, 0x01)
        assertEquals(1, machine.displayState.copyDots()[0].toInt())
        machine.coldReset()
        assertEquals(0, machine.readMemory(0x7000))
        assertEquals(0, machine.displayState.copyDots()[0].toInt())
    }
}
