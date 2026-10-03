package com.digihori.pgp.core.integration

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PokecomGoDeviceStateIntegrationTest {
    @Test
    fun importsCapturedPc1245Preferences() {
        if (System.getenv(ENABLE_ENV) != "1") return

        val preferences = sequenceOf(File(INPUT_PATH), File("../$INPUT_PATH"))
            .firstOrNull(File::isFile)
            ?: File(INPUT_PATH)
        require(preferences.isFile) {
            "Capture Pokecom GO preferences at '$INPUT_PATH' before running this test"
        }

        val expectation = PokecomGoPreferencesReader.readPc1245Expectation(
            preferences.readText(),
        )

        val cpu = assertNotNull(expectation.cpu)
        assertEquals(4, cpu.programCounter?.length)
        assertEquals(512, cpu.internalRamHex?.length)
        assertEquals(listOf("8000", "f800"), expectation.memory?.map { it.start })
        assertEquals(7, expectation.display?.rows?.size)
        assertEquals(true, expectation.display?.rows?.all { it.length == 80 })
    }

    private companion object {
        const val ENABLE_ENV: String = "PGP_VERIFY_POKECOM_GO_STATE"
        const val INPUT_PATH: String = "local-data/golden/pokecom-go-preferences.xml"
    }
}
