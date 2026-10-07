package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FamilyModel
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360RomDefinition
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParseResult
import com.digihori.pgp.core.source.machine.PgpMemoryDumpParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class EmulatorFactoryTest {
    @Test
    fun exposesPc1245AsTheInitialSupportedMachine() {
        assertEquals(
            listOf(
                MachineId("pc-1245"), MachineId("pc-1250"), MachineId("pc-1251"),
                MachineId("pc-1255"), MachineId("pc-1350"), MachineId("pc-1360"),
            ),
            EmulatorFactory.supportedMachineIds(),
        )
    }

    @Test
    fun createsPc1360SessionWithBankedRom() {
        val romSet = assertIs<RomImportResult.Success>(
            Pc1360ComponentRomImporter.importImages(ByteArray(0x2000), List(8) { ByteArray(0x4000) }),
        ).romSet
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1360RomDefinition.MACHINE_ID, romSet),
        ).session
        assertEquals(0, session.selectedRomBank())
        assertEquals(25, session.displaySnapshot().characterColumns)
        assertEquals(0, session.displaySnapshot().interCharacterColumnGap)
        assertEquals(0, session.displaySnapshot().interCharacterRowGap)
        assertIs<BasicProgramLoadResult.Success>(session.loadBasicProgram(byteArrayOf(0xff.toByte(), 0xff.toByte())))
        assertEquals("bank-0", session.resolveRomLocation(0x4000)?.componentId?.value)
        val controlWrite = assertIs<PgpMemoryDumpParseResult.Success>(
            PgpMemoryDumpParser.parse("2000 A5\n27FF 5A\n3FFF C3"),
        ).image
        assertEquals(MemoryImageLoadResult.Success(3, 3), session.loadMemoryImage(controlWrite))
        assertEquals(0xa5, session.memorySnapshot(0x2000, 1).copyBytes()[0].toInt() and 0xff)
        assertEquals(0x5a, session.memorySnapshot(0x27ff, 1).copyBytes()[0].toInt() and 0xff)
        assertEquals(0xc3, session.memorySnapshot(0x3fff, 1).copyBytes()[0].toInt() and 0xff)
    }

    @Test
    fun createsPc1350SessionWithFourLineDisplay() {
        val romSet = RomSet(
            Pc1350RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1350RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000)),
                RomComponent(Pc1350RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x8000)),
            ),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1350RomDefinition.MACHINE_ID, romSet),
        ).session

        assertEquals(Pc1350RomDefinition.MACHINE_ID, session.machineId)
        assertEquals(25, session.displaySnapshot().characterColumns)
        assertEquals(4, session.displaySnapshot().characterRows)
        assertEquals(150, session.displaySnapshot().dotColumns)
        assertEquals(0, session.displaySnapshot().interCharacterColumnGap)
        assertEquals(0, session.displaySnapshot().interCharacterRowGap)
        assertIs<BasicProgramLoadResult.Failure>(session.loadBasicProgram(byteArrayOf(0)))
    }

    @Test
    fun createsEveryPc1251FamilyModelFromTheSharedRomLayout() {
        Pc1251FamilyModel.entries.forEach { model ->
            val romSet = RomSet(
                model.machineId,
                listOf(
                    RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000) { 0x33 }),
                    RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
                ),
            )

            val session = assertIs<CreateSessionResult.Success>(
                EmulatorFactory.create(model.machineId, romSet),
            ).session

            assertEquals(model.machineId, session.machineId)
            assertEquals(24, session.displaySnapshot().characterColumns)
        }
    }

    @Test
    fun createsAPc1251SessionWithTwentyFourCharacterDisplay() {
        val romSet = RomSet(
            Pc1251RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000) { 0x33 }),
                RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000)),
            ),
        )
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1251RomDefinition.MACHINE_ID, romSet),
        ).session

        assertEquals(Pc1251RomDefinition.MACHINE_ID, session.machineId)
        assertEquals(24, session.displaySnapshot().characterColumns)
        assertEquals(listOf(DisplaySymbol.RUN), session.displaySnapshot().symbols)

        session.setOperatingMode(OperatingMode.RESERVE)
        assertEquals(listOf(DisplaySymbol.RESERVE), session.displaySnapshot().symbols)
        assertEquals(1, session.displaySnapshot().revision)

        session.setOperatingMode(OperatingMode.PROGRAM)
        assertEquals(listOf(DisplaySymbol.PRO), session.displaySnapshot().symbols)
        assertEquals(2, session.displaySnapshot().revision)
        assertEquals(ExecutionStatus.Ready, session.step().status)
    }

    @Test
    fun createsAHeadlessSessionFromAnImportedLegacyImage() {
        val image = ByteArray(Pc1245RomDefinition.LEGACY_IMAGE_SIZE).apply {
            this[0] = 0x02
            this[1] = 0x55
        }
        val imported = assertIs<RomImportResult.Success>(Pc1245FlatRomImporter.importImage(image))
        val created = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, imported.romSet),
        )

        val result = created.session.step()
        val snapshot = created.session.cpuSnapshot()

        assertEquals(4, result.cycles)
        assertEquals(ExecutionStatus.Ready, result.status)
        assertEquals(2, snapshot.programCounter)
        assertEquals(0x55, snapshot.copyInternalRam()[2].toInt() and 0xff)
    }

    @Test
    fun reportsUnsupportedAndMismatchedMachineIds() {
        val pc1245Set = validRomSet()
        val unsupported = assertIs<CreateSessionResult.Failure>(
            EmulatorFactory.create(MachineId("pc-9999"), pc1245Set),
        )
        assertIs<CreateSessionError.UnsupportedMachine>(unsupported.error)

        val mismatchedSet = RomSet(MachineId("pc-1251"), pc1245Set.components)
        val mismatch = assertIs<CreateSessionResult.Failure>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, mismatchedSet),
        )
        assertIs<CreateSessionError.MachineIdMismatch>(mismatch.error)
    }

    @Test
    fun reportsInvalidRomComponentsWithoutThrowing() {
        val invalid = RomSet(
            Pc1245RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(1)),
                RomComponent(
                    Pc1245RomDefinition.EXTERNAL_ID,
                    RomRole.EXTERNAL,
                    ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE),
                ),
            ),
        )

        val result = assertIs<CreateSessionResult.Failure>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, invalid),
        )

        assertIs<CreateSessionError.InvalidRomSet>(result.error)
    }

    @Test
    fun snapshotsDoNotExposeMutableCoreArrays() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session

        val cpuSnapshot = session.cpuSnapshot()
        val firstRamCopy = cpuSnapshot.copyInternalRam()
        firstRamCopy[0] = 0x7f

        val memorySnapshot = session.memorySnapshot(0, 2)
        val firstMemoryCopy = memorySnapshot.copyBytes()
        firstMemoryCopy[0] = 0x7f

        assertEquals(0, cpuSnapshot.copyInternalRam()[0].toInt())
        assertContentEquals(byteArrayOf(0x33, 0x00), memorySnapshot.copyBytes())
    }

    @Test
    fun memorySnapshotValidatesItsRange() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session

        assertEquals(0, session.memorySnapshot(0x10000 - 1, 0).size)
        assertFailsWith<IllegalArgumentException> { session.memorySnapshot(-1, 1) }
        assertFailsWith<IllegalArgumentException> { session.memorySnapshot(0, -1) }
        assertFailsWith<IllegalArgumentException> { session.memorySnapshot(0xffff, 2) }
    }

    @Test
    fun loadsAnAddressedMemoryImageAtomicallyAfterValidation() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session
        val image = assertIs<PgpMemoryDumpParseResult.Success>(
            PgpMemoryDumpParser.parse("C100 00010203\nC200 AABB"),
        ).image

        assertEquals(MemoryImageLoadResult.Success(2, 6), session.loadMemoryImage(image))
        assertContentEquals(byteArrayOf(0, 1, 2, 3), session.memorySnapshot(0xc100, 4).copyBytes())
        assertContentEquals(byteArrayOf(0xaa.toByte(), 0xbb.toByte()), session.memorySnapshot(0xc200, 2).copyBytes())
    }

    @Test
    fun rejectsTheWholeMemoryImageWhenItContainsARomAddress() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session
        val image = assertIs<PgpMemoryDumpParseResult.Success>(
            PgpMemoryDumpParser.parse("C100 AA\n4000 BB"),
        ).image

        assertEquals(
            MemoryImageLoadResult.Failure(MemoryImageLoadError.ReadOnlyAddress(0x4000, 2)),
            session.loadMemoryImage(image),
        )
        assertContentEquals(byteArrayOf(0), session.memorySnapshot(0xc100, 1).copyBytes())
    }

    @Test
    fun mapsUnsupportedOpcodesToThePublicFaultType() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet(firstOpcode = 0x16)),
        ).session

        val result = session.step()

        val status = assertIs<ExecutionStatus.Faulted>(result.status)
        val fault = assertIs<CoreFault.UnsupportedOpcode>(status.fault)
        assertEquals(0, fault.address)
        assertEquals(0x16, fault.opcode)
    }

    @Test
    fun exposesAnImmutableLogicalPc1245Display() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session

        val snapshot = session.displaySnapshot()
        val firstCopy = snapshot.copyDots()
        firstCopy[0] = 1

        assertEquals(16, snapshot.characterColumns)
        assertEquals(5, snapshot.characterWidth)
        assertEquals(80, snapshot.dotColumns)
        assertEquals(7, snapshot.dotRows)
        assertEquals(560, snapshot.copyDots().size)
        assertFalse(snapshot.isDotOn(0, 0))
        assertEquals(listOf(DisplaySymbol.RUN), snapshot.symbols)
        assertEquals(0, snapshot.revision)
        assertFailsWith<IllegalArgumentException> { snapshot.isDotOn(80, 0) }
        assertFailsWith<IllegalArgumentException> { snapshot.isDotOn(0, 7) }
    }

    @Test
    fun acceptsLogicalKeysAndUpdatesTheRunProgramIndicator() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session

        assertEquals(InputResult.Accepted, session.pressKey(PocketKey.A))
        assertEquals(InputResult.Accepted, session.releaseKey(PocketKey.A))
        session.setOperatingMode(OperatingMode.PROGRAM)

        val snapshot = session.displaySnapshot()
        assertEquals(listOf(DisplaySymbol.PRO), snapshot.symbols)
        assertEquals(1, snapshot.revision)

        session.reset()
        assertEquals(listOf(DisplaySymbol.RUN), session.displaySnapshot().symbols)
    }

    @Test
    fun exposesTheLogicalAudioSignalWithoutAPlatformAudioDependency() {
        val session = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, validRomSet()),
        ).session

        val snapshot = session.audioSnapshot()

        assertEquals(0, snapshot.frequencyHz)
        assertEquals(0, snapshot.revision)
    }

    private fun validRomSet(firstOpcode: Int = 0x33): RomSet {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE).apply {
            this[0] = firstOpcode.toByte()
        }
        return RomSet(
            Pc1245RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                RomComponent(
                    Pc1245RomDefinition.EXTERNAL_ID,
                    RomRole.EXTERNAL,
                    ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE),
                ),
            ),
        )
    }
}
