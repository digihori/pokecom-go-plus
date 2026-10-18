package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.*

class Pc1360kKeyboardLayoutTest {
    @Test
    fun kLayoutAddsFiveKeysWithoutOverlappingExistingCaps() {
        val standard = Pc1360KeyboardLayout.rows.flatten()
        val extended = Pc1360kKeyboardLayout.rows.flatten()
        assertEquals(setOf(PocketKey.KANJI, PocketKey.KANJI_FIX, PocketKey.KANJI_CONVERT, PocketKey.HIRA_KATA, PocketKey.YJ),
            extended.map { it.key }.toSet() - standard.map { it.key }.toSet())
        assertTrue(extended.containsAll(standard))
        Pc1360kKeyboardLayout.rows.forEach { row ->
            val occupied = row.flatMap { it.column until it.column + it.columnSpan }
            assertEquals(occupied.size, occupied.toSet().size)
            assertTrue(occupied.all { it in 0 until Pc1360kKeyboardLayout.COLUMN_COUNT })
        }
    }
}
