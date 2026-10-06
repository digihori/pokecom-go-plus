package com.digihori.pgp.core.emulator.cpu

import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.MemoryAccessKind

internal class Sc61860BusAccessRecorder(private val delegate: Sc61860Bus) : Sc61860Bus {
    private val accesses = mutableListOf<MemoryAccess>()
    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) accesses.clear()
        }

    override fun read(address: Int): Int {
        val value = delegate.read(address)
        if (enabled) accesses += MemoryAccess(MemoryAccessKind.READ, address and 0xffff, value and 0xff)
        return value
    }

    override fun write(address: Int, value: Int) {
        if (enabled) accesses += MemoryAccess(MemoryAccessKind.WRITE, address and 0xffff, value and 0xff)
        delegate.write(address, value)
    }

    fun drain(): List<MemoryAccess> = accesses.toList().also { accesses.clear() }
}
