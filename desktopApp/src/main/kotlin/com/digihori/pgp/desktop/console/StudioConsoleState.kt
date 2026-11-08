package com.digihori.pgp.desktop.console

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal enum class ConsoleLevel { INFO, ERROR }

/** UI-thread owned. Visibility and retained history are independent of operation execution. */
@Stable
internal class StudioConsoleState(
    initiallyVisible: Boolean = false,
    private val maxEntries: Int = 1000,
    private val maxCharacters: Int = 200_000,
    private val clock: () -> LocalTime = LocalTime::now,
) {
    init { require(maxEntries > 0 && maxCharacters > 0) }
    var visible by mutableStateOf(initiallyVisible)
        private set
    var entries by mutableStateOf<List<String>>(emptyList())
        private set
    var text by mutableStateOf("")
        private set

    fun changeVisibility(value: Boolean) { visible = value }
    fun reveal() { visible = true } // Automatic reveal must not persist a manual preference.
    fun clear() { entries = emptyList(); text = "" }

    fun append(message: String, level: ConsoleLevel = ConsoleLevel.INFO, reveal: Boolean = false) {
        val prefix = "[${clock().format(TIME_FORMAT)}] ${level.name}  "
        val entry = (prefix + message.replace("\r\n", "\n").replace('\r', '\n')).take(maxCharacters)
        val retained = (entries + entry).takeLast(maxEntries).toMutableList()
        var length = retained.sumOf { it.length } + retained.size - 1
        while (length > maxCharacters && retained.size > 1) {
            length -= retained.removeAt(0).length + 1
        }
        entries = retained.toList()
        text = entries.joinToString("\n")
        if (reveal) visible = true
    }

    private companion object { val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss") }
}
