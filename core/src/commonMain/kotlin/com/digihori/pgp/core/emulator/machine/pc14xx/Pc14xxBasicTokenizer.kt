package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicDetokenizeResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicTokenizer
import com.digihori.pgp.core.source.basic.BasicTextDocument
import com.digihori.pgp.core.source.basic.BasicSymbolEncoding

/** PC-1450/1460's S1 token table. The byte format is shared with PC-1350, the vocabulary is not. */
public object Pc14xxBasicTokenizer {
    public fun tokenize(document: BasicTextDocument): Pc1350BasicTokenizeResult =
        Pc1350BasicTokenizer.tokenize(document, KEYWORDS, allowKana = true, specialCodes = SPECIAL_CODES)

    public fun detokenize(bytes: ByteArray): Pc1350BasicDetokenizeResult =
        Pc1350BasicTokenizer.detokenize(bytes, KEYWORDS.associate { (name, code) -> code to name },
            allowKana = true, specialNames = BasicSymbolEncoding.names(BasicSymbolEncoding.later))

    private val SPECIAL_CODES = BasicSymbolEncoding.later

    private val KEYWORDS: List<Pair<String, Int>> = buildList {
        val groups = listOf(
            0x81 to "REC POL ROT DECI HEX TEN RCP SQU CUR HSN HCS HTN AHS AHC AHT FACT",
            0x91 to "LN LOG EXP SQR SIN COS TAN INT ABS SGN DEG DMS ASN ACS ATN",
            0xa0 to "RND AND OR NOT ASC VAL LEN PEEK CHR$ STR$ MID$ LEFT$ RIGHT$ INKEY$ PI MEM",
            0xb0 to "RUN NEW CONT PASS LIST LLIST CSAVE CLOAD MARGE _ _ OPEN CLOSE SAVE LOAD CONSOLE",
            0xc0 to "RANDOM DEGREE RADIAN GRAD BEEP WAIT GOTO TRON TROFF CLEAR USING DIM CALL POKE _ _",
            0xd0 to "TO STEP THEN ON IF FOR LET REM END NEXT STOP READ DATA PAUSE PRINT INPUT",
            0xe0 to "GOSUB AREAD LPRINT RETURN RESTORE CHAIN _ _ _ _ _ _ BASIC TEXT OPEN$ _",
        )
        groups.forEach { (base, words) ->
            words.split(' ').forEachIndexed { index, word -> if (word != "_") add(word to base + index) }
        }
    }.sortedByDescending { it.first.length }
}
