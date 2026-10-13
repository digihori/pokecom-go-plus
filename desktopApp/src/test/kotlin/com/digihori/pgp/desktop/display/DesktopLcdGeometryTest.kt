package com.digihori.pgp.desktop.display

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import kotlin.test.*

class DesktopLcdGeometryTest {
    @Test fun allCatalogDisplaysHaveSquareDotsExcept1460AtEveryPanelWidth() {
        for (machine in MachineCatalog.definitions) {
            val components = mutableListOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000)))
            val bankCount = (machine.memoryBanking as? MachineMemoryBanking.Banked)?.bankCount ?: 0
            if (bankCount > 0) repeat(bankCount) {
                components += RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000))
            } else components += RomComponent(RomComponentId("external"), RomRole.EXTERNAL,
                ByteArray(if (machine.generation == MachineGeneration.OLD) 0x4000 else 0x8000))
            val session = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(machine.id, RomSet(machine.id, components))).session
            val snapshot = session.displaySnapshot()
            val columns = CharacterCellGeometry.visualColumnCount(snapshot.characterColumns, snapshot.characterWidth, snapshot.interCharacterColumnGap)
            val rows = CharacterCellGeometry.visualRowCount(snapshot.characterRows, snapshot.characterHeight, snapshot.interCharacterRowGap)
            val aspect = DesktopLcdGeometry.panelAspectRatio(machine.id, snapshot)
            val expected = if (machine.id.value == "pc-1460") 0.75f else 1f
            for (width in listOf(320f, 640f, 1000f)) {
                val dotWidth = width / columns
                val dotHeight = (width / aspect) / rows
                assertEquals(expected, dotWidth / dotHeight, 0.0001f, machine.id.value)
                assertEquals(snapshot.characterWidth.toFloat() / snapshot.characterHeight * expected,
                    dotWidth * snapshot.characterWidth / (dotHeight * snapshot.characterHeight), 0.0001f, machine.id.value)
            }
            assertContentEquals(snapshot.copyDots(), session.displaySnapshot().copyDots())
        }
    }
}
