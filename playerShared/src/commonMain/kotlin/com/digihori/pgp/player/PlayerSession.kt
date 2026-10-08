package com.digihori.pgp.player

import com.digihori.pgp.core.api.CreateSessionError
import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.api.EmulatorSession
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.rom.RomSet

/**
 * Owns the single emulator session used by a Player screen.
 *
 * Calls are intentionally unsynchronized. A platform host must serialize access on its UI or
 * emulation thread. Platform lifecycle, clocks, persistence, and rendering stay outside this type.
 */
public class PlayerSession internal constructor(
    private val coreSession: EmulatorSession,
) {
    public val runner: PlayerRunner = PlayerRunner(coreSession)
    public val machineId: MachineId get() = coreSession.machineId
    public val supportedOperatingModes: Set<OperatingMode> =
        MachineCatalog.require(machineId).supportedOperatingModes
    public var operatingMode: OperatingMode = OperatingMode.RUN
        private set

    public fun displayFrame(): PlayerDisplayFrame = PlayerDisplayFrame(coreSession.displaySnapshot())

    public fun keyboard(): PlayerKeyboard? = PlayerKeyboardCatalog.forMachine(machineId)

    public fun pressKey(key: PocketKey): InputResult = coreSession.pressKey(key)

    public fun releaseKey(key: PocketKey): InputResult = coreSession.releaseKey(key)

    public fun setOperatingMode(mode: OperatingMode): Boolean {
        if (mode !in supportedOperatingModes) return false
        coreSession.setOperatingMode(mode)
        operatingMode = mode
        return true
    }

    public fun reset() {
        runner.reset()
        operatingMode = OperatingMode.RUN
    }

    public fun screenState(
        presentationMode: PresentationMode = PresentationMode.FULL_DEVICE,
    ): PlayerScreenState.Emulator = PlayerScreenState.Emulator(
        machineId = machineId,
        runState = runner.state,
        presentationMode = presentationMode,
        operatingMode = operatingMode,
        supportedOperatingModes = supportedOperatingModes,
    )
}

public object PlayerSessionFactory {
    public fun create(romSet: RomSet): PlayerSessionCreationResult =
        when (val created = EmulatorFactory.create(romSet.machineId, romSet)) {
            is CreateSessionResult.Success -> PlayerSessionCreationResult.Success(
                PlayerSession(created.session),
            )
            is CreateSessionResult.Failure -> PlayerSessionCreationResult.Failure(created.error)
        }
}

public sealed interface PlayerSessionCreationResult {
    public data class Success(public val session: PlayerSession) : PlayerSessionCreationResult
    public data class Failure(public val error: CreateSessionError) : PlayerSessionCreationResult
}
