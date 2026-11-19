package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxModel
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxRomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import kotlinx.serialization.json.*
import kotlin.test.*

class LcdBoundaryExamplesTest {
    @Test fun pc1251() = verify("pc-1251", 0xc100, 58, listOf(0xf83a, 0xf83b, 0xf87b, 0xf87a))
    @Test fun pc1401() = verify("pc-1401", 0x4400, 38, listOf(0x6026, 0x6027, 0x6067, 0x6066))
    @Test fun pc1450() = verify("pc-1450", 0x5000, 58, listOf(0x703a, 0x703b, 0x707b, 0x707a))
    @Test fun pc1460() = verify("pc-1460", 0xc000, 28, listOf(0x301c, 0x301d, 0x302d, 0x302e))
    @Test fun pc1470u() = verify("pc-1470u", 0xc000, 58, listOf(0x283a, 0x283b, 0x2a00, 0x2a01))

    private fun verify(id: String, entry: Int, firstColumn: Int, targets: List<Int>) {
        val machine = MachineId(id)
        val model = id.replace("-", "")
        val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
        fun call(tool: String, args: String): JsonObject {
            val result = adapter.call(tool, Json.parseToJsonElement(args).jsonObject)
            assertEquals(JsonPrimitive(false), result["isError"], result.toString())
            return result.getValue("structuredContent").jsonObject
        }
        val batch = call(KnowledgeMcp.SEARCH,
            """{"machineId":"$id","ids":["pgp.program-generation","$model.display","$model.memory","$model.example.lcd-boundary"]}""")
        val items = batch.getValue("items").jsonArray
        assertEquals(4, items.size)
        val drawing = items[1].jsonObject.getValue("data").jsonObject.getValue("display").jsonObject.getValue("directDrawing").jsonObject
        val width = drawing.getValue("dotColumns").jsonPrimitive.int
        val height = drawing.getValue("dotRows").jsonPrimitive.int
        val publishedAddresses = drawing.getValue("columnAddressBlocks").jsonArray.flatMap { entry ->
            val block = entry.jsonObject
            val first = block.getValue("firstAddress").jsonPrimitive.int
            val last = block.getValue("lastAddressInclusive").jsonPrimitive.int
            if (block.getValue("step").jsonPrimitive.int == 1) (first..last).toList() else (first downTo last).toList()
        }
        assertEquals(targets, publishedAddresses.subList(firstColumn, firstColumn + 4))
        val exampleId = items.last().jsonObject.getValue("item").jsonObject.getValue("exampleId").jsonPrimitive.content
        val files = call(KnowledgeMcp.EXAMPLE, """{"machineId":"$id","exampleId":"$exampleId"}""")
            .getValue("files").jsonArray.associate {
                it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
            }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/strip.asm"), files.keys)
        assertEquals(JsonPrimitive(id), Json.parseToJsonElement(files.getValue("pgp-project.json")).jsonObject["machineId"])
        val basic = files.getValue("src/main.bas")
        assertTrue(basic.startsWith("10 CALL &${entry.toString(16).uppercase()}"))
        assertIs<DesktopBasicProgramCompileResult.Success>(DesktopBasicLoader.compileProgram(basic.encodeToByteArray(), machine))
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/strip.asm")))
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        assertEquals(entry, address("START"))

        val internal = byteArrayOf(
            0x02, 0x50, 0x32, 0x12, 4, 0x02, 0x11, 0xdb.toByte(), 0x50, 0x02, 0x22, 0xdb.toByte(),
            0x50, 0x02, 0x33, 0xdb.toByte(), 0x50, 0x02, 0x44, 0xdb.toByte(),
            0x78, (entry shr 8).toByte(), entry.toByte(),
        )
        val bankCount = Pc14xxModel.fromMachineId(machine)?.bankCount ?: 0
        val external = if (bankCount == 0) {
            listOf(RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(if (id == "pc-1251") 0x4000 else 0x8000)))
        } else {
            List(bankCount) { bank -> RomComponent(Pc14xxRomDefinition.bankId(bank), RomRole.EXTERNAL, ByteArray(Pc14xxRomDefinition.BANK_SIZE)) }
        }
        val rom = RomSet(machine, listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal.copyOf(0x2000))) + external)
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        fun step() = session.step().also { assertIs<ExecutionStatus.Ready>(it.status) }.cycles
        repeat(14) { step() }
        val before = session.cpuSnapshot()
        assertContentEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44), before.copyInternalRam().copyOfRange(4, 8))
        step()

        val frameDrawn = address("FRAME_DRAWN")
        val delayDone = address("DELAY_DONE")
        val cleared = address("STRIP_CLEARED")
        val writeInstructions = setOf(address("WRITE_LEFT"), address("WRITE_RIGHT"))
        val writes = mutableListOf<Int>()
        val sprite = listOf(0x0e, 0x04)
        var frames = 0
        var delays = 0
        var cycles = 0L
        var frameCycles = 0L
        var sawClear = false
        for (instruction in 0 until 2_000) {
            val pc = session.cpuSnapshot().programCounter
            if (pc == internal.size) break
            if (pc == frameDrawn || pc == cleared) {
                val display = session.displaySnapshot()
                if (pc == frameDrawn) assertTrue(display.enabled, id)
                for (y in 0 until height) for (x in 0 until width) {
                    val column = if (pc == cleared || y >= 7) 0 else sprite.getOrElse(x - (firstColumn + 2 - frames)) { 0 }
                    assertEquals(column and (1 shl (y % 7)) != 0, display.isDotOn(x, y), "$id frame $frames pixel $x,$y")
                }
                if (pc == frameDrawn) {
                    frames++
                    frameCycles = cycles
                } else sawClear = true
            }
            if (pc == delayDone) {
                assertEquals(17_348L, cycles - frameCycles)
                delays++
            }
            cycles += step()
            if (pc in writeInstructions) writes += session.cpuSnapshot().dataPointer
        }
        val after = session.cpuSnapshot()
        assertEquals(3, frames)
        assertEquals(3, delays)
        assertTrue(sawClear)
        assertEquals(List(4) { targets }.flatten(), writes)
        assertEquals(internal.size, after.programCounter)
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        assertEquals(before.copyInternalRam()[0x5f], after.copyInternalRam()[0x5f])
        assertContentEquals(before.copyInternalRam().copyOfRange(4, 8), after.copyInternalRam().copyOfRange(4, 8))
    }
}
