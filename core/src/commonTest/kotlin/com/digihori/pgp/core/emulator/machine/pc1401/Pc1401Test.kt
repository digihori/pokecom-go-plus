package com.digihori.pgp.core.emulator.machine.pc1401

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.*
import com.digihori.pgp.core.emulator.machine.pc1350.*
import com.digihori.pgp.core.source.basic.*
import com.digihori.pgp.core.source.machine.*
import kotlin.test.*

class Pc1401Test {
    private fun rom() = assertIs<RomImportResult.Success>(
        Pc1401ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)),
    ).romSet

    @Test fun flatImporterDiscardsRamAndOwnsComponents() {
        val flat = ByteArray(0x10000) { 0x5a }
        flat[0] = 0x12; flat[0x8000] = 0x34
        val imported = assertIs<RomImportResult.Success>(Pc1401FlatRomImporter.importImage(flat)).romSet
        flat.fill(0)
        assertEquals(0x12, imported.component(Pc1401RomDefinition.INTERNAL_ID)!!.copyBytes()[0].toInt())
        assertEquals(0x34, imported.component(Pc1401RomDefinition.EXTERNAL_ID)!!.copyBytes()[0].toInt())
        assertIs<RomImportResult.Failure>(Pc1401FlatRomImporter.importImage(ByteArray(0x8000)))
        assertIs<RomImportResult.Failure>(Pc1401ComponentRomImporter.importImages(ByteArray(1), ByteArray(0x8000)))
    }

    @Test fun physicalRamHolesAndLcdAliasesAreDistinct() {
        val display = Pc1401Display(); val bus = Pc1401MemoryBus(rom(), display)
        bus.write(0x3800, 0x12); bus.write(0x47ff, 0x34)
        assertEquals(0x12, bus.read(0x3800)); assertEquals(0x34, bus.read(0x47ff))
        for (address in listOf(0, 0x2000, 0x37ff, 0x4800, 0x5fff, 0x6800, 0x7800, 0xffff)) {
            val before = bus.read(address); bus.write(address, 0x91); assertEquals(before, bus.read(address))
            assertFalse(bus.isWritableAddress(address))
        }
        bus.write(0x7167, 0x40)
        assertEquals(0x40, bus.read(0x6067)); assertEquals(0x40, bus.read(0x6767))
        assertEquals(1, display.snapshot().copyDots()[6 * 80 + 40].toInt())
        bus.resetRam(); assertEquals(0, bus.read(0x3800)); assertEquals(0, bus.read(0x6067))
    }

    @Test fun lcdOrderingSymbolsEnableAndSnapshots() {
        val display = Pc1401Display()
        display.writeMemory(0x6000, 1); display.writeMemory(0x6027, 2)
        display.writeMemory(0x6067, 4); display.writeMemory(0x6040, 0x40)
        display.writeMemory(0x603c, 8); display.writeMemory(0x603d, 0x7f); display.writeMemory(0x607c, 0x7f)
        val before = display.snapshot(); val dots = before.copyDots()
        assertEquals(1, dots[0].toInt()); assertEquals(1, dots[80 + 39].toInt())
        assertEquals(1, dots[160 + 40].toInt()); assertEquals(1, dots[480 + 79].toInt())
        assertTrue(listOf(DisplaySymbol.CAL, DisplaySymbol.HYP, DisplaySymbol.STAT,
            DisplaySymbol.MEMORY, DisplaySymbol.PARENTHESES).all { it in before.symbols })
        display.setEnabled(false); display.reset()
        assertTrue(before.copyDots().contentEquals(dots)); assertTrue(before.symbols.isNotEmpty())
        assertTrue(display.snapshot().revision > before.revision)
        assertTrue(display.snapshot().copyDots().all { it == 0.toByte() })
    }

    @Test fun keyboardSeparatesFunctionMainAndBreakAndSessions() {
        val keyboard = Pc1401Keyboard(); val other = Pc1401Keyboard()
        assertTrue(keyboard.press(PocketKey.SIN)); assertEquals(0x10, keyboard.readInputA(0, 0x20))
        keyboard.press(PocketKey.SHIFT); assertEquals(0x50, keyboard.readInputA(0, 0x20))
        keyboard.release(PocketKey.SIN); assertEquals(0x40, keyboard.readInputA(0, 0x20))
        keyboard.press(PocketKey.NUM_0); assertEquals(0x80, keyboard.readInputA(0x40, 0))
        assertEquals(0, other.readInputA(0x40, 0)); assertFalse(keyboard.press(PocketKey.KANA))
        keyboard.press(PocketKey.BREAK); repeat(100) { assertTrue(keyboard.consumeKeyOnSignal()) }
        keyboard.release(PocketKey.BREAK)
        assertFalse(keyboard.consumeKeyOnSignal()); keyboard.reset(); assertEquals(0, keyboard.readInputA(0x40, 0))
    }

    @Test fun dedicatedTokensSpecialSymbolsAndComparatorBytesRoundTrip() {
        val document = assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(
            "10 PRINT FAC(5),HSN(0),π,√4\n20 IF A<=B THEN 30\n30 REM FAC HSN",
        )).document
        val bytes = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1401BasicTokenizer.tokenize(document)).bytes
        val values = bytes.map { it.toInt() and 255 }
        assertTrue(0x90 in values); assertTrue(0x8a in values); assertTrue(0xfb in values); assertTrue(0xfc in values)
        val decoded = assertIs<Pc1350BasicDetokenizeResult.Success>(Pc1401BasicTokenizer.detokenize(bytes)).source
        assertTrue("FAC(5)" in decoded); assertTrue("A<=B" in decoded); assertTrue("REM FAC HSN" in decoded)
        val again = assertIs<Pc1350BasicTokenizeResult.Success>(Pc1401BasicTokenizer.tokenize(
            assertIs<BasicTextParseResult.Success>(BasicTextParser.parse(decoded)).document,
        )).bytes
        assertContentEquals(bytes, again)
        assertIs<Pc1350BasicTokenizeResult.Failure>(Pc1401BasicTokenizer.tokenize(
            assertIs<BasicTextParseResult.Success>(BasicTextParser.parse("10 PRINT \"ｱ\"")).document,
        ))
        assertEquals(listOf(PocketKey.SHIFT, PocketKey.O), Pc1401CharacterInput.keySequence(':'))
        assertEquals(listOf(PocketKey.LEFT_PAREN), Pc1401CharacterInput.keySequence('('))
    }

    @Test fun malformedAndOversizeProgramsDoNotPartiallyOverwriteRam() {
        val bus = Pc1401MemoryBus(rom(), Pc1401Display())
        val valid = byteArrayOf(-1, 0, 10, 2, 0xd8.toByte(), 0x0d, -1)
        assertIs<Pc1245BasicMemoryResult.Success>(Pc1401BasicProgramMemory.load(valid, bus::read, bus::write))
        val before = ByteArray(0x1000) { bus.read(0x3800 + it).toByte() }
        val malformed = valid.copyOf().also { it[5] = 0 }
        assertIs<Pc1245BasicMemoryResult.InvalidProgram>(Pc1401BasicProgramMemory.load(malformed, bus::read, bus::write))
        val huge = buildList<Byte> {
            add(-1); repeat(1000) { add(0); add(10); add(2); add(0xd8.toByte()); add(0x0d) }; add(-1)
        }.toByteArray()
        assertIs<Pc1245BasicMemoryResult.TooLarge>(Pc1401BasicProgramMemory.load(huge, bus::read, bus::write))
        assertContentEquals(before, ByteArray(0x1000) { bus.read(0x3800 + it).toByte() })
    }

    @Test fun sessionRejectsMemoryImageAcrossHoleAtomicallyAndResolvesRom() {
        val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, rom())).session
        val image = assertIs<PgpMemoryDumpParseResult.Success>(PgpMemoryDumpParser.parse("47FF: 12 34")).image
        assertIs<MemoryImageLoadResult.Failure>(session.loadMemoryImage(image))
        assertEquals(0, session.memorySnapshot(0x47ff, 1).copyBytes()[0].toInt())
        assertEquals(Pc1401RomDefinition.EXTERNAL_ID, session.resolveRomLocation(0x9234)!!.componentId)
        assertEquals(0x1234, session.resolveRomLocation(0x9234)!!.offset)
    }
}
