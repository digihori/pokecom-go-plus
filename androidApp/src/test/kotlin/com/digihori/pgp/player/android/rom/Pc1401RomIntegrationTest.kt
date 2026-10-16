package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.*
import java.nio.file.Files
import kotlin.test.*

class Pc1401RomIntegrationTest {
    private val pc1245 = MachineId("pc-1245")
    private val pc1401 = MachineId("pc-1401")
    private fun rom1401(fill: Byte = 0) = assertIs<RomImportResult.Success>(
        Pc1401ComponentRomImporter.importImages(ByteArray(0x2000) { fill }, ByteArray(0x8000) { fill }),
    ).romSet

    @Test fun sameLegacySizeRequiresExplicitMachineAndPackageCannotBeRelabeled() {
        val legacy = ByteArray(65536)
        assertEquals(pc1245, assertNotNull(PlayerRomDecoder.decode(legacy, pc1245)).machineId)
        assertEquals(pc1401, assertNotNull(PlayerRomDecoder.decode(legacy, pc1401)).machineId)
        assertEquals(MachineId("pc-1350"), assertNotNull(PlayerRomDecoder.decode(legacy, MachineId("pc-1350"))).machineId)
        assertNull(PlayerRomDecoder.decode(legacy, MachineId("pc-1360")))
        assertNull(PlayerRomDecoder.decode(ByteArray(32768), pc1401))
        val packaged = AndroidRomPackage.write(rom1401())
        assertEquals(pc1401, assertNotNull(PlayerRomDecoder.decode(packaged, pc1401)).machineId)
        assertNull(PlayerRomDecoder.decode(packaged, pc1245))
        assertNull(PlayerRomDecoder.decode(packaged.copyOf(packaged.size / 2), pc1401))
    }

    @Test fun restoreReplaceSelectAndDeleteKeepOtherMachineRomIntact() {
        val root = Files.createTempDirectory("player-pc1401-library").toFile()
        try {
            val first = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(ByteArray(32768))).romSet
            val firstBytes = AndroidRomPackage.write(first)
            val second = rom1401()
            val secondBytes = AndroidRomPackage.write(second)
            AndroidRomLibrary(root).apply {
                install(firstBytes, first, "1245.bin")
                install(secondBytes, second, "1401.pgrom")
                select(pc1245)
            }
            val restored = AndroidRomLibrary(root)
            assertEquals(pc1245, restored.activeMachineId())
            assertTrue(restored.entries(listOf(pc1245, pc1401)).all { it.installed })
            restored.select(pc1401)
            val session = assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(
                assertNotNull(PlayerRomDecoder.decode(assertNotNull(restored.loadActive()), pc1401)),
            )).session
            assertEquals(pc1401, session.machineId)
            assertEquals(PlayerRunState.PAUSED, session.runner.state)
            val replacement = rom1401(1)
            restored.install(AndroidRomPackage.write(replacement), replacement, "replacement.pgrom")
            assertEquals("replacement.pgrom", restored.entries(listOf(pc1401)).single().sourceName)
            assertFalse(secondBytes.contentEquals(restored.load(pc1401)))
            assertContentEquals(firstBytes, restored.load(pc1245))
            restored.delete(pc1401)
            assertNull(restored.activeMachineId())
            assertNull(restored.load(pc1401))
            restored.select(pc1245)
            assertContentEquals(firstBytes, restored.loadActive())
        } finally { root.deleteRecursively() }
    }

    @Test fun failed1401ReplacementKeepsPreviousRomAnd1245Selection() {
        val root = Files.createTempDirectory("player-pc1401-rollback").toFile()
        try {
            val library = AndroidRomLibrary(root)
            val first = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(ByteArray(32768))).romSet
            val second = rom1401()
            val before = AndroidRomPackage.write(second)
            library.install(AndroidRomPackage.write(first), first, "1245.bin")
            library.install(before, second, "1401.pgrom")
            library.select(pc1245)
            val metadata = java.io.File(root, "rom-library/pc-1401/metadata.properties")
            assertTrue(metadata.delete()); assertTrue(metadata.mkdir())
            java.io.File(metadata, "blocker").writeText("block")
            val replacement = rom1401(1)
            assertFails { library.install(AndroidRomPackage.write(replacement), replacement, "replacement.pgrom") }
            assertContentEquals(before, library.load(pc1401))
            assertEquals(pc1245, library.activeMachineId())
        } finally { root.deleteRecursively() }
    }
}
