package com.digihori.pgp.player

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlayerKeyboardTest {
    @Test
    fun `pc1245 layout is selected from machine definition`() {
        val keyboard = assertNotNull(PlayerKeyboardCatalog.forMachine(Pc1245RomDefinition.MACHINE_ID))

        assertEquals(14, keyboard.columnCount)
        assertEquals(4, keyboard.rows.size)
        assertEquals(2, keyboard.rows.flatten().single { it.key == PocketKey.ENTER }.columnSpan)
        assertTrue(keyboard.rows.flatten().any { it.key == PocketKey.BREAK })
    }
}
