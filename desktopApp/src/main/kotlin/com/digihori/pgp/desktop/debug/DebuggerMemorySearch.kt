package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.MachineMemoryKind
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.RunnerState

internal enum class MemorySearchFormat { HEX, U8, U16_LE, U16_BE }

internal object DebuggerMemorySearch {
    const val MAX_RESULTS = 256
    const val MAX_PATTERN = 64

    fun address(text: String): Int? = text.trim().removePrefix("0x").removePrefix("0X")
        .takeIf { it.matches(Regex("[0-9a-fA-F]{1,4}")) }?.toIntOrNull(16)

    fun pattern(text: String, format: MemorySearchFormat): ByteArray? {
        if (text.length > 1024) return null
        if (format == MemorySearchFormat.HEX) {
            val parts = text.trim().split(Regex("\\s+"))
            if (parts.size !in 1..MAX_PATTERN || parts.any { !it.matches(Regex("[0-9a-fA-F]{2}")) }) return null
            return parts.map { it.toInt(16).toByte() }.toByteArray()
        }
        val valueText = text.trim()
        val hex = valueText.startsWith("0x", true)
        val digits = if (hex) valueText.drop(2) else valueText
        if (!digits.matches(if (hex) Regex("[0-9a-fA-F]{1,4}") else Regex("[0-9]{1,5}"))) return null
        val value = digits.toIntOrNull(if (hex) 16 else 10) ?: return null
        if (value !in 0..if (format == MemorySearchFormat.U8) 255 else 65535) return null
        return when (format) {
            MemorySearchFormat.U8 -> byteArrayOf(value.toByte())
            MemorySearchFormat.U16_LE -> byteArrayOf(value.toByte(), (value ushr 8).toByte())
            MemorySearchFormat.U16_BE -> byteArrayOf((value ushr 8).toByte(), value.toByte())
        }
    }

    data class Result(val addresses: List<Int>, val truncated: Boolean, val skipped: Int,
        val revision: Long, val bank: Int?)

    fun search(runner: DesktopEmulatorRunner, start: Int, end: Int, pattern: ByteArray): Result {
        require(runner.state == RunnerState.PAUSED)
        require(start in 0..0xffff && end in start..0xffff)
        require(pattern.size in 1..MAX_PATTERN)
        val revision = runner.sessionRevision
        val bank = runner.selectedRomBank()
        // Copy only ordinary RAM/ROM. Do not probe control registers, LCD or unmapped bus holes.
        val values = IntArray(end - start + 1) { -1 }
        fun readable(at: Int): Boolean = when (runner.memoryLayout.locate(at).kind) {
            MachineMemoryKind.RAM, MachineMemoryKind.ROM -> true
            else -> false
        }
        var skipped = 0
        var address = start
        while (address <= end) {
            if (!readable(address)) { skipped++; address++; continue }
            val first = address
            while (address <= end && readable(address)) address++
            val bytes = runner.memorySnapshot(first, address - first).copyBytes()
            bytes.forEachIndexed { index, byte -> values[first - start + index] = byte.toInt() and 255 }
        }
        val matches = mutableListOf<Int>()
        var truncated = false
        for (offset in 0..values.size - pattern.size) {
            if (pattern.indices.all { values[offset + it] == (pattern[it].toInt() and 255) }) {
                if (matches.size == MAX_RESULTS) { truncated = true; break }
                matches += start + offset
            }
        }
        check(runner.state == RunnerState.PAUSED && runner.sessionRevision == revision && runner.selectedRomBank() == bank)
        return Result(matches, truncated, skipped, revision, bank)
    }
}
