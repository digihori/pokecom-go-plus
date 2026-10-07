package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.BankSwitchEvent
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.emulator.cpu.Sc61860BusAccessRecorder
import com.digihori.pgp.core.emulator.cpu.Sc61860Cpu
import com.digihori.pgp.core.emulator.cpu.Sc61860Io
import com.digihori.pgp.core.emulator.cpu.Sc61860RunResult
import com.digihori.pgp.core.emulator.cpu.Sc61860StepResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Buzzer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.rom.RomSet

internal class Pc1360Machine(romSet: RomSet) {
    private val display = Pc1360Display()
    private val keyboard = Pc1360Keyboard()
    private val buzzer = Pc1245Buzzer()
    private val bus = Pc1360MemoryBus(romSet, display)
    private val cpuBus = Sc61860BusAccessRecorder(bus)
    private val cpu = Sc61860Cpu(cpuBus, Pc1360Io(bus, keyboard, buzzer))

    init { coldReset() }
    fun coldReset() { bus.resetRam(); keyboard.reset(); buzzer.reset(); cpu.reset() }
    fun step(): Sc61860StepResult = cpu.step().also { if (it.stopReason == null) buzzer.advanceCycles(it.cycles) }
    fun runCycles(cycleBudget: Long): Sc61860RunResult {
        require(cycleBudget > 0)
        var cycles = 0L
        var instructions = 0L
        while (cycles < cycleBudget) {
            val result = step()
            if (result.stopReason != null) return Sc61860RunResult(cycles, instructions, result.stopReason)
            cycles += result.cycles
            instructions++
        }
        return Sc61860RunResult(cycles, instructions)
    }
    fun readMemory(address: Int): Int = bus.read(address)
    fun writeMemory(address: Int, value: Int) = bus.write(address, value)
    fun loadBasicProgram(program: ByteArray): Pc1245BasicMemoryResult = Pc1360BasicProgramMemory.load(program, ::readMemory, ::writeMemory)
    fun basicProgram(): Pc1245BasicMemoryResult = Pc1360BasicProgramMemory.extract(::readMemory)
    val cpuState get() = cpu.state
    val displayState get() = display
    val keyboardState get() = keyboard
    val buzzerState get() = buzzer
    val selectedRomBank get() = bus.selectedBank
    fun drainBankSwitchEvents(): List<BankSwitchEvent> = bus.drainBankSwitchEvents()
    fun setMemoryAccessTracing(enabled: Boolean) { cpuBus.enabled = enabled }
    fun drainMemoryAccesses(): List<MemoryAccess> = cpuBus.drain()
}

private class Pc1360Io(
    private val bus: Pc1360MemoryBus,
    private val keyboard: Pc1360Keyboard,
    private val buzzer: Pc1245Buzzer,
) : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int): Int = keyboard.readInputA(ia, bus.read(0x3e00))
    override fun readInputB(ib: Int): Int = 0
    override fun consumeKeyOnSignal(): Boolean = keyboard.consumeKeyOnSignal()
    override fun writeOutputControl(value: Int) = buzzer.writeControl(value)
}
