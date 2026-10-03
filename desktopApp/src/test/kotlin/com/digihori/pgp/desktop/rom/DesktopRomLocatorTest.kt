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

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("pgp-rom-locator-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
