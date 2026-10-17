package com.digihori.pgp.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplePlayerFacadeTest {
    @Test fun audioBridgeKeepsSilenceDurationSpeedAndImmutableSamples() {
        val silent = ApplePlayerFactory.structuralProbe()
        silent.run(0)
        val silence = kotlin.test.assertNotNull(silent.tick(10_000_000))
        assertEquals(0, silence.sampleCount)
        assertTrue(silence.frameCount in 219..222)
        kotlin.test.assertNull(silent.tick(10_000_000))
        silent.pause()
        kotlin.test.assertNull(silent.tick(20_000_000))

        val internal = ByteArray(com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d }
        // Original oscillator exercise, not firmware: LIP 5f; LIA 20; EXAM; OUTC; JP 0006.
        byteArrayOf(0x12, 0x5f, 0x02, 0x20, 0xdb.toByte(), 0xdf.toByte(), 0x4d, 0x79, 0, 6).copyInto(internal)
        val definition = com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
        val rom = com.digihori.pgp.core.rom.RomSet(definition.MACHINE_ID, listOf(
            com.digihori.pgp.core.rom.RomComponent(definition.INTERNAL_ID, com.digihori.pgp.core.rom.RomRole.INTERNAL, internal),
            com.digihori.pgp.core.rom.RomComponent(definition.EXTERNAL_ID, com.digihori.pgp.core.rom.RomRole.EXTERNAL, ByteArray(definition.EXTERNAL_SIZE) { 0x4d }),
        ))
        for (speed in PlayerExecutionSpeed.entries) {
            val player = ApplePlayerFacade((PlayerSessionFactory.create(rom) as PlayerSessionCreationResult.Success).session)
            player.setSpeed(speed.id, 0)
            player.run(0)
            val packet = kotlin.test.assertNotNull(player.tick(10_000_000))
            assertEquals(speed.playbackRate(22_050), packet.playbackRate)
            assertTrue(packet.copySamples().any { it > 0 })
            assertTrue(packet.copySamples().any { it < 0 })
            val before = packet.copySamples()
            packet.copySamples().fill(0)
            kotlin.test.assertContentEquals(before, packet.copySamples())
            player.pause()
            player.reset()
            assertEquals(speed.id, player.speedId)
            assertEquals("PAUSED", player.snapshot().runState)
            player.setSpeed("bad", 20_000_000)
            assertEquals("1.0", player.speedId)
        }
    }

    @Test fun externalInputsShareOwnersAndRetainNeutralBarrierAcrossPause() {
        val player = ApplePlayerFactory.structuralProbe()
        player.run(0)
        fun configure(receive: Boolean, key: String = "A") = player.configureExternalInput(receive, true, "pad", listOf("SOUTH"), listOf(key))
        configure(true)
        player.updateGamepad("pad", emptyList())
        player.updateGamepad("other", listOf("SOUTH"))
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.updateGamepad("pad", listOf("SOUTH"))
        player.physicalKey(4, true, false)
        player.press("A", "touch:1")
        player.updateGamepad("pad", emptyList())
        assertEquals(listOf("A"), player.snapshot().heldKeys)
        player.physicalKey(4, false, false)
        player.tick(100_000_000)
        assertEquals(listOf("A"), player.snapshot().heldKeys)
        player.release("touch:1")
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.updateGamepad("pad", listOf("SOUTH"))
        player.pause(); configure(false)
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.run(1_000_000_000); configure(true)
        player.updateGamepad("pad", listOf("SOUTH"))
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
        assertEquals(listOf("A"), player.snapshot().heldKeys)
        configure(true, "B") // Old assignment releases; still held is blocked.
        player.updateGamepad("pad", listOf("SOUTH"))
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
        assertEquals(listOf("B"), player.snapshot().heldKeys)
        configure(true, "MODE")
        player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
        assertTrue(player.snapshot().heldKeys.isEmpty()) // Unsupported PC-1245 key retained but not delivered.
    }

    @Test fun applePhysicalCodesRemainPhysicalAndResetDiscardsShortTap() {
        assertEquals("SHIFT", AppleInputCatalog.keyForCode(225))
        assertEquals("SHIFT", AppleInputCatalog.keyForCode(229))
        assertEquals("DEF", AppleInputCatalog.keyForCode(224))
        assertEquals("NUM_7", AppleInputCatalog.keyForCode(36))
        assertEquals("NUM_7", AppleInputCatalog.keyForCode(95))
        kotlin.test.assertNull(AppleInputCatalog.keyForCode(227))
        val player = ApplePlayerFactory.structuralProbe()
        player.run(0)
        player.configureExternalInput(true, false, null, emptyList(), emptyList())
        player.physicalKey(225, true, false); player.physicalKey(4, true, false)
        assertTrue(player.snapshot().heldKeys.containsAll(listOf("SHIFT", "A")))
        player.physicalKey(4, true, true)
        player.physicalKey(4, false, false)
        player.disconnectKeyboard()
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.tick(200_000_000)
        assertTrue(player.snapshot().heldKeys.isEmpty())
        player.pause()
        assertFalse(player.physicalKey(4, true, false))
    }

    private fun parsed(text: String, basic: Boolean = false): PlayerProgramInput =
        kotlin.test.assertNotNull(AppleProgramCodec.parse(text.encodeToByteArray(), basic).input)

    @Test fun programBridgeRoundTripsBasicAndDumpWithoutRunningCpu() {
        val player = ApplePlayerFactory.structuralProbe()
        kotlin.test.assertNull(player.loadProgram(parsed("C6E1 00 C0 FF BF")))
        kotlin.test.assertNull(player.loadProgram(parsed("10 PRINT 1\n20 END", true)))
        val basic = player.exportBasic()
        kotlin.test.assertNull(basic.error)
        assertEquals("10 PRINT 1\n20 END\n", kotlin.test.assertNotNull(basic.copyBytes()).decodeToString())
        kotlin.test.assertNull(player.loadProgram(parsed("C000 01 02\nC510 FF")))
        val dump = player.exportDump(0xc000, 0xc001)
        kotlin.test.assertNull(dump.error)
        val image = kotlin.test.assertIs<com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult.Success>(
            com.digihori.pgp.core.source.machine.PgpMemoryDumpParser.parse(kotlin.test.assertNotNull(dump.copyBytes()).decodeToString())).image
        kotlin.test.assertContentEquals(byteArrayOf(1, 2), image.segments.single().copyBytes())
        val copy = kotlin.test.assertNotNull(dump.copyBytes())
        copy[0] = 0
        assertTrue(kotlin.test.assertNotNull(dump.copyBytes())[0] != 0.toByte())
        assertEquals("PAUSED", player.snapshot().runState)
        assertEquals(0, player.snapshot().executedCycles)
    }

    @Test fun programBridgeKeepsTypedErrorsAndRejectsWholeInvalidDump() {
        val player = ApplePlayerFactory.structuralProbe()
        kotlin.test.assertNull(player.loadProgram(parsed("C500 01 02")))
        val before = kotlin.test.assertNotNull(player.exportDump(0xc500, 0xc501).copyBytes())
        for (text in listOf("C500 99\n0000 FF", "F800 00", "C500 01\nD500 02")) {
            kotlin.test.assertNotNull(player.loadProgram(parsed(text)))
            kotlin.test.assertContentEquals(before, player.exportDump(0xc500, 0xc501).copyBytes())
        }
        kotlin.test.assertIs<PlayerProgramError.InvalidUtf8>(AppleProgramCodec.parse(byteArrayOf(0xc0.toByte()), false).error)
        kotlin.test.assertIs<PlayerProgramError.BasicCompile>(AppleProgramCodec.parse("10 PRINT \"あ\"".encodeToByteArray(), true).error)
        kotlin.test.assertIs<PlayerProgramError.InvalidRange>(player.exportDump(0xc500, 0xc400).error)
        player.run(0)
        kotlin.test.assertIs<PlayerProgramError.NotPaused>(player.loadProgram(parsed("C500 FF")))
        kotlin.test.assertIs<PlayerProgramError.NotPaused>(player.exportDump(0xc500, 0xc501).error)
    }

    @Test fun unsupportedModeIsRejectedAndResetReturnsRun() {
        val player = ApplePlayerFactory.structuralProbe()
        assertFalse(player.setOperatingMode("RSV"))
        assertTrue(player.setOperatingMode("PRO"))
        assertEquals("PRO", player.snapshot().operatingMode)
        assertFalse(player.setOperatingMode("bad"))
        player.reset()
        assertEquals("RUN", player.snapshot().operatingMode)
    }

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
