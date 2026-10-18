package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.BankSwitchEvent
import com.digihori.pgp.core.emulator.cpu.Sc61860ProgramDataRead
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.emulator.cpu.Sc61860Bus
import com.digihori.pgp.core.rom.RomSet

internal class Pc1360MemoryBus(
    romSet: RomSet,
    private val display: Pc1360Display = Pc1360Display(),
) : Sc61860Bus {
    private val memory = ByteArray(0x10000)
    private val banks: List<ByteArray>
    private val kanjiBanks: List<ByteArray>
    private val bankEvents = mutableListOf<BankSwitchEvent>()
    var selectedBank: Int = 0
        private set

    init {
        require(romSet.machineId == Pc1360RomDefinition.MACHINE_ID || romSet.machineId == Pc1360RomDefinition.PC1360K_MACHINE_ID) { "PC-1360 ROM set has wrong machine ID" }
        val kanjiComponents = (0 until Pc1360RomDefinition.KANJI_SELECTOR_COUNT).mapNotNull { romSet.component(Pc1360RomDefinition.kanjiId(it)) }
        require(kanjiComponents.isEmpty() || (romSet.machineId == Pc1360RomDefinition.PC1360K_MACHINE_ID && kanjiComponents.size in listOf(2, Pc1360RomDefinition.KANJI_SELECTOR_COUNT))) {
            "PC-1360K kanji ROM requires selectors 0/1 or all four selectors on pc-1360k"
        }
        require(kanjiComponents.map { it.id } == (0 until kanjiComponents.size).map(Pc1360RomDefinition::kanjiId)) { "Kanji ROM selectors must start at 0 with no gaps" }
        require(romSet.components.count { it.role == RomRole.KANJI } == kanjiComponents.size) { "Unknown kanji ROM component ID" }
        kanjiBanks = kanjiComponents.map {
            require(it.size == Pc1360RomDefinition.KANJI_SIZE && it.role == RomRole.KANJI) {
                "Component ${it.id.value} must be a 65536-byte kanji ROM"
            }
            it.copyBytes()
        }
        val internal = requireNotNull(romSet.component(Pc1360RomDefinition.INTERNAL_ID)) {
            "PC-1360 ROM set is missing internal"
        }
        require(internal.size == Pc1360RomDefinition.INTERNAL_SIZE) { "Component internal must be 8192 bytes" }
        internal.copyBytes().copyInto(memory, 0)
        banks = List(Pc1360RomDefinition.BANK_COUNT) { bank ->
            val component = requireNotNull(romSet.component(Pc1360RomDefinition.bankId(bank))) {
                "PC-1360 ROM set is missing bank-$bank"
            }
            require(component.size == Pc1360RomDefinition.BANK_SIZE) { "Component bank-$bank must be 16384 bytes" }
            component.copyBytes()
        }
    }

    override fun read(address: Int): Int {
        requireAddress(address)
        return if (address in BANK_WINDOW) {
            banks[selectedBank][address - BANK_WINDOW.first].toInt() and 0xff
        } else {
            memory[address].toInt() and 0xff
        }
    }

    override fun readProgramData(programCounter: Int, dataPointer: Int): Sc61860ProgramDataRead =
        if (kanjiBanks.isEmpty()) super.readProgramData(programCounter, dataPointer)
        else {
            requireAddress(dataPointer)
            val selector = memory[0x3600].toInt() and 0xff
            // Selectors 0..3 were captured independently; partial 0/1 sets remain valid for analysis.
            Sc61860ProgramDataRead(dataPointer, if (selector in kanjiBanks.indices) kanjiBanks[selector][dataPointer].toInt() and 0xff else 0xff)
        }

    override fun write(address: Int, value: Int) {
        requireAddress(address)
        if (address !in WRITABLE_CONTROL && address !in RAM_RANGE) return
        val byte = value and 0xff
        memory[address] = byte.toByte()
        if (address == BANK_SELECTOR_ADDRESS) {
            val next = byte and 0x07
            if (next != selectedBank) {
                bankEvents += BankSwitchEvent(selectedBank, next, byte)
                selectedBank = next
            }
        }
        display.writeMemory(address, byte)
    }

    fun resetRam() {
        memory.fill(0, WRITABLE_CONTROL.first, WRITABLE_CONTROL.last + 1)
        memory.fill(0, RAM_RANGE.first, RAM_RANGE.last + 1)
        selectedBank = 0
        bankEvents.clear()
        display.reset()
    }

    fun isWritableAddress(address: Int): Boolean = address in WRITABLE_CONTROL || address in RAM_RANGE
    fun drainBankSwitchEvents(): List<BankSwitchEvent> = bankEvents.toList().also { bankEvents.clear() }

    private fun requireAddress(address: Int) {
        require(address in 0..0xffff) { "PC-1360 address must be a 16-bit value" }
    }

    companion object {
        const val BANK_SELECTOR_ADDRESS = 0x3400
        val WRITABLE_CONTROL: IntRange = 0x2000..0x3fff
        val BANK_WINDOW: IntRange = 0x4000..0x7fff
        val RAM_RANGE: IntRange = 0x8000..0xffff
    }
}
