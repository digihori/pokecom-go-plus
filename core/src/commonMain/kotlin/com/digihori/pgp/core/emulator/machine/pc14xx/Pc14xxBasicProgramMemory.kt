package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult

internal object Pc14xxBasicProgramMemory {
    fun load(model: Pc14xxModel, program: ByteArray, read: (Int) -> Int, write: (Int, Int) -> Unit): Pc1245BasicMemoryResult {
        validate(program)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        val configured = pointer(model.basicPointer, read)
        val start = configured.takeIf { it in model.basicStart until model.basicLimit } ?: model.basicStart
        val capacity = model.basicLimit - start
        if (program.size > capacity) return Pc1245BasicMemoryResult.TooLarge(program.size, capacity)
        program.forEachIndexed { offset, byte -> write(start + offset, byte.toInt() and 0xff) }
        val end = start + program.lastIndex
        writePointer(model.basicPointer, start, write)
        writePointer((model.basicPointer + 2), end, write)
        return Pc1245BasicMemoryResult.Success(start, end, program.copyOf())
    }

    fun extract(model: Pc14xxModel, read: (Int) -> Int): Pc1245BasicMemoryResult {
        val start = pointer(model.basicPointer, read)
        val end = pointer((model.basicPointer + 2), read)
        if (start !in model.basicStart until model.basicLimit || end !in start until model.basicLimit) {
            return Pc1245BasicMemoryResult.InvalidPointers(start, end)
        }
        val bytes = ByteArray(end - start + 1) { read(start + it).toByte() }
        validate(bytes)?.let { return Pc1245BasicMemoryResult.InvalidProgram(it.first, it.second) }
        return Pc1245BasicMemoryResult.Success(start, end, bytes)
    }

    private fun validate(program: ByteArray): Pair<Int, String>? {
        if (program.size < 2 || program.first().toInt() and 0xff != 0xff) return 0 to "Missing BASIC start marker"
        if (program.last().toInt() and 0xff != 0xff) return program.lastIndex to "Missing BASIC end marker"
        var offset = 1
        while (offset < program.lastIndex) {
            if (offset + 3 > program.lastIndex) return offset to "Truncated BASIC line header"
            val length = program[offset + 2].toInt() and 0xff
            if (length < 1 || offset + 3 + length > program.size) return offset + 2 to "Invalid BASIC line length"
            if (program[offset + 2 + length].toInt() and 0xff != 0x0d) return offset + 2 + length to "Missing BASIC line terminator"
            offset += 3 + length
        }
        return if (offset == program.lastIndex) null else offset to "Invalid BASIC program boundary"
    }

    private fun pointer(low: Int, read: (Int) -> Int) = read(low) or (read(low + 1) shl 8)
    private fun writePointer(low: Int, value: Int, write: (Int, Int) -> Unit) {
        write(low, value and 0xff); write(low + 1, value ushr 8)
    }
}
