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
    fun continuousDisplayDoesNotInsertCharacterColumnGaps() {
        assertEquals(
            150,
            CharacterCellGeometry.visualColumnCount(
                characterColumns = 25,
                characterWidth = 6,
                gapColumns = 0,
            ),
        )
        assertEquals(
            149,
            CharacterCellGeometry.visualColumn(
                dotColumn = 149,
                characterWidth = 6,
                gapColumns = 0,
            ),
        )
    }

    @Test
    fun continuousDisplayDoesNotInsertCharacterRowGaps() {
        assertEquals(
            32,
            CharacterCellGeometry.visualRowCount(
                characterRows = 4,
                characterHeight = 8,
                gapRows = 0,
            ),
        )
        assertEquals(
            31,
            CharacterCellGeometry.visualRow(
                dotRow = 31,
                characterHeight = 8,
                gapRows = 0,
            ),
        )
    }

    @Test
    fun insertsOneBlankRowBetweenFourEightDotCharacterRows() {
        assertEquals(35, CharacterCellGeometry.visualRowCount(characterRows = 4, characterHeight = 8))
        assertEquals(7, CharacterCellGeometry.visualRow(dotRow = 7, characterHeight = 8))
        assertEquals(9, CharacterCellGeometry.visualRow(dotRow = 8, characterHeight = 8))
        assertEquals(34, CharacterCellGeometry.visualRow(dotRow = 31, characterHeight = 8))
    }

    @Test
    fun rejectsInvalidGeometry() {
        assertFailsWith<IllegalArgumentException> {
            CharacterCellGeometry.visualColumnCount(characterColumns = 0, characterWidth = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterCellGeometry.visualColumn(dotColumn = -1, characterWidth = 5)
        }
        assertFailsWith<IllegalArgumentException> {
            CharacterCellGeometry.visualRowCount(characterRows = 0, characterHeight = 8)
        }
    }
}
