package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.runtime.KeyInputQueue
import com.digihori.pgp.core.runtime.KeyTransition
import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.emulator.cpu.Sc61860BusAccessRecorder
import com.digihori.pgp.core.emulator.cpu.Sc61860Cpu
import com.digihori.pgp.core.emulator.cpu.Sc61860Io
import com.digihori.pgp.core.emulator.cpu.Sc61860RunResult
import com.digihori.pgp.core.emulator.cpu.Sc61860StepResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245Buzzer
import com.digihori.pgp.core.rom.RomSet

internal class Pc1401Machine(romSet: RomSet, val model: Pc1401FamilyModel = Pc1401FamilyModel.PC_1401) {
    private val display = Pc1401Display()
    private val keyboard = Pc1401Keyboard()
    private val buzzer = Pc1245Buzzer(clockHz = 192_000.0)
    private val bus = Pc1401MemoryBus(romSet, display, model)
    private val cpuBus = Sc61860BusAccessRecorder(bus)
    private val cpu = Sc61860Cpu(cpuBus, Pc1401Io(keyboard, display, buzzer))

    private val modeKeys = KeyInputQueue(11_520, 3_840)
    private var requestedMode: OperatingMode? = null
    private var modeSettleCycles = 0L
    private var modeAttempts = 0
    fun isOperatingModeTransitionPending(): Boolean = requestedMode != null || !modeKeys.isIdle
    fun currentOperatingMode(): OperatingMode? = display.snapshot().symbols.let {
        when {
            DisplaySymbol.CAL in it -> OperatingMode.CALCULATOR
            DisplaySymbol.PRO in it -> OperatingMode.PROGRAM
            DisplaySymbol.RUN in it -> OperatingMode.RUN
            else -> null
        }
    }
    fun pressKey(key: PocketKey): Boolean {
        applyModeTransition(modeKeys.cancel()); requestedMode = null
        return keyboard.press(key)
    }
    fun requestOperatingMode(mode: OperatingMode) {
        require(mode != OperatingMode.RESERVE && mode != OperatingMode.AER) { "PC-1401 has no reserve mode" }
        applyModeTransition(modeKeys.cancel())
        requestedMode = mode; modeAttempts = 0; modeSettleCycles = 0
    }
    private fun applyModeTransition(transition: KeyTransition?) {
        when (transition) {
            is KeyTransition.Press -> keyboard.press(transition.key)
            is KeyTransition.Release -> keyboard.release(transition.key)
            null -> Unit
        }
    }
    private fun prepareModeInput() {
        val target = requestedMode ?: return
        if (!modeKeys.isIdle || modeSettleCycles > 0) return
        if (currentOperatingMode() == target || modeAttempts >= 3) { requestedMode = null; return }
        modeAttempts++
        modeKeys.enqueue(listOf(if (target == OperatingMode.CALCULATOR) PocketKey.CAL else PocketKey.BASIC))
        applyModeTransition(modeKeys.start())
        modeSettleCycles = 38_400
    }
    init { coldReset() }
    fun coldReset() { modeKeys.cancel(); requestedMode = null; modeSettleCycles = 0; bus.resetRam(); keyboard.reset(); buzzer.reset(); cpu.reset() }
    fun step(): Sc61860StepResult {
        prepareModeInput()
        return cpu.step().also {
            if (it.stopReason == null) {
                buzzer.advanceCycles(it.cycles)
                applyModeTransition(modeKeys.advance(it.cycles.toLong()))
                modeSettleCycles = (modeSettleCycles - it.cycles).coerceAtLeast(0)
            }
        }
    }
    fun runCycles(cycleBudget: Long): Sc61860RunResult {
        require(cycleBudget > 0)
        var cycles = 0L; var instructions = 0L
        while (cycles < cycleBudget) {
            val result = step()
            if (result.stopReason != null) return Sc61860RunResult(cycles, instructions, result.stopReason)
            cycles += result.cycles; instructions++
        }
        return Sc61860RunResult(cycles, instructions)
    }
    fun readMemory(address: Int) = bus.read(address)
    fun writeMemory(address: Int, value: Int) = bus.write(address, value)
    fun loadBasicProgram(program: ByteArray): Pc1245BasicMemoryResult =
        Pc1401BasicProgramMemory.load(program, ::readMemory, ::writeMemory, model)
    fun basicProgram(): Pc1245BasicMemoryResult = Pc1401BasicProgramMemory.extract(::readMemory, model)
    fun isWritableAddress(address: Int) = bus.isWritableAddress(address)
    val cpuState get() = cpu.state
    val displayState get() = display
    val keyboardState get() = keyboard
    val buzzerState get() = buzzer
    fun setMemoryAccessTracing(enabled: Boolean) { cpuBus.enabled = enabled }
    fun drainMemoryAccesses(): List<MemoryAccess> = cpuBus.drain()
}

private class Pc1401Io(
    private val keyboard: Pc1401Keyboard,
    private val display: Pc1401Display,
    private val buzzer: Pc1245Buzzer,
) : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int) = keyboard.readInputA(ia, ib)
    override fun readInputB(ib: Int) = keyboard.readInputB(ib)
    override fun consumeKeyOnSignal() = keyboard.consumeKeyOnSignal()
    override fun writeOutputControl(value: Int) { display.setEnabled(value and 1 != 0); buzzer.writeControl(value) }
}
