package com.digihori.pgp.desktop.application.execution

import com.digihori.pgp.desktop.application.development.*
import com.digihori.pgp.desktop.runner.RunnerState
import kotlinx.serialization.json.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

internal class DisplayReadService(private val stateProvider: () -> DevelopmentReadState) {
    fun read(sessionId: String, revision: Long): JsonObject {
        val state = stateProvider()
        val runner = state.runner ?: throw DevelopmentReadError("NO_ACTIVE_SESSION")
        if (state.debugService?.sessionIdentity()?.id != sessionId) throw DevelopmentReadError("STALE_SESSION")
        if (runner.sessionRevision != revision) throw DevelopmentReadError("STALE_REVISION")
        if (runner.state == RunnerState.RUNNING) throw DevelopmentReadError("SESSION_RUNNING")
        if (runner.machineId.value !in GeneratedProjectFiles.MACHINES) throw DevelopmentReadError("UNSUPPORTED_DISPLAY_MACHINE")
        val display = runner.displaySnapshot()
        val scale = 6
        val image = BufferedImage((display.dotColumns + (display.characterColumns - 1) * display.interCharacterColumnGap + 4) * scale,
            (display.dotRows + 4) * scale, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = java.awt.Color(0xd5debd); graphics.fillRect(0, 0, image.width, image.height)
            graphics.color = java.awt.Color(0x293423)
            for (row in 0 until display.dotRows) for (column in 0 until display.dotColumns) if (display.isDotOn(column, row)) {
                val x = column + column / display.characterWidth * display.interCharacterColumnGap
                graphics.fillRect((x + 2) * scale, (row + 2) * scale, scale, scale)
            }
        } finally { graphics.dispose() }
        val png = ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
        val value = buildJsonObject {
            put("format", "pgp-display"); put("schemaVersion", 1); put("sessionId", sessionId); put("sessionRevision", revision)
            put("machineId", runner.machineId.value); put("pc", runner.cpuSnapshot().programCounter)
            put("runState", runner.state.name.lowercase()); put("operatingMode", runner.currentOperatingMode().name)
            put("displayRevision", display.revision); put("enabled", display.enabled)
            put("characterColumns", display.characterColumns); put("characterWidth", display.characterWidth)
            put("dotColumns", display.dotColumns); put("dotRows", display.dotRows)
            put("interCharacterColumnGap", display.interCharacterColumnGap)
            putJsonArray("symbols") { display.symbols.forEach { add(it.name) } }
            putJsonArray("pixels") { for (row in 0 until display.dotRows) add((0 until display.dotColumns).joinToString("") { if (display.isDotOn(it, row)) "1" else "0" }) }
            put("encoding", "pixels[row][column], 1=visible dot; PNG inserts character gaps; no OCR text")
        }
        return buildJsonObject {
            put("structuredContent", value); put("isError", false)
            putJsonArray("content") {
                add(buildJsonObject { put("type", "text"); put("text", value.toString()) })
                add(buildJsonObject { put("type", "image"); put("mimeType", "image/png"); put("data", Base64.getEncoder().encodeToString(png)) })
            }
        }
    }
}
