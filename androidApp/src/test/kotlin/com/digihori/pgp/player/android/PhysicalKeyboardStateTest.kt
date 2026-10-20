package com.digihori.pgp.player.android

import android.view.KeyEvent
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.input.HostKeyState
import kotlin.test.*

class PhysicalKeyboardStateTest {
    @Test fun pc1470uEditingKeysDoNotFallThroughToClearAndReconfigureReleasesHeldKeys() {
        assertEquals(PocketKey.BACKSPACE, physicalPocketKey(KeyEvent.KEYCODE_DEL, pc1470u = true))
        assertEquals(PocketKey.DELETE, physicalPocketKey(KeyEvent.KEYCODE_FORWARD_DEL, pc1470u = true))
        assertEquals(PocketKey.INSERT, physicalPocketKey(KeyEvent.KEYCODE_INSERT, pc1470u = true))
        assertEquals(PocketKey.CLEAR, physicalPocketKey(KeyEvent.KEYCODE_CLEAR, pc1470u = true))
        assertEquals(PocketKey.SHIFT, physicalPocketKey(KeyEvent.KEYCODE_F8, pc14xx = true, pc1470u = true))
        val events = mutableListOf<String>()
        val input = PhysicalKeyboardState({ _, key -> events += "down:$key" }, { events += "up:$it" })
        input.configure(true, setOf(PocketKey.BACKSPACE, PocketKey.CLEAR), pc14xx = true, pc1470u = true)
        input.handle(1, KeyEvent.KEYCODE_DEL, true)
        input.configure(true, setOf(PocketKey.BACKSPACE, PocketKey.CLEAR), pc14xx = true)
        assertEquals(2, events.size); assertEquals("down:BACKSPACE", events.first())
        input.handle(1, KeyEvent.KEYCODE_DEL, false)
        assertEquals(2, events.size)
    }
    @org.junit.Test fun pc14xxRetainsCalBasicAndAddsKanaControls() {
        val expected = mapOf(KeyEvent.KEYCODE_F2 to PocketKey.CAL, KeyEvent.KEYCODE_F3 to PocketKey.BASIC,
            KeyEvent.KEYCODE_F4 to PocketKey.KANA, KeyEvent.KEYCODE_F5 to PocketKey.SMALL,
            KeyEvent.KEYCODE_F6 to PocketKey.DAKUTEN, KeyEvent.KEYCODE_F7 to PocketKey.LONG_VOWEL,
            KeyEvent.KEYCODE_F8 to PocketKey.LOCK, KeyEvent.KEYCODE_DEL to PocketKey.CLEAR,
            KeyEvent.KEYCODE_SHIFT_LEFT to PocketKey.SHIFT, KeyEvent.KEYCODE_SHIFT_RIGHT to PocketKey.SHIFT)
        expected.forEach { (code, key) -> assertEquals(key, physicalPocketKey(code, pc14xx = true)) }
        assertEquals(null, physicalPocketKey(KeyEvent.KEYCODE_F4))
        val events = mutableListOf<String>()
        val input = PhysicalKeyboardState({ _, key -> events += "down:$key" }, { events += "up:$it" })
        input.configure(true, expected.values.toSet(), pc14xx = true)
        input.handle(1, KeyEvent.KEYCODE_F8, true)
        input.configure(true, expected.values.toSet())
        assertEquals(2, events.size)
        assertEquals("down:LOCK", events.first())
        input.handle(1, KeyEvent.KEYCODE_F8, false)
        assertEquals(2, events.size)
    }
    private val allowed = setOf(PocketKey.A, PocketKey.SHIFT, PocketKey.DEF, PocketKey.NUM_7, PocketKey.ENTER)

    @Test fun directMappingDoesNotConvertShiftedCharacters() {
        assertEquals(PocketKey.SHIFT, physicalPocketKey(KeyEvent.KEYCODE_SHIFT_LEFT))
        assertEquals(PocketKey.DEF, physicalPocketKey(KeyEvent.KEYCODE_CTRL_LEFT))
        assertEquals(PocketKey.NUM_7, physicalPocketKey(KeyEvent.KEYCODE_7))
        assertEquals(PocketKey.ENTER, physicalPocketKey(KeyEvent.KEYCODE_NUMPAD_ENTER))
        assertEquals(PocketKey.BREAK, physicalPocketKey(KeyEvent.KEYCODE_ESCAPE))
        assertNull(physicalPocketKey(KeyEvent.KEYCODE_HOME))
        assertEquals(PocketKey.CAL, physicalPocketKey(KeyEvent.KEYCODE_F2))
        assertEquals(PocketKey.BASIC, physicalPocketKey(KeyEvent.KEYCODE_F3))
        assertEquals(PocketKey.LEFT_PAREN, physicalPocketKey(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN))
        assertEquals(PocketKey.RIGHT_PAREN, physicalPocketKey(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN))
    }

    @Test fun pc1401QuickModeTapsUseExecutedCyclesAndLeaveAReleaseGap() {
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(
            com.digihori.pgp.core.rom.MachineId("pc-1401"))
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, setOf(PocketKey.CAL, PocketKey.BASIC),
            definition.automaticKeyHoldCycles, definition.automaticKeyGapCycles)
        keyboard.handle(1, KeyEvent.KEYCODE_F3, true)
        keyboard.handle(1, KeyEvent.KEYCODE_F3, false)
        keyboard.handle(1, KeyEvent.KEYCODE_F2, true)
        keyboard.handle(1, KeyEvent.KEYCODE_F2, false)
        keyboard.advanceCycles(39_999)
        assertEquals(listOf("BASIC"), events)
        keyboard.advanceCycles(1)
        assertEquals(listOf("BASIC", "up"), events)
        keyboard.advanceCycles(19_999)
        assertEquals(2, events.size)
        keyboard.advanceCycles(1)
        assertEquals(listOf("BASIC", "up", "CAL"), events)
        keyboard.advanceCycles(40_000)
        assertEquals(listOf("BASIC", "up", "CAL", "up"), events)
    }

    @Test fun pc1401PauseAndMachineChangeDiscardPendingInput() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed, 40_000, 20_000)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.configure(false, allowed, 40_000, 20_000)
        keyboard.advanceCycles(200_000)
        assertEquals(listOf("SHIFT", "up"), events)
        keyboard.configure(true, allowed, 11_520, 19_200)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.advanceCycles(11_520)
        assertEquals(listOf("SHIFT", "up", "A", "up"), events)
    }

    @Test fun simultaneousHoldsAndRepeatAreNotExtraPresses() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += "down:$key" }, { events += "up" })
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true, 2)
        assertEquals(listOf("down:SHIFT", "down:A"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        assertEquals(4, events.size)
    }

    @Test fun modalFocusLossRequiresReleaseBeforeRepressAndFiltersMachineKeys() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "release" })
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.configure(false, allowed)
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true, 1)
        assertEquals(listOf("A", "release"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        assertEquals(listOf("A", "release", "A"), events)
        assertFalse(keyboard.handle(1, KeyEvent.KEYCODE_SEMICOLON, true))
        keyboard.configure(false, allowed)
        assertFalse(keyboard.handle(2, KeyEvent.KEYCODE_A, true))
        keyboard.configure(true, allowed)
        keyboard.handle(2, KeyEvent.KEYCODE_A, true, 1)
        assertEquals(4, events.size) // only the release of device 1 was added
    }

    @Test fun inputOwnersSurviveOtherKeyboardOrTouchReleaseAndDisconnect() {
        val presses = mutableListOf<PocketKey>()
        val releases = mutableListOf<PocketKey>()
        val keys = HostKeyState({ presses += it; InputResult.Accepted }, { releases += it; InputResult.Accepted })
        val keyboard = PhysicalKeyboardState({ owner, key -> keys.press(owner, key) }, { keys.release(it) })
        keyboard.configure(true, allowed)
        keys.press("screen:A", PocketKey.A)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.handle(2, KeyEvent.KEYCODE_A, true)
        keys.release("screen:A")
        keyboard.reset(1)
        assertTrue(keys.isHeld(PocketKey.A))
        assertTrue(releases.isEmpty())
        keyboard.reset(2)
        assertFalse(keys.isHeld(PocketKey.A))
        assertEquals(listOf(PocketKey.A), presses)
        assertEquals(listOf(PocketKey.A), releases)
    }
    @Test fun shortShiftIsHeldForExecutedCyclesAndResetCancelsPendingRelease() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed, minimumHoldCycles = 11520)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        keyboard.advanceCycles(11519)
        assertEquals(listOf("SHIFT"), events)
        keyboard.advanceCycles(1)
        assertEquals(listOf("SHIFT", "up"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        keyboard.configure(false, allowed, minimumHoldCycles = 11520)
        keyboard.advanceCycles(11520)
        assertEquals(listOf("SHIFT", "up", "SHIFT", "up"), events)
    }

    @Test fun longShiftIsReleasedImmediatelyAndBothShiftKeysHaveSeparateOwners() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ owner, _ -> events += owner }, { events += "up:$it" })
        keyboard.configure(true, allowed, minimumHoldCycles = 11520)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_RIGHT, true)
        keyboard.advanceCycles(20000)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        assertEquals(3, events.size)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_RIGHT, false)
        assertEquals(4, events.size)
    }

    @Test fun pauseResumeAcceptsFreshKeysAndDoesNotReplayPausedInput() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.configure(false, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.handle(1, KeyEvent.KEYCODE_NUMPAD_ENTER, true)
        keyboard.handle(1, KeyEvent.KEYCODE_NUMPAD_ENTER, false)
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        assertEquals(listOf("A", "up", "A", "up"), events)
    }

    @Test fun freshDownRecoversFromAKeyUpLostOutsideTheWindow() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.configure(false, allowed)
        // No key-up is delivered while the app is unfocused.
        keyboard.configure(true, allowed)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true, 2)
        assertEquals(listOf("A", "up"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true, 0)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        assertEquals(listOf("A", "up", "A", "up"), events)
    }

    @Test fun fastSequentialTapsAreDeliveredInOrderWithScanAndReleaseGaps() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed, minimumHoldCycles = 10, minimumGapCycles = 5)
        for (key in listOf(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_ENTER)) {
            keyboard.handle(1, key, true)
            keyboard.handle(1, key, false)
        }
        assertEquals(listOf("A"), events)
        keyboard.advanceCycles(9)
        assertEquals(listOf("A"), events)
        keyboard.advanceCycles(1)
        keyboard.advanceCycles(4)
        assertEquals(listOf("A", "up"), events)
        keyboard.advanceCycles(1)
        keyboard.advanceCycles(10)
        keyboard.advanceCycles(5)
        keyboard.advanceCycles(10)
        assertEquals(listOf("A", "up", "NUM_7", "up", "ENTER", "up"), events)
    }

    @Test fun queuedChordRemainsSimultaneousAndResetDiscardsPendingTaps() {
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed, minimumHoldCycles = 10, minimumGapCycles = 5)
        keyboard.handle(1, KeyEvent.KEYCODE_ENTER, true)
        keyboard.handle(1, KeyEvent.KEYCODE_ENTER, false)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keyboard.handle(1, KeyEvent.KEYCODE_A, true)
        keyboard.advanceCycles(10)
        keyboard.advanceCycles(5)
        assertEquals(listOf("ENTER", "up", "SHIFT", "A"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        keyboard.handle(1, KeyEvent.KEYCODE_A, false)
        keyboard.handle(1, KeyEvent.KEYCODE_7, true)
        keyboard.handle(1, KeyEvent.KEYCODE_7, false)
        keyboard.configure(false, allowed, minimumHoldCycles = 10, minimumGapCycles = 5)
        keyboard.advanceCycles(100)
        assertFalse("NUM_7" in events)
    }

    @Test fun pc1360kFunctionKeysDispatchKanjiControlsAndSwitchingReleasesThem() {
        val mapping = listOf(KeyEvent.KEYCODE_F5 to PocketKey.KANJI, KeyEvent.KEYCODE_F6 to PocketKey.HIRA_KATA,
            KeyEvent.KEYCODE_F7 to PocketKey.KANJI_FIX, KeyEvent.KEYCODE_F8 to PocketKey.KANJI_CONVERT,
            KeyEvent.KEYCODE_F9 to PocketKey.YJ)
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        val allowed = mapping.map { it.second }.toSet()
        keyboard.configure(true, allowed, pc1360k = true)
        mapping.forEach { (code, key) ->
            assertEquals(key, physicalPocketKey(code, pc1360k = true))
            keyboard.handle(1, code, true); keyboard.handle(1, code, false)
        }
        assertEquals(mapping.flatMap { listOf(it.second.name, "up") }, events)
        keyboard.handle(1, KeyEvent.KEYCODE_F5, true)
        keyboard.configure(true, emptySet(), pc1360 = true)
        assertEquals(listOf("KANJI", "up"), events.takeLast(2))
        assertNull(physicalPocketKey(KeyEvent.KEYCODE_F5, pc1360 = true))
    }

    @Test fun pc1360ModeKanaSmallUseTimedInputAndReleaseOnBackground() {
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(com.digihori.pgp.core.rom.MachineId("pc-1360"))
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        val allowed = setOf(PocketKey.MODE, PocketKey.KANA, PocketKey.SMALL)
        keyboard.configure(true, allowed, definition.automaticKeyHoldCycles, definition.automaticKeyGapCycles, pc1360 = true)
        for ((code, key) in listOf(KeyEvent.KEYCODE_F2 to PocketKey.MODE, KeyEvent.KEYCODE_F3 to PocketKey.KANA,
            KeyEvent.KEYCODE_F4 to PocketKey.SMALL)) {
            assertEquals(key, physicalPocketKey(code, pc1360 = true))
            keyboard.handle(1, code, true); keyboard.handle(1, code, false)
            keyboard.advanceCycles(definition.automaticKeyHoldCycles)
            keyboard.advanceCycles(definition.automaticKeyGapCycles)
        }
        assertEquals(listOf("MODE", "up", "KANA", "up", "SMALL", "up"), events)
        keyboard.handle(1, KeyEvent.KEYCODE_F4, true)
        keyboard.configure(false, allowed, pc1360 = true)
        assertEquals(listOf("SMALL", "up"), events.takeLast(2))
    }

    @Test fun pc1350MappingAndModeTapUseMachineTiming() {
        assertEquals(PocketKey.MODE, physicalPocketKey(KeyEvent.KEYCODE_F2, true))
        assertEquals(PocketKey.KANA, physicalPocketKey(KeyEvent.KEYCODE_F3, true))
        assertEquals(PocketKey.INSERT, physicalPocketKey(KeyEvent.KEYCODE_INSERT, true))
        assertEquals(PocketKey.DELETE, physicalPocketKey(KeyEvent.KEYCODE_FORWARD_DEL, true))
        assertEquals(PocketKey.CLEAR, physicalPocketKey(KeyEvent.KEYCODE_DEL, true))
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(
            com.digihori.pgp.core.rom.MachineId("pc-1350"))
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, setOf(PocketKey.MODE, PocketKey.KANA),
            definition.automaticKeyHoldCycles, definition.automaticKeyGapCycles, pc1350 = true)
        keyboard.handle(1, KeyEvent.KEYCODE_F2, true)
        keyboard.handle(1, KeyEvent.KEYCODE_F2, false)
        keyboard.handle(1, KeyEvent.KEYCODE_F3, true)
        keyboard.handle(1, KeyEvent.KEYCODE_F3, false)
        keyboard.advanceCycles(15_359)
        assertEquals(listOf("MODE"), events)
        keyboard.advanceCycles(1)
        keyboard.advanceCycles(25_599)
        assertEquals(listOf("MODE", "up"), events)
        keyboard.advanceCycles(1)
        assertEquals(listOf("MODE", "up", "KANA"), events)
        keyboard.configure(false, setOf(PocketKey.MODE, PocketKey.KANA), pc1350 = true)
        keyboard.advanceCycles(100_000)
        assertEquals(listOf("MODE", "up", "KANA", "up"), events)
    }

    @Test fun pc1350TouchShiftOwnersAndPhysicalShiftReleaseIndependently() {
        val releases = mutableListOf<PocketKey>()
        val keys = HostKeyState({ InputResult.Accepted }, { releases += it; InputResult.Accepted })
        val keyboard = PhysicalKeyboardState({ owner, key -> keys.press(owner, key) }, { keys.release(it) })
        keyboard.configure(true, setOf(PocketKey.SHIFT), pc1350 = true)
        keys.press("screen:SHIFT_LEFT", PocketKey.SHIFT)
        keys.press("screen:SHIFT_RIGHT", PocketKey.SHIFT)
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, true)
        keys.release("screen:SHIFT_LEFT")
        assertTrue(keys.isHeld(PocketKey.SHIFT))
        keys.release("screen:SHIFT_RIGHT")
        assertTrue(keys.isHeld(PocketKey.SHIFT))
        keyboard.handle(1, KeyEvent.KEYCODE_SHIFT_LEFT, false)
        assertEquals(listOf(PocketKey.SHIFT), releases)
        keys.press("screen:SHIFT_RIGHT", PocketKey.SHIFT)
        keys.releaseAll()
        keys.release("screen:SHIFT_RIGHT") // cancellation after host reset is harmless
        assertFalse(keys.isHeld(PocketKey.SHIFT))
        assertEquals(2, releases.size)
    }

    @Test fun pc1251UsesOldKeysAndItsOwnHoldAndGapCycles() {
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(
            com.digihori.pgp.core.rom.MachineId("pc-1251"))
        val allowed = com.digihori.pgp.player.PlayerKeyboardCatalog.forMachine(definition.id)!!
            .rows.flatten().map { it.key }.toSet()
        val events = mutableListOf<String>()
        val keyboard = PhysicalKeyboardState({ _, key -> events += key.name }, { events += "up" })
        keyboard.configure(true, allowed, definition.automaticKeyHoldCycles, definition.automaticKeyGapCycles)
        assertFalse(keyboard.handle(1, KeyEvent.KEYCODE_F2, true))
        assertFalse(keyboard.handle(1, KeyEvent.KEYCODE_F3, true))
        assertTrue(keyboard.handle(1, KeyEvent.KEYCODE_DPAD_DOWN, true))
        keyboard.handle(1, KeyEvent.KEYCODE_DPAD_DOWN, false)
        keyboard.handle(1, KeyEvent.KEYCODE_DPAD_UP, true)
        keyboard.handle(1, KeyEvent.KEYCODE_DPAD_UP, false)
        keyboard.advanceCycles(38_399)
        assertEquals(listOf("DOWN"), events)
        keyboard.advanceCycles(1)
        keyboard.advanceCycles(19_200)
        assertEquals(listOf("DOWN", "up", "UP"), events)
        keyboard.configure(false, allowed)
        assertEquals(listOf("DOWN", "up", "UP", "up"), events)
    }

}
