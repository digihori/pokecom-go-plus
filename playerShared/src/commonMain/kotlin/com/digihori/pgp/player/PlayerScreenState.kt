package com.digihori.pgp.player

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.rom.MachineId

/** Platform-independent top-level states rendered by a Player application. */
public sealed interface PlayerScreenState {
    public data object RomMissing : PlayerScreenState
    public data object Loading : PlayerScreenState

    public data class Emulator(
        public val machineId: MachineId,
        public val runState: PlayerRunState,
        public val presentationMode: PresentationMode,
        public val operatingMode: OperatingMode,
        public val supportedOperatingModes: Set<OperatingMode>,
    ) : PlayerScreenState

    public data class Failure(public val kind: PlayerFailureKind) : PlayerScreenState
}

public enum class PlayerFailureKind {
    ROM_READ,
    ROM_VALIDATION,
    SESSION_CREATION,
    STORAGE,
}

public enum class PresentationMode {
    FULL_DEVICE,
    PLAYABLE,
    LANDSCAPE,
    CONTROLLER_DISPLAY,

    ;

    /** Thin Player toggle shared by Android and future iOS hosts. */
    public fun toggleControllerDisplay(): PresentationMode =
        if (this == CONTROLLER_DISPLAY) FULL_DEVICE else CONTROLLER_DISPLAY
}
