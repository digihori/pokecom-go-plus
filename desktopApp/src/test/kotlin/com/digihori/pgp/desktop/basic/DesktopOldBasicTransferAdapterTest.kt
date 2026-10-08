package com.digihori.pgp.desktop.basic

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertIs

class DesktopOldBasicTransferAdapterTest {
    @Test fun convertsBothDirections() {
        val transfer = assertIs<DesktopOldBasicTransferResult.Success>(
            DesktopOldBasicTransferAdapter.toTransferBody(byteArrayOf(0xFF.toByte(), 0xE0.toByte(), 0x10, 0x00, 0xFF.toByte())),
        )
        assertContentEquals(byteArrayOf(0xE0.toByte(), 0x10, 0x00, 0xF0.toByte()), transfer.bytes)
        val program = assertIs<DesktopOldBasicTransferResult.Success>(
            DesktopOldBasicTransferAdapter.fromTransferBody(transfer.bytes),
        )
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0xE0.toByte(), 0x10, 0x00, 0xFF.toByte()), program.bytes)
    }

    @Test fun convertsEmptyProgram() {
        val transfer = assertIs<DesktopOldBasicTransferResult.Success>(
            DesktopOldBasicTransferAdapter.toTransferBody(byteArrayOf(0xFF.toByte(), 0xFF.toByte())),
        )
        assertContentEquals(byteArrayOf(0xF0.toByte()), transfer.bytes)
    }

    @Test fun rejectsInvalidMarkers() {
        assertIs<DesktopOldBasicTransferResult.Failure>(DesktopOldBasicTransferAdapter.toTransferBody(byteArrayOf(0xFF.toByte(), 0xF0.toByte())))
        assertIs<DesktopOldBasicTransferResult.Failure>(DesktopOldBasicTransferAdapter.fromTransferBody(byteArrayOf(0xFF.toByte())))
    }
}
