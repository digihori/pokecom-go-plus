package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet

internal class Pc1350MemoryBus(
    romSet: RomSet,
    private val display: Pc1350Display = Pc1350Display(),
) : Sc61860Bus {
    private val memory = ByteArray(0x10000)

    init {
        require(romSet.machineId == Pc1350RomDefinition.MACHINE_ID) { "PC-1350 ROM set has wrong machine ID" }
        install(romSet, Pc1350RomDefinition.INTERNAL_ID, Pc1350RomDefinition.INTERNAL_SIZE, 0x0000)
        install(romSet, Pc1350RomDefinition.EXTERNAL_ID, Pc1350RomDefinition.EXTERNAL_SIZE, 0x8000)
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        return memory[address].toInt() and 0xff
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        if (address !in RAM_RANGE) return
        memory[address] = (value and 0xff).toByte()
        display.writeMemory(address, value)
    }

    fun resetRam() {
        memory.fill(0, RAM_RANGE.first, RAM_RANGE.last + 1)
        display.reset()
    }

    fun isWritableAddress(address: Int): Boolean = address in RAM_RANGE

    private fun install(romSet: RomSet, id: RomComponentId, size: Int, address: Int) {
        val component = requireNotNull(romSet.component(id)) { "PC-1350 ROM set is missing ${id.value}" }
        require(component.size == size) { "Component ${id.value} must be $size bytes" }
        component.copyBytes().copyInto(memory, address)
    }

    private fun requireAddress(address: Int) {
        require(address in 0..0xffff) { "PC-1350 address must be a 16-bit value" }
    }

    private companion object {
        val RAM_RANGE: IntRange = 0x2000..0x7fff
    }
}
