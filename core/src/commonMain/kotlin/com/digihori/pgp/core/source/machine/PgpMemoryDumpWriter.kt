package com.digihori.pgp.core.source.machine

public object PgpMemoryDumpWriter {
    public fun write(startAddress: Int, bytes: ByteArray, bytesPerLine: Int = 16): String {
        require(startAddress in 0..0xffff) { "Start address must be a 16-bit value" }
        require(bytes.isNotEmpty()) { "Memory dump must contain at least one byte" }
        require(bytes.size <= 0x10000 - startAddress) { "Memory range exceeds the 16-bit address space" }
        require(bytesPerLine > 0) { "Bytes per line must be greater than zero" }
        return buildString {
            bytes.asList().chunked(bytesPerLine).forEachIndexed { lineIndex, lineBytes ->
                append((startAddress + lineIndex * bytesPerLine).toString(16).uppercase().padStart(4, '0'))
                append(' ')
                append(lineBytes.joinToString(" ") {
                    (it.toInt() and 0xff).toString(16).uppercase().padStart(2, '0')
                })
                append('\n')
            }
        }
    }
}
