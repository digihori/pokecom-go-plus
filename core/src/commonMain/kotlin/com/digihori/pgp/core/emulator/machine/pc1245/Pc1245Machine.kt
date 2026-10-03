package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.emulator.cpu.Sc61860Cpu
import com.digihori.pgp.core.emulator.cpu.Sc61860RunResult
import com.digihori.pgp.core.emulator.cpu.Sc61860StepResult
import com.digihori.pgp.core.rom.RomSet

/** Owns all mutable state for one headless PC-1245 instance. */
internal class Pc1245Machine(
    romSet: RomSet,
) {
    private val display: Pc1245Display = Pc1245Display()
    private val keyboard: Pc1245Keyboard = Pc1245Keyboard()
    private val buzzer: Pc1245Buzzer = Pc1245Buzzer()
    private val io: Pc1245Io = Pc1245Io(keyboard, display, buzzer)
    private val bus: Pc1245MemoryBus = Pc1245MemoryBus(romSet, display)
    private val cpu: Sc61860Cpu = Sc61860Cpu(bus, io)

    init {
        coldReset()
    }

    fun coldReset() {
        bus.resetRam()
        keyboard.reset()
        buzzer.reset()
        cpu.reset()
    }

    fun step(): Sc61860StepResult = cpu.step()

    fun runCycles(cycleBudget: Long): Sc61860RunResult {
        require(cycleBudget > 0) { "Cycle budget must be greater than zero" }

        var executedCycles = 0L
        var executedInstructions = 0L

        while (executedCycles < cycleBudget) {
            val result = step()
            if (result.stopReason != null) {
                return Sc61860RunResult(
                    executedCycles = executedCycles,
                    executedInstructions = executedInstructions,
                    stopReason = result.stopReason,
                )
            }

            check(result.cycles > 0) { "A completed instruction must consume at least one cycle" }
            executedCycles += result.cycles
            executedInstructions++
        }

        return Sc61860RunResult(
            executedCycles = executedCycles,
            executedInstructions = executedInstructions,
        )
    }

    internal fun readMemory(address: Int): Int = bus.read(address)

    internal fun writeMemory(address: Int, value: Int) {
        bus.write(address, value)
    }

    internal val cpuState
        get() = cpu.state

    internal val displayState
        get() = display

    internal val keyboardState
        get() = keyboard

    internal val buzzerState
        get() = buzzer
}
