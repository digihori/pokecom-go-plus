package com.digihori.pgp.desktop.display

internal object CharacterCellGeometry {
    fun visualColumnCount(characterColumns: Int, characterWidth: Int): Int {
        require(characterColumns > 0) { "characterColumns must be positive" }
        require(characterWidth > 0) { "characterWidth must be positive" }
        return characterColumns * (characterWidth + CHARACTER_GAP_COLUMNS) - CHARACTER_GAP_COLUMNS
    }

    fun visualColumn(dotColumn: Int, characterWidth: Int): Int {
        require(dotColumn >= 0) { "dotColumn must not be negative" }
        require(characterWidth > 0) { "characterWidth must be positive" }
        return dotColumn + dotColumn / characterWidth * CHARACTER_GAP_COLUMNS
    }

    fun visualRowCount(characterRows: Int, characterHeight: Int): Int {
        require(characterRows > 0) { "characterRows must be positive" }
        require(characterHeight > 0) { "characterHeight must be positive" }
        return characterRows * (characterHeight + CHARACTER_GAP_ROWS) - CHARACTER_GAP_ROWS
    }

    fun visualRow(dotRow: Int, characterHeight: Int): Int {
        require(dotRow >= 0) { "dotRow must not be negative" }
        require(characterHeight > 0) { "characterHeight must be positive" }
        return dotRow + dotRow / characterHeight * CHARACTER_GAP_ROWS
    }

    private const val CHARACTER_GAP_COLUMNS: Int = 1
    private const val CHARACTER_GAP_ROWS: Int = 1
}
