package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.desktop.rom.*
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import java.io.File
import kotlin.test.*
import kotlinx.serialization.json.*

/** Runs the checked-in example through Studio's actual project builder/loader. */
class Pc1245HelloWorldTest {
    @Test fun buildsLoadsDrawsAndReturnsToTheCallerWithoutChangingItsStack() {
        val rom = RomSet(Pc1245RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000).also {
                byteArrayOf(0x02, 0x50, 0x32, 0x78, 0xc5.toByte(), 0, 0x79, 0, 6).copyInto(it)
            }), RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000))))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, rom)).session
        val runner = DesktopEmulatorRunner(session)
        val spec = DesktopDevelopmentReadService { DevelopmentReadState(runner) }.machineSpec(null)
        assertEquals(80, spec["display"]!!.jsonObject["directDrawing"]!!.jsonObject["dotColumns"]!!.jsonPrimitive.int)
        val pointers = assertIs<com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult.Success>(
            com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory.create(listOf(
                com.digihori.pgp.core.source.machine.AddressedMemorySegmentData(0xc6e1, byteArrayOf(0, 0xc0.toByte(), 0, 0xc0.toByte()))))).image
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(pointers))
        val artifact = build(runner)
        repeat(2) { session.step() } // Establish a RAM stack outside the control-port register.
        val before = session.cpuSnapshot().r
        session.step() // CALL C500 in the synthetic caller.
        assertEquals(0xc500, session.cpuSnapshot().programCounter)
        val hold = artifact.assemblySources.single().symbols.first { it.name == "HOLD_START" }.address
        val done = artifact.assemblySources.single().symbols.first { it.name == "DONE" }.address
        repeat(1_000) { if (session.cpuSnapshot().programCounter != hold) assertIs<ExecutionStatus.Ready>(session.step().status) }
        assertDisplay(session)
        assertEquals(hold, session.cpuSnapshot().programCounter)
        repeat(10_000) {
            if (session.cpuSnapshot().programCounter != done) assertIs<ExecutionStatus.Ready>(session.step().status)
        }
        assertEquals(done, session.cpuSnapshot().programCounter)
        assertDisplay(session)
        session.step() // RTN
        assertEquals(6, session.cpuSnapshot().programCounter)
        assertEquals(before, session.cpuSnapshot().r)
        assertTrue(artifact.memoryImage!!.segments.all { it.startAddress + it.size <= 0xc600 })
    }

    @Test fun localFirmwareRunsBasicCallDrawsAndReturns() {
        val rom = ancestors().map { File(it, "local-data/roms/pc-1245/pc1245mem.bin") }.firstOrNull(File::isFile)
        org.junit.Assume.assumeTrue("Optional locally supplied PC-1245 ROM is unavailable", rom != null)
        val loaded = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPc1245LegacyImage(rom!!.readBytes()))
        val runner = DesktopEmulatorRunner(loaded.session)
        val session = loaded.session
        assertIs<ExecutionStatus.Ready>(session.runCycles(1_000_000).status)
        fun tap(key: PocketKey) {
            session.pressKey(key); session.runCycles(17_280)
            session.releaseKey(key); session.runCycles(57_600)
        }
        fun calculate(): ByteArray {
            tap(PocketKey.CLEAR); tap(PocketKey.NUM_1); tap(PocketKey.PLUS); tap(PocketKey.NUM_1); tap(PocketKey.ENTER)
            session.runCycles(100_000)
            return session.displaySnapshot().copyDots()
        }
        val expectedCalculation = calculate()
        assertTrue(expectedCalculation.any { it.toInt() != 0 })
        val artifact = build(runner)
        tap(PocketKey.CLEAR)
        tap(PocketKey.R); tap(PocketKey.U); tap(PocketKey.N)
        session.pressKey(PocketKey.ENTER)
        var entered = false
        repeat(200_000) {
            if (!entered) {
                if (session.cpuSnapshot().programCounter != 0xc500) {
                    assertIs<ExecutionStatus.Ready>(session.step().status)
                } else entered = true
            }
        }
        session.releaseKey(PocketKey.ENTER)
        assertTrue(entered, "ROM did not invoke BASIC CALL 50432")
        val entryR = session.cpuSnapshot().r
        val hold = artifact.assemblySources.single().symbols.first { it.name == "HOLD_START" }.address
        val done = artifact.assemblySources.single().symbols.first { it.name == "DONE" }.address
        repeat(1_000) { if (session.cpuSnapshot().programCounter != hold) assertIs<ExecutionStatus.Ready>(session.step().status) }
        assertDisplay(session)
        assertEquals(hold, session.cpuSnapshot().programCounter)
        repeat(10_000) {
            if (session.cpuSnapshot().programCounter != done) assertIs<ExecutionStatus.Ready>(session.step().status)
        }
        assertEquals(done, session.cpuSnapshot().programCounter)
        assertDisplay(session)
        assertEquals(0x37, session.memorySnapshot(done, 1).copyBytes()[0].toInt() and 255)
        session.step()
        assertEquals((entryR + 2) and 0x7f, session.cpuSnapshot().r)
        assertTrue(session.cpuSnapshot().programCounter !in 0xc500..0xc5ff)
        assertIs<ExecutionStatus.Ready>(session.runCycles(500_000).status)
        assertContentEquals(expectedCalculation, calculate(), "BASIC 1+1 must still produce the pre-CALL display")
    }

    private fun build(runner: DesktopEmulatorRunner): DesktopProjectArtifact {
        val manifest = System.getenv("PGP_HELLO_PROJECT")?.let { File(it, "pgp-project.json") }
            ?: ancestors().map { File(it, "test-data/assembly/pc1245-hello/pgp-project.json") }.first(File::isFile)
        val workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace
        val compiled = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace))
        val artifact = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, runner.memoryLayout, compiledAssembly = compiled)).artifact
        assertIs<DesktopProjectApplyResult.Success>(DesktopProjectArtifactLoader.load(runner, artifact))
        return artifact
    }

    private fun assertDisplay(session: EmulatorSession) {
        val glyphs = mapOf(
            'H' to listOf(127,8,8,8,127), 'E' to listOf(127,73,73,73,65),
            'L' to listOf(127,64,64,64,64), 'O' to listOf(62,65,65,65,62),
            ' ' to List(5) { 0 }, 'W' to listOf(127,32,24,32,127),
            'R' to listOf(127,9,25,41,70), 'D' to listOf(127,65,65,65,62),
        )
        val columns = "HELLO WORLD".flatMap { glyphs.getValue(it) } + List(25) { 0 }
        val display = session.displaySnapshot()
        assertTrue(display.enabled)
        for (x in 0 until 80) for (y in 0 until 7) {
            assertEquals(columns[x] and (1 shl y) != 0, display.isDotOn(x, y), "pixel $x,$y")
        }
    }

    private fun ancestors() = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
}
