package com.digihori.pgp.desktop.input

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
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
    fun convertsParenthesesUsingTheAttachedMachineLayout() {
        val pc1245 = FakeSink(Pc1245RomDefinition.MACHINE_ID)
        val pc1251 = FakeSink(Pc1251RomDefinition.MACHINE_ID)
        val input = DesktopKeyboardInput().also { it.attach(pc1245) }

        input.handle(Key.Nine, '('.code, KeyEventType.KeyDown)
        input.handle(Key.Nine, '('.code, KeyEventType.KeyUp)
        input.attach(pc1251)
        input.handle(Key.Nine, '('.code, KeyEventType.KeyDown)
        input.handle(Key.Nine, '('.code, KeyEventType.KeyUp)

        assertEquals(listOf(listOf(PocketKey.SHIFT, PocketKey.NUM_1)), pc1245.sequences)
        assertEquals(listOf(listOf(PocketKey.SHIFT, PocketKey.DOWN)), pc1251.sequences)
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
    fun holdsAndReleasesACharacterWhosePhysicalAndPocketKeysMatch() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyUp)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        assertEquals(listOf("press:A", "release:A", "press:A"), sink.events)
        assertEquals(emptyList(), sink.sequences)
    }

    @Test
    fun holdsNumberKeyUntilItsPhysicalKeyUpEvent() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        input.handle(Key.Seven, '7'.code, KeyEventType.KeyDown)
        input.handle(Key.Seven, '7'.code, KeyEventType.KeyDown)

        assertEquals(listOf("press:NUM_7"), sink.events)

        input.handle(Key.Seven, '7'.code, KeyEventType.KeyUp)

        assertEquals(listOf("press:NUM_7", "release:NUM_7"), sink.events)
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
    fun altArrowKeysRecallHistoryWithoutReachingPocketKeyboard() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        assertTrue(input.handle(Key.DirectionUp, 0, KeyEventType.KeyDown, isAltPressed = true))
        assertTrue(input.handle(Key.DirectionUp, 0, KeyEventType.KeyUp, isAltPressed = true))
        assertTrue(input.handle(Key.DirectionDown, 0, KeyEventType.KeyDown, isAltPressed = true))
        assertTrue(input.handle(Key.DirectionDown, 0, KeyEventType.KeyUp, isAltPressed = true))

        assertEquals(1, sink.previousRecallCount)
        assertEquals(1, sink.nextRecallCount)
        assertEquals(emptyList(), sink.events)
    }

    @Test
    fun changingSinkReleasesOnlyDirectlyHeldKeys() {
        val first = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(first) }
        input.handle(Key.DirectionUp, 0, KeyEventType.KeyDown)
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        input.attach(FakeSink())

        assertEquals(listOf("press:UP", "press:A", "release:UP", "release:A"), first.events)
    }

    @Test
    fun focusLossClearsCharacterRepeatSuppressionAndReleasesDirectKeys() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)
        input.handle(Key.DirectionUp, 0, KeyEventType.KeyDown)

        input.clearActiveInputs()
        input.handle(Key.A, 'a'.code, KeyEventType.KeyDown)

        assertEquals(emptyList(), sink.sequences)
        assertEquals(listOf("press:A", "press:UP", "release:A", "release:UP", "press:A"), sink.events)
    }

    @Test
    fun disabledInputLeavesTextEditingKeysForComposeControls() {
        val sink = FakeSink()
        val input = DesktopKeyboardInput().also { it.attach(sink) }

        input.setEnabled(false)

        assertFalse(input.handle(Key.DirectionLeft, 0, KeyEventType.KeyDown))
        assertFalse(input.handle(Key.Backspace, 0, KeyEventType.KeyDown))
        assertTrue(sink.events.isEmpty())
    }

    private class FakeSink(
        override val machineId: MachineId = Pc1245RomDefinition.MACHINE_ID,
    ) : DesktopKeyInputSink {
        val events = mutableListOf<String>()
        val sequences = mutableListOf<List<PocketKey>>()
        var previousRecallCount = 0
        var nextRecallCount = 0

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

        override fun recallPreviousCommand(): Boolean {
            previousRecallCount++
            return true
        }

        override fun recallNextCommand(): Boolean {
            nextRecallCount++
            return true
        }
    }
}
