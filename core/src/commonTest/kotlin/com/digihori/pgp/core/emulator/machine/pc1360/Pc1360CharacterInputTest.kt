package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1360CharacterInputTest {
    @Test
    fun usesTheDomesticPc1360ShiftAssignments() {
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.COMMA), Pc1360CharacterInput.keySequence('<'))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.COLON), Pc1360CharacterInput.keySequence('>'))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.DIVIDE), Pc1360CharacterInput.keySequence('('))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.SEMICOLON), Pc1360CharacterInput.keySequence(')'))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.MULTIPLY), Pc1360CharacterInput.keySequence('¥'))
    }
}
