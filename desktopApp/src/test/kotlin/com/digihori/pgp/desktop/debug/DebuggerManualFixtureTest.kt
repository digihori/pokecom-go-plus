package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.MemoryImageLoadResult
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import java.io.File
import kotlin.test.*

class DebuggerManualFixtureTest {
    @Test fun documentedAddressesAndCallCaseOutcomesMatchTheShippedFixture() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "test-data/assembly/pc1245-debugger-acceptance/src/debugger.asm").isFile }
        val source = File(root, "test-data/assembly/pc1245-debugger-acceptance/src/debugger.asm").readText()
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source))
        val expected = mapOf("START" to 0xc000, "AFTER_CALL" to 0xc009, "SUB" to 0xc020,
            "CASE_START" to 0xc040, "CASE_ENTRY" to 0xc042, "CASE_AFTER" to 0xc080, "DATA" to 0xc100)
        expected.forEach { (name, address) -> assertEquals(address, assembled.symbols.single { it.name == name }.address) }
        for ((entry, stop, value) in listOf(Triple(0xc000, 0xc009, 0x42), Triple(0xc040, 0xc080, 1))) {
            // Synthetic ROM only transfers execution to the same RAM entry used by BASIC CALL.
            val runner = debuggerTestRunner(byteArrayOf(0x78, (entry shr 8).toByte(), entry.toByte()))
            assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(assembled.image))
            assertEquals(0xde, runner.memoryByte(0xc100))
            repeat(20) { if (runner.cpuSnapshot().programCounter != stop) runner.step() }
            assertEquals(stop, runner.cpuSnapshot().programCounter)
            assertEquals(value, runner.cpuSnapshot().copyInternalRam()[2].toInt() and 255)
            if (entry == 0xc000) assertEquals(0x42, runner.memoryByte(0xc100))
        }
    }
}
