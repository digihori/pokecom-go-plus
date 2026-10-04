package com.digihori.pgp.core.emulator.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Sc61860CpuTest {
    @Test
    fun immediateLoadsUpdateTheExpectedInternalRegisters() {
        val bus = TestBus(
            0x00, 0x11,
            0x01, 0x22,
            0x02, 0x33,
            0x03, 0x44,
        )
        val cpu = Sc61860Cpu(bus)

        repeat(4) {
            val result = cpu.step()
            assertEquals(4, result.cycles)
            assertNull(result.stopReason)
        }

        assertEquals(0x11, cpu.state.internalRam[0x00])
        assertEquals(0x22, cpu.state.internalRam[0x01])
        assertEquals(0x33, cpu.state.internalRam[0x02])
        assertEquals(0x44, cpu.state.internalRam[0x03])
        assertEquals(8, cpu.state.programCounter)
        assertEquals(6, cpu.state.currentProgramCounter)
        assertEquals(0x03, cpu.state.opcode)
    }

    @Test
    fun noptAliasesConsumeThreeCyclesWithoutChangingCpuData() {
        val cpu = Sc61860Cpu(TestBus(0x33, 0x68, 0x6a, 0xcd))

        repeat(4) { address ->
            val result = cpu.step()
            assertEquals(3, result.cycles)
            assertNull(result.stopReason)
            assertEquals(address + 1, cpu.state.programCounter)
        }

        assertEquals(0xcd, cpu.state.opcode)
        assertEquals(3, cpu.state.currentProgramCounter)
        assertEquals(0, cpu.state.internalRam.sum())
    }

    @Test
    fun unsupportedOpcodeStopsWithItsAddressAndValue() {
        val cpu = Sc61860Cpu(TestBus(0x16))

        val result = cpu.step()

        assertEquals(0, result.cycles)
        val reason = assertIs<Sc61860StopReason.UnsupportedOpcode>(result.stopReason)
        assertEquals(0, reason.address)
        assertEquals(0x16, reason.opcode)
        assertEquals(1, cpu.state.programCounter)
    }

    @Test
    fun programCounterWrapsAtSixteenBits() {
        val bus = TestBus().apply {
            this[0xffff] = 0x00
            this[0x0000] = 0xab
        }
        val cpu = Sc61860Cpu(bus).apply {
            state.programCounter = 0xffff
        }

        val result = cpu.step()

        assertEquals(4, result.cycles)
        assertEquals(0xab, cpu.state.internalRam[0x00])
        assertEquals(1, cpu.state.programCounter)
    }

    @Test
    fun resetDoesNotMutateMachineMemory() {
        val bus = TestBus(0x02, 0x55)
        val cpu = Sc61860Cpu(bus)
        cpu.step()

        cpu.reset()

        assertEquals(0x02, bus[0])
        assertEquals(0x55, bus[1])
        assertEquals(0, cpu.state.internalRam[0x02])
    }

    @Test
    fun lipAndExamCanPrepareAnIoPortRegister() {
        val cpu = Sc61860Cpu(TestBus(0x12, 0x5c, 0x02, 0x40, 0xdb, 0x5d))

        assertEquals(4, cpu.step().cycles)
        assertEquals(0x5c, cpu.state.p)
        assertEquals(4, cpu.step().cycles)
        assertEquals(3, cpu.step().cycles)
        assertEquals(0x40, cpu.state.internalRam[0x5c])
        assertEquals(0, cpu.state.internalRam[0x02])
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x5c, cpu.state.q)
        assertEquals(0x40, cpu.state.ia)
    }

    @Test
    fun inputInstructionsReadMachineIoAndUpdateTheZeroFlag() {
        val io = RecordingIo(inputA = 0x24, inputB = 0)
        val cpu = Sc61860Cpu(TestBus(0x4c, 0xcc), io).apply {
            state.ia = 0x08
            state.ib = 0x02
        }

        assertEquals(2, cpu.step().cycles)
        assertEquals(0x24, cpu.state.internalRam[0x02])
        assertEquals(false, cpu.state.zero)
        assertEquals(0x08 to 0x02, io.lastInputASelection)

        assertEquals(2, cpu.step().cycles)
        assertEquals(0, cpu.state.internalRam[0x02])
        assertEquals(true, cpu.state.zero)
        assertEquals(0x02, io.lastInputBSelection)
    }

    @Test
    fun outbCopiesItsInternalPortRegister() {
        val cpu = Sc61860Cpu(TestBus(0xdd)).apply {
            state.internalRam[0x5d] = 0x08
        }

        val result = cpu.step()

        assertEquals(2, result.cycles)
        assertEquals(0x5d, cpu.state.q)
        assertEquals(0x08, cpu.state.ib)
    }

    @Test
    fun lidpLoadsABigEndianSixteenBitAddress() {
        val cpu = Sc61860Cpu(TestBus(0x10, 0xab, 0xcd))

        val result = cpu.step()

        assertEquals(8, result.cycles)
        assertEquals(0xabcd, cpu.state.dataPointer)
        assertEquals(3, cpu.state.programCounter)
    }

    @Test
    fun pushAndPopUseTheInternalRamStack() {
        val cpu = Sc61860Cpu(TestBus(0x34, 0x5b)).apply {
            state.internalRam[0x02] = 0xa5
        }

        assertEquals(3, cpu.step().cycles)
        assertEquals(0x5f, cpu.state.r)
        assertEquals(0xa5, cpu.state.internalRam[0x5f])

        cpu.state.internalRam[0x02] = 0
        assertEquals(2, cpu.step().cycles)
        assertEquals(0xa5, cpu.state.internalRam[0x02])
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun callAndReturnPreserveTheAddressAfterTheOperands() {
        val bus = TestBus(0x78, 0x12, 0x34).apply {
            this[0x1234] = 0x37
        }
        val cpu = Sc61860Cpu(bus)

        assertEquals(8, cpu.step().cycles)
        assertEquals(0x1234, cpu.state.programCounter)
        assertEquals(0x5e, cpu.state.r)
        assertEquals(0x03, cpu.state.internalRam[0x5e])
        assertEquals(0x00, cpu.state.internalRam[0x5f])

        assertEquals(4, cpu.step().cycles)
        assertEquals(3, cpu.state.programCounter)
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun compactCallUsesTheOpcodeAsTheDestinationPage() {
        val cpu = Sc61860Cpu(TestBus(0xe5, 0x67))

        val result = cpu.step()

        assertEquals(7, result.cycles)
        assertEquals(0x0567, cpu.state.programCounter)
        assertEquals(0x02, cpu.state.internalRam[0x5e])
        assertEquals(0x00, cpu.state.internalRam[0x5f])
    }

    @Test
    fun absoluteJumpAlwaysConsumesItsTwoByteOperand() {
        val cpu = Sc61860Cpu(TestBus(0x79, 0x43, 0x21))

        val result = cpu.step()

        assertEquals(6, result.cycles)
        assertEquals(0x4321, cpu.state.programCounter)
    }

    @Test
    fun conditionalAbsoluteJumpsUseZeroAndCarryFlags() {
        assertConditionalJump(opcode = 0x7c, zero = false, carry = false, taken = true)
        assertConditionalJump(opcode = 0x7c, zero = true, carry = false, taken = false)
        assertConditionalJump(opcode = 0x7d, zero = false, carry = false, taken = true)
        assertConditionalJump(opcode = 0x7d, zero = false, carry = true, taken = false)
        assertConditionalJump(opcode = 0x7e, zero = true, carry = false, taken = true)
        assertConditionalJump(opcode = 0x7e, zero = false, carry = false, taken = false)
        assertConditionalJump(opcode = 0x7f, zero = false, carry = true, taken = true)
        assertConditionalJump(opcode = 0x7f, zero = false, carry = false, taken = false)
    }

    @Test
    fun unconditionalRelativeJumpsUseTheOperandAddressAsTheirBase() {
        val forward = cpuAt(0x1000, 0x2c, 0x20)
        val backward = cpuAt(0x1000, 0x2d, 0x20)

        assertEquals(7, forward.step().cycles)
        assertEquals(0x1021, forward.state.programCounter)
        assertEquals(0x20, forward.state.internalRam[0x5f])

        assertEquals(7, backward.step().cycles)
        assertEquals(0x0fe1, backward.state.programCounter)
        assertEquals(0x20, backward.state.internalRam[0x5f])
    }

    @Test
    fun conditionalRelativeJumpsUseZeroAndCarryFlags() {
        assertConditionalRelativeJump(0x28, zero = false, carry = false, taken = true)
        assertConditionalRelativeJump(0x28, zero = true, carry = false, taken = false)
        assertConditionalRelativeJump(0x29, zero = false, carry = false, taken = true)
        assertConditionalRelativeJump(0x29, zero = true, carry = false, taken = false)
        assertConditionalRelativeJump(0x2a, zero = false, carry = false, taken = true)
        assertConditionalRelativeJump(0x2a, zero = false, carry = true, taken = false)
        assertConditionalRelativeJump(0x2b, zero = false, carry = false, taken = true)
        assertConditionalRelativeJump(0x2b, zero = false, carry = true, taken = false)
        assertConditionalRelativeJump(0x38, zero = true, carry = false, taken = true)
        assertConditionalRelativeJump(0x38, zero = false, carry = false, taken = false)
        assertConditionalRelativeJump(0x39, zero = true, carry = false, taken = true)
        assertConditionalRelativeJump(0x39, zero = false, carry = false, taken = false)
        assertConditionalRelativeJump(0x3a, zero = false, carry = true, taken = true)
        assertConditionalRelativeJump(0x3a, zero = false, carry = false, taken = false)
        assertConditionalRelativeJump(0x3b, zero = false, carry = true, taken = true)
        assertConditionalRelativeJump(0x3b, zero = false, carry = false, taken = false)
    }

    @Test
    fun relativeJumpsWrapAtSixteenBits() {
        val forward = cpuAt(0xfffe, 0x2c, 0x03)
        val backward = cpuAt(0x0000, 0x2d, 0x03)

        forward.step()
        backward.step()

        assertEquals(0x0002, forward.state.programCounter)
        assertEquals(0xfffe, backward.state.programCounter)
    }

    @Test
    fun registerIncrementAndDecrementOpcodesSelectTheExpectedRegister() {
        val pairs = listOf(
            Triple(0x40, 0x41, 0x00),
            Triple(0xc0, 0xc1, 0x01),
            Triple(0x42, 0x43, 0x02),
            Triple(0xc2, 0xc3, 0x03),
            Triple(0x48, 0x49, 0x08),
            Triple(0xc8, 0xc9, 0x09),
            Triple(0x4a, 0x4b, 0x0a),
            Triple(0xca, 0xcb, 0x0b),
        )

        pairs.forEach { (incrementOpcode, decrementOpcode, register) ->
            val increment = Sc61860Cpu(TestBus(incrementOpcode)).apply {
                state.internalRam[register] = 0x7e
            }
            assertEquals(4, increment.step().cycles)
            assertEquals(0x7f, increment.state.internalRam[register])
            assertEquals(register, increment.state.q)

            val decrement = Sc61860Cpu(TestBus(decrementOpcode)).apply {
                state.internalRam[register] = 0x7e
            }
            assertEquals(4, decrement.step().cycles)
            assertEquals(0x7d, decrement.state.internalRam[register])
            assertEquals(register, decrement.state.q)
        }
    }

    @Test
    fun registerArithmeticUpdatesZeroCarryAndAluAtByteBoundaries() {
        val increment = Sc61860Cpu(TestBus(0x42)).apply {
            state.internalRam[0x02] = 0xff
        }
        increment.step()
        assertEquals(0, increment.state.internalRam[0x02])
        assertEquals(0x0100, increment.state.alu)
        assertEquals(true, increment.state.zero)
        assertEquals(true, increment.state.carry)

        val decrement = Sc61860Cpu(TestBus(0x43)).apply {
            state.internalRam[0x02] = 0
        }
        decrement.step()
        assertEquals(0xff, decrement.state.internalRam[0x02])
        assertEquals(0xffff, decrement.state.alu)
        assertEquals(false, decrement.state.zero)
        assertEquals(true, decrement.state.carry)

        val reachesZero = Sc61860Cpu(TestBus(0x43)).apply {
            state.internalRam[0x02] = 1
        }
        reachesZero.step()
        assertEquals(true, reachesZero.state.zero)
        assertEquals(false, reachesZero.state.carry)
    }

    @Test
    fun clraDoesNotChangeFlags() {
        val cpu = Sc61860Cpu(TestBus(0x23)).apply {
            state.internalRam[0x02] = 0xa5
            state.zero = false
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(2, result.cycles)
        assertEquals(0, cpu.state.internalRam[0x02])
        assertEquals(false, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun scAndRcSetZeroWhileUpdatingCarry() {
        val cpu = Sc61860Cpu(TestBus(0xd0, 0xd1)).apply {
            state.zero = false
        }

        assertEquals(2, cpu.step().cycles)
        assertEquals(true, cpu.state.carry)
        assertEquals(true, cpu.state.zero)

        cpu.state.zero = false
        assertEquals(2, cpu.step().cycles)
        assertEquals(false, cpu.state.carry)
        assertEquals(true, cpu.state.zero)
    }

    @Test
    fun immediateAddAndSubtractWriteAOrPAndUpdateFlags() {
        val cases = listOf(
            ImmediateWriteCase(0x70, register = 0x20, initial = 0xff, immediate = 1, expected = 0),
            ImmediateWriteCase(0x71, register = 0x20, initial = 0, immediate = 1, expected = 0xff),
            ImmediateWriteCase(0x74, register = 0x02, initial = 0xff, immediate = 1, expected = 0),
            ImmediateWriteCase(0x75, register = 0x02, initial = 0, immediate = 1, expected = 0xff),
        )

        cases.forEach { case ->
            val cpu = Sc61860Cpu(TestBus(case.opcode, case.immediate)).apply {
                state.p = 0x20
                state.internalRam[case.register] = case.initial
            }

            val result = cpu.step()

            assertEquals(4, result.cycles)
            assertEquals(2, cpu.state.programCounter)
            assertEquals(case.expected, cpu.state.internalRam[case.register])
            assertEquals(case.expected == 0, cpu.state.zero)
            assertEquals(true, cpu.state.carry)
        }
    }

    @Test
    fun immediateAndOrWriteTheirResultButPreserveCarry() {
        val andP = Sc61860Cpu(TestBus(0x60, 0x0f)).apply {
            state.p = 0x20
            state.internalRam[0x20] = 0xf0
            state.carry = true
        }
        andP.step()
        assertEquals(0, andP.state.internalRam[0x20])
        assertEquals(true, andP.state.zero)
        assertEquals(true, andP.state.carry)

        val orA = Sc61860Cpu(TestBus(0x65, 0x03)).apply {
            state.internalRam[0x02] = 0x30
            state.carry = true
        }
        orA.step()
        assertEquals(0x33, orA.state.internalRam[0x02])
        assertEquals(false, orA.state.zero)
        assertEquals(true, orA.state.carry)

        val andA = Sc61860Cpu(TestBus(0x64, 0xf0)).apply {
            state.internalRam[0x02] = 0x3c
        }
        andA.step()
        assertEquals(0x30, andA.state.internalRam[0x02])

        val orP = Sc61860Cpu(TestBus(0x61, 0x03)).apply {
            state.p = 0x20
            state.internalRam[0x20] = 0x30
        }
        orP.step()
        assertEquals(0x33, orP.state.internalRam[0x20])
    }

    @Test
    fun immediateTestOnlyUpdatesZero() {
        listOf(0x62 to 0x20, 0x66 to 0x02).forEach { (opcode, register) ->
            val cpu = Sc61860Cpu(TestBus(opcode, 0x0f)).apply {
                state.p = 0x20
                state.internalRam[register] = 0xf0
                state.alu = 0x1234
                state.carry = true
            }

            assertEquals(4, cpu.step().cycles)
            assertEquals(0xf0, cpu.state.internalRam[register])
            assertEquals(0x1234, cpu.state.alu)
            assertEquals(true, cpu.state.zero)
            assertEquals(true, cpu.state.carry)
        }
    }

    @Test
    fun immediateCompareUpdatesFlagsWithoutWritingTheRegister() {
        listOf(0x63 to 0x20, 0x67 to 0x02).forEach { (opcode, register) ->
            val cpu = Sc61860Cpu(TestBus(opcode, 0x20)).apply {
                state.p = 0x20
                state.internalRam[register] = 0x10
            }

            assertEquals(4, cpu.step().cycles)
            assertEquals(0x10, cpu.state.internalRam[register])
            assertEquals(0xfff0, cpu.state.alu)
            assertEquals(false, cpu.state.zero)
            assertEquals(true, cpu.state.carry)
        }
    }

    @Test
    fun accumulatorArithmeticWritesPWithoutChangingA() {
        val add = cpuWithAccumulatorOperation(0x44, pValue = 0xfe, aValue = 1)
        assertEquals(3, add.step().cycles)
        assertEquals(0xff, add.state.internalRam[0x20])
        assertEquals(false, add.state.carry)
        assertEquals(1, add.state.internalRam[0x02])

        val addCarry = cpuWithAccumulatorOperation(0xc4, pValue = 0xfe, aValue = 1).apply {
            state.carry = true
        }
        addCarry.step()
        assertEquals(0, addCarry.state.internalRam[0x20])
        assertEquals(true, addCarry.state.zero)
        assertEquals(true, addCarry.state.carry)

        val subtract = cpuWithAccumulatorOperation(0x45, pValue = 2, aValue = 1)
        subtract.step()
        assertEquals(1, subtract.state.internalRam[0x20])
        assertEquals(false, subtract.state.carry)

        val subtractCarry = cpuWithAccumulatorOperation(0xc5, pValue = 1, aValue = 1).apply {
            state.carry = true
        }
        subtractCarry.step()
        assertEquals(0xff, subtractCarry.state.internalRam[0x20])
        assertEquals(true, subtractCarry.state.carry)
    }

    @Test
    fun accumulatorLogicalOperationsWritePAndPreserveCarry() {
        val and = cpuWithAccumulatorOperation(0x46, pValue = 0xf0, aValue = 0x0f).apply {
            state.carry = true
        }
        and.step()
        assertEquals(0, and.state.internalRam[0x20])
        assertEquals(true, and.state.zero)
        assertEquals(true, and.state.carry)

        val or = cpuWithAccumulatorOperation(0x47, pValue = 0x30, aValue = 0x03).apply {
            state.carry = true
        }
        or.step()
        assertEquals(0x33, or.state.internalRam[0x20])
        assertEquals(false, or.state.zero)
        assertEquals(true, or.state.carry)
    }

    @Test
    fun tsmaAndCpmaDoNotWriteEitherOperand() {
        val test = cpuWithAccumulatorOperation(0xc6, pValue = 0xf0, aValue = 0x0f).apply {
            state.alu = 0x1234
            state.carry = true
        }
        assertEquals(3, test.step().cycles)
        assertEquals(0xf0, test.state.internalRam[0x20])
        assertEquals(0x0f, test.state.internalRam[0x02])
        assertEquals(0x1234, test.state.alu)
        assertEquals(true, test.state.zero)
        assertEquals(true, test.state.carry)

        val compare = cpuWithAccumulatorOperation(0xc7, pValue = 0x10, aValue = 0x20)
        assertEquals(3, compare.step().cycles)
        assertEquals(0x10, compare.state.internalRam[0x20])
        assertEquals(0x20, compare.state.internalRam[0x02])
        assertEquals(0xfff0, compare.state.alu)
        assertEquals(false, compare.state.zero)
        assertEquals(true, compare.state.carry)
    }

    @Test
    fun lidlReplacesOnlyTheLowByteOfTheDataPointer() {
        val cpu = Sc61860Cpu(TestBus(0x11, 0x34)).apply {
            state.dataPointer = 0xabcd
        }

        val result = cpu.step()

        assertEquals(5, result.cycles)
        assertEquals(0xab34, cpu.state.dataPointer)
        assertEquals(2, cpu.state.programCounter)
    }

    @Test
    fun lipAndLiqMaskImmediateValuesToSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x12, 0xff, 0x13, 0x80))

        assertEquals(4, cpu.step().cycles)
        assertEquals(0x7f, cpu.state.p)
        assertEquals(4, cpu.step().cycles)
        assertEquals(0, cpu.state.q)
    }

    @Test
    fun pointerLoadInstructionsCopyPqAndRIntoTheAccumulator() {
        val cpu = Sc61860Cpu(TestBus(0x20, 0x21, 0x22)).apply {
            state.p = 0x12
            state.q = 0x34
            state.r = 0x56
        }

        assertEquals(2, cpu.step().cycles)
        assertEquals(0x12, cpu.state.internalRam[0x02])
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x34, cpu.state.internalRam[0x02])
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x56, cpu.state.internalRam[0x02])
    }

    @Test
    fun pointerStoreInstructionsMaskTheAccumulatorToSevenBits() {
        listOf(0x30, 0x31, 0x32).forEach { opcode ->
            val cpu = Sc61860Cpu(TestBus(opcode)).apply {
                state.internalRam[0x02] = 0xff
            }

            assertEquals(2, cpu.step().cycles)
            val stored = when (opcode) {
                0x30 -> cpu.state.p
                0x31 -> cpu.state.q
                else -> cpu.state.r
            }
            assertEquals(0x7f, stored)
        }
    }

    @Test
    fun incpAndDecpWrapWithinSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x50, 0x51)).apply {
            state.p = 0x7f
        }

        assertEquals(2, cpu.step().cycles)
        assertEquals(0, cpu.state.p)
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x7f, cpu.state.p)
    }

    @Test
    fun lpOpcodeEncodesTheLowSixBitsOfP() {
        val cpu = Sc61860Cpu(TestBus(0x80, 0xa5, 0xbf))

        assertEquals(2, cpu.step().cycles)
        assertEquals(0, cpu.state.p)
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x25, cpu.state.p)
        assertEquals(2, cpu.step().cycles)
        assertEquals(0x3f, cpu.state.p)
    }

    @Test
    fun stdAndMvdmWriteAccumulatorOrPToTheDataPointer() {
        val stdBus = TestBus(0x52)
        val std = Sc61860Cpu(stdBus).apply {
            state.dataPointer = 0x9000
            state.internalRam[0x02] = 0xa5
        }
        assertEquals(2, std.step().cycles)
        assertEquals(0xa5, stdBus[0x9000])
        assertEquals(0x9000, std.state.dataPointer)

        val mvdmBus = TestBus(0x53)
        val mvdm = Sc61860Cpu(mvdmBus).apply {
            state.dataPointer = 0x9000
            state.p = 0x20
            state.internalRam[0x20] = 0x5a
        }
        assertEquals(3, mvdm.step().cycles)
        assertEquals(0x5a, mvdmBus[0x9000])
        assertEquals(0x20, mvdm.state.p)
    }

    @Test
    fun mvmdAndLddReadTheDataPointerWithoutAdvancingIt() {
        val mvmdBus = TestBus(0x55).apply { this[0x9000] = 0xa5 }
        val mvmd = Sc61860Cpu(mvmdBus).apply {
            state.dataPointer = 0x9000
            state.p = 0x20
        }
        assertEquals(3, mvmd.step().cycles)
        assertEquals(0xa5, mvmd.state.internalRam[0x20])
        assertEquals(0x9000, mvmd.state.dataPointer)

        val lddBus = TestBus(0x57).apply { this[0x9000] = 0x5a }
        val ldd = Sc61860Cpu(lddBus).apply { state.dataPointer = 0x9000 }
        assertEquals(3, ldd.step().cycles)
        assertEquals(0x5a, ldd.state.internalRam[0x02])
        assertEquals(0x9000, ldd.state.dataPointer)
    }

    @Test
    fun mvmpAndLdpcReadAtPcWithoutConsumingTheByte() {
        val mvmp = Sc61860Cpu(TestBus(0x54, 0xa5)).apply { state.p = 0x20 }
        assertEquals(3, mvmp.step().cycles)
        assertEquals(0xa5, mvmp.state.internalRam[0x20])
        assertEquals(1, mvmp.state.programCounter)

        val ldpc = Sc61860Cpu(TestBus(0x56, 0x5a))
        assertEquals(3, ldpc.step().cycles)
        assertEquals(0x5a, ldpc.state.internalRam[0x02])
        assertEquals(1, ldpc.state.programCounter)
    }

    @Test
    fun swpAndLdmTransformOrLoadTheAccumulatorWithoutChangingFlags() {
        val swap = Sc61860Cpu(TestBus(0x58)).apply {
            state.internalRam[0x02] = 0x3c
            state.zero = true
            state.carry = true
        }
        assertEquals(2, swap.step().cycles)
        assertEquals(0xc3, swap.state.internalRam[0x02])
        assertEquals(true, swap.state.zero)
        assertEquals(true, swap.state.carry)

        val load = Sc61860Cpu(TestBus(0x59)).apply {
            state.p = 0x20
            state.internalRam[0x20] = 0xa5
            state.zero = true
            state.carry = true
        }
        assertEquals(2, load.step().cycles)
        assertEquals(0xa5, load.state.internalRam[0x02])
        assertEquals(true, load.state.zero)
        assertEquals(true, load.state.carry)
    }

    @Test
    fun ixAndDyUpdateIndexRegistersDpAndQWithSixteenBitWrapping() {
        val ix = Sc61860Cpu(TestBus(0x04)).apply {
            state.internalRam[0x04] = 0xff
            state.internalRam[0x05] = 0xff
        }
        assertEquals(6, ix.step().cycles)
        assertEquals(0, ix.state.dataPointer)
        assertEquals(0, ix.state.internalRam[0x04])
        assertEquals(0, ix.state.internalRam[0x05])
        assertEquals(0x05, ix.state.q)

        val dy = Sc61860Cpu(TestBus(0x07))
        assertEquals(6, dy.step().cycles)
        assertEquals(0xffff, dy.state.dataPointer)
        assertEquals(0xff, dy.state.internalRam[0x06])
        assertEquals(0xff, dy.state.internalRam[0x07])
        assertEquals(0x07, dy.state.q)
    }

    @Test
    fun dxAndIyUpdateTheirCorrespondingIndexPair() {
        val dx = Sc61860Cpu(TestBus(0x05)).apply {
            state.internalRam[0x04] = 0x01
            state.internalRam[0x05] = 0x10
        }
        dx.step()
        assertEquals(0x1000, dx.state.dataPointer)
        assertEquals(0x00, dx.state.internalRam[0x04])
        assertEquals(0x10, dx.state.internalRam[0x05])

        val iy = Sc61860Cpu(TestBus(0x06)).apply {
            state.internalRam[0x06] = 0xff
            state.internalRam[0x07] = 0x0f
        }
        iy.step()
        assertEquals(0x1000, iy.state.dataPointer)
        assertEquals(0x00, iy.state.internalRam[0x06])
        assertEquals(0x10, iy.state.internalRam[0x07])
    }

    @Test
    fun ixlAndDxlMoveThenLoadFromTheNewXAddress() {
        listOf(0x24 to 0x8fff, 0x25 to 0x9001).forEach { (opcode, initial) ->
            val bus = TestBus(opcode).apply { this[0x9000] = 0xa5 }
            val cpu = Sc61860Cpu(bus).apply {
                state.internalRam[0x04] = initial and 0xff
                state.internalRam[0x05] = initial ushr 8
            }

            assertEquals(7, cpu.step().cycles)
            assertEquals(0x9000, cpu.state.dataPointer)
            assertEquals(0xa5, cpu.state.internalRam[0x02])
            assertEquals(0x00, cpu.state.internalRam[0x04])
            assertEquals(0x90, cpu.state.internalRam[0x05])
        }
    }

    @Test
    fun iysAndDysMoveThenStoreAtTheNewYAddress() {
        listOf(0x26 to 0x8fff, 0x27 to 0x9001).forEach { (opcode, initial) ->
            val bus = TestBus(opcode)
            val cpu = Sc61860Cpu(bus).apply {
                state.internalRam[0x02] = 0x5a
                state.internalRam[0x06] = initial and 0xff
                state.internalRam[0x07] = initial ushr 8
            }

            assertEquals(6, cpu.step().cycles)
            assertEquals(0x9000, cpu.state.dataPointer)
            assertEquals(0x5a, bus[0x9000])
            assertEquals(0x00, cpu.state.internalRam[0x06])
            assertEquals(0x90, cpu.state.internalRam[0x07])
        }
    }

    @Test
    fun slShiftsCarryIntoBitZeroAndBitSevenIntoCarry() {
        val carryOut = Sc61860Cpu(TestBus(0x5a)).apply {
            state.internalRam[0x02] = 0x80
            state.zero = true
        }
        assertEquals(2, carryOut.step().cycles)
        assertEquals(0, carryOut.state.internalRam[0x02])
        assertEquals(0x0100, carryOut.state.alu)
        assertEquals(true, carryOut.state.carry)
        assertEquals(true, carryOut.state.zero)

        val carryIn = Sc61860Cpu(TestBus(0x5a)).apply {
            state.internalRam[0x02] = 0
            state.carry = true
        }
        carryIn.step()
        assertEquals(1, carryIn.state.internalRam[0x02])
        assertEquals(1, carryIn.state.alu)
        assertEquals(false, carryIn.state.carry)
    }

    @Test
    fun srShiftsCarryIntoBitSevenAndBitZeroIntoCarry() {
        val carryOut = Sc61860Cpu(TestBus(0xd2)).apply {
            state.internalRam[0x02] = 1
            state.zero = true
        }
        assertEquals(2, carryOut.step().cycles)
        assertEquals(0, carryOut.state.internalRam[0x02])
        assertEquals(1, carryOut.state.alu)
        assertEquals(true, carryOut.state.carry)
        assertEquals(true, carryOut.state.zero)

        val carryIn = Sc61860Cpu(TestBus(0xd2)).apply {
            state.internalRam[0x02] = 0
            state.carry = true
        }
        carryIn.step()
        assertEquals(0x80, carryIn.state.internalRam[0x02])
        assertEquals(0x0100, carryIn.state.alu)
        assertEquals(false, carryIn.state.carry)
    }

    @Test
    fun adbAddsBaToTheLittleEndianWordAtP() {
        val cpu = Sc61860Cpu(TestBus(0x14)).apply {
            state.p = 0x20
            state.internalRam[0x20] = 0x34
            state.internalRam[0x21] = 0x12
            state.internalRam[0x02] = 0x02
            state.internalRam[0x03] = 0x01
            state.alu = 0x55aa
        }

        val result = cpu.step()

        assertEquals(5, result.cycles)
        assertEquals(0x36, cpu.state.internalRam[0x20])
        assertEquals(0x13, cpu.state.internalRam[0x21])
        assertEquals(0x21, cpu.state.p)
        assertEquals(false, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
        assertEquals(0x55aa, cpu.state.alu)
    }

    @Test
    fun adbReportsSixteenBitOverflowAndWrapsPAtSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x14)).apply {
            state.p = 0x7f
            state.internalRam[0x7f] = 0xff
            state.internalRam[0x00] = 0xff
            state.internalRam[0x02] = 0x01
            state.internalRam[0x03] = 0x00
        }

        cpu.step()

        assertEquals(0, cpu.state.internalRam[0x7f])
        assertEquals(0, cpu.state.internalRam[0x00])
        assertEquals(0, cpu.state.p)
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun sbbSubtractsBaAndUsesCarryAsSixteenBitBorrow() {
        val cpu = Sc61860Cpu(TestBus(0x15)).apply {
            state.p = 0x20
            state.internalRam[0x20] = 0
            state.internalRam[0x21] = 0
            state.internalRam[0x02] = 1
            state.internalRam[0x03] = 0
        }

        val result = cpu.step()

        assertEquals(5, result.cycles)
        assertEquals(0xff, cpu.state.internalRam[0x20])
        assertEquals(0xff, cpu.state.internalRam[0x21])
        assertEquals(0x21, cpu.state.p)
        assertEquals(false, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
        assertEquals(1, cpu.state.internalRam[0x02])
        assertEquals(0, cpu.state.internalRam[0x03])
    }

    @Test
    fun mvwAndMvbCopyIOrJPlusOneBytesFromQToP() {
        val cases = listOf(
            Triple(0x08, 0x00, 2),
            Triple(0x0a, 0x01, 1),
        )
        cases.forEach { (opcode, countRegister, encodedCount) ->
            val cpu = Sc61860Cpu(TestBus(opcode)).apply {
                state.internalRam[countRegister] = encodedCount
                state.p = 0x20
                state.q = 0x30
                repeat(encodedCount + 1) { state.internalRam[0x30 + it] = 0xa0 + it }
            }

            val result = cpu.step()

            assertEquals(encodedCount * 2 + 3, result.cycles)
            repeat(encodedCount + 1) { assertEquals(0xa0 + it, cpu.state.internalRam[0x20 + it]) }
            assertEquals(0x20 + encodedCount + 1, cpu.state.p)
            assertEquals(0x30 + encodedCount + 1, cpu.state.q)
            assertEquals(0, cpu.state.d)
        }
    }

    @Test
    fun exwAndExbExchangeIOrJPlusOneBytes() {
        val cases = listOf(
            Triple(0x09, 0x00, 2),
            Triple(0x0b, 0x01, 1),
        )
        cases.forEach { (opcode, countRegister, encodedCount) ->
            val cpu = Sc61860Cpu(TestBus(opcode)).apply {
                state.internalRam[countRegister] = encodedCount
                state.p = 0x20
                state.q = 0x30
                repeat(encodedCount + 1) {
                    state.internalRam[0x20 + it] = 0x10 + it
                    state.internalRam[0x30 + it] = 0xa0 + it
                }
            }

            val result = cpu.step()

            assertEquals(encodedCount * 3 + 3, result.cycles)
            repeat(encodedCount + 1) {
                assertEquals(0xa0 + it, cpu.state.internalRam[0x20 + it])
                assertEquals(0x10 + it, cpu.state.internalRam[0x30 + it])
            }
        }
    }

    @Test
    fun internalBlockPointersWrapAtSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x08)).apply {
            state.internalRam[0x00] = 1
            state.p = 0x7f
            state.q = 0x70
            state.internalRam[0x70] = 0xa5
            state.internalRam[0x71] = 0x5a
        }

        cpu.step()

        assertEquals(0xa5, cpu.state.internalRam[0x7f])
        assertEquals(0x5a, cpu.state.internalRam[0x00])
        assertEquals(0x01, cpu.state.p)
        assertEquals(0x72, cpu.state.q)
    }

    @Test
    fun mvwdAndMvbdCopyIOrJPlusOneBytesFromDpToP() {
        val cases = listOf(
            Triple(0x18, 0x00, 2),
            Triple(0x1a, 0x01, 1),
        )
        cases.forEach { (opcode, countRegister, encodedCount) ->
            val bus = TestBus(opcode).apply {
                repeat(encodedCount + 1) { this[0x9000 + it] = 0xa0 + it }
            }
            val cpu = Sc61860Cpu(bus).apply {
                state.internalRam[countRegister] = encodedCount
                state.p = 0x20
                state.dataPointer = 0x9000
            }

            val result = cpu.step()

            assertEquals(encodedCount * 4 + 3, result.cycles)
            repeat(encodedCount + 1) { assertEquals(0xa0 + it, cpu.state.internalRam[0x20 + it]) }
            assertEquals(0x20 + encodedCount + 1, cpu.state.p)
            assertEquals(0x9000 + encodedCount, cpu.state.dataPointer)
            assertEquals(0, cpu.state.d)
        }
    }

    @Test
    fun exwdAndExbdExchangeIOrJPlusOneBytesBetweenDpAndP() {
        val cases = listOf(
            Triple(0x19, 0x00, 2),
            Triple(0x1b, 0x01, 1),
        )
        cases.forEach { (opcode, countRegister, encodedCount) ->
            val bus = TestBus(opcode).apply {
                repeat(encodedCount + 1) { this[0x9000 + it] = 0xa0 + it }
            }
            val cpu = Sc61860Cpu(bus).apply {
                state.internalRam[countRegister] = encodedCount
                state.p = 0x20
                state.dataPointer = 0x9000
                repeat(encodedCount + 1) { state.internalRam[0x20 + it] = 0x10 + it }
            }

            val result = cpu.step()

            assertEquals(encodedCount * 6 + 3, result.cycles)
            repeat(encodedCount + 1) {
                assertEquals(0xa0 + it, cpu.state.internalRam[0x20 + it])
                assertEquals(0x10 + it, bus[0x9000 + it])
            }
            assertEquals(0x9000 + encodedCount, cpu.state.dataPointer)
        }
    }

    @Test
    fun dataBlockPointersWrapAtTheirRespectiveWidths() {
        val bus = TestBus().apply {
            this[0x1000] = 0x18
            this[0xffff] = 0xa5
            this[0x0000] = 0x5a
        }
        val cpu = Sc61860Cpu(bus).apply {
            state.programCounter = 0x1000
            state.internalRam[0x00] = 1
            state.p = 0x7f
            state.dataPointer = 0xffff
        }

        cpu.step()

        assertEquals(0xa5, cpu.state.internalRam[0x7f])
        assertEquals(0x5a, cpu.state.internalRam[0x00])
        assertEquals(0x01, cpu.state.p)
        assertEquals(0x0000, cpu.state.dataPointer)
    }

    @Test
    fun filmFillsIPlusOneInternalBytesAndAdvancesP() {
        val cpu = Sc61860Cpu(TestBus(0x1e)).apply {
            state.internalRam[0x00] = 2
            state.internalRam[0x02] = 0xa5
            state.p = 0x20
        }

        val result = cpu.step()

        assertEquals(6, result.cycles)
        assertEquals(0xa5, cpu.state.internalRam[0x20])
        assertEquals(0xa5, cpu.state.internalRam[0x21])
        assertEquals(0xa5, cpu.state.internalRam[0x22])
        assertEquals(0x23, cpu.state.p)
        assertEquals(0, cpu.state.d)
    }

    @Test
    fun filmWrapsPAtSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x1e)).apply {
            state.internalRam[0x00] = 2
            state.internalRam[0x02] = 0x5a
            state.p = 0x7e
        }

        cpu.step()

        assertEquals(0x5a, cpu.state.internalRam[0x7e])
        assertEquals(0x5a, cpu.state.internalRam[0x7f])
        assertEquals(0x5a, cpu.state.internalRam[0x00])
        assertEquals(0x01, cpu.state.p)
    }

    @Test
    fun fildFillsIPlusOneDataBytesAndEndsAtTheLastAddress() {
        val bus = TestBus().apply { this[0x1000] = 0x1f }
        val cpu = Sc61860Cpu(bus).apply {
            state.programCounter = 0x1000
            state.internalRam[0x00] = 2
            state.internalRam[0x02] = 0xa5
            state.dataPointer = 0xfffe
        }

        val result = cpu.step()

        assertEquals(10, result.cycles)
        assertEquals(0xa5, bus[0xfffe])
        assertEquals(0xa5, bus[0xffff])
        assertEquals(0xa5, bus[0x0000])
        assertEquals(0x0000, cpu.state.dataPointer)
        assertEquals(0, cpu.state.d)
    }

    @Test
    fun adnPropagatesPackedDecimalCarryTowardLowerPAddresses() {
        val cpu = Sc61860Cpu(TestBus(0x0c)).apply {
            state.internalRam[0x00] = 1
            state.internalRam[0x02] = 0x01
            state.p = 0x21
            state.internalRam[0x21] = 0x99
            state.internalRam[0x20] = 0x00
        }

        val result = cpu.step()

        assertEquals(7, result.cycles)
        assertEquals(0x00, cpu.state.internalRam[0x21])
        assertEquals(0x01, cpu.state.internalRam[0x20])
        assertEquals(0x1f, cpu.state.p)
        assertEquals(false, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
        assertEquals(0, cpu.state.alu)
        assertEquals(0, cpu.state.d)
    }

    @Test
    fun sbnPropagatesPackedDecimalBorrowTowardLowerPAddresses() {
        val cpu = Sc61860Cpu(TestBus(0x0d)).apply {
            state.internalRam[0x00] = 1
            state.internalRam[0x02] = 0x01
            state.p = 0x21
            state.internalRam[0x21] = 0x00
            state.internalRam[0x20] = 0x01
        }

        cpu.step()

        assertEquals(0x99, cpu.state.internalRam[0x21])
        assertEquals(0x00, cpu.state.internalRam[0x20])
        assertEquals(false, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
    }

    @Test
    fun adwAndSbwOperateOnPackedDecimalBlocksAtPAndQ() {
        val add = Sc61860Cpu(TestBus(0x0e)).apply {
            state.internalRam[0x00] = 1
            state.p = 0x21
            state.q = 0x31
            state.internalRam[0x21] = 0x99
            state.internalRam[0x20] = 0x00
            state.internalRam[0x31] = 0x01
            state.internalRam[0x30] = 0x00
        }
        assertEquals(7, add.step().cycles)
        assertEquals(0x00, add.state.internalRam[0x21])
        assertEquals(0x01, add.state.internalRam[0x20])
        assertEquals(0x1f, add.state.p)
        assertEquals(0x2f, add.state.q)
        assertEquals(false, add.state.carry)

        val subtract = Sc61860Cpu(TestBus(0x0f)).apply {
            state.internalRam[0x00] = 1
            state.p = 0x21
            state.q = 0x31
            state.internalRam[0x21] = 0x00
            state.internalRam[0x20] = 0x01
            state.internalRam[0x31] = 0x01
            state.internalRam[0x30] = 0x00
        }
        subtract.step()
        assertEquals(0x99, subtract.state.internalRam[0x21])
        assertEquals(0x00, subtract.state.internalRam[0x20])
        assertEquals(false, subtract.state.carry)
    }

    @Test
    fun decimalOperationsMaskInvalidPackedDecimalNibblesLikePokecomGo() {
        val cpu = Sc61860Cpu(TestBus(0x0c)).apply {
            state.internalRam[0x00] = 0
            state.internalRam[0x02] = 0
            state.p = 0x20
            state.internalRam[0x20] = 0xfa
        }

        cpu.step()

        assertEquals(0, cpu.state.internalRam[0x20])
        assertEquals(true, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
    }

    @Test
    fun srwShiftsAnInternalBlockRightByOneNibble() {
        val cpu = Sc61860Cpu(TestBus(0x1c)).apply {
            state.internalRam[0x00] = 1
            state.p = 0x20
            state.internalRam[0x20] = 0x12
            state.internalRam[0x21] = 0x34
            state.zero = true
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(5, result.cycles)
        assertEquals(0x01, cpu.state.internalRam[0x20])
        assertEquals(0x23, cpu.state.internalRam[0x21])
        assertEquals(0x22, cpu.state.p)
        assertEquals(0x1234, cpu.state.alu)
        assertEquals(0, cpu.state.d)
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun slwShiftsAnInternalBlockLeftByOneNibble() {
        val cpu = Sc61860Cpu(TestBus(0x1d)).apply {
            state.internalRam[0x00] = 1
            state.p = 0x21
            state.internalRam[0x20] = 0x12
            state.internalRam[0x21] = 0x34
            state.zero = true
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(5, result.cycles)
        assertEquals(0x23, cpu.state.internalRam[0x20])
        assertEquals(0x40, cpu.state.internalRam[0x21])
        assertEquals(0x1f, cpu.state.p)
        assertEquals(0x1234, cpu.state.alu)
        assertEquals(0, cpu.state.d)
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun wordShiftPointersWrapAtSevenBits() {
        val right = Sc61860Cpu(TestBus(0x1c)).apply {
            state.internalRam[0x00] = 0
            state.p = 0x7f
            state.internalRam[0x7f] = 0x12
        }
        right.step()
        assertEquals(0, right.state.p)
        assertEquals(0x01, right.state.internalRam[0x7f])

        val left = Sc61860Cpu(TestBus(0x1d)).apply {
            state.internalRam[0x00] = 0
            state.p = 0
        }
        left.step()
        assertEquals(0x7f, left.state.p)
    }

    @Test
    fun loopBranchesBackwardAndDecrementsANonzeroCounter() {
        val cpu = cpuAt(0x1000, 0x2f, 0x20).apply {
            state.internalRam[state.r] = 2
        }

        val result = cpu.step()

        assertEquals(10, result.cycles)
        assertEquals(0x0fe1, cpu.state.programCounter)
        assertEquals(1, cpu.state.internalRam[0x60])
        assertEquals(0x0001, cpu.state.alu)
        assertEquals(false, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun loopCanProduceZeroWithoutFinishingTheLoopFrame() {
        val cpu = cpuAt(0x1000, 0x2f, 0x20).apply {
            state.internalRam[state.r] = 1
        }

        val result = cpu.step()

        assertEquals(10, result.cycles)
        assertEquals(0x0fe1, cpu.state.programCounter)
        assertEquals(0, cpu.state.internalRam[0x60])
        assertEquals(true, cpu.state.zero)
        assertEquals(false, cpu.state.carry)
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun loopAdvancesPastTheOperandAndRWhenCounterStartsAtZero() {
        val cpu = cpuAt(0x1000, 0x2f, 0x20)

        val result = cpu.step()

        assertEquals(7, result.cycles)
        assertEquals(0x1002, cpu.state.programCounter)
        assertEquals(0xff, cpu.state.internalRam[0x60])
        assertEquals(0xffff, cpu.state.alu)
        assertEquals(false, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
        assertEquals(0x61, cpu.state.r)
    }

    @Test
    fun loopWrapsRAtSevenBitsWhenItFinishes() {
        val cpu = Sc61860Cpu(TestBus(0x2f, 0x01)).apply {
            state.r = 0x7f
            state.internalRam[0x7f] = 0
        }

        cpu.step()

        assertEquals(0, cpu.state.r)
    }

    @Test
    fun case1LoadsTheCountAndPushesItsTwoByteOperand() {
        val cpu = Sc61860Cpu(TestBus(0x7a, 0x02, 0x12, 0x34))

        val result = cpu.step()

        assertEquals(9, result.cycles)
        assertEquals(2, cpu.state.d)
        assertEquals(0x5e, cpu.state.r)
        assertEquals(0x34, cpu.state.internalRam[0x5e])
        assertEquals(0x12, cpu.state.internalRam[0x5f])
        assertEquals(4, cpu.state.programCounter)
    }

    @Test
    fun case2BranchesToTheMatchingTableEntry() {
        val cpu = Sc61860Cpu(
            TestBus(
                0x7a, 0x02, 0x12, 0x34,
                0x69,
                0x10, 0x20, 0x00,
                0x20, 0x30, 0x00,
                0x30, 0x40,
            ),
        ).apply {
            state.internalRam[0x02] = 0x20
        }
        cpu.step()

        val result = cpu.step()

        assertEquals(16, result.cycles)
        assertEquals(0x3000, cpu.state.programCounter)
        assertEquals(1, cpu.state.d)
    }

    @Test
    fun case2UsesTheDefaultAddressAfterAllEntriesMiss() {
        val cpu = Sc61860Cpu(
            TestBus(
                0x7a, 0x02, 0x12, 0x34,
                0x69,
                0x10, 0x20, 0x00,
                0x20, 0x30, 0x00,
                0x30, 0x40,
            ),
        ).apply {
            state.internalRam[0x02] = 0x99
        }
        cpu.step()

        val result = cpu.step()

        assertEquals(21, result.cycles)
        assertEquals(0x3040, cpu.state.programCounter)
        assertEquals(0, cpu.state.d)
        assertEquals(0x5e, cpu.state.r)
    }

    @Test
    fun exabSwapsAccumulatorAndBWithoutChangingFlags() {
        val cpu = Sc61860Cpu(TestBus(0xda)).apply {
            state.internalRam[0x02] = 0x12
            state.internalRam[0x03] = 0x34
            state.zero = true
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(3, result.cycles)
        assertEquals(0x34, cpu.state.internalRam[0x02])
        assertEquals(0x12, cpu.state.internalRam[0x03])
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun leaveClearsTheByteAtRWithoutMovingR() {
        val cpu = Sc61860Cpu(TestBus(0xd8)).apply {
            state.r = 0x40
            state.internalRam[0x40] = 0xa5
        }

        val result = cpu.step()

        assertEquals(2, result.cycles)
        assertEquals(0, cpu.state.internalRam[0x40])
        assertEquals(0x40, cpu.state.r)
    }

    @Test
    fun rzAliasesSkipOneByteAndClearOnlyZero() {
        listOf(0x72, 0x73, 0x76, 0x77).forEach { opcode ->
            val cpu = Sc61860Cpu(TestBus(opcode, 0xa5)).apply {
                state.zero = true
                state.carry = true
            }

            val result = cpu.step()

            assertEquals(4, result.cycles)
            assertEquals(2, cpu.state.programCounter)
            assertEquals(false, cpu.state.zero)
            assertEquals(true, cpu.state.carry)
        }
    }

    @Test
    fun nopwAliasesConsumeTwoCyclesWithoutChangingCpuData() {
        val cpu = Sc61860Cpu(TestBus(0x4d, 0xce, 0xd3, 0xd9)).apply {
            state.zero = true
            state.carry = true
            state.internalRam[0x02] = 0xa5
        }

        repeat(4) {
            assertEquals(2, cpu.step().cycles)
        }

        assertEquals(4, cpu.state.programCounter)
        assertEquals(0xa5, cpu.state.internalRam[0x02])
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
    }

    @Test
    fun anidAndOridSaveTheOldDataByteAndWriteTheLogicalResult() {
        val andBus = TestBus(0xd4, 0x0f).apply { this[0x9000] = 0xf0 }
        val and = Sc61860Cpu(andBus).apply {
            state.dataPointer = 0x9000
            state.carry = true
        }
        assertEquals(6, and.step().cycles)
        assertEquals(0, andBus[0x9000])
        assertEquals(0xf0, and.state.internalRam[0x5f])
        assertEquals(true, and.state.zero)
        assertEquals(true, and.state.carry)
        assertEquals(0x60, and.state.r)

        val orBus = TestBus(0xd5, 0x03).apply { this[0x9000] = 0x30 }
        val or = Sc61860Cpu(orBus).apply {
            state.dataPointer = 0x9000
            state.carry = true
        }
        assertEquals(6, or.step().cycles)
        assertEquals(0x33, orBus[0x9000])
        assertEquals(0x30, or.state.internalRam[0x5f])
        assertEquals(false, or.state.zero)
        assertEquals(true, or.state.carry)
    }

    @Test
    fun tsidSavesTheDataByteButOnlyUpdatesZero() {
        val bus = TestBus(0xd6, 0x0f).apply { this[0x9000] = 0xf0 }
        val cpu = Sc61860Cpu(bus).apply {
            state.dataPointer = 0x9000
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(6, result.cycles)
        assertEquals(0xf0, bus[0x9000])
        assertEquals(0xf0, cpu.state.internalRam[0x5f])
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
        assertEquals(2, cpu.state.programCounter)
    }

    @Test
    fun szSavesTheDataByteSkipsOneByteAndSetsZero() {
        val bus = TestBus(0xd7, 0xa5).apply { this[0x9000] = 0x5a }
        val cpu = Sc61860Cpu(bus).apply {
            state.dataPointer = 0x9000
            state.carry = true
        }

        val result = cpu.step()

        assertEquals(6, result.cycles)
        assertEquals(0x5a, cpu.state.internalRam[0x5f])
        assertEquals(2, cpu.state.programCounter)
        assertEquals(true, cpu.state.zero)
        assertEquals(true, cpu.state.carry)
        assertEquals(0x9000, cpu.state.dataPointer)
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun outfLatchesTheInternalPortAndNotifiesMachineIo() {
        val io = RecordingIo(inputA = 0, inputB = 0)
        val cpu = Sc61860Cpu(TestBus(0x5f), io).apply {
            state.internalRam[0x5e] = 0xa5
        }

        val result = cpu.step()

        assertEquals(3, result.cycles)
        assertEquals(0x5e, cpu.state.q)
        assertEquals(0xa5, cpu.state.fo)
        assertEquals(0xa5, io.lastOutputF)
    }

    @Test
    fun outcLatchesControlResetsDividersAndNotifiesMachineIo() {
        val io = RecordingIo(inputA = 0, inputB = 0)
        val cpu = Sc61860Cpu(TestBus(0xdf), io).apply {
            state.internalRam[0x5f] = 0x0a
            state.ticks = 12
            state.ticks2 = 34
            state.divider500 = true
            state.divider2 = true
        }

        val result = cpu.step()

        assertEquals(2, result.cycles)
        assertEquals(0x5f, cpu.state.q)
        assertEquals(0x0a, cpu.state.control)
        assertEquals(2, cpu.state.ticks)
        assertEquals(2, cpu.state.ticks2)
        assertEquals(false, cpu.state.divider500)
        assertEquals(false, cpu.state.divider2)
        assertEquals(false, cpu.state.powerOn)
        assertEquals(0x0a, io.lastOutputControl)
    }

    @Test
    fun outcWithoutResetOrPowerBitsPreservesTimersAndSetsPowerOn() {
        val cpu = Sc61860Cpu(TestBus(0xdf)).apply {
            state.internalRam[0x5f] = 0x01
            state.ticks = 12
            state.ticks2 = 34
            state.divider500 = true
            state.divider2 = true
            state.powerOn = false
        }

        cpu.step()

        assertEquals(14, cpu.state.ticks)
        assertEquals(36, cpu.state.ticks2)
        assertEquals(true, cpu.state.divider500)
        assertEquals(true, cpu.state.divider2)
        assertEquals(true, cpu.state.powerOn)
    }

    @Test
    fun instructionCyclesDriveTwoMillisecondAndFiveHundredMillisecondDividers() {
        val twoMilliseconds = Sc61860Cpu(TestBus(0x33)).apply {
            state.ticks = 573
        }
        val fiveHundredMilliseconds = Sc61860Cpu(TestBus(0x33)).apply {
            state.ticks2 = 143_997
        }

        twoMilliseconds.step()
        fiveHundredMilliseconds.step()

        assertEquals(0, twoMilliseconds.state.ticks)
        assertEquals(true, twoMilliseconds.state.divider2)
        assertEquals(0, fiveHundredMilliseconds.state.ticks2)
        assertEquals(true, fiveHundredMilliseconds.state.divider500)
    }

    @Test
    fun dividerLatchesRemainSetAcrossAdditionalPeriodsUntilTestConsumesThem() {
        val cpu = Sc61860Cpu(TestBus(0x33, 0x33, 0x6b, 0x03)).apply {
            state.ticks = 575
            state.ticks2 = 143_999
        }

        cpu.step()
        assertEquals(true, cpu.state.divider2)
        assertEquals(true, cpu.state.divider500)

        cpu.state.ticks = 575
        cpu.state.ticks2 = 143_999
        cpu.step()
        assertEquals(true, cpu.state.divider2)
        assertEquals(true, cpu.state.divider500)

        cpu.step()
        assertEquals(0x03, cpu.state.testPort)
        assertEquals(false, cpu.state.divider2)
        assertEquals(false, cpu.state.divider500)
    }

    @Test
    fun testConsumesDividerAndKeyOnSignalsAndAppliesTheImmediateMask() {
        val io = RecordingIo(inputA = 0, inputB = 0).apply { keyOnSignal = true }
        val cpu = Sc61860Cpu(TestBus(0x6b, 0x0b), io).apply {
            state.divider500 = true
            state.divider2 = true
        }

        val result = cpu.step()

        assertEquals(4, result.cycles)
        assertEquals(0x0b, cpu.state.testPort)
        assertEquals(false, cpu.state.divider500)
        assertEquals(false, cpu.state.divider2)
        assertEquals(false, io.keyOnSignal)
        assertEquals(false, cpu.state.zero)
        assertEquals(2, cpu.state.programCounter)
    }

    @Test
    fun testSetsZeroWhenNoSelectedSignalIsActive() {
        val cpu = Sc61860Cpu(TestBus(0x6b, 0x08))

        cpu.step()

        assertEquals(0, cpu.state.testPort)
        assertEquals(true, cpu.state.zero)
    }

    @Test
    fun mvwpCopiesIPlusOneBytesFromTheAccumulatorPairToP() {
        val bus = TestBus(0x35).apply {
            this[0x9000] = 0x12
            this[0x9001] = 0x34
            this[0x9002] = 0x56
        }
        val cpu = Sc61860Cpu(bus).apply {
            state.internalRam[0x00] = 2
            state.internalRam[0x02] = 0x00
            state.internalRam[0x03] = 0x90
            state.p = 0x20
            state.d = 0x7f
        }

        val result = cpu.step()

        assertEquals(15, result.cycles)
        assertEquals(0x12, cpu.state.internalRam[0x20])
        assertEquals(0x34, cpu.state.internalRam[0x21])
        assertEquals(0x56, cpu.state.internalRam[0x22])
        assertEquals(0x23, cpu.state.p)
        assertEquals(0, cpu.state.d)
        assertEquals(1, cpu.state.programCounter)
        assertEquals(0x60, cpu.state.r)
        assertEquals(0x00, cpu.state.internalRam[0x02])
        assertEquals(0x90, cpu.state.internalRam[0x03])
    }

    @Test
    fun fildUsesThreeCyclesForEachAdditionalExternalByte() {
        val bus = TestBus(0x1f)
        val cpu = Sc61860Cpu(bus).apply {
            state.internalRam[0x00] = 10
            state.internalRam[0x02] = 0x5a
            state.dataPointer = 0x4000
        }

        val result = cpu.step()

        assertEquals(34, result.cycles)
        assertEquals(0x5a, bus[0x4000])
        assertEquals(0x5a, bus[0x400a])
        assertEquals(0x400a, cpu.state.dataPointer)
    }

    @Test
    fun mvwpWrapsItsSourceAndSevenBitDestinationPointers() {
        val bus = TestBus().apply {
            this[0x1000] = 0x35
            this[0xffff] = 0xaa
            this[0x0000] = 0xbb
        }
        val cpu = Sc61860Cpu(bus).apply {
            state.programCounter = 0x1000
            state.internalRam[0x00] = 1
            state.internalRam[0x02] = 0xff
            state.internalRam[0x03] = 0xff
            state.p = 0x7f
        }

        val result = cpu.step()

        assertEquals(11, result.cycles)
        assertEquals(0xaa, cpu.state.internalRam[0x7f])
        assertEquals(0xbb, cpu.state.internalRam[0x00])
        assertEquals(0x01, cpu.state.p)
        assertEquals(0x1001, cpu.state.programCounter)
        assertEquals(0x60, cpu.state.r)
    }

    @Test
    fun cupCountsWhileXInputIsLowAndCdnCountsWhileItIsHigh() {
        val cup = Sc61860Cpu(TestBus(0x4f)).apply {
            state.internalRam[0x00] = 2
            state.p = 0x20
            state.xInput = 0
        }
        val cdn = Sc61860Cpu(TestBus(0x6f)).apply {
            state.internalRam[0x00] = 2
            state.p = 0x20
            state.xInput = 1
        }

        assertEquals(9, cup.step().cycles)
        assertEquals(9, cdn.step().cycles)
        assertEquals(0x24, cup.state.p)
        assertEquals(0x24, cdn.state.p)
        assertEquals(0xff, cup.state.d)
        assertEquals(0xff, cdn.state.d)
        assertEquals(true, cup.state.zero)
        assertEquals(true, cdn.state.zero)
    }

    @Test
    fun cupAndCdnDoNotCountWhenXInputHasTheOppositeLevel() {
        val cup = Sc61860Cpu(TestBus(0x4f)).apply {
            state.internalRam[0x00] = 3
            state.p = 0x7e
            state.xInput = 1
            state.zero = true
        }
        val cdn = Sc61860Cpu(TestBus(0x6f)).apply {
            state.internalRam[0x00] = 3
            state.p = 0x7e
            state.xInput = 0
            state.zero = true
        }

        assertEquals(13, cup.step().cycles)
        assertEquals(13, cdn.step().cycles)
        assertEquals(0x7e, cup.state.p)
        assertEquals(0x7e, cdn.state.p)
        assertEquals(4, cup.state.d)
        assertEquals(4, cdn.state.d)
        assertEquals(false, cup.state.zero)
        assertEquals(false, cdn.state.zero)
    }

    @Test
    fun cupWrapsPAtSevenBits() {
        val cpu = Sc61860Cpu(TestBus(0x4f)).apply {
            state.internalRam[0x00] = 0
            state.p = 0x7f
            state.xInput = 0
        }

        assertEquals(1, cpu.step().cycles)
        assertEquals(0x01, cpu.state.p)
        assertEquals(0xff, cpu.state.d)
        assertEquals(true, cpu.state.zero)
    }

    @Test
    fun waitConsumesItsOperandAndAddsSixCycles() {
        val minimum = Sc61860Cpu(TestBus(0x4e, 0x00)).apply {
            state.p = 0x12
            state.carry = true
            state.zero = true
        }
        val maximum = cpuAt(0xffff, 0x4e, 0xff)

        assertEquals(6, minimum.step().cycles)
        assertEquals(2, minimum.state.programCounter)
        assertEquals(0x12, minimum.state.p)
        assertEquals(true, minimum.state.carry)
        assertEquals(true, minimum.state.zero)

        assertEquals(261, maximum.step().cycles)
        assertEquals(1, maximum.state.programCounter)
    }

    private fun cpuWithAccumulatorOperation(opcode: Int, pValue: Int, aValue: Int): Sc61860Cpu =
        Sc61860Cpu(TestBus(opcode)).apply {
            state.p = 0x20
            state.internalRam[0x20] = pValue
            state.internalRam[0x02] = aValue
        }

    private data class ImmediateWriteCase(
        val opcode: Int,
        val register: Int,
        val initial: Int,
        val immediate: Int,
        val expected: Int,
    )

    private fun assertConditionalRelativeJump(
        opcode: Int,
        zero: Boolean,
        carry: Boolean,
        taken: Boolean,
    ) {
        val forward = opcode in setOf(0x28, 0x2a, 0x38, 0x3a)
        val cpu = cpuAt(0x1000, opcode, 0x20).apply {
            state.zero = zero
            state.carry = carry
        }

        val result = cpu.step()

        assertEquals(if (taken) 7 else 4, result.cycles)
        val takenAddress = if (forward) 0x1021 else 0x0fe1
        assertEquals(if (taken) takenAddress else 0x1002, cpu.state.programCounter)
        assertEquals(0x20, cpu.state.internalRam[0x5f])
    }

    private fun cpuAt(address: Int, opcode: Int, operand: Int): Sc61860Cpu {
        val bus = TestBus().apply {
            this[address] = opcode
            this[(address + 1) and 0xffff] = operand
        }
        return Sc61860Cpu(bus).apply {
            state.programCounter = address
        }
    }

    private fun assertConditionalJump(
        opcode: Int,
        zero: Boolean,
        carry: Boolean,
        taken: Boolean,
    ) {
        val cpu = Sc61860Cpu(TestBus(opcode, 0x12, 0x34)).apply {
            state.zero = zero
            state.carry = carry
        }

        val result = cpu.step()

        assertEquals(6, result.cycles)
        assertEquals(if (taken) 0x1234 else 3, cpu.state.programCounter)
    }

    private class RecordingIo(
        private val inputA: Int,
        private val inputB: Int,
    ) : Sc61860Io {
        var lastInputASelection: Pair<Int, Int>? = null
        var lastInputBSelection: Int? = null
        var lastOutputF: Int? = null
        var lastOutputControl: Int? = null
        var keyOnSignal: Boolean = false

        override fun readInputA(ia: Int, ib: Int): Int {
            lastInputASelection = ia to ib
            return inputA
        }

        override fun readInputB(ib: Int): Int {
            lastInputBSelection = ib
            return inputB
        }

        override fun writeOutputF(value: Int) {
            lastOutputF = value
        }

        override fun writeOutputControl(value: Int) {
            lastOutputControl = value
        }

        override fun consumeKeyOnSignal(): Boolean {
            val result = keyOnSignal
            keyOnSignal = false
            return result
        }
    }

    private class TestBus(vararg bytes: Int) : Sc61860Bus {
        private val memory = IntArray(0x10000)

        init {
            bytes.forEachIndexed { index, value -> memory[index] = value }
        }

        override fun read(address: Int): Int = memory[address]

        override fun write(address: Int, value: Int) {
            memory[address] = value and 0xff
        }

        operator fun get(address: Int): Int = memory[address]

        operator fun set(address: Int, value: Int) {
            memory[address] = value
        }
    }
}
