package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1350CharacterInputTest {
    @Test
    fun mapsHostAmpersandToLatchedShiftAndY() {
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.Y), Pc1350CharacterInput.keySequence('&'))
    }

    @Test
    fun usesDedicatedPunctuationKeysAndPc1350ShiftLegends() {
        assertEquals(listOf(PocketKey.COLON), Pc1350CharacterInput.keySequence(':'))
        assertEquals(listOf(PocketKey.LEFT_PAREN), Pc1350CharacterInput.keySequence('('))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.I), Pc1350CharacterInput.keySequence('π'))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.P), Pc1350CharacterInput.keySequence('@'))
    }
}
