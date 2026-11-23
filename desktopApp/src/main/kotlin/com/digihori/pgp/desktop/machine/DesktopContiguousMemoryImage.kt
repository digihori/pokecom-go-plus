package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.source.machine.AddressedMemoryImage

internal class DesktopContiguousMemoryImage(private val image: AddressedMemoryImage) {
    val fullRange = image.segments.first().startAddress..image.segments.last().endAddress
    val paddingBytes = fullRange.count() - image.byteCount

    val regions: List<IntRange> = buildList {
        var start = fullRange.first
        var end = start - 1
        for (segment in image.segments) {
            if (segment.startAddress > end + 1) {
                add(start..end)
                start = segment.startAddress
            }
            end = segment.endAddress
        }
        add(start..end)
    }

    fun ranges(split: Boolean): List<IntRange> = if (split) regions else listOf(fullRange)

    fun bytes(range: IntRange): ByteArray {
        val output = ByteArray(range.count())
        val firstIndex = image.segments.binarySearchBy(range.first) { it.startAddress }
        check(firstIndex >= 0)
        for (index in firstIndex until image.segments.size) {
            val segment = image.segments[index]
            if (segment.startAddress > range.last) break
            segment.copyBytes().copyInto(output, segment.startAddress - range.first)
        }
        return output
    }
}
