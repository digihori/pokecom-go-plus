package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.CpuSnapshot
import com.digihori.pgp.core.api.PhysicalRomLocation

internal data class DebuggerLogpoint(val id: Long, val address: Int, val value: String, val condition: String, val enabled: Boolean)
internal data class DebuggerLogEntry(val sequence: Long, val id: Long, val address: Int, val operand: String,
    val value: Int?, val rom: PhysicalRomLocation?, val failed: Boolean = false)
internal enum class DebuggerLogpointError { ADDRESS, VALUE, CONDITION, MEMORY, DUPLICATE, LIMIT, NOT_FOUND, PAUSED_ONLY }
internal sealed interface DebuggerLogpointResult {
    data class Success(val id: Long) : DebuggerLogpointResult
    data class Failure(val error: DebuggerLogpointError) : DebuggerLogpointResult
}

/** Parsed once; one enabled registration per PC, bounded history, no executable expressions. */
internal class DebuggerLogpoints(private val readable: (Int) -> Boolean) {
    private data class Rule(val definition: DebuggerLogpoint, val value: DebuggerBreakpointCondition, val condition: DebuggerBreakpointCondition?)
    private val rules = linkedMapOf<Long, Rule>()
    private val active = mutableMapOf<Int, Rule>()
    private var nextId = 0L
    private var nextSequence = 0L
    private val history = ArrayDeque<DebuggerLogEntry>()
    var dropped: Long = 0
        private set
    val hasEnabled: Boolean get() = active.isNotEmpty()
    fun registrations(): List<DebuggerLogpoint> = rules.values.map { it.definition }
    fun entries(): List<DebuggerLogEntry> = history.toList()

    fun save(id: Long?, address: Int, value: String, condition: String, enabled: Boolean): DebuggerLogpointResult {
        fun fail(error: DebuggerLogpointError) = DebuggerLogpointResult.Failure(error)
        if (address !in 0..0xffff) return fail(DebuggerLogpointError.ADDRESS)
        if (id != null && id !in rules) return fail(DebuggerLogpointError.NOT_FOUND)
        if (rules.values.any { it.definition.id != id && it.definition.address == address }) return fail(DebuggerLogpointError.DUPLICATE)
        if (id == null && rules.size >= MAX_POINTS) return fail(DebuggerLogpointError.LIMIT)
        if (value.length > 64) return fail(DebuggerLogpointError.VALUE)
        if (condition.length > 128) return fail(DebuggerLogpointError.CONDITION)
        val parsedValue = DebuggerBreakpointCondition.parse("${value.trim()} == 0") ?: return fail(DebuggerLogpointError.VALUE)
        val parsedCondition = if (condition.isBlank()) null else DebuggerBreakpointCondition.parse(condition)
            ?: return fail(DebuggerLogpointError.CONDITION)
        if (listOfNotNull(parsedValue.memoryAddress, parsedCondition?.memoryAddress).any { !readable(it) }) return fail(DebuggerLogpointError.MEMORY)
        val savedId = id ?: nextId++
        val definition = DebuggerLogpoint(savedId, address, value.trim(), condition.trim(), enabled)
        rules[savedId] = Rule(definition, parsedValue, parsedCondition)
        rebuild()
        return DebuggerLogpointResult.Success(savedId)
    }
    fun remove(id: Long): DebuggerLogpointResult {
        if (rules.remove(id) == null) return DebuggerLogpointResult.Failure(DebuggerLogpointError.NOT_FOUND)
        rebuild()
        return DebuggerLogpointResult.Success(id)
    }
    private fun rebuild() { active.clear(); rules.values.filter { it.definition.enabled }.forEach { active[it.definition.address] = it } }
    fun clearHistory() { history.clear(); nextSequence = 0; dropped = 0 }

    fun observe(pc: Int, cpu: () -> CpuSnapshot, memory: (Int) -> Int, rom: () -> PhysicalRomLocation?) {
        val rule = active[pc] ?: return
        val entry = try {
            if (rule.condition?.matches(cpu, memory) == false) return
            DebuggerLogEntry(nextSequence, rule.definition.id, pc, rule.definition.value, rule.value.read(cpu, memory), rom())
        } catch (_: Exception) {
            DebuggerLogEntry(nextSequence, rule.definition.id, pc, rule.definition.value, null, null, failed = true)
        }
        if (history.size == MAX_ENTRIES) { history.removeFirst(); if (dropped < Long.MAX_VALUE) dropped++ }
        history.addLast(entry)
        if (nextSequence < Long.MAX_VALUE) nextSequence++
    }
    companion object { const val MAX_POINTS = 32; const val MAX_ENTRIES = 256 }
}
