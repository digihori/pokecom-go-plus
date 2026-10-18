package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import java.io.File

internal object DesktopRomLocator {
    const val PC1245_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1245_ROM"
    const val PC1251_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1251_ROM"
    const val PC1360_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1360_ROM"
    const val PC1261_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1261_ROM"
    const val DEFAULT_PC1245_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"
    const val DEFAULT_PC1251_ROM_PATH: String = "local-data/roms/pc-1251/pc1251mem.bin"
    const val DEFAULT_PC1360_ROM_PATH: String = "local-data/roms/pc-1360/pc1360mem.bin"
    const val DEFAULT_PC1261_ROM_PATH: String = "local-data/roms/pc-1261/pc1261mem.bin"

    fun findPc1245Rom(
        configuredPath: String? = System.getenv(PC1245_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1245_ROM_PATH, workingDirectory)

    fun findPc1251Rom(
        configuredPath: String? = System.getenv(PC1251_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1251_ROM_PATH, workingDirectory)

    fun findPc1360Rom(
        configuredPath: String? = System.getenv(PC1360_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1360_ROM_PATH, workingDirectory)
        ?.takeIf { File(it.parentFile, "pc1360bank.bin").isFile }

    fun findPc1360kRom(workingDirectory: File = File(System.getProperty("user.dir"))): File? =
        findRom(null, "local-data/roms/pc-1360k/pc-1360k.pgrom", workingDirectory)
            ?: findRom(null, "local-data/roms/pc-1360k/pc1360kmem.bin", workingDirectory)
                ?.takeIf { File(it.parentFile, "pc1360kbank.bin").isFile }

    fun pc1360kKanjiFiles(directory: File): List<File> {
        val files = (0..3).map { File(directory, "kanji-selector-$it.bin") }
        return when {
            files.drop(2).any(File::exists) -> files
            files.take(2).any(File::exists) -> files.take(2)
            else -> emptyList()
        }
    }

    fun findPc1261Rom(
        configuredPath: String? = System.getenv(PC1261_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1261_ROM_PATH, workingDirectory)

    const val PC1401_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1401_ROM"
    const val DEFAULT_PC1401_ROM_PATH: String = "local-data/roms/pc-1401/pc1401mem.bin"
    fun findPc1401Rom(
        configuredPath: String? = System.getenv(PC1401_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? = findRom(configuredPath, DEFAULT_PC1401_ROM_PATH, workingDirectory)

    fun findFirstAvailableRom(
        pc1245ConfiguredPath: String? = System.getenv(PC1245_ROM_ENVIRONMENT_VARIABLE),
        pc1251ConfiguredPath: String? = System.getenv(PC1251_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
        pc1360ConfiguredPath: String? = System.getenv(PC1360_ROM_ENVIRONMENT_VARIABLE),
        pc1261ConfiguredPath: String? = System.getenv(PC1261_ROM_ENVIRONMENT_VARIABLE),
        pc1401ConfiguredPath: String? = System.getenv(PC1401_ROM_ENVIRONMENT_VARIABLE),
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
        if (!pc1360ConfiguredPath.isNullOrBlank()) {
            findPc1360Rom(pc1360ConfiguredPath, workingDirectory)?.let {
                return DesktopRomSelection(it, Pc1360RomDefinition.MACHINE_ID)
            }
        }
        if (!pc1401ConfiguredPath.isNullOrBlank()) {
            findPc1401Rom(pc1401ConfiguredPath, workingDirectory)?.let { return DesktopRomSelection(it, Pc1401RomDefinition.MACHINE_ID) }
        }
        if (!pc1261ConfiguredPath.isNullOrBlank()) {
            findPc1261Rom(pc1261ConfiguredPath, workingDirectory)?.let {
                return DesktopRomSelection(it, Pc1261RomDefinition.MACHINE_ID)
            }
        }
        return findRom(null, DEFAULT_PC1245_ROM_PATH, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1245RomDefinition.MACHINE_ID)
        } ?: findRom(null, DEFAULT_PC1251_ROM_PATH, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1251RomDefinition.MACHINE_ID)
        } ?: findPc1360Rom(null, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1360RomDefinition.MACHINE_ID)
        } ?: findPc1261Rom(null, workingDirectory)?.let {
            DesktopRomSelection(it, Pc1261RomDefinition.MACHINE_ID)
        } ?: findPc1401Rom(null, workingDirectory)?.let { DesktopRomSelection(it, Pc1401RomDefinition.MACHINE_ID) }
            ?: findPc1360kRom(workingDirectory)?.let { DesktopRomSelection(it, Pc1360RomDefinition.PC1360K_MACHINE_ID) }
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
