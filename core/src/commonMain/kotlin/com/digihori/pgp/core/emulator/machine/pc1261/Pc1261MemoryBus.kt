package com.digihori.pgp.core.emulator.machine.pc1261

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet

internal class Pc1261MemoryBus(
    romSet: RomSet,
    private val display: Pc1261Display = Pc1261Display(),
    private val model: Pc1261FamilyModel = Pc1261FamilyModel.PC_1261,
) : Sc61860Bus {
    private val memory = ByteArray(0x10000)

    init {
        require(romSet.machineId == model.machineId) { "PC-1261 ROM set has wrong machine ID" }
        install(romSet, Pc1261RomDefinition.INTERNAL_ID, Pc1261RomDefinition.INTERNAL_SIZE, 0x0000)
        install(romSet, Pc1261RomDefinition.EXTERNAL_ID, Pc1261RomDefinition.EXTERNAL_SIZE, 0x8000)
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        return memory[address].toInt() and 0xff
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        if (!isWritableAddress(address)) return
        val canonical = if (address in 0x2000..0x3fff) address and 0x28ff else address
        val byte = (value and 0xff).toByte()
        memory[canonical] = byte
        display.writeMemory(canonical, value)
        if (canonical in 0x2000..0x2fff) memory[canonical + 0x1000] = byte
    }

    fun resetRam() {
        memory.fill(0, 0x2000, 0x8000)
        display.reset()
    }

    fun isWritableAddress(address: Int): Boolean = address in 0x2000..0x3fff || address in model.physicalRamStart..0x67ff

    private fun install(romSet: RomSet, id: RomComponentId, size: Int, start: Int) {
        val component = requireNotNull(romSet.component(id)) { "PC-1261 ROM set is missing ${id.value}" }
        require(component.size == size) { "Component ${id.value} must be $size bytes" }
        component.copyBytes().copyInto(memory, start)
    }

    private fun requireAddress(address: Int) = require(address in 0..0xffff) {
        "PC-1261 address must be a 16-bit value"
    }

}
