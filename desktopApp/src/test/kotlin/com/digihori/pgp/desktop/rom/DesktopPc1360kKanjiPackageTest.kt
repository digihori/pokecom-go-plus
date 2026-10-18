package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import com.digihori.pgp.core.rom.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DesktopPc1360kKanjiPackageTest {
    @Test
    fun roundTripsAllSelectorsAndKeepsAnalysisMachineIdentity() {
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        val images = List(4) { selector -> ByteArray(65536) { (it xor (selector * 0x5a)).toByte() } }
        val converted = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.createPackage(
            id, ByteArray(8192), ByteArray(131072), images,
        ))
        val read = assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(converted.packageBytes))
        assertEquals(13, read.romSet.components.size)
        for (selector in 0..3) {
            val component = assertNotNull(read.romSet.component(Pc1360RomDefinition.kanjiId(selector)))
            assertEquals(RomRole.KANJI, component.role); assertContentEquals(images[selector], component.copyBytes())
        }
        val partial = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.createPackage(id, ByteArray(8192), ByteArray(131072), images.take(2)))
        assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(partial.packageBytes))
        assertEquals(id, assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(converted.packageBytes)).session.machineId)
        assertIs<DesktopRomLoadResult.Failure>(DesktopRomLoader.loadPackageForMachine(converted.packageBytes, Pc1360RomDefinition.MACHINE_ID))
        assertIs<DesktopRomPackageConversionResult.Failure>(DesktopRomPackageConverter.createPackage(Pc1360RomDefinition.MACHINE_ID, ByteArray(8192), ByteArray(131072), images))
    }

    @Test
    fun creatorRejectsIncompletePairAndDoesNotOverwriteKanjiInputs() {
        val root = Files.createTempDirectory("pc1360k-kanji-package-").toFile()
        try {
            val internal = File(root, "internal.bin").apply { writeBytes(ByteArray(8192)) }
            val external = File(root, "external.bin").apply { writeBytes(ByteArray(131072)) }
            val kanji = List(4) { File(root, "kanji-$it.bin").apply { writeBytes(ByteArray(65536)) } }
            val target = File(root, "pc-1360k.pgrom")
            val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
            assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(id, internal, external, target, kanjiFiles = kanji.take(1)) }
            assertFalse(target.exists())
            assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(id, internal, external, target, kanjiFiles = kanji.take(3)) }
            assertFalse(DesktopRomSetCreator.validKanjiInputs(listOf(null, null, kanji[2], kanji[3])))
            assertFalse(DesktopRomSetCreator.validKanjiInputs(listOf(kanji[0], kanji[1], kanji[2], null)))
            assertTrue(DesktopRomSetCreator.validKanjiInputs(listOf(kanji[0], kanji[1], null, null)))
            assertTrue(DesktopRomSetCreator.validKanjiInputs(kanji))
            assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(id, internal, external, kanji[0], overwrite = true, kanjiFiles = kanji) }
            assertEquals(65536, kanji[0].length().toInt())
            DesktopRomSetCreator.create(id, internal, external, target, kanjiFiles = kanji)
            assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(target.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test
    fun createAndLoadLocalPackageWithCapturedFirmware() {
        if (System.getenv("PGP_VERIFY_PC1360K_KANJI") != "1") return
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "local-data/roms/pc-1360k/pc1360kmem.bin").isFile }
        val directory = File(root, "local-data/roms/pc-1360k")
        val kanji = List(4) { File(directory, "kanji-selector-$it.bin") }
        val target = DesktopRomSetCreator.create(Pc1360RomDefinition.PC1360K_MACHINE_ID,
            File(directory, "pc1360kmem.bin"), File(directory, "pc1360kbank.bin"),
            File(directory, "pc-1360k.pgrom"), overwrite = true, kanjiFiles = kanji,
        )
        val packageRead = assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(target.readBytes()))
        assertEquals(13, packageRead.romSet.components.size)
        assertEquals(kanji.map(File::getCanonicalFile), DesktopRomLocator.pc1360kKanjiFiles(directory).map(File::getCanonicalFile))
        assertEquals(target.canonicalFile, DesktopRomLocator.findPc1360kRom(root)?.canonicalFile)
        val session = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(target.readBytes())).session
        assertEquals(Pc1360RomDefinition.PC1360K_MACHINE_ID, session.machineId)
        assertEquals(Pc1360RomDefinition.PC1360K_MACHINE_ID, assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadLegacyImage(
            Pc1360RomDefinition.PC1360K_MACHINE_ID, File(directory, "pc1360kmem.bin").readBytes(),
            bankImage = File(directory, "pc1360kbank.bin").readBytes(), kanjiImages = kanji.map(File::readBytes),
        )).session.machineId)
    }
}
