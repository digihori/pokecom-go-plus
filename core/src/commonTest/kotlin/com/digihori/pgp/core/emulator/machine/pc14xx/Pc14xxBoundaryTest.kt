package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class Pc14xxBoundaryTest {
    @Test fun basicCapacityMalformedLinesAndBadPointersAreAtomicForEachModel() {
        for (model in Pc14xxModel.entries) {
            val memory = ByteArray(65536)
            val start = model.basicLimit - 2
            memory[model.basicPointer] = start.toByte(); memory[model.basicPointer + 1] = (start ushr 8).toByte()
            val before = memory.copyOf()
            val read: (Int) -> Int = { memory[it].toInt() and 255 }
            val write: (Int, Int) -> Unit = { a,v -> memory[a] = v.toByte() }
            val valid = byteArrayOf(-1, 0, 10, 2, 0x41, 0x0d, -1)
            assertIs<Pc1245BasicMemoryResult.TooLarge>(Pc14xxBasicProgramMemory.load(model, valid, read, write))
            assertContentEquals(before, memory)
            assertIs<Pc1245BasicMemoryResult.InvalidProgram>(Pc14xxBasicProgramMemory.load(model, byteArrayOf(-1, 0, 10, 0, -1), read, write))
            assertContentEquals(before, memory)
            assertIs<Pc1245BasicMemoryResult.Success>(Pc14xxBasicProgramMemory.load(model, byteArrayOf(-1, -1), read, write))
            assertIs<Pc1245BasicMemoryResult.Success>(Pc14xxBasicProgramMemory.extract(model, read))
            val end = model.basicLimit
            memory[model.basicPointer + 2] = end.toByte(); memory[model.basicPointer + 3] = (end ushr 8).toByte()
            assertIs<Pc1245BasicMemoryResult.InvalidPointers>(Pc14xxBasicProgramMemory.extract(model, read))
        }
    }
    @Test fun cpuWritesSelectorThenExecutesTheSelectedBankAndTracesPhysicalRom() {
        for (model in listOf(Pc14xxModel.PC_1460, Pc14xxModel.PC_1470U)) {
            val selector = if (model == Pc14xxModel.PC_1460) 0x3c00 else 0x35ff
            // LIDP selector; LIA last bank; STD; JP 4000. Original synthetic probe.
            val code = byteArrayOf(0x10, (selector ushr 8).toByte(), selector.toByte(), 0x02,
                (model.bankCount - 1).toByte(), 0x52, 0x79, 0x40, 0)
            val internal = ByteArray(0x2000).also { code.copyInto(it) }
            val bankImage = ByteArray(model.bankCount * 0x4000).also { banks ->
                repeat(model.bankCount) { bank -> banks[bank * 0x4000] = 0x02; banks[bank * 0x4000 + 1] = bank.toByte() }
            }
            val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model, internal, bankImage)).romSet
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
            repeat(5) { session.step() }
            assertEquals(model.bankCount - 1, session.selectedRomBank())
            assertEquals(model.bankCount - 1, session.cpuSnapshot().copyInternalRam()[2].toInt() and 255)
            assertEquals(model.bankCount - 1, session.resolveRomLocation(0x4000)?.bank)
            assertEquals(0, session.resolveRomLocation(0x4000)?.offset)
            assertEquals(1, session.drainBankSwitchEvents().size)
            session.reset(); assertEquals(0, session.selectedRomBank())
        }
    }
    @Test fun ramBoundaryLoadFailureDoesNotPartiallyWriteAndBadComponentRolesAreRejected() {
        for (model in Pc14xxModel.entries) {
            val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model, ByteArray(0x2000),
                ByteArray(if (model.bankCount == 0) 0x8000 else model.bankCount * 0x4000))).romSet
            val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
            val end = if (model.bankCount == 0) 0x7fff else 0x3fff
            val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
                listOf(AddressedMemorySegmentData(end, byteArrayOf(1,2))))).image
            assertIs<MemoryImageLoadResult.Failure>(s.loadMemoryImage(image))
            assertEquals(0, s.memorySnapshot(end,1).copyBytes()[0].toInt())
            val bad = RomSet(model.machineId, rom.components.map { component ->
                if (component.id.value == "internal") RomComponent(component.id, RomRole.EXTERNAL, component.copyBytes()) else component
            })
            assertIs<CreateSessionResult.Failure>(EmulatorFactory.create(model.machineId, bad))
        }
    }
}
