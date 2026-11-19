package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import com.digihori.pgp.desktop.project.DesktopProjectWorkspace
import com.digihori.pgp.desktop.rom.DesktopRomLoadResult
import com.digihori.pgp.desktop.rom.DesktopRomLoader
import java.io.File
import java.nio.file.Files
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class BasicCallAddressFormattingTest {
    @Test fun formatsOnlyBareDecimalCallStatements() {
        val source = listOf(
            "\uFEFF10 CALL 50432:call 24576",
            "20 IF A=1 THEN CALL 00016 :CALL 65535",
            "30 PRINT \"CALL 50432:THEN CALL 5\":CALL 0",
            "40 REM CALL 50432:CALL 24576",
            "50 IF A=0 THEN REM CALL 50432:CALL 24576",
            "60 DATA \"A:CALL 50432\",12:CALL 24576",
            "70 CALL A:CALL 1+2:CALL (10):CALL -1:CALL 1E3",
            "80 CALL &c500:CALL &HC500:CALL 0xC500:CALL 65536",
            "90 PRINT A ' CALL 50432:CALL 24576",
            "100 CALL 24576\\x3A",
            "110 CALL 1,2:CALL 1.5:CALL 1 AND 2",
        ).joinToString("\r\n", postfix = "\r\n")
        val expected = source
            .replace("10 CALL 50432:call 24576", "10 CALL &C500:call &6000")
            .replace("THEN CALL 00016 :CALL 65535", "THEN CALL &0010 :CALL &FFFF")
            .replace("\":CALL 0", "\":CALL &0000")
            .replace(",12:CALL 24576", ",12:CALL &6000")
        assertEquals(expected, formatBasicCallAddresses("pc-1350", source))
        assertEquals(expected, formatBasicCallAddresses("pc-1350", expected))
        assertEquals(source, formatBasicCallAddresses("unknown", source))
        for (machine in GeneratedProjectFiles.MACHINES) {
            val program = formatBasicCallAddresses(machine, "10 CALL 24576\n20 END\n")
            assertEquals("10 CALL &6000\n20 END\n", program, machine)
            assertIs<DesktopBasicProgramCompileResult.Success>(
                DesktopBasicLoader.compileProgram(program.encodeToByteArray(), MachineId(machine)), machine,
            )
        }
    }

    @Test fun creationAdditionAndPatchWriteTheExactReviewedText() {
        val root = Files.createTempDirectory("pgp-call-review-").toFile()
        try {
            var project: DesktopProjectWorkspace? = null
            val service = DevelopmentChangeService({ DevelopmentReadState(project = project) }, onWorkspaceChanged = { project = it })
            val assembly = GeneratedSource("routine", ProjectSourceType.ASSEMBLY, "; 10 CALL 24576\nRTN\n")
            service.proposeProject("CALL review", "pc-1350",
                listOf(GeneratedSource("main", ProjectSourceType.BASIC, "10 CALL 24576\n"), assembly))
            val creation = service.review()!!.generatedSources
            assertEquals("10 CALL &6000\n", creation.first().text)
            assertEquals(assembly, creation.last())
            service.selectCreationParent(root)
            service.approve()
            val main = project!!.sources.first().file
            assertEquals(creation.first().text, main.readText())

            service.proposeSource(service.projectRevision(), GeneratedSource("extra", ProjectSourceType.BASIC, "10 CALL 24608\n"))
            val added = service.review()!!.generatedSources.single()
            assertEquals("10 CALL &6020\n", added.text)
            service.approve()
            assertEquals(added.text, project!!.sources.last().file.readText())

            val source = service.source("main")
            service.propose("main", source.getValue("projectRevision").jsonPrimitive.content,
                source.getValue("sourceSha256").jsonPrimitive.content, listOf(SourceReplacement("&6000", "24624")))
            val replacement = service.review()!!.replacements.single()
            assertEquals(SourceReplacement("10 CALL &6000\n", "10 CALL &6030\n"), replacement)
            assertEquals(replacement.oldText, main.readText())
            service.approve()
            assertEquals(replacement.newText, main.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun localRomRunsFormattedBasicCallOnEachSupportedModel() {
        if (System.getenv("PGP_VERIFY_BASIC_CALL_ROMS") != "1") return
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile }
        val addresses = mapOf(
            "pc-1245" to 0xc500, "pc-1251" to 0xc100, "pc-1261" to 0x6000,
            "pc-1350" to 0x6000, "pc-1401" to 0x4400, "pc-1450" to 0x5000,
            "pc-1360" to 0xc000, "pc-1360k" to 0xc000, "pc-1460" to 0xc000, "pc-1470u" to 0xc000,
        )
        assertEquals(GeneratedProjectFiles.MACHINES.toSet(), addresses.keys)
        val failures = addresses.mapNotNull { (id, address) ->
            runCatching { runFormattedCall(root, id, address) }.exceptionOrNull()?.let { "$id: ${it.message}" }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private fun runFormattedCall(root: File, id: String, address: Int) {
        val machine = MachineId(id)
        val rom = File(root, "local-data/roms/$id/$id.pgrom")
        val session = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadPackageForMachine(rom.readBytes(), machine),
        ).session
        val catalog = MachineCatalog.require(machine)
        fun press(vararg keys: PocketKey) {
            for (key in keys) {
                session.pressKey(key)
                session.runCycles(catalog.automaticKeyHoldCycles)
                session.releaseKey(key)
                session.runCycles(catalog.automaticKeyGapCycles)
            }
        }
        session.runCycles(1_000_000)
        if (id in setOf("pc-1360", "pc-1360k", "pc-1460", "pc-1470u")) {
            press(PocketKey.Y, PocketKey.ENTER, PocketKey.CLEAR)
            session.runCycles(500_000)
        }
        val loadMode = if (id in setOf("pc-1450", "pc-1460", "pc-1470u")) OperatingMode.PROGRAM else OperatingMode.RUN
        val pc1360Family = id in setOf("pc-1360", "pc-1360k")
        if (!pc1360Family) {
            session.setOperatingMode(loadMode)
            session.runCycles(1_000_000)
        }
        if (id == "pc-1350" && DisplaySymbol.RUN !in session.displaySnapshot().symbols) {
            press(PocketKey.MODE)
            session.runCycles(256_000)
        }
        val marker = address + 0x10
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(
            "ORG $address\nLIDP $marker\nLIA 0x5A\nSTD\nRTN\nORG $marker\nDB 0\n",
        ))
        val source = formatBasicCallAddresses(id, "10 CALL $address\n20 END\n")
        val basic = assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram(source.encodeToByteArray(), machine),
        )
        assertIs<BasicProgramLoadResult.Success>(session.loadBasicProgram(basic.bytes))
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        if (loadMode == OperatingMode.PROGRAM) {
            session.setOperatingMode(OperatingMode.RUN)
            session.runCycles(1_000_000)
        }
        if (!pc1360Family) press(PocketKey.CLEAR)
        press(PocketKey.R, PocketKey.U, PocketKey.N, PocketKey.ENTER)
        session.runCycles(2_000_000)
        assertContentEquals(byteArrayOf(0x5a), session.memorySnapshot(marker, 1).copyBytes(), source.trim())
        assertTrue(session.cpuSnapshot().programCounter !in address until marker, "CALL did not return")
    }
}
