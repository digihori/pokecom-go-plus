package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.PhysicalRomLocation

internal enum class DebuggerStepStatus {
    IDLE, OVER_RUNNING, OUT_RUNNING, OVER_COMPLETE, OUT_COMPLETE,
    NO_FRAME, LIMIT, FRAME_LOST, UNSUPPORTED_FLOW, INTERRUPTED,
}

/** Only observed CALL/CAL/CASE1 frames; never reconstruct a call stack from arbitrary RAM. */
internal class DebuggerCallTracker {
    internal data class Frame(val id: Long, val returnPc: Int, val callerR: Int, val stackR: Int, val rom: PhysicalRomLocation?)
    internal data class Event(val entered: Long? = null, val returned: Long? = null, val lost: Boolean = false)
    private val frames = mutableListOf<Frame>()
    private var nextId = 0L
    val top: Frame? get() = frames.lastOrNull()
    fun clear() { frames.clear() }

    fun observe(before: CpuSnapshot, after: CpuSnapshot, opcode: Int, target: Int?, returnRom: PhysicalRomLocation?,
        currentRom: (Int) -> PhysicalRomLocation?): Event {
        fun lost(): Event { clear(); return Event(lost = true) }
        val ram = after.copyInternalRam()
        if (opcode == 0x37) {
            val frame = top ?: return Event()
            val beforeRam = before.copyInternalRam()
            if (before.r != frame.stackR || word(beforeRam, frame.stackR) != frame.returnPc ||
                after.r != frame.callerR || after.programCounter != frame.returnPc || currentRom(after.programCounter) != frame.rom) return lost()
            frames.removeAt(frames.lastIndex)
            if (frames.any { word(ram, it.stackR) != it.returnPc }) return lost()
            return Event(returned = frame.id)
        }
        if (frames.any { word(ram, it.stackR) != it.returnPc }) return lost()
        if (createsFrame(opcode)) {
            // CASE1 pushes an explicit return operand and falls through to its dispatch.
            val returnPc = if (opcode == 0x7a) target ?: return lost()
                else (before.programCounter + if (opcode == 0x78) 3 else 2) and 0xffff
            val destination = if (opcode == 0x7a) (before.programCounter + 4) and 0xffff else target
            val stackR = (before.r - 2) and 255
            if (frames.size >= 128 || after.r != stackR || after.programCounter != destination || word(ram, stackR) != returnPc ||
                frames.any { existing -> listOf(stackR, (stackR + 1) and 255).any { it == existing.stackR || it == ((existing.stackR + 1) and 255) } }) return lost()
            val frame = Frame(nextId++, returnPc, before.r, stackR, returnRom)
            frames += frame
            return Event(entered = frame.id)
        }
        return Event()
    }

    private fun word(ram: ByteArray, r: Int): Int = (ram[r].toInt() and 255) or ((ram[(r + 1) and 255].toInt() and 255) shl 8)
    companion object {
        fun isCall(opcode: Int): Boolean = opcode == 0x78 || opcode in 0xe0..0xff
        fun createsFrame(opcode: Int): Boolean = isCall(opcode) || opcode == 0x7a
    }
}
