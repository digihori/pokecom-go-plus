package com.digihori.pgp.core.integration

import com.digihori.pgp.core.api.CreateSessionResult
import com.digihori.pgp.core.api.EmulatorFactory
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InstructionTraceRecorderTest {
    @Test
    fun recordsInstructionBoundariesAndKeepsOnlyTheRequestedTail() {
        val session = sessionWithRepeatedOpcode(0x33)

        val trace = InstructionTraceRecorder.recordUntil(session, cycleBudget = 7, tailCapacity = 2)

        assertEquals(3, trace.executedInstructions)
        assertEquals(9, trace.executedCycles)
        assertEquals(listOf(2L, 3L), trace.tail.map { it.instruction })
        assertEquals(listOf(1, 2), trace.tail.map { it.pc })
        assertEquals(listOf(3L, 6L), trace.tail.map { it.cycleBefore })
        assertEquals(listOf(0x33, 0x33), trace.tail.map { it.opcode })
    }

    @Test
    fun writesAStableTsvFormatForCrossImplementationComparison() {
        val trace = InstructionTraceRecorder.recordUntil(
            sessionWithRepeatedOpcode(0x33),
            cycleBudget = 3,
            tailCapacity = 1,
        )

        val lines = trace.toTsv().lines().filter(String::isNotEmpty)
        assertEquals(
            "instruction\tcycleBefore\tcycles\tpc\topcode\tpcAfter\tq\tib\ttestPort",
            lines.first(),
        )
        assertTrue(lines.single { it != lines.first() }.contains("\t0000\t33\t0001\t"))
    }

    private fun sessionWithRepeatedOpcode(opcode: Int) =
        assertIs<CreateSessionResult.Success>(
            EmulatorFactory.create(
                Pc1245RomDefinition.MACHINE_ID,
                assertIs<RomImportResult.Success>(
                    Pc1245FlatRomImporter.importImage(ByteArray(0x10000) { opcode.toByte() }),
                ).romSet,
            ),
        ).session
}
