package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.source.machine.AddressedMemoryImage

internal sealed interface DesktopContiguousMemoryImageResult {
    data class Success(val startAddress: Int, val bytes: ByteArray) : DesktopContiguousMemoryImageResult
    data class Failure(val message: String) : DesktopContiguousMemoryImageResult
}

internal object DesktopContiguousMemoryImage {
    fun flatten(image: AddressedMemoryImage): DesktopContiguousMemoryImageResult {
        val segments = image.segments.sortedBy { it.startAddress }
        if (segments.isEmpty()) return DesktopContiguousMemoryImageResult.Failure("The memory image is empty.")
        val startAddress = segments.first().startAddress
        var expectedAddress = startAddress
        val output = ByteArray(image.byteCount)
        var offset = 0
        segments.forEach { segment ->
            if (segment.startAddress != expectedAddress) {
                return DesktopContiguousMemoryImageResult.Failure(
                    "OLD WAV binary data must be contiguous; gap before 0x${segment.startAddress.toString(16).uppercase().padStart(4, '0')}.",
                )
            }
            val bytes = segment.copyBytes()
            bytes.copyInto(output, offset)
            offset += bytes.size
            expectedAddress += bytes.size
        }
        return DesktopContiguousMemoryImageResult.Success(startAddress, output)
    }
}
