package com.digihori.pgp.core.source.machine

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddressedMemoryImageFactoryTest {
    @Test
    fun createsSortedImageAndCopiesInputBytes() {
        val original = byteArrayOf(1, 2)
        val created = assertIs<AddressedMemoryImageCreateResult.Success>(
            AddressedMemoryImageFactory.create(
                listOf(
                    AddressedMemorySegmentData(0xc100, byteArrayOf(3)),
                    AddressedMemorySegmentData(0xc000, original),
                ),
            ),
        )
        original[0] = 9

        assertEquals(2, created.image.segments.size)
        assertEquals(0xc000, created.image.segments.first().startAddress)
        assertContentEquals(byteArrayOf(1, 2), created.image.segments.first().copyBytes())
    }

    @Test
    fun rejectsOverlapAndAddressOverflow() {
        assertIs<AddressedMemoryImageCreateResult.Overlap>(
            AddressedMemoryImageFactory.create(
                listOf(
                    AddressedMemorySegmentData(0xc000, byteArrayOf(1, 2)),
                    AddressedMemorySegmentData(0xc001, byteArrayOf(3)),
                ),
            ),
        )
        assertIs<AddressedMemoryImageCreateResult.InvalidSegment>(
            AddressedMemoryImageFactory.create(
                listOf(AddressedMemorySegmentData(0xffff, byteArrayOf(1, 2))),
            ),
        )
    }
}
