package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface DesktopExternalAssemblyResult {
    data class Success(val image: AddressedMemoryImage, val log: String, val exitCode: Int,
        val sourceListing: DesktopSc61Listing? = null) : DesktopExternalAssemblyResult
    data class Failure(val message: String, val log: String = "",
        val location: DesktopSc61SourceLocation? = null) : DesktopExternalAssemblyResult
}

/** Tool adapters share the runner, but retain their own success and output contracts. */
internal object DesktopConfiguredExternalAssembler {
    suspend fun assemble(settings: DesktopAssemblerSettings, source: File, layout: MachineMemoryLayout): DesktopExternalAssemblyResult {
        if (settings.tool == DesktopAssemblerTool.SC61) return DesktopSc61Assembler.assemble(settings, source, layout)
        return withContext(Dispatchers.IO) {
            var directory: File? = null
            try {
                require(source.isFile) { "Source file does not exist: ${source.path}" }
                settings.command(source)
                val scratch = Files.createTempDirectory("pgp-external-assembly-").toFile()
                directory = scratch
                val staged = File(scratch, "input.asm")
                source.copyTo(staged)
                val output = File(scratch, "input.dmp")
                when (val run = DesktopExternalAssemblerRunner.run(settings, staged, source.absoluteFile.parentFile, output)) {
                    is DesktopExternalAssemblerRunResult.Exited -> importOutput(settings, run, output)
                    is DesktopExternalAssemblerRunResult.Failed -> DesktopExternalAssemblyResult.Failure(run.message)
                    is DesktopExternalAssemblerRunResult.TimedOut -> DesktopExternalAssemblyResult.Failure("Assembler timed out", run.log)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { DesktopExternalAssemblyResult.Failure(error.message ?: error.javaClass.simpleName) }
            finally { directory?.deleteRecursively() }
        }
    }

    internal fun importOutput(settings: DesktopAssemblerSettings, run: DesktopExternalAssemblerRunResult.Exited, output: File): DesktopExternalAssemblyResult {
        val expected = if (settings.tool == DesktopAssemblerTool.YASM) 0 else settings.successExitCode
        if (run.code != expected) return DesktopExternalAssemblyResult.Failure("External assembly failed (exit code ${run.code})", run.log)
        if (settings.tool == DesktopAssemblerTool.YASM &&
            (!run.log.lineSequence().any { it.trim() == "complete." } || Regex("(?m)^ERROR:").containsMatchIn(run.log)))
            return DesktopExternalAssemblyResult.Failure("YASM did not report completion", run.log)
        if (!output.isFile || Files.isSymbolicLink(output.toPath()))
            return DesktopExternalAssemblyResult.Failure("External assembler did not generate a DMP file", run.log)
        if (output.length() > 2 * 1024 * 1024)
            return DesktopExternalAssemblyResult.Failure("External DMP exceeds 2 MiB", run.log)
        return try {
            val bytes = output.readBytes()
            val normalized = if (settings.tool == DesktopAssemblerTool.YASM) normalizeYasmDump(bytes.toString(Charsets.US_ASCII)).toByteArray() else bytes
            when (val parsed = DesktopMemoryDumpLoader.parse(normalized)) {
                is DesktopMemoryDumpLoadResult.Failure -> DesktopExternalAssemblyResult.Failure("Invalid external DMP: ${parsed.error}", run.log)
                is DesktopMemoryDumpLoadResult.Success -> DesktopExternalAssemblyResult.Success(parsed.image, run.log, run.code)
            }
        } catch (error: Exception) { DesktopExternalAssemblyResult.Failure("Invalid external DMP: ${error.message}", run.log) }
    }

    /** YASM prints aligned eight-byte rows; leading empty columns are address offsets, not zeros. */
    internal fun normalizeYasmDump(text: String): String = text.lineSequence().filter { it.isNotBlank() }.joinToString("\n") { line ->
        val match = Regex("^([0-9a-fA-F]{4}) : (.*)$").matchEntire(line.trimEnd())
            ?: throw IllegalArgumentException("Invalid YASM dump row")
        val base = match.groupValues[1].toInt(16)
        require(base % 8 == 0) { "YASM row address must be aligned" }
        val body = match.groupValues[2]
        val columns = body.substringBefore(':')
        val leading = columns.takeWhile { it == ' ' }.length
        require(leading % 3 == 0 && leading / 3 < 8) { "Invalid YASM empty columns" }
        val data = columns.drop(leading).trimEnd()
        require(Regex("[0-9a-fA-F]{2}( [0-9a-fA-F]{2})*").matches(data)) { "Invalid YASM bytes" }
        val count = data.split(' ').size
        require(leading / 3 + count <= 8) { "YASM row exceeds eight columns" }
        val checksum = if (':' in body) {
            val sum = body.substringAfter(':').trim()
            require(Regex("[0-9a-fA-F]{2}").matches(sum)) { "Invalid YASM checksum" }
            require(sum.toInt(16) == (data.split(' ').sumOf { it.toInt(16) } and 255)) { "YASM checksum mismatch" }
            ":$sum"
        } else ""
        "%04X:%s%s".format(base + leading / 3, data, checksum)
    }
}
