package com.digihori.pgp.player.android

import com.digihori.pgp.core.api.RamMode
import com.digihori.pgp.core.rom.MachineId
import org.junit.Test
import org.junit.Assert.assertEquals

class PlayerRamSettingsTest {
    @Test fun onlySupportedModelsCanUnlockRam() {
        for (model in listOf("1250", "1251", "1255", "1260", "1261", "1401", "1402")) {
            assertEquals(RamMode.UNRESTRICTED, playerRamMode(MachineId("pc-$model"), true))
            assertEquals(RamMode.HARDWARE, playerRamMode(MachineId("pc-$model"), false))
        }
        for (model in listOf("1245", "1350", "1360", "1450")) {
            assertEquals(RamMode.HARDWARE, playerRamMode(MachineId("pc-$model"), true))
        }
    }
}
