package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.source.basic.*
import kotlin.test.*

class Pc1460Test {
    private val model = Pc14xxModel.PC_1460
    @Test fun fourBanksSelectorMaskEventsResetAndRomProtection() {
        val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model, ByteArray(0x2000),
            ByteArray(0x10000) { (it / 0x4000 + 0x40).toByte() })).romSet
        val display = Pc14xxDisplay(model); val bus = Pc14xxMemoryBus(rom, model, display)
        repeat(4) { bank ->
            bus.write(0x3c00, bank or 0xfc)
            assertEquals(bank, bus.selectedBank); assertEquals(0x40 + bank, bus.read(0x4000)); assertEquals(0x40 + bank, bus.read(0x7fff))
            bus.write(0x4000, 0); assertEquals(0x40 + bank, bus.read(0x4000))
        }
        bus.write(0x3c01, 0); assertEquals(3, bus.selectedBank)
        assertEquals(3, bus.drainBankSwitchEvents().size); assertTrue(bus.drainBankSwitchEvents().isEmpty())
        bus.write(0x8000, 0x55); bus.write(0xffff, 0xaa)
        assertEquals(0x55, bus.read(0x8000)); assertEquals(0xaa, bus.read(0xffff))
        bus.resetRam(); assertEquals(0, bus.selectedBank); assertEquals(0, bus.read(0xffff))
        assertEquals(0x40, bus.read(0x4000))
        assertIs<RomImportResult.Failure>(Pc14xxComponentRomImporter.importImages(model, ByteArray(0x2000), ByteArray(0x20000)))
        val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
        assertEquals(0, s.selectedRomBank()); assertEquals(0, s.resolveRomLocation(0x4000)?.bank)
        assertNull(s.resolveRomLocation(0x8000))
    }
    @Test fun sixLcdSegmentsAreOrderedAndReversedAsWired() {
        val d = Pc14xxDisplay(model)
        listOf(0x3000 to 0, 0x301d to 29, 0x302d to 30, 0x303b to 44, 0x301e to 45,
            0x302c to 59, 0x306c to 60, 0x305e to 74, 0x307b to 75, 0x306d to 89,
            0x305d to 90, 0x3040 to 119).forEach { (address, column) ->
            d.reset(); d.writeMemory(address, 0x41)
            val s = d.snapshot(); assertTrue(s.isDotOn(column, 0)); assertTrue(s.isDotOn(column, 6))
            assertEquals(2, s.copyDots().count { it != 0.toByte() })
        }
        d.writeMemory(0x303c, 16); assertTrue(DisplaySymbol.MATRIX in d.snapshot().symbols)
        assertEquals(24, d.snapshot().characterColumns); assertEquals(120, d.snapshot().dotColumns)
    }
    @Test fun scanUsesBitSelectorAndIncludesFseAndStat() {
        val k = Pc14xxKeyboard(model)
        assertTrue(k.press(PocketKey.STAT)); assertEquals(4, k.readInputA(1, 0))
        k.release(PocketKey.STAT)
        assertTrue(k.press(PocketKey.FORMAT)); assertEquals(8, k.readInputA(1, 0))
        k.release(PocketKey.FORMAT)
        assertFalse(k.press(PocketKey.TITLE))
        k.press(PocketKey.NUM_1); assertEquals(1, k.readInputA(0, 4)); assertEquals(0, k.readInputA(0, 8))
        k.press(PocketKey.LOCK); assertEquals(64, k.readInputA(32, 0))
        k.press(PocketKey.BREAK); repeat(30) { assertTrue(k.consumeKeyOnSignal()) }; k.reset(); assertFalse(k.consumeKeyOnSignal())
    }
    @Test fun basic1460UsesHighRamPointersAndKanaS1Format() {
        val program = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram("10 PRINT \"ｱ\"\n20 END".encodeToByteArray(), model.machineId)).bytes
        assertTrue(program.toList().zipWithNext().any { (a, b) -> a.toInt() and 255 == 0xfe && b.toInt() and 255 == 0xb1 })
        val memory = ByteArray(65536)
        assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.Success>(Pc14xxBasicProgramMemory.load(model, program, { memory[it].toInt() and 255 }, { a,v -> memory[a] = v.toByte() }))
        assertContentEquals(byteArrayOf(0x30, 0x80.toByte()), memory.copyOfRange(0xff01, 0xff03))
        val result = assertIs<com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult.Success>(Pc14xxBasicProgramMemory.extract(model) { memory[it].toInt() and 255 })
        assertContentEquals(program, result.bytes)
    }
}
