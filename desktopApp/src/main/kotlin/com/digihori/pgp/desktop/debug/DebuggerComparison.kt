package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.PhysicalRomLocation
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

internal enum class DebuggerComparisonBasis { NONE, STEP, STOP, RUNNING }

internal data class DebuggerComparisonFrame(
    val previousCpu: CpuSnapshot? = null,
    val cpuBasis: DebuggerComparisonBasis = DebuggerComparisonBasis.NONE,
    val memoryBasis: DebuggerComparisonBasis = DebuggerComparisonBasis.NONE,
    val memoryStart: Int = 0,
    val memoryChanges: Map<Int, MemoryValueChange> = emptyMap(),
    val excludedMemoryBytes: Int = 0,
)

private data class MemoryComparisonByte(val value: Int, val physicalAddress: Int, val rom: PhysicalRomLocation?)
private data class ComparisonCapture(val cpu: CpuSnapshot, val memory: Map<Int, MemoryComparisonByte>?)

/** UI observations only. Never steps, pauses, writes, or drains emulator events. */
internal class DebuggerComparisonTracker(private var memoryStart: Int) {
    private var owner: DesktopEmulatorRunner? = null
    private var lastStop: ComparisonCapture? = null
    private var beforeStep: ComparisonCapture? = null
    var frame = DebuggerComparisonFrame(memoryStart = memoryStart)
        private set

    fun reset(runner: DesktopEmulatorRunner?): DebuggerComparisonFrame {
        owner = runner
        beforeStep = null
        lastStop = runner?.let(::capture)
        frame = DebuggerComparisonFrame(memoryStart = memoryStart)
        return frame
    }

    fun running(runner: DesktopEmulatorRunner): DebuggerComparisonFrame {
        ensureOwner(runner)
        beforeStep = null
        frame = DebuggerComparisonFrame(cpuBasis = DebuggerComparisonBasis.RUNNING,
            memoryBasis = DebuggerComparisonBasis.RUNNING, memoryStart = memoryStart)
        return frame
    }

    fun preparingStep(runner: DesktopEmulatorRunner) {
        ensureOwner(runner)
        beforeStep = capture(runner)
    }

    fun stepped(runner: DesktopEmulatorRunner): DebuggerComparisonFrame {
        ensureOwner(runner)
        return compare(capture(runner), beforeStep, DebuggerComparisonBasis.STEP)
    }

    fun stopped(runner: DesktopEmulatorRunner): DebuggerComparisonFrame {
        if (runner !== owner) return reset(runner)
        return compare(capture(runner), lastStop, DebuggerComparisonBasis.STOP)
    }

    /** A newly selected page has no earlier observation; do not infer its history. */
    fun selectMemory(runner: DesktopEmulatorRunner?, address: Int): DebuggerComparisonFrame {
        val normalized = address and 0xffff
        if (runner !== owner) reset(runner)
        if (normalized == memoryStart) return frame
        memoryStart = normalized
        val memory = runner?.takeIf { it.state != RunnerState.RUNNING }?.let(::captureMemory)
        lastStop = lastStop?.copy(memory = memory)
        frame = frame.copy(memoryStart = memoryStart, memoryBasis = if (runner?.state == RunnerState.RUNNING)
            DebuggerComparisonBasis.RUNNING else DebuggerComparisonBasis.NONE,
            memoryChanges = emptyMap(), excludedMemoryBytes = 0)
        return frame
    }

    private fun ensureOwner(runner: DesktopEmulatorRunner) { if (runner !== owner) reset(runner) }
    private fun capture(runner: DesktopEmulatorRunner) = ComparisonCapture(runner.cpuSnapshot(), captureMemory(runner))

    private fun captureMemory(runner: DesktopEmulatorRunner): Map<Int, MemoryComparisonByte> =
        (0 until MEMORY_BYTES).associate { offset ->
            val address = (memoryStart + offset) and 0xffff
            address to MemoryComparisonByte(runner.memoryByte(address), runner.memoryLayout.locate(address).physicalAddress,
                runner.resolveRomLocation(address))
        }

    private fun compare(current: ComparisonCapture, previous: ComparisonCapture?, basis: DebuggerComparisonBasis): DebuggerComparisonFrame {
        var excluded = 0
        val changes = buildMap {
            current.memory.orEmpty().forEach { (address, value) ->
                val old = previous?.memory?.get(address) ?: return@forEach
                if (old.physicalAddress != value.physicalAddress || old.rom != value.rom) {
                    excluded++
                } else if (old.value != value.value) {
                    put(address, MemoryValueChange(address, old.value, value.value))
                }
            }
        }
        frame = DebuggerComparisonFrame(previous?.cpu, if (previous == null) DebuggerComparisonBasis.NONE else basis,
            if (previous?.memory == null) DebuggerComparisonBasis.NONE else basis, memoryStart, changes, excluded)
        lastStop = current
        beforeStep = null
        return frame
    }

    companion object { const val MEMORY_BYTES = 256 }
}

internal data class DebuggerRegisterValue(val name: String, val value: String, val previous: String?) {
    val changed: Boolean get() = previous != null && value != previous
}

internal object DebuggerRegisterModel {
    fun rows(current: CpuSnapshot?, previous: CpuSnapshot?): List<List<DebuggerRegisterValue>> {
        fun values(cpu: CpuSnapshot?): List<List<Pair<String, String>>> {
            val ram = cpu?.copyInternalRam()
            fun ram(index: Int) = ram?.getOrNull(index)?.toInt()?.and(255).comparisonHex(2)
            return listOf(
                listOf("PC" to cpu?.programCounter.comparisonHex(4), "CURRENT_PC" to cpu?.currentProgramCounter.comparisonHex(4),
                    "OP" to cpu?.opcode.comparisonHex(2), "DP" to cpu?.dataPointer.comparisonHex(4),
                    "P" to cpu?.p.comparisonHex(2), "Q" to cpu?.q.comparisonHex(2), "R" to cpu?.r.comparisonHex(2),
                    "Z" to (cpu?.zero?.let { if (it) "1" else "0" } ?: "-"), "C" to (cpu?.carry?.let { if (it) "1" else "0" } ?: "-")),
                listOf("I" to ram(0), "J" to ram(1), "A" to ram(2), "B" to ram(3),
                    "XL" to ram(4), "XH" to ram(5), "YL" to ram(6), "YH" to ram(7)),
                listOf("K" to ram(8), "L" to ram(9), "M" to ram(10), "N" to ram(11),
                    "IA" to cpu?.ia.comparisonHex(2), "IB" to cpu?.ib.comparisonHex(2),
                    "FO" to cpu?.fo.comparisonHex(2), "OUTC" to cpu?.control.comparisonHex(2)),
            )
        }
        val old = if (previous == null || current == null) emptyMap() else values(previous).flatten().toMap()
        return values(current).map { row -> row.map { (name, value) -> DebuggerRegisterValue(name, value, old[name]) } }
    }
}

private fun Int?.comparisonHex(width: Int): String = this?.let {
    (it and if (width == 2) 255 else 65535).toString(16).uppercase().padStart(width, '0')
} ?: "-".repeat(width)
