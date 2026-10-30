package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CpuSnapshot

/** A bounded comparison, parsed once at registration; no scripting or expression execution. */
internal data class DebuggerBreakpointCondition(
    val operand: String, val memoryAddress: Int?, val operator: String, val value: Int,
) {
    fun matches(cpu: () -> CpuSnapshot, memoryByte: (Int) -> Int): Boolean {
        val actual = read(cpu, memoryByte)
        return when (operator) {
            "==" -> actual == value
            "!=" -> actual != value
            "<" -> actual < value
            "<=" -> actual <= value
            ">" -> actual > value
            else -> actual >= value
        }
    }

    fun read(cpu: () -> CpuSnapshot, memoryByte: (Int) -> Int): Int =
        if (memoryAddress != null) memoryByte(memoryAddress) else {
            val snapshot = cpu()
            when (operand) {
                "PC" -> snapshot.programCounter
                "DP" -> snapshot.dataPointer
                "P" -> snapshot.p
                "Q" -> snapshot.q
                "R" -> snapshot.r
                "D" -> snapshot.d
                "C" -> if (snapshot.carry) 1 else 0
                "Z" -> if (snapshot.zero) 1 else 0
                else -> snapshot.copyInternalRam()[ramRegisters.indexOf(operand)].toInt() and 255
            }
        }

    companion object {
        private val ramRegisters = listOf("I", "J", "A", "B", "XL", "XH", "YL", "YH", "K", "L", "M", "N")
        private val pattern = Regex("""^\s*([A-Za-z]+|MEM\[\s*(?:0[xX][0-9A-Fa-f]+|\$[0-9A-Fa-f]+|[0-9]+)\s*])\s*(==|!=|<=|>=|<|>)\s*(0[xX][0-9A-Fa-f]+|\$[0-9A-Fa-f]+|[0-9]+)\s*$""", RegexOption.IGNORE_CASE)
        private fun number(text: String): Int? = when {
            text.startsWith("0x", true) -> text.drop(2).toIntOrNull(16)
            text.startsWith('$') -> text.drop(1).toIntOrNull(16)
            else -> text.toIntOrNull()
        }
        fun parse(text: String): DebuggerBreakpointCondition? {
            if (text.length > 128) return null
            val match = pattern.matchEntire(text) ?: return null
            val operand = match.groupValues[1].uppercase(java.util.Locale.ROOT)
            val address = if (operand.startsWith("MEM[")) number(operand.substringAfter('[').substringBefore(']').trim()) ?: return null else null
            if (address != null && address !in 0..0xffff) return null
            if (address == null && operand !in ramRegisters && operand !in listOf("PC", "DP", "P", "Q", "R", "D", "C", "Z")) return null
            val maximum = when { address != null -> 255; operand == "PC" || operand == "DP" -> 65535; operand == "C" || operand == "Z" -> 1; else -> 255 }
            val value = number(match.groupValues[3])?.takeIf { it in 0..maximum } ?: return null
            return DebuggerBreakpointCondition(operand, address, match.groupValues[2], value)
        }
    }
}

internal data class DebuggerBreakpointHitCondition(val operator: String, val value: Long) {
    fun matches(count: Long): Boolean = when (operator) {
        "==" -> count == value
        ">=" -> count >= value
        else -> count % value == 0L
    }
    companion object {
        fun parse(text: String): DebuggerBreakpointHitCondition? {
            if (text.length > 32) return null
            val match = Regex("""^\s*(==|>=|%)\s*([0-9]+)\s*$""").matchEntire(text) ?: return null
            val value = match.groupValues[2].toLongOrNull()?.takeIf { it in 1..1_000_000_000L } ?: return null
            return DebuggerBreakpointHitCondition(match.groupValues[1], value)
        }
    }
}
