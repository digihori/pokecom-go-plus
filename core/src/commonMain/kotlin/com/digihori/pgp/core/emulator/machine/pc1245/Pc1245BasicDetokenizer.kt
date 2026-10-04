package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.source.basic.BasicCodeMeaning
import com.digihori.pgp.core.source.basic.BasicSpecialSymbol

public object Pc1245BasicDetokenizer {
    public fun detokenize(program: ByteArray): Pc1245BasicDetokenizeResult {
        val format = Pc1245BasicDialect.definition.programFormat
        if (program.isEmpty() || program[0].unsigned() != format.startMarker) {
            return failure(0, "PC-1245 BASIC program must start with 0xFF")
        }

        val output = StringBuilder()
        var offset = 1
        while (offset < program.size) {
            if (program[offset].unsigned() == format.endMarker) {
                if (offset != program.lastIndex) {
                    return failure(offset + 1, "Data follows the BASIC program end marker")
                }
                return Pc1245BasicDetokenizeResult.Success(output.toString())
            }
            if (offset + 1 >= program.size) return failure(offset, "Truncated BASIC line number")

            val high = program[offset].unsigned()
            val low = program[offset + 1].unsigned()
            val lineNumber = decodeLineNumber(high, low)
                ?: return failure(offset, "Invalid OLD-family BASIC line number")
            if (output.isNotEmpty()) output.append('\n')
            output.append(lineNumber).append(' ')
            offset += 2

            var inString = false
            var inRemark = false
            var afterKeyword = false
            var literalBeforeKeyword = false
            var terminated = false
            while (offset < program.size) {
                val code = program[offset].unsigned()
                if (code == format.lineTerminator) {
                    terminated = true
                    offset++
                    break
                }
                if (code == format.endMarker) {
                    return failure(offset, "BASIC line is missing its 0x00 terminator")
                }

                val meaning = Pc1245BasicDialect.definition.meaningOf(code)
                if (inString || inRemark) {
                    when (meaning) {
                        is BasicCodeMeaning.Character -> {
                            output.append(meaning.value)
                            if (meaning.value == '"' && inString) {
                                inString = false
                                literalBeforeKeyword = true
                            }
                        }
                        is BasicCodeMeaning.Special -> output.append(meaning.symbol.textForm())
                        is BasicCodeMeaning.Keyword, null -> output.append(rawByte(code))
                    }
                    offset++
                    continue
                }

                when (meaning) {
                    is BasicCodeMeaning.Keyword -> {
                        val symbolicOperator = meaning.text in SYMBOLIC_OPERATORS
                        if (!symbolicOperator && (afterKeyword || literalBeforeKeyword)) output.append(' ')
                        output.append(meaning.text)
                        afterKeyword = !symbolicOperator
                        literalBeforeKeyword = false
                        if (meaning.text == "REM") {
                            output.append(' ')
                            inRemark = true
                            afterKeyword = false
                        }
                    }
                    is BasicCodeMeaning.Character -> {
                        if (afterKeyword) output.append(' ')
                        output.append(meaning.value)
                        afterKeyword = false
                        literalBeforeKeyword = true
                        if (meaning.value == '"') inString = true
                    }
                    is BasicCodeMeaning.Special -> {
                        if (afterKeyword) output.append(' ')
                        output.append(meaning.symbol.textForm())
                        afterKeyword = false
                        literalBeforeKeyword = true
                    }
                    null -> {
                        if (afterKeyword) output.append(' ')
                        output.append(rawByte(code))
                        afterKeyword = false
                        literalBeforeKeyword = true
                    }
                }
                offset++
            }
            if (!terminated) return failure(offset, "Truncated BASIC line")
        }
        return failure(program.size, "PC-1245 BASIC program has no final 0xFF marker")
    }

    private fun decodeLineNumber(high: Int, low: Int): Int? {
        if (high and 0xf0 != 0xe0) return null
        val hundreds = high and 0x0f
        val tens = low ushr 4
        val ones = low and 0x0f
        if (hundreds > 9 || tens > 9 || ones > 9) return null
        val value = hundreds * 100 + tens * 10 + ones
        return value.takeIf { it in 1..Pc1245BasicDialect.definition.programFormat.maximumLineNumber }
    }

    private fun BasicSpecialSymbol.textForm(): String = when (this) {
        BasicSpecialSymbol.PI -> "π"
        BasicSpecialSymbol.SQUARE_ROOT -> "√"
        BasicSpecialSymbol.EXPONENT -> rawByte(0x4b)
        BasicSpecialSymbol.BLOCK -> rawByte(0x4c)
    }

    private fun rawByte(value: Int): String = "\\x" + value.toString(16).uppercase().padStart(2, '0')

    private fun Byte.unsigned(): Int = toInt() and 0xff

    private fun failure(offset: Int, message: String): Pc1245BasicDetokenizeResult.Failure =
        Pc1245BasicDetokenizeResult.Failure(Pc1245BasicDetokenizeError(offset, message))

    private val SYMBOLIC_OPERATORS: Set<String> = setOf(">=", "<=", "<>")
}

public sealed interface Pc1245BasicDetokenizeResult {
    public data class Success(public val source: String) : Pc1245BasicDetokenizeResult
    public data class Failure(public val error: Pc1245BasicDetokenizeError) : Pc1245BasicDetokenizeResult
}

public data class Pc1245BasicDetokenizeError(
    public val offset: Int,
    public val message: String,
)
