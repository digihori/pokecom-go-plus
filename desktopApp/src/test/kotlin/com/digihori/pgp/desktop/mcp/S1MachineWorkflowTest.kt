package com.digihori.pgp.desktop.mcp

import com.digihori.pgp.core.api.DisplaySymbol
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

class S1MachineWorkflowTest {
    @Test fun pc1261CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1261", 0x4000, 0x67ff)
    @Test fun pc1350CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1350", 0x6000, 0x6fff)

    @Test fun pc1401CatalogGenerationAndAnalysis() = catalogGenerationAndAnalysis("pc-1401", 0x4400, 0x47ff)

    @Test fun pc1261LocallySuppliedRomRunsGeneratedBasic() = runBasic("pc-1261", 24, 2)
    @Test fun pc1350LocallySuppliedRomRunsGeneratedBasic() = runBasic("pc-1350", 25, 4)

    @Test fun pc1401LocallySuppliedRomRunsGeneratedBasic() = runBasic("pc-1401", 16, 1)

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

    private fun runBasic(machine: String, columns: Int, rows: Int) {
        val rom = System.getenv("PGP_${machine.replace("-", "").uppercase()}_TEST_ROM") ?: return
        val session = assertIs<DesktopRomLoadResult.Success>(
            DesktopRomLoader.loadLegacyImage(MachineId(machine), File(rom).readBytes()),
        ).session
        session.setOperatingMode(OperatingMode.RUN)
        session.runCycles(1_000_000)
        if (machine == "pc-1350" && DisplaySymbol.RUN !in session.displaySnapshot().symbols) {
            session.pressKey(PocketKey.MODE)
            session.runCycles(64_000)
            session.releaseKey(PocketKey.MODE)
            session.runCycles(256_000)
        }
        val runner = DesktopEmulatorRunner(session)
        withProject(machine, runner) { state, changes ->
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
            val bounded = BoundedExecutionService(state, { changes.projectRevision() })
            val display = DisplayReadService(state)
            val adapter = PgpMcpToolAdapter({ state().debugService }, boundedProvider = { bounded }, displayProvider = { display })
            fun run(action: String, keys: List<String> = emptyList()) {
                val proposal = adapter.call(BoundedExecutionMcp.PROPOSE, buildJsonObject {
                    put("sessionId", SESSION); put("sessionRevision", runner.sessionRevision)
                    put("projectRevision", changes.projectRevision())
                    put("action", action); put("maxCycles", 2_000_000); put("maxInstructions", 1_000_000)
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
            if (machine == "pc-1350") {
                val populatedRows = finalDisplay.chunked(finalDisplay.size / rows).count { row ->
                    row.any { '1' in it.jsonPrimitive.content }
                }
                assertEquals(rows, populatedRows)
            }
        }
    }

    private fun withProject(machine: String, runner: DesktopEmulatorRunner? = null,
        action: (() -> DevelopmentReadState, DevelopmentChangeService) -> Unit) {
        val root = Files.createTempDirectory("pgp-$machine-workflow").toFile()
        try {
            var state = DevelopmentReadState(runner = runner, debugService = runner?.let { DesktopDebugContextService(it, SESSION) })
            val changes = DevelopmentChangeService({ state }, onWorkspaceChanged = { state = state.copy(project = it) })
            val proposal = changes.proposeProject("${machine.uppercase()} Numeric Labels", machine,
                listOf(GeneratedSource("main", ProjectSourceType.BASIC, source(machine))))
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
