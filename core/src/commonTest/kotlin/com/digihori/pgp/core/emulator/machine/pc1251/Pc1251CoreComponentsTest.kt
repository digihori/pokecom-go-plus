package com.digihori.pgp.core.emulator.machine.pc1251

import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Pc1251CoreComponentsTest {
    @Test
    fun importsSeparatePhysicalRomDumps() {
        val internal = ByteArray(0x2000) { 0x11 }
        val external = ByteArray(0x4000) { 0x22 }

        val result = assertIs<RomImportResult.Success>(Pc1251ComponentRomImporter.importImages(internal, external))

        assertEquals(Pc1251RomDefinition.MACHINE_ID, result.romSet.machineId)
        assertContentEquals(internal, result.romSet.component(Pc1251RomDefinition.INTERNAL_ID)?.copyBytes())
        assertContentEquals(external, result.romSet.component(Pc1251RomDefinition.EXTERNAL_ID)?.copyBytes())
    }

    @Test
    fun mapsOnlyThePhysicalRamAndLcdRanges() {
        val bus = Pc1251MemoryBus(
            romSet(machineId = Pc1251FamilyModel.PC_1255.machineId),
            model = Pc1251FamilyModel.PC_1255,
        )

        bus.write(0xb000, 0x12)
        bus.write(0xb800, 0x23)
        bus.write(0xc7ff, 0x34)
        bus.write(0xa000, 0x56)
        bus.write(0xf800, 0x78)

        assertEquals(0x12, bus.read(0xb000))
        assertEquals(0x23, bus.read(0xb800))
        assertEquals(0x34, bus.read(0xc7ff))
        assertEquals(0x56, bus.read(0xa000))
        assertEquals(0x78, bus.read(0xf800))
    }

    @Test
    fun hardwareMemoryModeUsesEachModelsInstalledRam() {
        val ranges = mapOf(
            Pc1251FamilyModel.PC_1250 to (0xa000 to 0xc000),
            Pc1251FamilyModel.PC_1251 to (0xa000 to 0xb800),
            Pc1251FamilyModel.PC_1255 to (null to 0xa000),
        )

        ranges.forEach { (model, addresses) ->
            val (unavailable, start) = addresses
            val bus = Pc1251MemoryBus(
                romSet(machineId = model.machineId),
                model = model,
                memoryMode = Pc1251FamilyMemoryMode.HARDWARE,
            )
            unavailable?.let { bus.write(it, 0x55) }
            bus.write(start, 0x66)

            unavailable?.let { assertEquals(0, bus.read(it), model.name) }
            assertEquals(0x66, bus.read(start), model.name)
        }
    }

    @Test
    fun expandedMemoryModeGivesEveryFamilyModelThePc1255Range() {
        val bus = Pc1251MemoryBus(
            romSet(machineId = Pc1251FamilyModel.PC_1250.machineId),
            model = Pc1251FamilyModel.PC_1250,
            memoryMode = Pc1251FamilyMemoryMode.EXPANDED,
        )

        bus.write(0xa000, 0x55)

        assertEquals(0x55, bus.read(0xa000))
    }

    @Test
    fun mapsTheScratchRamAliasesUsedByMachineLanguageSoundRoutines() {
        val bus = Pc1251MemoryBus(
            romSet(machineId = Pc1251FamilyModel.PC_1255.machineId),
            model = Pc1251FamilyModel.PC_1255,
        )

        bus.write(0x8000, 0x7f)
        assertEquals(0x7f, bus.read(0x8000))
        assertEquals(0x7f, bus.read(0xa000))

        bus.write(0x9000, 0x35)
        assertEquals(0x35, bus.read(0x9000))
        assertEquals(0x35, bus.read(0xb000))

        bus.write(0xd000, 0x42)
        assertEquals(0x42, bus.read(0xc000))
        assertEquals(0x42, bus.read(0xd000))
    }

    @Test
    fun keepsTheB000PartialMirrorOnPc1251ButNotPc1255() {
        val pc1251 = Pc1251MemoryBus(romSet())
        pc1251.write(0xb000, 0x12)
        assertEquals(0x12, pc1251.read(0xb800))

        val pc1255 = Pc1251MemoryBus(
            romSet(machineId = Pc1251FamilyModel.PC_1255.machineId),
            model = Pc1251FamilyModel.PC_1255,
        )
        pc1255.write(0xb000, 0x34)
        pc1255.write(0xb800, 0x56)
        assertEquals(0x34, pc1255.read(0xb000))
        assertEquals(0x56, pc1255.read(0xb800))
    }

    @Test
    fun pc1250UsesThePc1245RamMirrorsAroundC000() {
        val pc1250 = Pc1251MemoryBus(
            romSet(machineId = Pc1251FamilyModel.PC_1250.machineId),
            model = Pc1251FamilyModel.PC_1250,
            memoryMode = Pc1251FamilyMemoryMode.HARDWARE,
        )

        pc1250.write(0xb000, 0x12)
        assertEquals(0x12, pc1250.read(0xc000))
        assertEquals(0x12, pc1250.read(0xd000))

        pc1250.write(0xb800, 0x34)
        assertEquals(0x34, pc1250.read(0xc000))
        assertEquals(0x34, pc1250.read(0xd000))
    }

    @Test
    fun mapsTheExternalRomReadAlias() {
        val external = ByteArray(0x4000).apply {
            this[0] = 0x12
            this[0x1fff] = 0x34
        }
        val bus = Pc1251MemoryBus(romSet(external))

        assertEquals(0x12, bus.read(0x2000))
        assertEquals(0x34, bus.read(0x3fff))
        assertEquals(0x12, bus.read(0x4000))
        assertEquals(0x34, bus.read(0x5fff))
    }

    @Test
    fun mapsAllLowerAndUpperLcdMirrorPagesToF800() {
        val display = Pc1251Display()
        val bus = Pc1251MemoryBus(romSet(), display)

        bus.write(0xea12, 0x55)

        assertEquals(0x55, bus.read(0xe812))
        assertEquals(0x55, bus.read(0xef12))
        assertEquals(0x55, bus.read(0xf812))
        assertEquals(0x55, bus.read(0xf912))
        assertEquals(0x55, bus.read(0xff12))
        assertEquals(0x55, display.copyDotColumns()[0x12].toInt() and 0xff)

        bus.write(0xfa13, 0x66)

        assertEquals(0x66, bus.read(0xe813))
        assertEquals(0x66, bus.read(0xf813))
        assertEquals(0x66, bus.read(0xff13))
        assertEquals(0x66, display.copyDotColumns()[0x13].toInt() and 0xff)
    }

    @Test
    fun mapsTheTwoLcdHalvesIntoTwentyFourCharacters() {
        val display = Pc1251Display()

        display.writeMemory(0xf800, 0x01)
        display.writeMemory(0xf83b, 0x02)
        display.writeMemory(0xf87b, 0x04)
        display.writeMemory(0xf840, 0x08)

        val columns = display.copyDotColumns()
        assertEquals(120, columns.size)
        assertEquals(0x01, columns[0].toInt())
        assertEquals(0x02, columns[59].toInt())
        assertEquals(0x04, columns[60].toInt())
        assertEquals(0x08, columns[119].toInt())
    }

    @Test
    fun tracksOnlyTheTwoPc1251SymbolBytes() {
        val display = Pc1251Display()

        display.writeMemory(0xf83c, 0x0f)
        display.writeMemory(0xf83d, 0x07)
        val revision = display.revision
        display.writeMemory(0xf83e, 0xff)

        assertEquals(0x0f, display.symbolState0())
        assertEquals(0x07, display.symbolState1())
        assertEquals(2, revision)
        assertEquals(revision, display.revision)
    }

    private fun romSet(
        external: ByteArray = ByteArray(0x4000),
        machineId: com.digihori.pgp.core.rom.MachineId = Pc1251RomDefinition.MACHINE_ID,
    ): RomSet = RomSet(
        machineId,
        listOf(
            RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, ByteArray(0x2000)),
            RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
        ),
    )
}
