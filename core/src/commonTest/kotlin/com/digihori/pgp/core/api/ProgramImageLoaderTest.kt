package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyMemoryMode
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class ProgramImageLoaderTest {
    private val program = byteArrayOf(0xff.toByte(), 0xff.toByte())
    private fun image(vararg segments: Pair<Int, ByteArray>): AddressedMemoryImage =
        assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            segments.map { AddressedMemorySegmentData(it.first, it.second) },
        )).image

    private fun session(id: MachineId, mode: Pc1251FamilyMemoryMode = Pc1251FamilyMemoryMode.EXPANDED): EmulatorSession {
        val components = mutableListOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)))
        val bankCount = (MachineCatalog.require(id).memoryBanking as? MachineMemoryBanking.Banked)?.bankCount ?: 0
        if (bankCount > 0) repeat(bankCount) { components += RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000)) }
        else components += RomComponent(RomComponentId("external"), RomRole.EXTERNAL,
            ByteArray(if (id.value in listOf("pc-1245", "pc-1250", "pc-1251", "pc-1255")) 0x4000 else 0x8000))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, components), EmulatorConfiguration(mode))).session
    }

    private fun initialisePointers(session: EmulatorSession, start: Int = session.memoryLayout.defaultBasicStartAddress, end: Int = start - 1) {
        val pointer = session.memoryLayout.basicStartPointerAddress
        assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image(pointer to byteArrayOf(
            start.toByte(), (start ushr 8).toByte(), end.toByte(), (end ushr 8).toByte(),
        ))))
    }

    @Test fun loadsBasicAndMachineCodeForEveryModelAndMemoryMode() {
        for (id in EmulatorFactory.supportedMachineIds()) for (mode in Pc1251FamilyMemoryMode.entries) {
            val session = session(id, mode)
            initialisePointers(session)
            val address = session.memoryLayout.assemblyStartAddress
            val machineCode = image(address to byteArrayOf(0x6b, 0x01, 0x37))
            assertEquals(ProgramImageLoadResult.Success(2, 1, 3), session.loadProgramImage(program, machineCode), id.value)
            assertContentEquals(program, assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot()).copyBytes())
            assertContentEquals(byteArrayOf(0x6b, 0x01, 0x37), session.memorySnapshot(address, 3).copyBytes())
        }
    }

    @Test fun failuresLeaveMemoryCpuDisplayAndBankUnchanged() {
        for (id in EmulatorFactory.supportedMachineIds()) {
            val session = session(id)
            initialisePointers(session)
            val layout = session.memoryLayout
            val cases = listOf(
                program to image(layout.assemblyStartAddress to byteArrayOf(1), 0 to byteArrayOf(2)),
                program to image(layout.defaultBasicStartAddress to byteArrayOf(3)),
                program to image(layout.basicStartPointerAddress to byteArrayOf(4)),
                byteArrayOf(0) to image(layout.assemblyStartAddress to byteArrayOf(5)),
            )
            for ((basic, code) in cases) {
                val before = session.memorySnapshot(0, 0x10000).copyBytes()
                val pc = session.cpuSnapshot().programCounter
                val dots = session.displaySnapshot().copyDots()
                val displayRevision = session.displaySnapshot().revision
                val bank = session.selectedRomBank()
                assertFalse(session.loadProgramImage(basic, code) is ProgramImageLoadResult.Success, id.value)
                assertContentEquals(before, session.memorySnapshot(0, 0x10000).copyBytes(), id.value)
                assertEquals(pc, session.cpuSnapshot().programCounter)
                assertContentEquals(dots, session.displaySnapshot().copyDots())
                assertEquals(displayRevision, session.displaySnapshot().revision)
                assertEquals(bank, session.selectedRomBank())
            }
        }
    }

    @Test fun detectsMirrorsWithinAndBetweenSegmentsBeforeAnyWrites() {
        val pairs = mapOf("pc-1245" to (0xc000 to 0xd000), "pc-1250" to (0xc000 to 0xd000),
            "pc-1251" to (0xc000 to 0xd000), "pc-1255" to (0xc000 to 0xd000),
            "pc-1261" to (0x2000 to 0x2100), "pc-1401" to (0x6000 to 0x7000))
        for ((id, aliases) in pairs) {
            val session = session(MachineId(id))
            val before = session.memorySnapshot(0, 0x10000).copyBytes()
            val result = assertIs<MemoryImageLoadResult.Failure>(session.loadMemoryImage(image(
                aliases.first to byteArrayOf(1), aliases.second to byteArrayOf(2),
            )))
            assertIs<MemoryImageLoadError.MirrorOverlap>(result.error)
            assertContentEquals(before, session.memorySnapshot(0, 0x10000).copyBytes())
            // Verify the metadata against an actual bus write, rather than only the validator itself.
            assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image(aliases.second to byteArrayOf(0x5a))))
            assertEquals(0x5a, session.memorySnapshot(aliases.first, 1).copyBytes().single().toInt() and 0xff, id)
        }
        val lcd = session(MachineId("pc-1401"))
        assertIs<MemoryImageLoadError.MirrorOverlap>(assertIs<MemoryImageLoadResult.Failure>(
            lcd.loadMemoryImage(image(0x6000 to ByteArray(257))),
        ).error)
    }

    @Test fun replansUsingCurrentPointersAndIncludesOldBasicClearing() {
        val session = session(MachineId("pc-1245"))
        initialisePointers(session, 0xc100, 0xc110)
        for (address in listOf(0xc100, 0xc110, 0xd110)) {
            val before = session.memorySnapshot(0, 0x10000).copyBytes()
            val failure = assertIs<ProgramImageLoadResult.MemoryFailure>(session.loadProgramImage(program, image(address to byteArrayOf(1))))
            assertIs<MemoryImageLoadError.BasicOverlap>(failure.error)
            assertContentEquals(before, session.memorySnapshot(0, 0x10000).copyBytes())
        }
        assertIs<ProgramImageLoadResult.Success>(session.loadProgramImage(program, image(0xc120 to byteArrayOf(1))))
        assertEquals(0xc100, assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot()).startAddress)
    }

    @Test fun validatesActualHardwareRamAndReadonlyBoundaries() {
        val hardware = session(MachineId("pc-1250"), Pc1251FamilyMemoryMode.HARDWARE)
        val expanded = session(MachineId("pc-1250"), Pc1251FamilyMemoryMode.EXPANDED)
        assertFalse(hardware.memoryLayout.locate(0xa000).writable)
        assertTrue(expanded.memoryLayout.locate(0xa000).writable)
        assertIs<ProgramImageLoadResult.MemoryFailure>(hardware.loadProgramImage(null, image(0xa000 to byteArrayOf(1))))
        assertIs<ProgramImageLoadResult.Success>(expanded.loadProgramImage(null, image(0xa000 to byteArrayOf(1))))
        for ((id, boundary) in listOf("pc-1261" to 0x67ff, "pc-1350" to 0x7fff, "pc-1401" to 0x47ff)) {
            val current = session(MachineId(id))
            assertIs<MemoryImageLoadResult.Failure>(current.loadMemoryImage(image(boundary to byteArrayOf(1, 2))))
            assertEquals(0, current.memorySnapshot(boundary, 1).copyBytes()[0].toInt())
        }
    }

    @Test fun controlWritesRemainAvailableButAreNotAppliedOnFailure() {
        val session = session(MachineId("pc-1360"))
        assertEquals(MachineMemoryKind.CONTROL, session.memoryLayout.locate(0x3400).kind)
        assertEquals(MachineMemoryKind.ROM, session.memoryLayout.locate(0x4000).kind)
        assertIs<ProgramImageLoadResult.MemoryFailure>(session.loadProgramImage(null, image(0x3400 to byteArrayOf(7), 0x4000 to byteArrayOf(1))))
        assertEquals(0, session.selectedRomBank())
        assertIs<ProgramImageLoadResult.Success>(session.loadProgramImage(null, image(0x3400 to byteArrayOf(7))))
        assertEquals(7, session.selectedRomBank())
    }
}
