package com.digihori.pgp.desktop.application.development

import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.desktop.mcp.*
import com.digihori.pgp.desktop.project.*
import java.nio.file.Files
import kotlin.test.*
import kotlinx.serialization.json.*

class ProjectGenerationTest {
    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private val basic = GeneratedSource("main", ProjectSourceType.BASIC, "10 PRINT \"HELLO\"\n20 END\n")
    private val assembly = GeneratedSource("routine", ProjectSourceType.ASSEMBLY, "ORG 0xC500\nRTN\n")
    private fun expect(code: String, block: () -> Unit) = assertEquals(code, assertFailsWith<DevelopmentReadError>(block = block).code)
    private fun directory(block: (java.io.File) -> Unit) {
        val parent = Files.createTempDirectory("pgp-generation-").toFile()
        try { block(parent) } finally { parent.deleteRecursively() }
    }

    @Test fun creationNeedsLocalDestinationAndApprovalAndOpensExactlyReviewedSources() = directory { parent ->
        var state = DevelopmentReadState()
        val service = DevelopmentChangeService({ state }, onWorkspaceChanged = { state = state.copy(project = it) })
        val request = service.proposeProject("Generated example", "pc-1245", listOf(basic, assembly))
        val id = request.string("operationId")
        assertEquals("PENDING_APPROVAL", request.string("status"))
        assertNull(service.creationDestination()); assertTrue(parent.listFiles()!!.isEmpty())
        service.selectCreationParent(parent)
        val destination = service.creationDestination()!!
        assertFalse(destination.exists()); assertTrue(parent.listFiles()!!.isEmpty())
        assertFalse(service.operation(id).toString().contains(parent.absolutePath))
        service.approve()
        assertEquals("APPLIED", service.operation(id).string("status"))
        val project = state.project!!
        assertEquals(destination, project.manifestFile.parentFile)
        assertEquals(listOf("src/main.bas", "src/routine.asm"), project.sources.map { it.definition.path })
        assertEquals(basic.text, service.source("main").string("text"))
        assertEquals(assembly.text, service.source("routine").string("text"))
        assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(project))
        assertEquals(project.definition, assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(project.manifestFile)).workspace.definition)
    }

    @Test fun pc1251CreationAndAdditionPreserveMachineAcrossApprovalAndBuild() = directory { parent ->
        var state = DevelopmentReadState()
        val service = DevelopmentChangeService({ state }, onWorkspaceChanged = { state = state.copy(project = it) })
        val operation = service.proposeProject("PC1251", "pc-1251", listOf(basic)).string("operationId")
        assertEquals("pc-1251", service.review()!!.machineId)
        assertTrue(parent.listFiles()!!.isEmpty())
        service.selectCreationParent(parent); service.approve()
        assertEquals("APPLIED", service.operation(operation).string("status"))
        assertEquals("pc-1251", state.project!!.definition.machineId.value)
        assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(state.project!!))
        val addition = service.proposeSource(service.projectRevision(), assembly).string("operationId")
        assertEquals("pc-1251", service.review()!!.machineId)
        service.approve(); assertEquals("APPLIED", service.operation(addition).string("status"))
        assertEquals("pc-1251", assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(state.project!!.manifestFile)).workspace.definition.machineId.value)
    }

    @Test fun rejectionAndMissingDestinationCannotCreateFiles() = directory { parent ->
        val service = DevelopmentChangeService({ DevelopmentReadState() })
        val first = service.proposeProject("One", "pc-1245", listOf(basic)).string("operationId")
        service.selectCreationParent(parent); service.reject()
        assertEquals("REJECTED", service.operation(first).string("status")); assertTrue(parent.listFiles()!!.isEmpty())
        val second = service.proposeProject("Two", "pc-1245", listOf(basic)).string("operationId")
        assertNull(service.creationDestination()); service.approve()
        assertEquals("DESTINATION_REQUIRED", service.operation(second).string("code")); assertTrue(parent.listFiles()!!.isEmpty())
    }

    @Test fun existingDestinationAndChangedProjectPreventCreationWithoutOverwriting() = directory { parent ->
        var state = DevelopmentReadState()
        val service = DevelopmentChangeService({ state })
        val id = service.proposeProject("One", "pc-1245", listOf(basic)).string("operationId")
        service.selectCreationParent(parent)
        val destination = service.creationDestination()!!; destination.mkdir()
        val sentinel = destination.resolve("keep.txt").apply { writeText("keep") }
        service.approve()
        assertEquals("DESTINATION_EXISTS", service.operation(id).string("code")); assertEquals("keep", sentinel.readText())
        val other = GeneratedProjectFiles.create(parent.toPath(), "other", "Other", listOf(basic))
        val next = service.proposeProject("Two", "pc-1245", listOf(basic)).string("operationId")
        service.selectCreationParent(parent); val nextDestination = service.creationDestination()!!
        state = state.copy(project = other); service.approve()
        assertEquals("PROJECT_ALREADY_OPEN", service.operation(next).string("code")); assertFalse(nextDestination.exists())
    }

    @Test fun sourceAdditionRegistersNewFileAndRequiresFreshRevisionForBuild() = directory { parent ->
        var state = DevelopmentReadState(project = GeneratedProjectFiles.create(parent.toPath(), "project", "Example", listOf(basic)))
        var built = false
        lateinit var service: DevelopmentChangeService
        service = DevelopmentChangeService({ state }, onWorkspaceChanged = { state = state.copy(project = it) }, onBuild = {
            built = DesktopProjectBuilder.build(state.project!!) is DesktopProjectBuildResult.Success
            service.completeBuild(it, built)
        })
        val revision = service.source("main").string("projectRevision")
        val manifestBefore = state.project!!.manifestFile.readText()
        val id = service.proposeSource(revision, assembly).string("operationId")
        assertFalse(state.project!!.manifestFile.parentFile.resolve("src/routine.asm").exists())
        assertEquals(manifestBefore, state.project!!.manifestFile.readText())
        service.approve(); assertEquals("APPLIED", service.operation(id).string("status"))
        assertEquals(2, state.project!!.sources.size)
        expect("STALE_PROJECT") { service.proposeBuild(revision) }
        val build = service.proposeBuild(service.source("routine").string("projectRevision")).string("operationId")
        assertFalse(built); service.approve()
        assertTrue(built); assertEquals("SUCCEEDED", service.operation(build).string("status"))
        expect("SOURCE_EXISTS") { service.proposeSource(service.projectRevision(), basic) }
    }

    @Test fun changedSourcesAndUntrackedCollisionsRejectAdditionBeforeManifestChanges() = directory { parent ->
        val project = GeneratedProjectFiles.create(parent.toPath(), "project", "Example", listOf(basic))
        val service = DevelopmentChangeService({ DevelopmentReadState(project = project) })
        val revision = service.projectRevision()
        val id = service.proposeSource(revision, assembly).string("operationId")
        project.sources.first().file.appendText("30 REM user edit\n")
        service.approve(); assertEquals("STALE_PROJECT", service.operation(id).string("code"))
        val target = parent.resolve("project/src/routine.asm")
        assertFalse(target.exists())
        val second = service.proposeSource(service.projectRevision(), assembly).string("operationId")
        target.writeText("keep")
        val before = project.manifestFile.readText(); service.approve()
        assertEquals("SOURCE_EXISTS", service.operation(second).string("code"))
        assertEquals("keep", target.readText()); assertEquals(before, project.manifestFile.readText())
    }

    @Test fun linkedSourceDirectoryAndReplacedDestinationParentAreRejected() = directory { parent ->
        val project = GeneratedProjectFiles.create(parent.toPath(), "project", "Example", listOf(basic))
        val outside = parent.resolve("outside").apply { mkdir() }
        val src = parent.resolve("project/src")
        Files.move(src.toPath(), parent.resolve("saved-src").toPath())
        Files.createSymbolicLink(src.toPath(), outside.toPath())
        expect("LINK_NOT_ALLOWED") { GeneratedProjectFiles.checkAddition(project, assembly) }
        Files.delete(src.toPath())
        val service = DevelopmentChangeService({ DevelopmentReadState() })
        val id = service.proposeProject("Example", "pc-1245", listOf(basic)).string("operationId")
        service.selectCreationParent(outside)
        Files.move(outside.toPath(), parent.resolve("saved-parent").toPath()); outside.mkdir()
        service.approve(); assertEquals("DESTINATION_CHANGED", service.operation(id).string("code"))
        assertTrue(outside.listFiles()!!.isEmpty())
    }

    @Test fun failedFinalRevisionCheckRollsBackOnlyNewFilesAndKeepsManifest() = directory { parent ->
        val project = GeneratedProjectFiles.create(parent.toPath(), "project", "Example", listOf(basic))
        val manifest = project.manifestFile.readBytes()
        expect("STALE_PROJECT") { GeneratedProjectFiles.add(project, assembly) { throw DevelopmentReadError("STALE_PROJECT") } }
        assertContentEquals(manifest, project.manifestFile.readBytes())
        assertEquals(basic.text, project.sources.single().file.readText())
        assertFalse(parent.resolve("project/src/routine.asm").exists())
        assertFalse(project.manifestFile.parentFile.listFiles()!!.any { it.name.startsWith(".pgp-generated-") })
    }

    @Test fun strictMcpSchemaAndPortableTextLimitsDoNotOfferPathsOrRemoteApproval() {
        val service = DevelopmentChangeService({ DevelopmentReadState() })
        val adapter = PgpMcpToolAdapter({ error("Must not access emulator") }, changesProvider = { service })
        val valid = buildJsonObject {
            put("projectName", "Example"); put("machineId", "pc-1245")
            putJsonArray("sources") { add(buildJsonObject { put("sourceId", "main"); put("type", "basic"); put("text", basic.text) }) }
        }
        for (args in listOf(JsonObject(valid + ("path" to JsonPrimitive("/tmp/arbitrary"))), JsonObject(valid + ("approve" to JsonPrimitive(true))), JsonObject(valid + ("projectName" to JsonNull)))) {
            assertEquals("INVALID_ARGUMENTS", adapter.call(ProjectGenerationMcp.PROJECT, args).getValue("structuredContent").jsonObject.getValue("error").jsonObject.string("code"))
        }
        for (id in listOf("../escape", "UPPER", "con", "lpt1")) expect("INVALID_SOURCE_ID") { service.proposeProject("Example", "pc-1245", listOf(basic.copy(id = id))) }
        expect("INVALID_SOURCES") { service.proposeProject("Example", "pc-1245", listOf(basic, basic)) }
        expect("LIMIT_EXCEEDED") { service.proposeProject("Example", "pc-1245", listOf(basic.copy(text = "あ".repeat(11000)))) }
        expect("INVALID_TEXT") { service.proposeProject("Example", "pc-1245", listOf(basic.copy(text = "10 REM\u0000"))) }
        expect("UNSUPPORTED_GENERATION_MACHINE") { service.proposeProject("Example", "pc-1350", listOf(basic)) }
        val response = adapter.call(ProjectGenerationMcp.PROJECT, valid)
        val id = response.getValue("structuredContent").jsonObject.string("operationId")
        assertTrue(response.getValue("content").jsonArray.single().jsonObject.string("text").contains(id))
        expect("BUSY") { service.proposeProject("Another", "pc-1245", listOf(basic)) }
    }
}
