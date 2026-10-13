package com.digihori.pgp.core.api

import com.digihori.pgp.core.rom.*
import kotlin.test.*

class BreakInputTest {
    private fun session(id: MachineId): EmulatorSession {
        // Synthetic program: repeated TEST 8, with no copyrighted firmware.
        val internal = ByteArray(0x2000) { if (it % 2 == 0) 0x6b else 0x08 }
        val components = mutableListOf(RomComponent(RomComponentId("internal"), RomRole.INTERNAL, internal))
        if (id.value == "pc-1360") repeat(8) {
            components += RomComponent(RomComponentId("bank-$it"), RomRole.EXTERNAL, ByteArray(0x4000))
        } else components += RomComponent(RomComponentId("external"), RomRole.EXTERNAL,
            ByteArray(if (id.value in listOf("pc-1245", "pc-1250", "pc-1251", "pc-1255")) 0x4000 else 0x8000))
        return assertIs<CreateSessionResult.Success>(EmulatorFactory.create(id, RomSet(id, components))).session
    }

    @Test
    fun heldBreakSurvivesRepeatedMachineCodeScansAndReleaseClearsIt() {
        for (id in EmulatorFactory.supportedMachineIds()) {
            val session = session(id)
            session.step()
            assertTrue(session.cpuSnapshot().zero, id.value)
            session.pressKey(PocketKey.BREAK)
            repeat(100) {
                session.step()
                assertFalse(session.cpuSnapshot().zero, id.value)
            }
            session.releaseKey(PocketKey.BREAK)
            session.step()
            assertTrue(session.cpuSnapshot().zero, id.value)
            session.reset()
            session.step()
            assertTrue(session.cpuSnapshot().zero, id.value)
        }
    }

    @Test
    fun tapBetweenRunnerTicksIsLatchedAndCanBePressedAgain() {
        for (id in EmulatorFactory.supportedMachineIds()) {
            val session = session(id)
            repeat(2) {
                session.pressKey(PocketKey.BREAK)
                session.releaseKey(PocketKey.BREAK)
                repeat(10) {
                    session.step()
                    assertFalse(session.cpuSnapshot().zero, id.value)
                }
                session.step()
                assertTrue(session.cpuSnapshot().zero, id.value)
            }
        }
    }
}
