package com.digihori.pgp.desktop.project

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class DesktopAssemblerSettings(
    val external: Boolean = false,
    val executable: String = "",
    val arguments: String = "{source}",
    val timeoutSeconds: Int = 60,
) {
    fun command(source: File): List<String> {
        require(executable.isNotBlank()) { "Assembler executable is required" }
        require(timeoutSeconds in 1..3600) { "Timeout must be 1..3600 seconds" }
        val tokens = if (arguments.isBlank()) emptyList() else DesktopExternalEditor.splitCommand(arguments)
        return listOf(executable.trim()) + tokens.map { it.replace("{source}", source.absolutePath) } +
            if (tokens.none { "{source}" in it }) listOf(source.absolutePath) else emptyList()
    }
}

internal sealed interface DesktopExternalAssemblerRunResult {
    data class Exited(val code: Int, val log: String) : DesktopExternalAssemblerRunResult
    data class Failed(val message: String) : DesktopExternalAssemblerRunResult
    data class TimedOut(val log: String) : DesktopExternalAssemblerRunResult
}

/** Executes on IO; exit code is raw information, not a tool-independent success verdict. */
internal object DesktopExternalAssemblerRunner {
    suspend fun run(
        settings: DesktopAssemblerSettings,
        source: File,
        workingDirectory: File = source.absoluteFile.parentFile,
    ): DesktopExternalAssemblerRunResult = withContext(Dispatchers.IO) {
        var process: Process? = null
        var logFile: File? = null
        try {
            require(source.isFile) { "Source file does not exist: ${source.path}" }
            val command = settings.command(source)
            val log = Files.createTempFile("pgp-assembler-", ".log").toFile()
            logFile = log
            val child = ProcessBuilder(command).directory(workingDirectory)
                .redirectErrorStream(true).redirectOutput(log).start()
            process = child
            child.outputStream.close()
            val started = System.nanoTime()
            while (child.isAlive) {
                if (System.nanoTime() - started >= settings.timeoutSeconds * 1_000_000_000L) {
                    stop(child)
                    return@withContext DesktopExternalAssemblerRunResult.TimedOut(readLog(log))
                }
                delay(50)
            }
            DesktopExternalAssemblerRunResult.Exited(child.exitValue(), readLog(log))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DesktopExternalAssemblerRunResult.Failed(error.message ?: error.javaClass.simpleName)
        } finally {
            process?.let(::stop)
            logFile?.delete()
        }
    }

    private fun readLog(file: File): String = file.inputStream().use {
        val bytes = it.readNBytes(1024 * 1024)
        bytes.toString(Charsets.UTF_8) + if (it.read() >= 0) "\n[Log truncated at 1 MiB]" else ""
    }

    private fun stop(process: Process) {
        process.descendants().use { children -> children.forEach { it.destroyForcibly() } }
        if (process.isAlive) process.destroyForcibly()
        process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
    }
}
