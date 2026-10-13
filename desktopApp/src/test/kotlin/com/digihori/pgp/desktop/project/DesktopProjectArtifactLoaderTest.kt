package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.MonotonicClock
import com.digihori.pgp.desktop.runner.RunnerState
import java.nio.file.Files
import kotlin.test.*

class DesktopProjectArtifactLoaderTest {
    private fun session(id: MachineId): EmulatorSession {
        val components = mutableListOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)))
        val bankCount = (MachineCatalog.require(id).memoryBanking as? MachineMemoryBanking.Banked)?.bankCount ?: 0
        if (bankCount > 0) repeat(bankCount) { components += RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000)) }
        else components += RomComponent(RomComponentId("external"), RomRole.EXTERNAL,
            ByteArray(if (MachineCatalog.require(id).generation == MachineGeneration.OLD) 0x4000 else 0x8000))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, components))).session
    }

    private fun initialisePointers(session: EmulatorSession, start: Int = session.memoryLayout.defaultBasicStartAddress) {
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(listOf(
            AddressedMemorySegmentData(session.memoryLayout.basicStartPointerAddress, byteArrayOf(
                start.toByte(), (start ushr 8).toByte(), (start - 1).toByte(), ((start - 1) ushr 8).toByte(),
            )),
        ))).image
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image))
    }

    private fun artifact(id: MachineId): DesktopProjectArtifact {
        val root = Files.createTempDirectory("pgp-template-load")
        val created = assertIs<DesktopProjectCreateResult.Success>(DesktopProjectWorkspaceCreator.create(
            root.toFile(), id.value, id, DesktopProjectStarter.entries.toSet(),
        ))
        val workspace = assertIs<DesktopProjectOpenResult.Success>(DesktopProjectWorkspaceLoader.open(created.manifestFile)).workspace
        return assertIs<DesktopProjectBuildResult.Success>(DesktopProjectBuilder.build(workspace)).artifact
    }

    @Test fun buildsAndLoadsAllMachineStartersWithoutLosingRunPauseState() {
        for (id in EmulatorFactory.supportedMachineIds()) for (running in listOf(false, true)) {
            val session = session(id)
            initialisePointers(session)
            val runner = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            if (running) runner.run()
            val built = artifact(id)
            val loaded = assertIs<DesktopProjectApplyResult.Success>(DesktopProjectArtifactLoader.load(runner, built), id.value)
            assertEquals(checkNotNull(built.copyBasicProgram()).size, loaded.basicByteCount)
            assertContentEquals(built.copyBasicProgram(), assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot()).copyBytes())
            checkNotNull(built.memoryImage).segments.forEach { segment ->
                assertContentEquals(segment.copyBytes(), session.memorySnapshot(segment.startAddress, segment.size).copyBytes())
            }
            assertEquals(if (running) RunnerState.RUNNING else RunnerState.PAUSED, runner.state)
        }
    }

    @Test fun failedLoadUsesCurrentBasicPointersAndLeavesTheWholeArtifactUnapplied() {
        val id = MachineId("pc-1245")
        for (running in listOf(false, true)) {
            val session = session(id)
            initialisePointers(session, session.memoryLayout.assemblyStartAddress)
            val runner = DesktopEmulatorRunner(session, MonotonicClock { 0L })
            if (running) runner.run()
            val before = session.memorySnapshot(0, 0x10000).copyBytes()
            val failure = assertIs<DesktopProjectApplyResult.MemoryFailure>(DesktopProjectArtifactLoader.load(runner, artifact(id)))
            assertIs<MemoryImageLoadError.BasicOverlap>(failure.error)
            assertContentEquals(before, session.memorySnapshot(0, 0x10000).copyBytes())
            assertEquals(if (running) RunnerState.RUNNING else RunnerState.PAUSED, runner.state)
        }
    }
}
