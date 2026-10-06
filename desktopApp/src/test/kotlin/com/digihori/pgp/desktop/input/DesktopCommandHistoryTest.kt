package com.digihori.pgp.desktop.input

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopCommandHistoryTest {
    @Test
    fun commitsKeysOnEnterAndRecallsWithoutEnter() {
        val history = DesktopCommandHistory()
        history.recordUserSequence(listOf(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER))

        val recalled = requireNotNull(history.previous())

        assertEquals(listOf(PocketKey.R, PocketKey.U, PocketKey.N), recalled.keys)
        assertFalse(recalled.replaceCurrentInput)
        assertEquals(1, history.size)
    }

    @Test
    fun navigatesOlderAndNewerEntriesAndClearsAtNewestEnd() {
        val history = DesktopCommandHistory()
        history.recordUserSequence(listOf(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER))
        history.recordUserSequence(listOf(PocketKey.C, PocketKey.A, PocketKey.L, PocketKey.L, PocketKey.ENTER))

        assertEquals(listOf(PocketKey.C, PocketKey.A, PocketKey.L, PocketKey.L), history.previous()?.keys)
        val older = requireNotNull(history.previous())
        assertEquals(listOf(PocketKey.R, PocketKey.U, PocketKey.N), older.keys)
        assertTrue(older.replaceCurrentInput)
        assertEquals(listOf(PocketKey.C, PocketKey.A, PocketKey.L, PocketKey.L), history.next()?.keys)
        val blank = requireNotNull(history.next())
        assertTrue(blank.replaceCurrentInput)
        assertTrue(blank.keys.isEmpty())
        assertNull(history.next())
    }

    @Test
    fun replacesUncommittedUserInputWhenRecalling() {
        val history = DesktopCommandHistory()
        history.recordUserSequence(listOf(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER))
        history.recordUserKey(PocketKey.C)

        val recalled = requireNotNull(history.previous())
        assertTrue(recalled.replaceCurrentInput)
        assertEquals(listOf(PocketKey.R, PocketKey.U, PocketKey.N), recalled.keys)
    }

    @Test
    fun clearMakesInputEmptyAndConsecutiveDuplicatesAreCollapsed() {
        val history = DesktopCommandHistory()
        repeat(2) {
            history.recordUserSequence(listOf(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER))
        }
        history.recordUserSequence(listOf(PocketKey.C, PocketKey.A, PocketKey.CLEAR))

        assertEquals(1, history.size)
        assertFalse(history.hasPendingInput)
        assertEquals(listOf(PocketKey.R, PocketKey.U, PocketKey.N), history.previous()?.keys)
    }
}
