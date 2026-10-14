package com.digihori.pgp.desktop.input

import com.digihori.pgp.input.GamepadButton
import com.digihori.pgp.input.GamepadDevice
import com.digihori.pgp.input.SingleGamepadSelection
import org.lwjgl.sdl.SDLGamepad.*
import org.lwjgl.sdl.SDLInit.*
import org.lwjgl.sdl.SDLJoystick.SDL_SetJoystickEventsEnabled
import org.lwjgl.sdl.SDLEvents.SDL_PumpEvents
import org.lwjgl.sdl.SDLError.SDL_GetError
import org.lwjgl.sdl.SDLStdinc.SDL_free
import org.lwjgl.sdl.SDL_MainThreadCallback
import org.lwjgl.system.JNI
import org.lwjgl.system.Library
import java.awt.EventQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal data class DesktopGamepadSnapshot(
    val devices: List<GamepadDevice>,
    val selectedId: String?,
    val buttons: Set<GamepadButton>,
    val availableButtons: Set<GamepadButton>,
)

/** Input only: no SDL video window or audio subsystem. All native calls share one thread. */
internal class SdlGamepadBackend(private val unnamedDeviceName: String = "Gamepad") {
    private var initialized = false
    private var handle = 0L
    private var openedId: String? = null
    private val selection = SingleGamepadSelection()

    suspend fun poll(requestedId: String?): DesktopGamepadSnapshot = onNativeThread {
        if (!initialized) {
            check(SDL_Init(SDL_INIT_GAMEPAD)) { SDL_GetError().orEmpty() }
            initialized = true
            SDL_SetGamepadEventsEnabled(false)
            SDL_SetJoystickEventsEnabled(false)
        }
        SDL_PumpEvents()
        SDL_UpdateGamepads()
        val ids = SDL_GetGamepads() ?: error(SDL_GetError().orEmpty())
        val devices = try {
            (ids.position() until ids.limit()).map { index ->
                val id = ids[index]
                GamepadDevice(id.toString(), SDL_GetGamepadNameForID(id) ?: unnamedDeviceName)
            }
        } finally {
            SDL_free(ids)
        }
        val selectedId = selection.update(devices, requestedId)
        if (openedId != selectedId) {
            closeHandle()
            selectedId?.toIntOrNull()?.let { id ->
                handle = SDL_OpenGamepad(id)
                check(handle != 0L) { SDL_GetError().orEmpty() }
                openedId = selectedId
            }
        }
        val available = if (handle == 0L) emptySet() else buttonCodes.filterValues {
            SDL_GamepadHasButton(handle, it)
        }.keys
        val buttons = if (handle == 0L) emptySet() else buttonCodes.filterValues {
            SDL_GetGamepadButton(handle, it)
        }.keys
        DesktopGamepadSnapshot(devices, selectedId, buttons, available)
    }

    suspend fun close() = onNativeThread {
        closeHandle()
        if (initialized) SDL_QuitSubSystem(SDL_INIT_GAMEPAD)
        initialized = false
    }

    private fun closeHandle() {
        if (handle != 0L) SDL_CloseGamepad(handle)
        handle = 0L
        openedId = null
    }

    internal companion object {
        val buttonCodes = mapOf(
            GamepadButton.UP to SDL_GAMEPAD_BUTTON_DPAD_UP,
            GamepadButton.DOWN to SDL_GAMEPAD_BUTTON_DPAD_DOWN,
            GamepadButton.LEFT to SDL_GAMEPAD_BUTTON_DPAD_LEFT,
            GamepadButton.RIGHT to SDL_GAMEPAD_BUTTON_DPAD_RIGHT,
            GamepadButton.SOUTH to SDL_GAMEPAD_BUTTON_SOUTH,
            GamepadButton.EAST to SDL_GAMEPAD_BUTTON_EAST,
            GamepadButton.WEST to SDL_GAMEPAD_BUTTON_WEST,
            GamepadButton.NORTH to SDL_GAMEPAD_BUTTON_NORTH,
            GamepadButton.L1 to SDL_GAMEPAD_BUTTON_LEFT_SHOULDER,
            GamepadButton.R1 to SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER,
        )

        suspend fun <T> onNativeThread(action: () -> T): T {
            return if (System.getProperty("os.name").startsWith("Mac")) {
                MacGamepadThread.execute(action)
            } else {
                check(EventQueue.isDispatchThread()) { "SDL must run on the application event thread" }
                action()
            }
        }
    }
}

/** Cocoa main queue is distinct from AWT EDT. Never block EDT waiting for Cocoa. */
private object MacGamepadThread {
    private val dispatch = Library.loadNative(MacGamepadThread::class.java, "org.lwjgl", "/usr/lib/system/libdispatch.dylib")
    private val queue = dispatch.getFunctionAddress("_dispatch_main_q").also { check(it != 0L) }
    private val async = dispatch.getFunctionAddress("dispatch_async_f").also { check(it != 0L) }
    private val requests = ConcurrentHashMap<Long, () -> Unit>()
    private val nextId = AtomicLong()
    // One process-lifetime callback. It remains alive until every queued native call has returned.
    private val callback = SDL_MainThreadCallback.create { id -> requests.remove(id)?.invoke() }

    suspend fun <T> execute(action: () -> T): T = suspendCoroutine { continuation ->
        val id = nextId.incrementAndGet()
        requests[id] = { continuation.resumeWith(runCatching(action)) }
        try {
            JNI.invokePPPV(queue, id, callback.address(), async)
        } catch (failure: Throwable) {
            requests.remove(id)
            continuation.resumeWithException(failure)
        }
    }
}
