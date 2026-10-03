package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Pc1245CharacterInputTest {
    @Test
    fun mapsLettersDigitsAndDirectSymbolsToSingleTaps() {
        assertEquals(listOf(PocketKey.A), Pc1245CharacterInput.keySequence('A'))
        assertEquals(listOf(PocketKey.A), Pc1245CharacterInput.keySequence('a'))
        assertEquals(listOf(PocketKey.NUM_7), Pc1245CharacterInput.keySequence('7'))
        assertEquals(listOf(PocketKey.SPACE), Pc1245CharacterInput.keySequence(' '))
        assertEquals(listOf(PocketKey.PLUS), Pc1245CharacterInput.keySequence('+'))
    }

    @Test
    fun mapsPunctuationToPc1245ShiftThenKeySequences() {
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.Q),
            Pc1245CharacterInput.keySequence('!'),
        )
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.NUM_1),
            Pc1245CharacterInput.keySequence('('),
        )
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.MULTIPLY),
            Pc1245CharacterInput.keySequence('<'),
        )
        assertEquals(
            listOf(PocketKey.SHIFT, PocketKey.NUM_6),
            Pc1245CharacterInput.keySequence('¥'),
        )
    }

    @Test
    fun includesEveryShiftLegendRepresentedAsACharacter() {
        val characters = "!\"#$%&?:,;()@\\¥^<>π√"
        characters.forEach { character ->
            val sequence = Pc1245CharacterInput.keySequence(character)
            assertEquals(PocketKey.SHIFT, sequence?.first(), "character=$character")
            assertEquals(2, sequence?.size, "character=$character")
        }
    }

    @Test
    fun rejectsCharactersNotAvailableFromPc1245Keys() {
        assertNull(Pc1245CharacterInput.keySequence('_'))
        assertNull(Pc1245CharacterInput.keySequence('あ'))
    }
}
