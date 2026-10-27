package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet

internal class Pc1251MemoryBus(
    romSet: RomSet,
    private val display: Pc1251Display = Pc1251Display(),
    model: Pc1251FamilyModel = Pc1251FamilyModel.PC_1251,
    memoryMode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED,
) : Sc61860Bus {
    private val memory = ByteArray(0x10000)
    private val model = model
    private val memoryMode = memoryMode
    private val ramRange = (if (memoryMode == Pc1251FamilyMemoryMode.EXPANDED) 0xa000 else model.physicalRamStart)..0xc7ff

    init {
        require(romSet.machineId == model.machineId) { "${model.name} ROM set has wrong machine ID" }
        install(romSet, Pc1251RomDefinition.INTERNAL_ID, Pc1251RomDefinition.INTERNAL_SIZE, 0x0000)
        install(romSet, Pc1251RomDefinition.EXTERNAL_ID, Pc1251RomDefinition.EXTERNAL_SIZE, 0x4000)
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        val mapped = mapAddress(address, includeRomAlias = true)
        return memory[mapped].toInt() and 0xff
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        val mapped = mapAddress(address, includeRomAlias = false)
        if (mapped !in ramRange && mapped !in LCD_RANGE) return
        memory[mapped] = (value and 0xff).toByte()
        if (mapped in LCD_RANGE) display.writeMemory(mapped, value)
    }

    fun resetRam() {
        memory.fill(0, ramRange.first, ramRange.last + 1)
        memory.fill(0, LCD_RANGE.first, LCD_RANGE.last + 1)
        display.reset()
    }

    fun isWritableAddress(address: Int): Boolean {
        if (address !in 0..0xffff) return false
        val mapped = mapAddress(address, includeRomAlias = false)
        return mapped in ramRange || mapped in LCD_RANGE
    }

    private fun mapAddress(address: Int, includeRomAlias: Boolean): Int = when (address) {
        in 0x2000..0x3fff -> if (includeRomAlias) address + 0x2000 else address
        in 0xe800..0xefff -> 0xf800 or (address and 0x00ff)
        in 0xf900..0xffff -> address and 0xf8ff
        else -> pc1251RamAddress(address, model, memoryMode)
    }

    private fun install(romSet: RomSet, id: RomComponentId, size: Int, address: Int) {
        val component = requireNotNull(romSet.component(id)) { "PC-1251 ROM set is missing ${id.value}" }
        require(component.size == size) { "Component ${id.value} must be $size bytes" }
        component.copyBytes().copyInto(memory, address)
    }

    private fun requireAddress(address: Int) {
        require(address in 0..0xffff) { "PC-1251 address must be a 16-bit value" }
    }

    private companion object {
        val LCD_RANGE: IntRange = 0xf800..0xf8ff
    }
}
