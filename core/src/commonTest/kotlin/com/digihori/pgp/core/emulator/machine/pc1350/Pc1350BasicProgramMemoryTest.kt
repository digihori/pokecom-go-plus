package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1350BasicProgramMemoryTest {
    @Test
    fun loadsAndExtractsAnS1Program() {
        val memory = ByteArray(0x10000)
        val program = byteArrayOf(0xff.toByte(), 0x00, 0x0a, 0x02, 0x91.toByte(), 0x0d, 0xff.toByte())
        val load = assertIs<Pc1245BasicMemoryResult.Success>(
            Pc1350BasicProgramMemory.load(program, { memory[it].toInt() and 0xff }, { a, v -> memory[a] = v.toByte() }),
        )
        assertEquals(0x2030, load.start)
        assertEquals(0x30, memory[0x6f01].toInt() and 0xff)
        assertEquals(0x20, memory[0x6f02].toInt() and 0xff)
        val extracted = assertIs<Pc1245BasicMemoryResult.Success>(
            Pc1350BasicProgramMemory.extract { memory[it].toInt() and 0xff },
        )
        assertContentEquals(program, extracted.bytes)
    }
}
