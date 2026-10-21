package com.digihori.pgp.player

import com.digihori.pgp.core.rom.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplePlayerFacadeTest {
    @Test fun iosCatalogKeepsExplicitTargetsAndModePolicy() {
        assertEquals(listOf("pc-1245", "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261", "pc-1350", "pc-1401", "pc-1402", "pc-1450", "pc-1360", "pc-1460", "pc-1470u", "pc-1360k"), AppleMachineCatalog.machines.map { it.id })
        assertEquals(PlayerMachineCatalog.homeDefinitions.map { it.id.value }, AppleMachineCatalog.homeMachines.map { it.id })
        kotlin.test.assertNull(AppleMachineCatalog.find("pc-9999"))
        val player = ApplePlayerFactory.structuralProbe()
        assertEquals("PC-1245", player.machine.displayName)
        assertEquals(16, player.machine.characterColumns)
        assertEquals(1, player.machine.characterRows)
        assertEquals(listOf("RUN", "PRO"), player.machine.operatingModes)
        assertFalse(player.setOperatingMode("RSV"))
        assertFalse(player.setOperatingMode("CAL"))
        assertTrue(player.setOperatingMode("PRO"))
        assertEquals("PRO", player.snapshot().operatingMode)
        assertTrue(AppleInputCatalog.keys.containsAll(player.machine.keys))
    }

    @Test fun romComponentsAndMachineAwareCodecKeepCopyBoundary() {
        val prepared = ApplePlayerFactory.prepareLegacy(ByteArray(0x8000) { 0x4d })
        assertEquals("pc-1245", prepared.machineId)
        assertEquals(listOf("internal", "external"), prepared.components.map { it.id })
        val original = prepared.components.first().copyBytes()
        prepared.components.first().copyBytes().fill(0)
        kotlin.test.assertContentEquals(original, prepared.components.first().copyBytes())
        val bytes = "10 PRINT 1\n".encodeToByteArray()
        kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(bytes, true, "pc-1245").input)
        kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(bytes, true, "pc-1350").input)
    }

    @Test fun pc125xAppleBridgeUsesSharedRomAndHardwareModelBoundaries() {
        val bytes = ByteArray(0x8000) { 0x4d }
        byteArrayOf(0x79, 0, 0).copyInto(bytes)
        for ((id, start) in listOf("pc-1250" to 0xc000, "pc-1251" to 0xb800, "pc-1255" to 0xa000)) {
            val prepared = ApplePlayerFactory.prepareLegacyForMachine(bytes, id)
            val player = kotlin.test.assertNotNull(prepared.player)
            assertEquals("pc-1251", prepared.machineId)
            assertEquals(id, player.machine.id)
            assertEquals("skin_pc1251", player.machine.skinImageName)
            assertEquals(24, player.machine.characterColumns)
            assertEquals(listOf("RUN", "PRO", "RSV"), player.machine.operatingModes)
            fun parse(text: String, basic: Boolean = false) = kotlin.test.assertNotNull(
                AppleProgramCodec.parseForMachine(text.encodeToByteArray(), basic, id).input)
            kotlin.test.assertNull(player.loadProgram(parse("${start.toString(16)} 5A\nC7FF 6B")))
            kotlin.test.assertNull(player.exportDump(start, start).error)
            assertEquals(start, player.machine.standardRamStart)
            kotlin.test.assertNotNull(player.exportDump(0x1fff, 0x1fff).error)
            val before = kotlin.test.assertNotNull(player.exportDump(start, start).copyBytes())
            kotlin.test.assertNotNull(player.loadProgram(parse("${start.toString(16)} 7C\n1FFF 8D")))
            kotlin.test.assertContentEquals(before, player.exportDump(start, start).copyBytes())
            val pointer = listOf(start and 255, start shr 8, start and 255, start shr 8).joinToString(" ") { it.toString(16).padStart(2, '0') }
            kotlin.test.assertNull(player.loadProgram(parse("C6E1 $pointer")))
            kotlin.test.assertNull(player.loadProgram(parse("10 PRINT 1\n20 END\n", true)))
            val saved = kotlin.test.assertNotNull(player.exportBasic().copyBytes())
            assertTrue(saved.decodeToString().contains("PRINT"))
            kotlin.test.assertNull(player.loadProgram(kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(saved, true, id).input)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            val large = (1..700).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" }
            kotlin.test.assertNotNull(player.loadProgram(parse(large, true)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            for (mode in player.machine.operatingModes) assertTrue(player.setOperatingMode(mode))
            player.reset(); assertEquals("RUN", player.snapshot().operatingMode)
            player.run(0); player.configureExternalInput(true, true, "pad", listOf("SOUTH"), listOf("A"))
            player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
            assertTrue("A" in player.snapshot().heldKeys)
            player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
            for (key in player.machine.keys) { assertTrue(player.press(key, "test")); player.release("test") }
        }
    }

    @Test fun pc126xAppleBridgeSharesRomButUsesS1ModelRamAndTwoLcdRows() {
        val image = ByteArray(0x10000) { 0x4d }
        val code = mutableListOf<Int>()
        fun write(address: Int, value: Int) { code += listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52) }
        write(0x2000, 1); write(0x283b, 64); write(0x2040, 1); write(0x287b, 64)
        write(0x203d, 0x7b); write(0x207c, 0x23)
        val loop = code.size; code += listOf(0x79, loop shr 8, loop and 255)
        code.forEachIndexed { index, value -> image[index] = value.toByte() }
        for ((id, start) in listOf("pc-1260" to 0x5800, "pc-1261" to 0x4000)) {
            val prepared = ApplePlayerFactory.prepareLegacyForMachine(image, id)
            val player = kotlin.test.assertNotNull(prepared.player)
            assertEquals("pc-1261", prepared.machineId)
            assertEquals(id, player.machine.id)
            assertEquals(start, player.machine.standardRamStart)
            assertEquals("64 KiB", player.machine.legacyRomSizes)
            assertEquals("skin_pc1261", player.machine.skinImageName)
            val before = player.snapshot().display
            player.run(0); player.tick(10_000_000); player.pause()
            val display = player.snapshot().display
            assertEquals(24, display.characterColumns); assertEquals(2, display.characterRows)
            assertEquals(143, display.logicalColumns); assertEquals(15, display.logicalRows)
            assertEquals(8, display.logicalY(7))
            for ((x, y) in listOf(0 to 0, 119 to 6, 0 to 7, 119 to 13)) assertTrue(display.isDotOn(x, y))
            assertFalse(before.isDotOn(0, 0))
            assertTrue(com.digihori.pgp.core.api.DisplaySymbol.KANA in display.symbols)
            assertTrue(com.digihori.pgp.core.api.DisplaySymbol.SMALL in display.symbols)
            for (mode in listOf("RUN", "PRO", "RSV")) {
                assertTrue(player.setOperatingMode(mode)); assertEquals(mode, player.snapshot().operatingMode)
            }
            fun parse(text: String, basic: Boolean = false) = kotlin.test.assertNotNull(
                AppleProgramCodec.parseForMachine(text.encodeToByteArray(), basic, id).input)
            kotlin.test.assertNull(player.loadProgram(parse("${start.toString(16)} 5A\n67FF 6B")))
            for (address in listOf(start - 1, 0x2000, 0x6800, 0x8000)) {
                kotlin.test.assertNotNull(player.loadProgram(parse("${address.toString(16)} 12")))
                kotlin.test.assertNotNull(player.exportDump(address, address).error)
            }
            val basicStart = start + 0x80
            val pointers = listOf(basicStart and 255, basicStart shr 8, basicStart and 255, basicStart shr 8).joinToString(" ") { it.toString(16).padStart(2, '0') }
            kotlin.test.assertNull(player.loadProgram(parse("66E1 $pointers")))
            kotlin.test.assertNull(player.loadProgram(parse("10 PRINT \"S1\"\n20 END\n", true)))
            val saved = kotlin.test.assertNotNull(player.exportBasic().copyBytes())
            kotlin.test.assertNull(player.loadProgram(kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(saved, true, id).input)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            val large = (1..700).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" }
            kotlin.test.assertNotNull(player.loadProgram(parse(large, true)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            player.run(20_000_000); player.configureExternalInput(true, true, "pad", listOf("SOUTH"), listOf("A"))
            player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
            assertTrue(player.physicalKey(4, true, false))
            player.updateGamepad("pad", emptyList()); assertTrue("A" in player.snapshot().heldKeys)
            player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
            for (key in player.machine.keys) { assertTrue(player.press(key, "touch")); player.release("touch") }
            player.reset(); assertEquals("RUN", player.snapshot().operatingMode)
            assertFalse(player.snapshot().display.isDotOn(0, 0)); assertTrue(display.isDotOn(0, 0))
            kotlin.test.assertNull(ApplePlayerFactory.prepareLegacyForMachine(ByteArray(0x8000), id).player)
        }
    }

    @Test fun appleSkinsUseTheSharedFixedModePositions() {
        for (machine in AppleMachineCatalog.machines.filter { it.id.startsWith("pc-12") }) {
            val size = if (machine.romId == "pc-1261") 0x10000 else 0x8000
            val player = kotlin.test.assertNotNull(ApplePlayerFactory.prepareLegacyForMachine(ByteArray(size) { 0x4d }, machine.id).player)
            kotlin.test.assertNull(player.skin.operatingModeRegion)
            val symbols = listOf(com.digihori.pgp.core.api.DisplaySymbol.RUN,
                com.digihori.pgp.core.api.DisplaySymbol.PRO, com.digihori.pgp.core.api.DisplaySymbol.RESERVE)
            val regions = player.skin.statusRegions.filter { it.symbol in symbols }
            assertEquals(if (machine.id == "pc-1245") 2 else 3, regions.size)
            assertEquals(listOf(70f, 135f, 200f).take(regions.size), regions.map { it.bounds.x })
            for ((index, mode) in machine.operatingModes.withIndex()) {
                assertTrue(player.setOperatingMode(mode))
                assertEquals(listOf(symbols[index]), player.snapshot().display.symbols.filter { it in symbols })
            }
        }
    }

    @Test fun pc1350AppleBridgeFourRowsProgramIoAndDistinctShiftOwners() {
        val image = ByteArray(0x10000) { 0x4d }
        val code = mutableListOf<Int>()
        fun write(address: Int, value: Int) { code += listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52) }
        listOf(0x7000, 0x7040, 0x701e, 0x705e).forEach { write(it, 1) }
        listOf(0x781d, 0x785d, 0x783b, 0x787b).forEach { write(it, 0x80) }
        write(0x783c, 0xf3)
        val loop = code.size; code += listOf(0x79, loop shr 8, loop and 255)
        code.forEachIndexed { i, v -> image[i] = v.toByte() }
        val player = kotlin.test.assertNotNull(ApplePlayerFactory.prepareLegacyForMachine(image, "pc-1350").player)
        assertTrue(player.machine.hasModeKey); assertTrue(player.machine.operatingModes.isEmpty())
        assertFalse(player.setOperatingMode("PRO")); assertFalse(player.setOperatingMode("RSV"))
        assertEquals(0x2000, player.machine.standardRamStart)
        assertEquals(62, player.skin.keyRegions.size)
        assertEquals(61, player.skin.keyRegions.map { it.key }.toSet().size)
        val before = player.snapshot().display
        player.run(0); player.tick(10_000_000); player.pause()
        val frame = player.snapshot().display
        assertEquals(25, frame.characterColumns); assertEquals(4, frame.characterRows)
        assertEquals(150, frame.logicalColumns); assertEquals(32, frame.logicalRows)
        repeat(4) { row ->
            assertTrue(frame.isDotOn(0, row * 8)); assertTrue(frame.isDotOn(149, row * 8 + 7))
            assertFalse(before.isDotOn(0, row * 8))
        }
        assertTrue(com.digihori.pgp.core.api.DisplaySymbol.KANA in frame.symbols)
        for (key in player.machine.keys) { assertTrue(player.press(key, "key")); player.release("key") }
        player.press("SHIFT", "left"); player.press("SHIFT", "right"); player.release("left")
        assertTrue("SHIFT" in player.snapshot().heldKeys); player.release("right")
        assertTrue(player.snapshot().heldKeys.isEmpty())
        fun parse(text: String, basic: Boolean = false) = kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(text.encodeToByteArray(), basic, "pc-1350").input)
        kotlin.test.assertNull(player.loadProgram(parse("10 PRINT \"S1\"\n20 END\n", true)))
        val saved = kotlin.test.assertNotNull(player.exportBasic().copyBytes())
        kotlin.test.assertNull(player.loadProgram(kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(saved, true, "pc-1350").input)))
        kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
        val large = (1..700).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" }
        kotlin.test.assertNotNull(player.loadProgram(parse(large, true)))
        kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
        kotlin.test.assertNull(player.loadProgram(parse("2000 12\n6000 34\n6FFF 56\n7FFF 78")))
        val ram = kotlin.test.assertNotNull(player.exportDump(0x6000, 0x6000).copyBytes())
        for (address in listOf(0x1fff, 0x7000, 0x8000)) {
            kotlin.test.assertNotNull(player.loadProgram(parse("6000 99\n${address.toString(16)} 12")))
            kotlin.test.assertNotNull(player.exportDump(address, address).error)
            kotlin.test.assertContentEquals(ram, player.exportDump(0x6000, 0x6000).copyBytes())
        }
        player.run(20_000_000)
        player.configureExternalInput(true, true, "pad", listOf("SOUTH"), listOf("MODE"))
        player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
        assertTrue("MODE" in player.snapshot().heldKeys)
        player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
        for ((code, name) in listOf(59 to "MODE", 60 to "KANA", 73 to "INSERT", 76 to "DELETE", 54 to "COMMA", 51 to "SEMICOLON")) {
            assertEquals(name, AppleInputCatalog.keyForMachine(code, "pc-1350"))
            player.run(30_000_000); player.configureExternalInput(true, false, null, emptyList(), emptyList())
            assertTrue(player.physicalKey(code, true, false)); assertTrue(name in player.snapshot().heldKeys)
            player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
        }
        assertEquals("CLEAR", AppleInputCatalog.keyForMachine(76, "pc-1245"))
        kotlin.test.assertNull(AppleInputCatalog.keyForMachine(59, "pc-1261"))
        player.reset(); assertFalse(player.snapshot().display.isDotOn(0, 0)); assertTrue(frame.isDotOn(0, 0))
        kotlin.test.assertNull(ApplePlayerFactory.prepareLegacyForMachine(ByteArray(0x8000), "pc-1350").player)
    }

    @Test fun pc140xAnd1450BridgeUsesModelRamRomSymbolsAndScientificKeys() {
        for ((id, start) in listOf("pc-1401" to 0x3800, "pc-1402" to 0x2000, "pc-1450" to 0x2000)) {
            val image = ByteArray(0x10000) { 0x4d }
            val base = if (id == "pc-1450") 0x7000 else 0x6000
            val code = mutableListOf<Int>()
            fun write(address: Int, value: Int) { code += listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52) }
            write(base, 1); write(base + (if (id == "pc-1450") 0x68 else 0x40), 64)
            write(base + 0x3c, 0xf); write(base + 0x3d, 0x7f); write(base + 0x7c, 0x7f)
            val loop = code.size; code += listOf(0x79, loop shr 8, loop and 255)
            code.forEachIndexed { i, v -> image[i] = v.toByte() }
            val prepared = ApplePlayerFactory.prepareLegacyForMachine(image, id)
            val player = kotlin.test.assertNotNull(prepared.player)
            assertEquals(if (id == "pc-1450") id else "pc-1401", prepared.machineId)
            assertEquals(id, player.machine.id); assertEquals(start, player.machine.standardRamStart)
            assertEquals("64 KiB", player.machine.legacyRomSizes)
            assertEquals(listOf("CAL", "RUN", "PRO"), player.machine.operatingModes)
            assertFalse(player.setOperatingMode("RSV"))
            assertTrue(player.setOperatingMode("CAL")); assertTrue(player.setOperatingMode("PRO"))
            val before = player.snapshot().display
            player.run(0); player.tick(10_000_000); player.pause()
            val display = player.snapshot().display
            assertEquals(16, display.characterColumns); assertEquals(1, display.characterRows)
            assertTrue(display.isDotOn(0, 0)); assertTrue(display.isDotOn(79, 6)); assertFalse(before.isDotOn(0, 0))
            val symbols = listOf(com.digihori.pgp.core.api.DisplaySymbol.CAL, com.digihori.pgp.core.api.DisplaySymbol.RUN,
                com.digihori.pgp.core.api.DisplaySymbol.PRO, com.digihori.pgp.core.api.DisplaySymbol.HYP,
                com.digihori.pgp.core.api.DisplaySymbol.STAT, com.digihori.pgp.core.api.DisplaySymbol.PRINT)
            assertTrue(display.symbols.containsAll(symbols))
            assertTrue(player.skin.statusRegions.map { it.symbol }.containsAll(symbols))
            for (key in player.machine.keys) { assertTrue(player.press(key, "touch"), key); player.release("touch") }
            fun parse(text: String, basic: Boolean = false) = kotlin.test.assertNotNull(
                AppleProgramCodec.parseForMachine(text.encodeToByteArray(), basic, id).input)
            kotlin.test.assertNull(player.loadProgram(parse("10 PRINT 1\n20 END\n", true)))
            val saved = kotlin.test.assertNotNull(player.exportBasic().copyBytes())
            kotlin.test.assertNull(player.loadProgram(kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(saved, true, id).input)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            val large = (1..700).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" }
            kotlin.test.assertNotNull(player.loadProgram(parse(large, true)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            val end = if (id == "pc-1450") 0x5fff else 0x47ff
            kotlin.test.assertNull(player.loadProgram(parse("${start.toString(16)} 12\n${end.toString(16)} 34")))
            val ram = kotlin.test.assertNotNull(player.exportDump(start, start).copyBytes())
            for (bad in listOf(start - 1, end + 1, base, 0x8000)) {
                kotlin.test.assertNotNull(player.loadProgram(parse("${start.toString(16)} 99\n${bad.toString(16)} 56")))
                kotlin.test.assertNotNull(player.exportDump(bad, bad).error)
                kotlin.test.assertContentEquals(ram, player.exportDump(start, start).copyBytes())
            }
            val physical = listOf(59 to "CAL", 60 to "BASIC", 182 to "LEFT_PAREN") +
                if (id == "pc-1450") listOf(61 to "KANA", 62 to "SMALL", 63 to "DAKUTEN", 64 to "LONG_VOWEL", 65 to "LOCK") else emptyList()
            for ((code, name) in physical) {
                assertEquals(name, AppleInputCatalog.keyForMachine(code, id))
                player.run(20_000_000); player.configureExternalInput(true, false, null, emptyList(), emptyList())
                assertTrue(player.physicalKey(code, true, false)); assertTrue(name in player.snapshot().heldKeys)
                player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
            }
            player.run(30_000_000); player.configureExternalInput(true, true, "pad", listOf("SOUTH"), listOf("CAL"))
            player.updateGamepad("pad", emptyList()); player.updateGamepad("pad", listOf("SOUTH"))
            assertTrue("CAL" in player.snapshot().heldKeys)
            player.reset(); assertTrue(player.snapshot().heldKeys.isEmpty())
            assertFalse(player.snapshot().display.isDotOn(0, 0)); assertTrue(display.isDotOn(0, 0))
            kotlin.test.assertNull(ApplePlayerFactory.prepareLegacyForMachine(ByteArray(0x8000), id).player)
        }
    }

    @Test fun bankedApplePackagesExecuteEveryBankAndPreserveS2RamBoundaries() {
        for (id in listOf("pc-1360", "pc-1460", "pc-1470u")) {
            val count = if (id == "pc-1460") 4 else 8
            val selector = if (id == "pc-1460") 0x3c00 else 0x3400
            val bytes = linkedMapOf<String, ByteArray>()
            val code = mutableListOf<Int>()
            fun write(address: Int, value: Int) { code += listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52) }
            repeat(count) { bank ->
                write(selector, bank); code += listOf(0x79, 0x40, 0)
                val back = code.size
                val bankCode = listOf(0x10, 0x90, bank, 0x02, bank + 1, 0x52, 0x79, back shr 8, back and 255)
                bytes["bank-$bank"] = ByteArray(0x4000) { 0x4d }.also { image -> bankCode.forEachIndexed { i, v -> image[i] = v.toByte() } }
            }
            when (id) {
                "pc-1360" -> {
                    listOf(0x2800, 0x2840, 0x281e, 0x285e).forEach { write(it, 1) }
                    listOf(0x301d, 0x305d, 0x303b, 0x307b).forEach { write(it, 128) }
                    write(0x303c, 0x5f)
                }
                "pc-1460" -> { write(0x3000, 1); write(0x3040, 64); write(0x303c, 31); write(0x303d, 127); write(0x307c, 127) }
                else -> { write(0x2800, 1); write(0x2a3b, 64); write(0x2840, 1); write(0x2a7b, 64)
                    write(0x283c, 127); write(0x283d, 15); write(0x287c, 127); write(0x287d, 15) }
            }
            val loop = code.size; code += listOf(0x79, loop shr 8, loop and 255)
            bytes["internal"] = ByteArray(0x2000) { 0x4d }.also { image -> code.forEachIndexed { i, v -> image[i] = v.toByte() } }
            // Hash computation belongs to the Swift host; this test supplies matching boundary metadata.
            val hash = "a".repeat(64)
            val components = bytes.map { (key, image) -> RomPackageComponentManifest(key, if (key == "internal") "internal" else "external", "rom/$key.bin", image.size, hash) }
            val manifest = RomPackageManifest(RomPackageValidator.FORMAT, RomPackageValidator.VERSION, id, components = components)
            val entries = bytes.map { (key, image) -> AppleRomArchiveEntry("rom/$key.bin", hash, image) }
            val prepared = ApplePlayerFactory.preparePackage(manifest, entries)
            val player = kotlin.test.assertNotNull(prepared.player)
            assertEquals(count + 1, prepared.components.size)
            assertFalse(player.machine.supportsLegacyRom)
            assertEquals(0x8000, player.machine.standardRamStart)
            val original = player.snapshot().display
            player.run(0); player.tick(10_000_000); player.pause()
            val frame = player.snapshot().display
            assertTrue(frame.isDotOn(0, 0)); assertFalse(original.isDotOn(0, 0))
            if (id == "pc-1360") {
                assertEquals(25, frame.characterColumns); assertEquals(4, frame.characterRows)
                repeat(4) { row -> assertTrue(frame.isDotOn(0, row * 8)); assertTrue(frame.isDotOn(149, row * 8 + 7)) }
            } else {
                assertEquals(24, frame.characterColumns); assertEquals(if (id == "pc-1460") 1 else 2, frame.characterRows)
                assertTrue(frame.isDotOn(119, 6))
                if (id == "pc-1470u") { assertTrue(frame.isDotOn(0, 7)); assertTrue(frame.isDotOn(119, 13)) }
            }
            assertEquals(if (id == "pc-1360") 6 else if (id == "pc-1460") 19 else 22, frame.symbols.size)
            val dump = kotlin.test.assertNotNull(player.exportDump(0x9000, 0x9000 + count - 1).copyBytes())
            val parsed = kotlin.test.assertIs<com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult.Success>(
                com.digihori.pgp.core.source.machine.PgpMemoryDumpParser.parse(dump.decodeToString()))
            kotlin.test.assertContentEquals(ByteArray(count) { (it + 1).toByte() }, parsed.image.segments.single().copyBytes())
            fun input(text: String, basic: Boolean = false) = kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine(text.encodeToByteArray(), basic, id).input)
            kotlin.test.assertNull(player.loadProgram(input("10 PRINT \"S2\"\n20 END\n", true)))
            val saved = kotlin.test.assertNotNull(player.exportBasic().copyBytes())
            kotlin.test.assertNull(player.loadProgram(input(saved.decodeToString(), true)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            val huge = (1..900).joinToString("\n") { "$it PRINT \"${"A".repeat(60)}\"" }
            kotlin.test.assertNotNull(player.loadProgram(input(huge, true)))
            kotlin.test.assertContentEquals(saved, player.exportBasic().copyBytes())
            kotlin.test.assertNull(player.loadProgram(input("8000 12\nFFFF 34")))
            val ram = kotlin.test.assertNotNull(player.exportDump(0x9000, 0x9000).copyBytes())
            for (bad in listOf(0x1fff, 0x2800, 0x3000, selector, 0x4000, 0x7fff)) {
                kotlin.test.assertNotNull(player.loadProgram(input("9000 99\n${bad.toString(16)} 56")))
                kotlin.test.assertNotNull(player.exportDump(bad, bad).error)
                kotlin.test.assertContentEquals(ram, player.exportDump(0x9000, 0x9000).copyBytes())
            }
            for (key in player.machine.keys) { assertTrue(player.press(key, "touch"), key); player.release("touch") }
            val physical = if (id == "pc-1360") listOf(59 to "MODE", 60 to "KANA", 61 to "SMALL", 73 to "INSERT", 76 to "DELETE")
                else listOf(59 to "CAL", 60 to "BASIC", 61 to "KANA", 62 to "SMALL", 63 to "DAKUTEN", 64 to "LONG_VOWEL", 65 to if (id == "pc-1470u") "SHIFT" else "LOCK") +
                    if (id == "pc-1470u") listOf(42 to "BACKSPACE", 73 to "INSERT", 76 to "DELETE") else emptyList()
            for ((code, key) in physical) {
                assertEquals(key, AppleInputCatalog.keyForMachine(code, id))
                player.run(20_000_000); player.configureExternalInput(true, false, null, emptyList(), emptyList())
                assertTrue(player.physicalKey(code, true, false)); assertTrue(key in player.snapshot().heldKeys)
                player.pause(); assertTrue(player.snapshot().heldKeys.isEmpty())
            }
            player.press("SHIFT", "one"); player.press("SHIFT", "two"); player.release("one")
            assertTrue("SHIFT" in player.snapshot().heldKeys); player.reset(); assertTrue(player.snapshot().heldKeys.isEmpty())
            assertFalse(player.snapshot().display.isDotOn(0, 0)); assertTrue(frame.isDotOn(0, 0))
            kotlin.test.assertNull(ApplePlayerFactory.prepareLegacyForMachine(ByteArray(0x10000), id).player)
            kotlin.test.assertNull(ApplePlayerFactory.preparePackage(manifest.copy(components = components.dropLast(1)), entries).player)
            kotlin.test.assertNull(ApplePlayerFactory.preparePackage(manifest, entries.dropLast(1)).player)
            kotlin.test.assertNull(ApplePlayerFactory.preparePackage(manifest, entries + entries.first()).player)
            val wrongRole = components.map { if (it.id == "bank-0") it.copy(role = "internal") else it }
            kotlin.test.assertNull(ApplePlayerFactory.preparePackage(manifest.copy(components = wrongRole), entries).player)
        }
    }

    @Test fun kanjiPackagesValidateSelectorsAndExecuteReadAndReadm() {
        val id = "pc-1360k"
        val hash = "a".repeat(64)
        val code = mutableListOf<Int>()
        repeat(4) { selector ->
            code += listOf(0x10, 0x36, 0, 0x02, selector, 0x52,
                0x10, 0x12, 0x34, 0x56, 0x10, 0xc1, selector, 0x52,
                0x12, 0x02, 0x10, 0x12, 0x34, 0x54, 0x10, 0xc1, 0x10 + selector, 0x52)
        }
        code += listOf(0x10, 0x12, 0x34, 0x56, 0x10, 0x28, 0, 0x52,
            0x10, 0x30, 0x3c, 0x02, 255, 0x52)
        val loop = code.size; code += listOf(0x79, loop shr 8, loop and 255)
        val images = linkedMapOf("internal" to ByteArray(0x2000) { 0x4d }.also { bytes -> code.forEachIndexed { i, v -> bytes[i] = v.toByte() } })
        repeat(8) { images["bank-$it"] = ByteArray(0x4000) { 0x4d } }
        repeat(4) { images["kanji-selector-$it"] = ByteArray(0x10000) { _ -> (0x40 + it).toByte() } }
        val components = images.map { (key, image) -> RomPackageComponentManifest(key,
            if (key == "internal") "internal" else if (key.startsWith("kanji")) "kanji" else "external", "rom/$key.bin", image.size, hash) }
        val entries = images.map { (key, image) -> AppleRomArchiveEntry("rom/$key.bin", hash, image) }
        fun prepare(parts: List<RomPackageComponentManifest>) = ApplePlayerFactory.preparePackage(
            RomPackageManifest(RomPackageValidator.FORMAT, RomPackageValidator.VERSION, id, components = parts), entries)
        for (count in listOf(0, 2, 4)) {
            val parts = components.filter { !it.id.startsWith("kanji") || it.id.removePrefix("kanji-selector-").toInt() < count }
            kotlin.test.assertNotNull(prepare(parts).player)
        }
        for (excluded in listOf(setOf(0), setOf(1), setOf(0, 1), setOf(1, 3))) {
            kotlin.test.assertNull(prepare(components.filter { it.id !in excluded.map { n -> "kanji-selector-$n" } }).player)
        }
        kotlin.test.assertNull(prepare(components.map { if (it.id == "kanji-selector-0") it.copy(role = "external") else it }).player)
        kotlin.test.assertNull(prepare(components.map { if (it.id == "kanji-selector-0") it.copy(size = 32768) else it }).player)
        val player = kotlin.test.assertNotNull(prepare(components).player)
        assertEquals(65, player.machine.keys.size)
        assertEquals("pc-1360k", player.machine.romId)
        player.run(0); player.tick(10_000_000); player.pause()
        for (start in listOf(0xc100, 0xc110)) {
            val dump = kotlin.test.assertNotNull(player.exportDump(start, start + 3).copyBytes())
            val parsed = kotlin.test.assertIs<com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult.Success>(
                com.digihori.pgp.core.source.machine.PgpMemoryDumpParser.parse(dump.decodeToString()))
            kotlin.test.assertContentEquals(byteArrayOf(0x40, 0x41, 0x42, 0x43), parsed.image.segments.single().copyBytes())
        }
        assertTrue(player.snapshot().display.isDotOn(0, 6))
        assertEquals(setOf("RUN", "PRO", "HIRAGANA", "KATAKANA", "S", "SMALL", "SHIFT", "DEF"), player.snapshot().display.symbols.map { it.name }.toSet())
        val input = kotlin.test.assertNotNull(AppleProgramCodec.parseForMachine("2000 12\n27FF 34".encodeToByteArray(), false, id).input)
        kotlin.test.assertNull(player.loadProgram(input))
        for ((code, key) in listOf(62 to "KANJI", 63 to "HIRA_KATA", 64 to "KANJI_FIX", 65 to "KANJI_CONVERT", 66 to "YJ"))
            assertEquals(key, AppleInputCatalog.keyForMachine(code, id))
        kotlin.test.assertNotNull(player.exportDump(0x3600, 0x3600).error)
    }

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

    @Test fun unsupportedBankedTargetDoesNotEnableIosPackageImport() {
        val result = ApplePlayerFactory.preparePackage(
            com.digihori.pgp.core.rom.RomPackageManifest(
                format = com.digihori.pgp.core.rom.RomPackageValidator.FORMAT,
                formatVersion = com.digihori.pgp.core.rom.RomPackageValidator.VERSION,
                machineId = "pc-9999", components = emptyList(),
            ), emptyList(),
        )
        kotlin.test.assertNull(result.player)
        assertTrue(kotlin.test.assertNotNull(result.errorMessage).contains("未対応の機種"))
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
        val modes = setOf(com.digihori.pgp.core.api.DisplaySymbol.RUN, com.digihori.pgp.core.api.DisplaySymbol.PRO)
        assertTrue(player.skin.statusRegions.filter { it.symbol !in modes }.all { it.symbol in before.display.symbols })
        assertTrue(com.digihori.pgp.core.api.DisplaySymbol.PRO in before.display.symbols)
        assertFalse(com.digihori.pgp.core.api.DisplaySymbol.RUN in before.display.symbols)

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
