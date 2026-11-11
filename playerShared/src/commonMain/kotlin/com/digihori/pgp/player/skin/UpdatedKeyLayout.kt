package com.digihori.pgp.player.skin

import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import com.digihori.pgp.player.skin.SkinDefinition
import com.digihori.pgp.player.skin.SkinRect

/** Key geometry for the current PC-1245, PC-1251 and PC-1261 artwork. */
public fun SkinDefinition.withUpdatedKeyLayout(): SkinDefinition {
    val positions = when (id) {
        PlayerSkinCatalog.PC1245_SKIN_ID -> listOf(17f, 141f, 266f, 391f, 516f, 640f, 766f)
        PlayerSkinCatalog.PC1251_SKIN_ID -> listOf(18f, 141f, 267f, 392f, 517f, 640f, 766f)
        PlayerSkinCatalog.PC1261_SKIN_ID -> listOf(18f, 142f, 267f, 391f, 515f, 639f, 766f)
        else -> return this
    }
    val y = when (id) {
        PlayerSkinCatalog.PC1245_SKIN_ID -> 290f
        PlayerSkinCatalog.PC1261_SKIN_ID -> 274f
        else -> 275f
    }
    val height = when (id) {
        PlayerSkinCatalog.PC1245_SKIN_ID -> 53f
        PlayerSkinCatalog.PC1261_SKIN_ID -> 52f
        else -> 51f
    }
    val keyPositions = listOf(PocketKey.DEF, PocketKey.SHIFT, PocketKey.DOWN, PocketKey.UP,
        PocketKey.LEFT, PocketKey.RIGHT, PocketKey.BREAK).zip(positions).toMap()
    return copy(keyRegions = keyRegions.map { region ->
        val x = keyPositions[region.key] ?: return@map region
        region.copy(bounds = SkinRect(x, y, 70f, height))
    })
}
