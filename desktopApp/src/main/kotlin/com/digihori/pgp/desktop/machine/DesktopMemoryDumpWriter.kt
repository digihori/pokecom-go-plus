package com.digihori.pgp.desktop.machine

import com.digihori.pgp.core.api.MemorySnapshot
import com.digihori.pgp.core.source.machine.PgpMemoryDumpWriter

internal object DesktopMemoryDumpWriter {
    fun write(snapshot: MemorySnapshot): ByteArray =
        PgpMemoryDumpWriter.write(snapshot.startAddress, snapshot.copyBytes()).encodeToByteArray()
}
