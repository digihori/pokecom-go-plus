package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.emulator.cpu.Sc61860BusAccessRecorder
import com.digihori.pgp.core.emulator.cpu.Sc61860Cpu
import com.digihori.pgp.core.emulator.cpu.Sc61860Io
import com.digihori.pgp.core.emulator.cpu.Sc61860RunResult
import com.digihori.pgp.core.emulator.cpu.Sc61860StepResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Buzzer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.rom.RomSet

internal class Pc1350Machine(romSet: RomSet) {
    private val display = Pc1350Display()
    private val keyboard = Pc1350Keyboard()
    private val buzzer = Pc1245Buzzer()
    private val bus = Pc1350MemoryBus(romSet, display)
    private val cpuBus = Sc61860BusAccessRecorder(bus)
    private val io = Pc1350Io(bus, keyboard, buzzer)
    private val cpu = Sc61860Cpu(cpuBus, io)

    init { coldReset() }

    fun coldReset() {
        bus.resetRam()
        keyboard.reset()
        buzzer.reset()
        cpu.reset()
    }

    fun step(): Sc61860StepResult {
        val result = cpu.step()
        if (result.stopReason == null) buzzer.advanceCycles(result.cycles)
        return result
    }

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
    fun loadBasicProgram(program: ByteArray): Pc1245BasicMemoryResult =
        Pc1350BasicProgramMemory.load(program, ::readMemory, ::writeMemory)
    fun basicProgram(): Pc1245BasicMemoryResult = Pc1350BasicProgramMemory.extract(::readMemory)
    val cpuState get() = cpu.state
    val displayState get() = display
    val keyboardState get() = keyboard
    val buzzerState get() = buzzer
    fun setMemoryAccessTracing(enabled: Boolean) { cpuBus.enabled = enabled }
    fun drainMemoryAccesses(): List<MemoryAccess> = cpuBus.drain()
}

private class Pc1350Io(
    private val bus: Pc1350MemoryBus,
    private val keyboard: Pc1350Keyboard,
    private val buzzer: Pc1245Buzzer,
) : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int): Int = keyboard.readInputA(ia, bus.read(KEY_SELECTOR_ADDRESS))
    override fun readInputB(ib: Int): Int = 0
    override fun consumeKeyOnSignal(): Boolean = keyboard.consumeKeyOnSignal()
    override fun writeOutputControl(value: Int) = buzzer.writeControl(value)

    private companion object {
        const val KEY_SELECTOR_ADDRESS = 0x7e00
    }
}
