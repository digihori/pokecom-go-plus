package com.digihori.pgp.core.emulator.cpu

/**
 * Mutable execution state owned by one SC61860 CPU instance.
 *
 * This type intentionally contains no machine memory, display state, host timing, or platform API.
 * It is internal until the instruction implementation proves which fields belong in the public snapshot.
 */
internal class Sc61860State {
    var programCounter: Int = 0
    var currentProgramCounter: Int = 0
    var opcode: Int = 0
    var dataPointer: Int = 0

    var p: Int = 0
    var q: Int = 0
    var r: Int = INTERNAL_RAM_REGISTER_LIMIT
    var d: Int = 0

    var alu: Int = 0
    var carry: Boolean = false
    var zero: Boolean = false
    var xInput: Int = 0

    var ticks: Int = 0
    var ticks2: Int = 0
    var divider500: Boolean = false
    var divider2: Boolean = false

    var powerOn: Boolean = true

    var ia: Int = 0
    var ib: Int = 0
    var fo: Int = 0
    var control: Int = 0
    var testPort: Int = 0

    /*
     * Pokecom GO allocated 256 entries even though direct IRAM validation used 0x00..0x5f.
     * Keep that storage size until all indexed instruction behavior has Golden Tests.
     */
    val internalRam: IntArray = IntArray(INTERNAL_RAM_STORAGE_SIZE)

    fun reset() {
        programCounter = 0
        currentProgramCounter = 0
        opcode = 0
        dataPointer = 0

        p = 0
        q = 0
        r = INTERNAL_RAM_REGISTER_LIMIT
        d = 0

        alu = 0
        carry = false
        zero = false
        xInput = 0

        ticks = 0
        ticks2 = 0
        divider500 = false
        divider2 = false

        powerOn = true

        ia = 0
        ib = 0
        fo = 0
        control = 0
        testPort = 0

        internalRam.fill(0)
    }

    internal companion object {
        const val INTERNAL_RAM_REGISTER_LIMIT: Int = 0x60
        const val INTERNAL_RAM_STORAGE_SIZE: Int = 0x100
    }
}
