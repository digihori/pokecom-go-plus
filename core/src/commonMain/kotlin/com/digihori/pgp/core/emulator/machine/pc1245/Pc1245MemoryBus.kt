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
        val mappedAddress = mapAddress(address, includeRomAlias = true)
        return memory[mappedAddress].toInt() and BYTE_MASK
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        val mappedAddress = mapAddress(address, includeRomAlias = false)
        if (!isWritableAddress(address)) return

        val byteValue = value and BYTE_MASK
        memory[mappedAddress] = byteValue.toByte()

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

    fun isWritableAddress(address: Int): Boolean {
        if (address !in 0..0xffff) return false
        val mapped = mapAddress(address, includeRomAlias = false)
        return mapped in 0xc000..0xc7ff || mapped in 0xf800..0xf8ff
    }

    private fun mapAddress(address: Int, includeRomAlias: Boolean): Int = when (address) {
        in 0x2000..0x3fff -> if (includeRomAlias) address + 0x2000 else address
        in 0xd000..0xd7ff -> address - 0x1000
        in 0xe800..0xefff -> 0xf800 or (address and 0xff)
        in 0xf900..0xffff -> address and 0xf8ff
        else -> address
    }

    internal companion object {
        const val ADDRESS_SPACE_SIZE: Int = 0x10000
        const val RAM_START: Int = 0x8000

        private const val BYTE_MASK: Int = 0xff
    }
}
