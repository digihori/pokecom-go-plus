package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.CreateSessionError
import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult

internal object DesktopRomLoader {
    fun loadPc1245LegacyImage(image: ByteArray): DesktopRomLoadResult =
        when (val imported = Pc1245FlatRomImporter.importImage(image)) {
            is RomImportResult.Failure -> DesktopRomLoadResult.Failure(
                DesktopRomLoadError.InvalidRom(imported.error),
            )
            is RomImportResult.Success -> when (
                val created = EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet)
            ) {
                is CreateSessionResult.Failure -> DesktopRomLoadResult.Failure(
                    DesktopRomLoadError.SessionCreation(created.error),
                )
                is CreateSessionResult.Success -> DesktopRomLoadResult.Success(created.session)
            }
        }
}

internal sealed interface DesktopRomLoadResult {
    data class Success(val session: EmulatorSession) : DesktopRomLoadResult
    data class Failure(val error: DesktopRomLoadError) : DesktopRomLoadResult
}

internal sealed interface DesktopRomLoadError {
    data class InvalidRom(val error: RomImportError) : DesktopRomLoadError
    data class SessionCreation(val error: CreateSessionError) : DesktopRomLoadError
}
