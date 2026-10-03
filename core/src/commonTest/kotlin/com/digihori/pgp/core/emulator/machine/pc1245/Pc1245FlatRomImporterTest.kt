package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245FlatRomImporterTest {
    @Test
    fun extractsInternalAndExternalRomAndDropsUnusedSpace() {
        val image = ByteArray(0x10000) { address ->
            when (address) {
                in 0x0000..0x1fff -> 0x11
                in 0x2000..0x3fff -> 0x22
                in 0x4000..0x7fff -> 0x33
                else -> 0x44
            }.toByte()
        }

        val result = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(image))
        val internal = result.romSet.component(Pc1245RomDefinition.INTERNAL_ID)
        val external = result.romSet.component(Pc1245RomDefinition.EXTERNAL_ID)

        assertEquals(Pc1245RomDefinition.MACHINE_ID, result.romSet.machineId)
        assertContentEquals(ByteArray(0x2000) { 0x11 }, internal?.copyBytes())
        assertContentEquals(ByteArray(0x4000) { 0x33 }, external?.copyBytes())
    }

    @Test
    fun reportsInvalidLegacyImageSizeWithoutProducingAPartialSet() {
        val result = assertIs<RomImportResult.Failure>(
            Pc1245FlatRomImporter.importImage(ByteArray(123)),
        )
        val error = assertIs<RomImportError.InvalidImageSize>(result.error)

        assertEquals(0x10000, error.expected)
        assertEquals(123, error.actual)
    }
}
