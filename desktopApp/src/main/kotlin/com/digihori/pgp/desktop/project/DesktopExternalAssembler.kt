package com.digihori.pgp.desktop.project

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal enum class DesktopAssemblerTool { SC61, YASM, GENERIC }

internal data class DesktopAssemblerSettings(
    val external: Boolean = false,
    val executable: String = "",
    val arguments: String = "{source}",
    val timeoutSeconds: Int = 60,
    val tool: DesktopAssemblerTool = DesktopAssemblerTool.SC61,
    val successExitCode: Int = 0,
    val testSource: String = "",
) {
    fun command(source: File, output: File = File(source.absoluteFile.parentFile, source.nameWithoutExtension + ".dmp")): List<String> {
        require(executable.isNotBlank()) { "Assembler executable is required" }
        require(timeoutSeconds in 1..3600) { "Timeout must be 1..3600 seconds" }
        require(successExitCode in 0..255) { "Success exit code must be 0..255" }
        val configured = if (arguments.isBlank()) emptyList() else DesktopExternalEditor.splitCommand(arguments)
        val tokens = if (tool == DesktopAssemblerTool.YASM) {
            // YASM defaults to stdout. Its adapter always owns a fresh output file,
            // including when an existing configuration only supplies {source}.
            val managed = configured.toMutableList()
            var index = 0
            var hasDump = false
            while (index < managed.size) {
                if (managed[index] == "-d") {
                    require(index + 1 < managed.size && !managed[index + 1].startsWith("-")) {
                        "YASM -d requires an output path"
                    }
                    managed[index + 1] = "{output}"
                    hasDump = true
                    index++
                }
                index++
            }
            if (!hasDump) managed.addAll(listOf("-d", "{output}"))
            managed
        } else configured
        return listOf(executable.trim()) + tokens.map { token -> Regex("\\{(source|output)\\}").replace(token) {
            if (it.groupValues[1] == "source") source.absolutePath else output.absolutePath
        } } +
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
        output: File = File(source.absoluteFile.parentFile, source.nameWithoutExtension + ".dmp"),
    ): DesktopExternalAssemblerRunResult = withContext(Dispatchers.IO) {
        var process: Process? = null
        var logFile: File? = null
        try {
            require(source.isFile) { "Source file does not exist: ${source.path}" }
            val command = settings.command(source, output)
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
