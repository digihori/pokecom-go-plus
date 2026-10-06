package com.digihori.pgp.core.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DisplaySnapshotTest {
    @Test
    fun representsTwentyFiveByFourCharacterDisplayAsOneLogicalDotPlane() {
        val dots = ByteArray(150 * 32)
        dots[31 * 150 + 149] = 1
        val snapshot = DisplaySnapshot(
            characterColumns = 25,
            characterWidth = 6,
            dotRows = 32,
            characterRows = 4,
            symbols = emptyList(),
            enabled = true,
            revision = 1,
            dots = dots,
        )

        assertEquals(150, snapshot.dotColumns)
        assertEquals(8, snapshot.characterHeight)
        assertFalse(snapshot.isDotOn(0, 0))
        assertTrue(snapshot.isDotOn(149, 31))
    }

    @Test
    fun rejectsDotRowsThatCannotBeSplitIntoCharacterRows() {
        assertFailsWith<IllegalArgumentException> {
            DisplaySnapshot(25, 6, 31, 4, emptyList(), true, 0, ByteArray(25 * 6 * 31))
        }
    }
}
