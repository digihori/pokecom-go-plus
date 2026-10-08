package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

internal class AndroidRomLibrary(private val filesDir: File) {
    fun install(packageBytes: ByteArray, romSet: RomSet, sourceName: String?) {
        val directory = File(filesDir, "rom-library/${romSet.machineId.value}")
        check(directory.mkdirs() || directory.isDirectory)
        val destination = File(directory, PACKAGE_FILE)
        val temporary = File(directory, "$PACKAGE_FILE.tmp")
        temporary.outputStream().use { output ->
            output.write(packageBytes)
            output.flush()
            output.fd.sync()
        }
        moveAtomically(temporary, destination)

        val metadata = Properties().apply {
            setProperty("machineId", romSet.machineId.value)
            setProperty("sourceName", sourceName.orEmpty())
            setProperty("importedAtEpochMillis", System.currentTimeMillis().toString())
            setProperty("packageSize", packageBytes.size.toString())
            setProperty("packageSha256", AndroidRomPackage.run { packageBytes.sha256() })
        }
        val metadataTemp = File(directory, "$METADATA_FILE.tmp")
        metadataTemp.outputStream().use { output ->
            metadata.store(output, "Pokecom GO Player ROM metadata")
            output.flush()
            output.fd.sync()
        }
        moveAtomically(metadataTemp, File(directory, METADATA_FILE))

        val activeTemp = File(filesDir, "$ACTIVE_MACHINE_FILE.tmp")
        activeTemp.writeText(romSet.machineId.value)
        moveAtomically(activeTemp, File(filesDir, ACTIVE_MACHINE_FILE))
    }

    fun load(machineId: MachineId): ByteArray? =
        File(filesDir, "rom-library/${machineId.value}/$PACKAGE_FILE")
            .takeIf(File::isFile)?.readBytes()

    fun loadActive(): ByteArray? {
        val machineId = File(filesDir, ACTIVE_MACHINE_FILE)
            .takeIf(File::isFile)?.readText()?.trim()?.takeIf(String::isNotEmpty)
            ?: return null
        return load(MachineId(machineId))
    }

    private fun moveAtomically(source: File, destination: File) {
        try {
            Files.move(
                source.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val PACKAGE_FILE = "active.pgrom"
        const val METADATA_FILE = "metadata.properties"
        const val ACTIVE_MACHINE_FILE = "active-rom-machine-id"
    }
}
