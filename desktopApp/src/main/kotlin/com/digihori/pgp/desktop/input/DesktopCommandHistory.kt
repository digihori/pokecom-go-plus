package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey

internal data class CommandHistoryRecall(
    val keys: List<PocketKey>,
    val replaceCurrentInput: Boolean,
)

/** Records user-entered RUN-mode key sequences without interpreting BASIC text. */
internal class DesktopCommandHistory(private val maximumEntries: Int = DEFAULT_MAXIMUM_ENTRIES) {
    private val entries = mutableListOf<List<PocketKey>>() // newest first
    private val pending = mutableListOf<PocketKey>()
    private var recalledIndex: Int? = null

    init {
        require(maximumEntries > 0)
    }

    val size: Int get() = entries.size
    val hasPendingInput: Boolean get() = pending.isNotEmpty()

    fun recordUserKey(key: PocketKey) {
        recalledIndex = null
        when (key) {
            PocketKey.ENTER -> commitPending()
            PocketKey.CLEAR, PocketKey.BREAK -> pending.clear()
            else -> pending += key
        }
    }

    fun recordUserSequence(keys: Iterable<PocketKey>) {
        keys.forEach(::recordUserKey)
    }

    fun previous(): CommandHistoryRecall? {
        if (entries.isEmpty()) return null
        val current = recalledIndex
        val next = if (current == null) 0 else (current + 1).coerceAtMost(entries.lastIndex)
        val replace = current != null || pending.isNotEmpty()
        recalledIndex = next
        pending.clear()
        pending += entries[next]
        return CommandHistoryRecall(entries[next], replace)
    }

    fun next(): CommandHistoryRecall? {
        val current = recalledIndex ?: return null
        val replace = true
        if (current == 0) {
            recalledIndex = null
            pending.clear()
            return CommandHistoryRecall(emptyList(), replace)
        }
        val next = current - 1
        recalledIndex = next
        pending.clear()
        pending += entries[next]
        return CommandHistoryRecall(entries[next], replace)
    }

    fun clearPendingInput() {
        pending.clear()
        recalledIndex = null
    }

    internal fun snapshot(): List<List<PocketKey>> = entries.map { it.toList() }

    private fun commitPending() {
        if (pending.isNotEmpty()) {
            val entry = pending.toList()
            entries.remove(entry)
            entries.add(0, entry)
            while (entries.size > maximumEntries) entries.removeLast()
        }
        pending.clear()
        recalledIndex = null
    }

    private companion object {
        const val DEFAULT_MAXIMUM_ENTRIES: Int = 100
    }
}
