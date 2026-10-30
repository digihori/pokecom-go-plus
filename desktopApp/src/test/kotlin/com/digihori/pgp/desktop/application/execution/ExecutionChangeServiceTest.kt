package com.digihori.pgp.desktop.application.execution

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.desktop.application.debug.DesktopDebugContextService
import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.debug.DebuggerStopReason
import com.digihori.pgp.desktop.machine.*
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.runner.*
import java.nio.file.Files
import kotlin.test.*
import kotlinx.serialization.json.*

class ExecutionChangeServiceTest {
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun expect(code: String, action: () -> Unit) = assertEquals(code, assertFailsWith<DevelopmentReadError>(block = action).code)
    private inner class Fixture(basic: Boolean = false, manifestMachine: String = "pc-1245") : AutoCloseable {
        val root = Files.createTempDirectory("pgp-load-continue-").toFile()
        var now = 0L
        val runner: DesktopEmulatorRunner
        val workspace: DesktopProjectWorkspace
        var state: DevelopmentReadState
        val changes: DevelopmentChangeService
        val registry: ProjectBuildRegistry
        val service: ExecutionChangeService
        init {
            root.resolve("main.asm").writeText("ORG 0xC500\nENTRY: LIA 0x41\nRTN\n")
            root.resolve("main.bas").writeText("10 END\n")
            val manifest = root.resolve("pgp-project.json")
            manifest.writeText("""{"format":"pgp-project","formatVersion":1,"name":"Load test","machineId":"$manifestMachine","sources":[{"id":"main","type":"assembly","path":"main.asm"}${if (basic) ",{\"id\":\"basic\",\"type\":\"basic\",\"path\":\"main.bas\"}" else ""}]}""")
            workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace
            val rom = RomSet(Pc1245RomDefinition.MACHINE_ID, listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000).also {
                    byteArrayOf(0x02, 0x50, 0x32, 0x78, 0xc5.toByte(), 0, 0x79, 0, 6).copyInto(it)
                }), RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000))))
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, rom)).session
            runner = DesktopEmulatorRunner(session, MonotonicClock { now })
            state = DevelopmentReadState(runner = runner, debugService = DesktopDebugContextService(runner, "load-session"), project = workspace)
            changes = DevelopmentChangeService({ state })
            registry = ProjectBuildRegistry({ state }, { changes.projectRevision(ignoreBusy = true) })
            service = ExecutionChangeService({ state }, registry, { changes.projectRevision() },
                onLoad = { _, target, artifact -> DesktopProjectArtifactLoader.load(target, artifact) }, onContinue = { _, active -> active.run() })
        }
        fun build(): RegisteredProjectBuild {
            val stamp = registry.begin(workspace)
            val assembly = DesktopAssemblyWorkspaceCompiler.compile(workspace, runner.memoryLayout, validatePlacement = false)
            val result = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace, runner.memoryLayout, compiledAssembly = assembly))
            DesktopProjectBuildOutputWriter.write(workspace, result.artifact)
            assertTrue(registry.publish(stamp, result.artifact))
            return registry.requireCurrent()
        }
        fun load(build: RegisteredProjectBuild = registry.requireCurrent()) = service.proposeLoad("load-session", runner.sessionRevision, changes.projectRevision(), build.id)
        fun continueRequest() = service.proposeContinue("load-session", runner.sessionRevision, changes.projectRevision())
        fun operation(request: JsonObject) = service.operation(request.string("operationId"))
        fun pointers() {
            val image = assertIs<DesktopMemoryDumpLoadResult.Success>(DesktopMemoryDumpLoader.parse("C6E1:00C020C0\n".encodeToByteArray())).image
            assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
        }
        override fun close() { root.deleteRecursively() }
    }

    @Test fun manifestMachineDoesNotRestrictApprovedMcpBuildAndLoad() {
        Fixture(manifestMachine = "pc-1261").use { f ->
            val built = f.build()
            assertEquals("pc-1245", built.artifact.targetMachineId!!.value)
            assertEquals("pc-1245", f.registry.metadata().string("machineId"))
            val request = f.load(built)
            f.service.approve()
            assertEquals("SUCCEEDED", f.operation(request).string("status"))
            assertContentEquals(byteArrayOf(2, 0x41, 0x37), f.runner.memorySnapshot(0xc500, 3).copyBytes())
        }
    }

    @Test fun changingExecutionMachineInvalidatesPreviouslyBuiltArtifact() {
        Fixture().use { f ->
            val built = f.build()
            val id = MachineId("pc-1261")
            val rom = RomSet(id, listOf(
                RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(0x8000))))
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
            val other = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            f.state = f.state.copy(runner = other, debugService = DesktopDebugContextService(other, "other-session"))
            expect("STALE_BUILD") { f.registry.requireCurrent(built.id) }
            assertEquals("false", f.registry.metadata().getValue("available").jsonPrimitive.content)
        }
    }

    @Test fun approvedLoadUsesExactInMemoryArtifactStaysPausedAndContinueUsesCurrentPc() {
        Fixture().use { f ->
            val built = f.build()
            f.root.resolve("build/main.dmp").writeText("C500 FF FF FF\n") // Untrusted output edits are never reimported.
            val before = f.runner.memorySnapshot(0xc500, 3).copyBytes()
            val revision = f.runner.sessionRevision
            val load = f.load(built)
            assertEquals("PENDING_APPROVAL", load.string("status"))
            assertEquals(listOf(ExecutionWriteRange(0xc500, 3)), f.service.review()!!.writes)
            assertContentEquals(before, f.runner.memorySnapshot(0xc500, 3).copyBytes()); assertEquals(revision, f.runner.sessionRevision)
            f.service.approve()
            assertEquals("SUCCEEDED", f.operation(load).string("status"))
            assertContentEquals(byteArrayOf(2, 0x41, 0x37), f.runner.memorySnapshot(0xc500, 3).copyBytes())
            assertEquals(RunnerState.PAUSED, f.runner.state); assertEquals(0, f.runner.cpuSnapshot().programCounter)
            assertNotNull(f.runner.sourceMap)
            f.runner.saveBreakpoint(null, 0xc500)
            val resume = f.continueRequest()
            assertEquals(RunnerState.PAUSED, f.runner.state)
            f.service.approve()
            assertEquals("SUCCEEDED", f.operation(resume).string("status"))
            assertEquals("executionStartedNotProgramFinished", f.operation(resume).string("successMeaning"))
            assertEquals(RunnerState.RUNNING, f.runner.state); assertEquals(0, f.runner.cpuSnapshot().programCounter)
            f.now += 100_000_000; f.runner.tick()
            assertEquals(RunnerState.PAUSED, f.runner.state)
            assertEquals(0xc500, f.runner.cpuSnapshot().programCounter)
            assertIs<DebuggerStopReason.Breakpoint>(f.runner.stopReason)
            f.runner.step(); assertEquals(0x41, f.runner.cpuSnapshot().copyInternalRam()[2].toInt() and 255)
            f.runner.step(); assertEquals(6, f.runner.cpuSnapshot().programCounter)
        }
    }

    @Test fun basicPlanIncludesPointersAndOldProgramClearingBeforeAnyWrite() {
        Fixture(basic = true).use { f ->
            val built = f.build()
            val revision = f.runner.sessionRevision
            expect("BASIC_LOAD_FAILED") { f.load(built) } // Synthetic ROM has no initialized BASIC pointers yet.
            assertEquals(revision, f.runner.sessionRevision); assertNull(f.service.review())
            f.pointers()
            val before = f.runner.memorySnapshot(0xc000, 0x21).copyBytes()
            val request = f.load(built)
            assertTrue(f.service.review()!!.writes.contains(ExecutionWriteRange(0xc000, 0x21)))
            assertTrue(f.service.review()!!.writes.contains(ExecutionWriteRange(0xc6e1, 4)))
            assertContentEquals(before, f.runner.memorySnapshot(0xc000, 0x21).copyBytes())
            f.service.approve()
            assertEquals("SUCCEEDED", f.operation(request).string("status"))
            assertIs<BasicProgramSnapshotResult.Success>(f.runner.basicProgramSnapshot())
            assertEquals(RunnerState.PAUSED, f.runner.state)
        }
    }

    @Test fun changedSourceRebuiltArtifactAndFailedBuildCannotLoadOldResults() {
        Fixture().use { f ->
            val original = f.root.resolve("main.asm").readText()
            val build = f.build()
            val sourceChanged = f.load(build)
            f.root.resolve("main.asm").appendText("; edited\n")
            f.service.approve()
            assertEquals("STALE_PROJECT", f.operation(sourceChanged).string("code"))
            assertFalse(f.service.artifact()["available"]!!.jsonPrimitive.boolean)
            f.root.resolve("main.asm").writeText(original)
            val replaced = f.load(build)
            f.build()
            f.service.approve()
            assertEquals("STALE_BUILD", f.operation(replaced).string("code"))
            assertContentEquals(byteArrayOf(0, 0, 0), f.runner.memorySnapshot(0xc500, 3).copyBytes())
            f.registry.begin(f.workspace) // Every attempted Build invalidates the previous artifact, even on failure.
            expect("NO_BUILD") { f.registry.requireCurrent() }
            assertFalse(f.service.artifact()["available"]!!.jsonPrimitive.boolean)
        }
    }

    @Test fun staleSessionRevisionBusyRunningAndRejectionDoNotLoadOrResume() {
        Fixture().use { f ->
            f.build()
            val rejected = f.load(); f.service.reject()
            assertEquals("REJECTED", f.operation(rejected).string("status")); assertEquals(RunnerState.PAUSED, f.runner.state)
            val stale = f.load(); f.runner.step(); f.service.approve()
            assertEquals("STALE_REVISION", f.operation(stale).string("code"))
            val resume = f.continueRequest()
            f.state = f.state.copy(busy = true); f.service.approve()
            assertEquals("BUSY", f.operation(resume).string("code")); assertEquals(RunnerState.PAUSED, f.runner.state)
            f.state = f.state.copy(busy = false)
            expect("STALE_SESSION") { f.service.proposeContinue("wrong", f.runner.sessionRevision, f.changes.projectRevision()) }
            f.runner.run()
            expect("SESSION_NOT_PAUSED") { f.continueRequest() }
            assertContentEquals(byteArrayOf(0, 0, 0), f.runner.memorySnapshot(0xc500, 3).copyBytes())
        }
    }

    @Test fun buildAndLoadPreflightRejectSourceRacesAndReadOnlyPlacement() {
        Fixture().use { f ->
            val stamp = f.registry.begin(f.workspace)
            f.root.resolve("main.asm").writeText("ORG 0\nRTN\n")
            val artifact = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(f.workspace, compiledAssembly = DesktopAssemblyWorkspaceCompiler.compile(f.workspace, validatePlacement = false))).artifact
            assertFalse(f.registry.publish(stamp, artifact))
            val built = f.build() // Build-only permits disassembly of ROM-targeted output; Load must reject it.
            val revision = f.runner.sessionRevision
            expect("MEMORY_LOAD_FAILED") { f.load(built) }
            assertEquals(revision, f.runner.sessionRevision); assertNull(f.service.review())
        }
    }

    @Test fun failedLoadAttemptAndDifferentRunnerInvalidatePendingContinueOrLoad() {
        Fixture().use { f ->
            assertFalse(f.service.artifact()["available"]!!.jsonPrimitive.boolean)
            f.build()
            val pending = f.continueRequest()
            val revision = f.runner.sessionRevision
            assertIs<ProgramImageLoadResult.BasicFailure>(f.runner.loadProgramImage(byteArrayOf(0), null))
            assertEquals(revision, f.runner.sessionRevision)
            f.service.approve()
            assertEquals("STALE_SESSION", f.operation(pending).string("code"))
            assertEquals(RunnerState.PAUSED, f.runner.state)
            Fixture().use { other ->
                val load = f.load()
                f.state = f.state.copy(runner = other.runner, debugService = DesktopDebugContextService(other.runner, "load-session"))
                f.service.approve()
                assertEquals("STALE_SESSION", f.operation(load).string("code"))
                assertContentEquals(byteArrayOf(0, 0, 0), other.runner.memorySnapshot(0xc500, 3).copyBytes())
            }
        }
    }

    @Test fun authenticatedHttpLoadReviewPublishesOutcomeAndFreshMemory() {
        Fixture().use { f ->
            f.build()
            val directory = Files.createTempDirectory("pgp-execution-http-")
            val server = DesktopMcpServer(PgpMcpToolAdapter({ f.state.debugService }, AwtMcpApplicationDispatcher, executionProvider = { f.service }),
                token = "execution-test-token", credentialStore = object : DesktopMcpCredentialStore {
                    override fun publish(token: String) = DesktopMcpCredentialLocation(directory.resolve("headers.json"), "unused-test-helper")
                    override fun removeIfOwned(token: String) { }
                })
            try {
                val info = assertIs<DesktopMcpStartResult.Success>(server.start(0)).info
                val client = java.net.http.HttpClient.newHttpClient()
                fun call(name: String, args: JsonObject): JsonObject {
                    val body = buildJsonObject { put("jsonrpc", "2.0"); put("id", 1); put("method", "tools/call")
                        put("params", buildJsonObject { put("name", name); put("arguments", args) }) }
                    val request = java.net.http.HttpRequest.newBuilder(java.net.URI(info.url))
                        .header("Authorization", "Bearer execution-test-token").header("Content-Type", "application/json")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString())).build()
                    val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    assertEquals(200, response.statusCode())
                    return Json.parseToJsonElement(response.body()).jsonObject["result"]!!.jsonObject["structuredContent"]!!.jsonObject
                }
                val artifact = call(ExecutionChangeMcp.ARTIFACT, buildJsonObject {})
                val queued = call(ExecutionChangeMcp.LOAD, buildJsonObject {
                    put("sessionId", "load-session"); put("sessionRevision", f.runner.sessionRevision)
                    put("projectRevision", artifact.string("projectRevision")); put("artifactId", artifact.string("artifactId"))
                })
                assertEquals("PENDING_APPROVAL", queued.string("status")); assertEquals(0, f.runner.memoryByte(0xc500))
                javax.swing.SwingUtilities.invokeAndWait { f.service.approve() }
                assertEquals("SUCCEEDED", call(ExecutionChangeMcp.OPERATION, buildJsonObject { put("operationId", queued.string("operationId")) }).string("status"))
                val context = call(PgpMcpToolAdapter.GET_DEBUG_CONTEXT, buildJsonObject {
                    put("memoryRanges", buildJsonArray { add(buildJsonObject { put("startAddress", 0xc500); put("length", 3) }) })
                })
                assertEquals("024137", context["memory"]!!.jsonArray.single().jsonObject.string("data"))
                assertEquals("paused", context["session"]!!.jsonObject.string("runState"))
            } finally { server.close(); directory.toFile().deleteRecursively() }
        }
    }

    @Test fun mcpTypesAndNoApprovalToolAreEnforced() {
        Fixture().use { f ->
            f.build()
            val adapter = PgpMcpToolAdapter({ f.state.debugService }, executionProvider = { f.service })
            val metadata = adapter.call(ExecutionChangeMcp.ARTIFACT, buildJsonObject {})["structuredContent"]!!.jsonObject
            assertTrue(metadata["available"]!!.jsonPrimitive.boolean)
            val result = adapter.call(ExecutionChangeMcp.LOAD, buildJsonObject {
                put("sessionId", "load-session"); put("sessionRevision", "0"); put("projectRevision", f.changes.projectRevision()); put("artifactId", metadata.string("artifactId"))
            })
            assertEquals("INVALID_ARGUMENTS", result["structuredContent"]!!.jsonObject["error"]!!.jsonObject.string("code"))
            assertTrue(adapter.call("pgp_approve_load", buildJsonObject {})["isError"]!!.jsonPrimitive.boolean)
            assertNull(f.service.review())
        }
    }
}
