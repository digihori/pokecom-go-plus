package com.digihori.pgp.desktop.macro

import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import kotlinx.coroutines.*
import java.io.File

internal sealed interface MacroCommand {
    data class Load(val path: String) : MacroCommand
    data class Mode(val mode: OperatingMode) : MacroCommand
    data class Cmd(val text: String) : MacroCommand
    data class Wait(val milliseconds: Long) : MacroCommand
    data class Key(val key: PocketKey) : MacroCommand
    data class Pause(val message: String) : MacroCommand
}
internal data class MacroLine(val number: Int, val source: String, val command: MacroCommand)
internal class MacroSyntaxException(val line: Int) : IllegalArgumentException("Invalid macro syntax at line $line")

internal object StudioMacroParser {
    fun parse(text: String): List<MacroLine> = text.removePrefix("\uFEFF").lineSequence().mapIndexedNotNull { index, raw ->
        val source = raw.trim()
        if (source.isEmpty() || source.startsWith('#')) return@mapIndexedNotNull null
        val match = Regex("([A-Za-z]+)\\s+(.+)").matchEntire(source) ?: throw MacroSyntaxException(index + 1)
        val name = match.groupValues[1].lowercase()
        val argument = match.groupValues[2].trim()
        val command = try {
            when (name) {
                "load" -> MacroCommand.Load(string(argument).also { require(it.isNotBlank()) })
                "cmd" -> MacroCommand.Cmd(string(argument).also { require(it.isNotEmpty() && it.none(Char::isISOControl)) })
                "pause" -> MacroCommand.Pause(string(argument))
                "mode" -> MacroCommand.Mode(when (argument.uppercase()) {
                    "PRO" -> OperatingMode.PROGRAM
                    "RUN" -> OperatingMode.RUN
                    "RSV", "RESERVE" -> OperatingMode.RESERVE
                    "CAL" -> OperatingMode.CALCULATOR
                    "AER" -> OperatingMode.AER
                    else -> error("mode")
                })
                "key" -> MacroCommand.Key(PocketKey.valueOf(argument.uppercase()))
                "wait" -> {
                    val time = Regex("([0-9]+)(ms|s)", RegexOption.IGNORE_CASE).matchEntire(argument) ?: error("duration")
                    val value = time.groupValues[1].toLong()
                    val milliseconds = Math.multiplyExact(value, if (time.groupValues[2].equals("s", true)) 1000L else 1L)
                    MacroCommand.Wait(milliseconds)
                }
                else -> error("command")
            }
        } catch (_: Exception) { throw MacroSyntaxException(index + 1) }
        MacroLine(index + 1, source, command)
    }.toList()

    private fun string(value: String): String {
        require(value.length >= 2 && value.first() == '"' && value.last() == '"')
        return buildString {
            var index = 1
            while (index < value.lastIndex) {
                val char = value[index++]
                require(char != '"' && !char.isISOControl())
                if (char == '\\') {
                    require(index < value.lastIndex)
                    val escaped = value[index++]
                    require(escaped == '"' || escaped == '\\')
                    append(escaped)
                } else append(char)
            }
        }
    }
}

internal interface MacroHost {
    fun checkError()
    suspend fun load(file: File)
    suspend fun mode(value: OperatingMode)
    suspend fun command(value: String)
    suspend fun key(value: PocketKey)
    fun cancelInput()
}

/** All waits suspend; the emulator continues on the normal UI tick loop. */
internal class StudioMacroExecutor {
    private var resumeSignal: CompletableDeferred<Unit>? = null
    fun resume() { resumeSignal?.complete(Unit) }

    suspend fun execute(lines: List<MacroLine>, directory: File, host: MacroHost,
        onLine: (MacroLine) -> Unit, onPause: (String?) -> Unit) = coroutineScope {
        val monitor = launch {
            while (isActive) { host.checkError(); delay(10) }
        }
        try {
            host.checkError()
            for (line in lines) {
                ensureActive()
                host.checkError()
                onLine(line)
                when (val command = line.command) {
                    is MacroCommand.Load -> host.load(File(command.path).let { if (it.isAbsolute) it else File(directory, command.path) })
                    is MacroCommand.Mode -> host.mode(command.mode)
                    is MacroCommand.Cmd -> host.command(command.text)
                    is MacroCommand.Key -> host.key(command.key)
                    is MacroCommand.Wait -> delay(command.milliseconds)
                    is MacroCommand.Pause -> {
                        val signal = CompletableDeferred<Unit>()
                        resumeSignal = signal
                        onPause(command.message)
                        try { signal.await() } finally { resumeSignal = null; onPause(null) }
                    }
                }
                host.checkError()
                // Uniform settle interval, including the last command, wait and resumed pause.
                delay(1000)
                host.checkError()
            }
        } finally { monitor.cancel(); host.cancelInput() }
    }
}
