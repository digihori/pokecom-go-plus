package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Pc1245KeyboardLayoutTest {
    @Test
    fun containsEveryPc1245LogicalKeyExactlyOnce() {
        val keys = Pc1245KeyboardLayout.rows.flatten().map(PocketKeyCap::key)

        val pc1350Only = setOf(
            PocketKey.MODE, PocketKey.KANA, PocketKey.SMALL, PocketKey.INSERT, PocketKey.DELETE,
            PocketKey.LEFT_PAREN, PocketKey.RIGHT_PAREN, PocketKey.COLON,
            PocketKey.SEMICOLON, PocketKey.COMMA,
        )
        assertEquals(PocketKey.entries.toSet() - pc1350Only, keys.toSet())
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun rowsFitWithoutOverlappingTheFourteenColumnGrid() {
        Pc1245KeyboardLayout.rows.forEach { row ->
            val occupiedColumns = row.flatMap { cap ->
                cap.column until cap.column + cap.columnSpan
            }
            assertTrue(occupiedColumns.all { it in 0 until Pc1245KeyboardLayout.COLUMN_COUNT })
            assertEquals(occupiedColumns.size, occupiedColumns.distinct().size)
        }
    }

    @Test
    fun shiftedExclamationLegendMatchesCharacterInputMapping() {
        val q = Pc1245KeyboardLayout.rows.flatten().single { it.key == PocketKey.Q }
        assertEquals("!", q.shiftedLabel)
    }

    @Test
    fun pc1350RowsFitTheSixteenColumnGridAndContainMachineSpecificKeys() {
        Pc1350KeyboardLayout.rows.forEach { row ->
            val occupiedColumns = row.flatMap { cap -> cap.column until cap.column + cap.columnSpan }
            assertTrue(occupiedColumns.all { it in 0 until Pc1350KeyboardLayout.COLUMN_COUNT })
            assertEquals(occupiedColumns.size, occupiedColumns.distinct().size)
        }
        val keys = Pc1350KeyboardLayout.rows.flatten().map(PocketKeyCap::key).toSet()
        assertTrue(
            keys.containsAll(
                setOf(
                    PocketKey.MODE, PocketKey.KANA, PocketKey.INSERT, PocketKey.DELETE,
                    PocketKey.LEFT_PAREN, PocketKey.RIGHT_PAREN, PocketKey.COLON,
                    PocketKey.SEMICOLON, PocketKey.COMMA,
                ),
            ),
        )
    }

    @Test
    fun pc1360ShowsDomesticShiftLegends() {
        val caps = Pc1360KeyboardLayout.rows.flatten().associateBy(PocketKeyCap::key)
        assertEquals("<", caps.getValue(PocketKey.COMMA).shiftedLabel)
        assertEquals(">", caps.getValue(PocketKey.COLON).shiftedLabel)
        assertEquals("(", caps.getValue(PocketKey.DIVIDE).shiftedLabel)
        assertEquals(")", caps.getValue(PocketKey.SEMICOLON).shiftedLabel)
        assertEquals("¥", caps.getValue(PocketKey.MULTIPLY).shiftedLabel)
        assertEquals("KANA", caps.getValue(PocketKey.KANA).primaryLabel)
        assertEquals("SML", caps.getValue(PocketKey.SMALL).primaryLabel)
        assertTrue(PocketKey.LEFT_PAREN !in caps)
        assertTrue(PocketKey.RIGHT_PAREN !in caps)
        Pc1360KeyboardLayout.rows.forEach { row ->
            val occupied = row.flatMap { it.column until it.column + it.columnSpan }
            assertEquals(occupied.size, occupied.distinct().size)
        }
    }

    @Test
    fun pc1350KanaKeyShowsShiftedSmallLegend() {
        val kana = Pc1350KeyboardLayout.rows.flatten().single { it.key == PocketKey.KANA }

        assertEquals("KANA", kana.primaryLabel)
        assertEquals("SML", kana.shiftedLabel)
    }
}
