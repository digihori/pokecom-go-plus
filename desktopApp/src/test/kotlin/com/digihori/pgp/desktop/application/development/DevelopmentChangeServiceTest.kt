package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.desktop.project.*
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import java.nio.file.Files
import kotlin.test.*
import kotlinx.serialization.json.*

class DevelopmentChangeServiceTest {
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun expect(code: String, block: () -> Unit) { assertEquals(code, assertFailsWith<DevelopmentReadError>(block = block).code) }

    private fun project(block: (DesktopProjectWorkspace) -> Unit) {
        val root = Files.createTempDirectory("pgp-source-review-").toFile()
        try {
            root.resolve("main.asm").writeText("ORG 0xC500\nENTRY: LIA 1\nRTN\n")
            val manifest = root.resolve("pgp-project.json")
            manifest.writeText("""{"format":"pgp-project","formatVersion":1,"name":"Review test","machineId":"pc-1245","sources":[{"id":"main","type":"assembly","path":"main.asm"}]}""")
            block(assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(manifest)).workspace)
        } finally { root.deleteRecursively() }
    }
    private fun propose(service: DevelopmentChangeService, old: String = "LIA 1", new: String = "LIA 2"): JsonObject {
        val source = service.source("main")
        return service.propose("main", source.string("projectRevision"), source.string("sourceSha256"), listOf(SourceReplacement(old, new)))
    }

    @Test fun proposalsDoNotWriteAndOnlyLocalApprovalAppliesAllEditsAtomically() = project { workspace ->
        val file = workspace.sources.single().file
        var applied = 0
        val service = DevelopmentChangeService({ DevelopmentReadState(project = workspace) }, onApplied = { applied++ })
        val before = file.readText()
        val revision = service.source("main").string("projectRevision")
        val proposal = propose(service)
        assertEquals("PENDING_APPROVAL", proposal.string("status")); assertEquals(before, file.readText())
        assertEquals("main.asm", service.review()!!.sourcePath)
        service.approve()
        assertEquals("APPLIED", service.operation(proposal.string("operationId")).string("status"))
        assertEquals(before.replace("LIA 1", "LIA 2"), file.readText()); assertEquals(1, applied)
        assertNotEquals(revision, service.source("main").string("projectRevision"))
        val rejected = propose(service, "LIA 2", "LIA 3")
        service.reject(); assertEquals("REJECTED", service.operation(rejected.string("operationId")).string("status"))
        assertTrue(file.readText().contains("LIA 2"))
        expect("AMBIGUOUS_PATCH") { propose(service, "R", "X") }
    }

    @Test fun staleRevisionsProjectChangesAndExternalSettingsCannotBeApproved() = project { workspace ->
        var state = DevelopmentReadState(project = workspace)
        val service = DevelopmentChangeService({ state })
        val file = workspace.sources.single().file
        val pending = propose(service)
        file.appendText("; user edit\n")
        service.approve()
        assertEquals("STALE_PROJECT", service.operation(pending.string("operationId")).string("code"))
        assertTrue(file.readText().contains("LIA 1"))
        val source = service.source("main")
        file.appendText("; more edits\n")
        expect("STALE_PROJECT") { service.proposeBuild(source.string("projectRevision")) }
        val switched = propose(service)
        state = state.copy(project = workspace.copy())
        service.approve()
        assertEquals("FAILED", service.operation(switched.string("operationId")).string("status"))
        state = state.copy(assembler = DesktopAssemblerSettings(external = true))
        expect("BUILTIN_ONLY") { service.source("main") }
        state = state.copy(assembler = DesktopAssemblerSettings(), busy = true)
        expect("BUSY") { service.source("main") }
    }

    @Test fun buildDiagnosticsFixAndRebuildUseCommonCompilerWithoutLoadingTheEmulator() = project { workspace ->
        val runner = debuggerTestRunner()
        val before = runner.memorySnapshot(0xc500, 8).copyBytes()
        val cpu = runner.cpuSnapshot().programCounter
        var state = DevelopmentReadState(runner = runner, project = workspace)
        lateinit var service: DevelopmentChangeService
        service = DevelopmentChangeService({ state }, onBuild = { id ->
            val assembly = DesktopAssemblyWorkspaceCompiler.compile(workspace)
            val result = DesktopProjectBuilder.build(workspace, compiledAssembly = assembly)
            state = state.copy(
                diagnostics = (assembly as? DesktopAssemblyWorkspaceResult.Failure)?.diagnostics.orEmpty(),
                buildErrors = (result as? DesktopProjectBuildResult.Failure)?.errors.orEmpty(),
                assembly = (assembly as? DesktopAssemblyWorkspaceResult.Success)?.sources.orEmpty(),
                buildStatus = if (result is DesktopProjectBuildResult.Success) "BUILT_NOT_LOADED" else "BUILD_FAILED")
            service.completeBuild(id, result is DesktopProjectBuildResult.Success, state.buildErrors, state.diagnostics)
        })
        val adapter = PgpMcpToolAdapter({ null }, changesProvider = { service })
        fun call(name: String, args: JsonObject): JsonObject {
            val response = adapter.call(name, args)
            assertEquals(JsonPrimitive(false), response["isError"], response.toString())
            return response.getValue("structuredContent").jsonObject
        }
        fun poll(id: String) = call("pgp_get_operation", buildJsonObject { put("operationId", id) })
        fun patch(vararg edits: Pair<String, String>): JsonObject {
            val source = call("pgp_get_source", buildJsonObject { put("sourceId", "main") })
            return call("pgp_propose_source_patch", buildJsonObject {
                put("sourceId", "main")
                put("projectRevision", source.string("projectRevision"))
                put("sourceSha256", source.string("sourceSha256"))
                putJsonArray("edits") {
                    edits.forEach { (old, new) -> add(buildJsonObject { put("oldText", old); put("newText", new) }) }
                }
            })
        }
        fun build() = call("pgp_build_project", buildJsonObject { put("projectRevision", service.projectRevision()) })

        patch("LIA 1" to "BROKEN_ONE", "RTN" to "BROKEN_TWO"); service.approve()
        val failed = build()
        val failedId = failed.string("operationId")
        assertNull(poll(failedId)["buildDiagnostics"])
        val failedRevision = service.projectRevision()
        service.approve()
        val result = poll(failedId)
        assertEquals("FAILED", result.string("status"))
        val diagnostics = result.getValue("buildDiagnostics").jsonObject
        assertEquals(failedRevision, diagnostics.string("projectRevision"))
        assertEquals(1, diagnostics.getValue("total").jsonPrimitive.int)
        assertEquals(JsonPrimitive(false), diagnostics["truncated"])
        val items = diagnostics.getValue("items").jsonArray
        // The built-in assembler stops at the first error in this source.
        assertEquals(2, items.single().jsonObject.getValue("line").jsonPrimitive.int)
        assertTrue(items.single().jsonObject.string("message").contains("BROKEN_ONE"))
        assertTrue(items.all { it.jsonObject.string("sourceId") == "main" && it.jsonObject.string("code") == "ASSEMBLY_ERROR" })
        assertEquals(DesktopDevelopmentReadService { state }.context("diagnostics", null, 0, 32)["diagnostics"], items)

        val file = workspace.sources.single().file
        val brokenText = file.readText()
        val fix = patch("BROKEN_ONE" to "LIA 2", "BROKEN_TWO" to "RTN")
        assertEquals("PENDING_APPROVAL", fix.string("status"))
        assertEquals(2, service.review()?.replacements?.size)
        assertEquals(brokenText, file.readText())
        service.approve()
        assertEquals("APPLIED", poll(fix.string("operationId")).string("status"))
        assertEquals("ORG 0xC500\nENTRY: LIA 2\nRTN\n", file.readText())
        val success = build(); service.approve()
        val rebuilt = poll(success.string("operationId"))
        assertEquals("SUCCEEDED", rebuilt.string("status"))
        assertNull(rebuilt["buildDiagnostics"])
        assertEquals(diagnostics, poll(failedId)["buildDiagnostics"])
        assertContentEquals(before, runner.memorySnapshot(0xc500, 8).copyBytes()); assertEquals(cpu, runner.cpuSnapshot().programCounter)
    }

    @Test fun linksOversizeMalformedTextAndChangedManifestAreRejected() = project { workspace ->
        val file = workspace.sources.single().file
        val service = DevelopmentChangeService({ DevelopmentReadState(project = workspace) })
        file.writeBytes(byteArrayOf(0xc3.toByte()))
        expect("INVALID_UTF8") { service.source("main") }
        file.writeText("x".repeat(65537)); expect("LIMIT_EXCEEDED") { service.source("main") }
        file.delete()
        val outside = Files.createTempFile("pgp-external-source-", ".asm").toFile()
        try {
            outside.writeText("PRIVATE")
            Files.createSymbolicLink(file.toPath(), outside.toPath())
            assertFails { service.source("main") }
            file.delete(); file.writeText("RTN")
            workspace.manifestFile.writeText(workspace.manifestFile.readText().replace("Review test", "Changed"))
            expect("PROJECT_REOPEN_REQUIRED") { service.source("main") }
        } finally { outside.delete() }
    }

    @Test fun queuedBuildRejectsLinkedOutputsAndDoesNotEndorseConcurrentEdits() = project { workspace ->
        var builds = 0
        val service = DevelopmentChangeService({ DevelopmentReadState(project = workspace) }, onBuild = { builds++ })
        val root = workspace.manifestFile.parentFile
        val outside = Files.createTempDirectory("pgp-build-outside-")
        try {
            Files.createSymbolicLink(root.resolve("build").toPath(), outside)
            expect("LINK_NOT_ALLOWED") { service.proposeBuild(service.source("main").string("projectRevision")) }
            root.resolve("build").delete()
            val queued = service.proposeBuild(service.source("main").string("projectRevision"))
            assertEquals(0, builds)
            service.approve(); assertEquals(1, builds)
            assertEquals("RUNNING", service.operation(queued.string("operationId")).string("status"))
            workspace.sources.single().file.appendText("; concurrent edit\n")
            service.completeBuild(queued.string("operationId"), true)
            assertEquals("STALE_PROJECT", service.operation(queued.string("operationId")).string("code"))
            val source = service.source("main")
            val before = workspace.sources.single().file.readText()
            expect("AMBIGUOUS_PATCH") { service.propose("main", source.string("projectRevision"), source.string("sourceSha256"),
                listOf(SourceReplacement("LIA 1", "LIA 2"), SourceReplacement("missing", "new"))) }
            assertEquals(before, workspace.sources.single().file.readText()); assertNull(service.review())
        } finally { outside.toFile().deleteRecursively() }
    }

    @Test fun authenticatedHttpRoundTripQueuesReviewAndReturnsStatusWithoutApplying() = project { workspace ->
        val service = DevelopmentChangeService({ DevelopmentReadState(project = workspace) })
        val credentials = Files.createTempDirectory("pgp-review-http-")
        val server = DesktopMcpServer(PgpMcpToolAdapter({ null }, changesProvider = { service }), token = "review-test-token",
            credentialStore = object : DesktopMcpCredentialStore {
                override fun publish(token: String) = DesktopMcpCredentialLocation(credentials.resolve("headers.json"), "unused-test-helper")
                override fun removeIfOwned(token: String) { }
            })
        try {
            val info = assertIs<DesktopMcpStartResult.Success>(server.start(0)).info
            val client = java.net.http.HttpClient.newHttpClient()
            fun call(name: String, args: JsonObject): JsonObject {
                val body = buildJsonObject {
                    put("jsonrpc", "2.0"); put("id", 1); put("method", "tools/call")
                    put("params", buildJsonObject { put("name", name); put("arguments", args) })
                }
                val request = java.net.http.HttpRequest.newBuilder(java.net.URI(info.url))
                    .header("Authorization", "Bearer review-test-token").header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString())).build()
                val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                assertEquals(200, response.statusCode())
                return Json.parseToJsonElement(response.body()).jsonObject["result"]!!.jsonObject["structuredContent"]!!.jsonObject
            }
            val source = call("pgp_get_source", buildJsonObject { put("sourceId", "main") })
            assertTrue(source.string("text").contains("LIA 1"))
            val proposal = call("pgp_build_project", buildJsonObject { put("projectRevision", source.string("projectRevision")) })
            val status = call("pgp_get_operation", buildJsonObject { put("operationId", proposal.string("operationId")) })
            assertEquals("PENDING_APPROVAL", status.string("status"))
            service.reject()
            assertEquals("REJECTED", call("pgp_get_operation", buildJsonObject { put("operationId", proposal.string("operationId")) }).string("status"))
        } finally { server.close(); credentials.toFile().deleteRecursively() }
    }

    @Test fun mcpCannotApproveAndRejectsUnknownFieldsWithoutLeakingExceptionDetails() = project { workspace ->
        val service = DevelopmentChangeService({ DevelopmentReadState(project = workspace) })
        val adapter = PgpMcpToolAdapter({ null }, changesProvider = { service })
        fun call(name: String, args: JsonObject) = adapter.call(name, args)
        val source = call("pgp_get_source", buildJsonObject { put("sourceId", "main") })["structuredContent"]!!.jsonObject
        val queued = call("pgp_propose_source_patch", buildJsonObject {
            put("sourceId", "main"); put("projectRevision", source.string("projectRevision")); put("sourceSha256", source.string("sourceSha256"))
            put("edits", buildJsonArray { add(buildJsonObject { put("oldText", "LIA 1"); put("newText", "LIA 2") }) })
        })
        assertFalse(queued["isError"]!!.jsonPrimitive.boolean)
        assertTrue(call("pgp_apply_source_patch", buildJsonObject {})["isError"]!!.jsonPrimitive.boolean)
        assertTrue(call("pgp_get_source", buildJsonObject { put("sourceId", "main"); put("path", "/private/secret") })["isError"]!!.jsonPrimitive.boolean)
        assertTrue(workspace.sources.single().file.readText().contains("LIA 1"))
        val mutations = adapter.tools().filter { !it.jsonObject["annotations"]!!.jsonObject["readOnlyHint"]!!.jsonPrimitive.boolean }
        assertEquals(ProjectGenerationMcp.names + setOf(BoundedExecutionMcp.PROPOSE, "pgp_propose_source_patch", "pgp_build_project", DebugChangeMcp.PROPOSE, ExecutionChangeMcp.LOAD, ExecutionChangeMcp.CONTINUE), mutations.map { it.jsonObject.string("name") }.toSet())
    }
}
