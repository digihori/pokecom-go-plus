package com.digihori.pgp.player.android

import android.content.SharedPreferences
import android.os.Bundle
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.player.android.rom.AndroidRomLibrary
import com.digihori.pgp.player.android.rom.AndroidRomPackage
import java.io.File

/** Synthetic shared-ROM fixtures in a private namespace; never touches the regular Library. */
class PlayerSeriesProbeActivity : MainActivity() {
    override fun getFilesDir(): File = File(super.getFilesDir(), "player-series-probe").apply {
        check(mkdirs() || isDirectory)
    }
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        super.getSharedPreferences("player-series-probe-$name", mode)

    override fun onCreate(savedInstanceState: Bundle?) {
        val marker = File(filesDir, "seeded")
        if (!marker.exists()) {
            val library = AndroidRomLibrary(filesDir)
            for (name in listOf("1251", "1261", "1401")) {
                val internal = ByteArray(0x2000) { 0x4d }
                byteArrayOf(0x79, 0, 0).copyInto(internal)
                val rom = (when (name) {
                    "1251" -> Pc1251ComponentRomImporter.importImages(internal, ByteArray(0x4000))
                    "1261" -> Pc1261ComponentRomImporter.importImages(internal, ByteArray(0x8000))
                    else -> Pc1401ComponentRomImporter.importImages(internal, ByteArray(0x8000))
                } as RomImportResult.Success).romSet
                library.install(AndroidRomPackage.write(rom), rom, "synthetic-$name.pgrom")
            }
            marker.writeText("synthetic only")
        }
        super.onCreate(savedInstanceState)
    }
}
