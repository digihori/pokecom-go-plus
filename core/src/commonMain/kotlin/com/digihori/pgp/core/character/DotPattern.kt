package com.digihori.pgp.core.character

/** Immutable rectangular monochrome dot pattern, independent of UI and machine memory. */
public class DotPattern(
    public val width: Int,
    public val height: Int,
    dots: BooleanArray = BooleanArray(width * height),
) {
    private val content: BooleanArray = dots.copyOf()

    init {
        require(width > 0) { "Dot pattern width must be positive" }
        require(height > 0) { "Dot pattern height must be positive" }
        require(content.size == width * height) { "Dot array size must match width and height" }
    }

    public fun isSet(column: Int, row: Int): Boolean {
        require(column in 0 until width) { "Dot column is out of range" }
        require(row in 0 until height) { "Dot row is out of range" }
        return content[row * width + column]
    }

    public fun withDot(column: Int, row: Int, set: Boolean): DotPattern {
        require(column in 0 until width) { "Dot column is out of range" }
        require(row in 0 until height) { "Dot row is out of range" }
        val index = row * width + column
        if (content[index] == set) return this
        return DotPattern(width, height, content.copyOf().also { it[index] = set })
    }

    public fun clear(): DotPattern = DotPattern(width, height)

    public fun inverted(): DotPattern = DotPattern(width, height, BooleanArray(content.size) { !content[it] })

    public fun copyDots(): BooleanArray = content.copyOf()

    override fun equals(other: Any?): Boolean =
        other is DotPattern && width == other.width && height == other.height && content.contentEquals(other.content)

    override fun hashCode(): Int = 31 * (31 * width + height) + content.contentHashCode()
}

public enum class DotPackingAxis {
    COLUMNS,
    ROWS,
}

public enum class DotBitOrder {
    LEAST_SIGNIFICANT_FIRST,
    MOST_SIGNIFICANT_FIRST,
}

public data class DotPatternFormat(
    public val id: String,
    public val displayName: String,
    public val width: Int,
    public val height: Int,
    public val packingAxis: DotPackingAxis,
    public val bitOrder: DotBitOrder,
) {
    init {
        require(id.isNotBlank()) { "Format id must not be blank" }
        require(displayName.isNotBlank()) { "Format name must not be blank" }
        require(width > 0) { "Format width must be positive" }
        require(height > 0) { "Format height must be positive" }
    }

    public val byteCount: Int
        get() = when (packingAxis) {
            DotPackingAxis.COLUMNS -> width * bytesForBits(height)
            DotPackingAxis.ROWS -> height * bytesForBits(width)
        }

    private fun bytesForBits(bits: Int): Int = (bits + 7) / 8
}

public object CharacterEditorFormats {
    /** Initial generic profile; this does not claim a verified machine-specific UDC format. */
    public val FIVE_BY_SEVEN_COLUMN_LSB_TOP: DotPatternFormat = DotPatternFormat(
        id = "5x7-column-lsb-top",
        displayName = "5×7 column bytes (LSB top)",
        width = 5,
        height = 7,
        packingAxis = DotPackingAxis.COLUMNS,
        bitOrder = DotBitOrder.LEAST_SIGNIFICANT_FIRST,
    )
}
