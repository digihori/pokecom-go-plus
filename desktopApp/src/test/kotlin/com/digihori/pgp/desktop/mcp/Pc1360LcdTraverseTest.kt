package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import kotlinx.serialization.json.*
import kotlin.test.*

class Pc1360LcdTraverseTest {
    @Test fun pc1360DrawsAcrossAllFiveBlocks() = verify(Pc1360RomDefinition.MACHINE_ID)
    @Test fun pc1360kDrawsAcrossAllFiveBlocks() = verify(Pc1360RomDefinition.PC1360K_MACHINE_ID)

    private fun verify(machine: MachineId) {
        val machineId = machine.value
        val prefix = machineId.replace("-", "")
        val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
        fun call(tool: String, args: String): JsonObject {
            val result = adapter.call(tool, Json.parseToJsonElement(args).jsonObject)
            assertEquals(JsonPrimitive(false), result["isError"], result.toString())
            return result.getValue("structuredContent").jsonObject
        }
        val batch = call(KnowledgeMcp.SEARCH, """{"machineId":"$machineId","ids":["pgp.program-generation","$prefix.display","$prefix.memory","$prefix.example.lcd-traverse"]}""")
        val items = batch.getValue("items").jsonArray
        val drawing = items[1].jsonObject.getValue("data").jsonObject.getValue("display").jsonObject.getValue("directDrawing").jsonObject
        assertEquals(JsonPrimitive(150), drawing["dotColumns"])
        assertEquals(JsonPrimitive(32), drawing["dotRows"])
        val id = items.last().jsonObject.getValue("item").jsonObject.getValue("exampleId").jsonPrimitive.content
        val files = call(KnowledgeMcp.EXAMPLE, """{"machineId":"$machineId","exampleId":"$id"}""")
            .getValue("files").jsonArray.associate {
                it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
            }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/traverse.asm"), files.keys)
        val manifest = Json.parseToJsonElement(files.getValue("pgp-project.json")).jsonObject
        assertEquals(JsonPrimitive(machineId), manifest["machineId"])
        assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), machine),
        )
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/traverse.asm")))
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        assertEquals(297, address("BOTTOM") - address("TOP"))
        assertEquals(297, address("BOTTOM_END") - address("BOTTOM"))

        val internal = byteArrayOf(
            0x02, 0x50, 0x32, 0x12, 4, 0x02, 0x11, 0xdb.toByte(), 0x50, 0x02, 0x22, 0xdb.toByte(),
            0x50, 0x02, 0x33, 0xdb.toByte(), 0x50, 0x02, 0x44, 0xdb.toByte(), 0x78, 0xc0.toByte(), 0,
        )
        val rom = RomSet(machine, listOf(
            RomComponent(Pc1360RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal.copyOf(Pc1360RomDefinition.INTERNAL_SIZE)),
        ) + List(Pc1360RomDefinition.BANK_COUNT) { bank ->
            RomComponent(Pc1360RomDefinition.bankId(bank), RomRole.EXTERNAL, ByteArray(Pc1360RomDefinition.BANK_SIZE))
        })
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        fun step() = session.step().also { assertIs<ExecutionStatus.Ready>(it.status) }.cycles
        repeat(14) { step() }
        val before = session.cpuSnapshot()
        assertEquals(internal.size - 3, before.programCounter)
        assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44), before.copyInternalRam().copyOfRange(4, 8))
        step()

        fun band(base: Int) = (0 until 150).map { base + (it / 30) * 0x200 + it % 30 }
        val targets = band(0x2800) + List(148) { band(0x2840) + band(0x281e) }.flatten()
        val title = session.memorySnapshot(address("TITLE"), 150).copyBytes()
        val sprites = listOf(listOf(0x80, 0xe0, 0x80), listOf(0x01, 0x07, 0x01))
        val frameDrawn = address("FRAME_DRAWN")
        val delayDone = address("DELAY_DONE")
        val writeColumn = address("WRITE_COLUMN")
        var writes = 0
        var frames = 0
        var delays = 0
        var cycles = 0L
        var frameCycles = 0L
        for (instruction in 0 until 250_000) {
            val pc = session.cpuSnapshot().programCounter
            if (pc == internal.size) break
            if (pc == frameDrawn) {
                val display = session.displaySnapshot()
                for (y in 0 until 32) for (x in 0 until 150) {
                    val column = when (val band = y / 8) {
                        0 -> title[x].toInt()
                        3 -> 0
                        else -> sprites[band - 1].getOrElse(x - (147 - frames)) { 0 }
                    }
                    assertEquals(column and (1 shl (y % 8)) != 0, display.isDotOn(x, y), "frame $frames, pixel $x,$y")
                }
                frames++
                frameCycles = cycles
            }
            if (pc == delayDone) {
                assertEquals(4_340L, cycles - frameCycles)
                delays++
            }
            cycles += step()
            if (pc == writeColumn) assertEquals(targets[writes++], session.cpuSnapshot().dataPointer)
        }
        val after = session.cpuSnapshot()
        assertEquals(148, frames)
        assertEquals(148, delays)
        assertEquals(targets.size, writes)
        assertContentEquals(byteArrayOf(0), session.memorySnapshot(0x303c, 1).copyBytes())
        assertEquals(internal.size, after.programCounter)
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        assertContentEquals(before.copyInternalRam().copyOfRange(4, 8), after.copyInternalRam().copyOfRange(4, 8))
    }
}
