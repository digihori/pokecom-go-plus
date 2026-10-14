package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import kotlin.test.*

class DebuggerHistoryTest {
    @Test fun rangeSupportsBlankLimitsAndExistingHexSyntaxAndRejectsInvalidInput() {
        assertEquals(DebuggerHistoryRange.Valid(0..0xffff), historyRange("", ""))
        assertEquals(DebuggerHistoryRange.Valid(0xc000..0xc010), historyRange("0xC000", "c010"))
        assertEquals(DebuggerHistoryRange.Valid(0xffff..0xffff), historyRange("FFFF", ""))
        assertEquals(DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.START), historyRange("xyz", ""))
        assertEquals(DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.END), historyRange("", "10000"))
        assertEquals(DebuggerHistoryRange.Invalid(DebuggerHistoryFilterError.RANGE), historyRange("C010", "C000"))
    }

    @Test fun accessFiltersUseAccessAddressKeepSameValueWritesAndReturnNewestFirst() {
        val entries = listOf(
            DesktopMemoryAccessHistoryEntry(0, 0x100, MemoryAccess(MemoryAccessKind.READ, 0, 0x10)),
            DesktopMemoryAccessHistoryEntry(1, 0x103, MemoryAccess(MemoryAccessKind.WRITE, 0xc000, 0)),
            DesktopMemoryAccessHistoryEntry(2, 0x104, MemoryAccess(MemoryAccessKind.READ, 0xc000, 0)),
        )
        assertEquals(listOf(2L, 1L), DebuggerHistoryModel.accesses(entries, DebuggerAccessFilter.ALL, 0xc000..0xc000).map { it.sequence })
        assertEquals(listOf(2L), DebuggerHistoryModel.accesses(entries, DebuggerAccessFilter.READ, 0xc000..0xc000).map { it.sequence })
        assertEquals(listOf(1L), DebuggerHistoryModel.accesses(entries, DebuggerAccessFilter.WRITE, 0xc000..0xc000).map { it.sequence })
        assertTrue(DebuggerHistoryModel.accesses(entries, DebuggerAccessFilter.ALL, 0x100..0x104).isEmpty())
        assertEquals("#1 PC=0103 W:C000=00", entries[1].historyText())
        assertEquals(listOf(0L, 1L, 2L), entries.map { it.sequence })
    }

    @Test fun bankFilterMatchesBothSidesWithoutGuessingInstructionPc() {
        val entries = listOf(DesktopBankHistoryEntry(0, BankSwitchEvent(0, 1, 0xf9)),
            DesktopBankHistoryEntry(1, BankSwitchEvent(1, 2, 2)), DesktopBankHistoryEntry(2, BankSwitchEvent(2, 0, 0)))
        assertEquals(listOf(1L, 0L), DebuggerHistoryModel.banks(entries, 1).map { it.sequence })
        assertEquals(listOf(2L, 1L, 0L), DebuggerHistoryModel.banks(entries, null).map { it.sequence })
        assertEquals("#0 bank-0 → bank-1", entries[0].historyText())
    }

    @Test fun captureAndFilterPreserveWatchTraceRevisionAndDebugContextJson() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52, 0x57))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        runner.setDebugObservationEnabled(true)
        runner.setInstructionTraceEnabled(true)
        runner.setMemoryAccessWatch(0xc000, 0xc000, reads = true, writes = true)
        runner.run(); runner.tick()
        assertEquals(MemoryAccessKind.WRITE, assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason).accesses.single().kind)
        val service = DesktopDebugContextService(runner, "history-test", { "2026-10-14T00:00:00Z" })
        fun context() = DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(service.capture(DebugContextRequest())).context)
        val before = context()
        val revision = runner.sessionRevision
        val trace = runner.instructionTrace()
        val snapshot = captureDebuggerHistory(runner)
        DebuggerHistoryModel.accesses(snapshot.accesses, DebuggerAccessFilter.WRITE, 0xc000..0xc000)
        assertEquals(revision, runner.sessionRevision)
        assertEquals(trace, runner.instructionTrace())
        assertEquals(snapshot.accesses, runner.memoryAccessHistory())
        assertContentEquals(before, context())
        runner.run(); runner.tick()
        assertEquals(MemoryAccessKind.READ, assertIs<DebuggerStopReason.MemoryAccessed>(runner.stopReason).accesses.single().kind)
        assertEquals(2, runner.memoryAccessHistory().count { it.access.address == 0xc000 })
    }

    @Test fun boundedAccessHistoryAndDebuggerCloseReopenDoNotEraseRetainedEntries() {
        val runner = debuggerTestRunner(byteArrayOf(0x10, 0xc0.toByte(), 0, 0x02, 0x41, 0x52, 0x57, 0x79, 0, 0))
        runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
        runner.setDebugObservationEnabled(true)
        runner.run(); repeat(5) { runner.tick() }; runner.pause()
        val snapshot = captureDebuggerHistory(runner)
        assertEquals(4096, snapshot.accesses.size)
        assertTrue(snapshot.accesses.first().sequence > 0)
        runner.setDebugObservationEnabled(false)
        assertFalse(captureDebuggerHistory(runner).observationEnabled)
        assertEquals(snapshot.accesses, captureDebuggerHistory(runner).accesses)
        runner.setDebugObservationEnabled(true)
        assertEquals(snapshot.accesses, captureDebuggerHistory(runner).accesses)
        runner.reset()
        assertTrue(captureDebuggerHistory(runner).accesses.isEmpty())
        runner.step()
        assertEquals(0L, captureDebuggerHistory(runner).accesses.first().sequence)
    }

    @Test fun bankHistoryIsBoundedAndContinuouslyRecordedOnAllThreeBankedMachines() {
        for ((id, count, selector) in listOf(Triple("pc-1360", 8, 0x3400), Triple("pc-1460", 4, 0x3c00), Triple("pc-1470u", 8, 0x3400))) {
            val program = byteArrayOf(0x10, (selector ushr 8).toByte(), 0, 0x02, 1, 0x52, 0x02, 0, 0x52, 0x79, 0, 0)
            val runner = bankRunner(id, count, program)
            runner.setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED)
            runner.run(); repeat(20) { runner.tick() }; runner.pause()
            val revision = runner.sessionRevision
            val snapshot = captureDebuggerHistory(runner)
            assertFalse(snapshot.observationEnabled)
            assertEquals(4096, snapshot.banks.size, id)
            assertTrue(snapshot.banks.first().sequence > 0, id)
            assertEquals(count, snapshot.banking?.bankCount)
            assertEquals(0x4000, snapshot.banking?.windowStart)
            assertEquals(revision, runner.sessionRevision)
            runner.reset()
            assertTrue(captureDebuggerHistory(runner).banks.isEmpty(), id)
        }
    }

    @Test fun missingSessionAndNonBankedMachineReportUnsupportedRatherThanInventingEvents() {
        assertNull(captureDebuggerHistory(null).state)
        assertTrue(captureDebuggerHistory(null).accesses.isEmpty())
        assertNull(captureDebuggerHistory(debuggerTestRunner()).banking)
        assertNull(captureDebuggerHistory(debuggerTestRunner()).selectedBank)
    }
}
