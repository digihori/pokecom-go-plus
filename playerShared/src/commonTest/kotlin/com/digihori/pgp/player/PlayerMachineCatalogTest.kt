package com.digihori.pgp.player

import kotlin.test.*

class PlayerMachineCatalogTest {
    @Test fun onlyIntegratedPlayerMachinesAreSelectable() {
        assertEquals(listOf("pc-1245", "pc-1401"), PlayerMachineCatalog.definitions.map { it.id.value })
    }
}
