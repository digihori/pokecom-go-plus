package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.MachineMemoryLayout
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopSc61AssemblerTest {
    private val layout = MachineMemoryLayout(MachineId("pc-1350"))
    private val success = DesktopExternalAssemblerRunResult.Exited(2, "ASSEMBLY COMPLETE, NO ERRORS FOUND\n")

    @Test fun requiresSuccessLogAndValidFreshDump() {
        val directory = Files.createTempDirectory("pgp-sc61-import-").toFile()
        try {
            val dump = File(directory, "test.dmp")
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(success, dump, layout))
            dump.writeText("4000 : 12 34 : 46\n\u001a")
            val accepted = assertIs<DesktopExternalAssemblyResult.Success>(DesktopSc61Assembler.importDump(success, dump, layout))
            assertEquals(2, accepted.image.byteCount)
            assertEquals(0x4000, accepted.image.segments.single().startAddress)
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(
                DesktopExternalAssemblerRunResult.Exited(0, "LINE:2 ERROR CODE:009 Illegal Operand"), dump, layout))
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(
                success.copy(log = success.log + "LINE:2 ERROR CODE:009 Illegal Operand"), dump, layout))
            dump.writeText("bad dump")
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(success, dump, layout))
            dump.writeText("1000:12")
            assertIs<DesktopExternalAssemblyResult.Success>(DesktopSc61Assembler.importDump(success, dump, layout, validatePlacement = false))
            assertTrue(assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(success, dump, layout)).message.contains("cannot be loaded"))
            dump.writeText("C000:12\nD000:34")
            assertTrue(assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.importDump(
                success, dump, MachineMemoryLayout(MachineId("pc-1245")))).message.contains("same physical"))
        } finally { directory.deleteRecursively() }
    }

    @Test fun isolatesOutputAndPreservesRelativeInputs() = runBlocking {
        val directory = Files.createTempDirectory("pgp-sc61-source-").toFile()
        try {
            val source = File(directory, "test.asm").apply { writeText("source unchanged") }
            File(directory, "data.bin").writeBytes(byteArrayOf(0x42))
            val oldDump = File(directory, "test.dmp").apply { writeText("original dump") }
            val oldList = File(directory, "test.lst").apply { writeText("original list") }
            val classpath = listOf(Sc61ProcessFixture::class.java, Unit::class.java).joinToString(File.pathSeparator) {
                File(it.protectionDomain.codeSource.location.toURI()).absolutePath
            }
            val settings = DesktopAssemblerSettings(true, File(System.getProperty("java.home"), "bin/java").path,
                "-cp \"$classpath\" ${Sc61ProcessFixture::class.java.name} {source}")
            val result = assertIs<DesktopExternalAssemblyResult.Success>(DesktopSc61Assembler.assemble(settings, source, layout))
            assertContentEquals(byteArrayOf(0x42), result.image.segments.single().copyBytes())
            assertEquals("original dump", oldDump.readText())
            assertEquals("original list", oldList.readText())
            assertEquals("source unchanged", source.readText())
            val staged = File(result.log.lineSequence().first { it.startsWith("STAGED:") }.removePrefix("STAGED:"))
            assertFalse(staged.parentFile.exists())
            source.writeText("fail")
            assertIs<DesktopExternalAssemblyResult.Failure>(DesktopSc61Assembler.assemble(settings, source, layout))
            assertEquals("original dump", oldDump.readText())
        } finally { directory.deleteRecursively() }
    }
}

object Sc61ProcessFixture {
    @JvmStatic fun main(args: Array<String>) {
        val source = File(args.single())
        println("STAGED:${source.absolutePath}")
        if (source.readText() == "fail") {
            println("00001:4000  " + " ".repeat(15) + "fail")
            println("LINE:1 ERROR CODE:009 Illegal Operand")
            return
        }
        val value = File("data.bin").readBytes().single().toInt() and 255
        File(source.parentFile, "input.dmp").writeText("4000 : %02X : %02X\n\u001a".format(value, value))
        // INCBIN-style listing: data exists in the DMP but byte columns may be empty.
        File(source.parentFile, "input.lst").writeText(
            "SC61860 ASSEMBLER Rev 0.04a - ASSEMBLY LIST OF [${source.path}]\n" +
                "00001:4000  " + " ".repeat(15) + source.readText().lineSequence().first() + "\n" +
                "SC61860 ASSEMBLER Rev 0.04a - MAP LIST OF [${source.path}]\n RAW : 4000h\n",
        )
        println("ASSEMBLY COMPLETE, NO ERRORS FOUND")
        kotlin.system.exitProcess(2)
    }
}
