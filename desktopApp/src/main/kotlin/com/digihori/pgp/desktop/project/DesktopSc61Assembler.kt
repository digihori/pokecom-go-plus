package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.api.MemoryPlacementValidator
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface DesktopSc61AssemblyResult {
    data class Success(val image: AddressedMemoryImage, val log: String, val exitCode: Int,
        val sourceListing: DesktopSc61Listing? = null) : DesktopSc61AssemblyResult
    data class Failure(val message: String, val log: String = "",
        val location: DesktopSc61SourceLocation? = null) : DesktopSc61AssemblyResult
}

/** Fresh output directory isolates generated files; cwd preserves SC61 relative input references. */
internal object DesktopSc61Assembler {
    suspend fun assemble(settings: DesktopAssemblerSettings, source: File, layout: MachineMemoryLayout): DesktopSc61AssemblyResult =
        withContext(Dispatchers.IO) {
            var directory: File? = null
            try {
                require(source.isFile) { "Source file does not exist: ${source.path}" }
                settings.command(source)
                val scratch = Files.createTempDirectory("pgp-sc61-").toFile()
                directory = scratch
                val staged = File(scratch, "input.asm")
                source.copyTo(staged)
                when (val run = DesktopExternalAssemblerRunner.run(settings, staged, source.absoluteFile.parentFile)) {
                    is DesktopExternalAssemblerRunResult.Exited -> when (val result = importDump(run, File(scratch, "input.dmp"), layout, validatePlacement = false)) {
                        is DesktopSc61AssemblyResult.Failure -> result.copy(location = DesktopSc61ListingParser.diagnostic(run.log, source))
                        is DesktopSc61AssemblyResult.Success -> {
                            val listing = File(scratch, "input.lst")
                            val parsed = runCatching {
                                if (listing.isFile && listing.length() <= 16 * 1024 * 1024)
                                    DesktopSc61ListingParser.parse(DesktopSc61ListingParser.decode(listing.readBytes())
                                        .replace("[${staged.path}]", "[${source.absolutePath}]"), source) else null
                            }.getOrNull()
                            result.copy(sourceListing = parsed)
                        }
                    }
                    is DesktopExternalAssemblerRunResult.Failed -> DesktopSc61AssemblyResult.Failure(run.message)
                    is DesktopExternalAssemblerRunResult.TimedOut -> DesktopSc61AssemblyResult.Failure("Assembler timed out", run.log)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { DesktopSc61AssemblyResult.Failure(error.message ?: error.javaClass.simpleName) }
            finally { directory?.deleteRecursively() }
        }

    internal fun importDump(run: DesktopExternalAssemblerRunResult.Exited, dump: File, layout: MachineMemoryLayout, validatePlacement: Boolean = true): DesktopSc61AssemblyResult {
        if (run.code < 0 || !run.log.lineSequence().any { it.trim() == "ASSEMBLY COMPLETE, NO ERRORS FOUND" } ||
            Regex("""(?m)^\s*LINE:\d+\s+ERROR CODE:""").containsMatchIn(run.log))
            return DesktopSc61AssemblyResult.Failure("SC61 assembly failed (exit code ${run.code})", run.log)
        if (!dump.isFile) return DesktopSc61AssemblyResult.Failure("SC61 did not generate a DMP file", run.log)
        if (dump.length() > 2 * 1024 * 1024) return DesktopSc61AssemblyResult.Failure("SC61 DMP exceeds 2 MiB", run.log)
        val parsed = DesktopMemoryDumpLoader.parse(dump.readBytes())
        if (parsed is DesktopMemoryDumpLoadResult.Failure)
            return DesktopSc61AssemblyResult.Failure("Invalid SC61 DMP: ${parsed.error}", run.log)
        val image = (parsed as DesktopMemoryDumpLoadResult.Success).image
        val issue = if (validatePlacement) MemoryPlacementValidator.validate(image, layout) else null
        if (issue != null) return DesktopSc61AssemblyResult.Failure(issue.error.placementMessage(layout), run.log)
        return DesktopSc61AssemblyResult.Success(image, run.log, run.code)
    }
}
