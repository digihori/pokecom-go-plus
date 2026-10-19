package com.digihori.pgp.player.android

import android.content.SharedPreferences
import android.os.Bundle
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.android.rom.AndroidRomLibrary
import com.digihori.pgp.player.android.rom.AndroidRomPackage
import com.digihori.pgp.player.android.rom.PlayerRomDecoder
import java.io.File

/** Debug-only input checks. Optional local ROM must be explicitly supplied to this private namespace. */
class Pc1360kInputProbeActivity : MainActivity() {
    override fun getFilesDir(): File = File(super.getFilesDir(), "pc1360k-input-probe").apply {
        check(mkdirs() || isDirectory)
    }
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        super.getSharedPreferences("pc1360k-input-probe-$name", mode)

    override fun onCreate(savedInstanceState: Bundle?) {
        val incoming = File(filesDir, "incoming.pgrom")
        if (incoming.isFile) {
            val rom = checkNotNull(PlayerRomDecoder.decode(incoming.readBytes(), MachineId("pc-1360k")))
            AndroidRomLibrary(filesDir).install(AndroidRomPackage.write(rom), rom, "local input check")
            check(incoming.delete())
        }
        super.onCreate(savedInstanceState)
    }
}
