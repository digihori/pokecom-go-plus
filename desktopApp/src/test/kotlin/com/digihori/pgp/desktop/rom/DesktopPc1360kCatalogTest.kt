package com.digihori.pgp.desktop.rom

import com.digihori.pgp.core.rom.MachineId
import kotlin.test.*

class DesktopPc1360kCatalogTest {
    @Test
    fun pc1360kIsListedAndFirmwareCannotBeRelabelled() {
        val id = MachineId("pc-1360k")
        assertTrue(com.digihori.pgp.core.api.MachineCatalog.definitions.any { it.id == id && it.displayName == "PC-1360K" })
        assertTrue(DesktopRomRegistrationCatalog.definitions.any { it.id == id })
        val converted = assertIs<DesktopRomPackageConversionResult.Success>(
            DesktopRomPackageConverter.convertLegacyImage(id, ByteArray(8192), ByteArray(131072)),
        )
        val loaded = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(converted.packageBytes))
        assertEquals(id, loaded.session.machineId)
        assertIs<DesktopRomLoadResult.Failure>(DesktopRomLoader.loadPackageForMachine(converted.packageBytes, MachineId("pc-1360")))
    }
}
