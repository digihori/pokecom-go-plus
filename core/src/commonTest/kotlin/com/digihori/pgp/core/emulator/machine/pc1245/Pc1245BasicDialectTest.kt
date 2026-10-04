package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicCodeMeaning
import com.digihori.pgp.core.source.basic.BasicLineNumberEncoding
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Pc1245BasicDialectTest {
    private val dialect = Pc1245BasicDialect.definition

    @Test
    fun definesTheOldFamilyProgramEnvelope() {
        assertEquals("old.pc-1245", dialect.id.value)
        assertEquals(0xff, dialect.programFormat.startMarker)
        assertEquals(0xff, dialect.programFormat.endMarker)
        assertEquals(0x00, dialect.programFormat.lineTerminator)
        assertEquals(999, dialect.programFormat.maximumLineNumber)
        assertEquals(
            BasicLineNumberEncoding.OLD_PACKED_DECIMAL_3_DIGIT,
            dialect.programFormat.lineNumberEncoding,
        )
        assertEquals(0xc000, Pc1245BasicDialect.PROGRAM_TEXT_START)
        assertEquals(0xc6e1, Pc1245BasicDialect.PROGRAM_START_POINTER_LOW)
        assertEquals(0xc6e4, Pc1245BasicDialect.PROGRAM_END_POINTER_HIGH)
    }

    @Test
    fun distinguishesDisplaySymbolsFromBasicKeywords() {
        assertEquals(BasicCodeMeaning.Special(BasicSpecialSymbol.PI), dialect.meaningOf(0x19))
        assertEquals(BasicCodeMeaning.Keyword("PI"), dialect.meaningOf(0xbd))
        assertEquals(BasicCodeMeaning.Special(BasicSpecialSymbol.SQUARE_ROOT), dialect.meaningOf(0x1a))
        assertEquals(BasicCodeMeaning.Keyword("SQR"), dialect.meaningOf(0x87))
    }

    @Test
    fun mapsRepresentativeCharactersAndKeywords() {
        assertEquals(BasicCodeMeaning.Character(' '), dialect.meaningOf(0x50))
        assertEquals(0x51, dialect.codeOf('A'))
        assertEquals(0x51, dialect.codeOf('a'))
        assertEquals(0x40, dialect.codeOf('0'))
        assertEquals(0x17, dialect.codeOf('\\'))
        assertEquals(0xc1, dialect.codeOfKeyword("print"))
        assertEquals(0xd7, dialect.codeOfKeyword("GOTO"))
        assertEquals(0xb8, dialect.codeOfKeyword("MERGE"))
        assertNull(dialect.codeOfKeyword("MARGE"))
        assertNull(dialect.codeOf('_'))
        assertNull(dialect.codeOfKeyword("ELSE"))
    }
}
