package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.source.basic.*
import kotlin.test.*

class Pc1450Test {
    private val model = Pc14xxModel.PC_1450
    private fun session(): EmulatorSession {
        val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model, ByteArray(0x2000), ByteArray(0x8000)))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom.romSet)).session
    }
    @Test fun romImportDiscardsRamAndChecksSize() {
        val image = ByteArray(0x10000) { (it ushr 8).toByte() }
        val rom = assertIs<RomImportResult.Success>(Pc14xxFlatRomImporter.importImage(model, image)).romSet
        assertEquals(2, rom.components.size)
        val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
        assertEquals(0, s.memorySnapshot(0x2030, 1).copyBytes()[0].toInt())
        assertEquals(0x80, s.memorySnapshot(0x8000, 1).copyBytes()[0].toInt() and 255)
        assertIs<RomImportResult.Failure>(Pc14xxFlatRomImporter.importImage(model, ByteArray(0x8000)))
    }
    @Test fun lcdSegmentsAndSymbolsUse1450Wiring() {
        val display = Pc14xxDisplay(model)
        display.writeMemory(0x7000, 1); display.writeMemory(0x703b, 2)
        display.writeMemory(0x7068, 4); display.writeMemory(0x707b, 8)
        display.writeMemory(0x703c, 15); display.writeMemory(0x703d, 127); display.writeMemory(0x707c, 127)
        val snapshot = display.snapshot()
        assertTrue(snapshot.isDotOn(0, 0)); assertTrue(snapshot.isDotOn(59, 1))
        assertTrue(snapshot.isDotOn(79, 2)); assertTrue(snapshot.isDotOn(60, 3))
        assertTrue(DisplaySymbol.KANA in snapshot.symbols); assertTrue(DisplaySymbol.CAL in snapshot.symbols)
        assertTrue(DisplaySymbol.PRINT in snapshot.symbols)
        display.setEnabled(false); assertFalse(display.snapshot().isDotOn(0, 0))
        display.reset(); assertTrue(display.snapshot().copyDots().all { it == 0.toByte() })
    }
    @Test fun memorySelectorAndIaScanIndependentlyAndBreakIsHeld() {
        val keyboard = Pc14xxKeyboard(model)
        keyboard.press(PocketKey.NUM_7); keyboard.press(PocketKey.Q); keyboard.press(PocketKey.CAL)
        assertEquals(1, keyboard.readInputA(0, 1)); assertEquals(32, keyboard.readInputA(0, 4))
        assertEquals(128, keyboard.readInputA(32, 1)); assertEquals(0, keyboard.readInputA(64, 0))
        keyboard.press(PocketKey.FORMAT); assertEquals(8, keyboard.readInputA(1, 0))
        keyboard.press(PocketKey.BREAK); repeat(100) { assertTrue(keyboard.consumeKeyOnSignal()) }
        keyboard.release(PocketKey.BREAK); assertFalse(keyboard.consumeKeyOnSignal())
        keyboard.reset(); assertEquals(0, keyboard.readInputA(0, 1))
    }
    @Test fun basicVocabularyAndMemoryPointersRoundTrip() {
        val bytes = assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(
            "10 DECI 1:FACT(5):OPEN\n20 PRINT \"ｶπ√\"\n30 END".encodeToByteArray(), model.machineId)).bytes
        assertTrue(bytes.any { it.toInt() and 255 == 0x84 }); assertTrue(bytes.any { it.toInt() and 255 == 0x90 })
        val decoded = assertIs<BasicProgramDecodeResult.Success>(BasicProgramCodec.detokenizeProgram(bytes, model.machineId))
        assertContentEquals(bytes, assertIs<BasicProgramCompileResult.Success>(BasicProgramCodec.compileProgram(decoded.utf8Bytes, model.machineId)).bytes)
        val s = session(); assertIs<BasicProgramLoadResult.Success>(s.loadBasicProgram(bytes))
        assertContentEquals(bytes, assertIs<BasicProgramSnapshotResult.Success>(s.basicProgramSnapshot()).copyBytes())
        assertContentEquals(byteArrayOf(0x30, 0x20), s.memorySnapshot(0x5f01, 2).copyBytes())
        assertEquals(MachineMemoryKind.LCD, s.memoryLayout.locate(0x7000).kind)
        assertFalse(s.memoryLayout.locate(0x8000).writable)
        assertIs<BasicProgramLoadResult.Failure>(s.loadBasicProgram(byteArrayOf(-1, 0, 1, 0, -1)))
    }
}
