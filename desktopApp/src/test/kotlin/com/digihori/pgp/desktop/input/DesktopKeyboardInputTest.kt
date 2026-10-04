package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopKeyboardInputTest {
    @Test
    fun convertsHostCharacterToPc1245KeySequence() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        assertTrue(input.handle(Key.One, '!'.code, KeyEventType.KeyDown))
        assertTrue(input.handle(Key.One, '!'.code, KeyEventType.KeyUp))

        assertEquals(listOf(listOf(PocketKey.SHIFT, PocketKey.Q)), sink.sequences)
        assertEquals(emptyList(), sink.events)
    }

    @Test
    fun doesNotForwardHostShiftAsPc1245Shift() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        assertFalse(input.handle(Key.ShiftLeft, 0, KeyEventType.KeyDown))
        assertFalse(input.handle(Key.ShiftLeft, 0, KeyEventType.KeyUp))

        assertEquals(emptyList(), sink.events)
        assertEquals(emptyList(), sink.sequences)
    }

    @Test
    fun holdsAndReleasesNonCharacterControlKeysDirectly() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        assertTrue(input.handle(Key.DirectionLeft, 0, KeyEventType.KeyDown))
        assertTrue(input.handle(Key.DirectionLeft, 0, KeyEventType.KeyUp))

        assertEquals(listOf("press:LEFT", "release:LEFT"), sink.events)
    }

    @Test
    fun suppressesAutoRepeatForACharacterUntilKeyUp() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyUp)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        assertEquals(listOf(listOf(PocketKey.A), listOf(PocketKey.A)), sink.sequences)
    }

    @Test
    fun leavesOperatingSystemShortcutsUnhandled() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        assertFalse(
            input.handle(
                key = Key.Q,
                utf16CodePoint = 'q'.code,
                type = KeyEventType.KeyDown,
                isMetaPressed = true,
            ),
        )
        assertFalse(
            input.handle(
                key = Key.C,
                utf16CodePoint = 'c'.code,
                type = KeyEventType.KeyDown,
                isCtrlPressed = true,
            ),
        )

        assertEquals(emptyList(), sink.sequences)
    }

    @Test
    fun changingSinkReleasesOnlyDirectlyHeldKeys() {
        val first = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(first) }
        input.handle(Key.DirectionUp, 0, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        input.attach(FakeSink())

        assertEquals(listOf("press:UP", "release:UP"), first.events)
    }

    @Test
    fun focusLossClearsCharacterRepeatSuppressionAndReleasesDirectKeys() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.DirectionUp, 0, KeyEventType.KeyDown)

        input.clearActiveInputs()
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        assertEquals(listOf(listOf(PocketKey.A), listOf(PocketKey.A)), sink.sequences)
        assertEquals(listOf("press:UP", "release:UP"), sink.events)
    }

    private class FakeSink : DesktopKeyInputSink {
        val events = mutableListOf<String>()
        val sequences = mutableListOf<List<PocketKey>>()

        override fun pressKey(key: PocketKey): InputResult {
            events += "press:$key"
            return InputResult.Accepted
        }

        override fun releaseKey(key: PocketKey): InputResult {
            events += "release:$key"
            return InputResult.Accepted
        }

        override fun enqueueKeySequence(keys: Iterable<PocketKey>) {
            sequences += keys.toList()
        }
    }
}
