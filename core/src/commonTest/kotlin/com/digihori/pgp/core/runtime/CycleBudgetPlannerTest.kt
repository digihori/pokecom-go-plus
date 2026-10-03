package com.digihori.pgp.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CycleBudgetPlannerTest {
    @Test
    fun convertsElapsedTimeAtThePc1245Rate() {
        val planner = pc1245Planner(maximumCatchUpNanoseconds = 1_000_000_000)

        assertEquals(288_000, planner.plan(1_000_000_000).cycles)
        assertEquals(4_800, planner.plan(16_666_667).cycles)
    }

    @Test
    fun carriesFractionalCyclesBetweenFrames() {
        val planner = CycleBudgetPlanner(
            cyclesPerSecond = 3,
            maximumCatchUpNanoseconds = 1_000_000_000,
        )

        assertEquals(0, planner.plan(100_000_000).cycles)
        assertEquals(0, planner.plan(100_000_000).cycles)
        assertEquals(1, planner.plan(200_000_000).cycles)
    }

    @Test
    fun capsCatchUpAfterALongHostStall() {
        val planner = pc1245Planner(maximumCatchUpNanoseconds = 100_000_000)

        val budget = planner.plan(2_000_000_000)

        assertEquals(28_800, budget.cycles)
        assertEquals(100_000_000, budget.acceptedElapsedNanoseconds)
        assertEquals(1_900_000_000, budget.droppedElapsedNanoseconds)
    }

    @Test
    fun appliesRationalSpeedMultipliers() {
        val planner = pc1245Planner(maximumCatchUpNanoseconds = 1_000_000_000)

        assertEquals(576_000, planner.plan(1_000_000_000, SpeedRatio.DOUBLE).cycles)
        assertEquals(144_000, planner.plan(1_000_000_000, SpeedRatio.HALF).cycles)
    }

    @Test
    fun changingSpeedOrResettingDropsTheOldFraction() {
        val planner = CycleBudgetPlanner(
            cyclesPerSecond = 3,
            maximumCatchUpNanoseconds = 1_000_000_000,
        )

        planner.plan(300_000_000)
        assertEquals(0, planner.plan(100_000_000, SpeedRatio.DOUBLE).cycles)
        planner.reset()
        assertEquals(0, planner.plan(300_000_000).cycles)
    }

    @Test
    fun zeroElapsedTimeProducesNoWork() {
        val planner = pc1245Planner()

        assertEquals(CycleBudget(0, 0, 0), planner.plan(0))
    }

    @Test
    fun rejectsInvalidConfigurationAndInputs() {
        assertFailsWith<IllegalArgumentException> { CycleBudgetPlanner(0) }
        assertFailsWith<IllegalArgumentException> { CycleBudgetPlanner(1, 0) }
        assertFailsWith<IllegalArgumentException> { SpeedRatio(0, 1) }
        assertFailsWith<IllegalArgumentException> { SpeedRatio(1, 0) }
        assertFailsWith<IllegalArgumentException> { pc1245Planner().plan(-1) }
    }

    private fun pc1245Planner(
        maximumCatchUpNanoseconds: Long = 100_000_000,
    ): CycleBudgetPlanner = CycleBudgetPlanner(
        cyclesPerSecond = CycleBudgetPlanner.PC1245_CYCLES_PER_SECOND,
        maximumCatchUpNanoseconds = maximumCatchUpNanoseconds,
    )
}
