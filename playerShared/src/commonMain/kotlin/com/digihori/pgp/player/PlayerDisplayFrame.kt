package com.digihori.pgp.player

import com.digihori.pgp.core.api.DisplaySnapshot
import com.digihori.pgp.core.api.DisplaySymbol

/** Immutable LCD data prepared for a platform renderer. */
public class PlayerDisplayFrame internal constructor(snapshot: DisplaySnapshot) {
    public val characterColumns: Int = snapshot.characterColumns
    public val characterWidth: Int = snapshot.characterWidth
    public val dotRows: Int = snapshot.dotRows
    public val characterRows: Int = snapshot.characterRows
    public val interCharacterColumnGap: Int = snapshot.interCharacterColumnGap
    public val interCharacterRowGap: Int = snapshot.interCharacterRowGap
    public val symbols: List<DisplaySymbol> = snapshot.symbols.toList()
    public val enabled: Boolean = snapshot.enabled
    public val revision: Long = snapshot.revision
    private val dots: ByteArray = snapshot.copyDots()

    public val dotColumns: Int get() = characterColumns * characterWidth
    public val logicalColumns: Int
        get() = dotColumns + (characterColumns - 1) * interCharacterColumnGap
    public val logicalRows: Int
        get() = dotRows + (characterRows - 1) * interCharacterRowGap
    public val logicalAspectRatio: Float get() = logicalColumns.toFloat() / logicalRows

    public fun isDotOn(column: Int, row: Int): Boolean {
        require(column in 0 until dotColumns) { "Display column is out of range" }
        require(row in 0 until dotRows) { "Display row is out of range" }
        return enabled && dots[row * dotColumns + column].toInt() != 0
    }

    public fun logicalX(column: Int): Int {
        require(column in 0 until dotColumns) { "Display column is out of range" }
        return column + (column / characterWidth) * interCharacterColumnGap
    }

    public fun logicalY(row: Int): Int {
        require(row in 0 until dotRows) { "Display row is out of range" }
        val characterHeight = dotRows / characterRows
        return row + (row / characterHeight) * interCharacterRowGap
    }
}
