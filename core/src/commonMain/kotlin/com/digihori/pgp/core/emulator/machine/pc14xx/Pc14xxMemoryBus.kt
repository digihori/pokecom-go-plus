package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.api.BankSwitchEvent
import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomComponentId
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.core.rom.RomRole

internal class Pc14xxMemoryBus(rom: RomSet, private val model: Pc14xxModel, private val display: Pc14xxDisplay) : Sc61860Bus {
    private val memory = ByteArray(0x10000)
    private val layout = MachineMemoryLayout(model.machineId)
    var selectedBank: Int = 0
        private set
    private val bankEvents = mutableListOf<BankSwitchEvent>()
    private val banks: List<ByteArray>
    init {
        require(rom.machineId == model.machineId)
        val internal = requireNotNull(rom.component(RomComponentId("internal")))
        require(internal.size == 0x2000 && internal.role == RomRole.INTERNAL)
        internal.copyBytes().copyInto(memory)
        if (model.bankCount == 0) {
            val external = requireNotNull(rom.component(RomComponentId("external")))
            require(external.size == 0x8000 && external.role == RomRole.EXTERNAL && rom.components.size == 2)
            external.copyBytes().copyInto(memory, 0x8000)
            banks = emptyList()
        } else {
            require(rom.components.size == model.bankCount + 1)
            banks = List(model.bankCount) { bank ->
                val component = requireNotNull(rom.component(Pc14xxRomDefinition.bankId(bank)))
                require(component.size == 0x4000 && component.role == RomRole.EXTERNAL)
                component.copyBytes()
            }
        }
    }
    override fun read(address: Int): Int {
        val location = layout.locate(address)
        return if (model.bankCount != 0 && address in 0x4000..0x7fff)
            banks[selectedBank][address - 0x4000].toInt() and 255
        else memory[location.physicalAddress].toInt() and 255
    }
    override fun write(address: Int, value: Int) {
        val location = layout.locate(address)
        if (!location.writable) return
        memory[location.physicalAddress] = value.toByte()
        if (model.bankCount != 0 && (if (model == Pc14xxModel.PC_1460) address == 0x3c00 else address in 0x3400..0x35ff)) {
            val next = value and (model.bankCount - 1)
            if (next != selectedBank) { bankEvents += BankSwitchEvent(selectedBank, next, value and 255); selectedBank = next }
        }
        display.writeMemory(location.physicalAddress, value)
    }
    fun resetRam() {
        memory.fill(0, 0x2000, if (model.bankCount == 0) 0x8000 else 0x10000)
        selectedBank = 0; bankEvents.clear(); display.reset()
    }
    fun drainBankSwitchEvents(): List<BankSwitchEvent> = bankEvents.toList().also { bankEvents.clear() }
    fun isWritableAddress(address: Int): Boolean = layout.locate(address).writable
}
