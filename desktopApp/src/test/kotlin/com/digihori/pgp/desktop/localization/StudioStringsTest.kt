package com.digihori.pgp.desktop.localization

import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.util.MissingResourceException
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.checkpointDescription
import com.digihori.pgp.desktop.displayName

class StudioStringsTest {
    private fun resources(suffix: String): Properties = object : Properties() {
        override fun put(key: Any, value: Any): Any? {
            check(!containsKey(key)) { "Duplicate UI key: $key" }
            return super.put(key, value)
        }
    }.apply {
        checkNotNull(StudioStringsTest::class.java.getResourceAsStream("/com/digihori/pgp/desktop/localization/Messages$suffix.properties"))
            .bufferedReader(Charsets.UTF_8).use { load(it) }
    }

    @Test fun selectsJapaneseOnlyForJapaneseOsLanguage() {
        for (tag in listOf("ja", "ja-JP", "ja-JP-u-ca-japanese")) {
            assertEquals(StudioLanguage.JAPANESE, StudioLanguage.fromOsLocale(Locale.forLanguageTag(tag)))
        }
        for (locale in listOf(Locale.ENGLISH, Locale.FRENCH, Locale.CHINESE, Locale.ROOT)) {
            assertEquals(StudioLanguage.ENGLISH, StudioLanguage.fromOsLocale(locale))
        }
    }

    @Test fun resourcesHaveMatchingKeysAndArgumentsAndFormatWithoutLoss() {
        val english = resources("")
        val japanese = resources("_ja")
        assertEquals(english.stringPropertyNames(), japanese.stringPropertyNames())
        val placeholder = Regex("\\{(\\d+)(?:,[^}]*)?}")
        for (key in english.stringPropertyNames()) {
            val patterns = listOf(english.getProperty(key), japanese.getProperty(key))
            val indexes = patterns.map { pattern -> placeholder.findAll(pattern).map { it.groupValues[1] }.toSet() }
            assertEquals(indexes[0], indexes[1], key)
            for ((index, language) in StudioLanguage.entries.withIndex()) {
                val args = Array((indexes[0].maxOfOrNull { it.toInt() } ?: -1) + 1) { "ARG$it /日本語/{file}/1.25" }
                val actual = StudioStrings(language).text(key, *args)
                assertEquals(MessageFormat(patterns[index], language.locale).format(args), actual, key)
                for (argument in args) assertTrue(actual.contains(argument), "$key: $actual")
            }
        }
    }

    @Test fun uiLanguageDoesNotChangeDefaultLocaleOrOpaqueArguments() {
        val before = Locale.getDefault()
        val path = "/folder with spaces/日本語/{file}/test.bas"
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            assertTrue(strings.text("settings.currentEditor", path).endsWith(path))
            assertEquals(before, Locale.getDefault())

        }
    }

    @Test fun controllerTextIsInjectedAndPocketKeyLabelsRemainOpaque() {
        val english = StudioStrings(StudioLanguage.ENGLISH).controllerStrings()
        val japanese = StudioStrings(StudioLanguage.JAPANESE).controllerStrings()
        assertEquals("None", english.unassignedShort)
        assertEquals("Y / Up", english.buttonLabel(com.digihori.pgp.input.GamepadButton.NORTH))
        for (button in com.digihori.pgp.input.GamepadButton.entries) {
            assertEquals(button.label, japanese.buttonLabel(button))
        }
        assertEquals("Unassigned", english.unassigned)
        assertEquals("未割当", japanese.unassigned)
        assertEquals("Assignment for L1", english.assignment("L1"))
        assertEquals("L1 の割当", japanese.assignment("L1"))
        assertEquals(com.digihori.pgp.controller.ui.ControllerStrings().instructions, japanese.instructions)
    }
    @Test fun missingJapaneseEntryFallsBackToEnglishAndArgumentsCanChangeOrder() {
        val base = "com.digihori.pgp.desktop.localization.Fallback"
        val english = StudioStrings(StudioLanguage.ENGLISH, base)
        val japanese = StudioStrings(StudioLanguage.JAPANESE, base)
        assertEquals("English fallback", japanese.text("baseOnly"))
        assertEquals("A before B", english.text("ordered", "A", "B"))
        assertEquals("Bの後にA", japanese.text("ordered", "A", "B"))
        assertEquals("It's {source}\n/path/{file}", japanese.text("escapes", "/path/{file}"))
        assertFailsWith<MissingResourceException> { english.text("missing") }
        assertFailsWith<IllegalArgumentException> { english.text("ordered", "A") }
        assertFailsWith<IllegalArgumentException> { english.text("ordered", "A", "B", "C") }
    }

    @Test fun checkpointStopReasonKeepsExistingEnglishStorageText() {
        assertEquals("リセット", DebuggerStopReason.Reset.displayName(StudioStrings(StudioLanguage.JAPANESE)))
        assertEquals("Reset", DebuggerStopReason.Reset.displayName(StudioStrings(StudioLanguage.ENGLISH)))
        assertEquals("Reset", DebuggerStopReason.Reset.checkpointDescription())
        assertEquals("Paused by user", DebuggerStopReason.UserPause.checkpointDescription())
    }
    @Test fun basicLabelsPreserveCommandPathsAndStoredSpeedIdentity() {
        val command = "code --goto \"/folder with spaces/日本語/test.bas:42\""
        for (language in StudioLanguage.entries) {
            val strings = StudioStrings(language)
            for (speed in com.digihori.pgp.desktop.runner.DesktopExecutionSpeed.entries) {
                assertTrue(speed.uiLabel(strings).isNotBlank())
                assertEquals(speed, com.digihori.pgp.desktop.runner.DesktopExecutionSpeed.valueOf(speed.name))
            }
            for (state in com.digihori.pgp.desktop.runner.RunnerState.entries) assertTrue(state.uiLabel(strings).isNotBlank())
            val example = strings.text("settings.editorExample")
            assertTrue(example.contains("code --goto \"{file}:{line}\""))
            assertTrue(strings.text("settings.currentEditor", command).endsWith(command))
            assertEquals("raw OS error: /folder/file", strings.editorError("raw OS error: /folder/file"))
        }
        val japanese = StudioStrings(StudioLanguage.JAPANESE)
        assertEquals("3 — 100% (標準)", com.digihori.pgp.desktop.runner.DesktopExecutionSpeed.LEVEL_3.uiLabel(japanese))
        assertTrue(japanese.editorError("Editor command has an unclosed quote").contains("引用符"))
        val fault = DebuggerStopReason.Fault(com.digihori.pgp.core.api.CoreFault.UnsupportedOpcode(0xABCD, 0xFF))
        assertTrue(fault.displayName(japanese).contains("PC=0xABCD"))
        assertEquals("Fault: Emulation stopped: unsupported opcode 0xFF at PC=0xABCD.", fault.checkpointDescription())
    }
}
