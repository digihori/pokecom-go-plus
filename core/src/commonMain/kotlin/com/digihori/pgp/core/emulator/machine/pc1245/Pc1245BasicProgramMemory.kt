package com.digihori.pgp.core.emulator.machine.pc1245

internal object Pc1245BasicProgramMemory {
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
        val capacity = Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE - range.start
        if (program.size > capacity) return Pc1245BasicMemoryResult.TooLarge(program.size, capacity)

        val previousEnd = range.end.takeIf { it >= range.start } ?: (range.start - 1)
        program.forEachIndexed { offset, byte -> write(range.start + offset, byte.toInt() and 0xff) }
        val newEnd = range.start + program.lastIndex
        if (previousEnd > newEnd) {
            for (address in (newEnd + 1)..previousEnd.coerceAtMost(Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE - 1)) {
                write(address, 0)
            }
        }
        writePointer(Pc1245BasicDialect.PROGRAM_START_POINTER_LOW, range.start, write)
        writePointer(Pc1245BasicDialect.PROGRAM_END_POINTER_LOW, newEnd, write)
        return Pc1245BasicMemoryResult.Success(range.start, newEnd, program.copyOf())
    }

    fun extract(read: (Int) -> Int): Pc1245BasicMemoryResult {
        val range = readRange(read) ?: return invalidPointers(read)
        if (range.end < range.start || range.end >= Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE) {
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
        val start = readPointer(Pc1245BasicDialect.PROGRAM_START_POINTER_LOW, read)
        val end = readPointer(Pc1245BasicDialect.PROGRAM_END_POINTER_LOW, read)
        return ProgramRange(start, end).takeIf {
            it.start in Pc1245BasicDialect.PROGRAM_TEXT_START until Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE &&
                it.end in (it.start - 1) until Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE
        }
    }

    private fun invalidPointers(read: (Int) -> Int): Pc1245BasicMemoryResult.InvalidPointers =
        Pc1245BasicMemoryResult.InvalidPointers(
            readPointer(Pc1245BasicDialect.PROGRAM_START_POINTER_LOW, read),
            readPointer(Pc1245BasicDialect.PROGRAM_END_POINTER_LOW, read),
        )

    private fun readPointer(lowAddress: Int, read: (Int) -> Int): Int =
        read(lowAddress) or (read(lowAddress + 1) shl 8)

    private fun writePointer(lowAddress: Int, value: Int, write: (Int, Int) -> Unit) {
        write(lowAddress, value and 0xff)
        write(lowAddress + 1, value ushr 8)
    }

    private data class ProgramRange(val start: Int, val end: Int)
}

internal sealed interface Pc1245BasicMemoryResult {
    data class Success(val start: Int, val end: Int, val bytes: ByteArray) : Pc1245BasicMemoryResult
    data class InvalidPointers(val start: Int, val end: Int) : Pc1245BasicMemoryResult
    data class TooLarge(val size: Int, val capacity: Int) : Pc1245BasicMemoryResult
    data class InvalidProgram(val offset: Int, val reason: String) : Pc1245BasicMemoryResult
}
