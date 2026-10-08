package com.digihori.pgp.core.wav

/** OLD transfer nibble order and checksum operations. */
public object OldTransferChecksum {
    public const val CHUNK_SIZE: Int = 8
    public const val RESET_INTERVAL: Int = 80

    public fun nibbleSwap(value: Int): Int {
        require(value in 0..0xff) { "Nibble swap value must be one byte" }
        return ((value ushr 4) or (value shl 4)) and 0xff
    }

    public fun calculate(logicalBytes: LogicalByteSequence): Int {
        val accumulator = OldChecksumAccumulator()
        logicalBytes.copyBytes().forEach { accumulator.addLogicalByte(it.toInt() and 0xff) }
        return accumulator.value
    }

    /**
     * Converts logical body bytes to their raw nibble-swapped representation and inserts a
     * cumulative checksum after every complete eight-byte chunk. The sum resets every 80 data
     * bytes; a final partial chunk has no checksum.
     */
    public fun encodeBody(logicalBytes: LogicalByteSequence): RawTransferByteSequence {
        val logical = logicalBytes.copyBytes()
        val fullChunkCount = logical.size / CHUNK_SIZE
        val raw = ByteArray(logical.size + fullChunkCount)
        val accumulator = OldChecksumAccumulator()
        var logicalOffset = 0
        var rawOffset = 0

        while (logicalOffset < logical.size) {
            if (logicalOffset % RESET_INTERVAL == 0) accumulator.reset()
            val chunkLength = minOf(CHUNK_SIZE, logical.size - logicalOffset)

            repeat(chunkLength) { chunkOffset ->
                val value = logical[logicalOffset + chunkOffset].toInt() and 0xff
                accumulator.addLogicalByte(value)
                raw[rawOffset++] = nibbleSwap(value).toByte()
            }
            logicalOffset += chunkLength

            if (chunkLength == CHUNK_SIZE) {
                raw[rawOffset++] = nibbleSwap(accumulator.value).toByte()
            }
        }

        check(rawOffset == raw.size)
        return RawTransferByteSequence(raw)
    }
}

/** Mutable checksum state scoped to one encode/decode operation. */
internal class OldChecksumAccumulator {
    public var value: Int = 0
        private set

    public fun reset() {
        value = 0
    }

    public fun addLogicalByte(byte: Int) {
        require(byte in 0..0xff) { "OLD checksum input must be one byte" }

        value += (byte ushr 4) and 0x0f
        if (value > 0xff) value = (value + 1) and 0xff
        value = (value + (byte and 0x0f)) and 0xff
    }
}
