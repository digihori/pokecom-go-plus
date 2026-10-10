package com.digihori.pgp.desktop.window

import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.util.prefs.Preferences

internal data class DesktopWindowBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val maximized: Boolean,
)

internal class DesktopWindowPlacementStore(
    private val preferences: Preferences = Preferences.userRoot().node("com/digihori/pgp/desktop/windows"),
) {
    fun restore(id: String): DesktopWindowBounds? {
        val node = preferences.node(id)
        val width = node.getInt("width", -1)
        val height = node.getInt("height", -1)
        if (width < MIN_WIDTH || height < MIN_HEIGHT) return null
        return DesktopWindowBounds(
            node.getInt("x", 0),
            node.getInt("y", 0),
            width,
            height,
            node.getBoolean("maximized", false),
        )
    }

    fun save(id: String, bounds: Rectangle, maximized: Boolean) {
        if (bounds.width < MIN_WIDTH || bounds.height < MIN_HEIGHT) return
        preferences.node(id).apply {
            putInt("x", bounds.x)
            putInt("y", bounds.y)
            putInt("width", bounds.width)
            putInt("height", bounds.height)
            putBoolean("maximized", maximized)
        }
    }

    fun visibleBounds(saved: DesktopWindowBounds): Rectangle {
        val candidate = Rectangle(saved.x, saved.y, saved.width, saved.height)
        val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
            .map { it.defaultConfiguration.bounds }
        if (screens.any { it.intersects(candidate) }) return candidate
        val primary = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds
        return Rectangle(
            primary.x + ((primary.width - saved.width).coerceAtLeast(0) / 2),
            primary.y + ((primary.height - saved.height).coerceAtLeast(0) / 2),
            saved.width.coerceAtMost(primary.width),
            saved.height.coerceAtMost(primary.height),
        )
    }

    companion object {
        private const val MIN_WIDTH = 240
        private const val MIN_HEIGHT = 160
    }
}

internal class DesktopWindowPlacementSession(
    private val id: String,
    private val frame: Frame,
    private val store: DesktopWindowPlacementStore,
) {
    private var normalBounds = frame.bounds

    fun captureNormalBounds() {
        if (frame.extendedState and Frame.MAXIMIZED_BOTH == 0) normalBounds = frame.bounds
    }

    fun save() {
        captureNormalBounds()
        store.save(id, normalBounds, frame.extendedState and Frame.MAXIMIZED_BOTH != 0)
    }
}
