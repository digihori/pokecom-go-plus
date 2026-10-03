package com.digihori.pgp.core.api

import com.digihori.pgp.core.rom.MachineId

public interface EmulatorSession {
    public val machineId: MachineId

    public fun reset()
    public fun step(): StepResult
    public fun runCycles(cycleBudget: Long): RunResult

    public fun pressKey(key: PocketKey): InputResult
    public fun releaseKey(key: PocketKey): InputResult
    public fun setOperatingMode(mode: OperatingMode)

    public fun cpuSnapshot(): CpuSnapshot
    public fun memorySnapshot(startAddress: Int, length: Int): MemorySnapshot
    public fun displaySnapshot(): DisplaySnapshot
    public fun audioSnapshot(): AudioSnapshot
}

public data class StepResult(
    public val cycles: Int,
    public val status: ExecutionStatus,
)

public data class RunResult(
    public val executedCycles: Long,
    public val executedInstructions: Long,
    public val status: ExecutionStatus,
)

public sealed interface ExecutionStatus {
    public data object Ready : ExecutionStatus
    public data class Faulted(public val fault: CoreFault) : ExecutionStatus
}

public sealed interface CoreFault {
    public data class UnsupportedOpcode(
        public val address: Int,
        public val opcode: Int,
    ) : CoreFault
}

public enum class PocketKey {
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    NUM_0, NUM_1, NUM_2, NUM_3, NUM_4,
    NUM_5, NUM_6, NUM_7, NUM_8, NUM_9,
    ENTER, SPACE, SHIFT, DEF, BREAK,
    PLUS, MINUS, MULTIPLY, DIVIDE, DOT, EQUALS,
    LEFT, RIGHT, UP, DOWN, CLEAR,
}

public enum class OperatingMode {
    RUN,
    PROGRAM,
}

public sealed interface InputResult {
    public data object Accepted : InputResult
    public data class UnsupportedKey(public val key: PocketKey) : InputResult
}

public class CpuSnapshot internal constructor(
    public val programCounter: Int,
    public val currentProgramCounter: Int,
    public val opcode: Int,
    public val dataPointer: Int,
    public val p: Int,
    public val q: Int,
    public val r: Int,
    public val d: Int,
    public val alu: Int,
    public val carry: Boolean,
    public val zero: Boolean,
    public val xInput: Int,
    public val powerOn: Boolean,
    public val ia: Int,
    public val ib: Int,
    public val fo: Int,
    public val control: Int,
    public val testPort: Int,
    internalRam: ByteArray,
) {
    private val internalRamContent: ByteArray = internalRam.copyOf()

    public val internalRamSize: Int
        get() = internalRamContent.size

    public fun copyInternalRam(): ByteArray = internalRamContent.copyOf()
}

public class MemorySnapshot internal constructor(
    public val startAddress: Int,
    bytes: ByteArray,
) {
    private val content: ByteArray = bytes.copyOf()

    public val size: Int
        get() = content.size

    public fun copyBytes(): ByteArray = content.copyOf()
}

public enum class DisplaySymbol {
    BUSY,
    P,
    DEF,
    DE,
    G,
    RAD,
    SHIFT,
    RUN,
    PRO,
}

public class DisplaySnapshot internal constructor(
    public val characterColumns: Int,
    public val characterWidth: Int,
    public val dotRows: Int,
    public val symbols: List<DisplaySymbol>,
    public val enabled: Boolean,
    public val revision: Long,
    dots: ByteArray,
) {
    private val content: ByteArray = dots.copyOf()

    public val dotColumns: Int
        get() = characterColumns * characterWidth

    init {
        require(content.size == dotColumns * dotRows) { "Display dot array has an invalid size" }
    }

    public fun isDotOn(column: Int, row: Int): Boolean {
        require(column in 0 until dotColumns) { "Display column is out of range" }
        require(row in 0 until dotRows) { "Display row is out of range" }
        return enabled && content[row * dotColumns + column].toInt() != 0
    }

    public fun copyDots(): ByteArray = content.copyOf()
}

public data class AudioSnapshot(
    /** Zero means that no tone is currently requested. */
    public val frequencyHz: Int,
    public val revision: Long,
)
