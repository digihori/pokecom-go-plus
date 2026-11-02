package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.RamMode

import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomSet

/** Physical RAM and LCD decode, rather than the reference emulator's 24 KiB write window. */
internal class Pc1401MemoryBus(romSet: RomSet, private val display: Pc1401Display, private val model: Pc1401FamilyModel = Pc1401FamilyModel.PC_1401, ramMode: RamMode = RamMode.HARDWARE) : Sc61860Bus {
    private val internal = requireNotNull(romSet.component(Pc1401RomDefinition.INTERNAL_ID)).copyBytes()
    private val external = requireNotNull(romSet.component(Pc1401RomDefinition.EXTERNAL_ID)).copyBytes()
    private val ramRange = if (ramMode == RamMode.UNRESTRICTED) 0x2000..0x5fff else model.physicalRamStart..0x47ff
    private val ram = ByteArray(ramRange.count())
    private val lcd = ByteArray(0x100)
    init {
        require(romSet.machineId == model.machineId)
        require(internal.size == 0x2000 && external.size == 0x8000)
    }
    override fun read(address: Int): Int {
        require(address in 0..0xffff)
        return when (address) {
            in 0..0x1fff -> internal[address].toInt() and 0xff
            in ramRange -> ram[address - ramRange.first].toInt() and 0xff
            in 0x6000..0x67ff, in 0x7000..0x77ff -> lcd[address and 0xff].toInt() and 0xff
            in 0x8000..0xffff -> external[address - 0x8000].toInt() and 0xff
            else -> 0xff
        }
    }
    override fun write(address: Int, value: Int) {
        require(address in 0..0xffff)
        when (address) {
            in ramRange -> ram[address - ramRange.first] = value.toByte()
            in 0x6000..0x67ff, in 0x7000..0x77ff -> {
                lcd[address and 0xff] = value.toByte()
                display.writeMemory(0x6000 or (address and 0xff), value)
            }
        }
    }
    fun resetRam() { ram.fill(0); lcd.fill(0); display.reset() }
    fun isWritableAddress(address: Int): Boolean = address in ramRange ||
        address in 0x6000..0x67ff || address in 0x7000..0x77ff
}
