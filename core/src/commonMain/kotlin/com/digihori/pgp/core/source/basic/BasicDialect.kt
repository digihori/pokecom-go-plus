package com.digihori.pgp.core.source.basic

public data class BasicDialectId(public val value: String) {
    init {
        require(value.isNotBlank()) { "BASIC dialect ID must not be blank" }
    }
}

public data class BasicProgramFormat(
    public val startMarker: Int,
    public val endMarker: Int,
    public val lineTerminator: Int,
    public val maximumLineNumber: Int,
    public val lineNumberEncoding: BasicLineNumberEncoding,
) {
    init {
        require(startMarker in 0x00..0xff)
        require(endMarker in 0x00..0xff)
        require(lineTerminator in 0x00..0xff)
        require(maximumLineNumber > 0)
    }
}

public enum class BasicLineNumberEncoding {
    OLD_PACKED_DECIMAL_3_DIGIT,
}

public sealed interface BasicCodeMeaning {
    public data class Character(public val value: Char) : BasicCodeMeaning
    public data class Special(public val symbol: BasicSpecialSymbol) : BasicCodeMeaning
    public data class Keyword(public val text: String) : BasicCodeMeaning
}

public class BasicDialect(
    public val id: BasicDialectId,
    public val programFormat: BasicProgramFormat,
    codeMeanings: Map<Int, BasicCodeMeaning>,
    canonicalCharacterCodes: Map<Char, Int>,
    canonicalSpecialCodes: Map<BasicSpecialSymbol, Int>,
) {
    private val meanings: Map<Int, BasicCodeMeaning> = codeMeanings.toMap()
    private val characterCodes: Map<Char, Int> = canonicalCharacterCodes.toMap()
    private val specialCodes: Map<BasicSpecialSymbol, Int> = canonicalSpecialCodes.toMap()
    private val keywordCodes: Map<String, Int> = meanings.entries
        .mapNotNull { (code, meaning) ->
            (meaning as? BasicCodeMeaning.Keyword)?.let { it.text.uppercase() to code }
        }
        .toMap()

    init {
        require(meanings.keys.all { it in 0x00..0xff }) { "BASIC codes must fit in one byte" }
        require(characterCodes.values.all { it in 0x00..0xff }) { "Character codes must fit in one byte" }
        characterCodes.forEach { (character, code) ->
            require(meanings[code] == BasicCodeMeaning.Character(character)) {
                "Canonical character code must reference the same decoded character"
            }
        }
        specialCodes.forEach { (symbol, code) ->
            require(meanings[code] == BasicCodeMeaning.Special(symbol)) {
                "Canonical special code must reference the same decoded symbol"
            }
        }
    }

    public fun meaningOf(code: Int): BasicCodeMeaning? = meanings[code]

    public fun codeOf(character: Char): Int? = characterCodes[character.uppercaseChar()]

    public fun codeOf(symbol: BasicSpecialSymbol): Int? = specialCodes[symbol]

    public fun codeOfKeyword(keyword: String): Int? = keywordCodes[keyword.uppercase()]
}
