package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245CharacterInput
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.emulator.machine.pc1245.RomBasicInputCompiler
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextDocument

public object Pc1401CharacterInput {
    public fun keySequence(character: Char): List<PocketKey>? = when (character) {
        '(' -> listOf(PocketKey.LEFT_PAREN)
        ')' -> listOf(PocketKey.RIGHT_PAREN)
        ',' -> listOf(PocketKey.COMMA)
        ':' -> listOf(PocketKey.SHIFT, PocketKey.O)
        '@' -> listOf(PocketKey.SHIFT, PocketKey.I)
        '^' -> listOf(PocketKey.SHIFT, PocketKey.PLUS)
        'π' -> listOf(PocketKey.SHIFT, PocketKey.EXPONENT)
        '√' -> listOf(PocketKey.ROOT)
        '\\', '¥' -> null
        else -> Pc1245CharacterInput.keySequence(character)
    }
}

public object Pc1401RomBasicInput {
    public fun compile(document: BasicTextDocument): Pc1245RomInputResult = RomBasicInputCompiler.compile(
        document, Pc1401CharacterInput::keySequence,
        { symbol -> when (symbol) {
            BasicSpecialSymbol.PI -> listOf(PocketKey.SHIFT, PocketKey.EXPONENT)
            BasicSpecialSymbol.SQUARE_ROOT -> listOf(PocketKey.ROOT)
            BasicSpecialSymbol.EXPONENT -> listOf(PocketKey.EXPONENT)
            BasicSpecialSymbol.BLOCK -> null
        } },
    )
}
