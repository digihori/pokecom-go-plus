package com.digihori.pgp.desktop.character

import com.digihori.pgp.core.character.DotByteTextResult
import com.digihori.pgp.core.character.DotPattern
import com.digihori.pgp.core.character.DotPatternCodec
import com.digihori.pgp.core.character.DotPatternFormat
import com.digihori.pgp.core.character.DotPatternTextCodec

internal data class DesktopCharacterEditorModel(
    val format: DotPatternFormat,
    val pattern: DotPattern = DotPattern(format.width, format.height),
    val byteText: String = DotPatternTextCodec.hexBytes(DotPatternCodec.encode(pattern, format)),
    val inputError: String? = null,
) {
    fun withDot(column: Int, row: Int, set: Boolean): DesktopCharacterEditorModel =
        withPattern(pattern.withDot(column, row, set))

    fun clear(): DesktopCharacterEditorModel = withPattern(pattern.clear())

    fun invert(): DesktopCharacterEditorModel = withPattern(pattern.inverted())

    fun withByteText(value: String): DesktopCharacterEditorModel = copy(byteText = value, inputError = null)

    fun applyByteText(): DesktopCharacterEditorModel = when (
        val parsed = DotPatternTextCodec.parse(byteText, format.byteCount)
    ) {
        is DotByteTextResult.Failure -> copy(inputError = parsed.message)
        is DotByteTextResult.Success -> runCatching { DotPatternCodec.decode(parsed.bytes, format) }.fold(
            onSuccess = ::withPattern,
            onFailure = { copy(inputError = it.message ?: "The byte values do not fit this format.") },
        )
    }

    val hexOutput: String
        get() = DotPatternTextCodec.hexBytes(encodedBytes())

    val basicOutput: String
        get() = DotPatternTextCodec.basicData(encodedBytes())

    val assemblerOutput: String
        get() = DotPatternTextCodec.assemblerDb(encodedBytes())

    private fun withPattern(value: DotPattern): DesktopCharacterEditorModel = copy(
        pattern = value,
        byteText = DotPatternTextCodec.hexBytes(DotPatternCodec.encode(value, format)),
        inputError = null,
    )

    private fun encodedBytes(): ByteArray = DotPatternCodec.encode(pattern, format)
}

internal object CharacterGridGeometry {
    fun cellAt(x: Float, y: Float, width: Float, height: Float, columns: Int, rows: Int): Pair<Int, Int>? {
        if (width <= 0f || height <= 0f || x < 0f || y < 0f || x >= width || y >= height) return null
        if (columns <= 0 || rows <= 0) return null
        val column = (x / width * columns).toInt()
        val row = (y / height * rows).toInt()
        return column to row
    }
}
