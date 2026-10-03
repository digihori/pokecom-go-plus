package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.PocketKey

/**
 * Converts a character requested by a host into taps on the PC-1245 keyboard.
 *
 * SHIFT is a latched PC-1245 key, not a host modifier. A shifted character is
 * therefore represented as two consecutive taps rather than simultaneous keys.
 */
public object Pc1245CharacterInput {
    public fun keySequence(character: Char): List<PocketKey>? {
        val normalized = character.uppercaseChar()
        DIRECT_KEYS[normalized]?.let { return listOf(it) }
        SHIFTED_KEYS[character]?.let { return listOf(PocketKey.SHIFT, it) }
        return null
    }

    private val DIRECT_KEYS: Map<Char, PocketKey> = buildMap {
        val letterKeys = listOf(
            PocketKey.A, PocketKey.B, PocketKey.C, PocketKey.D, PocketKey.E, PocketKey.F,
            PocketKey.G, PocketKey.H, PocketKey.I, PocketKey.J, PocketKey.K, PocketKey.L,
            PocketKey.M, PocketKey.N, PocketKey.O, PocketKey.P, PocketKey.Q, PocketKey.R,
            PocketKey.S, PocketKey.T, PocketKey.U, PocketKey.V, PocketKey.W, PocketKey.X,
            PocketKey.Y, PocketKey.Z,
        )
        letterKeys.forEachIndexed { index, key -> put('A' + index, key) }

        val digitKeys = listOf(
            PocketKey.NUM_0, PocketKey.NUM_1, PocketKey.NUM_2, PocketKey.NUM_3, PocketKey.NUM_4,
            PocketKey.NUM_5, PocketKey.NUM_6, PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9,
        )
        digitKeys.forEachIndexed { index, key -> put('0' + index, key) }

        put(' ', PocketKey.SPACE)
        put('.', PocketKey.DOT)
        put('+', PocketKey.PLUS)
        put('-', PocketKey.MINUS)
        put('*', PocketKey.MULTIPLY)
        put('/', PocketKey.DIVIDE)
        put('=', PocketKey.EQUALS)
    }

    private val SHIFTED_KEYS: Map<Char, PocketKey> = mapOf(
        '!' to PocketKey.Q,
        '"' to PocketKey.W,
        '#' to PocketKey.E,
        '$' to PocketKey.R,
        '%' to PocketKey.T,
        '&' to PocketKey.Y,
        '?' to PocketKey.U,
        ':' to PocketKey.I,
        ',' to PocketKey.O,
        ';' to PocketKey.P,
        '(' to PocketKey.NUM_1,
        ')' to PocketKey.NUM_2,
        '@' to PocketKey.NUM_3,
        '\\' to PocketKey.NUM_6,
        '¥' to PocketKey.NUM_6,
        '^' to PocketKey.DIVIDE,
        '<' to PocketKey.MULTIPLY,
        '>' to PocketKey.MINUS,
        'π' to PocketKey.NUM_0,
        '√' to PocketKey.DOT,
    )
}
