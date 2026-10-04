package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol
import com.digihori.pgp.core.source.basic.BasicTextDocument
import com.digihori.pgp.core.source.basic.BasicTextElement
import com.digihori.pgp.core.source.basic.BasicTextToken

/** Compiles parsed BASIC text into consecutive taps accepted by the PC-1245 ROM editor. */
public object Pc1245RomBasicInput {
    public fun compile(document: BasicTextDocument): Pc1245RomInputResult = RomBasicInputCompiler.compile(
        document = document,
        characterSequence = Pc1245CharacterInput::keySequence,
        specialSequence = { symbol ->
            when (symbol) {
                BasicSpecialSymbol.PI -> listOf(PocketKey.SHIFT, PocketKey.NUM_0)
                BasicSpecialSymbol.SQUARE_ROOT -> listOf(PocketKey.SHIFT, PocketKey.DOT)
                BasicSpecialSymbol.EXPONENT -> listOf(PocketKey.SHIFT, PocketKey.PLUS)
                BasicSpecialSymbol.BLOCK -> null
            }
        },
    )
}

internal object RomBasicInputCompiler {
    fun compile(
        document: BasicTextDocument,
        characterSequence: (Char) -> List<PocketKey>?,
        specialSequence: (BasicSpecialSymbol) -> List<PocketKey>?,
    ): Pc1245RomInputResult {
        val keys = mutableListOf<PocketKey>()
        var linePrefix = LinePrefix.START
        for (token in document.tokens) {
            when (val element = token.element) {
                is BasicTextElement.Character -> {
                    val normalizedCharacter = if (
                        element.value == ':' &&
                        (linePrefix == LinePrefix.LINE_NUMBER || linePrefix == LinePrefix.AFTER_LINE_NUMBER)
                    ) {
                        linePrefix = LinePrefix.BODY
                        ' '
                    } else {
                        updateLinePrefix(linePrefix, element.value).also { linePrefix = it }
                        element.value
                    }
                    val sequence = characterSequence(normalizedCharacter)
                        ?: return unsupported(
                            token,
                            Pc1245RomInputUnsupported.Character(normalizedCharacter),
                        )
                    keys += sequence
                }
                is BasicTextElement.Special -> {
                    val sequence = specialSequence(element.symbol)
                        ?: return unsupported(
                        token,
                        Pc1245RomInputUnsupported.SpecialSymbol(element.symbol),
                    )
                    linePrefix = LinePrefix.BODY
                    keys += sequence
                }
                is BasicTextElement.RawByte -> return unsupported(
                    token,
                    Pc1245RomInputUnsupported.RawByte(element.value),
                )
                BasicTextElement.LineBreak -> {
                    keys += PocketKey.ENTER
                    linePrefix = LinePrefix.START
                }
            }
        }

        if (document.tokens.isNotEmpty() && document.tokens.last().element != BasicTextElement.LineBreak) {
            keys += PocketKey.ENTER
        }
        return Pc1245RomInputResult.Success(keys)
    }

    private fun unsupported(
        token: BasicTextToken,
        unsupported: Pc1245RomInputUnsupported,
    ): Pc1245RomInputResult.Failure = Pc1245RomInputResult.Failure(
        Pc1245RomInputError(token.line, token.column, unsupported),
    )

    private fun updateLinePrefix(current: LinePrefix, character: Char): LinePrefix = when (current) {
        LinePrefix.START -> if (character in '0'..'9') LinePrefix.LINE_NUMBER else LinePrefix.BODY
        LinePrefix.LINE_NUMBER -> when {
            character in '0'..'9' -> LinePrefix.LINE_NUMBER
            character == ' ' -> LinePrefix.AFTER_LINE_NUMBER
            else -> LinePrefix.BODY
        }
        LinePrefix.AFTER_LINE_NUMBER -> if (character == ' ') LinePrefix.AFTER_LINE_NUMBER else LinePrefix.BODY
        LinePrefix.BODY -> LinePrefix.BODY
    }

    private enum class LinePrefix {
        START,
        LINE_NUMBER,
        AFTER_LINE_NUMBER,
        BODY,
    }
}

public sealed interface Pc1245RomInputResult {
    public class Success(keys: List<PocketKey>) : Pc1245RomInputResult {
        public val keys: List<PocketKey> = keys.toList()
    }
    public data class Failure(public val error: Pc1245RomInputError) : Pc1245RomInputResult
}

public data class Pc1245RomInputError(
    public val line: Int,
    public val column: Int,
    public val unsupported: Pc1245RomInputUnsupported,
)

public sealed interface Pc1245RomInputUnsupported {
    public data class Character(public val value: Char) : Pc1245RomInputUnsupported
    public data class SpecialSymbol(public val value: BasicSpecialSymbol) : Pc1245RomInputUnsupported
    public data class RawByte(public val value: Int) : Pc1245RomInputUnsupported
}
