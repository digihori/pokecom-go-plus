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
        val result = assertIs<DesktopContiguousMemoryImageResult.Success>(
            DesktopContiguousMemoryImage.flatten(image),
        )
        assertEquals(0xC000, result.startAddress)
        assertContentEquals(byteArrayOf(1, 2, 3, 4), result.bytes)
    }

    @Test fun rejectsAddressGaps() {
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(
            AddressedMemoryImageFactory.create(
                listOf(
                    AddressedMemorySegmentData(0xC000, byteArrayOf(1)),
                    AddressedMemorySegmentData(0xC002, byteArrayOf(2)),
                ),
            ),
        ).image
        assertIs<DesktopContiguousMemoryImageResult.Failure>(DesktopContiguousMemoryImage.flatten(image))
    }
}
