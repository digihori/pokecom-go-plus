package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.PgpMemoryDumpWriter
import java.io.File
import kotlin.test.*

class Pc1401HelloScrollTest {
    @Test fun everyFrameMovesLeftAcrossTheLcdBankBoundaryAndReturnsWithRegistersRestored() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .first { File(it, "test-data/assembly/pc1401-hello-scroll/src/scroll.asm").isFile }
        val source = File(root, "test-data/assembly/pc1401-hello-scroll/src/scroll.asm").readText()
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(source))
        assertTrue(assembled.image.segments.all { it.startAddress >= 0x4400 && it.startAddress + it.size <= 0x4500 })
        val output = File(root, "desktopApp/build/generated/examples/pc1401-hello-scroll.dmp")
        output.parentFile.mkdirs()
        output.writeText(assembled.image.segments.joinToString("") { PgpMemoryDumpWriter.write(it.startAddress, it.copyBytes()) })
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        val rom = RomSet(Pc1401RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1401RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000).also {
                byteArrayOf(0x02, 0x50, 0x32, 0x78, 0x44.toByte(), 0, 0x79, 0, 6).copyInto(it)
            }),
            RomComponent(Pc1401RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x8000)),
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        repeat(2) { session.step() }
        val before = session.cpuSnapshot()
        session.step() // Synthetic CALL 0x4400, with a real return address on the stack.
        val columns = List(25) { 0 } + listOf(
            127,8,8,8,127, 127,73,73,73,65, 127,64,64,64,64,
            127,64,64,64,64, 62,65,65,65,62, 0,0,0,0,0,
            127,32,24,32,127, 62,65,65,65,62, 127,9,25,41,70,
            127,64,64,64,64, 127,65,65,65,62,
        )
        var frames = 0
        var initialChecked = false
        var steps = 0
        fun assertFrame(shift: Int) {
            val display = session.displaySnapshot()
            assertTrue(display.enabled)
            for (x in 0 until 80) for (y in 0 until 7) {
                val expected = if (x + shift < 80) columns[x + shift] else 0
                assertEquals(expected and (1 shl y) != 0, display.isDotOn(x, y), "frame $shift pixel $x,$y")
            }
        }
        while (session.cpuSnapshot().programCounter != address("DONE") && steps++ < 100_000) {
            when (session.cpuSnapshot().programCounter) {
                address("FRAME") -> if (!initialChecked) { assertFrame(0); initialChecked = true }
                address("FRAME_END") -> { frames++; assertFrame(frames) }
            }
            assertIs<ExecutionStatus.Ready>(session.step().status)
        }
        assertTrue(initialChecked)
        assertEquals(80, frames)
        assertEquals(address("DONE"), session.cpuSnapshot().programCounter)
        session.step() // RTN
        val after = session.cpuSnapshot()
        assertEquals(6, after.programCounter)
        assertEquals(before.r, after.r)
        for (register in listOf(0, 1, 4, 5, 6, 7)) {
            assertEquals(before.copyInternalRam()[register], after.copyInternalRam()[register], "internal register $register")
        }
        assertContentEquals(byteArrayOf(0, 0), session.memorySnapshot(0x603c, 2).copyBytes())
        assertContentEquals(byteArrayOf(0), session.memorySnapshot(0x607c, 1).copyBytes())
    }
}
