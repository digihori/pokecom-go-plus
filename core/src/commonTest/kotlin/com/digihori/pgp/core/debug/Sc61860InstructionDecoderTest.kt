package com.digihori.pgp.core.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class Sc61860InstructionDecoderTest {
    @Test
    fun decodesImmediateAndAbsoluteAddressOperands() {
        val memory = byteArrayOf(0x00, 0x12, 0x10, 0xC0.toByte(), 0x30)

        val immediate = decode(memory, 0)
        val absolute = decode(memory.copyOfRange(2, 5), 2)

        assertEquals("LII 0x12", Sc61860InstructionFormatter.format(immediate))
        assertEquals(2, immediate.length)
        assertEquals("LIDP 0xC030", Sc61860InstructionFormatter.format(absolute))
        assertEquals(0xc030, absolute.targetAddress)
        assertEquals(3, absolute.length)
    }

    @Test
    fun calculatesRelativeTargetsFromTheOperandAddress() {
        val forward = decode(byteArrayOf(0x2c, 0x10), 0x1000)
        val backward = decode(byteArrayOf(0x2d, 0x10), 0x1000)

        assertEquals(0x1011, forward.targetAddress)
        assertEquals(0x0ff1, backward.targetAddress)
        assertEquals("JRP 0x1011", Sc61860InstructionFormatter.format(forward))
        assertEquals("JRM 0x0FF1", Sc61860InstructionFormatter.format(backward))
    }

    @Test
    fun expandsPageCallOpcodeIntoItsDestination() {
        val decoded = decode(byteArrayOf(0xf1.toByte(), 0xe0.toByte()), 0xc000)

        assertEquals("CAL 0x11E0", Sc61860InstructionFormatter.format(decoded))
        assertEquals(Sc61860Flow.CALL, decoded.definition?.flow)
    }

    @Test
    fun preservesCompatibilityOpcodeLengthWithoutMakingItEncodable() {
        val decoded = decode(byteArrayOf(0x72, 0xaa.toByte()), 0)

        assertEquals(2, decoded.length)
        assertFalse(checkNotNull(decoded.definition).encodable)
    }

    @Test
    fun preservesTheRegisterEmbeddedInLpOpcode() {
        val decoded = decode(byteArrayOf(0x9f.toByte()), 0x1000)

        assertEquals("LP 0x1F", Sc61860InstructionFormatter.format(decoded))
        assertEquals(0x1f, decoded.operandValue)
    }

    @Test
    fun unknownOpcodeFallsBackToOneDataByte() {
        val decoded = decode(byteArrayOf(0x16), 0x2000)

        assertNull(decoded.definition)
        assertEquals("DB 0x16", Sc61860InstructionFormatter.format(decoded))
        assertEquals(1, decoded.length)
    }

    private fun decode(bytes: ByteArray, address: Int): Sc61860DecodedInstruction =
        Sc61860InstructionDecoder.decode(address) { absoluteAddress ->
            bytes[(absoluteAddress - address) and 0xffff].toInt()
        }
}
