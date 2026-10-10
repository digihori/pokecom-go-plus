package com.digihori.pgp.core.emulator.machine.pc1261

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult

internal object Pc1261BasicProgramMemory {
    private const val START_POINTER_LOW = 0x66e1
    private const val END_POINTER_LOW = 0x66e3
    private const val DEFAULT_START = 0x4080
    private const val STORAGE_END_EXCLUSIVE = 0x66e0

    fun load(program: ByteArray, read: (Int) -> Int, write: (Int, Int) -> Unit): Pc1245BasicMemoryResult {
        validate(program)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        val configured = pointer(START_POINTER_LOW, read)
        val start = configured.takeIf { it in DEFAULT_START until STORAGE_END_EXCLUSIVE } ?: DEFAULT_START
        val capacity = STORAGE_END_EXCLUSIVE - start
        if (program.size > capacity) return Pc1245BasicMemoryResult.TooLarge(program.size, capacity)
        program.forEachIndexed { offset, byte -> write(start + offset, byte.toInt() and 0xff) }
        val end = start + program.lastIndex
        writePointer(START_POINTER_LOW, start, write)
        writePointer(END_POINTER_LOW, end, write)
        return Pc1245BasicMemoryResult.Success(start, end, program.copyOf())
    }

    fun extract(read: (Int) -> Int): Pc1245BasicMemoryResult {
        val start = pointer(START_POINTER_LOW, read)
        val end = pointer(END_POINTER_LOW, read)
        if (start !in DEFAULT_START until STORAGE_END_EXCLUSIVE || end !in start until STORAGE_END_EXCLUSIVE) {
            return Pc1245BasicMemoryResult.InvalidPointers(start, end)
        }
        val bytes = ByteArray(end - start + 1) { read(start + it).toByte() }
        validate(bytes)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        return Pc1245BasicMemoryResult.Success(start, end, bytes)
    }

    private fun validate(program: ByteArray): Pair<Int, String>? {
        if (program.size < 2 || program.first().toInt() and 0xff != 0xff) return 0 to "Missing S1 start marker"
        if (program.last().toInt() and 0xff != 0xff) return program.lastIndex to "Missing S1 end marker"
        var offset = 1
        while (offset < program.lastIndex) {
            if (offset + 3 > program.lastIndex) return offset to "Truncated S1 line header"
            val length = program[offset + 2].toInt() and 0xff
            if (length < 1 || offset + 3 + length > program.size) return offset + 2 to "Invalid S1 line length"
            if (program[offset + 2 + length].toInt() and 0xff != 0x0d) return offset + 2 + length to "Missing S1 line terminator"
            offset += 3 + length
        }
        return if (offset == program.lastIndex) null else offset to "Invalid S1 program boundary"
    }

    private fun pointer(low: Int, read: (Int) -> Int) = read(low) or (read(low + 1) shl 8)
    private fun writePointer(low: Int, value: Int, write: (Int, Int) -> Unit) {
        write(low, value and 0xff); write(low + 1, value ushr 8)
    }
}
