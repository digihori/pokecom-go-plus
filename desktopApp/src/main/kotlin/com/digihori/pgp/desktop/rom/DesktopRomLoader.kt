package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.CreateSessionError
import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportError
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.rom.MachineId

internal object DesktopRomLoader {
    fun loadPc1245LegacyImage(image: ByteArray): DesktopRomLoadResult =
        loadLegacyImage(Pc1245RomDefinition.MACHINE_ID, image)

    fun loadLegacyImage(machineId: MachineId, image: ByteArray): DesktopRomLoadResult {
        val imported = when (machineId) {
            Pc1245RomDefinition.MACHINE_ID -> Pc1245FlatRomImporter.importImage(image)
            Pc1251RomDefinition.MACHINE_ID -> Pc1251FlatRomImporter.importImage(image)
            else -> return DesktopRomLoadResult.Failure(
                DesktopRomLoadError.SessionCreation(CreateSessionError.UnsupportedMachine(machineId)),
            )
        }
        return when (imported) {
            is RomImportResult.Failure -> DesktopRomLoadResult.Failure(
                DesktopRomLoadError.InvalidRom(imported.error),
            )
            is RomImportResult.Success -> when (
                val created = EmulatorFactory.create(machineId, imported.romSet)
            ) {
                is CreateSessionResult.Failure -> DesktopRomLoadResult.Failure(
                    DesktopRomLoadError.SessionCreation(created.error),
                )
                is CreateSessionResult.Success -> DesktopRomLoadResult.Success(created.session)
            }
        }
    }

    fun loadPackage(packageBytes: ByteArray): DesktopRomLoadResult =
        when (val read = DesktopRomPackage.read(packageBytes)) {
            is DesktopRomPackageReadResult.Failure -> DesktopRomLoadResult.Failure(
                DesktopRomLoadError.InvalidPackage(read.error),
            )
            is DesktopRomPackageReadResult.Success -> when (
                val created = EmulatorFactory.create(read.romSet.machineId, read.romSet)
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
    data class InvalidPackage(val error: DesktopRomPackageError) : DesktopRomLoadError
    data class SessionCreation(val error: CreateSessionError) : DesktopRomLoadError
}
