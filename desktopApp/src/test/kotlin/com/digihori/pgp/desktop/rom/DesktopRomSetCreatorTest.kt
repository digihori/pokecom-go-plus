package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.rom.MachineId
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DesktopRomSetCreatorTest {
    @Test fun createsValidPackageAndPreservesInputs() = withFiles { root ->
        val internal = File(root, "internal.bin").apply { writeBytes(ByteArray(8192)) }
        val external = File(root, "external.bin").apply { writeBytes(ByteArray(32768)) }
        val output = DesktopRomSetCreator.destination(File(root, "test"))
        DesktopRomSetCreator.create(MachineId("pc-1401"), internal, external, output)
        assertEquals("test.pgrom", output.name)
        assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(output.readBytes()))
        assertEquals(8192L, internal.length())
        assertEquals(32768L, external.length())
        assertEquals(3, root.listFiles()!!.size)
    }

    @Test fun rejectsBadSizeAndDoesNotOverwriteUntilConfirmed() = withFiles { root ->
        val internal = File(root, "internal.bin").apply { writeBytes(ByteArray(8192)) }
        val external = File(root, "external.bin").apply { writeBytes(ByteArray(16384)) }
        val output = File(root, "existing.pgrom").apply { writeText("keep") }
        assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(MachineId("pc-1401"), internal, external, output, true) }
        assertEquals("keep", output.readText())
        assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(MachineId("pc-1245"), internal, external, output) }
        assertEquals("keep", output.readText())
        assertFailsWith<IllegalArgumentException> { DesktopRomSetCreator.create(MachineId("pc-1245"), internal, external, internal, true) }
        DesktopRomSetCreator.create(MachineId("pc-1245"), internal, external, output, true)
        assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(output.readBytes()))
    }

    private fun withFiles(block: (File) -> Unit) {
        val root = Files.createTempDirectory("rom-set-test-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }
}
