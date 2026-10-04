package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicTextDocument
import com.digihori.pgp.core.source.basic.BasicTextElement
import com.digihori.pgp.core.source.basic.BasicTextToken

public object Pc1245BasicTokenizer {
    public fun tokenize(document: BasicTextDocument): Pc1245BasicTokenizeResult {
        val output = mutableListOf(Pc1245BasicDialect.definition.programFormat.startMarker)
        val lines = splitLines(document.tokens)

        for (line in lines) {
            if (line.all { it.isSpace() }) continue
            val encoded = encodeLine(line)
            if (encoded is LineResult.Failure) return Pc1245BasicTokenizeResult.Failure(encoded.error)
            output += (encoded as LineResult.Success).bytes
        }

        output += Pc1245BasicDialect.definition.programFormat.endMarker
        return Pc1245BasicTokenizeResult.Success(output.map(Int::toByte).toByteArray())
    }

    private fun encodeLine(tokens: List<BasicTextToken>): LineResult {
        var index = tokens.indexOfFirst { !it.isSpace() }
        val first = tokens[index]
        val digits = buildString {
            while (index < tokens.size) {
                val character = (tokens[index].element as? BasicTextElement.Character)?.value
                if (character?.isDigit() != true) break
                append(character)
                index++
            }
        }
        if (digits.isEmpty()) return failure(first, "BASIC line must start with a line number")
        val lineNumber = digits.toIntOrNull()
            ?: return failure(first, "Invalid BASIC line number")
        if (lineNumber !in 1..Pc1245BasicDialect.definition.programFormat.maximumLineNumber) {
            return failure(first, "BASIC line number must be between 1 and 999")
        }
        if (index < tokens.size && !tokens[index].isSpace() && !tokens[index].isCharacter(':')) {
            return failure(tokens[index], "Line number must be followed by whitespace or ':'")
        }

        while (index < tokens.size && tokens[index].isSpace()) index++
        if (index < tokens.size && tokens[index].isCharacter(':')) {
            index++
            while (index < tokens.size && tokens[index].isSpace()) index++
        }

        val output = mutableListOf(
            0xe0 or (lineNumber / 100),
            ((lineNumber / 10) % 10 shl 4) or (lineNumber % 10),
        )
        var inString = false
        var inRemark = false
        var skipRemarkSeparator = false

        while (index < tokens.size) {
            val token = tokens[index]
            when (val element = token.element) {
                is BasicTextElement.RawByte -> {
                    if (element.value == 0x00 || element.value == 0xff) {
                        return failure(token, "Raw byte would conflict with BASIC program framing")
                    }
                    output += element.value
                    index++
                }
                is BasicTextElement.Special -> {
                    output += Pc1245BasicDialect.definition.codeOf(element.symbol)
                        ?: return failure(token, "Special symbol is not supported by PC-1245")
                    index++
                }
                BasicTextElement.LineBreak -> error("Line breaks must be removed before line encoding")
                is BasicTextElement.Character -> {
                    val character = element.value
                    if (character == '"') {
                        output += requireCharacterCode(token, character) ?: return unsupportedCharacter(token)
                        inString = !inString
                        skipRemarkSeparator = false
                        index++
                        continue
                    }
                    if (inString || inRemark) {
                        if (inRemark && skipRemarkSeparator && character == ' ') {
                            skipRemarkSeparator = false
                            index++
                            continue
                        }
                        skipRemarkSeparator = false
                        output += requireCharacterCode(token, character) ?: return unsupportedCharacter(token)
                        index++
                        continue
                    }
                    if (character == ' ') {
                        index++
                        continue
                    }

                    val wordEnd = wordEnd(tokens, index)
                    if (wordEnd > index) {
                        val word = tokens.subList(index, wordEnd).joinToString("") {
                            ((it.element as BasicTextElement.Character).value).toString()
                        }
                        val keywordCode = Pc1245BasicDialect.definition.codeOfKeyword(word)
                        if (keywordCode != null) {
                            output += keywordCode
                            inRemark = word.equals("REM", ignoreCase = true)
                            skipRemarkSeparator = inRemark
                        } else {
                            tokens.subList(index, wordEnd).forEach { wordToken ->
                                val value = (wordToken.element as BasicTextElement.Character).value
                                output += requireCharacterCode(wordToken, value)
                                    ?: return unsupportedCharacter(wordToken)
                            }
                        }
                        index = wordEnd
                        continue
                    }

                    val pair = if (index + 1 < tokens.size) {
                        val next = (tokens[index + 1].element as? BasicTextElement.Character)?.value
                        if (next != null) "$character$next" else null
                    } else null
                    val pairCode = pair?.let(Pc1245BasicDialect.definition::codeOfKeyword)
                    if (pairCode != null) {
                        output += pairCode
                        index += 2
                    } else {
                        output += requireCharacterCode(token, character) ?: return unsupportedCharacter(token)
                        index++
                    }
                }
            }
        }

        output += Pc1245BasicDialect.definition.programFormat.lineTerminator
        return LineResult.Success(output)
    }

    private fun wordEnd(tokens: List<BasicTextToken>, start: Int): Int {
        var end = start
        while (end < tokens.size) {
            val character = (tokens[end].element as? BasicTextElement.Character)?.value ?: break
            if (!character.isLetter() && character != '$') break
            end++
        }
        return end
    }

    private fun requireCharacterCode(token: BasicTextToken, character: Char): Int? =
        Pc1245BasicDialect.definition.codeOf(character)

    private fun unsupportedCharacter(token: BasicTextToken): LineResult.Failure {
        val character = (token.element as BasicTextElement.Character).value
        return failure(token, "Character '$character' is not supported by PC-1245")
    }

    private fun splitLines(tokens: List<BasicTextToken>): List<List<BasicTextToken>> {
        val lines = mutableListOf<MutableList<BasicTextToken>>(mutableListOf())
        tokens.forEach { token ->
            if (token.element == BasicTextElement.LineBreak) lines.add(mutableListOf())
            else lines.last() += token
        }
        return lines
    }

    private fun BasicTextToken.isSpace(): Boolean = isCharacter(' ')

    private fun BasicTextToken.isCharacter(expected: Char): Boolean =
        (element as? BasicTextElement.Character)?.value == expected

    private fun failure(token: BasicTextToken, message: String): LineResult.Failure =
        LineResult.Failure(Pc1245BasicTokenizeError(token.line, token.column, message))

    private sealed interface LineResult {
        data class Success(val bytes: List<Int>) : LineResult
        data class Failure(val error: Pc1245BasicTokenizeError) : LineResult
    }
}

public sealed interface Pc1245BasicTokenizeResult {
    public data class Success(public val bytes: ByteArray) : Pc1245BasicTokenizeResult
    public data class Failure(public val error: Pc1245BasicTokenizeError) : Pc1245BasicTokenizeResult
}

public data class Pc1245BasicTokenizeError(
    public val line: Int,
    public val column: Int,
    public val message: String,
)
