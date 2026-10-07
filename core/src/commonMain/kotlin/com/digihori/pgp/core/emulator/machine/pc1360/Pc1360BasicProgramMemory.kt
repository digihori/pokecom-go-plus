package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult

internal object Pc1360BasicProgramMemory {
    private const val START_POINTER_LOW = 0xffd7
    private const val END_POINTER_LOW = 0xffd9
    private const val DEFAULT_START = 0x8030
    private const val STORAGE_END_EXCLUSIVE = 0xffd7

    fun load(program: ByteArray, read: (Int) -> Int, write: (Int, Int) -> Unit): Pc1245BasicMemoryResult {
        validate(program)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        val configured = pointer(START_POINTER_LOW, read)
        val start = configured.takeIf { it in DEFAULT_START until STORAGE_END_EXCLUSIVE } ?: DEFAULT_START
        val capacity = STORAGE_END_EXCLUSIVE - start
        if (program.size > capacity) return Pc1245BasicMemoryResult.TooLarge(program.size, capacity)
        program.forEachIndexed { offset, byte -> write(start + offset, byte.toInt() and 0xff) }
        val end = start + program.lastIndex
        writePointer(START_POINTER_LOW, start, write); writePointer(END_POINTER_LOW, end, write)
        return Pc1245BasicMemoryResult.Success(start, end, program.copyOf())
    }
    fun extract(read: (Int) -> Int): Pc1245BasicMemoryResult {
        val start = pointer(START_POINTER_LOW, read); val end = pointer(END_POINTER_LOW, read)
        if (start !in DEFAULT_START until STORAGE_END_EXCLUSIVE || end !in start until STORAGE_END_EXCLUSIVE) return Pc1245BasicMemoryResult.InvalidPointers(start, end)
        val bytes = ByteArray(end - start + 1) { read(start + it).toByte() }
        validate(bytes)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        return Pc1245BasicMemoryResult.Success(start, end, bytes)
    }
    private fun validate(bytes: ByteArray): Pair<Int, String>? = when {
        bytes.size < 2 || bytes.first().toInt() and 0xff != 0xff -> 0 to "Missing S2 start marker"
        bytes.last().toInt() and 0xff != 0xff -> bytes.lastIndex to "Missing S2 end marker"
        else -> null
    }
    private fun pointer(low: Int, read: (Int) -> Int) = read(low) or (read(low + 1) shl 8)
    private fun writePointer(low: Int, value: Int, write: (Int, Int) -> Unit) { write(low, value and 0xff); write(low + 1, value ushr 8) }
}
