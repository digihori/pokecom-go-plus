package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.input.*
import kotlin.test.*

class Pc1360kGamepadTest {
    private val id = MachineId("pc-1360k")
    private fun session(): PlayerSession {
        val rom = assertIs<RomImportResult.Success>(Pc1360ComponentRomImporter.importImages(
            ByteArray(0x2000) { 0x4d }, List(8) { ByteArray(0x4000) { 0x4d } }, id, List(4) { ByteArray(0x10000) },
        )).romSet
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }

    @Test fun every1360KeyCanBeAssignedAndDispatchedToCore() {
        val player = session()
        val events = mutableListOf<String>()
        val owners = HostKeyState({ key ->
            events += "down:$key"; player.pressKey(key).also { assertIs<InputResult.Accepted>(it) }
        }, { key -> events += "up:$key"; player.releaseKey(key).also { assertIs<InputResult.Accepted>(it) } })
        val pad = GamepadInput({ button, key -> owners.press("pad:$button", key) },
            { button, _ -> owners.release("pad:$button") })
        val keys = GamepadKeys.forMachine(id)
        assertEquals(65, keys.size)
        assertEquals(player.keyboard()!!.rows.flatten().map { it.key }.toSet(), keys)
        keys.forEach { key ->
            events.clear()
            pad.configure(GamepadSettings(true, mapOf(GamepadButton.SOUTH to key)), "pad", keys, true)
            pad.update("pad", emptySet())
            repeat(2) { pad.update("pad", setOf(GamepadButton.SOUTH)) }
            pad.update("pad", emptySet())
            assertEquals(listOf("down:$key", "up:$key"), events)
            assertFalse(owners.isHeld(key))
        }
    }

    @Test fun simultaneousOwnersAndShiftChordReleaseIndependently() {
        val player = session()
        val events = mutableListOf<String>()
        val owners = HostKeyState({ key -> events += "down:$key"; player.pressKey(key) },
            { key -> events += "up:$key"; player.releaseKey(key) })
        val pad = GamepadInput({ button, key -> owners.press("pad:$button", key) },
            { button, _ -> owners.release("pad:$button") })
        pad.configure(GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.NUM_4,
            GamepadButton.WEST to PocketKey.NUM_4, GamepadButton.L1 to PocketKey.SHIFT)),
            "pad", GamepadKeys.forMachine(id), true)
        pad.update("pad", emptySet())
        owners.press("touch", PocketKey.NUM_4); owners.press("keyboard", PocketKey.NUM_4)
        pad.update("pad", setOf(GamepadButton.SOUTH, GamepadButton.WEST, GamepadButton.L1))
        pad.update("pad", emptySet())
        assertTrue(owners.isHeld(PocketKey.NUM_4)); assertFalse(owners.isHeld(PocketKey.SHIFT))
        owners.release("touch"); assertTrue(owners.isHeld(PocketKey.NUM_4))
        owners.release("keyboard")
        assertEquals(listOf("down:NUM_4", "down:SHIFT", "up:SHIFT", "up:NUM_4"), events)
    }

    @Test fun pauseModalResetDisconnectAndMachineSwitchRequireReleaseBeforeRearming() {
        val events = mutableListOf<String>()
        val pad = GamepadInput({ _, key -> events += "down:$key" }, { _, key -> events += "up:$key" })
        val settings = GamepadSettings(true, mapOf(GamepadButton.SOUTH to PocketKey.CAL))
        val keys = GamepadKeys.forMachine(MachineId("pc-1401"))
        pad.configure(settings, "pad", keys, true); pad.update("pad", emptySet())
        pad.update("pad", setOf(GamepadButton.SOUTH))
        pad.configure(settings, "pad", keys, false)
        pad.update("pad", setOf(GamepadButton.SOUTH))
        pad.configure(settings, "pad", keys, true)
        pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(listOf("down:CAL", "up:CAL"), events)
        pad.update("pad", emptySet()); pad.update("pad", setOf(GamepadButton.SOUTH)); pad.reset()
        pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(4, events.size)
        pad.configure(settings, null, keys, true); pad.update("pad", emptySet())
        pad.configure(settings, "pad", keys, true); pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(4, events.size)
        pad.update("pad", emptySet()); pad.update("pad", setOf(GamepadButton.SOUTH))
        pad.configure(settings, "pad", GamepadKeys.forMachine(id), true)
        pad.update("pad", emptySet()); pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(6, events.size) // Release CAL; PC-1360 has no CAL key.
        assertEquals(PocketKey.CAL, settings.bindings[GamepadButton.SOUTH])
        pad.configure(settings, "pad", keys, true); pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals(6, events.size)
        pad.update("pad", emptySet()); pad.update("pad", setOf(GamepadButton.SOUTH))
        assertEquals("down:CAL", events.last())
    }
}
