package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineRomLayout
import com.digihori.pgp.core.rom.MachineId
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object DesktopRomSetCreator {
    fun externalSize(machineId: MachineId): Long = when (MachineCatalog.require(machineId).romLayout) {
        MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_16K -> 16_384L
        MachineRomLayout.SC61860_INTERNAL_8K_EXTERNAL_32K -> 32_768L
        MachineRomLayout.SC61860_INTERNAL_8K_BANKED_16K_X4 -> 65_536L
        MachineRomLayout.SC61860_INTERNAL_8K_BANKED_16K_X8 -> 131_072L
    }

    fun inputIssue(file: File?, expectedSize: Long): RomSetInputIssue? = when {
        file == null -> RomSetInputIssue.MissingFile
        !file.isFile || !file.canRead() -> RomSetInputIssue.UnreadableFile
        file.length() != expectedSize -> RomSetInputIssue.SizeMismatch(expectedSize, file.length())
        else -> null
    }

    // Keep the existing non-UI diagnostic contract for callers and exceptions.
    fun inputError(file: File?, expectedSize: Long): String? = inputIssue(file, expectedSize)?.description()

    private fun RomSetInputIssue.description(): String = when (this) {
        RomSetInputIssue.MissingFile -> "Select a file."
        RomSetInputIssue.UnreadableFile -> "The file cannot be read."
        is RomSetInputIssue.SizeMismatch -> "Expected ${expectedSize / 1024} KiB; selected ${actualSize} bytes."
    }

    fun destination(file: File): File = if (file.extension.equals("pgrom", ignoreCase = true)) file
        else File(file.parentFile, "${file.name}.pgrom")

    fun create(machineId: MachineId, internal: File, external: File, destination: File, overwrite: Boolean = false): File {
        inputIssue(internal, 8192)?.let { throw RomSetInputException(it, it.description()) }
        inputIssue(external, externalSize(machineId))?.let { throw RomSetInputException(it, it.description()) }
        val target = destination.canonicalFile
        require(target != internal.canonicalFile && target != external.canonicalFile) { "The output must not replace an input ROM." }
        require(overwrite || !target.exists()) { "The destination already exists." }
        val conversion = DesktopRomPackageConverter.createPackage(machineId, internal.readBytes(), external.readBytes())
        val bytes = when (conversion) {
            is DesktopRomPackageConversionResult.Success -> conversion.packageBytes
            is DesktopRomPackageConversionResult.Failure -> throw RomSetValidationException(conversion.error)
        }
        val temporary = Files.createTempFile(target.parentFile.toPath(), ".pgrom-", ".tmp")
        try {
            Files.write(temporary, bytes)
            if (overwrite) Files.move(temporary, target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            else Files.move(temporary, target.toPath())
        } finally {
            Files.deleteIfExists(temporary)
        }
        return target
    }
}

internal sealed interface RomSetInputIssue {
    data object MissingFile : RomSetInputIssue
    data object UnreadableFile : RomSetInputIssue
    data class SizeMismatch(val expectedSize: Long, val actualSize: Long) : RomSetInputIssue
}

internal class RomSetValidationException(val error: com.digihori.pgp.core.emulator.machine.pc1245.RomImportError) :
    IllegalStateException("ROM validation failed: $error")

internal class RomSetInputException(val issue: RomSetInputIssue, message: String) : IllegalArgumentException(message)
