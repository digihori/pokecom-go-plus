package com.digihori.pgp.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectInfoTest {
    @Test
    fun exposesProjectIdentityFromCommonCode() {
        assertEquals("Pokecom GO Plus", ProjectInfo.DISPLAY_NAME)
        assertEquals("PGP", ProjectInfo.DEVELOPMENT_NAME)
    }
}
