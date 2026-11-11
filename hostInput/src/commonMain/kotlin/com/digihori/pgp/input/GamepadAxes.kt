package com.digihori.pgp.input

import kotlin.math.abs

/** Normalized axes, with positive X to the right and positive Y down and triggers increasing from 0 to 1. */
class GamepadAxes {
    private var horizontal = 0
    private var vertical = 0
    private var right: GamepadButton? = null

    private var l2 = false
    private var r2 = false

    fun reset() {
        horizontal = 0
        vertical = 0
        right = null
        l2 = false
        r2 = false
    }

    fun update(leftX: Float, leftY: Float, rightX: Float, rightY: Float,
        leftTrigger: Float = 0f, rightTrigger: Float = 0f): Set<GamepadButton> {
        l2 = triggerPressed(leftTrigger.finite(), l2)
        r2 = triggerPressed(rightTrigger.finite(), r2)
        horizontal = direction(leftX, horizontal)
        vertical = direction(leftY, vertical)
        val x = rightX.finite()
        val y = rightY.finite()
        val previousStrength = when (right) {
            GamepadButton.RIGHT_STICK_UP -> -y
            GamepadButton.RIGHT_STICK_DOWN -> y
            GamepadButton.RIGHT_STICK_LEFT -> -x
            GamepadButton.RIGHT_STICK_RIGHT -> x
            else -> 0f
        }
        right = when {
            abs(x) >= PRESS && abs(x) >= abs(y) ->
                if (x < 0) GamepadButton.RIGHT_STICK_LEFT else GamepadButton.RIGHT_STICK_RIGHT
            abs(y) >= PRESS ->
                if (y < 0) GamepadButton.RIGHT_STICK_UP else GamepadButton.RIGHT_STICK_DOWN
            previousStrength > RELEASE -> right
            else -> null
        }
        return buildSet {
            if (horizontal < 0) add(GamepadButton.LEFT)
            if (horizontal > 0) add(GamepadButton.RIGHT)
            if (vertical < 0) add(GamepadButton.UP)
            if (vertical > 0) add(GamepadButton.DOWN)
            right?.let { add(it) }
            if (l2) add(GamepadButton.L2)
            if (r2) add(GamepadButton.R2)
        }
    }

    private fun direction(value: Float, previous: Int): Int {
        val axis = value.finite()
        return when {
            axis <= -PRESS -> -1
            axis >= PRESS -> 1
            axis * previous > RELEASE -> previous
            else -> 0
        }
    }

    private fun triggerPressed(value: Float, held: Boolean): Boolean =
        value >= PRESS || (held && value > RELEASE)

    private fun Float.finite(): Float = if (isFinite()) this else 0f

    private companion object {
        const val PRESS = .55f
        const val RELEASE = .35f
    }
}
