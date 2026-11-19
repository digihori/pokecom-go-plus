package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import kotlinx.serialization.json.*
import kotlin.test.*

class Pc1261LcdStripTest {
    @Test fun bundledTwoBandSpriteCrossesTheBoundaryInSyncAndReturns() {
        val machine = Pc1261RomDefinition.MACHINE_ID
        val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
        fun call(tool: String, args: String): JsonObject {
            val result = adapter.call(tool, Json.parseToJsonElement(args).jsonObject)
            assertEquals(JsonPrimitive(false), result["isError"], result.toString())
            return result.getValue("structuredContent").jsonObject
        }
        val batch = call(KnowledgeMcp.SEARCH, """{"machineId":"pc-1261","ids":["pgp.program-generation","pc1261.display","pc1261.memory","pc1261.example.lcd-strip"]}""")
        val items = batch.getValue("items").jsonArray
        assertEquals(4, items.size)
        val drawing = items[1].jsonObject.getValue("data").jsonObject.getValue("display").jsonObject
            .getValue("directDrawing").jsonObject
        assertEquals(JsonPrimitive(120), drawing["dotColumns"])
        assertEquals(JsonPrimitive(14), drawing["dotRows"])
        val exampleId = items.last().jsonObject.getValue("item").jsonObject.getValue("exampleId").jsonPrimitive.content
        val files = call(KnowledgeMcp.EXAMPLE, """{"machineId":"pc-1261","exampleId":"$exampleId"}""")
            .getValue("files").jsonArray.associate {
                it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
            }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/strip.asm"), files.keys)
        assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), machine),
        )
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/strip.asm")))
        assertTrue(assembled.image.segments.all { it.startAddress >= 0x6000 && it.startAddress + it.size <= 0x6800 })
        fun address(label: String) = assembled.symbols.single { it.name == label }.address

        // Seed X/Y and the stack pointer, then execute a CPU CALL at the BASIC entry address.
        val internal = byteArrayOf(
            0x02, 0x50, 0x32, 0x12, 4, 0x02, 0x11, 0xdb.toByte(), 0x50, 0x02, 0x22, 0xdb.toByte(),
            0x50, 0x02, 0x33, 0xdb.toByte(), 0x50, 0x02, 0x44, 0xdb.toByte(), 0x78, 0x60, 0,
        )
        val rom = RomSet(machine, listOf(
            RomComponent(Pc1261RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal.copyOf(Pc1261RomDefinition.INTERNAL_SIZE)),
            RomComponent(Pc1261RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(Pc1261RomDefinition.EXTERNAL_SIZE)),
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        fun step() = session.step().also { assertIs<ExecutionStatus.Ready>(it.status) }.cycles
        repeat(14) { step() }
        val before = session.cpuSnapshot()
        assertEquals(internal.size - 3, before.programCounter)
        assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44), before.copyInternalRam().copyOfRange(4, 8))
        step()

        val targets = listOf(0x2039, 0x203a, 0x203b, 0x2800, 0x2801, 0x2802, 0x2079, 0x207a, 0x207b, 0x2840, 0x2841, 0x2842)
        val sprites = listOf(listOf(0x40, 0x70, 0x40), listOf(0x01, 0x07, 0x01))
        val frameDrawn = address("FRAME_DRAWN")
        val delayDone = address("DELAY_DONE")
        val writeColumn = address("WRITE_COLUMN")
        val writes = mutableListOf<Int>()
        var frames = 0
        var delays = 0
        var cycles = 0L
        var frameCycles = 0L
        for (instruction in 0 until 2_500) {
            val pc = session.cpuSnapshot().programCounter
            if (pc == internal.size) break
            if (pc == frameDrawn) {
                val display = session.displaySnapshot()
                for (y in 0 until 14) for (x in 0 until 120) {
                    val column = sprites[y / 7].getOrElse(x - (60 - frames)) { 0 }
                    assertEquals(column and (1 shl (y % 7)) != 0, display.isDotOn(x, y), "frame $frames, pixel $x,$y")
                }
                frames++
                frameCycles = cycles
            }
            if (pc == delayDone) {
                assertEquals(34_692L, cycles - frameCycles)
                delays++
            }
            cycles += step()
            if (pc == writeColumn) writes += session.cpuSnapshot().dataPointer
        }
        val after = session.cpuSnapshot()
        assertEquals(4, frames)
        assertEquals(4, delays)
        assertEquals(List(5) { targets }.flatten(), writes)
        assertTrue(targets.all { session.memorySnapshot(it, 1).copyBytes().single() == 0.toByte() })
        assertEquals(internal.size, after.programCounter)
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        assertContentEquals(before.copyInternalRam().copyOfRange(4, 8), after.copyInternalRam().copyOfRange(4, 8))
    }
}
