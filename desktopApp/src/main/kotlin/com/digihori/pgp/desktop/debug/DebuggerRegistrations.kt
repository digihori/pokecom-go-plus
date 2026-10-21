package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.MemoryAccess
import com.digihori.pgp.core.api.MemoryAccessKind

internal data class DebuggerBreakpoint(
    val id: Long, val address: Int, val enabled: Boolean = true,
    val condition: String = "", val hitCondition: String = "", val hitCount: Long = 0,
)
internal enum class DebuggerWatchKind {
    CHANGE, READ, WRITE, READ_WRITE;
    fun matches(kind: MemoryAccessKind): Boolean = this == READ_WRITE ||
        (this == READ && kind == MemoryAccessKind.READ) || (this == WRITE && kind == MemoryAccessKind.WRITE)
}
internal data class DebuggerWatch(val id: Long, val range: IntRange, val kind: DebuggerWatchKind, val enabled: Boolean = true)
internal enum class DebuggerRegistrationError {
    CONDITION, HIT_CONDITION, ADDRESS, RANGE, DUPLICATE_ADDRESS, NOT_FOUND, BREAKPOINT_LIMIT, WATCH_LIMIT, WATCH_BYTES_LIMIT, OBSERVATION_FAILED,
}
internal sealed interface DebuggerRegistrationResult {
    data class Success(val id: Long) : DebuggerRegistrationResult
    data class Failure(val error: DebuggerRegistrationError) : DebuggerRegistrationResult
}

/** Session-local registrations. Copies cross the UI boundary; baselines are never exported. */
internal class DebuggerRegistrationStore(initialId: Long = 0L, private val readBytes: (Int, Int) -> ByteArray) {
    private var nextId = initialId
    val nextRegistrationId: Long get() = nextId
    private val breakpoints = linkedMapOf<Long, DebuggerBreakpoint>()
    private val conditions = mutableMapOf<Long, DebuggerBreakpointCondition>()
    private val hitConditions = mutableMapOf<Long, DebuggerBreakpointHitCondition>()
    private val hitCounts = mutableMapOf<Long, Long>()
    private val watches = linkedMapOf<Long, DebuggerWatch>()
    private val baselines = mutableMapOf<Long, ByteArray>()
    private val activeAddresses = mutableSetOf<Int>()
    val enabledBreakpointAddresses: Set<Int> get() = activeAddresses
    val hasChangeWatch: Boolean get() = watches.values.any { it.enabled && it.kind == DebuggerWatchKind.CHANGE }
    val hasAccessWatch: Boolean get() = watches.values.any { it.enabled && it.kind != DebuggerWatchKind.CHANGE }

    fun breakpoints(): List<DebuggerBreakpoint> = breakpoints.values.sortedBy { it.address }.map { it.copy(hitCount = hitCounts[it.id] ?: 0) }
    fun watches(): List<DebuggerWatch> = watches.values.toList()

    fun saveBreakpoint(id: Long?, address: Int, enabled: Boolean, condition: String? = null, hitCondition: String? = null): DebuggerRegistrationResult {
        if (address !in 0..0xffff) return failure(DebuggerRegistrationError.ADDRESS)
        if (id != null && id !in breakpoints) return failure(DebuggerRegistrationError.NOT_FOUND)
        if (breakpoints.values.any { it.id != id && it.address == address }) return failure(DebuggerRegistrationError.DUPLICATE_ADDRESS)
        if (id == null && breakpoints.size >= MAX_BREAKPOINTS) return failure(DebuggerRegistrationError.BREAKPOINT_LIMIT)
        val old = breakpoints[id]
        val conditionText = (condition ?: old?.condition.orEmpty()).trim()
        val hitText = (hitCondition ?: old?.hitCondition.orEmpty()).trim()
        val parsed = if (conditionText.isEmpty()) null else DebuggerBreakpointCondition.parse(conditionText)
            ?: return failure(DebuggerRegistrationError.CONDITION)
        val parsedHit = if (hitText.isEmpty()) null else DebuggerBreakpointHitCondition.parse(hitText)
            ?: return failure(DebuggerRegistrationError.HIT_CONDITION)
        val savedId = id ?: nextId++
        if (old == null || old.address != address || old.condition != conditionText || old.hitCondition != hitText) hitCounts.remove(savedId)
        if (parsed == null) conditions.remove(savedId) else conditions[savedId] = parsed
        if (parsedHit == null) hitConditions.remove(savedId) else hitConditions[savedId] = parsedHit
        breakpoints[savedId] = DebuggerBreakpoint(savedId, address, enabled, conditionText, hitText)
        activeAddresses.clear()
        activeAddresses.addAll(breakpoints.values.filter { it.enabled }.map { it.address })
        return DebuggerRegistrationResult.Success(savedId)
    }

    fun removeBreakpoint(id: Long): DebuggerRegistrationResult {
        val old = breakpoints.remove(id) ?: return failure(DebuggerRegistrationError.NOT_FOUND)
        activeAddresses.remove(old.address)
        conditions.remove(id); hitConditions.remove(id); hitCounts.remove(id)
        return DebuggerRegistrationResult.Success(id)
    }

    fun resetHitCounts() { hitCounts.clear() }

    /** Called once per enabled PC encounter, before execution, excluding resume's skipped encounter. */
    fun breakpointHit(address: Int, cpu: () -> com.digihori.pgp.core.api.CpuSnapshot): Boolean {
        val entry = breakpoints.values.firstOrNull { it.enabled && it.address == address } ?: return false
        val previous = hitCounts[entry.id] ?: 0
        val count = if (previous == Long.MAX_VALUE) previous else previous + 1
        hitCounts[entry.id] = count
        if (hitConditions[entry.id]?.matches(count) == false) return false
        return conditions[entry.id]?.matches(cpu) { readBytes(it, 1).single().toInt() and 255 } ?: true
    }

    fun saveWatch(id: Long?, start: Int, end: Int, kind: DebuggerWatchKind, enabled: Boolean): DebuggerRegistrationResult {
        if (start !in 0..0xffff || end !in 0..0xffff) return failure(DebuggerRegistrationError.ADDRESS)
        if (end < start) return failure(DebuggerRegistrationError.RANGE)
        if (id != null && id !in watches) return failure(DebuggerRegistrationError.NOT_FOUND)
        if (id == null && watches.size >= MAX_WATCHES) return failure(DebuggerRegistrationError.WATCH_LIMIT)
        if (kind == DebuggerWatchKind.CHANGE) {
            val length = end - start + 1
            val existingBytes = watches.values.filter { it.id != id && it.enabled && it.kind == DebuggerWatchKind.CHANGE }
                .sumOf { it.range.last - it.range.first + 1 }
            if (length > MAX_CHANGE_BYTES || (enabled && existingBytes + length > MAX_CHANGE_BYTES))
                return failure(DebuggerRegistrationError.WATCH_BYTES_LIMIT)
        }
        val baseline = if (enabled && kind == DebuggerWatchKind.CHANGE) {
            try { readBytes(start, end - start + 1) } catch (_: Exception) { return failure(DebuggerRegistrationError.OBSERVATION_FAILED) }
        } else null
        val savedId = id ?: nextId++
        watches[savedId] = DebuggerWatch(savedId, start..end, kind, enabled)
        if (baseline == null) baselines.remove(savedId) else baselines[savedId] = baseline
        return DebuggerRegistrationResult.Success(savedId)
    }

    fun removeWatch(id: Long): DebuggerRegistrationResult {
        if (watches.remove(id) == null) return failure(DebuggerRegistrationError.NOT_FOUND)
        baselines.remove(id)
        return DebuggerRegistrationResult.Success(id)
    }

    fun refreshBaselines() {
        baselines.clear()
        watches.values.filter { it.enabled && it.kind == DebuggerWatchKind.CHANGE }.forEach {
            baselines[it.id] = readBytes(it.range.first, it.range.last - it.range.first + 1)
        }
    }

    fun changes(): List<MemoryValueChange> {
        val changes = linkedMapOf<Int, MemoryValueChange>()
        watches.values.filter { it.enabled && it.kind == DebuggerWatchKind.CHANGE }.forEach { watch ->
            val before = baselines[watch.id]
            val after = readBytes(watch.range.first, watch.range.last - watch.range.first + 1)
            if (before != null) before.indices.forEach { offset ->
                val old = before[offset].toInt() and 255
                val value = after[offset].toInt() and 255
                if (old != value) {
                    val address = watch.range.first + offset
                    changes.putIfAbsent(address, MemoryValueChange(address, old, value))
                }
            }
            baselines[watch.id] = after
        }
        return changes.values.toList()
    }

    fun accesses(accesses: List<MemoryAccess>): List<MemoryAccess> = accesses.filter { access ->
        watches.values.any { it.enabled && access.address in it.range && it.kind.matches(access.kind) }
    }

    private fun failure(error: DebuggerRegistrationError) = DebuggerRegistrationResult.Failure(error)
    companion object {
        const val MAX_BREAKPOINTS = 256
        const val MAX_WATCHES = 64
        const val MAX_CHANGE_BYTES = 4096
    }
}
