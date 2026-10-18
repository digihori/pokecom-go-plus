package com.digihori.pgp.core.emulator.cpu

/** CPU-facing memory boundary. Machine-specific mapping is implemented outside the CPU. */
internal interface Sc61860Bus {
    /** Returns an unsigned byte in the range 0x00..0xff. */
    fun read(address: Int): Int

    /** READ/READM data source. Most machines read the next opcode without consuming it. */
    fun readProgramData(programCounter: Int, dataPointer: Int): Sc61860ProgramDataRead =
        Sc61860ProgramDataRead(programCounter, read(programCounter))

    /** Writes the low eight bits of [value] to a 16-bit [address]. */
    fun write(address: Int, value: Int)
}

/** Address and value in the machine-specific READ/READM data space. */
internal data class Sc61860ProgramDataRead(val address: Int, val value: Int)
