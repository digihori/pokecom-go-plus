package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.rom.*
import com.digihori.pgp.desktop.localization.debuggerTestRunner
import com.digihori.pgp.desktop.runner.DesktopEmulatorRunner
import com.digihori.pgp.desktop.runner.DesktopExecutionSpeed
import com.digihori.pgp.desktop.application.debug.*
import kotlinx.serialization.json.*
import kotlin.test.*

internal fun bankBreakpointRunner(): DesktopEmulatorRunner {
    val id = MachineId("pc-1360")
    val internal = ByteArray(0x2000).apply {
        byteArrayOf(0x79, 0x40, 0).copyInto(this)
        byteArrayOf(0x10, 0x34, 0, 2, 1, 0x52, 0x79, 0x40, 0).copyInto(this, 0x100)
    }
    val bank = ByteArray(0x4000).apply { byteArrayOf(0x79, 1, 0).copyInto(this) }
    val parts = listOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal)) +
        List(8) { RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, bank.copyOf()) }
    val rom = RomSet(id, parts)
    return DesktopEmulatorRunner(assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, rom)).session,
        debugRomFingerprint = DebuggerSettingsFiles.romFingerprint(rom)).apply { setExecutionSpeed(DesktopExecutionSpeed.UNLIMITED) }
}

class DebuggerBankBreakpointTest {
    private fun save(result: DebuggerRegistrationResult) = assertIs<DebuggerRegistrationResult.Success>(result).id
    @Test fun sameLogicalAddressStopsInEachBankAndCountsOnlyMatchingBank() {
        val runner = bankBreakpointRunner()
        save(runner.saveBreakpoint(null, 0x4000, bank = 0))
        save(runner.saveBreakpoint(null, 0x4000, bank = 1))
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(0x4000), runner.stopReason)
        assertEquals(0, runner.selectedRomBank())
        assertEquals(listOf(1L, 0L), runner.breakpointRegistrations().map { it.hitCount })
        runner.run(); runner.tick()
        assertEquals(1, runner.selectedRomBank())
        assertEquals(listOf(1L, 1L), runner.breakpointRegistrations().map { it.hitCount })
        runner.run(); runner.tick()
        assertEquals(listOf(1L, 2L), runner.breakpointRegistrations().map { it.hitCount })
        val points = runner.breakpointRegistrations()
        runner.runToAddress(0x4000); runner.tick()
        assertIs<DebuggerStopReason.RunToAddress>(runner.stopReason)
        assertEquals(points, runner.breakpointRegistrations())
    }

    @Test fun togglingDeletingAndInvalidEditsPreserveOtherBankAndCounts() {
        val runner = bankBreakpointRunner()
        val zero = save(runner.saveBreakpoint(null, 0x4000, bank = 0))
        val one = save(runner.saveBreakpoint(null, 0x4000, bank = 1))
        val before = runner.breakpointRegistrations()
        val revision = runner.sessionRevision
        for (address in listOf(0, 0x8000, 0xc000)) assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.BANK), runner.saveBreakpoint(zero, address, bank = 0))
        for (bank in listOf(-1, 8, 256)) assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.BANK), runner.saveBreakpoint(zero, 0x4000, bank = bank))
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.DUPLICATE_ADDRESS), runner.saveBreakpoint(null, 0x4000))
        assertEquals(before, runner.breakpointRegistrations()); assertEquals(revision, runner.sessionRevision)
        runner.setBreakpointEnabled(zero, false)
        assertEquals(0, runner.breakpointRegistrations().first().bank)
        assertTrue(runner.breakpoints().isEmpty()) // Only bank 1 is enabled, current bank is 0.
        runner.removeBreakpoint(zero)
        runner.run(); runner.tick()
        assertEquals(DebuggerStopReason.Breakpoint(0x4000), runner.stopReason)
        assertEquals(1, runner.selectedRomBank())
        assertEquals(one, runner.breakpointRegistrations().single().id)
        assertEquals(1L, runner.breakpointRegistrations().single().hitCount)
        assertTrue(runner.breakpoints().contains(0x4000))
        runner.setBreakpointEnabled(one, false); runner.setBreakpointEnabled(one, true)
        assertEquals(1L, runner.breakpointRegistrations().single().hitCount)
        runner.saveBreakpoint(one, 0x4000, bank = null)
        assertNull(runner.breakpointRegistrations().single().bank)
        assertEquals(0L, runner.breakpointRegistrations().single().hitCount)
        assertEquals(DebuggerRegistrationResult.Failure(DebuggerRegistrationError.BANK), debuggerTestRunner().saveBreakpoint(null, 0x4000, bank = 0))
    }

    @Test fun bankMismatchDoesNotEvaluateConditionAndUnknownBankDoesNotMatch() {
        val store = DebuggerRegistrationStore { _, _ -> error("memory read on wrong bank") }
        save(store.saveBreakpoint(null, 0x4000, true, "MEM[0xC000] == 7", bank = 1))
        assertFalse(store.breakpointHit(0x4000, { error("CPU read on wrong bank") }, 0))
        assertFalse(store.breakpointHit(0x4000, { error("CPU read on unknown bank") }))
        assertEquals(0L, store.breakpoints().single().hitCount)
    }

    @Test fun settingsVersionTwoRoundTripsWhileLegacySettingsKeepVersionOne() {
        val runner = bankBreakpointRunner()
        save(runner.saveBreakpoint(null, 0x4000, condition = "A == 0", bank = 0))
        save(runner.saveBreakpoint(null, 0x4000, bank = 1))
        val saved = DebuggerSettingsFiles.capture(runner, null)
        val text = DebuggerSettingsFiles.encode(saved)
        assertEquals(2, Json.parseToJsonElement(text).jsonObject.getValue("schemaVersion").jsonPrimitive.int)
        assertEquals(saved, DebuggerSettingsFiles.decode(text))
        val root = Json.parseToJsonElement(text).jsonObject
        val points = root.getValue("breakpoints").jsonArray
        fun invalidBank(bank: JsonElement): String = JsonObject(root + ("breakpoints" to JsonArray(listOf(
            JsonObject(points.first().jsonObject + ("bank" to bank)),
        )))).toString()
        assertEquals(DebuggerSettingsError.FORMAT, assertFailsWith<DebuggerSettingsException> { DebuggerSettingsFiles.decode(invalidBank(JsonPrimitive("1"))) }.code)
        assertEquals(DebuggerSettingsError.REGISTRATIONS, assertFailsWith<DebuggerSettingsException> { DebuggerSettingsFiles.decode(invalidBank(JsonPrimitive(8))) }.code)
        runner.reset()
        DebuggerSettingsFiles.restore(runner, null, DebuggerSettingsFiles.decode(text))
        assertEquals(listOf(0, 1), runner.breakpointRegistrations().map { it.bank })
        assertTrue(runner.breakpointRegistrations().all { it.hitCount == 0L })
        val before = runner.breakpointRegistrations()
        assertIs<DebuggerSettingsApplyResult.Failure>(runner.restoreDebugSettings(saved.copy(breakpoints = listOf(saved.breakpoints.first().copy(bank = 8)))))
        assertEquals(before, runner.breakpointRegistrations())
        val legacy = saved.copy(breakpoints = listOf(saved.breakpoints.first().copy(bank = null)))
        val legacyText = DebuggerSettingsFiles.encode(legacy)
        assertEquals(1, Json.parseToJsonElement(legacyText).jsonObject.getValue("schemaVersion").jsonPrimitive.int)
        assertEquals(legacy, DebuggerSettingsFiles.decode(legacyText))
    }

    @Test fun registrationApiIncludesBankWithoutChangingLegacyShapeForUnscopedEntries() {
        val runner = bankBreakpointRunner()
        save(runner.saveBreakpoint(null, 0x4000, bank = 1))
        val service = DesktopDebugContextService(runner, "bank-test")
        val revision = runner.sessionRevision
        val json = assertIs<DebugRegistrationsResult.Success>(service.registrations()).snapshot.toJson()
        assertEquals(1, json.getValue("breakpoints").jsonArray.single().jsonObject.getValue("bank").jsonPrimitive.int)
        assertEquals("logicalWithOptionalRomBank", json.getValue("addressScope").jsonPrimitive.content)
        assertEquals(revision, runner.sessionRevision)
    }
}
