package com.digihori.pgp.desktop.project

import java.nio.file.Files
import kotlin.test.*
import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.MonotonicClock
import com.digihori.pgp.core.rom.RomComponentId

class DesktopDebugSourceMapTest {
    @Test fun resolvesLoadedListingAndRejectsChangedBytesSourcesAndPhysicalLocations() {
        val root = Files.createTempDirectory("pgp-source-map-").toFile()
        try {
            val file = root.resolve("main.asm").apply { writeText("# Description\nORG 0xC500\nstart: LIA 7\nRTN\n") }
            val manifest = root.resolve("pgp-project.json").apply { writeText("""{
                "format":"pgp-project", "formatVersion":1, "name":"Test", "machineId":"pc-1245",
                "sources":[{"id":"main","type":"assembly","path":"main.asm"}]
            }""") }
            val workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace
            val source = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).sources.single()
            val id = MachineId("pc-1261")
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x8000)),
            )))).session
            val runner = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            assertNull(runner.sourceMap)
            runner.attachSourceMap(listOf(source))
            val previous = runner.sourceMap
            // Failed load must retain the last successful build metadata.
            assertIs<DesktopProjectApplyResult.MemoryFailure>(DesktopProjectArtifactLoader.load(runner,
                DesktopProjectArtifact(null, source.image, listOf(source))))
            assertSame(previous, runner.sourceMap)
            runner.reset()
            assertNull(runner.sourceMap)
            val bytes = source.listing.flatMap { row -> row.bytes.mapIndexed { i, value -> row.address + i to value } }.toMap()
            val map = DesktopDebugSourceMap.create(listOf(source)) { null }
            assertTrue(map.sourcesUnchanged())
            val row = assertNotNull(map.resolve(0xc500, { bytes.getValue(it) }) { null })
            assertEquals(3, row.location.line)
            assertEquals(listOf("start"), row.labels)
            assertNotNull(map.resolve(0xc501, { bytes.getValue(it) }) { null })
            assertNull(map.resolve(0xc500, { if (it == 0xc501) 8 else bytes.getValue(it) }) { null })
            assertNull(map.resolve(0xc499, { 0 }) { null })
            val bank0 = PhysicalRomLocation(RomComponentId("bank-0"), 0, 0)
            val bank1 = PhysicalRomLocation(RomComponentId("bank-1"), 1, 0)
            val bankMap = DesktopDebugSourceMap.create(listOf(source)) { bank0 }
            assertNull(bankMap.resolve(0xc500, { bytes.getValue(it) }) { bank1 })
            assertNotNull(bankMap.resolve(0xc500, { bytes.getValue(it) }) { bank0 })
            file.appendText("; changed\n")
            assertFalse(map.sourcesUnchanged())
            file.delete()
            assertFalse(map.sourcesUnchanged())
            assertNull(DesktopDebugSourceMap.create(listOf(source, source)) { null }.resolve(0xc500, { bytes.getValue(it) }) { null })
        } finally { root.deleteRecursively() }
    }

    @Test fun capturesIncludeInputsBeforeExternalAssembly() {
        val root = Files.createTempDirectory("pgp-source-inputs-").toFile()
        try {
            val included = root.resolve("part.asm").apply { writeText("part: RTN\n") }
            val source = root.resolve("main.asm").apply { writeText("INCLUDE (part.asm)\n") }
            val inputs = DesktopDebugSourceMap.captureInputs(source)
            assertEquals(2, inputs.size)
            included.writeText("part: LIA 1\n")
            assertEquals("part: RTN\n", inputs[included.canonicalFile])
        } finally { root.deleteRecursively() }
    }
}
