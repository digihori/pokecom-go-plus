package com.digihori.pgp.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GamepadAxesTest {
    @Test
    fun sticksUseDifferentDiagonalRulesAndHoldAcrossTheDeadZone() {
        val sticks = GamepadAxes()
        assertTrue(sticks.update(.45f, -.45f, .45f, -.45f).isEmpty())
        val held = setOf(GamepadButton.RIGHT, GamepadButton.UP, GamepadButton.RIGHT_STICK_UP)
        assertEquals(held, sticks.update(.7f, -.7f, .7f, -.9f))
        assertEquals(held, sticks.update(.4f, -.4f, .4f, -.45f))
        assertTrue(sticks.update(.3f, -.3f, .3f, -.3f).isEmpty())
        assertEquals(setOf(GamepadButton.LEFT, GamepadButton.RIGHT_STICK_DOWN), sticks.update(-1f, 0f, 0f, 1f))
        sticks.reset()
        assertTrue(sticks.update(-.4f, 0f, 0f, .4f).isEmpty())
    }

    @Test
    fun triggersPressAndReleaseIndependentlyWithHysteresis() {
        val axes = GamepadAxes()
        assertEquals(setOf(GamepadButton.L2), axes.update(0f, 0f, 0f, 0f, .6f, .4f))
        assertEquals(setOf(GamepadButton.L2, GamepadButton.R2), axes.update(0f, 0f, 0f, 0f, .4f, .6f))
        assertEquals(setOf(GamepadButton.R2), axes.update(0f, 0f, 0f, 0f, .3f, .4f))
        axes.reset()
        assertTrue(axes.update(0f, 0f, 0f, 0f, .4f, .4f).isEmpty())
    }
}
