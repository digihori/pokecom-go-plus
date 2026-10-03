package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Pc1245MemoryBusTest {
    @Test
    fun requiresThePc1245MachineIdAndBothExpectedComponents() {
        assertFailsWith<IllegalArgumentException> {
            Pc1245MemoryBus(romSet(machineId = MachineId("pc-1251")))
        }
        assertFailsWith<IllegalArgumentException> {
            Pc1245MemoryBus(
                RomSet(
                    Pc1245RomDefinition.MACHINE_ID,
                    listOf(
                        RomComponent(
                            Pc1245RomDefinition.INTERNAL_ID,
                            RomRole.INTERNAL,
                            ByteArray(Pc1245RomDefinition.INTERNAL_SIZE),
                        ),
                    ),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            Pc1245MemoryBus(romSet(internal = ByteArray(1)))
        }
    }

    @Test
    fun aliasesReadsFromTwoToFourKiBPagesIntoExternalRom() {
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE).apply {
            this[0x0000] = 0x12
            this[0x1fff] = 0x34
        }
        val bus = Pc1245MemoryBus(romSet(external = external))

        assertEquals(0x12, bus.read(0x2000))
        assertEquals(0x34, bus.read(0x3fff))
    }

    @Test
    fun ignoresWritesThatRemainInRom() {
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE).apply { this[0] = 0x12 }
        val bus = Pc1245MemoryBus(romSet(external = external))

        bus.write(0x4000, 0x99)

        assertEquals(0x12, bus.read(0x4000))
    }

    @Test
    fun appliesTheReferenceWriteMappingsInOrder() {
        val bus = emptyBus()

        bus.write(0x8000, 0x11)
        bus.write(0xb000, 0x22)
        bus.write(0xd000, 0x33)

        assertEquals(0x11, bus.read(0xa000))
        assertEquals(0x33, bus.read(0xc000))
        assertEquals(0x33, bus.read(0xd000))

        // 0xb000 is transformed to 0xb800 and then to 0xc000 before being mirrored to 0xd000.
        bus.write(0xb000, 0x44)
        assertEquals(0x44, bus.read(0xc000))
        assertEquals(0x44, bus.read(0xd000))
    }

    @Test
    fun aliasesReadsAndWritesAcrossE800ToEfffAndF800ToFfff() {
        val bus = emptyBus()

        bus.write(0xe900, 0x5a)
        bus.write(0xffff, 0xa5)

        for (address in 0xe800..0xef00 step 0x0100) {
            assertEquals(0x5a, bus.read(address))
        }
        for (address in 0xf800..0xff00 step 0x0100) {
            assertEquals(0x5a, bus.read(address))
        }
        for (address in 0xe8ff..0xefff step 0x0100) {
            assertEquals(0xa5, bus.read(address))
        }
        for (address in 0xf8ff..0xffff step 0x0100) {
            assertEquals(0xa5, bus.read(address))
        }
    }

    @Test
    fun preservesTheLowByteWhenResolvingAnE800Alias() {
        val bus = emptyBus()

        bus.write(0xe914, 0x5a)

        assertEquals(0x5a, bus.read(0xe914))
        assertEquals(0x5a, bus.read(0xf814))
        assertEquals(0, bus.read(0xf800))
    }

    @Test
    fun doesNotAliasTheF000ToF7ffWorkingRam() {
        val bus = emptyBus()

        bus.write(0xf014, 0x33)
        bus.write(0xf714, 0x44)

        assertEquals(0x33, bus.read(0xf014))
        assertEquals(0x44, bus.read(0xf714))
        assertEquals(0, bus.read(0xf814))
    }

    @Test
    fun storesOnlyTheLowEightBits() {
        val bus = emptyBus()

        bus.write(0xe000, 0x1ab)

        assertEquals(0xab, bus.read(0xe000))
    }

    @Test
    fun ramResetPreservesRomAndClearsTheWritableHalf() {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE).apply { this[0] = 0x11 }
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE).apply { this[0] = 0x22 }
        val bus = Pc1245MemoryBus(romSet(internal = internal, external = external))
        bus.write(0x8000, 0x33)
        bus.write(0xffff, 0x44)
        bus.write(0xe000, 0x55)

        bus.resetRam()

        assertEquals(0x11, bus.read(0x0000))
        assertEquals(0x22, bus.read(0x4000))
        assertEquals(0, bus.read(0x8000))
        assertEquals(0, bus.read(0xe000))
        assertEquals(0, bus.read(0xffff))
    }

    @Test
    fun rejectsAddressesOutsideTheSixteenBitSpace() {
        val bus = emptyBus()

        assertFailsWith<IllegalArgumentException> { bus.read(-1) }
        assertFailsWith<IllegalArgumentException> { bus.read(0x10000) }
        assertFailsWith<IllegalArgumentException> { bus.write(-1, 0) }
        assertFailsWith<IllegalArgumentException> { bus.write(0x10000, 0) }
    }

    private fun emptyBus(): Pc1245MemoryBus = Pc1245MemoryBus(romSet())

    private fun romSet(
        machineId: MachineId = Pc1245RomDefinition.MACHINE_ID,
        internal: ByteArray = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE),
        external: ByteArray = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE),
    ): RomSet = RomSet(
        machineId = machineId,
        components = listOf(
            RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
            RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
        ),
    )
}
