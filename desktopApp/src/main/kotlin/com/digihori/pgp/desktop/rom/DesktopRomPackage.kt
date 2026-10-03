package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.rom.RomPackageComponentManifest
import com.digihori.pgp.core.rom.RomPackageEntryInfo
import com.digihori.pgp.core.rom.RomPackageManifest
import com.digihori.pgp.core.rom.RomPackageValidationError
import com.digihori.pgp.core.rom.RomPackageValidationResult
import com.digihori.pgp.core.rom.RomPackageValidator
import com.digihori.pgp.core.rom.RomPackageWarning
import com.digihori.pgp.core.rom.RomSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object DesktopRomPackage {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        prettyPrint = true
    }

    fun read(packageBytes: ByteArray): DesktopRomPackageReadResult {
        if (packageBytes.size > MAX_PACKAGE_BYTES) {
            return DesktopRomPackageReadResult.Failure(
                DesktopRomPackageError.PackageTooLarge(MAX_PACKAGE_BYTES, packageBytes.size),
            )
        }
        val archive = readArchive(packageBytes)
        if (archive is ArchiveReadResult.Failure) {
            return DesktopRomPackageReadResult.Failure(archive.error)
        }
        val entries = (archive as ArchiveReadResult.Success).entries
        val manifestBytes = entries[MANIFEST_PATH]
            ?: return DesktopRomPackageReadResult.Failure(DesktopRomPackageError.MissingManifest)
        val manifest = try {
            json.decodeFromString<RomPackageManifest>(manifestBytes.decodeToString(throwOnInvalidSequence = true))
        } catch (error: IllegalArgumentException) {
            return DesktopRomPackageReadResult.Failure(
                DesktopRomPackageError.InvalidManifest(error.message ?: "Invalid UTF-8 or JSON"),
            )
        } catch (error: SerializationException) {
            return DesktopRomPackageReadResult.Failure(
                DesktopRomPackageError.InvalidManifest(error.message ?: "Invalid JSON"),
            )
        }

        val content = entries.filterKeys { it != MANIFEST_PATH }.mapValues { (path, bytes) ->
            RomPackageEntryInfo(path, bytes.size, bytes.sha256()) to bytes
        }
        return when (val validated = RomPackageValidator.validate(manifest, content)) {
            is RomPackageValidationResult.Failure -> DesktopRomPackageReadResult.Failure(
                DesktopRomPackageError.Validation(validated.errors),
            )
            is RomPackageValidationResult.Success -> DesktopRomPackageReadResult.Success(
                validated.romSet,
                manifest.title,
                validated.warnings,
            )
        }
    }

    fun write(romSet: RomSet, title: String? = null): ByteArray {
        val components = romSet.components.map { component ->
            val bytes = component.copyBytes()
            RomPackageComponentManifest(
                id = component.id.value,
                role = component.role.value,
                file = "rom/${component.id.value}.bin",
                size = bytes.size,
                sha256 = bytes.sha256(),
            ) to bytes
        }
        val manifest = RomPackageManifest(
            format = RomPackageValidator.FORMAT,
            formatVersion = RomPackageValidator.VERSION,
            machineId = romSet.machineId.value,
            title = title,
            components = components.map { it.first },
        )
        return ByteArrayOutputStream().use { output ->
            ZipOutputStream(output).use { zip ->
                zip.writeEntry(MANIFEST_PATH, json.encodeToString(manifest).encodeToByteArray())
                components.forEach { (component, bytes) -> zip.writeEntry(component.file, bytes) }
            }
            output.toByteArray()
        }
    }

    private fun readArchive(packageBytes: ByteArray): ArchiveReadResult {
        val entries = linkedMapOf<String, ByteArray>()
        var entryCount = 0
        var expandedSize = 0
        try {
            ZipInputStream(ByteArrayInputStream(packageBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entryCount > MAX_ENTRY_COUNT) {
                        return ArchiveReadResult.Failure(DesktopRomPackageError.TooManyEntries(MAX_ENTRY_COUNT))
                    }
                    val path = entry.name
                    if (!path.isSafeArchivePath()) {
                        return ArchiveReadResult.Failure(DesktopRomPackageError.UnsafePath(path))
                    }
                    if (path in entries) {
                        return ArchiveReadResult.Failure(DesktopRomPackageError.DuplicateEntry(path))
                    }
                    if (!entry.isDirectory) {
                        val remainingPackageBytes = MAX_EXPANDED_BYTES - expandedSize
                        val bytes = zip.readBounded(minOf(MAX_ENTRY_BYTES, remainingPackageBytes))
                            ?: return ArchiveReadResult.Failure(
                                DesktopRomPackageError.EntryTooLarge(
                                    path,
                                    minOf(MAX_ENTRY_BYTES, remainingPackageBytes),
                                ),
                            )
                        entries[path] = bytes
                        expandedSize += bytes.size
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: ZipException) {
            return ArchiveReadResult.Failure(
                DesktopRomPackageError.InvalidZip(error.message ?: "Invalid ZIP archive"),
            )
        }
        return ArchiveReadResult.Success(entries)
    }

    private fun ZipInputStream.readBounded(maxBytes: Int): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.writeEntry(path: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(path))
        write(bytes)
        closeEntry()
    }

    private fun String.isSafeArchivePath(): Boolean =
        isNotEmpty() && !startsWith('/') && '\\' !in this &&
            removeSuffix("/").split('/').none { it.isEmpty() || it == "." || it == ".." }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }

    private const val MANIFEST_PATH = "manifest.json"
    private const val MAX_PACKAGE_BYTES = 32 * 1024 * 1024
    private const val MAX_EXPANDED_BYTES = 32 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
    private const val MAX_ENTRY_COUNT = 128
}

internal sealed interface DesktopRomPackageReadResult {
    data class Success(
        val romSet: RomSet,
        val title: String?,
        val warnings: List<RomPackageWarning>,
    ) : DesktopRomPackageReadResult

    data class Failure(val error: DesktopRomPackageError) : DesktopRomPackageReadResult
}

internal sealed interface DesktopRomPackageError {
    data class PackageTooLarge(val maximum: Int, val actual: Int) : DesktopRomPackageError
    data class TooManyEntries(val maximum: Int) : DesktopRomPackageError
    data class EntryTooLarge(val path: String, val maximum: Int) : DesktopRomPackageError
    data class UnsafePath(val path: String) : DesktopRomPackageError
    data class DuplicateEntry(val path: String) : DesktopRomPackageError
    data class InvalidZip(val reason: String) : DesktopRomPackageError
    data object MissingManifest : DesktopRomPackageError
    data class InvalidManifest(val reason: String) : DesktopRomPackageError
    data class Validation(val errors: List<RomPackageValidationError>) : DesktopRomPackageError
}

private sealed interface ArchiveReadResult {
    data class Success(val entries: Map<String, ByteArray>) : ArchiveReadResult
    data class Failure(val error: DesktopRomPackageError) : ArchiveReadResult
}
