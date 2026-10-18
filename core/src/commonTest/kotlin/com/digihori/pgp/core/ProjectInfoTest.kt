package com.digihori.pgp.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectInfoTest {
    @Test
    fun exposesProjectIdentityFromCommonCode() {
        assertEquals("Pokecom GO Plus", ProjectInfo.DISPLAY_NAME)
        assertEquals("Pokecom GO Studio", ProjectInfo.STUDIO_DISPLAY_NAME)
        assertEquals("0.6.0-alpha.2", ProjectInfo.VERSION)
        assertEquals("PGP", ProjectInfo.DEVELOPMENT_NAME)
    }
}
