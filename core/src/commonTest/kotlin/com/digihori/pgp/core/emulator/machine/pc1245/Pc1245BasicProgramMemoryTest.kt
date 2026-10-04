package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245BasicProgramMemoryTest {
    @Test
    fun loadsAndExtractsAValidatedProgramAndUpdatesPointers() {
        val memory = preparedMemory(previousEnd = 0xc020, fill = 0x55)
        val program = tokenize("10 PRINT \"HELLO\"\n20 END")

        val loaded = assertIs<Pc1245BasicMemoryResult.Success>(load(memory, program))

        assertEquals(0xc000, loaded.start)
        assertEquals(0xc000 + program.lastIndex, loaded.end)
        assertContentEquals(program, memory.copyOfRange(loaded.start, loaded.end + 1))
        assertEquals(loaded.end, pointer(memory, Pc1245BasicDialect.PROGRAM_END_POINTER_LOW))
        assertEquals(0, memory[loaded.end + 1].toInt())
        val extracted = assertIs<Pc1245BasicMemoryResult.Success>(extract(memory))
        assertContentEquals(program, extracted.bytes)
    }

    @Test
    fun rejectsInvalidPointersWithoutWriting() {
        val memory = ByteArray(0x10000) { 0x33 }
        setPointer(memory, Pc1245BasicDialect.PROGRAM_START_POINTER_LOW, 0x0000)
        setPointer(memory, Pc1245BasicDialect.PROGRAM_END_POINTER_LOW, 0x0000)
        val before = memory.copyOf()

        assertIs<Pc1245BasicMemoryResult.InvalidPointers>(load(memory, tokenize("10 END")))
        assertContentEquals(before, memory)
    }

    @Test
    fun acceptsTheEmptyProgramEndPointerAtStartMinusOne() {
        val memory = preparedMemory(previousEnd = 0xbfff, fill = 0)
        val program = tokenize("10 END")

        assertIs<Pc1245BasicMemoryResult.Success>(load(memory, program))
    }

    @Test
    fun validatesTheImageBeforeWriting() {
        val memory = preparedMemory(previousEnd = 0xc010, fill = 0x44)
        val before = memory.copyOf()

        val result = load(memory, bytes(0xff, 0xe0, 0x10, 0xff))

        assertIs<Pc1245BasicMemoryResult.InvalidProgram>(result)
        assertContentEquals(before, memory)
    }

    @Test
    fun rejectsProgramsThatWouldReachThePointerArea() {
        val memory = preparedMemory(previousEnd = 0xc000, fill = 0)
        val oversized = buildList {
            add(0xff)
            repeat(600) {
                add(0xe0)
                add(0x01)
                add(0x00)
            }
            add(0xff)
        }.map(Int::toByte).toByteArray()

        val result = assertIs<Pc1245BasicMemoryResult.TooLarge>(load(memory, oversized))

        assertEquals(Pc1245BasicDialect.PROGRAM_STORAGE_END_EXCLUSIVE - 0xc000, result.capacity)
    }

    private fun load(memory: ByteArray, program: ByteArray): Pc1245BasicMemoryResult =
        Pc1245BasicProgramMemory.load(
            program,
            read = { memory[it].toInt() and 0xff },
            write = { address, value -> memory[address] = value.toByte() },
        )

    private fun extract(memory: ByteArray): Pc1245BasicMemoryResult =
        Pc1245BasicProgramMemory.extract { memory[it].toInt() and 0xff }

    private fun preparedMemory(previousEnd: Int, fill: Int): ByteArray =
        ByteArray(0x10000).also { memory ->
            memory.fill(fill.toByte(), 0xc000, previousEnd + 1)
            setPointer(memory, Pc1245BasicDialect.PROGRAM_START_POINTER_LOW, 0xc000)
            setPointer(memory, Pc1245BasicDialect.PROGRAM_END_POINTER_LOW, previousEnd)
        }

    private fun setPointer(memory: ByteArray, lowAddress: Int, value: Int) {
        memory[lowAddress] = value.toByte()
        memory[lowAddress + 1] = (value ushr 8).toByte()
    }

    private fun pointer(memory: ByteArray, lowAddress: Int): Int =
        (memory[lowAddress].toInt() and 0xff) or ((memory[lowAddress + 1].toInt() and 0xff) shl 8)

    private fun tokenize(source: String): ByteArray {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        return assertIs<Pc1245BasicTokenizeResult.Success>(Pc1245BasicTokenizer.tokenize(document)).bytes
    }

    private fun bytes(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()
}
