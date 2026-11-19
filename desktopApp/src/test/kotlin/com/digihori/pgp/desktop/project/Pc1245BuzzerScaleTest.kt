package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860Assembler
import com.digihori.pgp.core.debug.Sc61860AssemblyResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.mcp.PgpMcpToolAdapter
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import kotlinx.serialization.json.*
import kotlin.test.*

class Pc1245BuzzerScaleTest {
    @Test fun bundledScalePlaysFiveMeasuredPitchesAndRestoresState() {
        val machine = Pc1245RomDefinition.MACHINE_ID
        val adapter = PgpMcpToolAdapter({ error("Knowledge must not access an emulator session") })
        val search = adapter.call("pgp_search_knowledge", Json.parseToJsonElement(
            """{"machineId":"pc-1245","ids":["pc1245.example.hello-scroll","pc1245.example.buzzer-scale","sc61860.buzzer"]}""",
        ).jsonObject)
        assertEquals(JsonPrimitive(false), search["isError"], search.toString())
        val items = search.getValue("structuredContent").jsonObject.getValue("items").jsonArray
        assertEquals(3, items.size)
        assertEquals(JsonPrimitive(192_000), items.last().jsonObject.getValue("data").jsonObject["cyclesPerSecond"])

        val example = adapter.call("pgp_get_example",
            Json.parseToJsonElement("""{"machineId":"pc-1245","exampleId":"pc1245-buzzer-scale"}""").jsonObject)
        assertEquals(JsonPrimitive(false), example["isError"], example.toString())
        val files = example.getValue("structuredContent").jsonObject.getValue("files").jsonArray.associate {
            it.jsonObject.getValue("path").jsonPrimitive.content to it.jsonObject.getValue("text").jsonPrimitive.content
        }
        assertEquals(setOf("pgp-project.json", "src/main.bas", "src/scale.asm"), files.keys)
        assertIs<DesktopBasicProgramCompileResult.Success>(
            DesktopBasicLoader.compileProgram(files.getValue("src/main.bas").encodeToByteArray(), machine),
        )
        val assembled = assertIs<Sc61860AssemblyResult.Success>(Sc61860Assembler.assemble(files.getValue("src/scale.asm")))
        assertTrue(assembled.image.segments.all { it.startAddress >= 0xc500 && it.startAddress + it.size <= 0xc600 })
        fun address(label: String) = assembled.symbols.single { it.name == label }.address
        val internal = byteArrayOf(0x02, 0x50, 0x32, 0x12, 0x5f, 0x02, 0x21, 0xdb.toByte(),
            0xdf.toByte(), 0x78, 0xc5.toByte(), 0)
        val rom = RomSet(machine, listOf(
            RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal.copyOf(0x2000)),
            RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine, rom)).session
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(assembled.image))
        fun step() = session.step().also { assertIs<ExecutionStatus.Ready>(it.status) }.cycles
        repeat(6) { step() }
        val before = session.cpuSnapshot()
        assertEquals(9, before.programCounter)
        step()

        val notes = mutableListOf<MutableList<Long>>()
        val play = address("PLAY_NOTE")
        val high = address("HIGH_EDGE")
        var cycles = 0L
        var audible = false
        for (instruction in 0 until 80_000) {
            val cpu = session.cpuSnapshot()
            when (cpu.programCounter) {
                internal.size -> break
                play -> {
                    audible = session.drainAudioSamples().copySamples().any { it.toInt() != 0 } || audible
                    notes.add(mutableListOf())
                }
                high -> {
                    assertEquals(0x11, cpu.control)
                    notes.last().add(cycles)
                }
            }
            cycles += step()
        }
        audible = session.drainAudioSamples().copySamples().any { it.toInt() != 0 } || audible
        assertTrue(audible)
        assertEquals(5, notes.size)
        for ((period, edges) in listOf(732L, 644L, 578L, 556L, 490L).zip(notes)) {
            assertEquals(96, edges.size)
            assertEquals(setOf(period), edges.zipWithNext { a, b -> b - a }.toSet())
        }
        val after = session.cpuSnapshot()
        assertEquals(internal.size, after.programCounter)
        assertEquals(before.r, after.r)
        assertEquals(before.control, after.control)
        for (register in listOf(0, 4, 5, 8, 0x5f)) {
            assertEquals(before.copyInternalRam()[register], after.copyInternalRam()[register])
        }
    }
}
