package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1261.*
import com.digihori.pgp.core.emulator.machine.pc1401.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.basic.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class CapacityFamiliesTest {
    private val ids = listOf("1260", "1261", "1401", "1402").map { MachineId("pc-$it") }
    private fun rom(id: MachineId, revision: Int = 0): RomSet {
        val internal = ByteArray(0x2000) { revision.toByte() }
        val external = ByteArray(0x8000) { (revision + it).toByte() }
        return assertIs<RomImportResult.Success>(
            Pc1261FamilyModel.fromMachineId(id)?.let { Pc1261ComponentRomImporter.importImages(internal, external, it) }
                ?: Pc1401ComponentRomImporter.importImages(internal, external, checkNotNull(Pc1401FamilyModel.fromMachineId(id)))
        ).romSet
    }
    private fun session(id: MachineId, revision: Int = 0) = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom(id, revision))).session

    @Test fun revisionBytesDoNotChangeModelOrBasicAddresses() {
        assertNull(MachineCatalog.find(MachineId("pc-1262")))
        for (id in ids) for (revision in listOf(0, 73)) {
            val s = session(id, revision)
            assertEquals(id, s.machineId)
            assertEquals(revision, s.memorySnapshot(0x8000, 1).copyBytes()[0].toInt() and 255)
            val start = when (id.value) { "pc-1260" -> 0x5880; "pc-1401" -> 0x3800; "pc-1402" -> 0x2000; else -> 0x4080 }
            assertEquals(start, s.memoryLayout.defaultBasicStartAddress)
            val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram("10 PRINT 123\n20 END".encodeToByteArray(), id)).bytes
            val load = assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(program))
            assertEquals(start, load.startAddress)
            assertContentEquals(program, assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot()).copyBytes())
            val pointer = s.memorySnapshot(s.memoryLayout.basicStartPointerAddress, 2).copyBytes()
            assertEquals(start, (pointer[0].toInt() and 255) or ((pointer[1].toInt() and 255) shl 8))
            val planned = assertIs<ProgramImagePlanResult.Success>(ProgramImageLoader.plan(s.memoryLayout, program, null) { 0 }).image!!
            assertIs<MemoryImageLoadResult.Success>(s.loadMemoryImage(planned))
            assertContentEquals(program, assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot()).copyBytes())
            assertIs<CreateSessionResult.Failure>(EmulatorFactory.create(ids.first { it != id }, rom(id, revision)))
        }
    }

    @Test fun physicalRamSizeMatchesPlacementAndResetWithoutAddingRamMirrors() {
        for (id in ids) {
            val s = session(id)
            val start = Pc1261FamilyModel.fromMachineId(id)?.physicalRamStart
                ?: checkNotNull(Pc1401FamilyModel.fromMachineId(id)).physicalRamStart
            val end = if (id.value.startsWith("pc-126")) 0x67ff else 0x47ff
            fun image(address: Int, value: Byte = 85) = assertIs<AddressedMemoryImageCreateResult.Success>(
                AddressedMemoryImageFactory.create(listOf(AddressedMemorySegmentData(address, byteArrayOf(value))))).image
            for (address in listOf(start, end)) {
                assertTrue(s.memoryLayout.locate(address).writable)
                assertIs<MemoryImageLoadResult.Success>(s.loadMemoryImage(image(address)))
                assertEquals(85, s.memorySnapshot(address, 1).copyBytes()[0].toInt())
            }
            assertTrue(s.memoryLayout.locate(start - 1).kind != MachineMemoryKind.RAM)
            if (id.value != "pc-1261") {
                assertFalse(s.memoryLayout.locate(start - 1).writable)
                assertIs<MemoryImageLoadResult.Failure>(s.loadMemoryImage(image(start - 1)))
            }
            assertFalse(s.memoryLayout.locate(end + 1).writable)
            assertIs<MemoryImageLoadResult.Failure>(s.loadMemoryImage(image(end + 1)))
            val other = session(id)
            assertEquals(0, other.memorySnapshot(start, 1).copyBytes()[0].toInt())
            s.reset()
            assertEquals(0, s.memorySnapshot(start, 1).copyBytes()[0].toInt())
            assertEquals(0, s.memorySnapshot(end, 1).copyBytes()[0].toInt())
            // Capacity-dependent RAM aliases remain explicitly deferred.
            val alias = when (id.value) { "pc-1260" -> 0x4000; "pc-1401" -> 0x2000; else -> null }
            if (alias != null) {
                assertFalse(s.memoryLayout.locate(alias).writable)
                assertIs<MemoryImageLoadResult.Failure>(s.loadMemoryImage(image(alias)))
            }
        }
    }

    @Test fun oversizedBasicFailsAtomicallyAndMemoryStopsAtRamBoundary() {
        for (id in ids) {
            val s = session(id)
            val before = s.memorySnapshot(0x2000, 0x6000).copyBytes()
            val huge = buildString { repeat(600) { append("${it + 1} REM "); append("A".repeat(30)); append('\n') } }
            val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(huge.encodeToByteArray(), id)).bytes
            assertIs<BasicProgramLoadResult.Failure>(s.loadBasicProgram(program))
            assertContentEquals(before, s.memorySnapshot(0x2000, 0x6000).copyBytes())
            val end = if (id.value.startsWith("pc-126")) 0x67ff else 0x47ff
            val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(listOf(AddressedMemorySegmentData(end, byteArrayOf(1, 2))))).image
            assertIs<MemoryImageLoadResult.Failure>(s.loadMemoryImage(image))
            assertContentEquals(before, s.memorySnapshot(0x2000, 0x6000).copyBytes())
        }
    }
}
