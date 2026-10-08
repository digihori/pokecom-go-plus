package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull

class AndroidRomLibraryTest {
    @Test
    fun `installed package is loaded from app private root`() {
        val root = Files.createTempDirectory("player-rom-library-test")
        try {
            val imported = Pc1245FlatRomImporter.importImage(
                ByteArray(Pc1245RomDefinition.COMPACT_LEGACY_IMAGE_SIZE),
            ) as RomImportResult.Success
            val packageBytes = AndroidRomPackage.write(imported.romSet)
            val library = AndroidRomLibrary(root.toFile())

            library.install(packageBytes, imported.romSet, "owned-rom.bin")

            assertContentEquals(
                packageBytes,
                assertNotNull(library.load(Pc1245RomDefinition.MACHINE_ID)),
            )
            assertContentEquals(packageBytes, assertNotNull(library.loadActive()))
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
