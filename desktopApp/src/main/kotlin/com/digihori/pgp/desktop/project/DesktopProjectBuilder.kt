package com.digihori.pgp.desktop.project

import com.digihori.pgp.core.project.ProjectSourceType
import com.digihori.pgp.core.api.MachineMemoryLayout
import com.digihori.pgp.core.api.MemoryImageLoadError
import com.digihori.pgp.core.api.MemoryPlacementValidator
import com.digihori.pgp.core.api.ProgramImageLoader
import com.digihori.pgp.core.api.ProgramImagePlanResult
import com.digihori.pgp.core.api.BasicProgramMemoryError
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
    val assemblySources: List<DesktopAssemblySourceResult>,
    val targetMachineId: com.digihori.pgp.core.rom.MachineId? = null,
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
    data class Assembly(val sourceId: String, val line: Int, val message: String) : DesktopProjectBuildError
    data class Placement(val sourceId: String, val error: MemoryImageLoadError) : DesktopProjectBuildError
    data class BasicPlacement(val error: BasicProgramMemoryError) : DesktopProjectBuildError
}

/** Compiles every source before an artifact is allowed to modify an emulator session. */
internal object DesktopProjectBuilder {
    fun build(workspace: DesktopProjectWorkspace, layout: MachineMemoryLayout = MachineMemoryLayout(workspace.definition.machineId),
        validateForLoad: Boolean = false,
        compiledAssembly: DesktopAssemblyWorkspaceResult = DesktopAssemblyWorkspaceCompiler.compile(workspace, layout, validatePlacement = validateForLoad)): DesktopProjectBuildResult {
        val errors = mutableListOf<DesktopProjectBuildError>()
        val basicSources = workspace.sources.filter { it.definition.type == ProjectSourceType.BASIC }
        if (basicSources.size > 1) {
            errors += DesktopProjectBuildError.MultipleBasicSources(basicSources.map { it.definition.id })
        }

        var basicProgram: ByteArray? = null
        val memorySegments = mutableListOf<AddressedMemorySegmentData>()
        val memorySegmentOwners = mutableListOf<String>()
        val assemblySources = when (val assembly = compiledAssembly) {
            is DesktopAssemblyWorkspaceResult.Success -> assembly.sources
            is DesktopAssemblyWorkspaceResult.Failure -> {
                assembly.diagnostics.forEach { diagnostic ->
                    errors += DesktopProjectBuildError.Assembly(
                        diagnostic.source.definition.id,
                        diagnostic.line ?: 0,
                        diagnostic.message,
                    )
                }
                emptyList()
            }
        }
        val assemblyById = assemblySources.associateBy { it.source.definition.id }

        workspace.sources.forEach { source ->
            if (source.definition.type == ProjectSourceType.ASSEMBLY) {
                assemblyById[source.definition.id]?.image?.segments?.forEach { segment ->
                    memorySegments += AddressedMemorySegmentData(
                        segment.startAddress,
                        segment.copyBytes(),
                        segment.sourceLine,
                    )
                    memorySegmentOwners += source.definition.id
                }
                return@forEach
            }
            val bytes = runCatching { source.file.readBytes() }.getOrElse {
                errors += DesktopProjectBuildError.CouldNotReadSource(
                    source.definition.id,
                    it.message ?: it::class.simpleName.orEmpty(),
                )
                return@forEach
            }
            when (source.definition.type) {
                ProjectSourceType.BASIC -> when (val compiled = DesktopBasicLoader.compileProgram(bytes, layout.machineId)) {
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
                ProjectSourceType.ASSEMBLY -> error("Handled before reading source bytes")
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

        if (validateForLoad) memoryImage?.let { image ->
            fun owner(address: Int): String = memorySegmentOwners[memorySegments.indexOfFirst { address >= it.startAddress && address < it.startAddress + it.size }]
            val invalid = MemoryPlacementValidator.validate(image, layout)
            if (invalid != null) {
                val address = invalid.error.address
                errors += DesktopProjectBuildError.Placement(owner(address), invalid.error)
            } else when (val planned = ProgramImageLoader.planForBuild(layout, basicProgram, image)) {
                is ProgramImagePlanResult.BasicFailure -> errors += DesktopProjectBuildError.BasicPlacement(planned.error)
                is ProgramImagePlanResult.MemoryFailure -> {
                    val address = planned.error.address
                    // A BASIC-only address can be rejected for a hardware RAM mode.
                    val sourceId = if (memorySegments.any { address >= it.startAddress && address < it.startAddress + it.size }) owner(address)
                        else basicSources.singleOrNull()?.definition?.id ?: "BASIC"
                    errors += DesktopProjectBuildError.Placement(sourceId, planned.error)
                }
                is ProgramImagePlanResult.Success -> Unit
            }
        }
        if (validateForLoad && memoryImage == null && basicProgram != null) {
            when (val planned = ProgramImageLoader.planForBuild(layout, basicProgram, null)) {
                is ProgramImagePlanResult.BasicFailure -> errors += DesktopProjectBuildError.BasicPlacement(planned.error)
                is ProgramImagePlanResult.MemoryFailure -> errors += DesktopProjectBuildError.Placement(basicSources.single().definition.id, planned.error)
                is ProgramImagePlanResult.Success -> Unit
            }
        }
        if (errors.isNotEmpty()) return DesktopProjectBuildResult.Failure(errors)
        return DesktopProjectBuildResult.Success(DesktopProjectArtifact(basicProgram, memoryImage, assemblySources, layout.machineId))
    }
}
