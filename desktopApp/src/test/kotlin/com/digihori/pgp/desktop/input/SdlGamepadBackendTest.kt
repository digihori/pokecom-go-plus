package com.digihori.pgp.desktop.input

import com.digihori.pgp.input.GamepadButton
import kotlinx.coroutines.CoroutineDispatcher
import java.awt.EventQueue
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.lwjgl.sdl.SDLJoystick.*
import org.lwjgl.sdl.SDLGamepad.*
import org.lwjgl.sdl.SDL_VirtualJoystickDesc
import org.lwjgl.system.MemoryStack
import kotlin.test.*
import org.junit.Assume.assumeTrue

class SdlGamepadBackendTest {
    /** Opt in on a desktop with a GUI. Exercises the real native library without requiring a pad. */
    @Test
    fun virtualGamepadExercisesNativeDetectionButtonsAndDisconnect() {
        assumeTrue("Set PGP_TEST_GAMEPAD_NATIVE=1 on a desktop with a GUI",
            System.getenv("PGP_TEST_GAMEPAD_NATIVE") == "1")
        runBlocking {
            withContext(object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) = EventQueue.invokeLater(block)
            }) {
                val backend = SdlGamepadBackend()
                var joystick = 0L
                var id = 0
                try {
                    backend.poll(null)
                    id = SdlGamepadBackend.onNativeThread {
                        MemoryStack.stackPush().use { stack ->
                            val descriptor = SDL_VirtualJoystickDesc.calloc(stack)
                            SDL_INIT_INTERFACE(descriptor)
                            descriptor.type(SDL_JOYSTICK_TYPE_GAMEPAD.toShort())
                                .naxes(6).axis_mask(63)
                                .nbuttons(SDL_GAMEPAD_BUTTON_COUNT.toShort())
                                .button_mask((1 shl SDL_GAMEPAD_BUTTON_COUNT) - 1)
                                .name(stack.UTF8("PGP test controller"))
                            SDL_AttachVirtualJoystick(descriptor).also { assertTrue(it != 0) }
                        }
                    }
                    joystick = SdlGamepadBackend.onNativeThread { SDL_OpenJoystick(id) }
                    assertTrue(joystick != 0L)
                    val neutral = backend.poll(id.toString())
                    assertEquals(id.toString(), neutral.selectedId)
                    assertTrue(neutral.devices.any { it.id == id.toString() })
                    assertTrue(neutral.buttons.isEmpty())
                    assertEquals(GamepadButton.entries.toSet(), neutral.availableButtons)
                    SdlGamepadBackend.onNativeThread {
                        for (axis in 0 until 6) assertTrue(SDL_SetJoystickVirtualAxis(joystick, axis, Short.MAX_VALUE))
                    }
                    assertTrue(backend.poll(id.toString()).buttons.isEmpty(), "Analog axes must not type keys")
                    SdlGamepadBackend.onNativeThread {
                        assertTrue(SDL_SetJoystickVirtualButton(joystick, SDL_GAMEPAD_BUTTON_DPAD_LEFT, true))
                        assertTrue(SDL_SetJoystickVirtualButton(joystick, SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, true))
                    }
                    assertEquals(setOf(GamepadButton.LEFT, GamepadButton.L1), backend.poll(id.toString()).buttons)
                    SdlGamepadBackend.onNativeThread {
                        SDL_SetJoystickVirtualButton(joystick, SDL_GAMEPAD_BUTTON_DPAD_LEFT, false)
                        SDL_SetJoystickVirtualButton(joystick, SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, false)
                    }
                    assertTrue(backend.poll(id.toString()).buttons.isEmpty())
                    SdlGamepadBackend.onNativeThread {
                        SdlGamepadBackend.buttonCodes.values.forEach { SDL_SetJoystickVirtualButton(joystick, it, true) }
                    }
                    assertEquals(GamepadButton.entries.toSet(), backend.poll(id.toString()).buttons)
                    SdlGamepadBackend.onNativeThread {
                        SDL_CloseJoystick(joystick)
                        joystick = 0L
                        assertTrue(SDL_DetachVirtualJoystick(id))
                        id = 0
                    }
                    assertNull(backend.poll(neutral.selectedId).selectedId)
                } finally {
                    SdlGamepadBackend.onNativeThread {
                        if (joystick != 0L) SDL_CloseJoystick(joystick)
                        if (id != 0) SDL_DetachVirtualJoystick(id)
                    }
                    backend.close()
                }
            }
        }
    }
}
