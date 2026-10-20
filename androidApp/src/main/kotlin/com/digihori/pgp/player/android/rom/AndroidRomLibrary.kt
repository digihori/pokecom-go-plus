package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.player.PlayerRomEntry
import com.digihori.pgp.player.PlayerMachineCatalog
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

internal class AndroidRomLibrary(private val filesDir: File) {
    fun install(packageBytes: ByteArray, romSet: RomSet, sourceName: String?) {
        val id = storageId(romSet.machineId)
        val directory = File(filesDir, "rom-library/$id")
        check(directory.mkdirs() || directory.isDirectory)
        val metadata = Properties().apply {
            setProperty("machineId", id)
            setProperty("sourceName", sourceName.orEmpty())
            setProperty("importedAtEpochMillis", System.currentTimeMillis().toString())
            setProperty("packageSize", packageBytes.size.toString())
            setProperty("packageSha256", AndroidRomPackage.run { packageBytes.sha256() })
        }
        val metadataBytes = java.io.ByteArrayOutputStream().also { metadata.store(it, "Pokecom GO Player ROM metadata") }.toByteArray()
        replaceFiles(linkedMapOf(
            File(directory, PACKAGE_FILE) to packageBytes,
            File(directory, METADATA_FILE) to metadataBytes,
            File(filesDir, ACTIVE_MACHINE_FILE) to checkedId(romSet.machineId).encodeToByteArray(),
        ))
    }

    fun load(machineId: MachineId): ByteArray? =
        File(filesDir, "rom-library/${storageId(machineId)}/$PACKAGE_FILE")
            .takeIf(File::isFile)?.readBytes()

    fun activeMachineId(): MachineId? = File(filesDir, ACTIVE_MACHINE_FILE)
        .takeIf(File::isFile)?.readText()?.trim()?.takeIf(String::isNotEmpty)?.let {
            MachineId(it).takeIf { machine -> MachineCatalog.find(machine) != null }
        }

    fun loadActive(): ByteArray? = activeMachineId()?.let(::load)

    fun select(machineId: MachineId) {
        check(load(machineId) != null) { "ROM is not installed" }
        replaceFiles(mapOf(File(filesDir, ACTIVE_MACHINE_FILE) to checkedId(machineId).encodeToByteArray()))
    }

    fun entries(machineIds: List<MachineId>): List<PlayerRomEntry> = machineIds.map { machineId ->
        val directory = File(filesDir, "rom-library/${storageId(machineId)}")
        val metadata = File(directory, METADATA_FILE).takeIf(File::isFile)?.let { file ->
            runCatching { Properties().apply { file.inputStream().use(::load) } }.getOrNull()
        }
        PlayerRomEntry(machineId, File(directory, PACKAGE_FILE).isFile, metadata?.getProperty("sourceName"))
    }

    fun delete(machineId: MachineId) {
        val directory = File(filesDir, "rom-library/${storageId(machineId)}")
        val changes = linkedMapOf<File, ByteArray?>(
            File(directory, PACKAGE_FILE) to null,
            File(directory, METADATA_FILE) to null,
        )
        if (activeMachineId()?.let(PlayerMachineCatalog::romId) == PlayerMachineCatalog.romId(machineId)) changes[File(filesDir, ACTIVE_MACHINE_FILE)] = null
        replaceFiles(changes)
    }

    private fun storageId(machineId: MachineId): String = checkedId(PlayerMachineCatalog.romId(machineId))

    private fun checkedId(machineId: MachineId): String {
        require(MachineCatalog.find(machineId) != null) { "Unknown machine" }
        return machineId.value
    }

    /** Prepare every write before replacing anything; restore old files if a later move fails. */
    private fun replaceFiles(changes: Map<File, ByteArray?>) {
        val previous = changes.keys.associateWith { it.takeIf(File::isFile)?.readBytes() }
        val staged = mutableMapOf<File, File>()
        val changed = mutableListOf<File>()
        try {
            changes.forEach { (file, bytes) ->
                if (bytes != null) {
                    val temp = File(file.parentFile, "${file.name}.tmp")
                    staged[file] = temp
                    writeSynced(temp, bytes)
                }
            }
            changes.forEach { (file, bytes) ->
                if (bytes != null) moveAtomically(staged.getValue(file), file)
                else if (file.exists()) check(file.delete()) { "Could not delete ${file.name}" }
                changed += file
            }
        } catch (failure: Exception) {
            changed.asReversed().forEach { file ->
                runCatching {
                    val bytes = previous[file]
                    if (bytes == null) { if (file.exists()) check(file.delete()) }
                    else {
                        val temp = File(file.parentFile, "${file.name}.rollback")
                        writeSynced(temp, bytes)
                        moveAtomically(temp, file)
                    }
                }.exceptionOrNull()?.let(failure::addSuppressed)
            }
            throw failure
        } finally {
            staged.values.forEach { it.delete() }
        }
    }

    private fun writeSynced(file: File, bytes: ByteArray) {
        file.outputStream().use { output -> output.write(bytes); output.flush(); output.fd.sync() }
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
