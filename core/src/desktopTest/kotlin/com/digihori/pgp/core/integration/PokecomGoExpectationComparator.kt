package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.EmulatorSession

internal data class PokecomGoComparisonReport(
    val cpuFields: List<String>,
    val internalRamByteDifferences: Int,
    val memoryByteDifferences: Map<String, Int>,
    val displayDotDifferences: Int,
    val displayMetadataMatches: Boolean,
)

internal object PokecomGoExpectationComparator {
    fun compare(session: EmulatorSession, expected: GoldenExpectation): PokecomGoComparisonReport {
        val cpu = session.cpuSnapshot()
        val cpuDifferences = buildList {
            expected.cpu?.programCounter?.let { if (it.hexInt() != cpu.programCounter) add("programCounter") }
            expected.cpu?.currentProgramCounter?.let {
                if (it.hexInt() != cpu.currentProgramCounter) add("currentProgramCounter")
            }
            expected.cpu?.opcode?.let { if (it.hexInt() != cpu.opcode) add("opcode") }
            expected.cpu?.dataPointer?.let { if (it.hexInt() != cpu.dataPointer) add("dataPointer") }
            expected.cpu?.p?.let { if (it.hexInt() != cpu.p) add("p") }
            expected.cpu?.q?.let { if (it.hexInt() != cpu.q) add("q") }
            expected.cpu?.r?.let { if (it.hexInt() != cpu.r) add("r") }
            expected.cpu?.d?.let { if (it.hexInt() != cpu.d) add("d") }
            expected.cpu?.alu?.let { if (it.hexInt() != cpu.alu) add("alu") }
            expected.cpu?.carry?.let { if (it != cpu.carry) add("carry") }
            expected.cpu?.zero?.let { if (it != cpu.zero) add("zero") }
            expected.cpu?.xInput?.let { if (it.hexInt() != cpu.xInput) add("xInput") }
            expected.cpu?.powerOn?.let { if (it != cpu.powerOn) add("powerOn") }
            expected.cpu?.ia?.let { if (it.hexInt() != cpu.ia) add("ia") }
            expected.cpu?.ib?.let { if (it.hexInt() != cpu.ib) add("ib") }
            expected.cpu?.fo?.let { if (it.hexInt() != cpu.fo) add("fo") }
            expected.cpu?.control?.let { if (it.hexInt() != cpu.control) add("control") }
            expected.cpu?.testPort?.let { if (it.hexInt() != cpu.testPort) add("testPort") }
        }
        val internalDifferences = expected.cpu?.internalRamHex?.hexBytes()
            ?.differenceCount(cpu.copyInternalRam()) ?: 0
        val memoryDifferences = expected.memory.orEmpty().associate { range ->
            val expectedBytes = range.bytesHex.hexBytes()
            range.start to expectedBytes.differenceCount(
                session.memorySnapshot(range.start.hexInt(), expectedBytes.size).copyBytes(),
            )
        }
        val display = session.displaySnapshot()
        val expectedDisplay = expected.display
        val displayDotDifferences = expectedDisplay?.rows?.withIndex()?.sumOf { (rowIndex, row) ->
            row.indices.count { column ->
                (row[column] == '1') != display.isDotOn(column, rowIndex)
            }
        } ?: 0
        val metadataMatches =
            (expectedDisplay?.enabled == null || expectedDisplay.enabled == display.enabled) &&
                (expectedDisplay?.symbols == null || expectedDisplay.symbols == display.symbols.map { it.name }.sorted())

        return PokecomGoComparisonReport(
            cpuFields = cpuDifferences,
            internalRamByteDifferences = internalDifferences,
            memoryByteDifferences = memoryDifferences,
            displayDotDifferences = displayDotDifferences,
            displayMetadataMatches = metadataMatches,
        )
    }

    private fun ByteArray.differenceCount(other: ByteArray): Int {
        require(size == other.size) { "Cannot compare byte arrays of different sizes" }
        return indices.count { this[it] != other[it] }
    }

    private fun String.hexInt(): Int = removePrefix("0x").toInt(16)

    private fun String.hexBytes(): ByteArray {
        require(length % 2 == 0) { "Hex byte string must have an even length" }
        return ByteArray(length / 2) { index -> substring(index * 2, index * 2 + 2).toInt(16).toByte() }
    }
}
