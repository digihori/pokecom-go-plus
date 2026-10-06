package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult
import com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory
import com.digihori.pgp.core.source.machine.AddressedMemorySegmentData
import com.digihori.pgp.desktop.basic.DesktopBasicLoader
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileError
import com.digihori.pgp.desktop.basic.DesktopBasicProgramCompileResult
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadError
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoadResult
import com.digihori.pgp.desktop.machine.DesktopMemoryDumpLoader

internal data class DesktopProjectArtifact(
    private val basicProgram: ByteArray?,
    val memoryImage: AddressedMemoryImage?,
) {
    fun copyBasicProgram(): ByteArray? = basicProgram?.copyOf()
}

internal sealed interface DesktopProjectBuildResult {
    data class Success(val artifact: DesktopProjectArtifact) : DesktopProjectBuildResult
    data class Failure(val errors: List<DesktopProjectBuildError>) : DesktopProjectBuildResult
}

internal sealed interface DesktopProjectBuildError {
    data class MultipleBasicSources(val sourceIds: List<String>) : DesktopProjectBuildError
    data class CouldNotReadSource(val sourceId: String, val message: String) : DesktopProjectBuildError
    data class BasicCompile(val sourceId: String, val error: DesktopBasicProgramCompileError) :
        DesktopProjectBuildError
    data class MemoryDumpParse(val sourceId: String, val error: DesktopMemoryDumpLoadError) :
        DesktopProjectBuildError
    data class EmptyRawBinary(val sourceId: String) : DesktopProjectBuildError
    data class InvalidRawBinaryRange(
        val sourceId: String,
        val startAddress: Int,
        val size: Int,
    ) : DesktopProjectBuildError
    data class MemoryOverlap(
        val sourceId: String,
        val previousSourceId: String,
        val address: Int,
    ) : DesktopProjectBuildError
    data class AssemblyNotImplemented(val sourceId: String) : DesktopProjectBuildError
}

/** Compiles every source before an artifact is allowed to modify an emulator session. */
internal object DesktopProjectBuilder {
    fun build(workspace: DesktopProjectWorkspace): DesktopProjectBuildResult {
        val errors = mutableListOf<DesktopProjectBuildError>()
        val basicSources = workspace.sources.filter { it.definition.type == ProjectSourceType.BASIC }
        if (basicSources.size > 1) {
            errors += DesktopProjectBuildError.MultipleBasicSources(basicSources.map { it.definition.id })
        }

        var basicProgram: ByteArray? = null
        val memorySegments = mutableListOf<AddressedMemorySegmentData>()
        val memorySegmentOwners = mutableListOf<String>()

        workspace.sources.forEach { source ->
            val bytes = runCatching { source.file.readBytes() }.getOrElse {
                errors += DesktopProjectBuildError.CouldNotReadSource(
                    source.definition.id,
                    it.message ?: it::class.simpleName.orEmpty(),
                )
                return@forEach
            }
            when (source.definition.type) {
                ProjectSourceType.BASIC -> when (val compiled = DesktopBasicLoader.compilePc1245Program(bytes)) {
                    is DesktopBasicProgramCompileResult.Failure -> errors +=
                        DesktopProjectBuildError.BasicCompile(source.definition.id, compiled.error)
                    is DesktopBasicProgramCompileResult.Success -> if (basicSources.size == 1) {
                        basicProgram = compiled.bytes.copyOf()
                    }
                }
                ProjectSourceType.MEMORY_DUMP -> when (val parsed = DesktopMemoryDumpLoader.parse(bytes)) {
                    is DesktopMemoryDumpLoadResult.Failure -> errors +=
                        DesktopProjectBuildError.MemoryDumpParse(source.definition.id, parsed.error)
                    is DesktopMemoryDumpLoadResult.Success -> parsed.image.segments.forEach { segment ->
                        memorySegments += AddressedMemorySegmentData(
                            segment.startAddress,
                            segment.copyBytes(),
                            segment.sourceLine,
                        )
                        memorySegmentOwners += source.definition.id
                    }
                }
                ProjectSourceType.RAW_BINARY -> {
                    val startAddress = checkNotNull(source.definition.loadAddress)
                    if (bytes.isEmpty()) {
                        errors += DesktopProjectBuildError.EmptyRawBinary(source.definition.id)
                    } else {
                        memorySegments += AddressedMemorySegmentData(startAddress, bytes)
                        memorySegmentOwners += source.definition.id
                    }
                }
                ProjectSourceType.ASSEMBLY -> errors +=
                    DesktopProjectBuildError.AssemblyNotImplemented(source.definition.id)
            }
        }

        var memoryImage: AddressedMemoryImage? = null
        if (memorySegments.isNotEmpty()) {
            when (val created = AddressedMemoryImageFactory.create(memorySegments)) {
                AddressedMemoryImageCreateResult.Empty -> Unit
                is AddressedMemoryImageCreateResult.Success -> memoryImage = created.image
                is AddressedMemoryImageCreateResult.InvalidSegment -> errors +=
                    DesktopProjectBuildError.InvalidRawBinaryRange(
                        memorySegmentOwners[created.segmentIndex],
                        created.startAddress,
                        created.size,
                    )
                is AddressedMemoryImageCreateResult.Overlap -> errors += DesktopProjectBuildError.MemoryOverlap(
                    memorySegmentOwners[created.segmentIndex],
                    memorySegmentOwners[created.previousSegmentIndex],
                    created.address,
                )
            }
        }

        if (errors.isNotEmpty()) return DesktopProjectBuildResult.Failure(errors)
        return DesktopProjectBuildResult.Success(DesktopProjectArtifact(basicProgram, memoryImage))
    }
}
