package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350CharacterInput

public object Pc1360CharacterInput {
    public fun keySequence(character: Char): List<PocketKey>? = when (character) {
        '<' -> shifted(PocketKey.COMMA)
        '>' -> shifted(PocketKey.COLON)
        '(' -> shifted(PocketKey.DIVIDE)
        ')' -> shifted(PocketKey.SEMICOLON)
        '\\', '¥' -> shifted(PocketKey.MULTIPLY)
        else -> Pc1350CharacterInput.keySequence(character)
    }

    private fun shifted(key: PocketKey): List<PocketKey> = listOf(PocketKey.SHIFT, key)
}
