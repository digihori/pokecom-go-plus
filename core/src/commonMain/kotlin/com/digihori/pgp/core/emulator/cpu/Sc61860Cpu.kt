package com.digihori.pgp.core.emulator.cpu

internal class Sc61860Cpu(
    private val bus: Sc61860Bus,
    private val io: Sc61860Io = DisconnectedSc61860Io,
    internal val state: Sc61860State = Sc61860State(),
) {
    fun reset() {
        state.reset()
    }

    fun step(): Sc61860StepResult {
        val instructionAddress = state.programCounter
        val opcode = readByte(instructionAddress)

        state.currentProgramCounter = instructionAddress
        state.opcode = opcode
        state.programCounter = increment16(instructionAddress)

        val result = when (opcode) {
            0x00 -> loadImmediate(INTERNAL_I)
            0x01 -> loadImmediate(INTERNAL_J)
            0x02 -> loadImmediate(INTERNAL_A)
            0x03 -> loadImmediate(INTERNAL_B)
            0x04 -> updateIndex(INTERNAL_X_LOW, increment = true)
            0x05 -> updateIndex(INTERNAL_X_LOW, increment = false)
            0x06 -> updateIndex(INTERNAL_Y_LOW, increment = true)
            0x07 -> updateIndex(INTERNAL_Y_LOW, increment = false)
            0x08 -> transferInternalBlock(INTERNAL_I, exchange = false)
            0x09 -> transferInternalBlock(INTERNAL_I, exchange = true)
            0x0a -> transferInternalBlock(INTERNAL_J, exchange = false)
            0x0b -> transferInternalBlock(INTERNAL_J, exchange = true)
            0x0c -> decimalWithAccumulator(subtract = false)
            0x0d -> decimalWithAccumulator(subtract = true)
            0x0e -> decimalWithQ(subtract = false)
            0x0f -> decimalWithQ(subtract = true)
            0x10 -> loadDataPointerImmediate()
            0x11 -> loadDataPointerLowImmediate()
            0x12 -> loadPImmediate()
            0x13 -> loadQImmediate()
            0x14 -> addRegisterPairToP()
            0x15 -> subtractRegisterPairFromP()
            0x18 -> transferDataBlock(INTERNAL_I, exchange = false)
            0x19 -> transferDataBlock(INTERNAL_I, exchange = true)
            0x1a -> transferDataBlock(INTERNAL_J, exchange = false)
            0x1b -> transferDataBlock(INTERNAL_J, exchange = true)
            0x1c -> shiftInternalWordRight()
            0x1d -> shiftInternalWordLeft()
            0x1e -> fillInternalBlock()
            0x1f -> fillDataBlock()

            0x20 -> loadPointerIntoAccumulator(state.p)
            0x21 -> loadPointerIntoAccumulator(state.q)
            0x22 -> loadPointerIntoAccumulator(state.r)
            0x23 -> clearAccumulator()
            0x24 -> updateIndex(INTERNAL_X_LOW, increment = true, loadAccumulator = true)
            0x25 -> updateIndex(INTERNAL_X_LOW, increment = false, loadAccumulator = true)
            0x26 -> updateIndex(INTERNAL_Y_LOW, increment = true, storeAccumulator = true)
            0x27 -> updateIndex(INTERNAL_Y_LOW, increment = false, storeAccumulator = true)
            0x28 -> jumpRelative(forward = true, condition = !state.zero)
            0x29 -> jumpRelative(forward = false, condition = !state.zero)
            0x2a -> jumpRelative(forward = true, condition = !state.carry)
            0x2b -> jumpRelative(forward = false, condition = !state.carry)
            0x2c -> jumpRelative(forward = true)
            0x2d -> jumpRelative(forward = false)
            0x2f -> loop()
            0x30 -> storeAccumulatorInP()
            0x31 -> storeAccumulatorInQ()
            0x32 -> storeAccumulatorInR()
            0x34 -> pushAccumulator()
            0x35 -> moveWordFromAccumulatorPointer()
            0x37 -> returnFromCall()
            0x38 -> jumpRelative(forward = true, condition = state.zero)
            0x39 -> jumpRelative(forward = false, condition = state.zero)
            0x3a -> jumpRelative(forward = true, condition = state.carry)
            0x3b -> jumpRelative(forward = false, condition = state.carry)
            0x40 -> incrementRegister(INTERNAL_I)
            0x41 -> decrementRegister(INTERNAL_I)
            0x42 -> incrementRegister(INTERNAL_A)
            0x43 -> decrementRegister(INTERNAL_A)
            0x44 -> addAccumulatorToP(withCarry = false)
            0x45 -> subtractAccumulatorFromP(withCarry = false)
            0x46 -> andAccumulatorWithP()
            0x47 -> orAccumulatorWithP()
            0x4c -> readInputA()
            0x4d -> Sc61860StepResult(cycles = 2)
            0x4e -> waitCycles()
            0x4f -> countUpWhileXInput(isHigh = false)
            0x48 -> incrementRegister(INTERNAL_K)
            0x49 -> decrementRegister(INTERNAL_K)
            0x4a -> incrementRegister(INTERNAL_M)
            0x4b -> decrementRegister(INTERNAL_M)
            0x50 -> incrementP()
            0x51 -> decrementP()
            0x52 -> storeAccumulatorAtDataPointer()
            0x53 -> storePAtDataPointer()
            0x54 -> loadProgramByteIntoP()
            0x55 -> loadDataByteIntoP()
            0x56 -> loadProgramByteIntoAccumulator()
            0x57 -> loadDataByteIntoAccumulator()
            0x58 -> swapAccumulatorNibbles()
            0x59 -> loadPIntoAccumulator()
            0x5a -> shiftAccumulatorLeftThroughCarry()
            0x5b -> popAccumulator()
            0x5d -> outputA()
            0x5f -> outputF()
            0x60 -> andImmediate(state.p)
            0x61 -> orImmediate(state.p)
            0x62 -> testImmediate(state.p)
            0x63 -> compareImmediate(state.p)
            0x64 -> andImmediate(INTERNAL_A)
            0x65 -> orImmediate(INTERNAL_A)
            0x66 -> testImmediate(INTERNAL_A)
            0x67 -> compareImmediate(INTERNAL_A)
            0x69 -> case2()
            0x6b -> testPort()
            0x6f -> countUpWhileXInput(isHigh = true)
            0x70 -> addImmediate(state.p)
            0x71 -> subtractImmediate(state.p)
            0x72, 0x73, 0x76, 0x77 -> resetZeroAndSkipByte()
            0x74 -> addImmediate(INTERNAL_A)
            0x75 -> subtractImmediate(INTERNAL_A)
            0x78 -> callAbsolute()
            0x79 -> jumpAbsolute()
            0x7a -> case1()
            0x7c -> jumpAbsoluteIf(!state.zero)
            0x7d -> jumpAbsoluteIf(!state.carry)
            0x7e -> jumpAbsoluteIf(state.zero)
            0x7f -> jumpAbsoluteIf(state.carry)
            0xc0 -> incrementRegister(INTERNAL_J)
            0xc1 -> decrementRegister(INTERNAL_J)
            0xc2 -> incrementRegister(INTERNAL_B)
            0xc3 -> decrementRegister(INTERNAL_B)
            0xc4 -> addAccumulatorToP(withCarry = true)
            0xc5 -> subtractAccumulatorFromP(withCarry = true)
            0xc6 -> testAccumulatorWithP()
            0xc7 -> compareAccumulatorWithP()
            0xc8 -> incrementRegister(INTERNAL_L)
            0xc9 -> decrementRegister(INTERNAL_L)
            0xca -> incrementRegister(INTERNAL_N)
            0xcb -> decrementRegister(INTERNAL_N)
            0xcc -> readInputB()
            0xce -> Sc61860StepResult(cycles = 2)
            0xd0 -> setCarry()
            0xd1 -> resetCarry()
            0xd2 -> shiftAccumulatorRightThroughCarry()
            0xd3, 0xd9 -> Sc61860StepResult(cycles = 2)
            0xd4 -> andImmediateAtDataPointer()
            0xd5 -> orImmediateAtDataPointer()
            0xd6 -> testImmediateAtDataPointer()
            0xd7 -> setZeroAndSkipByte()
            0xd8 -> leave()
            0xda -> exchangeAccumulatorAndB()
            0xdb -> exchangeAccumulatorWithP()
            0xdd -> outputB()
            0xdf -> outputControl()

            in 0x80..0xbf -> loadPFromOpcode(opcode)
            in 0xe0..0xff -> callPage(opcode)

            0x33, 0x68, 0x6a, 0xcd -> Sc61860StepResult(cycles = 3)

            else -> Sc61860StepResult(
                cycles = 0,
                stopReason = Sc61860StopReason.UnsupportedOpcode(
                    address = instructionAddress,
                    opcode = opcode,
                ),
            )
        }

        if (result.stopReason == null) {
            advanceDividers(result.cycles)
        }
        return result
    }

    private fun advanceDividers(cycles: Int) {
        val divider2Total = state.ticks + cycles
        if (divider2Total >= DIVIDER_2_MS_CYCLES) state.divider2 = true
        state.ticks = divider2Total % DIVIDER_2_MS_CYCLES

        val divider500Total = state.ticks2 + cycles
        if (divider500Total >= DIVIDER_500_MS_CYCLES) state.divider500 = true
        state.ticks2 = divider500Total % DIVIDER_500_MS_CYCLES
    }

    private fun loadImmediate(register: Int): Sc61860StepResult {
        state.internalRam[register] = readByte(state.programCounter)
        state.programCounter = increment16(state.programCounter)
        return Sc61860StepResult(cycles = 4)
    }

    private fun loadPImmediate(): Sc61860StepResult {
        state.p = readByte(state.programCounter) and INTERNAL_ADDRESS_MASK
        state.programCounter = increment16(state.programCounter)
        return Sc61860StepResult(cycles = 4)
    }

    private fun loadQImmediate(): Sc61860StepResult {
        state.q = consumeImmediate() and INTERNAL_ADDRESS_MASK
        return Sc61860StepResult(cycles = 4)
    }

    private fun loadDataPointerLowImmediate(): Sc61860StepResult {
        state.dataPointer = (state.dataPointer and 0xff00) or consumeImmediate()
        return Sc61860StepResult(cycles = 5)
    }

    private fun loadPointerIntoAccumulator(value: Int): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = value and BYTE_MASK
        return Sc61860StepResult(cycles = 2)
    }

    private fun storeAccumulatorInP(): Sc61860StepResult {
        state.p = state.internalRam[INTERNAL_A] and INTERNAL_ADDRESS_MASK
        return Sc61860StepResult(cycles = 2)
    }

    private fun storeAccumulatorInQ(): Sc61860StepResult {
        state.q = state.internalRam[INTERNAL_A] and INTERNAL_ADDRESS_MASK
        return Sc61860StepResult(cycles = 2)
    }

    private fun storeAccumulatorInR(): Sc61860StepResult {
        state.r = state.internalRam[INTERNAL_A] and INTERNAL_ADDRESS_MASK
        return Sc61860StepResult(cycles = 2)
    }

    private fun incrementP(): Sc61860StepResult {
        state.p = increment7(state.p)
        return Sc61860StepResult(cycles = 2)
    }

    private fun decrementP(): Sc61860StepResult {
        state.p = decrement7(state.p)
        return Sc61860StepResult(cycles = 2)
    }

    private fun loadPFromOpcode(opcode: Int): Sc61860StepResult {
        state.p = opcode and SHORT_P_MASK
        return Sc61860StepResult(cycles = 2)
    }

    private fun updateIndex(
        lowRegister: Int,
        increment: Boolean,
        loadAccumulator: Boolean = false,
        storeAccumulator: Boolean = false,
    ): Sc61860StepResult {
        check(!(loadAccumulator && storeAccumulator))
        val highRegister = lowRegister + 1
        val current = (state.internalRam[highRegister] shl 8) or state.internalRam[lowRegister]
        val next = if (increment) increment16(current) else subtract16(current, 1)

        state.q = highRegister
        state.dataPointer = next
        if (loadAccumulator) state.internalRam[INTERNAL_A] = readByte(next)
        if (storeAccumulator) bus.write(next, state.internalRam[INTERNAL_A])
        state.internalRam[highRegister] = (next ushr 8) and BYTE_MASK
        state.internalRam[lowRegister] = next and BYTE_MASK

        return Sc61860StepResult(cycles = if (loadAccumulator) 7 else 6)
    }

    private fun transferInternalBlock(
        countRegister: Int,
        exchange: Boolean,
    ): Sc61860StepResult {
        val encodedCount = state.internalRam[countRegister]
        val count = encodedCount + 1

        repeat(count) {
            val source = state.internalRam[state.q]
            if (exchange) {
                val destination = state.internalRam[state.p]
                state.internalRam[state.q] = destination
            }
            state.internalRam[state.p] = source
            state.p = increment7(state.p)
            state.q = increment7(state.q)
        }
        state.d = 0

        val cyclesPerEncodedByte = if (exchange) 3 else 2
        return Sc61860StepResult(cycles = encodedCount * cyclesPerEncodedByte + 3)
    }

    private fun transferDataBlock(
        countRegister: Int,
        exchange: Boolean,
    ): Sc61860StepResult {
        val encodedCount = state.internalRam[countRegister]
        val count = encodedCount + 1

        repeat(count) {
            val dataValue = readByte(state.dataPointer)
            if (exchange) bus.write(state.dataPointer, state.internalRam[state.p])
            state.internalRam[state.p] = dataValue
            state.p = increment7(state.p)
            state.dataPointer = increment16(state.dataPointer)
        }
        state.dataPointer = subtract16(state.dataPointer, 1)
        state.d = 0

        val cyclesPerEncodedByte = if (exchange) 6 else 4
        return Sc61860StepResult(cycles = encodedCount * cyclesPerEncodedByte + 3)
    }

    private fun fillInternalBlock(): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        repeat(encodedCount + 1) {
            state.internalRam[state.p] = state.internalRam[INTERNAL_A]
            state.p = increment7(state.p)
        }
        state.d = 0
        return Sc61860StepResult(cycles = encodedCount + 4)
    }

    private fun fillDataBlock(): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        repeat(encodedCount + 1) {
            bus.write(state.dataPointer, state.internalRam[INTERNAL_A])
            state.dataPointer = increment16(state.dataPointer)
        }
        state.dataPointer = subtract16(state.dataPointer, 1)
        state.d = 0
        // FILD performs an external bus write for every byte. Unlike FILM, each
        // additional byte costs three cycles; timing loops use this distinction.
        return Sc61860StepResult(cycles = encodedCount * 3 + 4)
    }

    private fun decimalWithAccumulator(subtract: Boolean): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        var carryOrBorrow = state.internalRam[INTERNAL_A]
        var allZero = true

        repeat(encodedCount + 1) {
            val result = if (subtract) {
                decimalSubtract(state.internalRam[state.p], carryOrBorrow)
            } else {
                decimalAdd(state.internalRam[state.p], carryOrBorrow)
            }
            state.internalRam[state.p] = result and BYTE_MASK
            allZero = allZero && state.internalRam[state.p] == 0
            state.p = decrement7(state.p)
            carryOrBorrow = result ushr 8
        }

        state.alu = carryOrBorrow
        state.zero = allZero
        state.carry = carryOrBorrow != 0
        state.d = 0
        return Sc61860StepResult(cycles = encodedCount * 3 + 4)
    }

    private fun decimalWithQ(subtract: Boolean): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        var carryOrBorrow = 0
        var allZero = true

        repeat(encodedCount + 1) {
            var result = if (subtract) {
                decimalSubtract(state.internalRam[state.p], carryOrBorrow)
            } else {
                decimalAdd(state.internalRam[state.p], carryOrBorrow)
            }
            result = (result and 0x0100) or if (subtract) {
                decimalSubtract(result and BYTE_MASK, state.internalRam[state.q])
            } else {
                decimalAdd(result and BYTE_MASK, state.internalRam[state.q])
            }
            state.internalRam[state.p] = result and BYTE_MASK
            allZero = allZero && state.internalRam[state.p] == 0
            state.p = decrement7(state.p)
            state.q = decrement7(state.q)
            carryOrBorrow = result ushr 8
        }

        state.alu = carryOrBorrow
        state.zero = allZero
        state.carry = carryOrBorrow != 0
        state.d = 0
        return Sc61860StepResult(cycles = encodedCount * 3 + 4)
    }

    private fun decimalAdd(leftValue: Int, rightValue: Int): Int {
        val left = normalizePackedDecimal(leftValue)
        val right = normalizePackedDecimal(rightValue)
        var result = (left and 0x0f) + (right and 0x0f)
        result = if (result >= 10) {
            result - 10 + (left and 0xf0) + (right and 0xf0) + 0x10
        } else {
            result + (left and 0xf0) + (right and 0xf0)
        }
        if (result >= 0xa0) result = result - 0xa0 + 0x100
        return result and 0x01ff
    }

    private fun decimalSubtract(leftValue: Int, rightValue: Int): Int {
        val left = normalizePackedDecimal(leftValue)
        val right = normalizePackedDecimal(rightValue)
        var result = (left and 0x0f) - (right and 0x0f)
        result = if (result < 0) {
            result + 10 + (left and 0xf0) - (right and 0xf0) - 0x10
        } else {
            result + (left and 0xf0) - (right and 0xf0)
        }
        if (result < 0) result += 0xa0 + 0x100
        return result and 0x01ff
    }

    private fun normalizePackedDecimal(value: Int): Int {
        var normalized = value and BYTE_MASK
        if (normalized and 0x0f > 9) normalized = normalized and 0xf0
        if (normalized and 0xf0 > 0x90) normalized = normalized and 0x0f
        return normalized
    }

    private fun shiftInternalWordRight(): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        var shiftBuffer = 0
        repeat(encodedCount + 1) {
            shiftBuffer = (shiftBuffer shl 8) or state.internalRam[state.p]
            state.internalRam[state.p] = (shiftBuffer ushr 4) and BYTE_MASK
            state.p = increment7(state.p)
        }
        state.alu = shiftBuffer
        state.d = 0
        return Sc61860StepResult(cycles = encodedCount + 4)
    }

    private fun shiftInternalWordLeft(): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        var shiftBuffer = 0
        repeat(encodedCount + 1) {
            shiftBuffer = (state.internalRam[state.p] shl 8) or (shiftBuffer ushr 8)
            state.internalRam[state.p] = (shiftBuffer ushr 4) and BYTE_MASK
            state.p = decrement7(state.p)
        }
        state.alu = shiftBuffer
        state.d = 0
        return Sc61860StepResult(cycles = encodedCount + 4)
    }

    private fun storeAccumulatorAtDataPointer(): Sc61860StepResult {
        bus.write(state.dataPointer, state.internalRam[INTERNAL_A])
        return Sc61860StepResult(cycles = 2)
    }

    private fun storePAtDataPointer(): Sc61860StepResult {
        bus.write(state.dataPointer, state.internalRam[state.p])
        return Sc61860StepResult(cycles = 3)
    }

    private fun loadProgramByteIntoP(): Sc61860StepResult {
        state.internalRam[state.p] = readByte(state.programCounter)
        return Sc61860StepResult(cycles = 3)
    }

    private fun loadDataByteIntoP(): Sc61860StepResult {
        state.internalRam[state.p] = readByte(state.dataPointer)
        return Sc61860StepResult(cycles = 3)
    }

    private fun loadProgramByteIntoAccumulator(): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = readByte(state.programCounter)
        return Sc61860StepResult(cycles = 3)
    }

    private fun loadDataByteIntoAccumulator(): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = readByte(state.dataPointer)
        return Sc61860StepResult(cycles = 3)
    }

    private fun swapAccumulatorNibbles(): Sc61860StepResult {
        val value = state.internalRam[INTERNAL_A]
        state.internalRam[INTERNAL_A] = ((value and 0x0f) shl 4) or ((value and 0xf0) ushr 4)
        return Sc61860StepResult(cycles = 2)
    }

    private fun loadPIntoAccumulator(): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = state.internalRam[state.p]
        return Sc61860StepResult(cycles = 2)
    }

    private fun shiftAccumulatorLeftThroughCarry(): Sc61860StepResult {
        val carryIn = if (state.carry) 1 else 0
        state.alu = (state.internalRam[INTERNAL_A] shl 1) or carryIn
        state.internalRam[INTERNAL_A] = state.alu and BYTE_MASK
        state.carry = state.alu and 0xff00 != 0
        return Sc61860StepResult(cycles = 2)
    }

    private fun shiftAccumulatorRightThroughCarry(): Sc61860StepResult {
        val carryIn = if (state.carry) 1 shl 8 else 0
        state.alu = carryIn or state.internalRam[INTERNAL_A]
        state.internalRam[INTERNAL_A] = (state.alu ushr 1) and BYTE_MASK
        state.carry = state.alu and 1 != 0
        return Sc61860StepResult(cycles = 2)
    }

    private fun loadDataPointerImmediate(): Sc61860StepResult {
        state.dataPointer = readWord(state.programCounter)
        state.programCounter = add16(state.programCounter, 2)
        return Sc61860StepResult(cycles = 8)
    }

    private fun addRegisterPairToP(): Sc61860StepResult =
        updatePWord(readPWord() + readAccumulatorPair())

    private fun subtractRegisterPairFromP(): Sc61860StepResult =
        updatePWord(readPWord() - readAccumulatorPair())

    private fun readPWord(): Int {
        val highAddress = increment7(state.p)
        return (state.internalRam[highAddress] shl 8) or state.internalRam[state.p]
    }

    private fun readAccumulatorPair(): Int =
        (state.internalRam[INTERNAL_B] shl 8) or state.internalRam[INTERNAL_A]

    private fun updatePWord(result: Int): Sc61860StepResult {
        state.internalRam[state.p] = result and BYTE_MASK
        state.p = increment7(state.p)
        state.internalRam[state.p] = (result ushr 8) and BYTE_MASK
        state.zero = result and ADDRESS_MASK == 0
        state.carry = result and -0x10000 != 0
        return Sc61860StepResult(cycles = 5)
    }

    private fun pushAccumulator(): Sc61860StepResult {
        state.r = decrement8(state.r)
        state.internalRam[state.r] = state.internalRam[INTERNAL_A]
        return Sc61860StepResult(cycles = 3)
    }

    private fun moveWordFromAccumulatorPointer(): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        val opcodeAddress = subtract16(state.programCounter, 1)
        pushReturnAddress(opcodeAddress)
        state.programCounter = readAccumulatorPair()

        repeat(encodedCount + 1) {
            state.internalRam[state.p] = readByte(state.programCounter)
            state.p = increment7(state.p)
            state.programCounter = increment16(state.programCounter)
        }
        state.d = 0

        val low = state.internalRam[state.r]
        val high = state.internalRam[increment8(state.r)]
        state.programCounter = increment16((high shl 8) or low)
        state.r = add8(state.r, 2)
        return Sc61860StepResult(cycles = encodedCount * 4 + 7)
    }

    private fun countUpWhileXInput(isHigh: Boolean): Sc61860StepResult {
        val encodedCount = state.internalRam[INTERNAL_I]
        state.d = increment8(encodedCount)
        state.zero = false

        val inputMatches = (state.xInput != 0) == isHigh
        if (inputMatches) {
            do {
                state.p = increment7(state.p)
                state.d = decrement8(state.d)
            } while (state.d != BYTE_MASK)
            state.zero = true
        }

        return Sc61860StepResult(cycles = encodedCount * 4 + 1)
    }

    private fun waitCycles(): Sc61860StepResult =
        Sc61860StepResult(cycles = consumeImmediate() + 6)

    private fun clearAccumulator(): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = 0
        return Sc61860StepResult(cycles = 2)
    }

    private fun incrementRegister(register: Int): Sc61860StepResult =
        updateRegister(register, state.internalRam[register] + 1)

    private fun decrementRegister(register: Int): Sc61860StepResult =
        updateRegister(register, state.internalRam[register] - 1)

    private fun updateRegister(register: Int, result: Int): Sc61860StepResult {
        state.q = register
        state.alu = result and ADDRESS_MASK
        state.internalRam[register] = state.alu and BYTE_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 4)
    }

    private fun updateZeroAndCarry() {
        state.zero = state.alu and BYTE_MASK == 0
        state.carry = state.alu and 0xff00 != 0
    }

    private fun setCarry(): Sc61860StepResult {
        state.carry = true
        state.zero = true
        return Sc61860StepResult(cycles = 2)
    }

    private fun resetCarry(): Sc61860StepResult {
        state.carry = false
        state.zero = true
        return Sc61860StepResult(cycles = 2)
    }

    private fun resetZeroAndSkipByte(): Sc61860StepResult {
        state.programCounter = increment16(state.programCounter)
        state.zero = false
        return Sc61860StepResult(cycles = 4)
    }

    private fun leave(): Sc61860StepResult {
        state.internalRam[state.r] = 0
        return Sc61860StepResult(cycles = 2)
    }

    private fun exchangeAccumulatorAndB(): Sc61860StepResult {
        val accumulator = state.internalRam[INTERNAL_A]
        state.internalRam[INTERNAL_A] = state.internalRam[INTERNAL_B]
        state.internalRam[INTERNAL_B] = accumulator
        return Sc61860StepResult(cycles = 3)
    }

    private fun andImmediateAtDataPointer(): Sc61860StepResult {
        saveDataByteBelowR()
        bus.write(state.dataPointer, readByte(state.dataPointer) and consumeImmediate())
        state.zero = readByte(state.dataPointer) == 0
        return Sc61860StepResult(cycles = 6)
    }

    private fun orImmediateAtDataPointer(): Sc61860StepResult {
        saveDataByteBelowR()
        bus.write(state.dataPointer, readByte(state.dataPointer) or consumeImmediate())
        state.zero = readByte(state.dataPointer) == 0
        return Sc61860StepResult(cycles = 6)
    }

    private fun testImmediateAtDataPointer(): Sc61860StepResult {
        val value = readByte(state.dataPointer)
        state.internalRam[decrement8(state.r)] = value
        state.zero = value and consumeImmediate() == 0
        return Sc61860StepResult(cycles = 6)
    }

    private fun setZeroAndSkipByte(): Sc61860StepResult {
        saveDataByteBelowR()
        state.programCounter = increment16(state.programCounter)
        state.zero = true
        return Sc61860StepResult(cycles = 6)
    }

    private fun saveDataByteBelowR() {
        state.internalRam[decrement8(state.r)] = readByte(state.dataPointer)
    }

    private fun addImmediate(register: Int): Sc61860StepResult {
        val immediate = consumeImmediate()
        state.alu = (state.internalRam[register] + immediate) and ADDRESS_MASK
        state.internalRam[register] = state.alu and BYTE_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 4)
    }

    private fun subtractImmediate(register: Int): Sc61860StepResult {
        val immediate = consumeImmediate()
        state.alu = (state.internalRam[register] - immediate) and ADDRESS_MASK
        state.internalRam[register] = state.alu and BYTE_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 4)
    }

    private fun andImmediate(register: Int): Sc61860StepResult {
        state.alu = state.internalRam[register] and consumeImmediate()
        state.internalRam[register] = state.alu
        state.zero = state.alu == 0
        return Sc61860StepResult(cycles = 4)
    }

    private fun orImmediate(register: Int): Sc61860StepResult {
        state.alu = state.internalRam[register] or consumeImmediate()
        state.internalRam[register] = state.alu
        state.zero = state.alu == 0
        return Sc61860StepResult(cycles = 4)
    }

    private fun testImmediate(register: Int): Sc61860StepResult {
        state.zero = state.internalRam[register] and consumeImmediate() == 0
        return Sc61860StepResult(cycles = 4)
    }

    private fun compareImmediate(register: Int): Sc61860StepResult {
        state.alu = (state.internalRam[register] - consumeImmediate()) and ADDRESS_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 4)
    }

    private fun consumeImmediate(): Int {
        val value = readByte(state.programCounter)
        state.programCounter = increment16(state.programCounter)
        return value
    }

    private fun addAccumulatorToP(withCarry: Boolean): Sc61860StepResult {
        val carryIn = if (withCarry && state.carry) 1 else 0
        state.alu = (state.internalRam[state.p] + state.internalRam[INTERNAL_A] + carryIn) and ADDRESS_MASK
        state.internalRam[state.p] = state.alu and BYTE_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 3)
    }

    private fun subtractAccumulatorFromP(withCarry: Boolean): Sc61860StepResult {
        val carryIn = if (withCarry && state.carry) 1 else 0
        state.alu = (state.internalRam[state.p] - state.internalRam[INTERNAL_A] - carryIn) and ADDRESS_MASK
        state.internalRam[state.p] = state.alu and BYTE_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 3)
    }

    private fun andAccumulatorWithP(): Sc61860StepResult {
        state.alu = state.internalRam[state.p] and state.internalRam[INTERNAL_A]
        state.internalRam[state.p] = state.alu
        state.zero = state.alu == 0
        return Sc61860StepResult(cycles = 3)
    }

    private fun orAccumulatorWithP(): Sc61860StepResult {
        state.alu = state.internalRam[state.p] or state.internalRam[INTERNAL_A]
        state.internalRam[state.p] = state.alu
        state.zero = state.alu == 0
        return Sc61860StepResult(cycles = 3)
    }

    private fun testAccumulatorWithP(): Sc61860StepResult {
        state.zero = state.internalRam[state.p] and state.internalRam[INTERNAL_A] == 0
        return Sc61860StepResult(cycles = 3)
    }

    private fun compareAccumulatorWithP(): Sc61860StepResult {
        state.alu = (state.internalRam[state.p] - state.internalRam[INTERNAL_A]) and ADDRESS_MASK
        updateZeroAndCarry()
        return Sc61860StepResult(cycles = 3)
    }

    private fun popAccumulator(): Sc61860StepResult {
        state.internalRam[INTERNAL_A] = state.internalRam[state.r]
        state.r = increment8(state.r)
        return Sc61860StepResult(cycles = 2)
    }

    private fun callAbsolute(): Sc61860StepResult {
        val destination = readWord(state.programCounter)
        val returnAddress = add16(state.programCounter, 2)
        pushReturnAddress(returnAddress)
        state.programCounter = destination
        return Sc61860StepResult(cycles = 8)
    }

    private fun case1(): Sc61860StepResult {
        state.d = consumeImmediate()
        state.r = decrement8(state.r)
        state.internalRam[state.r] = consumeImmediate()
        state.r = decrement8(state.r)
        state.internalRam[state.r] = consumeImmediate()
        return Sc61860StepResult(cycles = 9)
    }

    private fun case2(): Sc61860StepResult {
        val initialCount = state.d
        val comparisons = if (initialCount == 0) 0x100 else initialCount

        repeat(comparisons) { index ->
            val candidate = readByte(state.programCounter)
            state.programCounter = increment16(state.programCounter)
            if (state.internalRam[INTERNAL_A] == candidate) {
                state.programCounter = readWord(state.programCounter)
                return Sc61860StepResult(cycles = (index + 1) * 7 + 2)
            }
            state.programCounter = add16(state.programCounter, 2)
            state.d = decrement8(state.d)
        }

        state.programCounter = readWord(state.programCounter)
        return Sc61860StepResult(cycles = initialCount * 7 + 7)
    }

    private fun callPage(opcode: Int): Sc61860StepResult {
        val destinationLow = readByte(state.programCounter)
        val returnAddress = increment16(state.programCounter)
        pushReturnAddress(returnAddress)
        state.programCounter = ((opcode and 0x1f) shl 8) or destinationLow
        return Sc61860StepResult(cycles = 7)
    }

    private fun returnFromCall(): Sc61860StepResult {
        val low = state.internalRam[state.r]
        val high = state.internalRam[increment8(state.r)]
        state.programCounter = (high shl 8) or low
        state.r = add8(state.r, 2)
        return Sc61860StepResult(cycles = 4)
    }

    private fun jumpAbsolute(): Sc61860StepResult {
        state.programCounter = readWord(state.programCounter)
        return Sc61860StepResult(cycles = 6)
    }

    private fun jumpAbsoluteIf(condition: Boolean): Sc61860StepResult {
        state.programCounter = if (condition) {
            readWord(state.programCounter)
        } else {
            add16(state.programCounter, 2)
        }
        return Sc61860StepResult(cycles = 6)
    }

    private fun jumpRelative(
        forward: Boolean,
        condition: Boolean = true,
    ): Sc61860StepResult {
        val offsetAddress = state.programCounter
        val offset = readByte(offsetAddress)
        state.internalRam[decrement8(state.r)] = offset

        if (!condition) {
            state.programCounter = increment16(offsetAddress)
            return Sc61860StepResult(cycles = 4)
        }

        state.programCounter = if (forward) {
            add16(offsetAddress, offset)
        } else {
            subtract16(offsetAddress, offset)
        }
        return Sc61860StepResult(cycles = 7)
    }

    private fun loop(): Sc61860StepResult {
        val offsetAddress = state.programCounter
        val counter = state.internalRam[state.r]
        state.programCounter = if (counter != 0) {
            subtract16(offsetAddress, readByte(offsetAddress))
        } else {
            increment16(offsetAddress)
        }

        state.alu = (counter - 1) and ADDRESS_MASK
        state.internalRam[state.r] = state.alu and BYTE_MASK
        updateZeroAndCarry()

        return if (state.internalRam[state.r] == BYTE_MASK) {
            state.r = increment7(state.r)
            Sc61860StepResult(cycles = 7)
        } else {
            Sc61860StepResult(cycles = 10)
        }
    }

    private fun pushReturnAddress(address: Int) {
        state.r = decrement8(state.r)
        state.internalRam[state.r] = (address ushr 8) and BYTE_MASK
        state.r = decrement8(state.r)
        state.internalRam[state.r] = address and BYTE_MASK
    }

    private fun exchangeAccumulatorWithP(): Sc61860StepResult {
        val accumulator = state.internalRam[INTERNAL_A]
        state.internalRam[INTERNAL_A] = state.internalRam[state.p]
        state.internalRam[state.p] = accumulator
        return Sc61860StepResult(cycles = 3)
    }

    private fun outputA(): Sc61860StepResult {
        state.q = INTERNAL_IA_PORT
        state.ia = state.internalRam[INTERNAL_IA_PORT]
        return Sc61860StepResult(cycles = 2)
    }

    private fun outputB(): Sc61860StepResult {
        state.q = INTERNAL_IB_PORT
        state.ib = state.internalRam[INTERNAL_IB_PORT]
        return Sc61860StepResult(cycles = 2)
    }

    private fun outputF(): Sc61860StepResult {
        state.q = INTERNAL_FO_PORT
        state.fo = state.internalRam[INTERNAL_FO_PORT]
        io.writeOutputF(state.fo)
        return Sc61860StepResult(cycles = 3)
    }

    private fun outputControl(): Sc61860StepResult {
        state.q = INTERNAL_CONTROL_PORT
        state.control = state.internalRam[INTERNAL_CONTROL_PORT]
        if (state.control and 0x02 != 0) {
            state.ticks = 0
            state.ticks2 = 0
            state.divider500 = false
            state.divider2 = false
        }
        state.powerOn = state.control and 0x08 == 0
        io.writeOutputControl(state.control)
        return Sc61860StepResult(cycles = 2)
    }

    private fun testPort(): Sc61860StepResult {
        var value = 0
        if (state.divider500) {
            state.divider500 = false
            value = value or 0x01
        }
        if (state.divider2) {
            state.divider2 = false
            value = value or 0x02
        }
        if (io.consumeKeyOnSignal()) value = value or 0x08

        state.testPort = value
        state.zero = value and consumeImmediate() == 0
        return Sc61860StepResult(cycles = 4)
    }

    private fun readInputA(): Sc61860StepResult = readInput(io.readInputA(state.ia, state.ib))

    private fun readInputB(): Sc61860StepResult = readInput(io.readInputB(state.ib))

    private fun readInput(value: Int): Sc61860StepResult {
        val normalized = value and BYTE_MASK
        state.internalRam[INTERNAL_A] = normalized
        state.zero = normalized == 0
        return Sc61860StepResult(cycles = 2)
    }

    private fun readByte(address: Int): Int {
        val normalizedAddress = address and ADDRESS_MASK
        val value = bus.read(normalizedAddress)
        require(value in 0..BYTE_MASK) {
            "Bus returned a non-byte value $value at address ${normalizedAddress.toString(16)}"
        }
        return value
    }

    private fun readWord(address: Int): Int =
        (readByte(address) shl 8) or readByte(increment16(address))

    private fun increment16(value: Int): Int = (value + 1) and ADDRESS_MASK

    private fun add16(value: Int, amount: Int): Int = (value + amount) and ADDRESS_MASK

    private fun subtract16(value: Int, amount: Int): Int = (value - amount) and ADDRESS_MASK

    private fun increment8(value: Int): Int = (value + 1) and BYTE_MASK

    private fun decrement8(value: Int): Int = (value - 1) and BYTE_MASK

    private fun add8(value: Int, amount: Int): Int = (value + amount) and BYTE_MASK

    private fun increment7(value: Int): Int = (value + 1) and INTERNAL_ADDRESS_MASK

    private fun decrement7(value: Int): Int = (value - 1) and INTERNAL_ADDRESS_MASK

    private companion object {
        const val DIVIDER_2_MS_CYCLES: Int = 576
        const val DIVIDER_500_MS_CYCLES: Int = 144_000
        const val ADDRESS_MASK: Int = 0xffff
        const val BYTE_MASK: Int = 0xff
        const val INTERNAL_ADDRESS_MASK: Int = 0x7f
        const val SHORT_P_MASK: Int = 0x3f

        const val INTERNAL_I: Int = 0x00
        const val INTERNAL_J: Int = 0x01
        const val INTERNAL_A: Int = 0x02
        const val INTERNAL_B: Int = 0x03
        const val INTERNAL_X_LOW: Int = 0x04
        const val INTERNAL_Y_LOW: Int = 0x06
        const val INTERNAL_K: Int = 0x08
        const val INTERNAL_L: Int = 0x09
        const val INTERNAL_M: Int = 0x0a
        const val INTERNAL_N: Int = 0x0b
        const val INTERNAL_IA_PORT: Int = 0x5c
        const val INTERNAL_IB_PORT: Int = 0x5d
        const val INTERNAL_FO_PORT: Int = 0x5e
        const val INTERNAL_CONTROL_PORT: Int = 0x5f
    }
}

internal data class Sc61860StepResult(
    val cycles: Int,
    val stopReason: Sc61860StopReason? = null,
)

internal data class Sc61860RunResult(
    val executedCycles: Long,
    val executedInstructions: Long,
    val stopReason: Sc61860StopReason? = null,
)

internal sealed interface Sc61860StopReason {
    data class UnsupportedOpcode(
        val address: Int,
        val opcode: Int,
    ) : Sc61860StopReason
}
