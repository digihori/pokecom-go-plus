package com.digihori.pgp.player

import kotlin.test.*

class PlayerMachineCatalogTest {
    @Test fun onlyCompletedPlayerSkinsAreSelectable() {
        assertEquals(listOf("pc-1245"), PlayerMachineCatalog.definitions.map { it.id.value })
    }
}
