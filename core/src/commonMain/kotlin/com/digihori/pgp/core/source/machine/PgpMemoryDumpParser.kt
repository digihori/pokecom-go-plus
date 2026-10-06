package com.digihori.pgp.core.source.machine

/** Parses the human-readable PGP addressed memory dump format (`.dmp`). */
public object PgpMemoryDumpParser {
    public fun parse(source: String): PgpMemoryDumpParseResult {
        val normalized = source
            .removePrefix(UTF8_BOM.toString())
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val segments = mutableListOf<AddressedMemorySegment>()
        val ownerLines = IntArray(ADDRESS_SPACE_SIZE)

        normalized.split('\n').forEachIndexed { index, originalLine ->
            val lineNumber = index + 1
            val content = originalLine
                .substringBefore(';')
                .substringBefore('#')
                .trim()
            if (content.isEmpty()) return@forEachIndexed

            val addressText = content.takeWhile(::isHexadecimalDigit)
            if (addressText.length != ADDRESS_DIGITS) {
                return failure(lineNumber, 1, "Address must contain exactly four hexadecimal digits")
            }
            val address = addressText.toInt(16)
            val remainder = content.substring(addressText.length)
            if (remainder.isEmpty()) {
                return failure(lineNumber, addressText.length + 1, "Data is missing")
            }

            if (remainder.first() != ':' && !remainder.first().isWhitespace()) {
                return failure(
                    lineNumber,
                    addressText.length + 1,
                    "Address and data must be separated by whitespace or ':'",
                )
            }
            val afterWhitespace = remainder.trimStart()
            val dataAndChecksum = if (afterWhitespace.startsWith(':')) {
                afterWhitespace.drop(1).trim()
            } else {
                afterWhitespace.trim()
            }
            val checksumSeparator = dataAndChecksum.indexOf(':')
            val dataText = if (checksumSeparator >= 0) {
                val checksum = dataAndChecksum.substring(checksumSeparator + 1).trim()
                if (checksum.length != 2 || !checksum.all(::isHexadecimalDigit)) {
                    return failure(
                        lineNumber,
                        content.lastIndexOf(':') + 2,
                        "Optional checksum must contain exactly two hexadecimal digits",
                    )
                }
                dataAndChecksum.substring(0, checksumSeparator).trim()
            } else {
                dataAndChecksum
            }
            if (dataText.isEmpty()) {
                return failure(lineNumber, originalLine.length + 1, "Data is missing")
            }

            val compactData = buildString {
                dataText.forEachIndexed { dataIndex, character ->
                    when {
                        isHexadecimalDigit(character) -> append(character)
                        character.isWhitespace() -> Unit
                        else -> return failure(
                            lineNumber,
                            content.indexOf(dataText) + dataIndex + 1,
                            "Data contains a non-hexadecimal character",
                        )
                    }
                }
            }
            if (compactData.isEmpty()) {
                return failure(lineNumber, originalLine.length + 1, "Data is missing")
            }
            if (compactData.length % 2 != 0) {
                return failure(lineNumber, originalLine.length, "Data must contain complete two-digit bytes")
            }

            val byteCount = compactData.length / 2
            val endAddress = address + byteCount - 1
            if (endAddress >= ADDRESS_SPACE_SIZE) {
                return PgpMemoryDumpParseResult.Failure(
                    PgpMemoryDumpError.AddressOverflow(lineNumber, address, byteCount),
                )
            }

            for (target in address..endAddress) {
                val previousLine = ownerLines[target]
                if (previousLine != 0) {
                    return PgpMemoryDumpParseResult.Failure(
                        PgpMemoryDumpError.Overlap(lineNumber, previousLine, target),
                    )
                }
            }

            val bytes = ByteArray(byteCount) { byteIndex ->
                compactData.substring(byteIndex * 2, byteIndex * 2 + 2).toInt(16).toByte()
            }
            for (target in address..endAddress) ownerLines[target] = lineNumber
            segments += AddressedMemorySegment(address, bytes, lineNumber)
        }

        if (segments.isEmpty()) {
            return PgpMemoryDumpParseResult.Failure(PgpMemoryDumpError.Empty)
        }
        return PgpMemoryDumpParseResult.Success(AddressedMemoryImage(segments.sortedBy { it.startAddress }))
    }

    private fun failure(line: Int, column: Int, message: String): PgpMemoryDumpParseResult.Failure =
        PgpMemoryDumpParseResult.Failure(PgpMemoryDumpError.Syntax(line, column, message))

    private fun isHexadecimalDigit(character: Char): Boolean =
        character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'

    private const val ADDRESS_DIGITS: Int = 4
    private const val ADDRESS_SPACE_SIZE: Int = 0x10000
    private const val UTF8_BOM: Char = '\uFEFF'
}

public class AddressedMemoryImage internal constructor(segments: List<AddressedMemorySegment>) {
    public val segments: List<AddressedMemorySegment> = segments.toList()
    public val byteCount: Int = segments.sumOf(AddressedMemorySegment::size)
}

public class AddressedMemorySegmentData(
    public val startAddress: Int,
    bytes: ByteArray,
    public val sourceLine: Int = 0,
) {
    private val content: ByteArray = bytes.copyOf()
    public val size: Int get() = content.size
    public fun copyBytes(): ByteArray = content.copyOf()
}

public sealed interface AddressedMemoryImageCreateResult {
    public data class Success(public val image: AddressedMemoryImage) : AddressedMemoryImageCreateResult
    public data object Empty : AddressedMemoryImageCreateResult
    public data class InvalidSegment(
        public val segmentIndex: Int,
        public val startAddress: Int,
        public val size: Int,
    ) : AddressedMemoryImageCreateResult
    public data class Overlap(
        public val segmentIndex: Int,
        public val previousSegmentIndex: Int,
        public val address: Int,
    ) : AddressedMemoryImageCreateResult
}

/** Creates an addressed image for generated or raw-binary data. */
public object AddressedMemoryImageFactory {
    public fun create(segments: List<AddressedMemorySegmentData>): AddressedMemoryImageCreateResult {
        if (segments.isEmpty()) return AddressedMemoryImageCreateResult.Empty
        val ownerSegments = IntArray(ADDRESS_SPACE_SIZE) { -1 }
        val result = mutableListOf<AddressedMemorySegment>()
        segments.forEachIndexed { index, segment ->
            val endAddress = segment.startAddress.toLong() + segment.size - 1L
            if (
                segment.size == 0 ||
                segment.startAddress !in 0 until ADDRESS_SPACE_SIZE ||
                endAddress !in 0 until ADDRESS_SPACE_SIZE.toLong()
            ) {
                return AddressedMemoryImageCreateResult.InvalidSegment(
                    index,
                    segment.startAddress,
                segment.size,
                )
            }
            for (address in segment.startAddress..endAddress.toInt()) {
                val previous = ownerSegments[address]
                if (previous >= 0) {
                    return AddressedMemoryImageCreateResult.Overlap(index, previous, address)
                }
                ownerSegments[address] = index
            }
            result += AddressedMemorySegment(segment.startAddress, segment.copyBytes(), segment.sourceLine)
        }
        return AddressedMemoryImageCreateResult.Success(AddressedMemoryImage(result.sortedBy { it.startAddress }))
    }

    private const val ADDRESS_SPACE_SIZE: Int = 0x10000
}

public class AddressedMemorySegment internal constructor(
    public val startAddress: Int,
    bytes: ByteArray,
    public val sourceLine: Int,
) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int get() = content.size
    public val endAddress: Int get() = startAddress + size - 1

    public fun copyBytes(): ByteArray = content.copyOf()
}

public sealed interface PgpMemoryDumpParseResult {
    public data class Success(public val image: AddressedMemoryImage) : PgpMemoryDumpParseResult
    public data class Failure(public val error: PgpMemoryDumpError) : PgpMemoryDumpParseResult
}

public sealed interface PgpMemoryDumpError {
    public data object Empty : PgpMemoryDumpError

    public data class Syntax(
        public val line: Int,
        public val column: Int,
        public val message: String,
    ) : PgpMemoryDumpError

    public data class AddressOverflow(
        public val line: Int,
        public val startAddress: Int,
        public val byteCount: Int,
    ) : PgpMemoryDumpError

    public data class Overlap(
        public val line: Int,
        public val previousLine: Int,
        public val address: Int,
    ) : PgpMemoryDumpError
}
