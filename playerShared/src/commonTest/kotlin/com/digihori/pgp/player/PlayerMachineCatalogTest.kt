package com.digihori.pgp.player

import kotlin.test.*

class PlayerMachineCatalogTest {
    @Test fun onlyExplicitPlayerTargetsAreSelectable() {
        assertEquals(listOf("pc-1245", "pc-1401", "pc-1350"), PlayerMachineCatalog.definitions.map { it.id.value })
    }
}
