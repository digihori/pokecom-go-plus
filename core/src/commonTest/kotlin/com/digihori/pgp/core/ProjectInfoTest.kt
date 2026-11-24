package com.digihori.pgp.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectInfoTest {
    @Test
    fun exposesProjectIdentityFromCommonCode() {
        assertEquals("Pokecom SC61 Plus", ProjectInfo.DISPLAY_NAME)
        assertEquals("Pokecom SC61 Studio", ProjectInfo.STUDIO_DISPLAY_NAME)
        assertEquals("1.2.2", ProjectInfo.VERSION)
        assertEquals("PGP", ProjectInfo.DEVELOPMENT_NAME)
    }
}
