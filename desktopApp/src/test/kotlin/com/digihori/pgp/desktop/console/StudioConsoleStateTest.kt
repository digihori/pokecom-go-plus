package com.digihori.pgp.desktop.console

import java.time.LocalTime
import kotlin.test.*

class StudioConsoleStateTest {
    @Test fun hiddenConsoleKeepsLogsAndRevealDoesNotClearThem() {
        val state = StudioConsoleState(clock = { LocalTime.of(12, 34, 56) })
        state.append("load one.bas")
        assertFalse(state.visible)
        state.append("ROM error", ConsoleLevel.ERROR, reveal = true)
        assertTrue(state.visible)
        assertEquals("[12:34:56] INFO  load one.bas\n[12:34:56] ERROR  ROM error", state.text)
        state.changeVisibility(false)
        state.append("after hide")
        assertFalse(state.visible)
        assertTrue(state.text.contains("load one.bas"))
        state.reveal()
        state.clear()
        assertTrue(state.visible)
        assertEquals("", state.text)
    }

    @Test fun retainsNewestEntriesWithinBothLimits() {
        val state = StudioConsoleState(maxEntries = 2, maxCharacters = 60, clock = { LocalTime.NOON })
        state.append("one")
        state.append("two")
        state.append("three")
        assertEquals(2, state.entries.size)
        assertFalse(state.text.contains("one"))
        assertTrue(state.text.endsWith("three"))
        state.append("x".repeat(200))
        assertTrue(state.text.length <= 60)
        assertEquals(1, state.entries.size)
        repeat(20) { state.append("entry $it\r\nnext\rline") }
        assertTrue(state.text.length <= 60)
        assertFalse(state.text.contains('\r'))
        assertTrue(state.text.contains("entry 19"))
    }
}
