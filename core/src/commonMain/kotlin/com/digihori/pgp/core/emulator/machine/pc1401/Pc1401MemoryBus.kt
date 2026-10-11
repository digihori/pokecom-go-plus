package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomSet

/** Physical RAM and LCD decode, rather than the reference emulator's 24 KiB write window. */
internal class Pc1401MemoryBus(romSet: RomSet, private val display: Pc1401Display) : Sc61860Bus {
    private val internal = requireNotNull(romSet.component(Pc1401RomDefinition.INTERNAL_ID)).copyBytes()
    private val external = requireNotNull(romSet.component(Pc1401RomDefinition.EXTERNAL_ID)).copyBytes()
    private val ram = ByteArray(0x1000)
    private val lcd = ByteArray(0x100)
    init {
        require(romSet.machineId == Pc1401RomDefinition.MACHINE_ID)
        require(internal.size == 0x2000 && external.size == 0x8000)
    }
    override fun read(address: Int): Int {
        require(address in 0..0xffff)
        return when (address) {
            in 0..0x1fff -> internal[address].toInt() and 0xff
            in 0x3800..0x47ff -> ram[address - 0x3800].toInt() and 0xff
            in 0x6000..0x67ff, in 0x7000..0x77ff -> lcd[address and 0xff].toInt() and 0xff
            in 0x8000..0xffff -> external[address - 0x8000].toInt() and 0xff
            else -> 0xff
        }
    }
    override fun write(address: Int, value: Int) {
        require(address in 0..0xffff)
        when (address) {
            in 0x3800..0x47ff -> ram[address - 0x3800] = value.toByte()
            in 0x6000..0x67ff, in 0x7000..0x77ff -> {
                lcd[address and 0xff] = value.toByte()
                display.writeMemory(0x6000 or (address and 0xff), value)
            }
        }
    }
    fun resetRam() { ram.fill(0); lcd.fill(0); display.reset() }
    fun isWritableAddress(address: Int): Boolean = address in 0x3800..0x47ff ||
        address in 0x6000..0x67ff || address in 0x7000..0x77ff
}
