package com.digihori.pgp.desktop.localization

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.*
import com.digihori.pgp.desktop.debug.*
import com.digihori.pgp.desktop.application.debug.*
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import kotlin.test.*

internal fun debuggerTestRunner(program: ByteArray = byteArrayOf()): DesktopEmulatorRunner {
    val internalRom = ByteArray(0x2000).also { program.copyInto(it) }
    val rom = RomSet(Pc1245RomDefinition.MACHINE_ID, listOf(
        RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internalRom),
        RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL, ByteArray(0x4000))))
    return DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(Pc1245RomDefinition.MACHINE_ID, rom)).session)
}

class StudioDebuggerStringsTest {
    private val en = StudioStrings(StudioLanguage.ENGLISH)
    private val ja = StudioStrings(StudioLanguage.JAPANESE)

    @Test fun allStopReasonsKeepLegacyCheckpointTextAndHex() {
        val reasons = listOf(
            DebuggerStopReason.Reset to "Reset",
            DebuggerStopReason.UserPause to "Paused by user",
            DebuggerStopReason.StepComplete to "Step complete",
            DebuggerStopReason.Breakpoint(0xABCD) to "Breakpoint at 0xABCD",
            DebuggerStopReason.RunToAddress(0xABCD) to "Run to address reached at 0xABCD",
            DebuggerStopReason.MemoryChanged(0xABCD, List(5) { MemoryValueChange(0xC000, 0x12, 0xFF) }) to
                "Memory changed after 0xABCD: C000:12→FF, C000:12→FF, C000:12→FF, C000:12→FF (+1 more)",
            DebuggerStopReason.MemoryAccessed(0xABCD, List(5) { MemoryAccess(MemoryAccessKind.WRITE, 0xC000, 0xFF) }) to
                "Memory access by 0xABCD: W:C000=FF, W:C000=FF, W:C000=FF, W:C000=FF (+1 more)",
            DebuggerStopReason.Fault(CoreFault.UnsupportedOpcode(0xABCD, 0xFF)) to
                "Fault: Emulation stopped: unsupported opcode 0xFF at PC=0xABCD.")
        val runner = debuggerTestRunner()
        for ((reason, legacy) in reasons) {
            assertEquals(legacy, reason.displayName(en))
            assertNotEquals(legacy, reason.displayName(ja))
            assertEquals(legacy, reason.checkpointDescription())
            fun bytes(strings: StudioStrings): ByteArray {
                reason.displayName(strings)
                return DesktopDebuggerCheckpointWriter.write(runner.machineId.value, "2026-10-14T00:00:00Z", 123,
                    runner.cpuSnapshot(), runner.memorySnapshot(0xC000, 4), emptyList(), reason.checkpointDescription())
            }
            assertContentEquals(bytes(en), bytes(ja))
            if (legacy.contains("ABCD")) assertTrue(reason.displayName(ja).contains("ABCD"))
        }
        assertTrue(DebuggerStopReason.MemoryAccessed(0xABCD, listOf(MemoryAccess(MemoryAccessKind.READ, 0xC000, 0x12))).displayName(ja).contains("R:C000=12"))
    }

    @Test fun contextJsonAndProtocolErrorsStayIndependentOfUiLanguage() {
        val service = DesktopDebugContextService(debuggerTestRunner(), sessionId = "localization-test",
            capturedAtProvider = { "2026-10-14T00:00:00Z" }, executedCyclesProvider = { 123L })
        val request = DebugContextRequest(memoryRanges = listOf(DebugMemoryRangeRequest(0xC000, 4)),
            disassemblyStartAddress = 0, disassemblyInstructionCount = 2)
        fun bytes(strings: StudioStrings): ByteArray {
            DebuggerStopReason.Reset.displayName(strings)
            return DebugContextJsonWriter.write(assertIs<DebugContextResult.Success>(service.capture(request)).context)
        }
        assertContentEquals(bytes(en), bytes(ja))
        val error = assertIs<DebugContextError.InvalidRequest>(assertIs<DebugContextResult.Failure>(
            service.capture(DebugContextRequest(disassemblyInstructionCount = 1))).error)
        val original = error.message
        assertEquals(original, error.displayMessage(en))
        assertNotEquals(original, error.displayMessage(ja))
        assertEquals(original, error.message)
        for (message in listOf("At most 8 memory ranges may be requested", "At most 4096 memory bytes may be requested",
            "History and trace limits must be between 0 and 4096")) {
            assertEquals(message, en.debugContextRequestError(message))
            assertTrue(ja.debugContextRequestError(message).contains(if (message.contains("4096")) "4096" else "8"))
        }
        val raw = "future error: /日本語/{file}/1.25"
        assertTrue(ja.debugContextRequestError(raw).contains(raw))
    }

    @Test fun registerIdentifiersAndCoreRegionValuesRemainOpaque() {
        for (field in CpuField.entries) {
            if (field != CpuField.INTERNAL_RAM) assertEquals(field.uiLabel(en), field.uiLabel(ja))
        }
        val regions = MachineCatalog.find(Pc1245RomDefinition.MACHINE_ID)!!.memoryRegions
        val before = regions.map { it.copy() }
        for (region in regions) {
            assertEquals(region.displayName, en.memoryRegionLabel(region))
            assertTrue(ja.memoryRegionLabel(region).isNotBlank())
        }
        assertEquals(before, regions)
    }
}
