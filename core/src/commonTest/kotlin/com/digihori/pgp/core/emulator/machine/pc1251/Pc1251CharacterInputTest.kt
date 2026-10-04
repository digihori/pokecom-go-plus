package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1251CharacterInputTest {
    @Test
    fun mapsParenthesesToPc1251ArrowLegends() {
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.DOWN),
            Pc1251CharacterInput.keySequence('('),
        )
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.UP),
            Pc1251CharacterInput.keySequence(')'),
        )
    }

    @Test
    fun retainsSharedOldFamilyCharacterMappings() {
        assertEquals(listOf(PocketKey.A), Pc1251CharacterInput.keySequence('a'))
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.Q),
            Pc1251CharacterInput.keySequence('!'),
        )
    }
}
