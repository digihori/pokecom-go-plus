package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Pc1251KeyboardLayoutTest {
    @Test
    fun displaysParenthesesOnShiftedArrowKeys() {
        val caps = Pc1251KeyboardLayout.rows.flatten().associateBy(PocketKeyCap::key)

        assertEquals("(", caps.getValue(PocketKey.DOWN).shiftedLabel)
        assertEquals(")", caps.getValue(PocketKey.UP).shiftedLabel)
        assertNull(caps.getValue(PocketKey.NUM_1).shiftedLabel)
        assertNull(caps.getValue(PocketKey.NUM_2).shiftedLabel)
    }

    @Test
    fun doesNotClaimFixedBasicKeywordAssignments() {
        Pc1251KeyboardLayout.rows.flatten().forEach { cap ->
            assertNull(cap.basicLabel, "key=${cap.key}")
        }
    }
}
