package com.digihori.pgp.input

import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class GamepadInputTest {
    @Test
    fun disconnectedSelectionDoesNotSwitchToAnotherAlreadyConnectedPad() {
        val one = GamepadDevice("one", "Pad")
        val two = GamepadDevice("two", "Pad")
        val selection = SingleGamepadSelection()
        assertEquals("one", selection.update(listOf(one)))
        assertEquals("one", selection.update(listOf(one, two)))
        assertNull(selection.update(listOf(two)))
        assertNull(selection.update(listOf(two)))
        assertEquals("two", selection.update(listOf(two), "two"))
        assertNull(selection.update(emptyList()))
        assertEquals("one", selection.update(listOf(one)))
    }

    @Test
    fun multiplePadsAtStartupRequireAnExplicitChoice() {
        val devices = listOf(GamepadDevice("one", "Pad"), GamepadDevice("two", "Pad"))
        val selection = SingleGamepadSelection()
        assertNull(selection.update(devices))
        assertEquals("two", selection.update(devices, "two"))
        assertEquals("two", selection.update(devices, "missing"))
    }

    @Test
    fun releaseWaitsForEveryInputOwnerAndRepeatedDownIsIgnored() {
        val events = mutableListOf<String>()
        val keys = HostKeyState(
            { events += "down:$it"; InputResult.Accepted },
            { events += "up:$it"; InputResult.Accepted },
        )
        keys.press("keyboard", PocketKey.NUM_4)
        keys.press("pad:left", PocketKey.NUM_4)
        keys.press("pad:left", PocketKey.NUM_4)
        keys.press("pad:a", PocketKey.NUM_4)
        keys.release("pad:left")
        keys.release("pad:a")
        assertEquals(listOf("down:NUM_4"), events)
        keys.release("keyboard")
        keys.release("keyboard")
        assertEquals(listOf("down:NUM_4", "up:NUM_4"), events)
    }

    @Test
    fun unsupportedPressDoesNotBecomeAnOwner() {
        val releases = mutableListOf<PocketKey>()
        val keys = HostKeyState({ InputResult.UnsupportedKey(it) }, { releases += it; InputResult.Accepted })
        assertIs<InputResult.UnsupportedKey>(keys.press("pad", PocketKey.MODE))
        assertFalse(keys.isHeld(PocketKey.MODE))
        keys.releaseAll()
        assertTrue(releases.isEmpty())
    }

    @Test
    fun onlySelectedDeviceCanSendMappedButtonsAndNoRepeatIsGenerated() {
        val events = mutableListOf<String>()
        val input = GamepadInput({ button, key -> events += "down:$button:$key" },
            { button, key -> events += "up:$button:$key" })
        input.configure(GamepadSettings(true, mapOf(GamepadButton.LEFT to PocketKey.NUM_4)),
            "one", setOf(PocketKey.NUM_4), true)
        input.update("one", emptySet())
        input.update("other", setOf(GamepadButton.LEFT))
        input.update("one", setOf(GamepadButton.LEFT, GamepadButton.SOUTH))
        input.update("one", setOf(GamepadButton.LEFT))
        input.update("one", emptySet())
        assertEquals(listOf("down:LEFT:NUM_4", "up:LEFT:NUM_4"), events)
    }

    @Test
    fun reassigningOrDisconnectingReleasesAndRequiresNeutral() {
        val events = mutableListOf<String>()
        val input = GamepadInput({ _, key -> events += "down:$key" }, { _, key -> events += "up:$key" })
        val settings = GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.NUM_4))
        val allowed = setOf(PocketKey.NUM_4, PocketKey.NUM_6)
        input.configure(settings, "pad", allowed, true)
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.SOUTH))
        input.configure(settings.withBinding(GamepadButton.SOUTH, PocketKey.NUM_6), "pad", allowed, true)
        input.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(listOf("down:NUM_4", "up:NUM_4"), events)
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.SOUTH))
        input.configure(settings, null, allowed, true)
        assertEquals(listOf("down:NUM_4", "up:NUM_4", "down:NUM_6", "up:NUM_6"), events)
    }

    @Test
    fun settingsTestAndFocusLossNeverTypeIntoEmulator() {
        val events = mutableListOf<String>()
        val input = GamepadInput({ _, _ -> events += "down" }, { _, _ -> events += "up" })
        val settings = GamepadSettings(true, mapOf(GamepadButton.NORTH to PocketKey.ENTER))
        input.configure(settings, "pad", setOf(PocketKey.ENTER), false)
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.NORTH))
        input.configure(settings, "pad", setOf(PocketKey.ENTER), true)
        input.update("pad", setOf(GamepadButton.NORTH))
        assertTrue(events.isEmpty())
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.NORTH))
        input.configure(settings, "pad", setOf(PocketKey.ENTER), false)
        assertEquals(listOf("down", "up"), events)
    }

    @Test
    fun savedSettingOverwritesAndUnknownKeysAreUnassigned() {
        val stored = GamepadSettings(true, mapOf(GamepadButton.UP to PocketKey.NUM_8)).preferenceValues().toMutableMap()
        val replacement = GamepadSettings(false, mapOf(GamepadButton.SOUTH to PocketKey.ENTER))
        stored.putAll(replacement.preferenceValues())
        assertEquals(replacement, GamepadSettings.fromPreferences(stored::get))
        stored["SOUTH"] = "REMOVED_KEY"
        stored["enabled"] = "corrupt"
        assertEquals(GamepadSettings(), GamepadSettings.fromPreferences(stored::get))
    }

    @Test
    fun unsupportedMachineKeysAreSuppressedWithoutErasingSavedBindings() {
        val settings = GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.MODE))
        var presses = 0
        val input = GamepadInput({ _, _ -> presses++ }, { _, _ -> })
        input.configure(settings, "pad", GamepadKeys.forMachine(MachineId("pc-1245")), true)
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(0, presses)
        input.configure(settings, "pad", GamepadKeys.forMachine(MachineId("pc-1350")), true)
        input.update("pad", emptySet())
        input.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(1, presses)
        assertEquals(PocketKey.MODE, settings.bindings[GamepadButton.SOUTH])
    }
}
