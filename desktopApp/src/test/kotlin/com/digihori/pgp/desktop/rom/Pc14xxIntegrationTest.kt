package com.digihori.pgp.desktop.rom

import androidx.compose.ui.input.key.Key
import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxModel
import com.digihori.pgp.desktop.display.DesktopLcdGeometry
import com.digihori.pgp.desktop.input.DesktopKeyMapper
import com.digihori.pgp.desktop.input.Pc14xxKeyboardLayout
import com.digihori.pgp.player.PlayerKeyboard
import kotlin.test.*

class Pc14xxIntegrationTest {
    private fun packageFor(model: Pc14xxModel): ByteArray = assertIs<DesktopRomPackageConversionResult.Success>(
        DesktopRomPackageConverter.createPackage(model.machineId, ByteArray(0x2000),
            ByteArray(if (model.bankCount == 0) 0x8000 else model.bankCount * 0x4000))).packageBytes
    private fun keyboard(model: Pc14xxModel): PlayerKeyboard = when (model) {
        Pc14xxModel.PC_1450 -> Pc14xxKeyboardLayout.pc1450
        Pc14xxModel.PC_1460 -> Pc14xxKeyboardLayout.pc1460
        Pc14xxModel.PC_1470U -> Pc14xxKeyboardLayout.pc1470u
    }
    @Test fun packagesLoadOnlyForTheirOwnModelAndAllShownKeysAreAccepted() {
        for (model in Pc14xxModel.entries) {
            val bytes = packageFor(model)
            val read = assertIs<DesktopRomPackageReadResult.Success>(DesktopRomPackage.read(bytes))
            assertEquals(if (model.bankCount == 0) 2 else model.bankCount + 1, read.romSet.components.size)
            val session = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(bytes)).session
            val panel = checkNotNull(Pc14xxKeyboardLayout.forLayout(MachineCatalog.require(model.machineId).keyboardLayout))
            assertSame(keyboard(model), panel)
            for (cap in (panel.rows + checkNotNull(panel.functionPanel).rows).flatten()) {
                assertIs<InputResult.Accepted>(session.pressKey(cap.key), "$model ${cap.key}")
                assertIs<InputResult.Accepted>(session.releaseKey(cap.key))
            }
            for (other in Pc14xxModel.entries.filter { it != model })
                assertIs<DesktopRomLoadResult.Failure>(DesktopRomLoader.loadPackageForMachine(bytes, other.machineId))
        }
    }
    @Test fun functionPanelsMatchThePhotographedThreeBySixLayout() {
        val expected = listOf(
            listOf(PocketKey.HYP, PocketKey.SIN, PocketKey.COS, PocketKey.TAN, PocketKey.FORMAT, PocketKey.CLEAR),
            listOf(PocketKey.HEX, PocketKey.DEGREES, PocketKey.LN, PocketKey.LOG, PocketKey.RECIPROCAL, PocketKey.STAT),
            listOf(PocketKey.EXPONENT, PocketKey.POWER, PocketKey.ROOT, PocketKey.SQUARE, PocketKey.LEFT_PAREN, PocketKey.RIGHT_PAREN),
        )
        for (model in Pc14xxModel.entries) {
            val panel = checkNotNull(keyboard(model).functionPanel)
            assertEquals(6, panel.columnCount)
            assertEquals(3, panel.rows.size)
            assertEquals(18, panel.rows.flatten().map { it.key }.toSet().size)
            panel.rows.forEachIndexed { index, row ->
                assertEquals(expected[index].map { if (model == Pc14xxModel.PC_1470U && it == PocketKey.STAT) PocketKey.TITLE else it }, row.map { it.key })
                assertEquals((0..5).toList(), row.map { it.column })
            }
            assertEquals(if (model == Pc14xxModel.PC_1450) "F↔E" else "FSE", panel.rows[0][4].primaryLabel)
            assertEquals("↕︎", panel.rows[1][5].primaryLabel)
            assertEquals(if (model == Pc14xxModel.PC_1470U) "TITLE" else "STAT", panel.rows[1][5].shiftedLabel)
            assertEquals("TAB", panel.rows[0][4].shiftedLabel)
        }
        assertEquals("STAT", keyboard(Pc14xxModel.PC_1470U).rows.flatten().single { it.key == PocketKey.EQUALS }.shiftedLabel)
    }
    @Test fun bankedLegacyFormsNormalizeAndRejectMissingOrWrongBankFiles() {
        for (model in listOf(Pc14xxModel.PC_1460, Pc14xxModel.PC_1470U)) {
            val bankBytes = ByteArray(model.bankCount * 0x4000) { (it / 0x4000).toByte() }
            for (size in listOf(0x2000, 0x10000)) {
                val converted = assertIs<DesktopRomPackageConversionResult.Success>(DesktopRomPackageConverter.convertLegacyImage(model.machineId, ByteArray(size), bankBytes))
                val session = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(converted.packageBytes)).session
                assertEquals(0, session.selectedRomBank())
            }
            assertIs<DesktopRomPackageConversionResult.Failure>(DesktopRomPackageConverter.convertLegacyImage(model.machineId, ByteArray(0x2000)))
            assertIs<DesktopRomLoadResult.Failure>(DesktopRomLoader.loadLegacyImage(model.machineId, ByteArray(0x2000), bankImage = ByteArray(1)))
            assertEquals(model.bankCount * 0x4000L, DesktopRomSetCreator.externalSize(model.machineId))
        }
    }
    @Test fun logicalLcdCellsAreUnchangedWhile1460PhysicalDotsAreNarrower() {
        for (model in Pc14xxModel.entries) {
            val s = assertIs<DesktopRomLoadResult.Success>(DesktopRomLoader.loadPackage(packageFor(model))).session.displaySnapshot()
            assertEquals(5, s.characterWidth); assertEquals(7, s.characterHeight)
            val logicalColumns = model.characterColumns * 6 - 1
            val logicalRows = model.characterRows * 8 - 1
            val ratio = DesktopLcdGeometry.panelAspectRatio(model.machineId, s)
            val physicalDotRatio = ratio * logicalRows / logicalColumns
            assertEquals(if (model == Pc14xxModel.PC_1460) 0.75f else 1f, physicalDotRatio, 0.0001f)
        }
        assertEquals(PocketKey.BACKSPACE, DesktopKeyMapper.map(Key.Backspace, Pc14xxModel.PC_1470U.machineId))
        assertEquals(PocketKey.DELETE, DesktopKeyMapper.map(Key.Delete, Pc14xxModel.PC_1470U.machineId))
        assertEquals(PocketKey.CLEAR, DesktopKeyMapper.map(Key.Backspace, Pc14xxModel.PC_1450.machineId))
    }
}
