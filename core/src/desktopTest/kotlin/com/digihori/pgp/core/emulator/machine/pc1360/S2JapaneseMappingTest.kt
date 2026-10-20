package com.digihori.pgp.core.emulator.machine.pc1360

import java.nio.charset.Charset
import kotlin.test.*

class S2JapaneseMappingTest {
    @Test fun assignedPairsAgreeWithHostWindows31jAndCanonicalRoundTrip() {
        val charset = Charset.forName("windows-31j")
        var count = 0
        for (lead in (0x81..0x9f) + (0xe0..0xfc)) for (trail in (0x40..0x7e) + (0x80..0xfc)) {
            val decoded = S2JapaneseText.decode(lead, trail) ?: continue
            assertEquals(String(byteArrayOf(lead.toByte(), trail.toByte()), charset), decoded.toString(), "${lead.toString(16)}${trail.toString(16)}")
            val encoded = assertNotNull(S2JapaneseText.encode(decoded))
            assertEquals(decoded, S2JapaneseText.decode(encoded ushr 8, encoded and 255))
            count++
        }
        assertTrue(count > 9000)
        assertNull(S2JapaneseText.decode(0x81, 0x7f))
        assertNull(S2JapaneseText.encode('\ud83d'))
    }
}
