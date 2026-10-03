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

    private const val CHARACTER_GAP_COLUMNS: Int = 1
}
