package com.digihori.pgp.player.android.rom

import android.content.ContentResolver
import android.net.Uri
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
    private val library: AndroidRomLibrary,
) {
    fun import(uri: Uri): RomImportOutcome {
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
        return try {
            library.install(canonicalPackage, romSet, displayName(uri))
            RomImportOutcome.Success(created.session)
        } catch (_: Exception) {
            RomImportOutcome.Failure(PlayerFailureKind.STORAGE)
        }
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

    private fun displayName(uri: Uri): String? = uri.lastPathSegment
}

internal sealed interface RomImportOutcome {
    data class Success(val session: PlayerSession) : RomImportOutcome
    data class Failure(val kind: PlayerFailureKind) : RomImportOutcome
}
