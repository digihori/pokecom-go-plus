package com.digihori.pgp.core.emulator.machine.pc14xx

import com.digihori.pgp.core.api.PocketKey

/** Host choices share the machine's actual scan definition, including its independent BRK signal. */
public object Pc14xxKeyCatalog {
    public fun supportedKeys(model: Pc14xxModel): Set<PocketKey> = Pc14xxKeyboard(model).supportedKeys()
}
