package com.digihori.pgp.player.android

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Input survives dialog dismissal, but not a newly launched emulator session. */
internal class MemorySaveAddressState {
    var start by mutableStateOf("")
    var end by mutableStateOf("")

    fun clear() {
        start = ""
        end = ""
    }
}
