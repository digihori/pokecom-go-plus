package com.digihori.pgp.desktop.application.development

import kotlinx.serialization.json.*

/** Instruction-specific effects from Sc61860Cpu. Timing excludes caller and loop overhead. */
internal object Sc61860InstructionKnowledge {
    const val SOURCE = "core/src/commonMain/kotlin/com/digihori/pgp/core/emulator/cpu/Sc61860Cpu.kt"

    private data class Timing(val base: Int, val whenUsed: String? = null, val perImmediate: Int? = null)
    private data class Notes(
        val reads: String,
        val writes: String,
        val timing: List<Timing>,
        val zero: String = "unchanged",
        val carry: String = "unchanged",
    )

    private fun fixed(cycles: Int, reads: String, writes: String, zero: String = "unchanged", carry: String = "unchanged") =
        Notes(reads, writes, listOf(Timing(cycles)), zero, carry)

    private val entries = mapOf(
        "LIA" to fixed(4, "Immediate byte", "A = immediate byte"),
        "LIP" to fixed(4, "Immediate byte", "P = immediate byte AND 0x7F"),
        "INCP" to fixed(2, "P", "P = (P + 1) modulo 128"),
        "DECP" to fixed(2, "P", "P = (P - 1) modulo 128"),
        "LDM" to fixed(2, "P and internal RAM[P]", "A = internal RAM[P]"),
        "EXAM" to fixed(3, "A, P and internal RAM[P]", "Swap A with internal RAM[P]"),
        "IX" to fixed(6, "X (internal RAM[4..5])", "Increment X modulo 65536; DP = X; Q = 5"),
        "IXL" to fixed(7, "X and bus byte at X + 1", "Increment X modulo 65536; DP = X; A = bus[DP]; Q = 5"),
        "IYS" to fixed(6, "Y (internal RAM[6..7]) and A", "Increment Y modulo 65536; DP = Y; write A to bus[DP]; Q = 7"),
        "PUSH" to fixed(3, "R and A", "Decrement R modulo 256; internal RAM[R] = A"),
        "POP" to fixed(2, "R and internal RAM[R]", "A = internal RAM[R]; increment R modulo 256"),
        "CALL" to fixed(8, "16-bit target and R", "Push the return address, high byte then low byte; R decreases by 2; PC = target"),
        "RTN" to fixed(4, "R and two stack bytes", "Set PC from return bytes (low then high); R increases by 2"),
        "WAIT" to Notes("Immediate byte n", "No instruction-specific register or memory writes", listOf(Timing(6, perImmediate = 1))),
        "LOOP" to Notes(
            "R, stack counter and backward displacement",
            "Decrement internal RAM[R]. If the old counter was nonzero, branch backward from the displacement byte; otherwise advance PC and increment R modulo 128.",
            listOf(Timing(10, "old counter nonzero; branch taken"), Timing(7, "old counter zero; exit and pop counter")),
            zero = "set when the old counter was 1", carry = "set when the old counter was 0",
        ),
        "DECI" to fixed(4, "I (internal RAM[0])", "Decrement I modulo 256; Q = 0",
            zero = "set when old I was 1", carry = "set when old I was 0"),
        "JRNZM" to Notes(
            "Zero flag, backward displacement and R",
            "Copy displacement to internal RAM[(R - 1) modulo 256] without changing R; branch backward from the displacement byte when zero is clear.",
            listOf(Timing(7, "zero clear; branch taken"), Timing(4, "zero set; fall through")),
        ),
        "ANIM" to fixed(4, "P, internal RAM[P] and immediate byte", "internal RAM[P] = internal RAM[P] AND immediate",
            zero = "set if the result is zero"),
        "ORIM" to fixed(4, "P, internal RAM[P] and immediate byte", "internal RAM[P] = internal RAM[P] OR immediate",
            zero = "set if the result is zero"),
        "OUTC" to fixed(2, "Internal RAM[0x5F]", "Q = 0x5F; update control latch, power state and output port C. Bit 1 resets the Core timer counters and dividers."),
    )

    fun contains(mnemonic: String): Boolean = mnemonic in entries

    fun details(mnemonic: String): JsonObject? = entries[mnemonic]?.let { notes ->
        buildJsonObject {
            put("basis", "PGP Core implementation; physical hardware and ROM calling conventions are unverified")
            put("reads", notes.reads)
            put("writes", notes.writes)
            putJsonObject("flags") { put("zero", notes.zero); put("carry", notes.carry) }
            putJsonArray("cycles") {
                notes.timing.forEach { timing ->
                    add(buildJsonObject {
                        put("base", timing.base)
                        timing.whenUsed?.let { put("condition", it) }
                        timing.perImmediate?.let { put("perUnsignedImmediate", it) }
                    })
                }
            }
        }
    }
}
