package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.source.basic.*
import kotlin.test.*

class Pc1470Test {
    private val model = Pc14xxModel.PC_1470U
    @Test fun arrowAndFseUseDistinctIaBitsWithoutIndependentStatKey() {
        val k = Pc14xxKeyboard(Pc14xxModel.PC_1470U)
        assertTrue(k.press(PocketKey.TITLE)); assertEquals(4, k.readInputA(1, 0))
        k.release(PocketKey.TITLE)
        assertTrue(k.press(PocketKey.FORMAT)); assertEquals(8, k.readInputA(1, 0))
        k.release(PocketKey.FORMAT); assertEquals(0, k.readInputA(1, 0))
        assertFalse(k.press(PocketKey.STAT))
    }
    @Test fun eightBanksAndWholeSelectorAperture() {
        val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model, ByteArray(0x2000),
            ByteArray(0x20000) { (it / 0x4000 + 0x40).toByte() })).romSet
        val bus = Pc14xxMemoryBus(rom, model, Pc14xxDisplay(model))
        repeat(8) { bank ->
            bus.write(0x3400 + bank, bank or 0xf8)
            assertEquals(bank, bus.selectedBank); assertEquals(0x40 + bank, bus.read(0x4000)); assertEquals(0x40 + bank, bus.read(0x7fff))
        }
        bus.write(0x35ff, 0xfa); assertEquals(2, bus.selectedBank)
        bus.write(0x3600, 7); assertEquals(2, bus.selectedBank)
        bus.write(0x4000, 0); assertEquals(0x42, bus.read(0x4000))
        assertEquals(8, bus.drainBankSwitchEvents().size)
        bus.write(0x8000, 1); bus.write(0xe000, 2)
        assertEquals(1, bus.read(0x8000)); assertEquals(2, bus.read(0xe000)) // no commented-out mirror
        bus.resetRam(); assertEquals(0, bus.selectedBank); assertEquals(0, bus.read(0x8000))
    }
    @Test fun lcdFourHalvesAndDistinctStatusRegisterBits() {
        val d = Pc14xxDisplay(model)
        listOf(0x2800 to (0 to 0), 0x283b to (59 to 0), 0x2a00 to (60 to 0), 0x2a3b to (119 to 0),
            0x2840 to (0 to 7), 0x287b to (59 to 7), 0x2a40 to (60 to 7), 0x2a7b to (119 to 7)).forEach { (address, cell) ->
            d.reset(); d.writeMemory(address, 1); assertTrue(d.snapshot().isDotOn(cell.first, cell.second))
            assertEquals(1, d.snapshot().copyDots().count { it != 0.toByte() })
        }
        d.writeMemory(0x283c, 0x7f); d.writeMemory(0x283d, 0x0f); d.writeMemory(0x287c, 0x7f); d.writeMemory(0x287d, 0x0f)
        val s = d.snapshot(); assertEquals(2, s.characterRows); assertEquals(14, s.dotRows)
        assertTrue(DisplaySymbol.DBL in s.symbols); assertTrue(DisplaySymbol.AER in s.symbols)
        assertTrue(DisplaySymbol.MATRIX in s.symbols); assertTrue(DisplaySymbol.DE in s.symbols)
        assertEquals(22, s.symbols.size)
    }
    @Test fun numericSelectorAndEditingKeysAre1470uSpecific() {
        val k = Pc14xxKeyboard(model)
        k.press(PocketKey.NUM_1); assertEquals(1, k.readInputA(0, 3)); assertEquals(0, k.readInputA(0, 4))
        k.press(PocketKey.DELETE); assertEquals(16, k.readInputA(0, 1))
        k.press(PocketKey.BACKSPACE); assertEquals(16, k.readInputA(0, 2))
        k.press(PocketKey.INSERT); assertEquals(17, k.readInputA(0, 3))
        assertFalse(k.press(PocketKey.LOCK)); assertFalse(k.press(PocketKey.MEMORY_STORE))
        assertEquals(0, k.readInputA(0, 0)); assertEquals(0, k.readInputA(0, 8))
        k.press(PocketKey.CAL); assertEquals(128, k.readInputA(32, 0))
        k.press(PocketKey.BREAK); repeat(100) { assertTrue(k.consumeKeyOnSignal()) }; k.release(PocketKey.BREAK); assertFalse(k.consumeKeyOnSignal())
    }
    @Test fun s2KeywordsLineReferencesKanaAndMachineSpecificVocabulary() {
        val source = "10 PRINT \"ｶπ\":GOTO 20\n20 DEFDBL A:FACT(5):END".encodeToByteArray()
        val bytes = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(source, model.machineId)).bytes
        assertTrue(bytes.toList().zipWithNext().any { (a,b) -> a.toInt() and 255 == 0xfe && b.toInt() and 255 == 0x46 })
        assertTrue(bytes.any { it.toInt() and 255 == 0x1f })
        val decoded = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, model.machineId))
        assertContentEquals(bytes, assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(decoded.utf8Bytes, model.machineId)).bytes)
        // GCursor belongs to 1360, not the confirmed 1470U vocabulary.
        val unknown = byteArrayOf(-1, 0, 10, 3, 0xfe.toByte(), 0x68, 0x0d, -1)
        assertIs<BasicProgramDecodeResult.GenericFailure>(BasicProgramCodec.detokenizeProgram(unknown, model.machineId))
        assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(unknown, com.digihori.pgp.core.rom.MachineId("pc-1360")))
    }
}
