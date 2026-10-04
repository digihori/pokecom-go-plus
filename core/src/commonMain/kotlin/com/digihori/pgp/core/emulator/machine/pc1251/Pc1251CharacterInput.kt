package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245CharacterInput

/** Converts host text into taps on the PC-1251 keyboard legends. */
public object Pc1251CharacterInput {
    public fun keySequence(character: Char): List<PocketKey>? = when (character) {
        '(' -> listOf(PocketKey.SHIFT, PocketKey.DOWN)
        ')' -> listOf(PocketKey.SHIFT, PocketKey.UP)
        else -> Pc1245CharacterInput.keySequence(character)
    }
}
