package com.digihori.pgp.desktop.rom

import java.io.File

internal object DesktopRomLocator {
    const val PC1245_ROM_ENVIRONMENT_VARIABLE: String = "PGP_PC1245_ROM"
    const val DEFAULT_PC1245_ROM_PATH: String = "local-data/roms/pc-1245/pc1245mem.bin"

    fun findPc1245Rom(
        configuredPath: String? = System.getenv(PC1245_ROM_ENVIRONMENT_VARIABLE),
        workingDirectory: File = File(System.getProperty("user.dir")),
    ): File? {
        if (!configuredPath.isNullOrBlank()) {
            val configured = File(configuredPath).let { file ->
                if (file.isAbsolute) file else File(workingDirectory, configuredPath)
            }
            return configured.takeIf(File::isFile)
        }

        var directory: File? = workingDirectory.absoluteFile
        while (directory != null) {
            val candidate = File(directory, DEFAULT_PC1245_ROM_PATH)
            if (candidate.isFile) return candidate
            directory = directory.parentFile
        }
        return null
    }
}
