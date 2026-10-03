package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Pc1245KeyboardLayoutTest {
    @Test
    fun containsEveryPc1245LogicalKeyExactlyOnce() {
        val keys = Pc1245KeyboardLayout.rows.flatten().map(Pc1245KeyCap::key)

        assertEquals(PocketKey.entries.toSet(), keys.toSet())
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
}
