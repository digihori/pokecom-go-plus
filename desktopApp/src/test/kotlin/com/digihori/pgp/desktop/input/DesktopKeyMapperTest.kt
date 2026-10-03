package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.Key
import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopKeyMapperTest {
    @Test
    fun mapsLettersTopRowDigitsAndNumpadDigits() {
        assertEquals(PocketKey.A, DesktopKeyMapper.map(Key.A, 'a'.code))
        assertEquals(PocketKey.Z, DesktopKeyMapper.map(Key.Z, 'Z'.code))
        assertEquals(PocketKey.NUM_4, DesktopKeyMapper.map(Key.Four, '4'.code))
        assertEquals(PocketKey.NUM_7, DesktopKeyMapper.map(Key.NumPad7, '7'.code))
    }

    @Test
    fun mapsControlAndNavigationKeys() {
        assertEquals(PocketKey.ENTER, DesktopKeyMapper.map(Key.Enter))
        assertEquals(PocketKey.SHIFT, DesktopKeyMapper.map(Key.ShiftLeft))
        assertEquals(PocketKey.CLEAR, DesktopKeyMapper.map(Key.Backspace))
        assertEquals(PocketKey.BREAK, DesktopKeyMapper.map(Key.Escape))
        assertEquals(PocketKey.DEF, DesktopKeyMapper.map(Key.F1))
        assertEquals(PocketKey.LEFT, DesktopKeyMapper.map(Key.DirectionLeft))
    }

    @Test
    fun characterTakesPriorityForShiftedOperators() {
        assertEquals(PocketKey.PLUS, DesktopKeyMapper.map(Key.Equals, '+'.code))
        assertEquals(PocketKey.MULTIPLY, DesktopKeyMapper.map(Key.Eight, '*'.code))
        assertEquals(PocketKey.DIVIDE, DesktopKeyMapper.map(Key.Slash, '/'.code))
    }

    @Test
    fun ignoresUnassignedDesktopKeys() {
        assertNull(DesktopKeyMapper.map(Key.F12))
    }
}
