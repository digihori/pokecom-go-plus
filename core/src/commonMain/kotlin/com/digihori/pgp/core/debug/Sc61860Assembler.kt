package com.digihori.pgp.core.debug

import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult
import com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory
import com.digihori.pgp.core.source.machine.AddressedMemorySegmentData

public sealed interface Sc61860AssemblyResult {
    public data class Success(
        public val image: AddressedMemoryImage,
        public val symbols: List<Sc61860AssemblySymbol>,
        public val listing: List<Sc61860AssemblyListingLine>,
    ) : Sc61860AssemblyResult
    public data class Failure(public val line: Int, public val message: String) : Sc61860AssemblyResult
}

public enum class Sc61860AssemblySymbolKind { LABEL, CONSTANT }

public data class Sc61860AssemblySymbol(
    public val name: String,
    public val address: Int,
    public val line: Int,
    public val kind: Sc61860AssemblySymbolKind = Sc61860AssemblySymbolKind.LABEL,
) {
    public val value: Int get() = address
}

public data class Sc61860AssemblyListingLine(
    public val line: Int,
    public val address: Int,
    public val bytes: List<Int>,
    public val sourceText: String,
)

public object Sc61860Assembler {
    public fun assemble(source: String): Sc61860AssemblyResult = try {
        Assembly(source).run()
    } catch (error: AssemblySyntaxError) {
        Sc61860AssemblyResult.Failure(error.line, error.message.orEmpty())
    }

    private class Assembly(source: String) {
        private val statements = parseStatements(source)
        private val labels = mutableMapOf<String, Int>()
        private val declarations = linkedMapOf<String, Statement>()
        private val constants = mutableMapOf<String, Statement>()
        private val constantValues = mutableMapOf<String, Int>()
        private val positions = mutableMapOf<Int, Int?>()
        private val sizes = mutableMapOf<Int, Int>()
        private val operands = mutableMapOf<Int, List<String>>()
        private val expressions = mutableMapOf<String, AssemblyExpression>()
        private val resolving = mutableSetOf<String>()

        private fun <T> at(statement: Statement, action: () -> T): T = try {
            action()
        } catch (error: IllegalArgumentException) {
            throw AssemblySyntaxError(statement.line, error.message.orEmpty())
        }

        private fun evaluate(text: String, position: Int?, layout: Boolean): Int =
            expressions.getOrPut(text) { AssemblyExpressionParser(text).parse() }.evaluate(position) { name ->
                labels[name] ?: constantValues[name] ?: constants[name]?.let { constant ->
                    require(resolving.size < 32) { "Constant dependency chain is too deep" }
                    require(resolving.add(name)) { "Circular EQU dependency: ${(resolving + name).joinToString(" -> ")}" }
                    try {
                        at(constant) { evaluate(constant.operand, positions[constant.line], layout) }.also { constantValues[name] = it }
                    } finally { resolving.remove(name) }
                } ?: throw IllegalArgumentException(if (layout && name in declarations)
                    "Layout expression requires an already resolved label: $name" else "Undefined symbol: $name")
            }

        fun run(): Sc61860AssemblyResult {
            statements.forEach { statement -> at(statement) {
                if (statement.operation == "EQU") require(statement.label != null) { "EQU requires a name" }
                statement.label?.let { name ->
                    val key = name.uppercase()
                    require(key !in declarations) { "Duplicate symbol: $name (first defined at line ${declarations[key]?.line})" }
                    declarations[key] = statement
                    if (statement.operation == "EQU") {
                        require(statement.operand.isNotBlank()) { "EQU requires an expression" }
                        expressions.getOrPut(statement.operand) { AssemblyExpressionParser(statement.operand).parse() }
                        constants[key] = statement
                    }
                }
            } }
            var address: Int? = null
            statements.forEach { statement -> at(statement) {
                positions[statement.line] = address
                if (statement.operation == "EQU") return@at
                statement.label?.let { name ->
                    val current = requireNotNull(address) { "ORG is required before a label" }
                    require(current in 0..0xffff) { "Label is outside the 16-bit address space" }
                    labels[name.uppercase()] = current
                }
                val operation = statement.operation ?: return@at
                if (operation == "ORG") {
                    address = evaluate(statement.operand, address, true).also {
                        require(it in 0..0xffff) { "ORG requires a 16-bit address" }
                    }
                    positions[statement.line] = address
                    return@at
                }
                val current = requireNotNull(address) { "ORG is required before code or data" }
                val size = when (operation) {
                    "DB", "DW" -> {
                        val values = AssemblyText.operands(statement.operand)
                        operands[statement.line] = values
                        values.sumOf { value ->
                            if (operation == "DB" && value.startsWith('"')) AssemblyText.quoted(value, '"').size
                            else { expressions.getOrPut(value) { AssemblyExpressionParser(value).parse() }; if (operation == "DW") 2 else 1 }
                        }
                    }
                    "DS", "ALIGN" -> {
                        val values = AssemblyText.operands(statement.operand)
                        require(values.size in 1..2) { "$operation expects a size and optional fill byte" }
                        operands[statement.line] = values
                        val amount = evaluate(values.first(), current, true)
                        if (operation == "DS") {
                            require(amount in 0..0x10000) { "DS size must be 0..65536" }; amount
                        } else {
                            require(amount in 1..0x10000 && amount and (amount - 1) == 0) { "ALIGN boundary must be a power of two in 1..65536" }
                            (amount - current % amount) % amount
                        }
                    }
                    else -> definitions(operation).firstOrNull()?.length
                        ?: throw IllegalArgumentException("Unknown instruction or directive: $operation")
                }
                require(current.toLong() + size <= 0x10000) { "Output exceeds address 0xFFFF" }
                sizes[statement.line] = size
                address = current + size
            } }
            val symbols = declarations.map { (key, statement) -> at(statement) {
                if (key in constants) Sc61860AssemblySymbol(checkNotNull(statement.label), evaluate(key, positions[statement.line], false), statement.line, Sc61860AssemblySymbolKind.CONSTANT)
                else Sc61860AssemblySymbol(checkNotNull(statement.label), labels.getValue(key), statement.line)
            } }
            val segments = mutableListOf<AddressedMemorySegmentData>()
            val listing = mutableListOf<Sc61860AssemblyListingLine>()
            val bytes = mutableListOf<Byte>()
            var segmentStart: Int? = null
            var segmentLine = 0
            fun flush() {
                segmentStart?.let { if (bytes.isNotEmpty()) segments += AddressedMemorySegmentData(it, bytes.toByteArray(), segmentLine) }
                bytes.clear()
                segmentStart = null
            }
            statements.forEach { statement -> at(statement) {
                val operation = statement.operation ?: return@at
                if (operation == "EQU") return@at
                if (operation == "ORG") {
                    flush()
                    segmentStart = positions.getValue(statement.line)
                    segmentLine = statement.line
                    return@at
                }
                val current = checkNotNull(positions[statement.line])
                fun value(text: String): Int = evaluate(text, current, false)
                val emitted = when (operation) {
                    "DB", "DW" -> buildList<Byte> {
                        operands.getValue(statement.line).forEach { text ->
                            if (operation == "DB" && text.startsWith('"')) addAll(AssemblyText.quoted(text, '"').map(Int::toByte))
                            else {
                                val v = value(text)
                                require(v in 0..(if (operation == "DW") 0xffff else 0xff)) { "$operation value is out of range: $text" }
                                if (operation == "DW") add((v ushr 8).toByte())
                                add(v.toByte())
                            }
                        }
                    }.toByteArray()
                    "DS", "ALIGN" -> {
                        val fill = operands.getValue(statement.line).getOrNull(1)?.let(::value) ?: 0
                        require(fill in 0..255) { "$operation fill byte must be 0..255" }
                        ByteArray(sizes.getValue(statement.line)) { fill.toByte() }
                    }
                    else -> when (val encoded = encodeInstruction(statement, current, ::value)) {
                        is EncodeResult.Success -> encoded.bytes
                        is EncodeResult.Failure -> throw IllegalArgumentException(encoded.message)
                    }
                }
                check(emitted.size == sizes.getValue(statement.line))
                bytes.addAll(emitted.toList())
                if (emitted.isNotEmpty()) listing += Sc61860AssemblyListingLine(statement.line, current, emitted.map { it.toInt() and 0xff }, statement.sourceText)
            } }
            flush()
            return when (val result = AddressedMemoryImageFactory.create(segments)) {
                is AddressedMemoryImageCreateResult.Success -> Sc61860AssemblyResult.Success(result.image, symbols, listing)
                AddressedMemoryImageCreateResult.Empty -> Sc61860AssemblyResult.Failure(0, "Assembly produced no data")
                is AddressedMemoryImageCreateResult.InvalidSegment -> Sc61860AssemblyResult.Failure(0, "Generated segment is outside the 16-bit address space")
                is AddressedMemoryImageCreateResult.Overlap -> Sc61860AssemblyResult.Failure(segments[result.segmentIndex].sourceLine, "ORG regions overlap at 0x${result.address.hex(4)}")
            }
        }
    }

    private fun encodeInstruction(statement: Statement, address: Int, evaluate: (String) -> Int): EncodeResult {
        val candidates = definitions(checkNotNull(statement.operation))
        if (candidates.isEmpty()) return EncodeResult.Failure("Unknown instruction: ${statement.operation}")
        val operandText = statement.operand.trim()
        val definition = candidates.first()
        val value = if (definition.operandEncoding == Sc61860OperandEncoding.NONE) {
            if (operandText.isNotEmpty()) return EncodeResult.Failure("${definition.mnemonic} takes no operand")
            null
        } else {
            if (operandText.isEmpty()) return EncodeResult.Failure("${definition.mnemonic} requires an operand")
            evaluate(operandText)
        }
        return when (definition.operandEncoding) {
            Sc61860OperandEncoding.NONE -> EncodeResult.Success(byteArrayOf(definition.opcode.toByte()))
            Sc61860OperandEncoding.IMMEDIATE_8 -> encode8(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.ADDRESS_16 -> encode16(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.CASE1_DATA -> encode24(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.RELATIVE_FORWARD -> {
                val target = checkNotNull(value)
                if (target !in 0..0xffff) return EncodeResult.Failure("Relative target must be a 16-bit address")
                val offset = (target - (address + 1)) and 0xffff
                if (offset !in 0..0xff) EncodeResult.Failure("Forward target is out of range")
                else encode8(definition.opcode, offset)
            }
            Sc61860OperandEncoding.RELATIVE_BACKWARD -> {
                val target = checkNotNull(value)
                if (target !in 0..0xffff) return EncodeResult.Failure("Relative target must be a 16-bit address")
                val offset = ((address + 1) - target) and 0xffff
                if (offset !in 0..0xff) EncodeResult.Failure("Backward target is out of range")
                else encode8(definition.opcode, offset)
            }
            Sc61860OperandEncoding.PAGE_ADDRESS -> {
                val target = checkNotNull(value)
                if (target !in 0..0x1fff) EncodeResult.Failure("CAL target must be 0x0000..0x1FFF")
                else EncodeResult.Success(byteArrayOf((0xe0 or (target ushr 8)).toByte(), target.toByte()))
            }
            Sc61860OperandEncoding.EMBEDDED_6 -> {
                val embedded = checkNotNull(value)
                if (embedded !in 0..0x3f) EncodeResult.Failure("LP operand must be 0..0x3F")
                else EncodeResult.Success(byteArrayOf((0x80 or embedded).toByte()))
            }
        }
    }

    private fun encode8(opcode: Int, value: Int): EncodeResult =
        if (value in 0..0xff) EncodeResult.Success(byteArrayOf(opcode.toByte(), value.toByte()))
        else EncodeResult.Failure("8-bit operand must be 0..255")

    private fun encode16(opcode: Int, value: Int): EncodeResult =
        if (value in 0..0xffff) {
            EncodeResult.Success(byteArrayOf(opcode.toByte(), (value ushr 8).toByte(), value.toByte()))
        } else EncodeResult.Failure("16-bit operand must be 0..65535")

    private fun encode24(opcode: Int, value: Int): EncodeResult =
        if (value in 0..0xffffff) {
            EncodeResult.Success(
                byteArrayOf(opcode.toByte(), (value ushr 16).toByte(), (value ushr 8).toByte(), value.toByte()),
            )
        } else EncodeResult.Failure("24-bit operand must be 0..0xFFFFFF")

    private fun definitions(mnemonic: String): List<Sc61860InstructionDefinition> =
        Sc61860InstructionSet.allDefined().filter {
            it.encodable && it.mnemonic.equals(mnemonic, ignoreCase = true)
        }

    private fun parseStatements(source: String): List<Statement> = source.removePrefix("\uFEFF")
        .replace("\r\n", "\n").replace('\r', '\n').split('\n').mapIndexed { index, original ->
            var content = AssemblyText.withoutComment(original).trim()
            var label: String? = null
            val colon = content.indexOf(':')
            if (colon >= 0 && LABEL.matches(content.substring(0, colon).trim())) {
                label = content.substring(0, colon).trim()
                content = content.substring(colon + 1).trim()
            }
            if (label == null) {
                val equ = Regex("^([A-Za-z_][A-Za-z0-9_]*)\\s+EQU(?:\\s+|$)(.*)$", RegexOption.IGNORE_CASE).matchEntire(content)
                if (equ != null) {
                    label = equ.groupValues[1]
                    content = "EQU " + equ.groupValues[2]
                }
            }
            val operation = content.takeWhile { !it.isWhitespace() }.ifEmpty { null }
            Statement(index + 1, label, operation?.uppercase(), operation?.let { content.drop(it.length).trim() }.orEmpty(), original)
        }

    private data class Statement(val line: Int, val label: String?, val operation: String?, val operand: String, val sourceText: String)
    private sealed interface EncodeResult {
        data class Success(val bytes: ByteArray) : EncodeResult
        data class Failure(val message: String) : EncodeResult
    }
    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
    private val LABEL = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
