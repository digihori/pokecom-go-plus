package com.digihori.pgp.core.emulator.cpu

/** CPU-facing memory boundary. Machine-specific mapping is implemented outside the CPU. */
internal interface Sc61860Bus {
    /** Returns an unsigned byte in the range 0x00..0xff. */
    fun read(address: Int): Int

    /** Writes the low eight bits of [value] to a 16-bit [address]. */
    fun write(address: Int, value: Int)
}
