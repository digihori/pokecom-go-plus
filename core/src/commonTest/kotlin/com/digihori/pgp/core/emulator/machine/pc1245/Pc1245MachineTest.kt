package com.digihori.pgp.core.emulator.machine.pc1245

import com.digihori.pgp.core.emulator.cpu.Sc61860StopReason
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class Pc1245MachineTest {
    @Test
    fun constructionStartsFromAColdReset() {
        val machine = machineWithProgram(0x02, 0x55)

        assertEquals(0, machine.cpuState.programCounter)
        assertEquals(0x60, machine.cpuState.r)
        assertEquals(0, machine.cpuState.internalRam.sum())
        assertEquals(0, machine.readMemory(0x8000))
    }

    @Test
    fun stepExecutesOneInstructionFromTheInstalledRom() {
        val machine = machineWithProgram(0x02, 0x55)

        val result = machine.step()

        assertEquals(4, result.cycles)
        assertNull(result.stopReason)
        assertEquals(0x55, machine.cpuState.internalRam[0x02])
        assertEquals(2, machine.cpuState.programCounter)
    }

    @Test
    fun runCyclesFinishesAtAnInstructionBoundary() {
        val machine = machineWithProgram(
            0x00, 0x11,
            0x01, 0x22,
        )

        val result = machine.runCycles(cycleBudget = 5)

        assertEquals(8, result.executedCycles)
        assertEquals(2, result.executedInstructions)
        assertNull(result.stopReason)
        assertEquals(4, machine.cpuState.programCounter)
    }

    @Test
    fun runCyclesStopsBeforeCountingAnUnsupportedInstruction() {
        val machine = machineWithProgram(
            0x33,
            0x16,
        )

        val result = machine.runCycles(cycleBudget = 100)

        assertEquals(3, result.executedCycles)
        assertEquals(1, result.executedInstructions)
        val reason = assertIs<Sc61860StopReason.UnsupportedOpcode>(result.stopReason)
        assertEquals(1, reason.address)
        assertEquals(0x16, reason.opcode)
    }

    @Test
    fun rejectsAZeroOrNegativeCycleBudget() {
        val machine = machineWithProgram(0x33)

        assertFailsWith<IllegalArgumentException> { machine.runCycles(0) }
        assertFailsWith<IllegalArgumentException> { machine.runCycles(-1) }
    }

    @Test
    fun coldResetClearsCpuAndRamButPreservesRom() {
        val machine = machineWithProgram(0x02, 0x55)
        machine.step()
        machine.writeMemory(0xe000, 0xaa)

        machine.coldReset()

        assertEquals(0, machine.cpuState.programCounter)
        assertEquals(0, machine.cpuState.internalRam[0x02])
        assertEquals(0, machine.readMemory(0xe000))
        assertEquals(0x02, machine.readMemory(0x0000))
        assertEquals(0x55, machine.readMemory(0x0001))
    }

    @Test
    fun romCodeReadsAKeySelectedThroughOutputA() {
        val machine = machineWithProgram(
            0x12, 0x5c, // LIP 5c
            0x02, 0x01, // LIA 01: select keyboard column 3
            0xdb,       // EXAM
            0x5d,       // OUTA
            0x4c,       // INA
        )
        machine.keyboardState.press(PocketKey.NUM_7)

        val result = machine.runCycles(cycleBudget = 15)

        assertNull(result.stopReason)
        assertEquals(0x02, machine.cpuState.internalRam[0x02])
        assertEquals(false, machine.cpuState.zero)
    }

    @Test
    fun romCodeReadsProgramModeThroughOutputB() {
        val machine = machineWithProgram(
            0x12, 0x5d, // LIP 5d
            0x02, 0x08, // LIA 08: select the RUN/PRO contact
            0xdb,       // EXAM
            0xdd,       // OUTB
            0xcc,       // INB
        )
        machine.keyboardState.setOperatingMode(OperatingMode.PROGRAM)

        val result = machine.runCycles(cycleBudget = 15)

        assertNull(result.stopReason)
        assertEquals(0x02, machine.cpuState.internalRam[0x02])
        assertEquals(false, machine.cpuState.zero)
    }

    @Test
    fun testInstructionConsumesTheBreakKeyOnSignal() {
        val machine = machineWithProgram(0x6b, 0x08)
        machine.keyboardState.press(PocketKey.BREAK)

        val result = machine.step()

        assertNull(result.stopReason)
        assertEquals(4, result.cycles)
        assertEquals(0x08, machine.cpuState.testPort)
        assertEquals(false, machine.cpuState.zero)
    }

    @Test
    fun romCodeControlsDisplayEnableThroughOutc() {
        val machine = machineWithProgram(
            0x12, 0x5f, // LIP 5f
            0x02, 0x00, // LIA 00: display off
            0xdb,       // EXAM
            0xdf,       // OUTC
        )

        val result = machine.runCycles(cycleBudget = 13)

        assertNull(result.stopReason)
        assertEquals(false, machine.displayState.enabled)
        assertEquals(1, machine.displayState.revision)
        assertEquals(0, machine.cpuState.control)

        machine.coldReset()
        assertEquals(true, machine.displayState.enabled)
        assertEquals(2, machine.displayState.revision)
    }

    @Test
    fun romCodeSelectsTheLogicalBuzzerToneThroughOutc() {
        val machine = machineWithProgram(
            0x12, 0x5f, // LIP 5f
            0x02, 0x20, // LIA 20: 2 kHz
            0xdb,       // EXAM
            0xdf,       // OUTC
        )

        machine.runCycles(cycleBudget = 13)

        assertEquals(2_000, machine.buzzerState.frequencyHz)
        assertEquals(1, machine.buzzerState.revision)

        machine.coldReset()
        assertEquals(0, machine.buzzerState.frequencyHz)
        assertEquals(2, machine.buzzerState.revision)
    }

    private fun machineWithProgram(vararg program: Int): Pc1245Machine {
        require(program.size <= Pc1245RomDefinition.INTERNAL_SIZE)
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE)
        program.forEachIndexed { index, value -> internal[index] = value.toByte() }

        return Pc1245Machine(
            RomSet(
                machineId = Pc1245RomDefinition.MACHINE_ID,
                components = listOf(
                    RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
                    RomComponent(
                        Pc1245RomDefinition.EXTERNAL_ID,
                        RomRole.EXTERNAL,
                        ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE),
                    ),
                ),
            ),
        )
    }
}
