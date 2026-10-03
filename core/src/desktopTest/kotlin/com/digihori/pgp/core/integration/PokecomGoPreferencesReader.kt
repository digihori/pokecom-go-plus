package com.digihori.pgp.core.integration

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

internal object PokecomGoPreferencesReader {
    fun readPc1245Expectation(preferencesXml: String): GoldenExpectation =
        PokecomGoStateImporter.importPc1245Expectation(extractSavedStateJson(preferencesXml))

    fun extractSavedStateJson(preferencesXml: String): String {
        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(preferencesXml)))
        val strings = document.getElementsByTagName("string")
        for (index in 0 until strings.length) {
            val element = strings.item(index)
            val name = element.attributes?.getNamedItem("name")?.nodeValue
            if (name == SAVED_STATE_KEY) {
                return element.textContent.takeIf(String::isNotBlank)
                    ?: error("Pokecom GO preference '$SAVED_STATE_KEY' is empty")
            }
        }
        error("Pokecom GO preference '$SAVED_STATE_KEY' was not found")
    }

    private const val SAVED_STATE_KEY: String = "PREF_SC"
}
