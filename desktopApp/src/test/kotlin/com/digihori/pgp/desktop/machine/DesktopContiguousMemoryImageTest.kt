package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult
import com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory
import com.digihori.pgp.core.source.machine.AddressedMemorySegmentData
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopContiguousMemoryImageTest {
    @Test fun joinsAdjacentDumpLines() {
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(
            AddressedMemoryImageFactory.create(
                listOf(
                    AddressedMemorySegmentData(0xC000, byteArrayOf(1, 2)),
                    AddressedMemorySegmentData(0xC002, byteArrayOf(3, 4)),
                ),
            ),
        ).image
        val result = DesktopContiguousMemoryImage(image)
        assertEquals(listOf(0xC000..0xC003), result.regions)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), result.bytes(result.fullRange))
    }

    @Test fun findsRegionsAndFillsAddressGaps() {
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(
            AddressedMemoryImageFactory.create(
                listOf(
                    AddressedMemorySegmentData(0xC000, byteArrayOf(1)),
                    AddressedMemorySegmentData(0xC002, byteArrayOf(2)),
                ),
            ),
        ).image
        val result = DesktopContiguousMemoryImage(image)
        assertEquals(1, result.paddingBytes)
        assertEquals(listOf(0xC000..0xC000, 0xC002..0xC002), result.regions)
        assertContentEquals(byteArrayOf(1, 0, 2), result.bytes(result.fullRange))
        assertContentEquals(byteArrayOf(2), result.bytes(result.regions.last()))
    }
}
