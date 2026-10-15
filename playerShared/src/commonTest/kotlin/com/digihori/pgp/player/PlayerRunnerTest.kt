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

        assertEquals(1_920L, tick.budget.cycles)
        assertEquals(1_920L, tick.runResult?.executedCycles)
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

        assertEquals(1_920L, resumed.budget.cycles)
    }

    @Test
    fun resetLeavesTheRunnerPaused() {
        val runner = runner()
        runner.run(0L)

        runner.reset()

        assertEquals(PlayerRunState.PAUSED, runner.state)
    }

    @Test
    fun fixedSpeedsAreIndependentOfFrameRateAndKeepAudioRateAligned() {
        for (speed in PlayerExecutionSpeed.entries) for (hz in listOf(30, 60, 120)) {
            val runner = runner()
            runner.setSpeed(speed, 0)
            runner.run(0)
            var cycles = 0L
            var frames = 0
            var executed = 0L
            repeat(hz) { index ->
                val tick = runner.tick((index + 1L) * 1_000_000_000 / hz)
                cycles += tick.budget.cycles
                frames += tick.audio?.frameCount ?: 0
                executed += tick.runResult?.executedCycles ?: 0
                assertEquals(speed, tick.speed)
            }
            assertEquals(192_000L * speed.numerator / speed.denominator, cycles)
            val rate = speed.playbackRate(22_050)
            // Instructions finish atomically: actual cycles can exceed a tick's budget slightly.
            kotlin.test.assertTrue(kotlin.math.abs(executed - cycles) <= hz * 4L)
            kotlin.test.assertTrue(kotlin.math.abs(executed * 22_050 / 192_000 - frames) <= 1,
                "speed=$speed hz=$hz targetRate=$rate frames=$frames executed=$executed")
        }
    }

    @Test
    fun speedChangeDoesNotApplyNewRatioToPastTimeOrResumePausedCpu() {
        val runner = runner()
        runner.run(0)
        runner.tick(10_000_000)
        runner.setSpeed(PlayerExecutionSpeed.FASTEST, 20_000_000)
        assertEquals(2304L, runner.tick(30_000_000).budget.cycles)
        runner.pause()
        runner.setSpeed(PlayerExecutionSpeed.SLOWEST, 100_000_000)
        assertEquals(PlayerRunState.PAUSED, runner.state)
        runner.reset()
        assertEquals(PlayerExecutionSpeed.SLOWEST, runner.speed)
        runner.run(1_000_000_000)
        assertEquals(1536L, runner.tick(1_010_000_000).budget.cycles)
        assertEquals(PlayerExecutionSpeed.NORMAL, PlayerExecutionSpeed.fromId("invalid"))
        assertEquals(PlayerExecutionSpeed.NORMAL, PlayerExecutionSpeed.fromId(null))
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
