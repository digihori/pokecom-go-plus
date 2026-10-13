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

    fun inputError(file: File?, expectedSize: Long): String? = when {
        file == null -> "Select a file."
        !file.isFile || !file.canRead() -> "The file cannot be read."
        file.length() != expectedSize -> "Expected ${expectedSize / 1024} KiB; selected ${file.length()} bytes."
        else -> null
    }

    fun destination(file: File): File = if (file.extension.equals("pgrom", ignoreCase = true)) file
        else File(file.parentFile, "${file.name}.pgrom")

    fun create(machineId: MachineId, internal: File, external: File, destination: File, overwrite: Boolean = false): File {
        require(inputError(internal, 8192) == null) { inputError(internal, 8192).orEmpty() }
        require(inputError(external, externalSize(machineId)) == null) { inputError(external, externalSize(machineId)).orEmpty() }
        val target = destination.canonicalFile
        require(target != internal.canonicalFile && target != external.canonicalFile) { "The output must not replace an input ROM." }
        require(overwrite || !target.exists()) { "The destination already exists." }
        val conversion = DesktopRomPackageConverter.createPackage(machineId, internal.readBytes(), external.readBytes())
        val bytes = when (conversion) {
            is DesktopRomPackageConversionResult.Success -> conversion.packageBytes
            is DesktopRomPackageConversionResult.Failure -> error("ROM validation failed: ${conversion.error}")
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
