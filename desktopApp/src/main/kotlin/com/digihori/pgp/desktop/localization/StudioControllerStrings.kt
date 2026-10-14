package com.digihori.pgp.desktop.localization

import com.digihori.pgp.controller.ui.ControllerStrings
import com.digihori.pgp.input.GamepadButton

internal fun StudioStrings.controllerStrings(): ControllerStrings = ControllerStrings(
    title = text("controller.title"),
    pressed = text("controller.pressed"),
    released = text("controller.released"),
    enable = text("controller.enable"),
    disconnected = text("controller.disconnected"),
    selectDevice = text("controller.selectDevice"),
    instructions = text("controller.instructions"),
    positions = text("controller.positions"),
    saveHelp = text("controller.saveHelp"),
    unavailable = text("controller.unavailable"),
    unavailableShort = text("controller.unavailableShort"),
    unassigned = text("controller.unassigned"),
    unassignedShort = text("controller.unassignedShort"),
    buttonLabel = { button ->
        when (button) {
            GamepadButton.NORTH -> text("controller.north")
            GamepadButton.SOUTH -> text("controller.south")
            GamepadButton.EAST -> text("controller.east")
            GamepadButton.WEST -> text("controller.west")
            else -> button.label
        }
    },
    assignment = { text("controller.assignment", it) },
    close = text("common.close"),
)
