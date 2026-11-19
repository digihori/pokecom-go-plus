package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import kotlinx.serialization.json.*
import kotlin.test.*

class Pc1350LcdStripTest {
    @Test fun bundledExampleDrawsAcrossTheBoundaryAndReturns() {
        val machine = Pc1350RomDefinition.MACHINE_ID
        val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
        fun request(tool: String, args: String): JsonObject =
            adapter.call(tool, Json.parseToJsonElement(args).jsonObject)

        val search = request(KnowledgeMcp.SEARCH, """{"machineId":"pc-1350","query":"sprite","topic":"example","includeContent":true}""")
        assertEquals(JsonPrimitive(false), search["isError"])
        val match = search.getValue("structuredContent").jsonObject.getValue("items").jsonArray.single().jsonObject
        val exampleId = match.getValue("item").jsonObject.getValue("exampleId").jsonPrimitive.content
        val example = request(KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1350","exampleId":"$exampleId"}""")
        assertEquals(JsonPrimitive(false), example["isError"])
        val bundle = example.getValue("structuredContent").jsonObject
        assertEquals(match["body"], bundle["guidance"])
        assertEquals(JsonPrimitive(false), bundle["createsFiles"])
        val files = bundle.getValue("files").jsonArray.associate {
            it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
        }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/strip.asm"), files.keys)
        val manifest = Json.parseToJsonElement(files.getValue("pgp-project.json")).jsonObject
        assertEquals("pc-1350", manifest.getValue("machineId").jsonPrimitive.content)
        assertEquals(files.keys - "pgp-project.json", manifest.getValue("sources").jsonArray.map {
            it.jsonObject.getValue("path").jsonPrimitive.content
        }.toSet())
        assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), machine),
        )
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/strip.asm")))
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        assertEquals(0x6000, address("START"))

        // Seed X and Y with distinct bytes, then make an ordinary CALL into RAM.
        val internal = byteArrayOf(
            0x12, 4, 0x02, 0x11, 0xdb.toByte(), 0x50, 0x02, 0x22, 0xdb.toByte(),
            0x50, 0x02, 0x33, 0xdb.toByte(), 0x50, 0x02, 0x44, 0xdb.toByte(),
            0x78, 0x60, 0x00,
        )
        val rom = RomSet(machine, listOf(
            RomComponent(Pc1350RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal.copyOf(0x2000)),
            RomComponent(Pc1350RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x8000)),
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        fun step() = session.step().also { assertIs<ExecutionStatus.Ready>(it.status) }.cycles
        repeat(12) { step() }
        val before = session.cpuSnapshot()
        assertEquals(17, before.programCounter)
        assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44), before.copyInternalRam().copyOfRange(4, 8))
        step()

        val strip = listOf(0x701b, 0x701c, 0x701d, 0x7200, 0x7201, 0x7202)
        val writes = mutableListOf<Int>()
        var frames = 0
        var cycles = 0L
        var delayStarted = 0L
        var delays = 0
        val drawn = address("FRAME_DRAWN")
        val delayDone = address("DELAY_DONE")
        val writeColumn = address("WRITE_COLUMN")
        val sprite = listOf(0x04, 0x0e, 0x04)
        for (instruction in 0 until 2_000) {
            val pc = session.cpuSnapshot().programCounter
            if (pc == internal.size) break
            when (pc) {
                drawn -> {
                    val display = session.displaySnapshot()
                    for (y in 0 until 32) for (x in 0 until 150) {
                        val column = sprite.getOrElse(x - (30 - frames)) { 0 }
                        val expected = y < 8 && column and (1 shl y) != 0
                        assertEquals(expected, display.isDotOn(x, y), "frame $frames, pixel $x,$y")
                    }
                    frames++
                    delayStarted = cycles
                }
                delayDone -> {
                    assertEquals(34_692L, cycles - delayStarted)
                    delays++
                }
            }
            cycles += step()
            if (pc == writeColumn) writes += session.cpuSnapshot().dataPointer
        }
        val after = session.cpuSnapshot()
        assertEquals(internal.size, after.programCounter)
        assertEquals(4, frames)
        assertEquals(4, delays)
        assertEquals(List(5) { strip }.flatten(), writes)
        assertTrue(strip.all { session.memorySnapshot(it, 1).copyBytes().single() == 0.toByte() })
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        for (register in listOf(0, 1, 4, 5, 6, 7, 0x5f)) {
            assertEquals(before.copyInternalRam()[register], after.copyInternalRam()[register], "register $register")
        }
    }
}
