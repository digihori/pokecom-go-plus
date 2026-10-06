package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.source.machine.PgpMemoryDumpWriter
import com.digihori.pgp.core.source.machine.AddressedMemoryImage

internal object DesktopMemoryDumpWriter {
    fun write(snapshot: MemorySnapshot): ByteArray =
        PgpMemoryDumpWriter.write(snapshot.startAddress, snapshot.copyBytes()).encodeToByteArray()

    fun write(image: AddressedMemoryImage): ByteArray = image.segments.joinToString("\n") { segment ->
        PgpMemoryDumpWriter.write(segment.startAddress, segment.copyBytes()).trimEnd()
    }.plus("\n").encodeToByteArray()
}
