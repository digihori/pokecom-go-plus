package com.digihori.pgp.player.android.rom

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.player.PlayerFailureKind
import com.digihori.pgp.player.PlayerSession
import com.digihori.pgp.player.PlayerSessionCreationResult
import com.digihori.pgp.player.PlayerSessionFactory
import java.io.ByteArrayOutputStream

internal class AndroidRomImporter(
    private val contentResolver: ContentResolver,
) {
    fun prepare(uri: Uri): RomImportOutcome {
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > AndroidRomPackage.MAX_PACKAGE_BYTES) {
                        return RomImportOutcome.Failure(PlayerFailureKind.ROM_VALIDATION)
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: return RomImportOutcome.Failure(PlayerFailureKind.ROM_READ)
        } catch (_: Exception) {
            return RomImportOutcome.Failure(PlayerFailureKind.ROM_READ)
        }

        val romSet = parse(bytes) ?: return RomImportOutcome.Failure(PlayerFailureKind.ROM_VALIDATION)
        val created = PlayerSessionFactory.create(romSet)
        if (created !is PlayerSessionCreationResult.Success) {
            return RomImportOutcome.Failure(PlayerFailureKind.SESSION_CREATION)
        }
        val canonicalPackage = AndroidRomPackage.write(romSet)
        return RomImportOutcome.Success(PreparedAndroidRom(canonicalPackage, romSet, created.session, displayName(uri)))
    }

    private fun parse(bytes: ByteArray): RomSet? {
        if (bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) {
            return (AndroidRomPackage.read(bytes) as? RomPackageReadResult.Success)?.romSet
        }
        if (bytes.size in Pc1245RomDefinition.SUPPORTED_LEGACY_IMAGE_SIZES) {
            return (Pc1245FlatRomImporter.importImage(bytes) as? RomImportResult.Success)?.romSet
        }
        return (AndroidRomPackage.read(bytes) as? RomPackageReadResult.Success)?.romSet
    }

    private fun displayName(uri: Uri): String? = runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment
}

internal sealed interface RomImportOutcome {
    data class Success(val prepared: PreparedAndroidRom) : RomImportOutcome
    data class Failure(val kind: PlayerFailureKind) : RomImportOutcome
}

internal data class PreparedAndroidRom(val bytes: ByteArray, val romSet: RomSet, val session: PlayerSession, val sourceName: String?)
