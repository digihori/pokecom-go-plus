package com.digihori.pgp.input

import com.digihori.pgp.core.api.PocketKey

/** Keycode-based input: no character conversion, IME injection, or generated Shift sequences. */
class PhysicalKeyInput(
    private val press: (String, PocketKey) -> Unit,
    private val release: (String) -> Unit,
    private val mapKey: (Int) -> PocketKey?,
) {
    private data class Hold(
        val token: Pair<Int, Int>, val key: PocketKey, var remaining: Long,
        var released: Boolean = false, val chord: Boolean = false,
    )
    private val held = mutableMapOf<Pair<Int, Int>, Hold>()
    private val waiting = mutableListOf<Hold>()
    private val blocked = mutableSetOf<Pair<Int, Int>>()
    private var minimumHoldCycles = 0L
    private var minimumGapCycles = 0L
    private var gapRemaining = 0L
    private var downEvents = 0L
    private var upEvents = 0L
    private var deliveredPresses = 0L
    fun diagnosticState(): String =
        "receiving=$receiving down=$downEvents up=$upEvents delivered=$deliveredPresses " +
            "held=${held.size} queued=${waiting.size} blocked=${blocked.size} gap=$gapRemaining"
    private var receiving = false
    private var allowed = emptySet<PocketKey>()

    fun configure(receiving: Boolean, allowed: Set<PocketKey>, minimumHoldCycles: Long = 0,
                  minimumGapCycles: Long = 0) {
        require(minimumHoldCycles >= 0 && minimumGapCycles >= 0)
        if (this.receiving != receiving || this.allowed != allowed ||
            this.minimumHoldCycles != minimumHoldCycles || this.minimumGapCycles != minimumGapCycles) reset()
        this.receiving = receiving
        this.allowed = allowed
        this.minimumHoldCycles = minimumHoldCycles
        this.minimumGapCycles = minimumGapCycles
    }

    fun handle(device: Int, code: Int, down: Boolean, repeat: Int = 0): Boolean {
        val token = device to code
        val key = mapKey(code) ?: return false
        if (down) downEvents++ else upEvents++
        if (!down) {
            blocked.remove(token)
            val queued = waiting.lastOrNull { it.token == token && !it.released }
            val active = queued ?: held[token]?.takeUnless { it.released }
            if (active != null) {
                active.released = true
                if (queued == null && active.remaining == 0L) finish(active)
            }
            pump()
            return active != null || (receiving && key in allowed)
        }
        if (!receiving || key !in allowed) {
            blocked.add(token)
            return false
        }
        // A fresh down proves a new physical press even if focus loss swallowed the previous up.
        // Repeats after a reset must still wait for a release/new press.
        if (repeat == 0) blocked.remove(token)
        if (token in blocked) return true
        if (held[token]?.released == false || waiting.any { it.token == token && !it.released }) return true
        if (repeat > 0) { blocked.add(token); return true }
        val chord = held.values.any { !it.released } || waiting.any { !it.released }
        val hold = Hold(token, key, minimumHoldCycles, chord = chord)
        if (waiting.isNotEmpty() || gapRemaining > 0 || (held.isNotEmpty() && held.values.all { it.released })) {
            waiting += hold
        } else start(hold)
        pump()
        return true
    }

    /** Retain short physical taps until the ROM can scan them; never synthesize text. */
    fun advanceCycles(cycles: Long) {
        require(cycles >= 0)
        if (held.isEmpty()) gapRemaining = (gapRemaining - cycles).coerceAtLeast(0)
        held.values.toList().forEach { hold ->
            hold.remaining = (hold.remaining - cycles).coerceAtLeast(0)
            if (hold.released && hold.remaining == 0L) finish(hold)
        }
        pump()
    }

    private fun start(hold: Hold) {
        deliveredPresses++
        held[hold.token] = hold
        press(owner(hold.token), hold.key)
    }

    private fun finish(hold: Hold) {
        held.remove(hold.token)
        release(owner(hold.token))
        if (held.isEmpty()) gapRemaining = minimumGapCycles
    }

    private fun pump() {
        if (held.isNotEmpty() || gapRemaining > 0 || waiting.isEmpty()) return
        start(waiting.removeAt(0))
        while (waiting.firstOrNull()?.chord == true) start(waiting.removeAt(0))
    }

    fun reset(device: Int? = null) {
        held.values.filter { device == null || it.token.first == device }.toList().forEach { hold ->
            held.remove(hold.token)
            blocked.add(hold.token)
            release(owner(hold.token))
        }
        waiting.filter { device == null || it.token.first == device }.forEach { blocked.add(it.token) }
        waiting.removeAll { device == null || it.token.first == device }
        if (device != null) blocked.removeAll { it.first == device }
        if (held.isEmpty()) gapRemaining = 0
        // Do not start queued keys while releasing for a context change.
    }

    private fun owner(token: Pair<Int, Int>) = "keyboard:${token.first}:${token.second}"
}
