package com.digihori.pgp.core.emulator.machine.pc1350

import com.digihori.pgp.core.source.basic.BasicTextDocument
import com.digihori.pgp.core.source.basic.BasicTextElement

public object Pc1350BasicTokenizer {
    public fun tokenize(document: BasicTextDocument): Pc1350BasicTokenizeResult {
        val lines = mutableListOf<StringBuilder>(StringBuilder())
        document.tokens.forEach { token ->
            when (val element = token.element) {
                BasicTextElement.LineBreak -> lines += StringBuilder()
                is BasicTextElement.Character -> lines.last().append(element.value)
                is BasicTextElement.RawByte -> return failure(token.line, token.column, "Raw bytes are not supported by the S1 tokenizer")
                is BasicTextElement.Special -> return failure(token.line, token.column, "Special symbol ${element.symbol} is not supported yet")
            }
        }
        val output = mutableListOf(0xff)
        lines.forEachIndexed { sourceLine, builder ->
            val text = builder.toString().trimStart()
            if (text.isBlank()) return@forEachIndexed
            val match = LINE_PATTERN.matchEntire(text)
                ?: return failure(sourceLine + 1, 1, "BASIC line must start with a line number")
            val lineNumber = match.groupValues[1].toIntOrNull()
                ?: return failure(sourceLine + 1, 1, "Invalid BASIC line number")
            if (lineNumber !in 1..65279) return failure(sourceLine + 1, 1, "BASIC line number must be between 1 and 65279")
            var body = match.groupValues[2]
            if (body.startsWith(':')) body = body.drop(1)
            body = body.trimStart()
            val encoded = encodeBody(body, sourceLine + 1)
            if (encoded is BodyResult.Failure) return Pc1350BasicTokenizeResult.Failure(encoded.error)
            val bytes = (encoded as BodyResult.Success).bytes
            if (bytes.size + 1 > 0xff) return failure(sourceLine + 1, 1, "Encoded S1 line is too long")
            output += lineNumber ushr 8
            output += lineNumber and 0xff
            output += bytes.size + 1
            output += bytes
            output += 0x0d
        }
        output += 0xff
        return Pc1350BasicTokenizeResult.Success(output.map(Int::toByte).toByteArray())
    }

    private fun encodeBody(body: String, sourceLine: Int): BodyResult {
        val output = mutableListOf<Int>()
        var index = 0
        var inString = false
        var inRemark = false
        while (index < body.length) {
            val char = body[index]
            if (char == '"') {
                output += requireNotNull(encodeCharacter(char))
                inString = !inString
                index++
                continue
            }
            if (!inString && !inRemark && char == ' ') { index++; continue }
            if (!inString && !inRemark) {
                val keyword = KEYWORDS.firstOrNull { (word, _) ->
                    body.regionMatches(index, word, 0, word.length, ignoreCase = true) &&
                        keywordBoundary(body, index, word)
                }
                if (keyword != null) {
                    output += keyword.second
                    if (keyword.first == "REM") inRemark = true
                    index += keyword.first.length
                    continue
                }
            }
            val encodedCharacter = encodeCharacter(char)
            if (encodedCharacter == null) return BodyResult.Failure(
                Pc1350BasicTokenizeError(sourceLine, index + 1, "Character '$char' is not supported yet"),
            )
            output += encodedCharacter
            index++
        }
        return BodyResult.Success(output)
    }

    /** PCWAV S1 codec: ASCII is direct; Unicode half-width kana is FE + A1..DF. */
    private fun encodeCharacter(character: Char): List<Int>? = when {
        character.code in 0x20..0x7e -> listOf(character.code)
        character.code in 0xff61..0xff9f -> listOf(0xfe, 0xa1 + character.code - 0xff61)
        else -> null
    }

    private fun keywordBoundary(text: String, start: Int, keyword: String): Boolean {
        if (keyword.none { it.isLetter() }) return true
        val end = start + keyword.length
        val before = text.getOrNull(start - 1)
        val after = text.getOrNull(end)
        return before?.let { !it.isLetter() && it != '$' } ?: true &&
            (after?.let { !it.isLetter() && it != '$' } ?: true)
    }

    private fun failure(line: Int, column: Int, message: String) =
        Pc1350BasicTokenizeResult.Failure(Pc1350BasicTokenizeError(line, column, message))

    private sealed interface BodyResult {
        data class Success(val bytes: List<Int>) : BodyResult
        data class Failure(val error: Pc1350BasicTokenizeError) : BodyResult
    }

    private val KEYWORDS: List<Pair<String, Int>> = buildList {
        val groups = listOf(
            0x91 to "LN LOG EXP SQR SIN COS TAN INT ABS SGN DEG DMS ASN ACS ATN",
            0xa0 to "RND AND OR NOT ASC VAL LEN PEEK CHR$ STR$ MID$ LEFT$ RIGHT$ INKEY$ PI MEM",
            0xb0 to "RUN NEW CONT PASS LIST LLIST CSAVE CLOAD MARGE _ _ OPEN CLOSE SAVE LOAD CONSOLE",
            0xc0 to "RANDOM DEGREE RADIAN GRAD BEEP WAIT GOTO TRON TROFF CLEAR USING DIM CALL POKE CLS CURSOR",
            0xd0 to "TO STEP THEN ON IF FOR LET REM END NEXT STOP READ DATA PAUSE PRINT INPUT",
            0xe0 to "GOSUB AREAD LPRINT RETURN RESTORE CHAIN GCURSOR GPRINT LINE POINT PSET PRESET BASIC TEXT OPEN$ _",
        )
        groups.forEach { (base, words) ->
            words.split(' ').forEachIndexed { index, word -> if (word != "_") add(word to base + index) }
        }
        add("<=" to 0x3c); add(">=" to 0x3e); add("<>" to 0x3c)
    }.sortedByDescending { it.first.length }

    private val LINE_PATTERN = Regex("(\\d+)(?:\\s+|(?=:)|$)(.*)")
}

public sealed interface Pc1350BasicTokenizeResult {
    public data class Success(public val bytes: ByteArray) : Pc1350BasicTokenizeResult
    public data class Failure(public val error: Pc1350BasicTokenizeError) : Pc1350BasicTokenizeResult
}

public data class Pc1350BasicTokenizeError(val line: Int, val column: Int, val message: String)
