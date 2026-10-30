package com.digihori.pgp.desktop.debug

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.api.MachineMemoryBanking
import com.digihori.pgp.core.rom.MachineId

internal fun validBreakpointBank(machine: MachineId, address: Int, bank: Int?): Boolean {
    if (bank == null) return true
    val banking = MachineCatalog.find(machine)?.memoryBanking as? MachineMemoryBanking.Banked ?: return false
    return address in banking.windowStart..banking.windowEndInclusive && bank in 0 until banking.bankCount
}
