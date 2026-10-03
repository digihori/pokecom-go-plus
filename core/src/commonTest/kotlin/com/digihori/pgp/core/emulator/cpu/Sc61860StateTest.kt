package com.digihori.pgp.core.emulator.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class Sc61860StateTest {
    @Test
    fun startsInResetState() {
        val state = Sc61860State()

        assertEquals(0, state.programCounter)
        assertEquals(0, state.currentProgramCounter)
        assertEquals(0, state.dataPointer)
        assertEquals(0, state.p)
        assertEquals(0, state.q)
        assertEquals(0x60, state.r)
        assertEquals(0, state.d)
        assertFalse(state.carry)
        assertFalse(state.zero)
        assertTrue(state.powerOn)
        assertEquals(0x100, state.internalRam.size)
        assertTrue(state.internalRam.all { it == 0 })
    }

    @Test
    fun resetRestoresRegistersPortsTimersAndInternalRam() {
        val state = Sc61860State().apply {
            programCounter = 0xffff
            currentProgramCounter = 0x1234
            opcode = 0xff
            dataPointer = 0xffff
            p = 0x7f
            q = 0x7f
            r = 0x7f
            d = 0xff
            alu = 0xff
            carry = true
            zero = true
            xInput = 0xff
            ticks = 10
            ticks2 = 20
            divider500 = true
            divider2 = true
            powerOn = false
            ia = 0xff
            ib = 0xff
            fo = 0xff
            control = 0xff
            testPort = 0xff
            internalRam.fill(0xff)
        }

        state.reset()

        assertEquals(0, state.programCounter)
        assertEquals(0, state.currentProgramCounter)
        assertEquals(0, state.opcode)
        assertEquals(0, state.dataPointer)
        assertEquals(0, state.p)
        assertEquals(0, state.q)
        assertEquals(0x60, state.r)
        assertEquals(0, state.d)
        assertEquals(0, state.alu)
        assertFalse(state.carry)
        assertFalse(state.zero)
        assertEquals(0, state.xInput)
        assertEquals(0, state.ticks)
        assertEquals(0, state.ticks2)
        assertFalse(state.divider500)
        assertFalse(state.divider2)
        assertTrue(state.powerOn)
        assertEquals(0, state.ia)
        assertEquals(0, state.ib)
        assertEquals(0, state.fo)
        assertEquals(0, state.control)
        assertEquals(0, state.testPort)
        assertTrue(state.internalRam.all { it == 0 })
    }

    @Test
    fun stateIsOwnedByEachCpuInstance() {
        val first = Sc61860State()
        val second = Sc61860State()

        first.programCounter = 0x1234
        first.internalRam[0x10] = 0x56

        assertNotSame(first.internalRam, second.internalRam)
        assertEquals(0, second.programCounter)
        assertEquals(0, second.internalRam[0x10])
    }
}
