package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.MachineId

/** Platform-neutral skin geometry. Image resources remain owned by each platform application. */
public object PlayerSkinCatalog {
    public const val PC1245_SKIN_ID: String = "pc1245-default"

    public fun forMachine(machineId: MachineId): SkinDefinition? = when (machineId) {
        Pc1245RomDefinition.MACHINE_ID -> pc1245
        else -> null
    }

    private val pc1245 = SkinDefinition(
        id = PC1245_SKIN_ID,
        machineIds = setOf(Pc1245RomDefinition.MACHINE_ID),
        designSize = SkinSize(width = 1206f, height = 616f),
        fullDeviceRegion = SkinRect(x = 0f, y = 0f, width = 1206f, height = 616f),
        playableRegion = SkinRect(x = 0f, y = 0f, width = 1206f, height = 616f),
        landscapeRegion = SkinRect(x = 0f, y = 0f, width = 1206f, height = 616f),
        controllerDisplayRegion = SkinRect(x = 30f, y = 35f, width = 1146f, height = 210f),
        // The illustration has no explicit LCD outline. Keep this provisional rectangle isolated here
        // so device testing can refine it without changing renderer or input coordinates.
        lcdContentRegion = SkinRect(x = 70f, y = 106f, width = 730f, height = 54f),
        keyRegions = pc1245KeyRegions(),
    )

    private fun pc1245KeyRegions(): List<SkinKeyRegion> {
        val letterX = listOf(18f, 103f, 188f, 274f, 360f, 446f, 532f, 618f, 704f, 789f)
        val numberX = listOf(864f, 949f, 1034f, 1122f)
        val regions = mutableListOf<SkinKeyRegion>()

        fun add(key: PocketKey, x: Float, y: Float, width: Float, height: Float) {
            regions += SkinKeyRegion(key, SkinRect(x, y, width, height))
        }

        listOf(
            PocketKey.DEF to 10f,
            PocketKey.SHIFT to 94f,
            PocketKey.DOWN to 438f,
            PocketKey.UP to 525f,
            PocketKey.LEFT to 610f,
            PocketKey.RIGHT to 695f,
            PocketKey.BREAK to 782f,
        ).forEach { (key, x) -> add(key, x, 290f, if (key == PocketKey.DEF) 70f else 73f, 53f) }
        listOf(PocketKey.NUM_7, PocketKey.NUM_8, PocketKey.NUM_9, PocketKey.CLEAR)
            .forEachIndexed { index, key -> add(key, numberX[index], 286f, 80f, 62f) }

        listOf(PocketKey.Q, PocketKey.W, PocketKey.E, PocketKey.R, PocketKey.T,
            PocketKey.Y, PocketKey.U, PocketKey.I, PocketKey.O, PocketKey.P)
            .forEachIndexed { index, key -> add(key, letterX[index], 374f, 53f, 51f) }
        listOf(PocketKey.NUM_4, PocketKey.NUM_5, PocketKey.NUM_6, PocketKey.DIVIDE)
            .forEachIndexed { index, key -> add(key, numberX[index], 372f, 80f, 63f) }

        listOf(PocketKey.A, PocketKey.S, PocketKey.D, PocketKey.F, PocketKey.G,
            PocketKey.H, PocketKey.J, PocketKey.K, PocketKey.L, PocketKey.EQUALS)
            .forEachIndexed { index, key -> add(key, letterX[index], 466f, 53f, 51f) }
        listOf(PocketKey.NUM_1, PocketKey.NUM_2, PocketKey.NUM_3, PocketKey.MULTIPLY)
            .forEachIndexed { index, key -> add(key, numberX[index], 458f, 80f, 63f) }

        listOf(PocketKey.Z, PocketKey.X, PocketKey.C, PocketKey.V, PocketKey.B, PocketKey.N, PocketKey.M)
            .forEachIndexed { index, key -> add(key, letterX[index], 550f, 53f, 51f) }
        add(PocketKey.SPACE, 618f, 550f, 53f, 51f)
        add(PocketKey.ENTER, 700f, 550f, 151f, 51f)
        listOf(PocketKey.NUM_0, PocketKey.DOT, PocketKey.PLUS, PocketKey.MINUS)
            .forEachIndexed { index, key -> add(key, numberX[index], 546f, 80f, 62f) }

        return regions
    }
}
