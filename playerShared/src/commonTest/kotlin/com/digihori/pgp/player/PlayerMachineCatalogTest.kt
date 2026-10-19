package com.digihori.pgp.player

import kotlin.test.*

class PlayerMachineCatalogTest {
    @Test fun onlyExplicitPlayerTargetsAreSelectable() {
        assertEquals(listOf("pc-1245", "pc-1251", "pc-1261", "pc-1350", "pc-1360", "pc-1360k", "pc-1401"), PlayerMachineCatalog.definitions.map { it.id.value })
    }
}
