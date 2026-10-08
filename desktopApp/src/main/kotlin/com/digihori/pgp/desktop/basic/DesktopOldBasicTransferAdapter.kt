package com.digihori.pgp.desktop.basic

internal sealed interface DesktopOldBasicTransferResult {
    data class Success(val bytes: ByteArray) : DesktopOldBasicTransferResult
    data class Failure(val message: String) : DesktopOldBasicTransferResult
}

/** Converts between the Studio/emulator BASIC image and an OLD cassette body. */
internal object DesktopOldBasicTransferAdapter {
    fun toTransferBody(program: ByteArray): DesktopOldBasicTransferResult {
        if (program.size < 2 || program.first().toInt() and 0xFF != MARKER) {
            return DesktopOldBasicTransferResult.Failure("BASIC program must begin with FF.")
        }
        if (program.last().toInt() and 0xFF != MARKER) {
            return DesktopOldBasicTransferResult.Failure("BASIC program must end with FF.")
        }
        return DesktopOldBasicTransferResult.Success(
            program.copyOfRange(1, program.size).also { it[it.lastIndex] = TRANSFER_END.toByte() },
        )
    }

    fun fromTransferBody(body: ByteArray): DesktopOldBasicTransferResult {
        if (body.isEmpty() || body.last().toInt() and 0xFF != TRANSFER_END) {
            return DesktopOldBasicTransferResult.Failure("OLD BASIC body must end with F0.")
        }
        val program = ByteArray(body.size + 1)
        program[0] = MARKER.toByte()
        body.copyInto(program, destinationOffset = 1)
        program[program.lastIndex] = MARKER.toByte()
        return DesktopOldBasicTransferResult.Success(program)
    }

    private const val MARKER = 0xFF
    private const val TRANSFER_END = 0xF0
}
