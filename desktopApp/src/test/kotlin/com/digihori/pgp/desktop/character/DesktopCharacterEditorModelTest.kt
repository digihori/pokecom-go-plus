package com.digihori.pgp.desktop.character

import com.digihori.pgp.core.character.CharacterEditorFormats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopCharacterEditorModelTest {
    private val format = CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP

    @Test
    fun dotEditingSynchronizesTextAndOutputs() {
        val model = DesktopCharacterEditorModel(format).withDot(0, 0, true).withDot(4, 6, true)

        assertEquals("0x01, 0x00, 0x00, 0x00, 0x40", model.byteText)
        assertEquals("DATA 1, 0, 0, 0, 64", model.basicOutput)
        assertEquals("DB 0x01, 0x00, 0x00, 0x00, 0x40", model.assemblerOutput)
        assertEquals("&01,&00,&00,&00,&40", model.ampersandHexOutput)
        assertEquals("1,0,0,0,64", model.decimalOutput)
        assertEquals("$01,$00,$00,$00,$40", model.dollarHexOutput)
        assertEquals("0x01,0x00,0x00,0x00,0x40", model.prefixedHexOutput)
    }

    @Test
    fun invalidTextKeepsThePreviousPattern() {
        val original = DesktopCharacterEditorModel(format).withDot(2, 3, true)
        val applied = original.withByteText("1, 2").applyByteText()

        assertEquals(original.pattern, applied.pattern)
        assertNotNull(applied.inputError)
    }

    @Test
    fun validTextUpdatesTheGrid() {
        val applied = DesktopCharacterEditorModel(format).withByteText("1, 0, 0, 0, 64").applyByteText()

        assertTrue(applied.pattern.isSet(0, 0))
        assertTrue(applied.pattern.isSet(4, 6))
        assertFalse(applied.pattern.isSet(0, 1))
        assertNull(applied.inputError)
    }

    @Test
    fun unusedBitsDoNotReplaceThePreviousPattern() {
        val original = DesktopCharacterEditorModel(format).withDot(2, 3, true)
        val applied = original.withByteText("0x80, 0, 0, 0, 0").applyByteText()

        assertEquals(original.pattern, applied.pattern)
        assertNotNull(applied.inputError)
    }

    @Test
    fun mapsPointerPositionsAndRejectsGridEdges() {
        assertEquals(0 to 0, CharacterGridGeometry.cellAt(0f, 0f, 500f, 700f, 5, 7))
        assertEquals(4 to 6, CharacterGridGeometry.cellAt(499f, 699f, 500f, 700f, 5, 7))
        assertNull(CharacterGridGeometry.cellAt(500f, 100f, 500f, 700f, 5, 7))
        assertNull(CharacterGridGeometry.cellAt(-1f, 100f, 500f, 700f, 5, 7))
    }
}
