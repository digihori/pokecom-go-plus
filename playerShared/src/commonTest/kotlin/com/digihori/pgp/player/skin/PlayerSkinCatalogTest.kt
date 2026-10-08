package com.digihori.pgp.player.skin

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.player.PlayerKeyboardCatalog
import com.digihori.pgp.player.PresentationMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PlayerSkinCatalogTest {
    @Test
    fun `pc1245 skin uses the exported illustration as its design canvas`() {
        val skin = assertNotNull(PlayerSkinCatalog.forMachine(Pc1245RomDefinition.MACHINE_ID))

        assertEquals(SkinSize(1206f, 616f), skin.designSize)
        assertEquals(730f, skin.lcdContentRegion.width)
        assertEquals(54f, skin.lcdContentRegion.height)
        assertEquals(52, skin.keyRegions.size)
        assertEquals(
            PlayerKeyboardCatalog.forMachine(Pc1245RomDefinition.MACHINE_ID)
                ?.rows?.flatten()?.map { it.key }?.toSet(),
            skin.keyRegions.map { it.key }.toSet(),
        )
        assertEquals(
            SkinRect(700f, 550f, 151f, 51f),
            skin.keyRegions.single { it.key == PocketKey.ENTER }.bounds,
        )

        val placement = SkinLayoutEngine.calculate(
            skin,
            SkinLayoutRequest(SkinSize(603f, 308f), mode = PresentationMode.FULL_DEVICE),
        )
        assertEquals(0.5f, placement.scale)
        assertEquals(35f, placement.screenX(skin.lcdContentRegion.x))
        assertEquals(53f, placement.screenY(skin.lcdContentRegion.y))
    }
}
