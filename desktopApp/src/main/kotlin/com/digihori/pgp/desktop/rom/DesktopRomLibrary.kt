package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineFamily
import com.digihori.pgp.core.rom.MachineId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal class DesktopRomLibrary(
    private val root: File = defaultRoot(),
) {
    fun installedMachineIds(): Set<MachineId> = MachineCatalog.definitions.mapNotNullTo(linkedSetOf()) {
        it.id.takeIf(::isInstalled)
    }

    fun isInstalled(machineId: MachineId): Boolean {
        val packageFile = packageFile(machineId)
        if (!packageFile.isFile) return false
        val read = runCatching { DesktopRomPackage.read(packageFile.readBytes()) }.getOrNull()
        return read is DesktopRomPackageReadResult.Success &&
            MachineCatalog.find(read.romSet.machineId)?.family == MachineCatalog.find(machineId)?.family
    }

    fun packageFile(machineId: MachineId): File = File(File(root, storageId(machineId)), PACKAGE_NAME)

    fun metadata(machineId: MachineId): DesktopRomMetadata? = runCatching {
        val objectValue = Json.parseToJsonElement(
            File(File(root, storageId(machineId)), METADATA_NAME).readText(),
        ).jsonObject
        DesktopRomMetadata(
            sourceName = objectValue.getValue("sourceName").jsonPrimitive.content,
            importedAt = objectValue.getValue("importedAt").jsonPrimitive.content,
            packageSha256 = objectValue.getValue("packageSha256").jsonPrimitive.content,
        )
    }.getOrNull()

    fun install(packageBytes: ByteArray, sourceName: String): DesktopRomLibraryResult {
        val read = DesktopRomPackage.read(packageBytes)
        if (read is DesktopRomPackageReadResult.Failure) {
            return DesktopRomLibraryResult.Failure("Invalid ROM package: ${read.error}")
        }
        val machineId = (read as DesktopRomPackageReadResult.Success).romSet.machineId
        if (DesktopRomLoader.loadPackage(packageBytes) is DesktopRomLoadResult.Failure) {
            return DesktopRomLibraryResult.Failure("The ROM package does not create a valid emulator session")
        }
        val storageId = storageId(machineId)
        val machineDirectory = File(root, storageId)
        return runCatching {
            Files.createDirectories(machineDirectory.toPath())
            replaceAtomically(packageFile(machineId), packageBytes)
            val metadata = buildJsonObject {
                put("machineId", machineId.value)
                put("sourceName", sourceName)
                put("importedAt", Instant.now().toString())
                put("packageSize", packageBytes.size)
                put("packageSha256", packageBytes.sha256())
                put("validated", true)
            }
            replaceAtomically(
                File(machineDirectory, METADATA_NAME),
                Json { prettyPrint = true }.encodeToString(metadata).encodeToByteArray(),
            )
            DesktopRomLibraryResult.Success(machineId, packageFile(machineId))
        }.getOrElse { DesktopRomLibraryResult.Failure(it.message ?: it::class.simpleName.orEmpty()) }
    }

    fun remove(machineId: MachineId): Boolean {
        val directory = File(root, storageId(machineId))
        val packageDeleted = !packageFile(machineId).exists() || packageFile(machineId).delete()
        File(directory, METADATA_NAME).delete()
        directory.delete()
        return packageDeleted
    }

    private fun replaceAtomically(destination: File, bytes: ByteArray) {
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.writeBytes(bytes)
        try {
            Files.move(
                temporary.toPath(), destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: Exception) {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }

    private companion object {
        const val PACKAGE_NAME = "active.pgrom"
        const val METADATA_NAME = "metadata.json"

        fun defaultRoot(): File {
            val home = System.getProperty("user.home")
            val os = System.getProperty("os.name").lowercase()
            val dataRoot = when {
                os.contains("mac") -> File(home, "Library/Application Support")
                os.contains("win") -> System.getenv("APPDATA")?.let(::File) ?: File(home, "AppData/Roaming")
                else -> System.getenv("XDG_DATA_HOME")?.let(::File) ?: File(home, ".local/share")
            }
            return File(dataRoot, "Pokecom GO Studio/rom-library")
        }
    }

    private fun storageId(machineId: MachineId): String =
        if (MachineCatalog.find(machineId)?.family == MachineFamily.PC_1251) {
            "pc-1251-family"
        } else {
            machineId.value
        }
}

internal sealed interface DesktopRomLibraryResult {
    data class Success(val machineId: MachineId, val file: File) : DesktopRomLibraryResult
    data class Failure(val message: String) : DesktopRomLibraryResult
}

internal data class DesktopRomMetadata(
    val sourceName: String,
    val importedAt: String,
    val packageSha256: String,
)
