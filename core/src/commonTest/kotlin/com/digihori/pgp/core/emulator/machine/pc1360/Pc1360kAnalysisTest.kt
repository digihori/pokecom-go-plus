package com.digihori.pgp.core.emulator.machine.pc1360

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.*

class Pc1360kAnalysisTest {
    @Test
    fun cpuAccessRecordingCapturesRepeatedSelectorWrites() {
        // LIDP 3600; LIA 1; STD; STD; LIA 0; STD.
        val program = byteArrayOf(0x10, 0x36, 0x00, 0x02, 0x01, 0x52, 0x52, 0x02, 0x00, 0x52)
        val internal = ByteArray(0x2000).also { program.copyInto(it) }
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        val rom = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(internal, ByteArray(0x20000), id)).romSet
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session
        session.setMemoryAccessTracing(true)
        repeat(6) { assertIs<ExecutionStatus.Ready>(session.step().status) }
        val writes = session.drainMemoryAccesses().filter { it.kind == MemoryAccessKind.WRITE && it.address == 0x3600 }
        assertEquals(listOf(1, 1, 0), writes.map { it.value })
    }

    @Test
    fun analysisSessionRetainsIdentityWorkRamAndIndependentRomControl() {
        val id = Pc1360RomDefinition.PC1360K_MACHINE_ID
        assertTrue(id in EmulatorFactory.supportedMachineIds())
        val imported = assertIs<RomImportResult.Success>(Pc1360LegacyRomImporter.importImages(
            ByteArray(0x2000), ByteArray(0x20000) { (it / 0x4000).toByte() }, id,
        ))
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, imported.romSet)).session
        assertEquals(id, session.machineId)
        assertEquals(MachineMemoryKind.RAM, session.memoryLayout.locate(0x2000).kind)
        val bus = Pc1360MemoryBus(imported.romSet)
        bus.write(0x2000, 0x5a)
        bus.write(0x27ff, 0xa5)
        bus.write(0x3400, 3)
        bus.write(0x3600, 1)
        assertEquals(3, bus.selectedBank)
        assertEquals(3, bus.read(0x4000))
        assertEquals(0x5a, bus.read(0x2000))
        assertEquals(0xa5, bus.read(0x27ff))
        bus.resetRam()
        assertEquals(0, bus.read(0x3600))
        assertEquals(0, bus.read(0x2000))
        assertIs<CreateSessionResult.Failure>(EmulatorFactory.create(Pc1360RomDefinition.MACHINE_ID, imported.romSet))
    }
}
