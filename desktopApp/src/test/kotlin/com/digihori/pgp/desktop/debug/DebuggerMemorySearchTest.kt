package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.core.source.machine.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import kotlin.test.*

class DebuggerMemorySearchTest {
    @Test fun parsesBoundedBytePatternsAndUnsignedNumbers() {
        fun bytes(text: String, format: MemorySearchFormat, vararg expected: Int) =
            assertContentEquals(expected.map { it.toByte() }.toByteArray(), DebuggerMemorySearch.pattern(text, format))
        bytes("00 FF aB", MemorySearchFormat.HEX, 0, 255, 171)
        bytes("255", MemorySearchFormat.U8, 255)
        bytes("0x1234", MemorySearchFormat.U16_LE, 0x34, 0x12)
        bytes("4660", MemorySearchFormat.U16_BE, 0x12, 0x34)
        for (input in listOf("", "0", "GG", "0011", "00," , "00 ".repeat(65)))
            assertNull(DebuggerMemorySearch.pattern(input, MemorySearchFormat.HEX), input)
        for (input in listOf("-1", "256", "65536", "0x10000", "1.0", "1+1"))
            assertNull(DebuggerMemorySearch.pattern(input, MemorySearchFormat.U8), input)
        assertNull(DebuggerMemorySearch.pattern("65536", MemorySearchFormat.U16_LE))
        assertEquals(65535, DebuggerMemorySearch.address("0xFFFF"))
        for (input in listOf("-1", "10000", "", "xyz")) assertNull(DebuggerMemorySearch.address(input))
    }

    @Test fun findsOverlapsAndRangeEndpointsWithoutWrappingOrMutatingSession() {
        val runner = debuggerTestRunner()
        val image = assertIs<AddressedMemoryImageCreateResult.Success>(AddressedMemoryImageFactory.create(
            listOf(AddressedMemorySegmentData(0xc000, byteArrayOf(1, 1, 1, 2))))).image
        assertIs<MemoryImageLoadResult.Success>(runner.loadMemoryImage(image))
        runner.setDebugObservationEnabled(true)
        val revision = runner.sessionRevision
        val pc = runner.cpuSnapshot().programCounter
        assertEquals(listOf(0xc000, 0xc001), DebuggerMemorySearch.search(runner, 0xc000, 0xc003, byteArrayOf(1, 1)).addresses)
        assertEquals(listOf(0xc003), DebuggerMemorySearch.search(runner, 0xc003, 0xc003, byteArrayOf(2)).addresses)
        assertTrue(DebuggerMemorySearch.search(runner, 0xc003, 0xc003, byteArrayOf(2, 0)).addresses.isEmpty())
        assertTrue(DebuggerMemorySearch.search(runner, 0xffff, 0xffff, byteArrayOf(0, 0)).addresses.isEmpty())
        assertEquals(revision, runner.sessionRevision)
        assertEquals(pc, runner.cpuSnapshot().programCounter)
        assertTrue(runner.memoryAccessHistory().isEmpty())
        assertTrue(runner.instructionTrace().isEmpty())
    }

    @Test fun skipsBusHolesAndLcdAndBoundsResultsAndRejectsRunning() {
        val runner = debuggerTestRunner()
        val skipped = DebuggerMemorySearch.search(runner, 0x8000, 0xbfff, byteArrayOf(0))
        assertEquals(0x4000, skipped.skipped)
        assertTrue(skipped.addresses.isEmpty())
        assertEquals(256, DebuggerMemorySearch.search(runner, 0xf800, 0xf8ff, byteArrayOf(0)).skipped)
        val exact = DebuggerMemorySearch.search(runner, 0, 255, byteArrayOf(0))
        assertEquals(256, exact.addresses.size)
        assertFalse(exact.truncated)
        val bounded = DebuggerMemorySearch.search(runner, 0, 256, byteArrayOf(0))
        assertEquals(256, bounded.addresses.size)
        assertTrue(bounded.truncated)
        assertFailsWith<IllegalArgumentException> { DebuggerMemorySearch.search(runner, 2, 1, byteArrayOf(0)) }
        runner.run()
        assertFailsWith<IllegalArgumentException> { DebuggerMemorySearch.search(runner, 0, 2, byteArrayOf(0)) }
    }

    @Test fun searchesOnlyCurrentlySelectedBankWithoutSelectingAnotherBank() {
        val id = MachineId("pc-1360")
        val components = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, ByteArray(0x2000))) +
            List(8) { bank -> RomComponent(RomComponentId("bank-$bank"), RomRole.EXTERNAL, ByteArray(0x4000) { bank.toByte() }) }
        val runner = DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, components))).session)
        val bank = assertNotNull(runner.selectedRomBank())
        val result = DebuggerMemorySearch.search(runner, 0x4000, 0x4000, byteArrayOf(bank.toByte()))
        assertEquals(listOf(0x4000), result.addresses)
        assertEquals(bank, result.bank)
        assertEquals(bank, runner.selectedRomBank())
        assertTrue(DebuggerMemorySearch.search(runner, 0x4000, 0x4000, byteArrayOf(((bank + 1) % 8).toByte())).addresses.isEmpty())
        assertEquals(0x1000, DebuggerMemorySearch.search(runner, 0x2000, 0x2fff, byteArrayOf(0)).skipped)
    }
}
