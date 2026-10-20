package com.digihori.pgp.desktop.wav

/** Addressed Intel HEX import/export for a 16-bit binary transfer. No gap filling. */
internal object DesktopWavBinaryFiles {
    fun asDump(bytes: ByteArray): ByteArray {
        val text = bytes.decodeToString(throwOnInvalidSequence = true).removePrefix("\uFEFF").trim()
        if (!text.startsWith(':')) return bytes
        var eof = false
        val dump = buildString {
            text.lineSequence().forEachIndexed { index, original ->
                val line = original.trim()
                if (line.isEmpty()) return@forEachIndexed
                require(!eof) { "Intel HEX data after EOF at line ${index + 1}" }
                require(line.startsWith(':') && line.length >= 11 && line.length % 2 == 1) { "Invalid Intel HEX record at line ${index + 1}" }
                require(line.drop(1).all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) { "Invalid Intel HEX byte at line ${index + 1}" }
                val record = line.drop(1).chunked(2).map { it.toIntOrNull(16) ?: error("Invalid Intel HEX byte at line ${index + 1}") }
                val count = record[0]
                require(record.size == count + 5 && record.sum() and 255 == 0) { "Intel HEX length or checksum mismatch at line ${index + 1}" }
                val address = record[1] * 256 + record[2]
                val data = record.subList(4, 4 + count)
                when (record[3]) {
                    0 -> {
                        require(address + count <= 65536) { "Intel HEX data exceeds the 16-bit address space" }
                        if (count > 0) appendLine("${DesktopWavOperation.hex(address)}: " + data.joinToString(" ") { "%02X".format(it) })
                    }
                    1 -> { require(count == 0 && address == 0) { "Invalid Intel HEX EOF record" }; eof = true }
                    2, 4 -> require(count == 2 && address == 0 && data.all { it == 0 }) { "Intel HEX base address exceeds the 16-bit address space" }
                    else -> error("Unsupported Intel HEX record type ${record[3]}")
                }
            }
        }
        require(eof) { "Intel HEX EOF record is missing" }
        return dump.encodeToByteArray()
    }

    fun writeHex(address: Int, bytes: ByteArray): ByteArray = buildString {
        bytes.asList().chunked(16).forEachIndexed { index, chunk ->
            val at = address + index * 16
            val record = listOf(chunk.size, at ushr 8, at and 255, 0) + chunk.map { it.toInt() and 255 }
            append(':')
            record.forEach { append("%02X".format(it)) }
            appendLine("%02X".format((-record.sum()) and 255))
        }
        appendLine(":00000001FF")
    }.encodeToByteArray()
}
