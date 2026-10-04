package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomInputResult
import com.digihori.pgp.core.emulator.machine.pc1245.RomBasicInputCompiler
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextDocument

/** Compiles BASIC text into taps accepted by the PC-1251 ROM editor. */
public object Pc1251RomBasicInput {
    public fun compile(document: BasicTextDocument): Pc1245RomInputResult = RomBasicInputCompiler.compile(
        document = document,
        characterSequence = Pc1251CharacterInput::keySequence,
        specialSequence = { symbol ->
            when (symbol) {
                BasicSpecialSymbol.PI -> listOf(PocketKey.SHIFT, PocketKey.NUM_0)
                BasicSpecialSymbol.SQUARE_ROOT -> listOf(PocketKey.SHIFT, PocketKey.DOT)
                BasicSpecialSymbol.EXPONENT -> listOf(PocketKey.SHIFT, PocketKey.PLUS)
                BasicSpecialSymbol.BLOCK -> null
            }
        },
    )
}
