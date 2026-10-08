package com.digihori.pgp.player.android.rom

import com.digihori.pgp.core.rom.RomPackageComponentManifest
import com.digihori.pgp.core.rom.RomPackageEntryInfo
import com.digihori.pgp.core.rom.RomPackageManifest
import com.digihori.pgp.core.rom.RomPackageValidationResult
import com.digihori.pgp.core.rom.RomPackageValidator
import com.digihori.pgp.core.rom.RomSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object AndroidRomPackage {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        prettyPrint = true
    }

    fun read(packageBytes: ByteArray): RomPackageReadResult {
        if (packageBytes.size > MAX_PACKAGE_BYTES) return RomPackageReadResult.Failure
        val entries = readArchive(packageBytes) ?: return RomPackageReadResult.Failure
        val manifestBytes = entries[MANIFEST_PATH] ?: return RomPackageReadResult.Failure
        val manifest = try {
            json.decodeFromString<RomPackageManifest>(
                manifestBytes.decodeToString(throwOnInvalidSequence = true),
            )
        } catch (_: IllegalArgumentException) {
            return RomPackageReadResult.Failure
        } catch (_: SerializationException) {
            return RomPackageReadResult.Failure
        }
        val content = entries.filterKeys { it != MANIFEST_PATH }.mapValues { (path, bytes) ->
            RomPackageEntryInfo(path, bytes.size, bytes.sha256()) to bytes
        }
        return when (val validated = RomPackageValidator.validate(manifest, content)) {
            is RomPackageValidationResult.Failure -> RomPackageReadResult.Failure
            is RomPackageValidationResult.Success -> RomPackageReadResult.Success(validated.romSet)
        }
    }

    fun write(romSet: RomSet): ByteArray {
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

    private fun readArchive(packageBytes: ByteArray): Map<String, ByteArray>? {
        val entries = linkedMapOf<String, ByteArray>()
        var count = 0
        var expandedSize = 0
        try {
            ZipInputStream(ByteArrayInputStream(packageBytes)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++count > MAX_ENTRY_COUNT || !entry.name.isSafeArchivePath()) return null
                    if (entry.name in entries) return null
                    if (!entry.isDirectory) {
                        val limit = minOf(MAX_ENTRY_BYTES, MAX_EXPANDED_BYTES - expandedSize)
                        if (limit < 0) return null
                        val bytes = zip.readBounded(limit) ?: return null
                        entries[entry.name] = bytes
                        expandedSize += bytes.size
                    }
                    zip.closeEntry()
                }
            }
        } catch (_: ZipException) {
            return null
        } catch (_: IOException) {
            return null
        }
        return entries
    }

    private fun ZipInputStream.readBounded(maxBytes: Int): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            output.write(buffer, 0, read)
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

    internal fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this).joinToString("") { "%02x".format(it) }

    const val MAX_PACKAGE_BYTES: Int = 32 * 1024 * 1024
    private const val MAX_EXPANDED_BYTES = 32 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024
    private const val MAX_ENTRY_COUNT = 128
    private const val MANIFEST_PATH = "manifest.json"
}

internal sealed interface RomPackageReadResult {
    data class Success(val romSet: RomSet) : RomPackageReadResult
    data object Failure : RomPackageReadResult
}
