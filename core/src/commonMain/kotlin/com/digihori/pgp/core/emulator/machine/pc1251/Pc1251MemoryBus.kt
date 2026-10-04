package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet

internal class Pc1251MemoryBus(
    romSet: RomSet,
    private val display: Pc1251Display = Pc1251Display(),
) : Sc61860Bus {
    private val memory = ByteArray(0x10000)

    init {
        require(romSet.machineId == Pc1251RomDefinition.MACHINE_ID) { "PC-1251 ROM set has wrong machine ID" }
        install(romSet, Pc1251RomDefinition.INTERNAL_ID, Pc1251RomDefinition.INTERNAL_SIZE, 0x0000)
        install(romSet, Pc1251RomDefinition.EXTERNAL_ID, Pc1251RomDefinition.EXTERNAL_SIZE, 0x4000)
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        val mapped = mapRamMirror(address)
        return memory[mapped].toInt() and 0xff
    }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        val mapped = mapRamMirror(address)
        if (mapped !in RAM_RANGE && mapped !in LCD_RANGE) return
        memory[mapped] = (value and 0xff).toByte()
        if (mapped in LCD_RANGE) display.writeMemory(mapped, value)
    }

    fun resetRam() {
        memory.fill(0, RAM_RANGE.first, RAM_RANGE.last + 1)
        memory.fill(0, LCD_RANGE.first, LCD_RANGE.last + 1)
        display.reset()
    }

    fun isWritableAddress(address: Int): Boolean {
        if (address !in 0..0xffff) return false
        val mapped = mapRamMirror(address)
        return mapped in RAM_RANGE || mapped in LCD_RANGE
    }

    private fun mapRamMirror(address: Int): Int = when (address) {
        in 0x8000..0x9fff -> address + 0x2000
        in 0xb000..0xb7ff -> address + 0x0800
        in 0xd000..0xd7ff -> address - 0x1000
        else -> address
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
        // A000-B7FF is reached through the 8000-97FF mirror on real PC-1251
        // hardware. Programs commonly use 8000 as a scratch counter.
        val RAM_RANGE: IntRange = 0xa000..0xc7ff
        val LCD_RANGE: IntRange = 0xf800..0xf8ff
    }
}
