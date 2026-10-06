package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245CharacterInput

public object Pc1350CharacterInput {
    public fun keySequence(character: Char): List<PocketKey>? = when (character) {
        '(' -> listOf(PocketKey.LEFT_PAREN)
        ')' -> listOf(PocketKey.RIGHT_PAREN)
        ':' -> listOf(PocketKey.COLON)
        ';' -> listOf(PocketKey.SEMICOLON)
        ',' -> listOf(PocketKey.COMMA)
        'π' -> shifted(PocketKey.I)
        '√' -> shifted(PocketKey.O)
        '@' -> shifted(PocketKey.P)
        '<' -> shifted(PocketKey.LEFT_PAREN)
        '>' -> shifted(PocketKey.RIGHT_PAREN)
        '\\', '¥' -> shifted(PocketKey.DIVIDE)
        '^' -> shifted(PocketKey.MINUS)
        else -> Pc1245CharacterInput.keySequence(character)
    }

    private fun shifted(key: PocketKey): List<PocketKey> = listOf(PocketKey.SHIFT, key)
}
