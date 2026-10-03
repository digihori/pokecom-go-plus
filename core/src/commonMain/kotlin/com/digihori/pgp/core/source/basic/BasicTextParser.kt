package com.digihori.pgp.core.source.basic

public object BasicTextParser {
    public fun parse(source: String): BasicTextParseResult {
        val normalized = source
            .removePrefix(UTF8_BOM.toString())
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val tokens = mutableListOf<BasicTextToken>()
        var index = 0
        var line = 1
        var column = 1

        while (index < normalized.length) {
            val character = normalized[index]
            when (character) {
                '\n' -> {
                    tokens += BasicTextToken(BasicTextElement.LineBreak, line, column)
                    index++
                    line++
                    column = 1
                }
                'π' -> {
                    tokens += BasicTextToken(BasicTextElement.Special(BasicSpecialSymbol.PI), line, column)
                    index++
                    column++
                }
                '√' -> {
                    tokens += BasicTextToken(BasicTextElement.Special(BasicSpecialSymbol.SQUARE_ROOT), line, column)
                    index++
                    column++
                }
                '\\' -> {
                    val escape = parseEscape(normalized, index, line, column)
                    when (escape) {
                        is EscapeParseResult.Failure -> return BasicTextParseResult.Failure(escape.error)
                        is EscapeParseResult.Success -> {
                            tokens += BasicTextToken(escape.element, line, column)
                            index += escape.consumedCharacters
                            column += escape.consumedCharacters
                        }
                    }
                }
                else -> {
                    tokens += BasicTextToken(BasicTextElement.Character(character), line, column)
                    index++
                    column++
                }
            }
        }

        return BasicTextParseResult.Success(BasicTextDocument(tokens))
    }

    private fun parseEscape(source: String, start: Int, line: Int, column: Int): EscapeParseResult {
        if (start + 1 >= source.length) {
            return failure(line, column, "Trailing escape character")
        }
        if (source[start + 1] == '\\') {
            return EscapeParseResult.Success(BasicTextElement.Character('\\'), 2)
        }
        if (source[start + 1] == 'x') {
            if (start + 3 >= source.length) {
                return failure(line, column, "Raw byte escape requires two hexadecimal digits")
            }
            val digits = source.substring(start + 2, start + 4)
            val value = digits.toIntOrNull(16)
                ?: return failure(line, column, "Invalid raw byte escape: \\x$digits")
            return EscapeParseResult.Success(BasicTextElement.RawByte(value), 4)
        }

        var end = start + 1
        while (end < source.length && source[end].isLetter()) end++
        val name = source.substring(start + 1, end)
        val symbol = NAMED_ESCAPES[name]
            ?: return failure(line, column, "Unknown escape: \\$name")
        return EscapeParseResult.Success(BasicTextElement.Special(symbol), end - start)
    }

    private fun failure(line: Int, column: Int, message: String): EscapeParseResult.Failure =
        EscapeParseResult.Failure(BasicTextParseError(line, column, message))

    private sealed interface EscapeParseResult {
        data class Success(
            val element: BasicTextElement,
            val consumedCharacters: Int,
        ) : EscapeParseResult

        data class Failure(val error: BasicTextParseError) : EscapeParseResult
    }

    private val NAMED_ESCAPES: Map<String, BasicSpecialSymbol> = mapOf(
        "PI" to BasicSpecialSymbol.PI,
        "SQR" to BasicSpecialSymbol.SQUARE_ROOT,
        "EX" to BasicSpecialSymbol.EXPONENT,
        "BX" to BasicSpecialSymbol.BLOCK,
    )

    private const val UTF8_BOM: Char = '\uFEFF'
}

public class BasicTextDocument internal constructor(tokens: List<BasicTextToken>) {
    public val tokens: List<BasicTextToken> = tokens.toList()
    public val elements: List<BasicTextElement> = this.tokens.map(BasicTextToken::element)
}

public data class BasicTextToken(
    public val element: BasicTextElement,
    public val line: Int,
    public val column: Int,
)

public sealed interface BasicTextElement {
    public data class Character(public val value: Char) : BasicTextElement
    public data class Special(public val symbol: BasicSpecialSymbol) : BasicTextElement
    public data class RawByte(public val value: Int) : BasicTextElement {
        init {
            require(value in 0x00..0xff) { "Raw byte must fit in one byte" }
        }
    }
    public data object LineBreak : BasicTextElement
}

public enum class BasicSpecialSymbol {
    PI,
    SQUARE_ROOT,
    EXPONENT,
    BLOCK,
}

public sealed interface BasicTextParseResult {
    public data class Success(public val document: BasicTextDocument) : BasicTextParseResult
    public data class Failure(public val error: BasicTextParseError) : BasicTextParseResult
}

public data class BasicTextParseError(
    public val line: Int,
    public val column: Int,
    public val message: String,
)
