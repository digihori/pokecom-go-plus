package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet

/**
 * PC-1245 address space extracted from Pokecom GO's reference behavior.
 *
 * The input is a normalized ROM set. Legacy files and package I/O are handled before this boundary.
 */
internal class Pc1245MemoryBus(
    romSet: RomSet,
    private val display: Pc1245Display = Pc1245Display(),
) : Sc61860Bus {
    private val memory: ByteArray = ByteArray(ADDRESS_SPACE_SIZE)

    init {
        require(romSet.machineId == Pc1245RomDefinition.MACHINE_ID) {
            "PC-1245 requires machine ID ${Pc1245RomDefinition.MACHINE_ID.value}"
        }
        installComponent(
            component = requireComponent(romSet, Pc1245RomDefinition.INTERNAL_ID),
            expectedSize = Pc1245RomDefinition.INTERNAL_SIZE,
            startAddress = Pc1245RomDefinition.INTERNAL_START,
        )
        installComponent(
            component = requireComponent(romSet, Pc1245RomDefinition.EXTERNAL_ID),
            expectedSize = Pc1245RomDefinition.EXTERNAL_SIZE,
            startAddress = Pc1245RomDefinition.EXTERNAL_START,
        )
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        val mappedAddress = if (address in READ_ALIAS_RANGE) {
            address + READ_ALIAS_OFFSET
        } else {
            address
        }
        return memory[mappedAddress].toInt() and BYTE_MASK
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        var mappedAddress = address

        // Keep the order of the Pokecom GO mapping: some ranges are transformed more than once.
        if (mappedAddress in 0xb000..0xb7ff) mappedAddress += 0x0800
        if (mappedAddress in 0x8000..0x9fff) mappedAddress += 0x2000
        if (mappedAddress in 0xd000..0xd7ff) mappedAddress -= 0x1000
        if (mappedAddress >= 0xf900) mappedAddress = mappedAddress and 0xf8ff
        if (mappedAddress in 0xb800..0xbfff) mappedAddress += 0x0800

        if (mappedAddress < RAM_START) return

        val byteValue = value and BYTE_MASK
        memory[mappedAddress] = byteValue.toByte()

        when (mappedAddress) {
            in 0xc000..0xc7ff -> memory[mappedAddress + 0x1000] = byteValue.toByte()
            in 0xb800..0xbfff -> memory[mappedAddress - 0x0800] = byteValue.toByte()
            in LCD_MIRROR_SOURCE_RANGE -> {
                for (offset in 0x0100..0x0700 step 0x0100) {
                    memory[mappedAddress + offset] = byteValue.toByte()
                }
            }
        }

        display.writeMemory(mappedAddress, byteValue)
    }

    /** Clears writable machine memory while preserving the ROM regions. */
    fun resetRam() {
        memory.fill(0, fromIndex = RAM_START, toIndex = ADDRESS_SPACE_SIZE)
        display.reset()
    }

    private fun requireComponent(
        romSet: RomSet,
        id: RomComponentId,
    ): RomComponent = requireNotNull(romSet.component(id)) {
        "PC-1245 ROM set is missing component ${id.value}"
    }

    private fun installComponent(component: RomComponent, expectedSize: Int, startAddress: Int) {
        require(component.size == expectedSize) {
            "Component ${component.id.value} must be $expectedSize bytes, but was ${component.size}"
        }
        component.copyBytes().copyInto(memory, destinationOffset = startAddress)
    }

    private fun requireAddress(address: Int) {
        require(address in 0 until ADDRESS_SPACE_SIZE) {
            "PC-1245 address must be a 16-bit value, but was $address"
        }
    }

    internal companion object {
        const val ADDRESS_SPACE_SIZE: Int = 0x10000
        const val RAM_START: Int = 0x8000

        private const val BYTE_MASK: Int = 0xff
        private const val READ_ALIAS_OFFSET: Int = 0x2000
        private val READ_ALIAS_RANGE: IntRange = 0x2000..0x3fff
        private val LCD_MIRROR_SOURCE_RANGE: IntRange = 0xf800..0xf8ff
    }
}
