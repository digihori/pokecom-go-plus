package com.digihori.pgp.player.android

import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.RamMode

internal fun supportsPlayerRamUnlock(id: MachineId): Boolean = id.value in setOf(
    "pc-1250", "pc-1251", "pc-1255", "pc-1260", "pc-1261", "pc-1401", "pc-1402",
)

internal fun playerRamMode(id: MachineId, enabled: Boolean): RamMode =
    if (enabled && supportsPlayerRamUnlock(id)) RamMode.UNRESTRICTED else RamMode.HARDWARE
