package com.digihori.pgp.core.emulator.cpu

/** Machine-specific signals connected to the SC61860 ports. */
internal interface Sc61860Io {
    fun readInputA(ia: Int, ib: Int): Int

    fun readInputB(ib: Int): Int

    fun consumeKeyOnSignal(): Boolean = false

    fun writeOutputF(value: Int) = Unit

    fun writeOutputControl(value: Int) = Unit
}

internal object DisconnectedSc61860Io : Sc61860Io {
    override fun readInputA(ia: Int, ib: Int): Int = 0

    override fun readInputB(ib: Int): Int = 0
}
