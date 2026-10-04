package com.digihori.pgp.desktop.rom

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DesktopRomLocatorTest {
    @Test
    fun configuredRomPathTakesPriority() {
        withTemporaryDirectory { root ->
            val configured = File(root, "custom.bin").apply { writeBytes(byteArrayOf(1)) }
            val default = File(root, DesktopRomLocator.DEFAULT_PC1245_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(2))
            }

            assertEquals(
                configured.canonicalFile,
                DesktopRomLocator.findPc1245Rom(configured.absolutePath, root)?.canonicalFile,
            )
            check(default.isFile)
        }
    }

    @Test
    fun findsDefaultRomFromAChildWorkingDirectory() {
        withTemporaryDirectory { root ->
            val rom = File(root, DesktopRomLocator.DEFAULT_PC1245_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(1))
            }
            val child = File(root, "desktopApp/build").apply { mkdirs() }

            assertEquals(
                rom.canonicalFile,
                DesktopRomLocator.findPc1245Rom(configuredPath = null, workingDirectory = child)?.canonicalFile,
            )
        }
    }

    @Test
    fun returnsNullWhenNoRomExists() {
        withTemporaryDirectory { root ->
            assertNull(DesktopRomLocator.findPc1245Rom(configuredPath = null, workingDirectory = root))
        }
    }

    @Test
    fun findsPc1251WhenItIsTheOnlyAvailableDefaultRom() {
        withTemporaryDirectory { root ->
            val rom = File(root, DesktopRomLocator.DEFAULT_PC1251_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(1))
            }

            val selection = requireNotNull(
                DesktopRomLocator.findFirstAvailableRom(
                    pc1245ConfiguredPath = null,
                    pc1251ConfiguredPath = null,
                    workingDirectory = root,
                ),
            )

            assertEquals(rom.canonicalFile, selection.file.canonicalFile)
            assertEquals("pc-1251", selection.machineId.value)
        }
    }

    @Test
    fun keepsPc1245AsTheInitialDefaultWhenBothRomsExist() {
        withTemporaryDirectory { root ->
            val pc1245 = File(root, DesktopRomLocator.DEFAULT_PC1245_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(1))
            }
            File(root, DesktopRomLocator.DEFAULT_PC1251_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(2))
            }

            val selection = requireNotNull(
                DesktopRomLocator.findFirstAvailableRom(null, null, root),
            )

            assertEquals(pc1245.canonicalFile, selection.file.canonicalFile)
            assertEquals("pc-1245", selection.machineId.value)
        }
    }

    @Test
    fun explicitPc1251PathTakesPriorityOverDefaultPc1245Rom() {
        withTemporaryDirectory { root ->
            File(root, DesktopRomLocator.DEFAULT_PC1245_ROM_PATH).apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(1))
            }
            val configuredPc1251 = File(root, "roms/custom-1251.bin").apply {
                parentFile.mkdirs()
                writeBytes(byteArrayOf(2))
            }

            val selection = requireNotNull(
                DesktopRomLocator.findFirstAvailableRom(
                    pc1245ConfiguredPath = null,
                    pc1251ConfiguredPath = configuredPc1251.path,
                    workingDirectory = root,
                ),
            )

            assertEquals(configuredPc1251.canonicalFile, selection.file.canonicalFile)
            assertEquals("pc-1251", selection.machineId.value)
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("pgp-rom-locator-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
