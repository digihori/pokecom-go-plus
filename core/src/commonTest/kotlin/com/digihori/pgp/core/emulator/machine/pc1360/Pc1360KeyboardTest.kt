package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.PocketKey
import kotlin.test.Test
import kotlin.test.assertEquals

class Pc1360KeyboardTest {
    @Test
    fun kOnlyKeysUseReferenceScanPositionsAndReleaseCleanly() {
        val cases = listOf(
            Triple(PocketKey.KANJI_FIX, 0, 2), Triple(PocketKey.KANJI_CONVERT, 0, 3),
            Triple(PocketKey.KANJI, 10, 4), Triple(PocketKey.HIRA_KATA, 11, 5), Triple(PocketKey.YJ, 11, 6),
        )
        val standard = Pc1360Keyboard()
        val keyboard = Pc1360Keyboard(kanjiModel = true)
        for ((key, group, bit) in cases) {
            kotlin.test.assertFalse(standard.press(key))
            kotlin.test.assertTrue(keyboard.press(key))
            fun scan(): Int = if (group < 7) keyboard.readInputA(0, group + 1)
                else keyboard.readInputA(1 shl (group - 7), 0)
            assertEquals(1 shl bit, scan(), key.name)
            kotlin.test.assertTrue(keyboard.release(key))
            assertEquals(0, scan(), key.name)
        }
        keyboard.press(PocketKey.HIRA_KATA)
        keyboard.press(PocketKey.YJ)
        assertEquals(0x60, keyboard.readInputA(0x10, 0))
        keyboard.reset()
        assertEquals(0, keyboard.readInputA(0x10, 0))
    }

    @Test
    fun memorySelectorUsesOneBasedGroupNumber() {
        val keyboard = Pc1360Keyboard()
        keyboard.press(PocketKey.Q)
        assertEquals(0, keyboard.readInputA(0, 1))
        assertEquals(0x40, keyboard.readInputA(0, 2))
    }

    @Test
    fun iaSelectsUpperFiveGroups() {
        val keyboard = Pc1360Keyboard()
        keyboard.press(PocketKey.ENTER)
        assertEquals(0x10, keyboard.readInputA(0x04, 0))
    }

    @Test
    fun smallAndKanaAreIndependentKeys() {
        val keyboard = Pc1360Keyboard()

        keyboard.press(PocketKey.SMALL)
        assertEquals(0x10, keyboard.readInputA(0, 1))
        assertEquals(0, keyboard.readInputA(0, 6))
        keyboard.release(PocketKey.SMALL)

        keyboard.press(PocketKey.KANA)
        assertEquals(0, keyboard.readInputA(0, 1))
        assertEquals(0x02, keyboard.readInputA(0, 6))
    }
}
