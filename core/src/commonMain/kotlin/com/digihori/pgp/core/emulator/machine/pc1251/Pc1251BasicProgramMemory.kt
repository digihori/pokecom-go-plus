package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicDetokenizer
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult

/** The PC-1250/1251/1255 family uses OLD BASIC; the largest model can start at A000. */
internal object Pc1251BasicProgramMemory {
    const val PROGRAM_TEXT_START: Int = 0xa000
    const val PROGRAM_START_POINTER_LOW: Int = 0xc6e1
    const val PROGRAM_END_POINTER_LOW: Int = 0xc6e3
    const val PROGRAM_STORAGE_END_EXCLUSIVE: Int = PROGRAM_START_POINTER_LOW

    fun load(
        program: ByteArray,
        read: (Int) -> Int,
        write: (Int, Int) -> Unit,
    ): Pc1245BasicMemoryResult {
        val range = readRange(read) ?: return invalidPointers(read)
        val validation = Pc1245BasicDetokenizer.detokenize(program)
        if (validation is Pc1245BasicDetokenizeResult.Failure) {
            return Pc1245BasicMemoryResult.InvalidProgram(validation.error.offset, validation.error.message)
        }
        val capacity = PROGRAM_STORAGE_END_EXCLUSIVE - range.start
        if (program.size > capacity) return Pc1245BasicMemoryResult.TooLarge(program.size, capacity)

        val previousEnd = range.end.takeIf { it >= range.start } ?: (range.start - 1)
        program.forEachIndexed { offset, byte -> write(range.start + offset, byte.toInt() and 0xff) }
        val newEnd = range.start + program.lastIndex
        if (previousEnd > newEnd) {
            for (address in (newEnd + 1)..previousEnd.coerceAtMost(PROGRAM_STORAGE_END_EXCLUSIVE - 1)) {
                write(address, 0)
            }
        }
        writePointer(PROGRAM_START_POINTER_LOW, range.start, write)
        writePointer(PROGRAM_END_POINTER_LOW, newEnd, write)
        return Pc1245BasicMemoryResult.Success(range.start, newEnd, program.copyOf())
    }

    fun extract(read: (Int) -> Int): Pc1245BasicMemoryResult {
        val range = readRange(read) ?: return invalidPointers(read)
        if (range.end < range.start || range.end >= PROGRAM_STORAGE_END_EXCLUSIVE) {
            return Pc1245BasicMemoryResult.InvalidPointers(range.start, range.end)
        }
        val program = ByteArray(range.end - range.start + 1) { read(range.start + it).toByte() }
        val validation = Pc1245BasicDetokenizer.detokenize(program)
        if (validation is Pc1245BasicDetokenizeResult.Failure) {
            return Pc1245BasicMemoryResult.InvalidProgram(validation.error.offset, validation.error.message)
        }
        return Pc1245BasicMemoryResult.Success(range.start, range.end, program)
    }

    private fun readRange(read: (Int) -> Int): ProgramRange? {
        val start = readPointer(PROGRAM_START_POINTER_LOW, read)
        val end = readPointer(PROGRAM_END_POINTER_LOW, read)
        return ProgramRange(start, end).takeIf {
            it.start in PROGRAM_TEXT_START until PROGRAM_STORAGE_END_EXCLUSIVE &&
                it.end in (it.start - 1) until PROGRAM_STORAGE_END_EXCLUSIVE
        }
    }

    private fun invalidPointers(read: (Int) -> Int): Pc1245BasicMemoryResult.InvalidPointers =
        Pc1245BasicMemoryResult.InvalidPointers(
            readPointer(PROGRAM_START_POINTER_LOW, read),
            readPointer(PROGRAM_END_POINTER_LOW, read),
        )

    private fun readPointer(lowAddress: Int, read: (Int) -> Int): Int =
        read(lowAddress) or (read(lowAddress + 1) shl 8)

    private fun writePointer(lowAddress: Int, value: Int, write: (Int, Int) -> Unit) {
        write(lowAddress, value and 0xff)
        write(lowAddress + 1, value ushr 8)
    }

    private data class ProgramRange(val start: Int, val end: Int)
}
