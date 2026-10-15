package com.digihori.pgp.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplePlayerFacadeTest {
    @Test fun android1401TargetDoesNotEnableIosPackageImport() {
        val result = ApplePlayerFactory.preparePackage(
            com.digihori.pgp.core.rom.RomPackageManifest(
                format = com.digihori.pgp.core.rom.RomPackageValidator.FORMAT,
                formatVersion = com.digihori.pgp.core.rom.RomPackageValidator.VERSION,
                machineId = "pc-1401", components = emptyList(),
            ), emptyList(),
        )
        kotlin.test.assertNull(result.player)
        assertTrue(kotlin.test.assertNotNull(result.errorMessage).contains("PC-1245のみ"))
    }

    @Test fun probeRunsAndResetClearsInputAndMode() {
        val player = ApplePlayerFactory.structuralProbe()
        player.setProgramMode(true)
        assertTrue(player.press("BREAK", "touch:1"))
        player.run(0)
        player.tick(10_000_000)
        val before = player.snapshot()
        assertEquals("RUNNING", before.runState)
        assertTrue(before.executedCycles > 0)
        assertTrue(before.breakHeld)
        assertEquals("PRO", before.operatingMode)
        assertTrue(before.display.isDotOn(0, 0))
        assertFalse(before.display.isDotOn(0, 1))
        assertTrue(before.display.isDotOn(79, 1))
        assertTrue(player.skin.statusRegions.all { it.symbol in before.display.symbols })

        player.reset()
        val after = player.snapshot()
        assertEquals("PAUSED", after.runState)
        assertEquals("RUN", after.operatingMode)
        assertEquals(0, after.executedCycles)
        assertFalse(after.breakHeld)
        // Snapshot captured before reset stays unchanged.
        assertTrue(before.breakHeld)
    }

    @Test fun allSkinKeysShareIndependentOwners() {
        val player = ApplePlayerFactory.structuralProbe()
        assertEquals(52, player.skin.keyRegions.size)
        for (region in player.skin.keyRegions) {
            assertTrue(player.press(region.key.name, "first"))
            assertTrue(player.press(region.key.name, "second"))
            player.release("first")
            assertEquals(listOf(region.key.name), player.snapshot().heldKeys)
            player.release("second")
            assertTrue(player.snapshot().heldKeys.isEmpty())
        }
    }

    @Test fun pauseReleasesAllOwnersAndResumeDoesNotCatchUpBackgroundTime() {
        val player = ApplePlayerFactory.structuralProbe()
        assertTrue(player.press("BREAK", "touch:1"))
        assertTrue(player.press("BREAK", "touch:2"))
        player.release("touch:1")
        assertTrue(player.snapshot().breakHeld)
        assertFalse(player.press("not-a-key", "invalid"))
        player.run(0)
        player.tick(10_000_000)
        player.pause()
        val paused = player.snapshot()
        assertFalse(paused.breakHeld)
        player.tick(20_000_000_000)
        assertEquals(paused.executedCycles, player.snapshot().executedCycles)
        player.run(20_000_000_000)
        player.tick(20_010_000_000)
        assertTrue(player.snapshot().executedCycles - paused.executedCycles in 1_920..1_930)
        val frame = paused.display
        assertEquals(16, frame.characterColumns)
        assertEquals(7, frame.dotRows)
    }
}
