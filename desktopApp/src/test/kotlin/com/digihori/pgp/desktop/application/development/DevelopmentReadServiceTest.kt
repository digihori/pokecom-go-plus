package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.debug.Sc61860InstructionSet
import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.debug.*
import com.digihori.pgp.desktop.mcp.PgpMcpToolAdapter
import java.nio.file.Files
import kotlinx.serialization.json.*
import kotlin.test.*

class DevelopmentReadServiceTest {
    @Test fun everyMachineUsesCoreDefinitionsWithoutNeedingARomOrMutatingTheSession() {
        val service = DesktopDevelopmentReadService { DevelopmentReadState() }
        for (definition in MachineCatalog.definitions) {
            val value = service.machineSpec(definition.id.value)
            assertEquals(definition.cyclesPerSecond, value.getValue("cyclesPerSecond").jsonPrimitive.long)
            assertEquals(definition.memoryRegions.size, value.getValue("memoryRegions").jsonArray.size)
            assertEquals(definition.characterColumns, value.getValue("display").jsonObject.getValue("characterColumns").jsonPrimitive.int)
            val instructions = value.getValue("instructions").jsonArray
            assertEquals((0..255).count { Sc61860InstructionSet.find(it) != null }, instructions.size)
            for (item in instructions) {
                val row = item.jsonObject
                val spec = Sc61860InstructionSet.find(row.getValue("opcode").jsonPrimitive.int)!!
                assertEquals(spec.length, row.getValue("length").jsonPrimitive.int)
                assertEquals(spec.flow.name, row.getValue("flow").jsonPrimitive.content)
            }
        }
        error("NO_ACTIVE_SESSION") { service.machineSpec(null) }
        error("UNSUPPORTED_MACHINE") { service.machineSpec("pc-unknown") }
        val runner = debuggerTestRunner()
        val active = DesktopDevelopmentReadService { DevelopmentReadState(runner) }
        val revision = runner.sessionRevision
        assertEquals("pc-1245", active.machineSpec(null).getValue("machineId").jsonPrimitive.content)
        runner.run()
        assertEquals("activeSession", active.machineSpec(null).getValue("memoryLayout").jsonObject.getValue("scope").jsonPrimitive.content)
        assertEquals(revision + 1, runner.sessionRevision)
    }

    @Test fun scopedProjectMetadataSymbolsAndLoadedLocationNeverExposeRawSourceCommandsOrPaths() = project { workspace ->
        val runner = debuggerTestRunner(byteArrayOf(0x79, 0xc5.toByte(), 0))
        val assembly = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).sources
        val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        assertIs<DesktopProjectApplyResult.Success>(DesktopProjectArtifactLoader.load(runner, built.artifact))
        runner.step() // Synthetic ROM jumps to the loaded user assembly at C500.
        val debug = DesktopDebugContextService(runner, "development-test")
        var state = DevelopmentReadState(runner, debug, workspace,
            DesktopAssemblerSettings(external = true, executable = "/private/secret/tool", arguments = "--token SECRET"), assembly = assembly)
        val service = DesktopDevelopmentReadService { state }
        val beforeRevision = runner.sessionRevision
        val meta = service.context("metadata", null, 0, 1)
        assertEquals("main", meta.getValue("sources").jsonArray.single().jsonObject.getValue("id").jsonPrimitive.content)
        assertEquals("development-test", meta.getValue("session").jsonObject.getValue("id").jsonPrimitive.content)
        assertFalse(meta.toString().contains("SECRET")); assertFalse(meta.toString().contains(workspace.manifestFile.parent))
        assertFalse(meta.toString().contains("/private/secret"))
        val revision = meta.getValue("metadataRevision")
        assertEquals(revision, service.context("metadata", null, 0, 1).getValue("metadataRevision"))
        state = state.copy(buildStatus = "BUILT_NOT_LOADED")
        assertNotEquals(revision, service.context("metadata", null, 0, 1).getValue("metadataRevision"))
        val value = service.context("assembly", "main", 0, 1)
        assertEquals("current", value.getValue("freshness").jsonPrimitive.content)
        assertEquals("matchedLoadedBuild", value.getValue("loadedLocation").jsonObject.getValue("status").jsonPrimitive.content)
        assertEquals("ENTRY", value.getValue("symbols").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
        val row = value.getValue("listing").jsonArray.single().jsonObject
        assertEquals("main.asm", row.getValue("path").jsonPrimitive.content)
        assertEquals(3, row.getValue("line").jsonPrimitive.int)
        assertEquals(1, value.getValue("nextListingOffset").jsonPrimitive.int)
        assertFalse(value.toString().contains("PRIVATE_COMMENT"))
        assertFalse(value.toString().contains("sourceText\"")); assertFalse(value.toString().contains("consoleLog"))
        assertEquals(beforeRevision, runner.sessionRevision)
        workspace.sources.single().file.appendText("; changed\n")
        assertEquals("staleOrMissing", service.context("assembly", "main", 0, 32).getValue("freshness").jsonPrimitive.content)
    }

    @Test fun diagnosticsIncludeBuildErrorsButOmitExternalLogsAndProjectExternalContent() = project { workspace ->
        val outside = Files.createTempFile("pgp-external-diagnostic-", ".asm").toFile()
        try {
            var state = DevelopmentReadState(project = workspace, assembler = DesktopAssemblerSettings(external = true), diagnostics = listOf(
                DesktopAssemblyDiagnostic(workspace.sources.single(), 3, "SECRET /private/credentials", "SECRET", outside, "TOKEN=SECRET")))
            val service = DesktopDevelopmentReadService { state }
            val value = service.context("diagnostics", null, 0, 32)
            assertFalse(value.toString().contains("SECRET")); assertFalse(value.toString().contains(outside.path))
            assertEquals(JsonNull, value.getValue("diagnostics").jsonArray.single().jsonObject.getValue("path"))
            state = state.copy(assembler = DesktopAssemblerSettings(), diagnostics = listOf(
                DesktopAssemblyDiagnostic(workspace.sources.single(), 3, "SECRET external command", null,
                    consoleLog = "", externalTool = true)))
            assertFalse(service.context("diagnostics", null, 0, 32).toString().contains("SECRET"))
            state = state.copy(diagnostics = emptyList(), buildErrors = listOf(DesktopProjectBuildError.MemoryOverlap("main", "other", 0xc500)))
            val error = service.context("diagnostics", null, 0, 32).getValue("diagnostics").jsonArray.single().jsonObject
            assertEquals("MEMORY_OVERLAP", error.getValue("code").jsonPrimitive.content)
            assertEquals(0xc500, error.getValue("address").jsonPrimitive.int)
            state = state.copy(buildStatus = "BUILD_FAILED", buildErrors = emptyList())
            assertFalse(service.context("diagnostics", null, 0, 32).getValue("buildFailureDetailsAvailable").jsonPrimitive.boolean)
        } finally { outside.delete() }
    }

    @Test fun debugSectionIncludesConditionalRulesAndSteppingWithoutChangingLegacySchemasOrExecution() {
        val runner = debuggerTestRunner()
        runner.saveBreakpoint(null, 7, condition = "A == 1", hitCondition = "% 3")
        runner.saveWatch(null, 0xc000, 0xc000, DebuggerWatchKind.CHANGE, false)
        val debug = DesktopDebugContextService(runner, "debug-read", { "fixed" })
        val service = DesktopDevelopmentReadService { DevelopmentReadState(runner, debug) }
        val before = DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(debug.capture(DebugContextRequest())).context)
        val revision = runner.sessionRevision
        val value = service.context("debug", null, 0, 32)
        val point = value.getValue("breakpoints").jsonArray.single().jsonObject
        assertEquals("A == 1", point.getValue("condition").jsonPrimitive.content)
        assertEquals("% 3", point.getValue("hitCondition").jsonPrimitive.content)
        assertEquals(0, point.getValue("hitCount").jsonPrimitive.int)
        assertEquals("IDLE", value.getValue("step").jsonObject.getValue("status").jsonPrimitive.content)
        val legacy = assertIs<DebugRegistrationsResult.Success>(debug.registrations()).snapshot.toJson()
        assertEquals(setOf("id", "address", "enabled"), legacy.getValue("breakpoints").jsonArray.single().jsonObject.keys)
        assertEquals(revision, runner.sessionRevision)
        assertContentEquals(before, DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(debug.capture(DebugContextRequest())).context))
        runner.run()
        val runningRevision = runner.sessionRevision
        error("SESSION_RUNNING") { service.context("debug", null, 0, 32) }
        assertEquals(runningRevision, runner.sessionRevision)
        assertEquals("RUNNING", runner.state.name)
    }

    @Test fun staleBytesSymlinkEscapeEncodingAndInspectionLimitsCannotProduceConfirmedMappings() = project { workspace ->
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc5.toByte(), 0, 2, 0x33, 0x52, 0x79, 0xc5.toByte(), 0))
        val built = assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace))
        assertIs<DesktopProjectApplyResult.Success>(DesktopProjectArtifactLoader.load(runner, built.artifact))
        repeat(4) { runner.step() }
        var state = DevelopmentReadState(runner, DesktopDebugContextService(runner), workspace, assembly = built.artifact.assemblySources)
        val service = DesktopDevelopmentReadService { state }
        assertEquals("unmappedOrBytesOrPhysicalLocationMismatch", service.context("assembly", "main", 0, 32)
            .getValue("loadedLocation").jsonObject.getValue("status").jsonPrimitive.content)
        workspace.sources.single().file.writeBytes(ByteArray(2 * 1024 * 1024 + 1))
        assertEquals("inspectionLimit", service.context("assembly", "main", 0, 32).getValue("freshness").jsonPrimitive.content)
        val outside = Files.createTempFile("pgp-development-outside-", ".asm").toFile()
        try {
            outside.writeText("DO_NOT_EXPORT")
            workspace.sources.single().file.delete()
            Files.createSymbolicLink(workspace.sources.single().file.toPath(), outside.toPath())
            error("OUTSIDE_PROJECT") { service.context("assembly", "main", 0, 32) }
            val metadata = service.context("metadata", null, 0, 32)
            assertEquals(JsonNull, metadata.getValue("sources").jsonArray.single().jsonObject.getValue("path"))
            assertFalse(metadata.toString().contains(outside.path))
            workspace.sources.single().file.delete()
            workspace.sources.single().file.writeText("ORG 0xC500\nRTN\n")
            state = state.copy(assembly = listOf(built.artifact.assemblySources.single().copy(
                sourceSnapshots = mapOf(workspace.sources.single().file to "\uFFFD"))))
            assertEquals("encodingUnverified", service.context("assembly", "main", 0, 32).getValue("freshness").jsonPrimitive.content)
        } finally { outside.delete() }
    }

    @Test fun responseSizeLimitRequestsSmallerPagesRatherThanReturningUnboundedOutput() = project { workspace ->
        val compiled = assertIs<DesktopAssemblyWorkspaceResult.Success>(DesktopAssemblyWorkspaceCompiler.compile(workspace)).sources.single()
        val deep = workspace.manifestFile.parentFile.resolve("p".repeat(240)).resolve("q".repeat(240)).resolve("main.asm")
        val source = workspace.sources.single().copy(file = deep)
        val project = workspace.copy(sources = listOf(source))
        val assembled = compiled.copy(source = source, sourceSnapshots = emptyMap(),
            symbols = List(128) { com.digihori.pgp.core.debug.Sc61860AssemblySymbol("N".repeat(128), it, 3) },
            listing = List(128) { com.digihori.pgp.core.debug.Sc61860AssemblyListingLine(3, it, listOf(0), "not exported") })
        val service = DesktopDevelopmentReadService { DevelopmentReadState(project = project, assembly = listOf(assembled)) }
        error("RESPONSE_TOO_LARGE") { service.context("assembly", "main", 0, 128) }
        assertEquals(32, service.context("assembly", "main", 0, 32).getValue("listing").jsonArray.size)
    }

    @Test fun strictMcpArgumentsDispatchAndPaginationAreBoundedAndDoNotReadArbitraryPaths() {
        var dispatches = 0
        val reader = DesktopDevelopmentReadService { DevelopmentReadState() }
        val adapter = PgpMcpToolAdapter({ null }, applicationDispatcher = com.digihori.pgp.desktop.mcp.McpApplicationDispatcher { dispatches++; it() }, developmentProvider = { reader })
        fun code(args: JsonObject) = adapter.call(PgpMcpToolAdapter.GET_DEVELOPMENT_CONTEXT, args).getValue("structuredContent").jsonObject.getValue("error").jsonObject.getValue("code").jsonPrimitive.content
        for (args in listOf(buildJsonObject { put("path", "/private/credentials") }, buildJsonObject { put("limit", "1") },
            buildJsonObject { put("limit", 0) }, buildJsonObject { put("limit", 129) }, buildJsonObject { put("offset", -1) },
            buildJsonObject { put("section", "assembly") }, buildJsonObject { put("sourceId", "main") }, buildJsonObject { put("section", JsonNull) })) {
            assertEquals("INVALID_ARGUMENTS", code(args))
        }
        val metadata = adapter.call(PgpMcpToolAdapter.GET_DEVELOPMENT_CONTEXT, buildJsonObject {}).getValue("structuredContent").jsonObject
        assertEquals(JsonNull, metadata.getValue("project")); assertEquals(0, metadata.getValue("total").jsonPrimitive.int)
        assertTrue(dispatches >= 9)
        error("SOURCE_NOT_FOUND") { reader.context("assembly", "../../secret", 0, 32) }
    }

    private fun error(code: String, body: () -> Unit) { assertEquals(code, assertFailsWith<DevelopmentReadError>(block = body).code) }
    private fun project(body: (DesktopProjectWorkspace) -> Unit) {
        val root = Files.createTempDirectory("pgp-development-").toFile()
        try {
            root.resolve("main.asm").writeText("# PRIVATE_COMMENT\nORG 0xC500\nENTRY: LIA 7\nRTN\n")
            val manifest = root.resolve("pgp-project.json").apply { writeText("""{
                "format":"pgp-project", "formatVersion":1, "name":"Test", "machineId":"pc-1245",
                "sources":[{"id":"main","type":"assembly","path":"main.asm"}]
            }""") }
            body(assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace)
        } finally { root.deleteRecursively() }
    }
}
