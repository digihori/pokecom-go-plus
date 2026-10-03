package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.Key
import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopKeyMapperTest {
    @Test
    fun mapsLettersTopRowDigitsAndNumpadDigits() {
        assertEquals(PocketKey.A, DesktopKeyMapper.map(Key.A))
        assertEquals(PocketKey.Z, DesktopKeyMapper.map(Key.Z))
        assertEquals(PocketKey.NUM_4, DesktopKeyMapper.map(Key.Four))
        assertEquals(PocketKey.NUM_7, DesktopKeyMapper.map(Key.NumPad7))
    }

    @Test
    fun mapsControlAndNavigationKeys() {
        assertEquals(PocketKey.ENTER, DesktopKeyMapper.map(Key.Enter))
        assertNull(DesktopKeyMapper.map(Key.ShiftLeft))
        assertEquals(PocketKey.CLEAR, DesktopKeyMapper.map(Key.Backspace))
        assertEquals(PocketKey.BREAK, DesktopKeyMapper.map(Key.Escape))
        assertEquals(PocketKey.DEF, DesktopKeyMapper.map(Key.F1))
        assertEquals(PocketKey.LEFT, DesktopKeyMapper.map(Key.DirectionLeft))
    }

    @Test
    fun mapsDedicatedOperatorKeysAsDirectFallbacks() {
        assertEquals(PocketKey.PLUS, DesktopKeyMapper.map(Key.Plus))
        assertEquals(PocketKey.MULTIPLY, DesktopKeyMapper.map(Key.Multiply))
        assertEquals(PocketKey.DIVIDE, DesktopKeyMapper.map(Key.Slash))
    }

    @Test
    fun ignoresUnassignedDesktopKeys() {
        assertNull(DesktopKeyMapper.map(Key.F12))
    }
}
