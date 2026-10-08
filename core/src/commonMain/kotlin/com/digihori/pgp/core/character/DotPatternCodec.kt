package com.digihori.pgp.core.character

public object DotPatternCodec {
    public fun encode(pattern: DotPattern, format: DotPatternFormat): ByteArray {
        require(pattern.width == format.width && pattern.height == format.height) {
            "Dot pattern dimensions do not match the format"
        }
        val bytes = ByteArray(format.byteCount)
        when (format.packingAxis) {
            DotPackingAxis.COLUMNS -> {
                val bytesPerColumn = bytesForBits(format.height)
                for (column in 0 until format.width) {
                    for (row in 0 until format.height) {
                        if (pattern.isSet(column, row)) {
                            setBit(bytes, column * bytesPerColumn, row, format.bitOrder)
                        }
                    }
                }
            }
            DotPackingAxis.ROWS -> {
                val bytesPerRow = bytesForBits(format.width)
                for (row in 0 until format.height) {
                    for (column in 0 until format.width) {
                        if (pattern.isSet(column, row)) {
                            setBit(bytes, row * bytesPerRow, column, format.bitOrder)
                        }
                    }
                }
            }
        }
        return bytes
    }

    public fun decode(bytes: ByteArray, format: DotPatternFormat): DotPattern {
        require(bytes.size == format.byteCount) {
            "Expected ${format.byteCount} bytes but received ${bytes.size}"
        }
        requireUnusedBitsAreClear(bytes, format)
        val dots = BooleanArray(format.width * format.height)
        when (format.packingAxis) {
            DotPackingAxis.COLUMNS -> {
                val bytesPerColumn = bytesForBits(format.height)
                for (column in 0 until format.width) {
                    for (row in 0 until format.height) {
                        dots[row * format.width + column] = bitIsSet(bytes, column * bytesPerColumn, row, format.bitOrder)
                    }
                }
            }
            DotPackingAxis.ROWS -> {
                val bytesPerRow = bytesForBits(format.width)
                for (row in 0 until format.height) {
                    for (column in 0 until format.width) {
                        dots[row * format.width + column] = bitIsSet(bytes, row * bytesPerRow, column, format.bitOrder)
                    }
                }
            }
        }
        return DotPattern(format.width, format.height, dots)
    }

    private fun setBit(bytes: ByteArray, base: Int, coordinate: Int, order: DotBitOrder) {
        val byteOffset = coordinate / 8
        val bitWithinByte = coordinate % 8
        val bit = when (order) {
            DotBitOrder.LEAST_SIGNIFICANT_FIRST -> bitWithinByte
            DotBitOrder.MOST_SIGNIFICANT_FIRST -> 7 - bitWithinByte
        }
        bytes[base + byteOffset] = (bytes[base + byteOffset].toInt() or (1 shl bit)).toByte()
    }

    private fun bitIsSet(bytes: ByteArray, base: Int, coordinate: Int, order: DotBitOrder): Boolean {
        val byteOffset = coordinate / 8
        val bitWithinByte = coordinate % 8
        val bit = when (order) {
            DotBitOrder.LEAST_SIGNIFICANT_FIRST -> bitWithinByte
            DotBitOrder.MOST_SIGNIFICANT_FIRST -> 7 - bitWithinByte
        }
        return bytes[base + byteOffset].toInt() and (1 shl bit) != 0
    }

    private fun bytesForBits(bits: Int): Int = (bits + 7) / 8

    private fun requireUnusedBitsAreClear(bytes: ByteArray, format: DotPatternFormat) {
        val bitsPerUnit = when (format.packingAxis) {
            DotPackingAxis.COLUMNS -> format.height
            DotPackingAxis.ROWS -> format.width
        }
        val usedBitsInLastByte = bitsPerUnit % 8
        if (usedBitsInLastByte == 0) return
        val bytesPerUnit = bytesForBits(bitsPerUnit)
        val unitCount = when (format.packingAxis) {
            DotPackingAxis.COLUMNS -> format.width
            DotPackingAxis.ROWS -> format.height
        }
        val usedMask = when (format.bitOrder) {
            DotBitOrder.LEAST_SIGNIFICANT_FIRST -> (1 shl usedBitsInLastByte) - 1
            DotBitOrder.MOST_SIGNIFICANT_FIRST -> 0xff shl (8 - usedBitsInLastByte) and 0xff
        }
        for (unit in 0 until unitCount) {
            val value = bytes[unit * bytesPerUnit + bytesPerUnit - 1].toInt() and 0xff
            require(value and usedMask.inv() and 0xff == 0) {
                "Byte ${unit * bytesPerUnit + bytesPerUnit} contains bits outside the ${format.width}×${format.height} pattern"
            }
        }
    }
}

public sealed interface DotByteTextResult {
    public data class Success(public val bytes: ByteArray) : DotByteTextResult
    public data class Failure(public val message: String) : DotByteTextResult
}

public object DotPatternTextCodec {
    public fun parse(text: String, expectedByteCount: Int): DotByteTextResult {
        require(expectedByteCount > 0) { "Expected byte count must be positive" }
        val tokens = text.trim().split(Regex("[,\\s]+"))
        if (tokens.size == 1 && tokens.single().isEmpty()) return DotByteTextResult.Failure("Enter $expectedByteCount byte values.")
        if (tokens.size != expectedByteCount) {
            return DotByteTextResult.Failure("Expected $expectedByteCount byte values but found ${tokens.size}.")
        }
        val bytes = ByteArray(tokens.size)
        tokens.forEachIndexed { index, token ->
            val value = parseByte(token)
                ?: return DotByteTextResult.Failure("Invalid byte value at position ${index + 1}: $token")
            bytes[index] = value.toByte()
        }
        return DotByteTextResult.Success(bytes)
    }

    public fun hexBytes(bytes: ByteArray): String = bytes.joinToString(", ") { "0x${it.unsignedHex()}" }

    public fun basicData(bytes: ByteArray): String = bytes.joinToString(prefix = "DATA ", separator = ", ") {
        (it.toInt() and 0xff).toString()
    }

    public fun assemblerDb(bytes: ByteArray): String = bytes.joinToString(prefix = "DB ", separator = ", ") {
        "0x${it.unsignedHex()}"
    }

    private fun parseByte(token: String): Int? {
        val (digits, radix) = when {
            token.startsWith("0x", ignoreCase = true) -> token.drop(2) to 16
            token.startsWith("&") -> token.drop(1) to 16
            else -> token to 10
        }
        if (digits.isEmpty()) return null
        return digits.toIntOrNull(radix)?.takeIf { it in 0..0xff }
    }

    private fun Byte.unsignedHex(): String = (toInt() and 0xff).toString(16).uppercase().padStart(2, '0')
}
