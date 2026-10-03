package com.digihori.pgp.core.emulator.machine.pc1245

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1245ComponentRomImporterTest {
    @Test
    fun importsSeparatePhysicalRomDumps() {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x11 }
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x22 }

        val success = assertIs<RomImportResult.Success>(
            Pc1245ComponentRomImporter.importImages(internal, external),
        )

        assertEquals(Pc1245RomDefinition.MACHINE_ID, success.romSet.machineId)
        assertContentEquals(
            internal,
            success.romSet.component(Pc1245RomDefinition.INTERNAL_ID)?.copyBytes(),
        )
        assertContentEquals(
            external,
            success.romSet.component(Pc1245RomDefinition.EXTERNAL_ID)?.copyBytes(),
        )
    }

    @Test
    fun identifiesTheComponentWhoseSizeIsInvalid() {
        val failure = assertIs<RomImportResult.Failure>(
            Pc1245ComponentRomImporter.importImages(
                ByteArray(Pc1245RomDefinition.INTERNAL_SIZE),
                ByteArray(42),
            ),
        )
        val error = assertIs<RomImportError.InvalidComponentSize>(failure.error)

        assertEquals(Pc1245RomDefinition.EXTERNAL_ID, error.componentId)
        assertEquals(Pc1245RomDefinition.EXTERNAL_SIZE, error.expected)
        assertEquals(42, error.actual)
    }

    @Test
    fun copiesBothInputArrays() {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x11 }
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x22 }
        val success = assertIs<RomImportResult.Success>(
            Pc1245ComponentRomImporter.importImages(internal, external),
        )

        internal[0] = 0x33
        external[0] = 0x44

        assertEquals(0x11, success.romSet.component(Pc1245RomDefinition.INTERNAL_ID)?.copyBytes()?.get(0))
        assertEquals(0x22, success.romSet.component(Pc1245RomDefinition.EXTERNAL_ID)?.copyBytes()?.get(0))
    }
}
