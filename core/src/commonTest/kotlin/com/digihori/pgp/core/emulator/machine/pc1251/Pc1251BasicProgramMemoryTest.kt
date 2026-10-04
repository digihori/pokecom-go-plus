package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicTokenizer
import com.digihori.pgp.core.source.basic.BasicTextParseResult
import com.digihori.pgp.core.source.basic.BasicTextParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1251BasicProgramMemoryTest {
    @Test
    fun loadsOldBasicAtThePc1251ProgramStartAndUpdatesPointers() {
        val memory = ByteArray(0x10000)
        setPointer(memory, Pc1251BasicProgramMemory.PROGRAM_START_POINTER_LOW, 0xb800)
        setPointer(memory, Pc1251BasicProgramMemory.PROGRAM_END_POINTER_LOW, 0xb7ff)
        val program = tokenize("10 PRINT \"PC-1251\"\n20 END")

        val loaded = assertIs<Pc1245BasicMemoryResult.Success>(
            Pc1251BasicProgramMemory.load(
                program,
                read = { memory[it].toInt() and 0xff },
                write = { address, value -> memory[address] = value.toByte() },
            ),
        )

        assertEquals(0xb800, loaded.start)
        assertEquals(0xb800 + program.lastIndex, loaded.end)
        assertContentEquals(program, memory.copyOfRange(loaded.start, loaded.end + 1))
        assertEquals(loaded.end, pointer(memory, Pc1251BasicProgramMemory.PROGRAM_END_POINTER_LOW))
        val extracted = assertIs<Pc1245BasicMemoryResult.Success>(
            Pc1251BasicProgramMemory.extract { memory[it].toInt() and 0xff },
        )
        assertContentEquals(program, extracted.bytes)
    }

    private fun tokenize(source: String): ByteArray {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(source)).document
        return assertIs<Pc1245BasicTokenizeResult.Success>(Pc1245BasicTokenizer.tokenize(document)).bytes
    }

    private fun setPointer(memory: ByteArray, lowAddress: Int, value: Int) {
        memory[lowAddress] = value.toByte()
        memory[lowAddress + 1] = (value ushr 8).toByte()
    }

    private fun pointer(memory: ByteArray, lowAddress: Int): Int =
        (memory[lowAddress].toInt() and 0xff) or ((memory[lowAddress + 1].toInt() and 0xff) shl 8)
}
