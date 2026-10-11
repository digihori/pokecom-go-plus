package com.digihori.pgp.core.api

import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicMemoryResult
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245BasicProgramMemory
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251BasicProgramMemory
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261BasicProgramMemory
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350BasicProgramMemory
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360BasicProgramMemory
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401BasicProgramMemory
import com.digihori.pgp.core.source.machine.AddressedMemoryImage
import com.digihori.pgp.core.source.machine.AddressedMemoryImageCreateResult
import com.digihori.pgp.core.source.machine.AddressedMemoryImageFactory
import com.digihori.pgp.core.source.machine.AddressedMemorySegmentData

public sealed interface ProgramImageLoadResult {
    public data class Success(public val basicByteCount: Int, public val memorySegmentCount: Int, public val memoryByteCount: Int) : ProgramImageLoadResult
    public data class BasicFailure(public val error: BasicProgramMemoryError) : ProgramImageLoadResult
    public data class MemoryFailure(public val error: MemoryImageLoadError) : ProgramImageLoadResult
}

public sealed interface ProgramImagePlanResult {
    public data class Success(public val image: AddressedMemoryImage?) : ProgramImagePlanResult
    public data class BasicFailure(public val error: BasicProgramMemoryError) : ProgramImagePlanResult
    public data class MemoryFailure(public val error: MemoryImageLoadError) : ProgramImagePlanResult
}

public object ProgramImageLoader {
    /** Reuses the machine's BASIC writer against a recording sink, including pointer updates and old-text clearing. */
    public fun plan(
        layout: MachineMemoryLayout,
        program: ByteArray?,
        image: AddressedMemoryImage?,
        read: (Int) -> Int,
    ): ProgramImagePlanResult {
        image?.let { MemoryPlacementValidator.validate(it, layout)?.let { issue ->
            return ProgramImagePlanResult.MemoryFailure(issue.error)
        } }
        val writes = linkedMapOf<Int, Int>()
        if (program != null) {
            val write: (Int, Int) -> Unit = { address, value -> writes[address] = value and 0xff }
            val result = when (layout.machineId.value) {
                "pc-1245" -> Pc1245BasicProgramMemory.load(program, read, write)
                "pc-1250", "pc-1251", "pc-1255" -> Pc1251BasicProgramMemory.load(program, read, write)
                "pc-1261" -> Pc1261BasicProgramMemory.load(program, read, write)
                "pc-1350" -> Pc1350BasicProgramMemory.load(program, read, write)
                "pc-1360" -> Pc1360BasicProgramMemory.load(program, read, write)
                else -> Pc1401BasicProgramMemory.load(program, read, write)
            }
            when (result) {
                is Pc1245BasicMemoryResult.InvalidPointers -> return ProgramImagePlanResult.BasicFailure(BasicProgramMemoryError.InvalidPointer(result.start, result.end))
                is Pc1245BasicMemoryResult.InvalidProgram -> return ProgramImagePlanResult.BasicFailure(BasicProgramMemoryError.InvalidProgram(result.offset, result.reason))
                is Pc1245BasicMemoryResult.TooLarge -> return ProgramImagePlanResult.BasicFailure(BasicProgramMemoryError.ProgramTooLarge(result.size, result.capacity))
                is Pc1245BasicMemoryResult.Success -> Unit
            }
        }
        val reserved = writes.keys.map { layout.locate(it).physicalAddress }.toSet()
        image?.segments?.forEach { segment ->
            for (address in segment.startAddress..segment.endAddress) {
                if (layout.locate(address).physicalAddress in reserved) {
                    return ProgramImagePlanResult.MemoryFailure(MemoryImageLoadError.BasicOverlap(address, segment.sourceLine))
                }
            }
        }
        val segments = mutableListOf<AddressedMemorySegmentData>()
        // Preserve source lines on machine-code segments. BASIC writes are compacted into contiguous runs.
        val addresses = writes.keys.sorted()
        var offset = 0
        while (offset < addresses.size) {
            val start = offset
            while (offset + 1 < addresses.size && addresses[offset + 1] == addresses[offset] + 1) offset++
            segments += AddressedMemorySegmentData(addresses[start], ByteArray(offset - start + 1) { writes.getValue(addresses[start + it]).toByte() })
            offset++
        }
        image?.segments?.forEach { segments += AddressedMemorySegmentData(it.startAddress, it.copyBytes(), it.sourceLine) }
        if (segments.isEmpty()) return ProgramImagePlanResult.Success(null)
        val combined = checkNotNull((AddressedMemoryImageFactory.create(segments) as? AddressedMemoryImageCreateResult.Success)?.image)
        MemoryPlacementValidator.validate(combined, layout)?.let { return ProgramImagePlanResult.MemoryFailure(it.error) }
        return ProgramImagePlanResult.Success(combined)
    }

    /** Build-time estimate only. Load always plans again using the actual session pointers and memory mode. */
    public fun planForBuild(layout: MachineMemoryLayout, program: ByteArray?, image: AddressedMemoryImage?): ProgramImagePlanResult {
        val start = layout.defaultBasicStartAddress
        val low = layout.basicStartPointerAddress
        return plan(layout, program, image) { address -> when (address) {
            low -> start and 0xff
            low + 1 -> start ushr 8
            low + 2 -> (start - 1) and 0xff
            low + 3 -> (start - 1) ushr 8
            else -> 0
        } }
    }

    internal fun load(session: EmulatorSession, program: ByteArray?, image: AddressedMemoryImage?): ProgramImageLoadResult {
        val memory = if (program == null) null else session.memorySnapshot(0, 0x10000).copyBytes()
        return when (val planned = plan(session.memoryLayout, program, image) { checkNotNull(memory)[it].toInt() and 0xff }) {
            is ProgramImagePlanResult.BasicFailure -> ProgramImageLoadResult.BasicFailure(planned.error)
            is ProgramImagePlanResult.MemoryFailure -> ProgramImageLoadResult.MemoryFailure(planned.error)
            is ProgramImagePlanResult.Success -> {
                planned.image?.let { when (val loaded = session.loadMemoryImage(it)) {
                    is MemoryImageLoadResult.Failure -> return ProgramImageLoadResult.MemoryFailure(loaded.error)
                    is MemoryImageLoadResult.Success -> Unit
                } }
                ProgramImageLoadResult.Success(program?.size ?: 0, image?.segments?.size ?: 0, image?.byteCount ?: 0)
            }
        }
    }
}
