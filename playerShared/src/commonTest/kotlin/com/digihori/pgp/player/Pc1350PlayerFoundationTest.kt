package com.digihori.pgp.player

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.input.HostKeyState
import kotlin.test.*

class Pc1350PlayerFoundationTest {
    @Test fun factoryProvidesPausedSessionAndEveryPhysicalKeyReachesCore() {
        val player = session()
        assertEquals(Pc1350RomDefinition.MACHINE_ID, player.machineId)
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        val caps = assertNotNull(player.keyboard()).rows.flatten()
        assertEquals(62, caps.size)
        assertEquals(61, caps.map { it.key }.toSet().size)
        assertEquals(62, caps.map { it.id }.toSet().size)
        caps.forEach {
            assertIs<InputResult.Accepted>(player.pressKey(it.key), it.id)
            assertIs<InputResult.Accepted>(player.releaseKey(it.key), it.id)
        }
        assertIs<InputResult.UnsupportedKey>(player.pressKey(PocketKey.CAL))
        assertTrue(PlayerMachineCatalog.definitions.any { it.id == player.machineId })
    }

    @Test fun twoShiftOwnersKeepLogicalKeyPressedUntilBothRelease() {
        val player = session()
        val events = mutableListOf<String>()
        val input = HostKeyState(
            { key -> events += "press:$key"; player.pressKey(key) },
            { key -> events += "release:$key"; player.releaseKey(key) },
        )
        val shifts = player.keyboard()!!.rows.flatten().filter { it.key == PocketKey.SHIFT }
        assertEquals(setOf("SHIFT_LEFT", "SHIFT_RIGHT"), shifts.map { it.id }.toSet())
        shifts.forEach { input.press("skin:${it.id}", it.key) }
        input.release("skin:${shifts[0].id}")
        assertTrue(input.isHeld(PocketKey.SHIFT))
        assertEquals(listOf("press:SHIFT"), events)
        input.release("skin:${shifts[1].id}")
        assertFalse(input.isHeld(PocketKey.SHIFT))
        assertEquals(listOf("press:SHIFT", "release:SHIFT"), events)
    }

    @Test fun originalInstructionsExerciseFourRowsAndSnapshotSymbolsWithoutRom() {
        // LIDP address; LIA value; STD. Each physical VRAM row has five disjoint blocks.
        val program = buildList<Int> {
            fun write(address: Int, value: Int) { addAll(listOf(0x10, address shr 8, address and 255, 0x02, value, 0x52)) }
            listOf(0x7000, 0x7040, 0x701e, 0x705e).forEach { write(it, 0x01) }
            listOf(0x781d, 0x785d, 0x783b, 0x787b).forEach { write(it, 0x80) }
            write(0x783c, 0xf3) // Exercise all six exported flags, not a real operating mode.
            val loop = size
            addAll(listOf(0x4d, 0x79, loop shr 8, loop and 255))
        }.map(Int::toByte).toByteArray()
        val player = session(program)
        val before = player.displayFrame()
        player.runner.run(0)
        val tick = player.runner.tick(10_000_000)
        assertEquals(2560L, tick.budget.cycles)
        assertIs<ExecutionStatus.Ready>(tick.runResult?.status)
        val frame = player.displayFrame()
        assertEquals(25, frame.characterColumns)
        assertEquals(4, frame.characterRows)
        assertEquals(6, frame.characterWidth)
        assertEquals(150, frame.dotColumns)
        assertEquals(32, frame.dotRows)
        assertEquals(150, frame.logicalColumns)
        assertEquals(32, frame.logicalRows)
        assertEquals(150f / 32f, frame.logicalAspectRatio)
        repeat(4) { row ->
            assertTrue(frame.isDotOn(0, row * 8))
            assertTrue(frame.isDotOn(149, row * 8 + 7))
            assertFalse(frame.isDotOn(1, row * 8))
            assertFalse(before.isDotOn(0, row * 8))
        }
        assertEquals(149, frame.logicalX(149))
        assertEquals(31, frame.logicalY(31))
        assertEquals(setOf(DisplaySymbol.SHIFT, DisplaySymbol.DEF, DisplaySymbol.RUN,
            DisplaySymbol.PRO, DisplaySymbol.KANA, DisplaySymbol.SMALL), frame.symbols.toSet())
        player.runner.pause()
        assertNull(player.runner.tick(1_000_000_000).runResult)
        player.reset()
        assertEquals(PlayerRunState.PAUSED, player.runner.state)
        assertTrue(player.displayFrame().symbols.isEmpty())
        val reset = player.displayFrame()
        repeat(32) { row -> repeat(150) { column -> assertFalse(reset.isDotOn(column, row)) } }
        assertTrue(frame.isDotOn(0, 0)) // Earlier snapshots remain independent after reset.
    }

    private fun session(program: ByteArray = byteArrayOf(0x79, 0, 0)): PlayerSession {
        val internal = ByteArray(Pc1350RomDefinition.INTERNAL_SIZE) { 0x4d }
        program.copyInto(internal)
        val rom = RomSet(Pc1350RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1350RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
            RomComponent(Pc1350RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL,
                ByteArray(Pc1350RomDefinition.EXTERNAL_SIZE) { 0x4d }),
        ))
        return assertIs<PlayerSessionCreationResult.Success>(PlayerSessionFactory.create(rom)).session
    }
}
