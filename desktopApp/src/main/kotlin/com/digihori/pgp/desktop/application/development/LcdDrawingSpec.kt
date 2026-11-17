package com.digihori.pgp.desktop.application.development

import kotlinx.serialization.json.*

/** Canonical writable pixel addresses, in the same order as the Core LCD snapshots. */
internal fun lcdDrawingSpec(machineId: String, characterColumns: Int, characterRows: Int): JsonElement {
    val blocks = when (machineId) {
        "pc-1250", "pc-1251", "pc-1255" -> listOf(0xf800..0xf83b, 0xf87b downTo 0xf840)
        "pc-1260", "pc-1261" -> listOf(0x2000..0x203b, 0x2800..0x283b, 0x2040..0x207b, 0x2840..0x287b)
        "pc-1350" -> fourLineBlocks(0x7000)
        "pc-1360", "pc-1360k" -> fourLineBlocks(0x2800)
        "pc-1401", "pc-1402" -> listOf(0x6000..0x6027, 0x6067 downTo 0x6040)
        "pc-1450" -> listOf(0x7000..0x703b, 0x707b downTo 0x7068)
        "pc-1460" -> listOf(0x3000..0x301d, 0x302d..0x303b, 0x301e..0x302c,
            0x306c downTo 0x305e, 0x307b downTo 0x306d, 0x305d downTo 0x3040)
        "pc-1470u" -> listOf(0x2800..0x283b, 0x2a00..0x2a3b, 0x2840..0x287b, 0x2a40..0x2a7b)
        else -> return JsonNull
    }
    val symbols = when (machineId) {
        "pc-1250", "pc-1251", "pc-1255" -> listOf(0xf83c, 0xf83d)
        "pc-1260", "pc-1261" -> listOf(0x203d, 0x207c)
        "pc-1350" -> listOf(0x783c)
        "pc-1360", "pc-1360k" -> listOf(0x303c)
        "pc-1401", "pc-1402" -> listOf(0x603c, 0x603d, 0x607c)
        "pc-1450" -> listOf(0x703c, 0x703d, 0x707c)
        "pc-1460" -> listOf(0x303c, 0x303d, 0x307c)
        else -> listOf(0x283c, 0x283d, 0x287c, 0x287d)
    }
    val characterWidth = if (characterRows == 4) 6 else 5
    val bits = if (characterRows == 4) 8 else 7
    val gated = machineId !in setOf("pc-1260", "pc-1261", "pc-1350", "pc-1360", "pc-1360k")
    return buildJsonObject {
        put("source", "PGP Core machine display and memory bus implementations")
        put("characterWidth", characterWidth)
        put("dotColumns", characterColumns * characterWidth)
        put("dotRows", characterRows * bits)
        put("encoding", "One byte per column within a band of $bits pixel rows; bit 0 is the top pixel in each band. " +
            "Concatenate columnAddressBlocks in order, top band first, left to right within each band. " +
            "For zero-based pixel (x,y), use address index (y / $bits) * dotColumns + x and bit y % $bits. " +
            "Preserve other bits and write only pixel addresses for drawing.")
        putJsonArray("columnAddressBlocks") {
            blocks.forEach { block -> add(buildJsonObject {
                put("firstAddress", block.first)
                put("lastAddressInclusive", block.last)
                put("step", block.step)
            }) }
        }
        putJsonArray("symbolAddressesExcluded") { symbols.forEach { add(it) } }
        put("lcdMustBeEnabled", gated)
        if (gated) put("enableSequence", "LIP 0x5F; ORIM 0x01; OUTC (preserves other control bits)")
        put("firmwareMayOverwriteDisplay", true)
        put("firmwareCallingConventionVerified", false)
    }
}

private fun fourLineBlocks(base: Int): List<IntRange> =
    listOf(0x00, 0x40, 0x1e, 0x5e).flatMap { band ->
        (0..4).map { block ->
            val start = base + band + block * 0x200
            start until start + 30
        }
    }
