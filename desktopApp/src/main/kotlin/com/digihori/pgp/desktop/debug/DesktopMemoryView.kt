package com.digihori.pgp.desktop.debug

internal data class DesktopMemoryViewLine(
    val address: Int,
    val bytes: List<Int>,
    val hexText: String,
    val asciiText: String,
)

internal object DesktopMemoryViewModel {
    const val BYTES_PER_LINE: Int = 16

    fun build(
        startAddress: Int,
        lineCount: Int,
        readByte: (Int) -> Int,
    ): List<DesktopMemoryViewLine> {
        require(startAddress in 0..0xffff)
        require(lineCount >= 0)
        return List(lineCount) { lineIndex ->
            val address = (startAddress + lineIndex * BYTES_PER_LINE) and 0xffff
            val bytes = List(BYTES_PER_LINE) { offset ->
                readByte((address + offset) and 0xffff) and 0xff
            }
            DesktopMemoryViewLine(
                address = address,
                bytes = bytes,
                hexText = bytes.joinToString(" ") { it.hex(2) },
                asciiText = bytes.map { value ->
                    if (value in 0x20..0x7e) value.toChar() else '.'
                }.joinToString(""),
            )
        }
    }

    private fun Int.hex(width: Int): String = toString(16).uppercase().padStart(width, '0')
}
