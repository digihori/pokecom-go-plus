package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import kotlin.test.*

class PlayerSeriesTest {
    private val groups = listOf(listOf("1250", "1251", "1255"), listOf("1260", "1261"), listOf("1401", "1402"))

    private fun rom(group: String): RomSet {
        val internal = ByteArray(0x2000) { 0x4d }
        byteArrayOf(0x79, 0, 0).copyInto(internal) // Original loop, no firmware fixture.
        val imported = when (group) {
            "1251" -> Pc1251ComponentRomImporter.importImages(internal, ByteArray(0x4000))
            "1261" -> Pc1261ComponentRomImporter.importImages(internal, ByteArray(0x8000))
            else -> Pc1401ComponentRomImporter.importImages(internal, ByteArray(0x8000))
        }
        return assertIs<RomImportResult.Success>(imported).romSet
    }

    @Test fun explicitGroupsSharePresentationButRetainRuntimeIdentityAndIndependentRam() {
        for (group in groups) {
            val representative = PlayerMachineCatalog.romId(MachineId("pc-${group.first()}"))
            val source = rom(representative.value.removePrefix("pc-"))
            assertEquals(group.map { "pc-$it" }, PlayerMachineCatalog.models(representative).map { it.id.value })
            for (name in group) {
                val id = MachineId("pc-$name")
                val targeted = assertNotNull(PlayerMachineCatalog.forModel(source, id))
                val session = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(targeted)).session
                assertEquals(id, session.machineId)
                val skin = assertNotNull(PlayerSkinCatalog.forMachine(id))
                assertTrue(id in skin.machineIds)
                assertEquals(PlayerSkinCatalog.forMachine(representative), skin)
                assertEquals(PlayerKeyboardCatalog.forMachine(representative), session.keyboard())
                val keyboard = assertNotNull(session.keyboard())
                val caps = (keyboard.rows + keyboard.functionPanel?.rows.orEmpty()).flatten()
                assertEquals(caps.map { it.key }.toSet(), com.digihori.pgp.input.GamepadKeys.forMachine(id))
                for (cap in caps) {
                    assertIs<InputResult.Accepted>(session.pressKey(cap.key))
                    assertIs<InputResult.Accepted>(session.releaseKey(cap.key))
                }
                session.runner.run(0);session.runner.tick(10_000_000);session.runner.pause()
                session.reset()
                assertEquals(if (representative.value == "pc-1401") OperatingMode.CALCULATOR else OperatingMode.RUN, session.operatingMode)
            }
        }
        assertNull(PlayerMachineCatalog.forModel(rom("1251"), MachineId("pc-1245")))
        assertNull(PlayerMachineCatalog.forModel(rom("1251"), MachineId("pc-1360k")))
    }

    @Test fun hardwareBoundariesBasicRoundTripAndFailedLoadAtomicity() {
        val ranges = mapOf("1250" to Triple(0xc000, 0xc7ff, 0xc6e1), "1251" to Triple(0xb800, 0xc7ff, 0xc6e1),
            "1255" to Triple(0xa000, 0xc7ff, 0xc6e1), "1260" to Triple(0x5800, 0x67ff, 0x66e1),
            "1261" to Triple(0x4000, 0x67ff, 0x66e1), "1401" to Triple(0x3800, 0x47ff, 0x46e1),
            "1402" to Triple(0x2000, 0x47ff, 0x46e1))
        for (name in groups.flatten()) {
            val id = MachineId("pc-$name")
            val source = rom(PlayerMachineCatalog.romId(id).value.removePrefix("pc-"))
            fun fresh() = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(
                assertNotNull(PlayerMachineCatalog.forModel(source, id)))).session
            val session = fresh()
            fun parse(text: String, format: PlayerProgramFormat = PlayerProgramFormat.MEMORY_DUMP) =
                assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(text.encodeToByteArray(), format, id)).input
            val (start, end, pointer) = ranges.getValue(name)
            assertIs<PlayerProgramApplyResult.Success>(session.loadProgram(parse("${start.toString(16)} 5A\n${end.toString(16)} 6B")))
            if (name.startsWith("125")) {
                val extra = session.loadProgram(parse("A000 33"))
                if (name == "1255") assertIs<PlayerProgramApplyResult.Success>(extra)
                else assertIs<PlayerProgramApplyResult.Failure>(extra)
            }
            val before = assertIs<PlayerProgramExportResult.Success>(session.exportMemoryDump(start, start)).copyBytes()
            assertIs<PlayerProgramApplyResult.Failure>(session.loadProgram(parse("${start.toString(16)} 7C\n1FFF 8D")))
            assertContentEquals(before, assertIs<PlayerProgramExportResult.Success>(session.exportMemoryDump(start, start)).copyBytes())
            assertIs<PlayerProgramExportResult.Failure>(session.exportMemoryDump(0x1fff, 0x1fff))
            assertFalse(before.contentEquals(assertIs<PlayerProgramExportResult.Success>(fresh().exportMemoryDump(start, start)).copyBytes()))
            // Synthetic ROM does not initialize BASIC pointers; initialize them as a host RAM load.
            val basicStart = if (name == "1260") 0x5880 else if (name == "1261") 0x4080 else start
            assertIs<PlayerProgramApplyResult.Success>(session.loadProgram(parse("${pointer.toString(16)} " + listOf(basicStart and 255, basicStart shr 8, basicStart and 255, basicStart shr 8).joinToString(" ") { it.toString(16).padStart(2, '0') })))
            val basic = parse("10 PRINT 1\n20 END\n", PlayerProgramFormat.BASIC)
            assertIs<PlayerProgramApplyResult.Success>(session.loadProgram(basic))
            val saved = assertIs<PlayerProgramExportResult.Success>(session.exportBasic()).copyBytes()
            assertTrue(saved.decodeToString().contains("PRINT"))
            assertIs<PlayerProgramApplyResult.Success>(session.loadProgram(assertIs<PlayerProgramParseResult.Success>(
                PlayerProgramParser.parse(saved, PlayerProgramFormat.BASIC, id)).input))
            assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(session.exportBasic()).copyBytes())
            val large = parse((1..700).joinToString("\n") { "$it PRINT \"${"A".repeat(40)}\"" }, PlayerProgramFormat.BASIC)
            assertIs<PlayerProgramApplyResult.Failure>(session.loadProgram(large))
            assertContentEquals(saved, assertIs<PlayerProgramExportResult.Success>(session.exportBasic()).copyBytes())
        }
    }

    @Test fun pendingSelectionDefaultsAndInvalidPreferencesDoNotChangeRuntimeModel() {
        val base = MachineId("pc-1251")
        assertEquals(base, PlayerMachineCatalog.selectedModel(base, emptyMap()))
        assertEquals(base, PlayerMachineCatalog.selectedModel(base, mapOf(base to MachineId("pc-1401"))))
        val source = rom("1251")
        val active = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(source)).session
        val preferences = mapOf(base to MachineId("pc-1255"))
        assertEquals(MachineId("pc-1255"), PlayerMachineCatalog.selectedModel(base, preferences))
        assertEquals(base, active.machineId)
    }
}
