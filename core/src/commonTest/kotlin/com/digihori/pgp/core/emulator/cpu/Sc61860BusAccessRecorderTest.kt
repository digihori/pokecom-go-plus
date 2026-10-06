package com.digihori.pgp.core.emulator.cpu

import com.digihori.pgp.core.api.MemoryAccessKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Sc61860BusAccessRecorderTest {
    @Test
    fun recordsCpuFacingReadsAndSameValueWritesOnlyWhileEnabled() {
        val memory = IntArray(0x10000)
        val recorder = Sc61860BusAccessRecorder(object : Sc61860Bus {
            override fun read(address: Int): Int = memory[address]
            override fun write(address: Int, value: Int) { memory[address] = value and 0xff }
        })
        recorder.read(0x1000)
        recorder.enabled = true
        recorder.read(0x1000)
        recorder.write(0xc000, 0)

        val events = recorder.drain()
        assertEquals(listOf(MemoryAccessKind.READ, MemoryAccessKind.WRITE), events.map { it.kind })
        assertEquals(listOf(0x1000, 0xc000), events.map { it.address })
        assertTrue(recorder.drain().isEmpty())
    }
}
