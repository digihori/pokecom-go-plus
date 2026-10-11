package com.digihori.pgp.core.debug

internal class AssemblySyntaxError(val line: Int, message: String) : RuntimeException(message)

internal sealed interface AssemblyExpression {
    data class Literal(val value: Long) : AssemblyExpression
    data class Name(val name: String) : AssemblyExpression
    data object Position : AssemblyExpression
    data class Unary(val operator: String, val operand: AssemblyExpression) : AssemblyExpression
    data class Binary(val operator: String, val left: AssemblyExpression, val right: AssemblyExpression) : AssemblyExpression

    fun evaluate(position: Int?, resolve: (String) -> Int): Int {
        fun checked(value: Long): Int {
            require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "Expression overflows signed 32-bit range" }
            return value.toInt()
        }
        return when (this) {
            is Literal -> checked(value)
            is Name -> resolve(name.uppercase())
            Position -> requireNotNull(position) { "Current position requires ORG and a resolved location" }
            is Unary -> {
                // Permit the spelling -2147483648 without permitting positive 2147483648.
                if (operator == "-" && operand is Literal) return checked(-operand.value)
                val value = operand.evaluate(position, resolve)
                when (operator) {
                    "+" -> value
                    "-" -> checked(-value.toLong())
                    "~" -> value.inv()
                    "HIGH", "LOW" -> {
                        require(value in 0..0xffff) { "$operator requires a 16-bit value" }
                        if (operator == "HIGH") value ushr 8 else value and 0xff
                    }
                    else -> error("Invalid unary operator")
                }
            }
            is Binary -> {
                val a = left.evaluate(position, resolve)
                val b = right.evaluate(position, resolve)
                when (operator) {
                    "+" -> checked(a.toLong() + b)
                    "-" -> checked(a.toLong() - b)
                    "*" -> checked(a.toLong() * b)
                    "/" -> { require(b != 0) { "Division by zero" }; checked(a.toLong() / b) }
                    "%" -> { require(b != 0) { "Division by zero" }; (a.toLong() % b).toInt() }
                    "<<" -> { require(b in 0..31) { "Shift count must be 0..31" }; checked(a.toLong() shl b) }
                    ">>" -> { require(b in 0..31) { "Shift count must be 0..31" }; a shr b }
                    "&" -> a and b
                    "^" -> a xor b
                    "|" -> a or b
                    else -> error("Invalid binary operator")
                }
            }
        }
    }
}

internal object AssemblyText {
    fun withoutComment(text: String): String {
        var quote: Char? = null
        var escaped = false
        text.forEachIndexed { index, c ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == quote) quote = null
            } else if (c == '\'' || c == '"') quote = c
            else if (c == ';' || c == '#') return text.substring(0, index)
        }
        return text
    }

    fun operands(text: String): List<String> {
        val result = mutableListOf<String>()
        var quote: Char? = null
        var escaped = false
        var depth = 0
        var start = 0
        text.forEachIndexed { index, c ->
            if (quote != null) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == quote) quote = null
            } else when (c) {
                '\'', '"' -> quote = c
                '(' -> depth++
                ')' -> { depth--; require(depth >= 0) { "Unmatched ')'" } }
                ',' -> if (depth == 0) { result += text.substring(start, index).trim(); start = index + 1 }
            }
        }
        require(quote == null) { "Unterminated string or character literal" }
        require(depth == 0) { "Unmatched '('" }
        result += text.substring(start).trim()
        require(result.none { it.isEmpty() }) { "Every comma-separated position requires a value" }
        return result
    }

    /** Explicit byte characters, without machine-specific text conversion. */
    fun quoted(text: String, quote: Char): List<Int> {
        require(text.length >= 2 && text.first() == quote && text.last() == quote) { "Unterminated literal" }
        val bytes = mutableListOf<Int>()
        var index = 1
        while (index < text.lastIndex) {
            var c = text[index++]
            require(c != quote) { "Unexpected text after literal" }
            val value = if (c == '\\') {
                require(index < text.lastIndex) { "Incomplete escape" }
                c = text[index++]
                when (c) {
                    'n' -> 10; 'r' -> 13; 't' -> 9; '0' -> 0
                    '\\', '\'', '"' -> c.code
                    'x' -> {
                        require(index + 2 <= text.lastIndex) { "Hex escape requires two digits" }
                        val digits = text.substring(index, index + 2)
                        require(digits.all { it.digitToIntOrNull(16) != null }) { "Invalid hex escape" }
                        val value = digits.toInt(16)
                        index += 2
                        value
                    }
                    else -> throw IllegalArgumentException("Unknown escape: \\$c")
                }
            } else c.code
            require(value in 0..255) { "Literal characters must fit one byte; use explicit machine character codes" }
            bytes += value
        }
        return bytes
    }
}

internal class AssemblyExpressionParser(private val text: String) {
    private var index = 0
    private var nodes = 0
    private var depth = 0
    fun parse(): AssemblyExpression {
        val expression = binary(0)
        space()
        require(index == text.length) { "Unexpected expression text: ${text.substring(index)}" }
        return expression
    }
    private fun space() { while (index < text.length && text[index].isWhitespace()) index++ }
    private fun binary(minimum: Int): AssemblyExpression {
        var left = primary()
        while (true) {
            space()
            val operator = listOf("<<", ">>", "+", "-", "*", "/", "%", "&", "^", "|").firstOrNull { text.startsWith(it, index) } ?: break
            val precedence = when (operator) { "|" -> 1; "^" -> 2; "&" -> 3; "<<", ">>" -> 4; "+", "-" -> 5; else -> 6 }
            if (precedence < minimum) break
            index += operator.length
            require(++nodes <= 128) { "Expression is too complex (maximum 128 nodes)" }
            left = AssemblyExpression.Binary(operator, left, binary(precedence + 1))
        }
        return left
    }
    private fun primary(): AssemblyExpression {
        require(++nodes <= 128) { "Expression is too complex (maximum 128 nodes)" }
        require(++depth <= 64) { "Expression nesting is too deep (maximum 64 levels)" }
        return try { primaryValue() } finally { depth-- }
    }
    private fun primaryValue(): AssemblyExpression {
        space()
        require(index < text.length) { "Expression is missing a value" }
        val c = text[index]
        if (c in listOf('+', '-', '~')) {
            index++
            return AssemblyExpression.Unary(c.toString(), primary())
        }
        if (c == '(') {
            index++
            val result = binary(0)
            space()
            require(index < text.length && text[index++] == ')') { "Expected ')'" }
            return result
        }
        if (c == '.') { index++; return AssemblyExpression.Position }
        if (c == '\'') {
            val start = index++
            var escaped = false
            while (index < text.length) {
                val next = text[index++]
                if (!escaped && next == '\'') {
                    val bytes = AssemblyText.quoted(text.substring(start, index), '\'')
                    require(bytes.size == 1) { "Character literal requires exactly one byte" }
                    return AssemblyExpression.Literal(bytes.single().toLong())
                }
                if (escaped) escaped = false else if (next == '\\') escaped = true
            }
            throw IllegalArgumentException("Unterminated character literal")
        }
        if (c.isDigit() || c == '$' || c == '&') {
            val radix = if (c == '$' || c == '&') { index++; 16 }
                else if (text.startsWith("0x", index, true)) { index += 2; 16 } else 10
            val start = index
            while (index < text.length && text[index].digitToIntOrNull(radix) != null) index++
            val value = text.substring(start, index).toLongOrNull(radix)
            require(value != null && value <= 2147483648L) { "Invalid or overflowing numeric literal" }
            return AssemblyExpression.Literal(value)
        }
        require(c.isLetter() && c.code < 128 || c == '_') { "Invalid expression character: $c" }
        val start = index++
        while (index < text.length && (text[index].isLetterOrDigit() && text[index].code < 128 || text[index] == '_')) index++
        val name = text.substring(start, index)
        space()
        if (name.uppercase() in listOf("HIGH", "LOW") && index < text.length && text[index] == '(') {
            index++
            val operand = binary(0)
            space()
            require(index < text.length && text[index++] == ')') { "Expected ')'" }
            return AssemblyExpression.Unary(name.uppercase(), operand)
        }
        return AssemblyExpression.Name(name)
    }
}
