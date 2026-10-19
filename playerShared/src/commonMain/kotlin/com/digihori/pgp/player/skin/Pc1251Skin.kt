package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.DisplaySymbol
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.MachineId

/** Geometry measured against the bundled illustration; LCD/status placement approved in the synthetic preview; hardware review remains. */
internal object Pc1251Skin {
    val definition = SkinDefinition(
        id = PlayerSkinCatalog.PC1251_SKIN_ID,
        machineIds = setOf(MachineId("pc-1251")),
        designSize = SkinSize(1205f, 600f),
        fullDeviceRegion = SkinRect(0f, 0f, 1205f, 600f),
        controllerDisplayRegion = SkinRect(30f, 35f, 1146f, 185f),
        lcdPanelRegion = SkinRect(50f, 62f, 1106f, 132f),
        // 24 five-dot cells + 23 inter-character gaps = 143 logical columns.
        lcdContentRegion = SkinRect(70f, 104f, 1072.5f, 52.5f),
        statusRegions = listOf(
            DisplaySymbol.BUSY to SkinRect(70f, 66f, 90f, 24f),
            DisplaySymbol.P to SkinRect(180f, 66f, 40f, 24f),
            DisplaySymbol.DEF to SkinRect(250f, 66f, 90f, 24f),
            DisplaySymbol.DE to SkinRect(450f, 66f, 30f, 24f),
            DisplaySymbol.G to SkinRect(480f, 66f, 20f, 24f),
            DisplaySymbol.RAD to SkinRect(500f, 66f, 60f, 24f),
            DisplaySymbol.SHIFT to SkinRect(874.375f, 66f, 110f, 24f),
        ).map { (symbol, rect) -> SkinStatusRegion(symbol, rect) },
        operatingModeRegion = SkinRect(70f, 163f, 180f, 26f),
        keyRegions = keys(),
    )

    private fun keys(): List<SkinKeyRegion> = buildList {
        fun key(key: PocketKey, x: Float, y: Float, w: Float, h: Float) {
            add(SkinKeyRegion(key, SkinRect(x, y, w, h)))
        }
        listOf(PocketKey.DEF, PocketKey.SHIFT, PocketKey.DOWN, PocketKey.UP,
            PocketKey.LEFT, PocketKey.RIGHT, PocketKey.BREAK).zip(
            listOf(10f, 95f, 439f, 524f, 610f, 695f, 782f)
        ).forEach { (k, x) -> key(k, x, 275f, 70f, 50f) }
        listOf(
            listOf(PocketKey.Q, PocketKey.W, PocketKey.E, PocketKey.R, PocketKey.T,
                PocketKey.Y, PocketKey.U, PocketKey.I, PocketKey.O, PocketKey.P),
            listOf(PocketKey.A, PocketKey.S, PocketKey.D, PocketKey.F, PocketKey.G,
                PocketKey.H, PocketKey.J, PocketKey.K, PocketKey.L, PocketKey.EQUALS),
            listOf(PocketKey.Z, PocketKey.X, PocketKey.C, PocketKey.V, PocketKey.B,
                PocketKey.N, PocketKey.M, PocketKey.SPACE),
        ).forEachIndexed { row, keys ->
            keys.forEachIndexed { col, k -> key(k, 18f + col * 85.7f,
                listOf(359f, 450f, 535f)[row], 51f, 49f) }
        }
        key(PocketKey.ENTER, 697f, 535f, 152f, 49f)
        listOf(
            listOf(PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9, PocketKey.CLEAR),
            listOf(PocketKey.NUM_4, PocketKey.NUM_5, PocketKey.NUM_6, PocketKey.DIVIDE),
            listOf(PocketKey.NUM_1, PocketKey.NUM_2, PocketKey.NUM_3, PocketKey.MULTIPLY),
            listOf(PocketKey.NUM_0, PocketKey.DOT, PocketKey.PLUS, PocketKey.MINUS),
        ).forEachIndexed { row, keys ->
            keys.forEachIndexed { col, k -> key(k, listOf(865f, 950f, 1035f, 1122f)[col],
                listOf(271f, 357f, 443f, 530f)[row], 78f, 61f) }
        }
    }
}
