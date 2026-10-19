package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360ComponentRomImporter
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.player.*
import com.digihori.pgp.player.android.PlayerEntryPolicy
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class Pc1360RomIntegrationTest {
    private val ids = listOf("pc-1245", "pc-1401", "pc-1350", "pc-1251", "pc-1261", "pc-1360").map(::MachineId)
    private val id = MachineId("pc-1360")
    private fun rom(machine: MachineId, revision: Byte = 0): RomSet {
        val internal = ByteArray(0x2000) { 0x4d }
        byteArrayOf(0x79, 0, 0).copyInto(internal) // Original JP 0000; no firmware data.
        val external = ByteArray(0x8000) { revision }
        return when (machine.value) {
            "pc-1245" -> assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(
                ByteArray(65536).also { internal.copyInto(it); external.copyInto(it, 0x8000) })).romSet
            "pc-1401" -> assertIs<RomImportResult.Success>(Pc1401ComponentRomImporter.importImages(internal, external)).romSet
            "pc-1360" -> assertIs<RomImportResult.Success>(Pc1360ComponentRomImporter.importImages(internal, List(8) { ByteArray(0x4000) { revision } })).romSet
            "pc-1261" -> assertIs<RomImportResult.Success>(Pc1261ComponentRomImporter.importImages(internal, external)).romSet
            "pc-1251" -> assertIs<RomImportResult.Success>(Pc1251ComponentRomImporter.importImages(internal, external.copyOf(0x4000))).romSet
            else -> assertIs<RomImportResult.Success>(Pc1350ComponentRomImporter.importImages(internal, external)).romSet
        }
    }

    @Test fun explicitLegacySelectionAndEveryCrossMachinePackageRejection() {
        val legacy = ByteArray(65536)
        assertNull(PlayerRomDecoder.decode(legacy, id))
        for (size in listOf(0, 32767, 32769, 65535, 65537)) assertNull(PlayerRomDecoder.decode(ByteArray(size), id))
        assertNull(PlayerRomDecoder.decode(ByteArray(32768), id))
        assertNull(PlayerRomDecoder.decode(legacy, MachineId("pc-1360k")))
        for (source in ids) {
            val bytes = AndroidRomPackage.write(rom(source))
            for (target in ids) {
                val decoded = PlayerRomDecoder.decode(bytes, target)
                if (source == target) assertEquals(source, assertNotNull(decoded).machineId) else assertNull(decoded)
            }
            assertNull(PlayerRomDecoder.decode(bytes.copyOf(bytes.size / 2), source))
        }
    }

    @Test fun restoreReplacementAndDeletionPreserveTheOtherFiveLibraries() = withLibrary { root ->
        val library = AndroidRomLibrary(root)
        val packages = ids.associateWith { machine ->
            val rom = rom(machine)
            AndroidRomPackage.write(rom).also { library.install(it, rom, "${machine.value}.pgrom") }
        }
        val restored = AndroidRomLibrary(root)
        assertEquals(id, restored.activeMachineId())
        assertTrue(restored.entries(ids).all { it.installed })
        assertEquals(ids, restored.entries(ids).map { it.machineId })
        val player = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(
            assertNotNull(PlayerRomDecoder.decode(assertNotNull(restored.loadActive()), id)))).session
        assertEquals(id, player.machineId)
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        val replacement = rom(id, 1)
        restored.install(AndroidRomPackage.write(replacement), replacement, "replacement.pgrom")
        assertEquals("replacement.pgrom", restored.entries(listOf(id)).single().sourceName)
        assertFalse(packages.getValue(id).contentEquals(restored.load(id)))
        for (other in ids.filter { it != id }) assertContentEquals(packages.getValue(other), restored.load(other))
        restored.select(ids[1])
        restored.delete(id) // inactive deletion must retain the active 1401 selection
        assertEquals(ids[1], restored.activeMachineId())
        assertNull(restored.load(id))
        restored.install(packages.getValue(id), rom(id), "original.pgrom")
        restored.delete(id) // active deletion clears only the 1360 selection
        assertNull(restored.activeMachineId())
        restored.select(ids[0])
        assertContentEquals(packages.getValue(ids[0]), restored.loadActive())
        assertContentEquals(packages.getValue(ids[1]), restored.load(ids[1]))
    }

    @Test fun failedReplacementRollsBack1360WithoutChangingOtherRomOrSelection() = withLibrary { root ->
        val library = AndroidRomLibrary(root)
        val packages = ids.associateWith { machine ->
            val rom = rom(machine)
            AndroidRomPackage.write(rom).also { library.install(it, rom, "original.pgrom") }
        }
        library.select(ids[1])
        val metadata = File(root, "rom-library/pc-1360/metadata.properties")
        assertTrue(metadata.delete())
        assertTrue(metadata.mkdir())
        File(metadata, "blocker").writeText("block")
        val replacement = rom(id, 1)
        assertFails { library.install(AndroidRomPackage.write(replacement), replacement, "replacement.pgrom") }
        assertEquals(ids[1], library.activeMachineId())
        packages.forEach { (machine, bytes) -> assertContentEquals(bytes, library.load(machine)) }
    }

    @Test fun existingEntryPolicyKeepsSameSessionRamAndNewSessionExecutionIntent() {
        val player = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom(id))).session
        val input = assertIs<PlayerProgramParseResult.Success>(PlayerProgramParser.parse(
            "C000 5A".encodeToByteArray(), PlayerProgramFormat.MEMORY_DUMP, id)).input
        assertIs<PlayerProgramApplyResult.Success>(player.loadProgram(input))
        val policy = PlayerEntryPolicy()
        assertTrue(policy.shouldRun)
        player.runner.run(0)
        assertIs<ExecutionStatus.Ready>(player.runner.tick(10_000_000).runResult?.status)
        policy.leavingPlayer(player.runner.state, false)
        player.runner.pause()
        assertTrue(policy.shouldRun)
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        player.runner.run(1_000_000_000)
        player.runner.pause()
        policy.leavingPlayer(player.runner.state, false)
        assertFalse(policy.shouldRun)
        val saved = assertIs<PlayerProgramExportResult.Success>(player.exportMemoryDump(0xc000, 0xc000))
        val savedImage = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(saved.copyBytes().decodeToString())).image
        assertContentEquals(byteArrayOf(0x5a), savedImage.segments.single().copyBytes())
        val switched = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom(id))).session
        policy.newSession()
        assertTrue(policy.shouldRun)
        assertEquals(PlayerRunState.PAUSED, switched.runner.state)
        val after = assertIs<PlayerProgramExportResult.Success>(switched.exportMemoryDump(0xc000, 0xc000))
        val freshImage = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse(after.copyBytes().decodeToString())).image
        assertContentEquals(byteArrayOf(0), freshImage.segments.single().copyBytes())
    }

    private fun withLibrary(action: (File) -> Unit) {
        val root = Files.createTempDirectory("player-pc1360-test-").toFile()
        try { action(root) } finally { root.deleteRecursively() }
    }
}
