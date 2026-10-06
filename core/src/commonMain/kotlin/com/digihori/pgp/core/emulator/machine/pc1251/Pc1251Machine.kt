package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.cpu.Sc61860Cpu
import com.digihori.pgp.core.emulator.cpu.Sc61860Io
import com.digihori.pgp.core.emulator.cpu.Sc61860RunResult
import com.digihori.pgp.core.emulator.cpu.Sc61860StepResult
import com.digihori.pgp.core.emulator.cpu.Sc61860BusAccessRecorder
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Buzzer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.core.source.machine.AddressedMemoryImage

internal class Pc1251Machine(
    romSet: RomSet,
    model: Pc1251FamilyModel = Pc1251FamilyModel.PC_1251,
    memoryMode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED,
) {
    private val display = Pc1251Display()
    private val keyboard = Pc1251Keyboard()
    private val buzzer = Pc1245Buzzer()
    private val bus = Pc1251MemoryBus(romSet, display, model, memoryMode)
    private val cpuBus = Sc61860BusAccessRecorder(bus)
    private val io = Pc1251Io(keyboard, display, buzzer)
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
        Pc1251BasicProgramMemory.load(program, ::readMemory, ::writeMemory)

    fun basicProgram(): Pc1245BasicMemoryResult =
        Pc1251BasicProgramMemory.extract(::readMemory)

    fun loadMemoryImage(image: AddressedMemoryImage): Pc1251MemoryImageLoadResult {
        val invalid = image.segments.firstNotNullOfOrNull { segment ->
            (segment.startAddress..segment.endAddress).firstOrNull { !bus.isWritableAddress(it) }
                ?.let { Pc1251MemoryImageLoadResult.ReadOnlyAddress(it, segment.sourceLine) }
        }
        if (invalid != null) return invalid
        image.segments.forEach { segment ->
            segment.copyBytes().forEachIndexed { offset, byte ->
                bus.write(segment.startAddress + offset, byte.toInt() and 0xff)
            }
        }
        return Pc1251MemoryImageLoadResult.Success(image.segments.size, image.byteCount)
    }

    val cpuState get() = cpu.state
    val displayState get() = display
    val keyboardState get() = keyboard
    val buzzerState get() = buzzer
    fun setMemoryAccessTracing(enabled: Boolean) { cpuBus.enabled = enabled }
    fun drainMemoryAccesses(): List<MemoryAccess> = cpuBus.drain()
}

private class Pc1251Io(
    private val keyboard: Pc1251Keyboard,
    private val display: Pc1251Display,
    private val buzzer: Pc1245Buzzer,
) : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int): Int = keyboard.readInputA(ia, ib)
    override fun readInputB(ib: Int): Int = keyboard.readInputB(ib)
    override fun consumeKeyOnSignal(): Boolean = keyboard.consumeKeyOnSignal()
    override fun writeOutputControl(value: Int) {
        display.setEnabled(value and 0x01 != 0)
        buzzer.writeControl(value)
    }
}

internal sealed interface Pc1251MemoryImageLoadResult {
    data class Success(val segmentCount: Int, val byteCount: Int) : Pc1251MemoryImageLoadResult
    data class ReadOnlyAddress(val address: Int, val sourceLine: Int) : Pc1251MemoryImageLoadResult
}
