package com.digihori.pgp.player.android

import com.digihori.pgp.player.PlayerProgramFormat
import java.io.ByteArrayInputStream
import kotlin.test.*

class ProgramFileBoundaryTest {
    @Test fun readsEntireInputAndRejectsOversizedStream() {
        val bytes = ByteArray(20000) { (it % 256).toByte() }
        assertContentEquals(bytes, readProgramBytes(ByteArrayInputStream(bytes), bytes.size))
        assertFailsWith<IllegalArgumentException> { readProgramBytes(ByteArrayInputStream(bytes), bytes.size - 1) }
    }

    @Test fun rejectsUnsupportedAndMismatchedFileTypes() {
        assertEquals(PlayerProgramFormat.BASIC, programFormatForName("EXAMPLE.BAS"))
        assertEquals(PlayerProgramFormat.MEMORY_DUMP, programFormatForName("example.dmp"))
        for (name in listOf("example.bin", "example.hex", "example.bas.txt", "example")) {
            assertNull(programFormatForName(name))
        }
    }

}
