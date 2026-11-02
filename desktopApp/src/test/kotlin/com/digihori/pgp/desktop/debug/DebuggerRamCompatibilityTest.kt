package com.digihori.pgp.desktop.debug

import kotlin.test.*

class DebuggerRamCompatibilityTest {
    @Test fun oldNon1251LabelMeansHardwareButOld1251ExpansionIsNotTheNewMode() {
        for (id in listOf("pc-1251", "pc-1260", "pc-1261", "pc-1401", "pc-1402", "pc-1450")) {
            val saved = DebuggerSavedSettings(DebuggerSettingsTarget(id, "0".repeat(64), "EXPANDED", null),
                emptyList(), emptyList(), false, 256)
            val restored = DebuggerSettingsFiles.decode(DebuggerSettingsFiles.encode(saved))
            assertEquals(if (id == "pc-1251") "EXPANDED" else "HARDWARE", restored.target.memoryConfiguration)
        }
    }
}
