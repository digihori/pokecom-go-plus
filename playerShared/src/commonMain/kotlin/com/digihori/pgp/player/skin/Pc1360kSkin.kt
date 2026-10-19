package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

/** Same LCD/standard keys; five additional keys from the bundled K illustration. */
internal object Pc1360kSkin {
    val definition = Pc1360Skin.definition.copy(
        id = PlayerSkinCatalog.PC1360K_SKIN_ID,
        machineIds = setOf(MachineId("pc-1360k")),
        statusRegions = FourLinePlayerLcdLayout.symbols(kanji = true),
        keyRegions = Pc1360Skin.definition.keyRegions + listOf(
            SkinKeyRegion(PocketKey.KANJI, SkinRect(971f, 20f, 71f, 53f)),
            SkinKeyRegion(PocketKey.HIRA_KATA, SkinRect(1065f, 20f, 71f, 53f)),
            SkinKeyRegion(PocketKey.YJ, SkinRect(1158f, 20f, 71f, 53f)),
            SkinKeyRegion(PocketKey.KANJI_FIX, SkinRect(1340f, 467f, 78f, 61f)),
            SkinKeyRegion(PocketKey.KANJI_CONVERT, SkinRect(1340f, 554f, 78f, 61f)),
        ),
    )
}
