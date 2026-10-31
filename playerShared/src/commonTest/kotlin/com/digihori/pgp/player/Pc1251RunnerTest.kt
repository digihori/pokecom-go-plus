package com.digihori.pgp.player

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class Pc1251RunnerTest {
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

    @Test
    fun longInstructionsDoNotAccumulateExtraAudioAcrossTicks() {
        // Independently authored: enable 2 kHz output, then WAIT 255; JP 0006.
        // WAIT can overrun the cycle budget at a tick boundary.
        for (speed in PlayerExecutionSpeed.entries) for (hz in listOf(30, 60, 120)) {
            val runner = runner(byteArrayOf(0x12, 0x5f, 0x02, 0x20, 0xdb.toByte(), 0xdf.toByte(),
                0x4e, 0xff.toByte(), 0x79, 0x00, 0x06))
            runner.setSpeed(speed, 0)
            runner.run(0)
            var executed = 0L
            var frames = 0L
            var audibleFrames = 0L
            repeat(hz * 10) { index ->
                val tick = runner.tick((index + 1L) * 1_000_000_000 / hz)
                executed += tick.runResult?.executedCycles ?: 0
                frames += tick.audio?.frameCount ?: 0
                audibleFrames += tick.audio?.sampleCount ?: 0
            }
            kotlin.test.assertTrue(audibleFrames > 0)
            val expectedCycles = 1_920_000L * speed.numerator / speed.denominator
            kotlin.test.assertTrue(executed in expectedCycles..expectedCycles + 261,
                "speed=$speed hz=$hz executed=$executed expected=$expectedCycles")
            val expectedFrames = speed.playbackRate(22_050) * 10L
            kotlin.test.assertTrue(kotlin.math.abs(frames - expectedFrames) <= 31,
                "speed=$speed hz=$hz frames=$frames expected=$expectedFrames")
        }
    }

    @Test
    fun overshootCanSkipTinyTicksAndTimelineChangesClearIt() {
        val runner = runner(byteArrayOf(0x4e, 0xff.toByte(), 0x79, 0x00, 0x00))
        runner.run(0)
        val first = runner.tick(10_000) // One cycle budget, full 261-cycle WAIT.
        assertEquals(261L, first.runResult?.executedCycles)
        assertNull(runner.tick(20_000).runResult)
        assertNull(runner.tick(20_000).audio)
        runner.pause()
        runner.run(1_000_000)
        kotlin.test.assertNotNull(runner.tick(1_010_000).runResult)
        runner.reset()
        runner.run(2_000_000)
        assertEquals(261L, runner.tick(2_010_000).runResult?.executedCycles)
        runner.setSpeed(PlayerExecutionSpeed.FASTEST, 2_010_000)
        kotlin.test.assertNotNull(runner.tick(2_020_000).runResult)
    }

    private fun runner(program: ByteArray? = null): PlayerRunner {
        val internal = ByteArray(Pc1251RomDefinition.INTERNAL_SIZE) { 0x4d.toByte() }
        program?.copyInto(internal)
        val external = ByteArray(Pc1251RomDefinition.EXTERNAL_SIZE) { 0x4d.toByte() }
        val romSet = RomSet(
            Pc1251RomDefinition.MACHINE_ID,
            listOf(
                RomComponent(Pc1251RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                RomComponent(Pc1251RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, external),
            ),
        )
        val created = assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(Pc1251RomDefinition.MACHINE_ID, romSet),
        )
        return PlayerRunner(created.session)
    }
}
