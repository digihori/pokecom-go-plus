package com.digihori.pgp.core.api

import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class UnrestrictedRamTest {
    private val regions = mapOf(
        "pc-1250" to (0x8000..0xcfff), "pc-1251" to (0x8000..0xcfff), "pc-1255" to (0x8000..0xcfff),
        "pc-1260" to (0x4000..0x7fff), "pc-1261" to (0x4000..0x7fff),
        "pc-1401" to (0x2000..0x5fff), "pc-1402" to (0x2000..0x5fff),
    )
    private fun create(id: String, mode: RamMode, configuration: EmulatorConfiguration = EmulatorConfiguration(mode)): EmulatorSession {
        val machineId = MachineId(id)
        val externalSize = if (id.startsWith("pc-125")) 0x4000 else 0x8000
        val rom = RomSet(machineId, listOf(
            RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000) { 0x4d }),
            RomComponent(RomComponentId("external"), RomRole.EXTERNAL, ByteArray(externalSize) { 0x4d }),
        ))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machineId, rom, configuration)).session
    }
    private fun image(start: Int, bytes: ByteArray) = assertIs<AddressedMemoryImageCreateResult.Success>(
        AddressedMemoryImageFactory.create(listOf(AddressedMemorySegmentData(start, bytes))),
    ).image

    @Test fun fullRangeIsIndependentWritableRamAndResetClearsIt() {
        for ((id, range) in regions) {
            val session = create(id, RamMode.UNRESTRICTED)
            assertTrue(MachineCatalog.require(session.machineId).supportsConfigurableRam)
            assertEquals(RamMode.UNRESTRICTED, session.memoryLayout.ramMode)
            for (address in range) {
                val location = session.memoryLayout.locate(address)
                assertEquals(MachineMemoryKind.RAM, location.kind, "$id/${address.toString(16)}")
                assertEquals(address, location.physicalAddress)
                assertTrue(location.writable)
            }
            // Different values across pages reveal mirrors, not just writability.
            val bytes = ByteArray(range.count()) { ((it / 0x100) xor it).toByte() }
            assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image(range.first, bytes)))
            assertContentEquals(bytes, session.memorySnapshot(range.first, bytes.size).copyBytes(), id)
            assertContentEquals(ByteArray(0x2000) { 0x4d }, session.memorySnapshot(0, 0x2000).copyBytes(), id)
            session.reset()
            assertContentEquals(ByteArray(bytes.size), session.memorySnapshot(range.first, bytes.size).copyBytes(), id)
            assertNotEquals(MachineMemoryKind.RAM, session.memoryLayout.locate(if (id.startsWith("pc-125")) 0xd800 else range.last + 1).kind, id)
        }
    }

    @Test fun familyOverridesSelectTheCorrectMapForPackagedAndModelVariants() {
        val configuration = EmulatorConfiguration(ramModes = mapOf(MachineFamily.PC_1261 to RamMode.UNRESTRICTED))
        for (id in regions.keys) {
            val session = create(id, RamMode.HARDWARE, configuration)
            assertEquals(if (id.startsWith("pc-126")) RamMode.UNRESTRICTED else RamMode.HARDWARE, session.memoryLayout.ramMode)
        }
    }

    @Test fun basicImportUsesThePointersSelectedByRomAndDoesNotMoveThem() {
        for ((id, range) in regions) {
            val session = create(id, RamMode.UNRESTRICTED)
            val start = range.first + if (id.startsWith("pc-126")) 0x80 else if (id.startsWith("pc-125")) 0x30 else 0
            val pointer = session.memoryLayout.basicStartPointerAddress
            assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image(pointer, byteArrayOf(
                start.toByte(), (start ushr 8).toByte(), (start - 1).toByte(), ((start - 1) ushr 8).toByte(),
            ))))
            val program = byteArrayOf(0xff.toByte(), 0xff.toByte())
            assertIs<ProgramImageLoadResult.Success>(session.loadProgramImage(program, null))
            val snapshot = assertIs<BasicProgramSnapshotResult.Success>(session.basicProgramSnapshot())
            assertEquals(start, snapshot.startAddress, id)
            assertContentEquals(program, snapshot.copyBytes())
        }
    }

    @Test fun onlyTheD000MirrorRemainsInUnrestricted1251Family() {
        for (id in regions.keys.filter { it.startsWith("pc-125") }) {
            val session = create(id, RamMode.UNRESTRICTED)
            assertIs<MemoryImageLoadResult.Success>(session.loadMemoryImage(image(0xd000, byteArrayOf(0x55))))
            assertEquals(0x55, session.memorySnapshot(0xc000, 1).copyBytes()[0].toInt())
            assertEquals(0xc000, session.memoryLayout.locate(0xd000).physicalAddress)
            for (address in listOf(0x8000, 0x9000, 0x9800, 0xa000, 0xb000, 0xb800, 0xc800, 0xcfff)) {
                assertEquals(address, session.memoryLayout.locate(address).physicalAddress)
            }
            val alias = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(listOf(
                AddressedMemorySegmentData(0xc000, byteArrayOf(1)), AddressedMemorySegmentData(0xd000, byteArrayOf(2)),
            ))).image
            assertIs<MemoryImageLoadResult.Failure>(session.loadMemoryImage(alias))
        }
    }

    @Test fun hardwareLimitsAndPeripheralAliasesStayUnchanged() {
        val hardwareStarts = mapOf("pc-1250" to 0xc000, "pc-1251" to 0xb800, "pc-1255" to 0xa000,
            "pc-1260" to 0x5800, "pc-1261" to 0x4000, "pc-1401" to 0x3800, "pc-1402" to 0x2000)
        for ((id, range) in regions) {
            val hardware = create(id, RamMode.HARDWARE)
            val unlocked = create(id, RamMode.UNRESTRICTED)
            assertEquals(RamMode.HARDWARE, hardware.memoryLayout.ramMode)
            assertEquals(MachineMemoryKind.RAM, hardware.memoryLayout.locate(hardwareStarts.getValue(id)).kind)
            assertNotEquals(MachineMemoryKind.RAM, hardware.memoryLayout.locate(range.last).kind)
            // Compare every address outside the new RAM window, preserving LCD/ROM aliases.
            for (address in 0..0xffff) {
                if (address in range) continue
                assertEquals(hardware.memoryLayout.locate(address), unlocked.memoryLayout.locate(address), "$id/${address.toString(16)}")
            }
        }
    }

    @Test fun cardBasedMachinesIgnoreUnrestrictedMode() {
        for (definition in MachineCatalog.definitions.filterNot { it.id.value in regions }) {
            assertFalse(definition.supportsConfigurableRam)
            val hardware = MachineMemoryLayout(definition.id, RamMode.HARDWARE)
            val unrestricted = MachineMemoryLayout(definition.id, RamMode.UNRESTRICTED)
            assertEquals(RamMode.HARDWARE, unrestricted.ramMode)
            for (address in 0..0xffff) assertEquals(hardware.locate(address), unrestricted.locate(address))
        }
    }
}
