package com.digihori.pgp.desktop.display

internal object CharacterCellGeometry {
    fun visualColumnCount(characterColumns: Int, characterWidth: Int, gapColumns: Int = 1): Int {
        require(characterColumns > 0) { "characterColumns must be positive" }
        require(characterWidth > 0) { "characterWidth must be positive" }
        require(gapColumns >= 0) { "gapColumns must not be negative" }
        return characterColumns * (characterWidth + gapColumns) - gapColumns
    }

    fun visualColumn(dotColumn: Int, characterWidth: Int, gapColumns: Int = 1): Int {
        require(dotColumn >= 0) { "dotColumn must not be negative" }
        require(characterWidth > 0) { "characterWidth must be positive" }
        require(gapColumns >= 0) { "gapColumns must not be negative" }
        return dotColumn + dotColumn / characterWidth * gapColumns
    }

    fun visualRowCount(characterRows: Int, characterHeight: Int, gapRows: Int = 1): Int {
        require(characterRows > 0) { "characterRows must be positive" }
        require(characterHeight > 0) { "characterHeight must be positive" }
        require(gapRows >= 0) { "gapRows must not be negative" }
        return characterRows * (characterHeight + gapRows) - gapRows
    }

    fun visualRow(dotRow: Int, characterHeight: Int, gapRows: Int = 1): Int {
        require(dotRow >= 0) { "dotRow must not be negative" }
        require(characterHeight > 0) { "characterHeight must be positive" }
        require(gapRows >= 0) { "gapRows must not be negative" }
        return dotRow + dotRow / characterHeight * gapRows
    }
}
