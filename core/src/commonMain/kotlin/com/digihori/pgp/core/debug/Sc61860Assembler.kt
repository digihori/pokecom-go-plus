package com.digihori.pgp.core.debug

import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult
import com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory
import com.digihori.pgp.core.source.machine.AddressedMemorySegmentData

public sealed interface Sc61860AssemblyResult {
    public data class Success(public val image: AddressedMemoryImage) : Sc61860AssemblyResult
    public data class Failure(public val line: Int, public val message: String) : Sc61860AssemblyResult
}

public object Sc61860Assembler {
    public fun assemble(source: String): Sc61860AssemblyResult {
        val statements = parseStatements(source)
        val labels = mutableMapOf<String, Int>()
        var address: Int? = null

        statements.forEach { statement ->
            statement.label?.let { label ->
                val current = address ?: return failure(statement, "ORG is required before a label")
                if (labels.put(label.uppercase(), current) != null) return failure(statement, "Duplicate label: $label")
            }
            val operation = statement.operation ?: return@forEach
            if (operation.equals("ORG", ignoreCase = true)) {
                address = parseLiteral(statement.operand)
                    ?.takeIf { it in 0..0xffff }
                    ?: return failure(statement, "ORG requires a 16-bit numeric address")
                return@forEach
            }
            val current = address ?: return failure(statement, "ORG is required before code or data")
            val size = statementSize(statement) ?: return failure(statement, "Unknown instruction or directive: $operation")
            if (current + size > 0x10000) return failure(statement, "Output exceeds address 0xFFFF")
            address = current + size
        }

        val segments = mutableListOf<AddressedMemorySegmentData>()
        var segmentStart: Int? = null
        val segmentBytes = mutableListOf<Byte>()
        fun flush() {
            val start = segmentStart ?: return
            if (segmentBytes.isNotEmpty()) {
                segments += AddressedMemorySegmentData(start, segmentBytes.toByteArray())
            }
            segmentStart = null
            segmentBytes.clear()
        }

        address = null
        statements.forEach { statement ->
            val operation = statement.operation ?: return@forEach
            if (operation.equals("ORG", ignoreCase = true)) {
                flush()
                address = checkNotNull(parseLiteral(statement.operand))
                segmentStart = address
                return@forEach
            }
            val current = checkNotNull(address)
            val encoded = if (operation.equals("DB", ignoreCase = true)) {
                encodeData(statement, labels)
            } else {
                encodeInstruction(statement, current, labels)
            }
            when (encoded) {
                is EncodeResult.Failure -> return failure(statement, encoded.message)
                is EncodeResult.Success -> {
                    segmentBytes += encoded.bytes.toList()
                    address = current + encoded.bytes.size
                }
            }
        }
        flush()
        return when (val created = AddressedMemoryImageFactory.create(segments)) {
            is AddressedMemoryImageCreateResult.Success -> Sc61860AssemblyResult.Success(created.image)
            AddressedMemoryImageCreateResult.Empty -> Sc61860AssemblyResult.Failure(0, "Assembly produced no data")
            is AddressedMemoryImageCreateResult.InvalidSegment ->
                Sc61860AssemblyResult.Failure(0, "Generated segment is outside the 16-bit address space")
            is AddressedMemoryImageCreateResult.Overlap ->
                Sc61860AssemblyResult.Failure(0, "ORG regions overlap at 0x${created.address.hex(4)}")
        }
    }

    private fun statementSize(statement: Statement): Int? {
        if (statement.operation.equals("DB", ignoreCase = true)) {
            return statement.operand.split(',').count { it.trim().isNotEmpty() }.takeIf { it > 0 }
        }
        return definitions(checkNotNull(statement.operation)).firstOrNull()?.length
    }

    private fun encodeData(statement: Statement, labels: Map<String, Int>): EncodeResult {
        val operands = statement.operand.split(',').map(String::trim).filter(String::isNotEmpty)
        if (operands.isEmpty()) return EncodeResult.Failure("DB requires at least one byte")
        val bytes = ByteArray(operands.size)
        operands.forEachIndexed { index, expression ->
            val value = resolve(expression, labels)
                ?: return EncodeResult.Failure("Unknown value or label: $expression")
            if (value !in 0..0xff) return EncodeResult.Failure("DB value must be 0..255: $expression")
            bytes[index] = value.toByte()
        }
        return EncodeResult.Success(bytes)
    }

    private fun encodeInstruction(statement: Statement, address: Int, labels: Map<String, Int>): EncodeResult {
        val candidates = definitions(checkNotNull(statement.operation))
        if (candidates.isEmpty()) return EncodeResult.Failure("Unknown instruction: ${statement.operation}")
        val operandText = statement.operand.trim()
        val definition = candidates.first()
        val value = if (definition.operandEncoding == Sc61860OperandEncoding.NONE) {
            if (operandText.isNotEmpty()) return EncodeResult.Failure("${definition.mnemonic} takes no operand")
            null
        } else {
            if (operandText.isEmpty()) return EncodeResult.Failure("${definition.mnemonic} requires an operand")
            resolve(operandText, labels) ?: return EncodeResult.Failure("Unknown value or label: $operandText")
        }
        return when (definition.operandEncoding) {
            Sc61860OperandEncoding.NONE -> EncodeResult.Success(byteArrayOf(definition.opcode.toByte()))
            Sc61860OperandEncoding.IMMEDIATE_8 -> encode8(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.ADDRESS_16 -> encode16(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.CASE1_DATA -> encode24(definition.opcode, checkNotNull(value))
            Sc61860OperandEncoding.RELATIVE_FORWARD -> {
                val offset = checkNotNull(value) - (address + 1)
                if (offset !in 0..0xff) EncodeResult.Failure("Forward target is out of range")
                else encode8(definition.opcode, offset)
            }
            Sc61860OperandEncoding.RELATIVE_BACKWARD -> {
                val offset = (address + 1) - checkNotNull(value)
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

    private fun resolve(expression: String, labels: Map<String, Int>): Int? =
        parseLiteral(expression) ?: labels[expression.trim().uppercase()]

    private fun parseLiteral(value: String): Int? {
        val text = value.trim()
        return when {
            text.startsWith("0x", true) -> text.drop(2).toIntOrNull(16)
            text.startsWith('&') || text.startsWith('$') -> text.drop(1).toIntOrNull(16)
            else -> text.toIntOrNull()
        }
    }

    private fun parseStatements(source: String): List<Statement> = source
        .removePrefix("\uFEFF")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .split('\n')
        .mapIndexed { index, original ->
            var content = original.substringBefore(';').trim()
            var label: String? = null
            val colon = content.indexOf(':')
            if (colon >= 0) {
                val candidate = content.substring(0, colon).trim()
                if (LABEL.matches(candidate)) {
                    label = candidate
                    content = content.substring(colon + 1).trim()
                }
            }
            val operation = content.takeWhile { !it.isWhitespace() }.ifEmpty { null }
            val operand = operation?.let { content.drop(it.length).trim() }.orEmpty()
            Statement(index + 1, label, operation, operand)
        }

    private fun failure(statement: Statement, message: String): Sc61860AssemblyResult.Failure =
        Sc61860AssemblyResult.Failure(statement.line, message)

    private data class Statement(val line: Int, val label: String?, val operation: String?, val operand: String)
    private sealed interface EncodeResult {
        data class Success(val bytes: ByteArray) : EncodeResult
        data class Failure(val message: String) : EncodeResult
    }

    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
    private val LABEL = Regex("[A-Za-z_][A-Za-z0-9_]*")
}
