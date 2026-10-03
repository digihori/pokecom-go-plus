package com.digihori.pgp.desktop.display

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharacterCellGeometryTest {
    @Test
    fun insertsOneBlankColumnBetweenFiveDotCharacters() {
        assertEquals(0, CharacterCellGeometry.visualColumn(0, characterWidth = 5))
        assertEquals(4, CharacterCellGeometry.visualColumn(4, characterWidth = 5))
        assertEquals(6, CharacterCellGeometry.visualColumn(5, characterWidth = 5))
        assertEquals(10, CharacterCellGeometry.visualColumn(9, characterWidth = 5))
        assertEquals(12, CharacterCellGeometry.visualColumn(10, characterWidth = 5))
    }

    @Test
    fun pc1245DisplayUsesNinetyFiveColumnsIncludingFifteenGaps() {
        assertEquals(
            95,
            CharacterCellGeometry.visualColumnCount(characterColumns = 16, characterWidth = 5),
        )
        assertEquals(94, CharacterCellGeometry.visualColumn(dotColumn = 79, characterWidth = 5))
    }

    @Test
    fun rejectsInvalidGeometry() {
        assertFailsWith<IllegalArgumentException> {
            CharacterCellGeometry.visualColumnCount(characterColumns = 0, characterWidth = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterCellGeometry.visualColumn(dotColumn = -1, characterWidth = 5)
        }
    }
}
