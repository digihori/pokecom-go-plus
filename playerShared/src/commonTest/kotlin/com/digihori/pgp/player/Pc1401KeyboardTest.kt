package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1401.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.input.GamepadKeys
import kotlin.test.*

class Pc1401KeyboardTest {
    @Test fun panelsCoverEveryPhysicalKeyExactlyOnceAndFitTheirGrids() {
        val keyboard = assertNotNull(PlayerKeyboardCatalog.forMachine(Pc1401RomDefinition.MACHINE_ID))
        val functions = assertNotNull(keyboard.functionPanel)
        assertEquals(6, functions.columnCount); assertEquals(3, functions.rows.size)
        assertEquals(18, functions.rows.flatten().size)
        val caps = keyboard.rows.flatten() + functions.rows.flatten()
        val keys = caps.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
        assertEquals(GamepadKeys.forMachine(Pc1401RomDefinition.MACHINE_ID), keys.toSet())
        val imported = assertIs<RomImportResult.Success>(Pc1401ComponentRomImporter.importImages(ByteArray(0x2000), ByteArray(0x8000)))
        val core = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1401RomDefinition.MACHINE_ID, imported.romSet)).session
        keys.forEach { assertIs<InputResult.Accepted>(core.pressKey(it)); assertIs<InputResult.Accepted>(core.releaseKey(it)) }
        for (panel in listOf(keyboard, functions)) {
            panel.rows.forEach { row ->
                val occupied = row.flatMap { it.column until it.column + it.columnSpan }
                assertEquals(occupied.size, occupied.distinct().size)
                assertTrue(occupied.all { it in 0 until panel.columnCount })
            }
        }
    }
}
