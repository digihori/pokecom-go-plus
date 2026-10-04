package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import java.io.File

internal object DesktopRomLocator {
    const val PC1245_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1245_ROM"
    const val PC1251_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1251_ROM"
    const val DEFAULT_PC1245_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
    const val DEFAULT_PC1251_ROM_PATH: String = "local-data/roms/pc-1251/pc1251mem.bin"

    fun findPc1245Rom(
        configuredPath: String? = System.getenv(PC1245_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1245_ROM_PATH, workingDirectory)

    fun findPc1251Rom(
        configuredPath: String? = System.getenv(PC1251_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1251_ROM_PATH, workingDirectory)

    fun findFirstAvailableRom(
        pc1245ConfiguredPath: String? = System.getenv(PC1245_ROM_ENVIRONMENT_VARIABLE),
        pc1251ConfiguredPath: String? = System.getenv(PC1251_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): DesktopRomSelection? {
        if (!pc1245ConfiguredPath.isNullOrBlank()) {
            findRom(pc1245ConfiguredPath, DEFAULT_PC1245_ROM_PATH, workingDirectory)?.let {
                return DesktopRomSelection(it, Pc1245RomDefinition.MACHINE_ID)
            }
        }
        if (!pc1251ConfiguredPath.isNullOrBlank()) {
            findRom(pc1251ConfiguredPath, DEFAULT_PC1251_ROM_PATH, workingDirectory)?.let {
                return DesktopRomSelection(it, Pc1251RomDefinition.MACHINE_ID)
            }
        }
        return findRom(null, DEFAULT_PC1245_ROM_PATH, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1245RomDefinition.MACHINE_ID)
        } ?: findRom(null, DEFAULT_PC1251_ROM_PATH, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1251RomDefinition.MACHINE_ID)
        }
    }

    private fun findRom(configuredPath: String?, defaultPath: String, workingDirectory: File): File? {
        if (!configuredPath.isNullOrBlank()) {
            val configured = File(configuredPath).let { file ->
                if (file.isAbsolute) file else File(workingDirectory, configuredPath)
            }
            return configured.takeIf(File::isFile)
        }

        var directory: File? = workingDirectory.absoluteFile
        while (directory != null) {
            val candidate = File(directory, defaultPath)
            if (candidate.isFile) return candidate
            directory = directory.parentFile
        }
        return null
    }
}
