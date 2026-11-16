package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.desktop.application.debug.DesktopDebugContextService
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.application.execution.*
import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.rom.*
import com.digihori.pgp.desktop.runner.*
import java.io.File
import java.nio.file.Files
import java.util.Base64
import kotlinx.serialization.json.*
import kotlin.test.*

class BasicMachineWorkflowTest {
    @Test fun pc1261CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1261", 0x4000, 0x67ff)
    @Test fun pc1350CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1350", 0x6000, 0x6fff)

    @Test fun pc1401CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1401", 0x4400, 0x47ff)

    @Test fun pc1261LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1261", 24, 2, 0x6000)
    @Test fun pc1350LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1350", 25, 4, 0x6000)

    @Test fun pc1401LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1401", 16, 1, 0x4400)

    @Test fun pc1450CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1450", 0x5000, 0x5fff)
    @Test fun pc1450LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1450", 16, 1, 0x5000)

    @Test fun pc1360CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1360", 0xc000, 0x7fff)
    @Test fun pc1360LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1360", 25, 4, 0xc000)

    @Test fun pc1360kCatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1360k", 0xc000, 0x7fff)
    @Test fun pc1360kLocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1360k", 25, 4, 0xc000)

    @Test fun pc1460CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1460", 0xc000, 0x7fff)
    @Test fun pc1460LocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1460", 24, 1, 0xc000)

    @Test fun pc1470uCatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1470u", 0xc000, 0x7fff)
    @Test fun pc1470uLocallySuppliedRomRunsGeneratedBasic() = runBasicAndCall("pc-1470u", 24, 2, 0xc000)

    private fun catalogGenerationAndAnalysis(machine: String, address: Int, invalidAddress: Int) {
        val knowledge = PocketKnowledgeService()
        val memory = knowledge.get(machine, "${machine.replace("-", "")}.memory")
        assertEquals("${machine.replace("-", "")}-v1", memory.getValue("catalogVersion").jsonPrimitive.content)
        assertFailsWith<DevelopmentReadError> { knowledge.get(machine, "pc1251.memory") }
        val analysis = ProgramAnalysisService()
        val source = analysis.prepare("main.bas", source(machine).encodeToByteArray(), AnalysisProgramKind.BASIC, machine, null, null, "")
        analysis.publish(source)
        assertEquals(JsonPrimitive(machine), analysis.describe(source)["machineId"])
        val routine = byteArrayOf(0x02, 0x5a, 0x37)
        val imported = analysis.prepare("routine.bin", routine, AnalysisProgramKind.RAW, machine, address, address, "")
        analysis.publish(imported)
        val instructions = analysis.disassembly(imported.id, imported.revision, null, 2).getValue("instructions").jsonArray
        assertEquals(address, instructions.first().jsonObject.getValue("address").jsonPrimitive.int)
        assertEquals(listOf("LIA 0x5A", "RTN"), instructions.map { it.jsonObject.getValue("text").jsonPrimitive.content })
        assertFailsWith<DevelopmentReadError> {
            analysis.prepare("outside.bin", routine, AnalysisProgramKind.RAW, machine, invalidAddress, null, "")
        }
        withProject(machine) { state, _ ->
            assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(state().project!!))
        }
    }

    private fun runBasicAndCall(machine: String, columns: Int, rows: Int, callAddress: Int) {
        val rom = System.getenv("PGP_${machine.replace("-", "").uppercase()}_TEST_ROM") ?: return
        val session = assertIs<DesktopRomLoadResult.Success>(
            if (rom.endsWith(".pgrom")) DesktopRomLoader.loadPackageForMachine(File(rom).readBytes(), MachineId(machine))
            else DesktopRomLoader.loadLegacyImage(MachineId(machine), File(rom).readBytes()),
        ).session
        val loadMode = if (machine in setOf("pc-1450", "pc-1460", "pc-1470u")) OperatingMode.PROGRAM else OperatingMode.RUN
        val requiresInitialization = machine in setOf("pc-1360", "pc-1360k", "pc-1460", "pc-1470u")
        if (requiresInitialization || loadMode == OperatingMode.PROGRAM) session.runCycles(1_000_000)
        if (requiresInitialization) {
            val definition = MachineCatalog.require(session.machineId)
            for (key in listOf(PocketKey.Y, PocketKey.ENTER, PocketKey.CLEAR)) {
                session.pressKey(key)
                session.runCycles(definition.automaticKeyHoldCycles)
                session.releaseKey(key)
                session.runCycles(definition.automaticKeyGapCycles)
            }
            session.runCycles(500_000)
        }
        session.setOperatingMode(loadMode)
        session.runCycles(1_000_000)
        if (machine == "pc-1350" && DisplaySymbol.RUN !in session.displaySnapshot().symbols) {
            session.pressKey(PocketKey.MODE)
            session.runCycles(64_000)
            session.releaseKey(PocketKey.MODE)
            session.runCycles(256_000)
        }
        if (loadMode == OperatingMode.PROGRAM) assertEquals(loadMode, session.currentOperatingMode())
        val runner = DesktopEmulatorRunner(session)
        val resultAddress = callAddress + 0x10
        val routine = GeneratedSource("call", ProjectSourceType.ASSEMBLY, """
            ORG $callAddress
            LIDP $resultAddress
            LIA 0x5A
            STD
            RTN
            ORG $resultAddress
            DB 0
        """.trimIndent() + "\n")
        withProject(machine, runner, listOf(routine)) { state, changes ->
            val workspace = checkNotNull(state().project)
            val registry = ProjectBuildRegistry(state, { changes.projectRevision(ignoreBusy = true) })
            val stamp = registry.begin(workspace)
            val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, runner.memoryLayout))
            assertTrue(registry.publish(stamp, built.artifact))
            val execution = ExecutionChangeService(state, registry, { changes.projectRevision() },
                onLoad = { _, target, artifact -> DesktopProjectArtifactLoader.load(target, artifact) },
                onContinue = { _, target -> target.run() })
            val load = execution.proposeLoad(SESSION, runner.sessionRevision, changes.projectRevision(), registry.requireCurrent().id)
            execution.approve()
            assertEquals(JsonPrimitive("SUCCEEDED"),
                execution.operation(load.getValue("operationId").jsonPrimitive.content)["status"])
            if (loadMode == OperatingMode.PROGRAM) {
                session.setOperatingMode(OperatingMode.RUN)
                session.runCycles(1_000_000)
                assertEquals(OperatingMode.RUN, session.currentOperatingMode())
            }
            val bounded = BoundedExecutionService(state, { changes.projectRevision() })
            val display = DisplayReadService(state)
            val adapter = PgpMcpToolAdapter({ state().debugService }, boundedProvider = { bounded }, displayProvider = { display })
            fun run(action: String, keys: List<String> = emptyList(), address: Int? = null) {
                val proposal = adapter.call(BoundedExecutionMcp.PROPOSE, buildJsonObject {
                    put("sessionId", SESSION); put("sessionRevision", runner.sessionRevision)
                    put("projectRevision", changes.projectRevision())
                    put("action", action); put("maxCycles", 2_000_000); put("maxInstructions", 1_000_000)
                    address?.let { put("callAddress", it) }
                    if (keys.isNotEmpty()) putJsonArray("keys") { keys.forEach { add(it) } }
                })
                assertEquals(JsonPrimitive(false), proposal["isError"], proposal.toString())
                val id = proposal.getValue("structuredContent").jsonObject.getValue("operationId").jsonPrimitive.content
                bounded.approve()
                repeat(500) { if (runner.state == RunnerState.RUNNING) runner.tick() }
                bounded.observe()
                val result = bounded.operation(id)
                assertEquals(JsonPrimitive("SUCCEEDED"), result["status"], result.toString())
                assertEquals(JsonPrimitive(true), result["inputComplete"], result.toString())
            }
            fun capture(name: String): JsonArray {
                val response = display.read(SESSION, runner.sessionRevision)
                val snapshot = response.getValue("structuredContent").jsonObject
                assertEquals(JsonPrimitive(machine), snapshot["machineId"])
                assertEquals(JsonPrimitive(columns), snapshot["characterColumns"])
                assertEquals(JsonPrimitive(rows), snapshot["characterRows"])
                val encoded = response.getValue("content").jsonArray[1].jsonObject.getValue("data").jsonPrimitive.content
                System.getenv("PGP_S1_TEST_LCD_DIR")?.let { File(it, "$machine-$name.png").writeBytes(Base64.getDecoder().decode(encoded)) }
                return snapshot.getValue("pixels").jsonArray
            }
            run("run")
            val prompt = capture("run")
            val outputs = listOf(1, 2, 3).map { value ->
                run("keys", listOf("NUM_$value", "ENTER"))
                capture("input-$value")
            }
            assertEquals(4, (outputs + listOf(prompt)).distinct().size)
            run("keys", listOf("NUM_0", "ENTER"))
            val finalDisplay = capture("end")
            if (rows == 4) {
                val populatedRows = finalDisplay.chunked(finalDisplay.size / rows).count { row ->
                    row.any { '1' in it.jsonPrimitive.content }
                }
                assertEquals(rows, populatedRows)
            }
            assertContentEquals(byteArrayOf(0), runner.memorySnapshot(resultAddress, 1).copyBytes())
            run("call", address = callAddress)
            capture("after-call")
            assertContentEquals(byteArrayOf(0x5a), runner.memorySnapshot(resultAddress, 1).copyBytes(), machine)
            assertTrue(runner.cpuSnapshot().programCounter !in callAddress until resultAddress)

            // Execute a second BASIC command to confirm the interpreter accepts input again after RTN.
            run("run")
            val restarted = capture("after-call-run")
            if (rows > 1) {
                // A multiline display retains earlier commands above the current prompt.
                fun lastVisibleRow(pixels: JsonArray) = pixels.chunked(pixels.size / rows)
                    .last { row -> row.any { '1' in it.jsonPrimitive.content } }
                assertEquals(lastVisibleRow(prompt), lastVisibleRow(restarted), machine)
            } else {
                assertEquals(prompt, restarted, machine)
            }

        }
    }

    private fun withProject(machine: String, runner: DesktopEmulatorRunner? = null,
        extraSources: List<GeneratedSource> = emptyList(),
        action: (() -> DevelopmentReadState, DevelopmentChangeService) -> Unit) {
        val root = Files.createTempDirectory("pgp-$machine-workflow").toFile()
        try {
            var state = DevelopmentReadState(runner = runner, debugService = runner?.let { DesktopDebugContextService(it, SESSION) })
            val changes = DevelopmentChangeService({ state }, onWorkspaceChanged = { state = state.copy(project = it) })
            val proposal = changes.proposeProject("${machine.uppercase()} Numeric Labels", machine,
                listOf(GeneratedSource("main", ProjectSourceType.BASIC, source(machine))) + extraSources)
            val id = proposal.getValue("operationId").jsonPrimitive.content
            changes.selectCreationParent(root)
            changes.approve()
            assertEquals(JsonPrimitive("APPLIED"), changes.operation(id)["status"])
            assertEquals(machine, state.project!!.definition.machineId.value)
            action({ state }, changes)
        } finally {
            root.deleteRecursively()
        }
    }

    private companion object {
        const val SESSION = "s1-workflow-test"
        fun source(machine: String): String = PocketKnowledgeService().example(machine, "${machine.replace("-", "")}-numeric-labels")
                .getValue("files").jsonArray.map { it.jsonObject }
                .single { it["path"] == JsonPrimitive("src/main.bas") }
                .getValue("text").jsonPrimitive.content
    }
}
