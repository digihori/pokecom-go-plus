package com.digihori.pgp.input

import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.emulator.machine.pc14xx.*
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import kotlin.test.*

class Pc14xxGamepadKeysTest {
    @Test fun newMachineChoicesAreExactlyTheKeysAcceptedByCore() {
        for (model in Pc14xxModel.entries) {
            val rom = assertIs<RomImportResult.Success>(Pc14xxComponentRomImporter.importImages(model,
                ByteArray(0x2000), ByteArray(if (model.bankCount == 0) 0x8000 else model.bankCount * 0x4000))).romSet
            val s = assertIs<CreateSessionResult.Success>(EmulatorFactory.create(model.machineId, rom)).session
            val accepted = PocketKey.entries.filter { key -> s.pressKey(key) == InputResult.Accepted }.toSet()
            assertEquals(accepted, GamepadKeys.forMachine(model.machineId))
        }
    }
}
