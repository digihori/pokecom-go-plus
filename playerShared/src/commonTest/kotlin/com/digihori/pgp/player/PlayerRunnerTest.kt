package com.digihori.pgp.player

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PlayerRunnerTest {
    @Test
    fun runsFromHostSuppliedMonotonicTime() {
        val runner = runner()

        runner.run(1_000_000_000L)
        val tick = runner.tick(1_010_000_000L)

        assertEquals(2_880L, tick.budget.cycles)
        assertEquals(2_880L, tick.runResult?.executedCycles)
        assertEquals(PlayerRunState.RUNNING, runner.state)
    }

    @Test
    fun pauseDiscardsElapsedHostTime() {
        val runner = runner()

        runner.run(0L)
        runner.pause()
        assertNull(runner.tick(10_000_000_000L).runResult)
        runner.run(10_000_000_000L)
        val resumed = runner.tick(10_010_000_000L)

        assertEquals(2_880L, resumed.budget.cycles)
    }

    @Test
    fun resetLeavesTheRunnerPaused() {
        val runner = runner()
        runner.run(0L)

        runner.reset()

        assertEquals(PlayerRunState.PAUSED, runner.state)
    }

    private fun runner(): PlayerRunner {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d.toByte() }
        val external = ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d.toByte() }
        val romSet = RomSet(
            Pc1245RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
            ),
        )
        val created = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, romSet),
        )
        return PlayerRunner(created.session)
    }
}
