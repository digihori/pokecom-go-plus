package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicCodeMeaning
import com.digihori.pgp.core.source.basic.BasicDialect
import com.digihori.pgp.core.source.basic.BasicDialectId
import com.digihori.pgp.core.source.basic.BasicLineNumberEncoding
import com.digihori.pgp.core.source.basic.BasicProgramFormat
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol

/** PC-1245 OLD-family BASIC byte-code and character definition. */
public object Pc1245BasicDialect {
    public val definition: BasicDialect by lazy { BasicDialect(
        id = BasicDialectId("old.pc-1245"),
        programFormat = BasicProgramFormat(
            startMarker = 0xff,
            endMarker = 0xff,
            lineTerminator = 0x00,
            maximumLineNumber = 999,
            lineNumberEncoding = BasicLineNumberEncoding.OLD_PACKED_DECIMAL_3_DIGIT,
        ),
        codeMeanings = buildMap {
            put(0x11, BasicCodeMeaning.Character(' '))
            put(0x12, BasicCodeMeaning.Character('"'))
            put(0x13, BasicCodeMeaning.Character('?'))
            put(0x14, BasicCodeMeaning.Character('!'))
            put(0x15, BasicCodeMeaning.Character('#'))
            put(0x16, BasicCodeMeaning.Character('%'))
            put(0x17, BasicCodeMeaning.Character('\\'))
            put(0x18, BasicCodeMeaning.Character('$'))
            put(0x19, BasicCodeMeaning.Special(BasicSpecialSymbol.PI))
            put(0x1a, BasicCodeMeaning.Special(BasicSpecialSymbol.SQUARE_ROOT))
            ",;:@&".forEachIndexed { index, character ->
                put(0x1b + index, BasicCodeMeaning.Character(character))
            }
            "()><=+-*/^".forEachIndexed { index, character ->
                put(0x30 + index, BasicCodeMeaning.Character(character))
            }
            "0123456789.".forEachIndexed { index, character ->
                put(0x40 + index, BasicCodeMeaning.Character(character))
            }
            put(0x4b, BasicCodeMeaning.Special(BasicSpecialSymbol.EXPONENT))
            put(0x4c, BasicCodeMeaning.Special(BasicSpecialSymbol.BLOCK))
            put(0x4d, BasicCodeMeaning.Character('~'))
            put(0x50, BasicCodeMeaning.Character(' '))
            ('A'..'Z').forEachIndexed { index, character ->
                put(0x51 + index, BasicCodeMeaning.Character(character))
            }
            KEYWORDS.forEach { (code, keyword) -> put(code, BasicCodeMeaning.Keyword(keyword)) }
        },
        canonicalCharacterCodes = buildMap {
            put(' ', 0x11)
            put('"', 0x12)
            put('?', 0x13)
            put('!', 0x14)
            put('#', 0x15)
            put('%', 0x16)
            put('\\', 0x17)
            put('$', 0x18)
            ",;:@&".forEachIndexed { index, character -> put(character, 0x1b + index) }
            "()><=+-*/^".forEachIndexed { index, character -> put(character, 0x30 + index) }
            "0123456789.".forEachIndexed { index, character -> put(character, 0x40 + index) }
            put('~', 0x4d)
            ('A'..'Z').forEachIndexed { index, character -> put(character, 0x51 + index) }
        },
        canonicalSpecialCodes = mapOf(
            BasicSpecialSymbol.PI to 0x19,
            BasicSpecialSymbol.SQUARE_ROOT to 0x1a,
            BasicSpecialSymbol.EXPONENT to 0x4b,
            BasicSpecialSymbol.BLOCK to 0x4c,
        ),
    ) }

    public const val PROGRAM_TEXT_START: Int = 0xc000
    public const val PROGRAM_START_POINTER_LOW: Int = 0xc6e1
    public const val PROGRAM_START_POINTER_HIGH: Int = 0xc6e2
    public const val PROGRAM_END_POINTER_LOW: Int = 0xc6e3
    public const val PROGRAM_END_POINTER_HIGH: Int = 0xc6e4
    public const val PROGRAM_STORAGE_END_EXCLUSIVE: Int = PROGRAM_START_POINTER_LOW

    private val KEYWORDS: Map<Int, String> = mapOf(
        0x7d to "ASC", 0x7e to "VAL", 0x7f to "LEN",
        0x81 to "AND", 0x82 to ">=", 0x83 to "<=", 0x84 to "<>", 0x85 to "OR",
        0x86 to "NOT", 0x87 to "SQR", 0x88 to "CHR$", 0x89 to "COM$", 0x8a to "INKEY$",
        0x8b to "STR$", 0x8c to "LEFT$", 0x8d to "RIGHT$", 0x8e to "MID$",
        0x90 to "TO", 0x91 to "STEP", 0x92 to "THEN", 0x93 to "RANDOM", 0x95 to "WAIT",
        0x96 to "ERROR", 0x99 to "KEY", 0x9b to "SETCOM", 0x9e to "ROM", 0x9f to "LPRINT",
        0xa0 to "SIN", 0xa1 to "COS", 0xa2 to "TAN", 0xa3 to "ASN", 0xa4 to "ACS",
        0xa5 to "ATN", 0xa6 to "EXP", 0xa7 to "LN", 0xa8 to "LOG", 0xa9 to "INT",
        0xaa to "ABS", 0xab to "SGN", 0xac to "DEG", 0xad to "DMS", 0xae to "RND",
        0xaf to "PEEK", 0xb0 to "RUN", 0xb1 to "NEW", 0xb2 to "MEM", 0xb3 to "LIST",
        0xb4 to "CONT", 0xb5 to "DEBUG", 0xb6 to "CSAVE", 0xb7 to "CLOAD", 0xb8 to "MERGE",
        0xb9 to "TRON", 0xba to "TROFF", 0xbb to "PASS", 0xbc to "LLIST", 0xbd to "PI",
        0xbe to "OUTSTAT", 0xbf to "INSTAT", 0xc0 to "GRAD", 0xc1 to "PRINT",
        0xc2 to "INPUT", 0xc3 to "RADIAN", 0xc4 to "DEGREE", 0xc5 to "CLEAR",
        0xc9 to "CALL", 0xca to "DIM", 0xcb to "DATA", 0xcc to "ON", 0xcd to "OFF",
        0xce to "POKE", 0xcf to "READ", 0xd0 to "IF", 0xd1 to "FOR", 0xd2 to "LET",
        0xd3 to "REM", 0xd4 to "END", 0xd5 to "NEXT", 0xd6 to "STOP", 0xd7 to "GOTO",
        0xd8 to "GOSUB", 0xd9 to "CHAIN", 0xda to "PAUSE", 0xdb to "BEEP",
        0xdc to "AREAD", 0xdd to "USING", 0xde to "RETURN", 0xdf to "RESTORE",
    )
}
