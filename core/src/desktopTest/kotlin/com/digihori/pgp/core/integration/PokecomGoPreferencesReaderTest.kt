package com.digihori.pgp.core.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PokecomGoPreferencesReaderTest {
    @Test
    fun extractsAndDecodesTheExistingPrefScValue() {
        val xml = """
            <?xml version="1.0" encoding="utf-8" standalone="yes"?>
            <map>
                <boolean name="debug" value="false" />
                <string name="PREF_SC">{&quot;id&quot;:1245,&quot;label&quot;:&quot;A&amp;B&quot;}</string>
            </map>
        """.trimIndent()

        assertEquals(
            "{\"id\":1245,\"label\":\"A&B\"}",
            PokecomGoPreferencesReader.extractSavedStateJson(xml),
        )
    }

    @Test
    fun rejectsMissingOrEmptySavedState() {
        assertFailsWith<IllegalStateException> {
            PokecomGoPreferencesReader.extractSavedStateJson("<map />")
        }
        assertFailsWith<IllegalStateException> {
            PokecomGoPreferencesReader.extractSavedStateJson("<map><string name=\"PREF_SC\" /></map>")
        }
    }

    @Test
    fun rejectsDoctypeAndExternalEntityInput() {
        val xml = """<!DOCTYPE map [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <map><string name="PREF_SC">&xxe;</string></map>"""

        assertFailsWith<Exception> {
            PokecomGoPreferencesReader.extractSavedStateJson(xml)
        }
    }
}
