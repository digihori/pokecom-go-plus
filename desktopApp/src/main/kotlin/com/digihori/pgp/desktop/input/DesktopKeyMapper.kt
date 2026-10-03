package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.Key
import com.digihori.pgp.core.api.PocketKey

internal object DesktopKeyMapper {
    fun map(key: Key): PocketKey? = KEY_MAP[key]

    private val KEY_MAP: Map<Key, PocketKey> = buildMap {
        put(Key.A, PocketKey.A); put(Key.B, PocketKey.B); put(Key.C, PocketKey.C)
        put(Key.D, PocketKey.D); put(Key.E, PocketKey.E); put(Key.F, PocketKey.F)
        put(Key.G, PocketKey.G); put(Key.H, PocketKey.H); put(Key.I, PocketKey.I)
        put(Key.J, PocketKey.J); put(Key.K, PocketKey.K); put(Key.L, PocketKey.L)
        put(Key.M, PocketKey.M); put(Key.N, PocketKey.N); put(Key.O, PocketKey.O)
        put(Key.P, PocketKey.P); put(Key.Q, PocketKey.Q); put(Key.R, PocketKey.R)
        put(Key.S, PocketKey.S); put(Key.T, PocketKey.T); put(Key.U, PocketKey.U)
        put(Key.V, PocketKey.V); put(Key.W, PocketKey.W); put(Key.X, PocketKey.X)
        put(Key.Y, PocketKey.Y); put(Key.Z, PocketKey.Z)

        val digits = listOf(
            PocketKey.NUM_0, PocketKey.NUM_1, PocketKey.NUM_2, PocketKey.NUM_3, PocketKey.NUM_4,
            PocketKey.NUM_5, PocketKey.NUM_6, PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9,
        )
        val numberKeys = listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
        val numPadKeys = listOf(Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4, Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9)
        digits.indices.forEach { index ->
            put(numberKeys[index], digits[index])
            put(numPadKeys[index], digits[index])
        }

        put(Key.Enter, PocketKey.ENTER); put(Key.NumPadEnter, PocketKey.ENTER)
        put(Key.Spacebar, PocketKey.SPACE)
        put(Key.F1, PocketKey.DEF); put(Key.Escape, PocketKey.BREAK)
        put(Key.Backspace, PocketKey.CLEAR); put(Key.Delete, PocketKey.CLEAR)
        put(Key.DirectionLeft, PocketKey.LEFT); put(Key.DirectionRight, PocketKey.RIGHT)
        put(Key.DirectionUp, PocketKey.UP); put(Key.DirectionDown, PocketKey.DOWN)
        put(Key.Plus, PocketKey.PLUS); put(Key.NumPadAdd, PocketKey.PLUS)
        put(Key.Minus, PocketKey.MINUS); put(Key.NumPadSubtract, PocketKey.MINUS)
        put(Key.Multiply, PocketKey.MULTIPLY); put(Key.NumPadMultiply, PocketKey.MULTIPLY)
        put(Key.Slash, PocketKey.DIVIDE); put(Key.NumPadDivide, PocketKey.DIVIDE)
        put(Key.Period, PocketKey.DOT); put(Key.NumPadDot, PocketKey.DOT)
        put(Key.Equals, PocketKey.EQUALS); put(Key.NumPadEquals, PocketKey.EQUALS)
    }
}
