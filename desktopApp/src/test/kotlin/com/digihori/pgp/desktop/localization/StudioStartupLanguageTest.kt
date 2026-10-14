package com.digihori.pgp.desktop.localization

import java.io.File
import java.io.PrintStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs only string lookup, never Main, preferences, credentials, SDL or ROM loading. */
object StudioStartupLanguageProbe {
    @JvmStatic fun main(args: Array<String>) {
        if (args.isNotEmpty()) StudioStrings.configureForStartup(StudioLanguagePreference.fromValue(args[0]))
        val before = Locale.getDefault()
        val displayBefore = Locale.getDefault(Locale.Category.DISPLAY)
        val strings = StudioStrings.system
        // The subprocess protocol is UTF-8 even when Windows chooses a legacy stdout encoding.
        val output = PrintStream(System.out, true, Charsets.UTF_8)
        output.println(strings.language.name)
        output.println(strings.text("character.title"))
        output.println(1.25.toString())
        output.println(0xC000.toString(16).uppercase())
        check(before == Locale.getDefault())
        check(displayBefore == Locale.getDefault(Locale.Category.DISPLAY))
        output.println("LOCALE_UNCHANGED")
        if (args.size > 1) {
            check(runCatching { StudioStrings.configureForStartup(StudioLanguagePreference.fromValue(args[1])) }.isFailure)
            check(StudioStrings.system === strings)
            output.println("LANGUAGE_FROZEN")
        }
    }
}

class StudioStartupLanguageTest {
    @Test fun freshJvmStartupSelectsJapaneseAndEnglishFallbackWithoutChangingNumbers() {
        for (language in listOf("ja", "en", "fr", "zh")) {
            val lines = launch(language)
            assertEquals(if (language == "ja") "JAPANESE" else "ENGLISH", lines[0])
            assertEquals(if (language == "ja") "ドットエディター" else "Character Editor", lines[1])
            assertEquals("1.25", lines[2])
            assertEquals("C000", lines[3])
            assertEquals("LOCALE_UNCHANGED", lines[4])
        }
    }

    @Test fun storedOverrideWinsAtStartupAndDoesNotChangeDuringTheProcess() {
        for (language in listOf("ja", "en", "fr")) {
            for (preference in listOf("system", "ja", "en", "unknown")) {
                val expected = if (preference == "ja" || (preference !in listOf("ja", "en") && language == "ja")) "JAPANESE" else "ENGLISH"
                val lines = launch(language, preference, if (expected == "JAPANESE") "en" else "ja")
                assertEquals(expected, lines[0], "$language / $preference")
                assertEquals("1.25", lines[2])
                assertEquals("C000", lines[3])
                assertEquals("LOCALE_UNCHANGED", lines[4])
                assertEquals("LANGUAGE_FROZEN", lines[5])
            }
        }
    }

    @Test fun japaneseTextSurvivesNonUtf8StdoutDefault() {
        val lines = launch("ja", asciiOutput = true)
        assertEquals("JAPANESE", lines[0])
        assertEquals("ドットエディター", lines[1])
        assertEquals("LOCALE_UNCHANGED", lines[4])
    }

    private fun launch(language: String, vararg preferences: String, asciiOutput: Boolean = false): List<String> {
        val classes = listOf(StudioStartupLanguageProbe::class.java, StudioStrings::class.java, kotlin.Unit::class.java)
            .map { File(it.protectionDomain.codeSource.location.toURI()).path }
        val resource = "/com/digihori/pgp/desktop/localization/Messages.properties"
        val resourceFile = File(checkNotNull(StudioStrings::class.java.getResource(resource)).toURI())
        val resourceRoot = resourceFile.path.removeSuffix(resource.replace('/', File.separatorChar))
        val classpath = (classes + resourceRoot).distinct().joinToString(File.pathSeparator)
        val executable = File(System.getProperty("java.home"), "bin/${if (System.getProperty("os.name").startsWith("Windows")) "java.exe" else "java"}")
        val command = listOf(executable.path) + (if (asciiOutput) listOf("-Dstdout.encoding=US-ASCII") else emptyList()) +
            listOf("-Duser.language=$language", "-Duser.country=", "-cp", classpath,
            StudioStartupLanguageProbe::class.java.name) + preferences
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val completed = process.waitFor(20, TimeUnit.SECONDS)
        if (!completed) process.destroyForcibly()
        assertTrue(completed, "String lookup subprocess timed out: $language")
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        assertEquals(0, process.exitValue(), output)
        return output.trim().lines()
    }
}
