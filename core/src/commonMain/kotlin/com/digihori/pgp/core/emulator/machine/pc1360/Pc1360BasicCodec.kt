package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextDocument
import com.digihori.pgp.core.source.basic.BasicTextElement

public object Pc1360BasicCodec {
    public fun tokenize(document: BasicTextDocument): Pc1360BasicTokenizeResult {
        val lines = mutableListOf<MutableList<Pair<BasicTextElement, Int>>>(mutableListOf())
        document.tokens.forEach { token ->
            if (token.element == BasicTextElement.LineBreak) lines.add(mutableListOf())
            else lines.last() += token.element to token.column
        }
        val output = mutableListOf(0xff)
        lines.forEachIndexed { index, elements ->
            if (elements.isEmpty()) return@forEachIndexed
            var position = 0
            while (position < elements.size && (elements[position].first as? BasicTextElement.Character)?.value?.isWhitespace() == true) position++
            val numberStart = position
            while (position < elements.size && (elements[position].first as? BasicTextElement.Character)?.value?.isDigit() == true) position++
            if (position == numberStart) return failure(index + 1, 1, "BASIC line must start with a line number")
            val lineNumber = elements.subList(numberStart, position).joinToString("") {
                ((it.first as BasicTextElement.Character).value).toString()
            }.toIntOrNull() ?: return failure(index + 1, 1, "Invalid BASIC line number")
            if (lineNumber !in 1..65279) return failure(index + 1, 1, "BASIC line number must be between 1 and 65279")
            while (position < elements.size && (elements[position].first as? BasicTextElement.Character)?.value?.let { it == ':' || it.isWhitespace() } == true) position++
            val bodyElements = elements.drop(position)
            val encoded = encodeBody(bodyElements, index + 1)
            if (encoded is BodyResult.Failure) return Pc1360BasicTokenizeResult.Failure(encoded.error)
            val bytes = (encoded as BodyResult.Success).bytes
            if (bytes.size + 1 > 0xff) return failure(index + 1, 1, "Encoded S2 line is too long")
            output += lineNumber ushr 8; output += lineNumber and 0xff; output += bytes.size + 1
            output += bytes; output += 0x0d
        }
        output += 0xff
        return Pc1360BasicTokenizeResult.Success(output.map(Int::toByte).toByteArray())
    }

    private fun encodeBody(elements: List<Pair<BasicTextElement, Int>>, line: Int): BodyResult {
        val chars = elements.map { (it.first as? BasicTextElement.Character)?.value ?: '\u0000' }.joinToString("")
        val output = mutableListOf<Int>()
        var i = 0; var quoted = false; var remark = false; var lineReference = false
        while (i < elements.size) {
            val (element, column) = elements[i]
            val char = (element as? BasicTextElement.Character)?.value
            if (!quoted && !remark && char?.isWhitespace() == true) { i++; continue }
            if (!quoted && !remark) {
                val keyword = KEYWORDS.firstOrNull { (word, _) ->
                    chars.regionMatches(i, word, 0, word.length, true) && boundary(chars, i, word)
                }
                if (keyword != null) {
                    output += 0xfe; output += keyword.second
                    remark = keyword.first == "REM"
                    lineReference = keyword.first in setOf("GOTO", "GOSUB", "THEN")
                    i += keyword.first.length
                    continue
                }
                if (lineReference && char?.isDigit() == true) {
                    var end = i
                    while (end < elements.size && (elements[end].first as? BasicTextElement.Character)?.value?.isDigit() == true) end++
                    val number = chars.substring(i, end).toInt()
                    output += 0x1f; output += number ushr 8; output += number and 0xff
                    lineReference = false; i = end; continue
                }
            }
            when (element) {
                is BasicTextElement.Character -> when {
                    element.value == '"' -> { output += 0x22; quoted = !quoted }
                    element.value.code in 0x20..0x7e -> output += element.value.code
                    element.value.code in 0xff61..0xff9f -> output += 0xa1 + element.value.code - 0xff61
                    else -> return BodyResult.Failure(Pc1360BasicTokenizeError(line, column, "Character '${element.value}' is not supported by PC-1360"))
                }
                is BasicTextElement.RawByte -> output += element.value
                is BasicTextElement.Special -> output += when (element.symbol) {
                    BasicSpecialSymbol.PI -> 0xfb
                    BasicSpecialSymbol.SQUARE_ROOT -> 0xfc
                    BasicSpecialSymbol.EXPONENT -> 'E'.code
                    BasicSpecialSymbol.BLOCK -> 0xf9
                }
                BasicTextElement.LineBreak -> Unit
            }
            i++
        }
        if (quoted) return BodyResult.Failure(Pc1360BasicTokenizeError(line, elements.lastOrNull()?.second ?: 1, "Unterminated string"))
        return BodyResult.Success(output)
    }

    public fun detokenize(bytes: ByteArray): Pc1360BasicDetokenizeResult {
        if (bytes.size < 2 || bytes.first().toInt() and 0xff != 0xff) return decodeFailure(0, "Missing S2 start marker")
        val lines = mutableListOf<String>(); var offset = 1
        while (offset < bytes.lastIndex) {
            if (offset + 3 > bytes.lastIndex) return decodeFailure(offset, "Truncated S2 line header")
            val number = ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
            val length = bytes[offset + 2].toInt() and 0xff
            if (length < 1 || offset + 3 + length > bytes.size) return decodeFailure(offset + 2, "Invalid S2 line length")
            val end = offset + 3 + length
            if (bytes[end - 1].toInt() and 0xff != 0x0d) return decodeFailure(end - 1, "Missing S2 line terminator")
            val body = StringBuilder(); var p = offset + 3; var quoted = false; var remark = false
            while (p < end - 1) {
                val value = bytes[p].toInt() and 0xff
                when {
                    !quoted && !remark && value == 0xfe && p + 1 < end - 1 -> {
                        val keyword = TOKEN_NAMES[bytes[p + 1].toInt() and 0xff]
                            ?: return decodeFailure(p, "Unknown S2 token")
                        if (body.isNotEmpty() && body.last().isLetterOrDigit()) body.append(' ')
                        body.append(keyword)
                        if (keyword == "REM") { body.append(' '); remark = true }
                        p += 2; continue
                    }
                    !quoted && !remark && value == 0x1f && p + 2 < end - 1 -> {
                        if (body.isNotEmpty() && body.last() != ' ') body.append(' ')
                        body.append(((bytes[p + 1].toInt() and 0xff) shl 8) or (bytes[p + 2].toInt() and 0xff))
                        p += 3; continue
                    }
                    value == 0x22 -> { body.append('"'); quoted = !quoted }
                    value in 0x20..0x7e -> body.append(value.toChar())
                    value in 0xa1..0xdf -> body.append((0xff61 + value - 0xa1).toChar())
                    value == 0xfb -> body.append('π')
                    value == 0xfc -> body.append('√')
                    else -> body.append("\\x${value.toString(16).uppercase().padStart(2, '0')}")
                }
                p++
            }
            lines += "$number:${body.toString().trimEnd()}"
            offset = end
        }
        if (offset != bytes.lastIndex || bytes.last().toInt() and 0xff != 0xff) return decodeFailure(offset, "Missing S2 end marker")
        return Pc1360BasicDetokenizeResult.Success(lines.joinToString("\n"))
    }

    private fun boundary(text: String, start: Int, keyword: String): Boolean {
        val before = text.getOrNull(start - 1); val after = text.getOrNull(start + keyword.length)
        return (before == null || !before.isLetterOrDigit() && before != '$') &&
            (after == null || !after.isLetterOrDigit() && after != '$')
    }
    private fun failure(l: Int, c: Int, m: String) = Pc1360BasicTokenizeResult.Failure(Pc1360BasicTokenizeError(l, c, m))
    private fun decodeFailure(o: Int, m: String) = Pc1360BasicDetokenizeResult.Failure(Pc1360BasicDetokenizeError(o, m))
    private sealed interface BodyResult { data class Success(val bytes: List<Int>) : BodyResult; data class Failure(val error: Pc1360BasicTokenizeError) : BodyResult }
    private val TOKEN_NAMES = mapOf(
        0x10 to "RUN", 0x11 to "NEW", 0x12 to "CONT", 0x13 to "PASS", 0x14 to "LIST", 0x15 to "LLIST", 0x16 to "CLOAD", 0x17 to "MERGE", 0x18 to "LOAD", 0x19 to "RENUM", 0x1a to "AUTO", 0x1b to "DELETE", 0x1c to "FILES", 0x1d to "INIT", 0x1e to "CONVERT",
        0x20 to "CSAVE", 0x21 to "OPEN", 0x22 to "CLOSE", 0x23 to "SAVE", 0x24 to "CONSOLE", 0x25 to "RANDOM", 0x26 to "DEGREE", 0x27 to "RADIAN", 0x28 to "GRAD", 0x29 to "BEEP", 0x2a to "WAIT", 0x2b to "GOTO", 0x2c to "TRON", 0x2d to "TROFF", 0x2e to "CLEAR", 0x2f to "USING",
        0x30 to "DIM", 0x31 to "CALL", 0x32 to "POKE", 0x33 to "GPRINT", 0x34 to "PSET", 0x35 to "PRESET", 0x36 to "BASIC", 0x37 to "TEXT", 0x38 to "WIDTH", 0x3a to "ERASE", 0x3b to "LFILES", 0x3c to "KILL", 0x3d to "COPY", 0x3e to "NAME", 0x3f to "SET",
        0x40 to "LTEXT", 0x41 to "GRAPH", 0x42 to "LF", 0x43 to "CSIZE", 0x44 to "COLOR", 0x46 to "DEFDBL", 0x47 to "DEFSNG",
        0x50 to "CLS", 0x51 to "CURSOR", 0x52 to "TO", 0x53 to "STEP", 0x54 to "THEN", 0x55 to "ON", 0x56 to "IF", 0x57 to "FOR", 0x58 to "LET", 0x59 to "REM", 0x5a to "END", 0x5b to "NEXT", 0x5c to "STOP", 0x5d to "READ", 0x5e to "DATA", 0x5f to "PAUSE",
        0x60 to "PRINT", 0x61 to "INPUT", 0x62 to "GOSUB", 0x63 to "AREAD", 0x64 to "LPRINT", 0x65 to "RETURN", 0x66 to "RESTORE", 0x67 to "CHAIN", 0x68 to "GCURSOR", 0x69 to "LINE", 0x6a to "LLINE", 0x6b to "RLINE", 0x6c to "GLCURSOR", 0x6d to "SORGN", 0x6e to "CROTATE", 0x6f to "CIRCLE",
        0x70 to "PAINT", 0x71 to "OUTPUT", 0x72 to "APPEND", 0x73 to "AS", 0x74 to "ARUN", 0x75 to "AUTOGOTO", 0x78 to "ERROR",
        0x80 to "MDF", 0x81 to "REC", 0x82 to "POL", 0x83 to "ROT", 0x84 to "DECI", 0x85 to "HEX", 0x86 to "TEN", 0x87 to "RCP", 0x88 to "SQU", 0x89 to "CUR", 0x8a to "HSN", 0x8b to "HCS", 0x8c to "HTN", 0x8d to "AHS", 0x8e to "AHC", 0x8f to "AHT",
        0x90 to "FACT", 0x91 to "LN", 0x92 to "LOG", 0x93 to "EXP", 0x94 to "SQR", 0x95 to "SIN", 0x96 to "COS", 0x97 to "TAN", 0x98 to "INT", 0x99 to "ABS", 0x9a to "SGN", 0x9b to "DEG", 0x9c to "DMS", 0x9d to "ASN", 0x9e to "ACS", 0x9f to "ATN",
        0xa0 to "RND", 0xa1 to "AND", 0xa2 to "OR", 0xa3 to "NOT", 0xa4 to "PEEK", 0xa5 to "XOR", 0xad to "POINT", 0xae to "PI", 0xaf to "MEM",
        0xb0 to "EOF", 0xb1 to "DSKF", 0xb2 to "LOF", 0xb3 to "LOC", 0xb6 to "NCR", 0xb7 to "NRR", 0xc0 to "ERN", 0xc1 to "ERL", 0xd0 to "ASC", 0xd1 to "VAL", 0xd2 to "LEN", 0xd4 to "KLEN",
        0xe0 to "AKCNV$", 0xe1 to "KACNV$", 0xe2 to "JIS$", 0xe8 to "OPEN$", 0xe9 to "INKEY$", 0xea to "MID$", 0xeb to "LEFT$", 0xec to "RIGHT$", 0xed to "KMID$", 0xee to "KLEFT$", 0xef to "KRIGHT$", 0xf0 to "CHR$", 0xf1 to "STR$", 0xf2 to "HEX$",
    )
    private val KEYWORDS = TOKEN_NAMES.map { (code, name) -> name to code }.sortedByDescending { it.first.length }
}

public sealed interface Pc1360BasicTokenizeResult { public data class Success(public val bytes: ByteArray) : Pc1360BasicTokenizeResult; public data class Failure(public val error: Pc1360BasicTokenizeError) : Pc1360BasicTokenizeResult }
public data class Pc1360BasicTokenizeError(val line: Int, val column: Int, val message: String)
public sealed interface Pc1360BasicDetokenizeResult { public data class Success(public val source: String) : Pc1360BasicDetokenizeResult; public data class Failure(public val error: Pc1360BasicDetokenizeError) : Pc1360BasicDetokenizeResult }
public data class Pc1360BasicDetokenizeError(val offset: Int, val message: String)
