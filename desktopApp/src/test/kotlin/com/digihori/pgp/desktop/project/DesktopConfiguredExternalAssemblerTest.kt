package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.rom.MachineId
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopConfiguredExternalAssemblerTest {
    private val layout = MachineMemoryLayout(MachineId("pc-1350"))
    private val generic = DesktopAssemblerSettings(external = true, tool = DesktopAssemblerTool.GENERIC)
    private val yasm = generic.copy(tool = DesktopAssemblerTool.YASM)

    @Test fun legacyPreferencesRemainSc61AndNewSelectionsRoundTrip() {
        val preferences = MemoryAssemblerPreferences()
        preferences.putBoolean("assemblerExternal", true)
        preferences.put("assemblerExecutable", "sc61")
        preferences.put("assemblerArguments", "{source} /tab")
        val store = DesktopAssemblerPreferences(preferences)
        assertEquals(DesktopAssemblerSettings(true, "sc61", "{source} /tab"), store.load())
        for (tool in DesktopAssemblerTool.entries) {
            val value = generic.copy(tool = tool, executable = "tool", arguments = "-o {output} {source}", timeoutSeconds = 7, successExitCode = 2)
            store.save(value)
            assertEquals(value, store.load())
        }
        preferences.remove("assemblerActive")
        preferences.put("assemblerTool", "future-unknown-tool")
        assertEquals(DesktopAssemblerTool.SC61, store.load().tool)
    }

    @Test fun profilesAndActiveSelectionAreIndependent() {
        val preferences = MemoryAssemblerPreferences()
        preferences.putBoolean("assemblerExternal", true)
        preferences.put("assemblerTool", "YASM")
        preferences.put("assemblerExecutable", "original-yasm")
        preferences.put("assemblerArguments", "{source}")
        val store = DesktopAssemblerPreferences(preferences)
        val sc61 = generic.copy(tool = DesktopAssemblerTool.SC61, executable = "sc61", arguments = "{source} /tab", testSource = "sc61-test.asm")
        val custom = generic.copy(executable = "custom", arguments = "-o {output} {source}", successExitCode = 7, testSource = "custom-test.asm")
        store.saveProfile(sc61)
        store.saveProfile(custom)
        assertEquals("original-yasm", store.load().executable)
        store.select(DesktopAssemblerTool.SC61)
        assertEquals(sc61, store.load())
        store.select(DesktopAssemblerTool.GENERIC)
        assertEquals(custom, store.load())
        store.select(DesktopAssemblerTool.YASM)
        assertEquals("original-yasm", store.load().executable)
        store.select(null)
        assertFalse(store.load().external)
        assertEquals(sc61, store.profile(DesktopAssemblerTool.SC61))
        assertEquals(custom, DesktopAssemblerPreferences(preferences).profile(DesktopAssemblerTool.GENERIC))
    }

    @Test fun genericRequiresConfiguredExitAndValidOutput() {
        val directory = Files.createTempDirectory("pgp-generic-import-").toFile()
        try {
            val output = File(directory, "out.dmp")
            fun result(code: Int = 0, config: DesktopAssemblerSettings = generic) =
                DesktopConfiguredExternalAssembler.importOutput(config, DesktopExternalAssemblerRunResult.Exited(code, "tool log"), output)
            assertIs<DesktopExternalAssemblyResult.Failure>(result())
            output.writeText("4000:37")
            assertIs<DesktopExternalAssemblyResult.Success>(result())
            assertIs<DesktopExternalAssemblyResult.Failure>(result(7))
            assertIs<DesktopExternalAssemblyResult.Success>(result(7, generic.copy(successExitCode = 7)))
            output.writeText("4000:37\n4000:12")
            assertIs<DesktopExternalAssemblyResult.Failure>(result())
            output.writeText("FFFF:12 34")
            assertIs<DesktopExternalAssemblyResult.Failure>(result())
            output.writeText("")
            assertIs<DesktopExternalAssemblyResult.Failure>(result())
        } finally { directory.deleteRecursively() }
    }

    @Test fun yasmPreservesUnalignedStartsAndGapsAndRejectsCorruptRows() {
        val directory = Files.createTempDirectory("pgp-yasm-import-").toFile()
        try {
            val output = File(directory, "out.dmp")
            fun result(log: String = "pass 1\npass 2\ncomplete.\n", code: Int = 0) =
                DesktopConfiguredExternalAssembler.importOutput(yasm, DesktopExternalAssemblerRunResult.Exited(code, log), output)
            output.writeText("\n\n4000 :          02 12 37 \n\n4010 :    37 ")
            val accepted = assertIs<DesktopExternalAssemblyResult.Success>(result()).image
            assertEquals(listOf(0x4003, 0x4011), accepted.segments.map { it.startAddress })
            assertContentEquals(byteArrayOf(2, 0x12, 0x37), accepted.segments.first().copyBytes())
            assertIs<DesktopExternalAssemblyResult.Failure>(result(log = "pass 2"))
            assertIs<DesktopExternalAssemblyResult.Failure>(result(code = 25))
            for (bad in listOf("4000 : 37 : 00", "4000 :  37", "4001 : 37", "4000 : 37\n4000 : 37", "4000 : 37 xyz")) {
                output.writeText(bad)
                assertIs<DesktopExternalAssemblyResult.Failure>(result(), bad)
            }
        } finally { directory.deleteRecursively() }
    }

    private fun fixtureSettings(mode: String): DesktopAssemblerSettings {
        val classpath = listOf(GenericAssemblerProcessFixture::class.java, Unit::class.java).joinToString(File.pathSeparator) {
            File(it.protectionDomain.codeSource.location.toURI()).absolutePath
        }
        return generic.copy(executable = File(System.getProperty("java.home"), "bin/java").path,
            arguments = "-cp \"$classpath\" ${GenericAssemblerProcessFixture::class.java.name} $mode {source} {output}")
    }

    @Test fun isolatesOutputsAndNeverImportsStaleOrFailedOutput() = runBlocking {
        val directory = Files.createTempDirectory("pgp generic source-").toFile()
        try {
            val source = File(directory, "test.asm").apply { writeText("original source") }
            val old = File(directory, "test.dmp").apply { writeText("original dump") }
            val settings = fixtureSettings("success")
            val success = assertIs<DesktopExternalAssemblyResult.Success>(DesktopConfiguredExternalAssembler.assemble(settings, source, layout))
            assertContentEquals(byteArrayOf(0x37), success.image.segments.single().copyBytes())
            val scratchOutput = File(success.log.trim())
            assertFalse(scratchOutput.parentFile.exists())
            for (mode in listOf("missing", "failure", "invalid")) {
                assertIs<DesktopExternalAssemblyResult.Failure>(DesktopConfiguredExternalAssembler.assemble(fixtureSettings(mode), source, layout))
                assertEquals("original dump", old.readText())
                assertEquals("original source", source.readText())
            }
        } finally { directory.deleteRecursively() }
    }

    @Test fun localYasmEndToEnd() = runBlocking {
        val script = System.getenv("PGP_YASM_SCRIPT") ?: return@runBlocking
        val directory = Files.createTempDirectory("pgp yasm source-").toFile()
        try {
            val source = File(directory, "test.asm")
            val settings = yasm.copy(executable = System.getenv("PGP_PERL") ?: "perl", arguments = "\"$script\" -d {output} {source}")
            for (arguments in listOf("\"$script\" {source}", "\"$script\" -d {output} {source}", "\"$script\" -d obsolete.dmp {source}"))
            for (address in listOf(0x4000, 0x4003, 0xfff7)) {
                source.writeText("ORG $" + address.toString(16) + "\nLIA $12\nRTN\n")
                val result = assertIs<DesktopExternalAssemblyResult.Success>(DesktopConfiguredExternalAssembler.assemble(settings.copy(arguments = arguments), source, layout))
                assertEquals(address, result.image.segments.first().startAddress)
                assertEquals(listOf<Byte>(2, 0x12, 0x37), result.image.segments.flatMap { it.copyBytes().toList() })
            }
            assertFalse(File(directory, "obsolete.dmp").exists())
            if (File(script).canExecute()) {
                source.writeText("ORG $4000\nRTN\n")
                val direct = settings.copy(executable = script, arguments = "{source}")
                assertIs<DesktopExternalAssemblyResult.Success>(DesktopConfiguredExternalAssembler.assemble(direct, source, layout))
                System.getenv("PGP_YASM_SOURCE")?.let { original ->
                    assertIs<DesktopExternalAssemblyResult.Success>(DesktopConfiguredExternalAssembler.assemble(direct, File(original), layout))
                }
            }
            source.writeText("ORG $4003\nRTN\nORG $4011\nRTN\n")
            val gaps = assertIs<DesktopExternalAssemblyResult.Success>(DesktopConfiguredExternalAssembler.assemble(settings, source, layout)).image
            assertEquals(listOf(0x4003, 0x4011), gaps.segments.map { it.startAddress })
            source.writeText("ORG $4000\nBOGUS\n")
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopConfiguredExternalAssembler.assemble(settings, source, layout))
        } finally { directory.deleteRecursively() }
    }
}

internal class MemoryAssemblerPreferences(parent: java.util.prefs.AbstractPreferences? = null, name: String = "") :
    java.util.prefs.AbstractPreferences(parent, name) {
    private val values = mutableMapOf<String, String>()
    override fun putSpi(key: String, value: String) { values[key] = value }
    override fun getSpi(key: String): String? = values[key]
    override fun removeSpi(key: String) { values.remove(key) }
    override fun keysSpi(): Array<String> = values.keys.toTypedArray()
    override fun childrenNamesSpi(): Array<String> = emptyArray()
    override fun childSpi(name: String) = MemoryAssemblerPreferences(this, name)
    override fun removeNodeSpi() { values.clear() }
    override fun syncSpi() {}
    override fun flushSpi() {}
}

object GenericAssemblerProcessFixture {
    @JvmStatic fun main(args: Array<String>) {
        val output = File(args[2])
        println(output.path)
        when (args[0]) {
            "missing" -> return
            "invalid" -> output.writeText("not a DMP")
            else -> output.writeText("4000:37")
        }
        if (args[0] == "failure") kotlin.system.exitProcess(1)
    }
}
