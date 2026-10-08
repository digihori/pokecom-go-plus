package com.digihori.pgp.player

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlayerSessionTest {
    @Test
    fun `factory creates one paused player session from a rom set`() {
        val created = assertIs<PlayerSessionCreationResult.Success>(
            PlayerSessionFactory.create(pc1245RomSet()),
        )

        assertEquals(Pc1245RomDefinition.MACHINE_ID, created.session.machineId)
        assertEquals(PlayerRunState.PAUSED, created.session.runner.state)
        assertEquals(
            PlayerScreenState.Emulator(
                machineId = Pc1245RomDefinition.MACHINE_ID,
                runState = PlayerRunState.PAUSED,
                presentationMode = PresentationMode.FULL_DEVICE,
                operatingMode = OperatingMode.RUN,
                supportedOperatingModes = setOf(OperatingMode.RUN, OperatingMode.PROGRAM),
            ),
            created.session.screenState(),
        )
        val display = created.session.displayFrame()
        assertEquals(16, display.characterColumns)
        assertEquals(95, display.logicalColumns)
        assertEquals(7, display.logicalRows)
        assertEquals(95f / 7f, display.logicalAspectRatio)
        assertEquals(4, display.logicalX(4))
        assertEquals(6, display.logicalX(5))
        assertEquals(0, display.logicalY(0))
        assertIs<InputResult.Accepted>(created.session.pressKey(PocketKey.A))
        assertIs<InputResult.Accepted>(created.session.releaseKey(PocketKey.A))
        assertEquals(true, created.session.setOperatingMode(OperatingMode.PROGRAM))
        assertEquals(OperatingMode.PROGRAM, created.session.operatingMode)
        assertEquals(false, created.session.setOperatingMode(OperatingMode.RESERVE))
        assertEquals(OperatingMode.PROGRAM, created.session.operatingMode)
        created.session.reset()
        assertEquals(OperatingMode.RUN, created.session.operatingMode)
    }

    @Test
    fun `controller display toggle returns to the full device`() {
        assertEquals(
            PresentationMode.CONTROLLER_DISPLAY,
            PresentationMode.FULL_DEVICE.toggleControllerDisplay(),
        )
        assertEquals(
            PresentationMode.FULL_DEVICE,
            PresentationMode.CONTROLLER_DISPLAY.toggleControllerDisplay(),
        )
    }

    private fun pc1245RomSet(): RomSet = RomSet(
        Pc1245RomDefinition.MACHINE_ID,
        listOf(
            RomComponent(
                Pc1245RomDefinition.INTERNAL_ID,
                RomRole.INTERNAL,
                ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d.toByte() },
            ),
            RomComponent(
                Pc1245RomDefinition.EXTERNAL_ID,
                RomRole.EXTERNAL,
                ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d.toByte() },
            ),
        ),
    )
}
