package com.digihori.pgp.player.android

import android.content.SharedPreferences
import android.os.Bundle
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.emulator.machine.pc1261.Pc1261ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1251.Pc1251ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1350.Pc1350ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1401.Pc1401ComponentRomImporter
import com.digihori.pgp.core.emulator.machine.pc1360.Pc1360ComponentRomImporter
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.player.android.rom.AndroidRomLibrary
import com.digihori.pgp.player.android.rom.AndroidRomPackage
import java.io.File

/** Debug-only: real MainActivity flow, but no normal ROM Library or settings are read/written. */
class Pc1360RomProbeActivity : MainActivity() {
    override fun getFilesDir(): File = File(super.getFilesDir(), "pc1360-rom-probe").apply {
        check(mkdirs() || isDirectory)
    }

    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        super.getSharedPreferences("pc1360-rom-probe-$name", mode)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Generated from instruction definitions, not pocket-computer firmware. Fixture packages
        // stay in this debug namespace. Seed once so process restart tests exercise real restore.
        val root = filesDir
        val marker = File(root, "seeded")
        if (!marker.exists()) {
            val library = AndroidRomLibrary(root)
            val fixtures = File(root, "fixtures").apply { check(mkdirs() || isDirectory) }
            for (machine in listOf("pc-1245", "pc-1401", "pc-1350", "pc-1251", "pc-1261", "pc-1360")) {
                for (revision in if (machine == "pc-1360") 0..1 else 0..0) {
                    val internal = ByteArray(0x2000) { 0x4d }
                    byteArrayOf(0x79, 0, 0).copyInto(internal) // JP 0000 forever.
                    val imported = when (machine) {
                        "pc-1360" -> Pc1360ComponentRomImporter.importImages(internal, List(8) { ByteArray(0x4000) { revision.toByte() } })
                        "pc-1245" -> Pc1245ComponentRomImporter.importImages(internal, ByteArray(0x4000))
                        "pc-1401" -> Pc1401ComponentRomImporter.importImages(internal, ByteArray(0x8000))
                        "pc-1261" -> Pc1261ComponentRomImporter.importImages(internal, ByteArray(0x8000) { revision.toByte() })
                        "pc-1251" -> Pc1251ComponentRomImporter.importImages(internal, ByteArray(0x4000) { revision.toByte() })
                        else -> Pc1350ComponentRomImporter.importImages(internal, ByteArray(0x8000) { revision.toByte() })
                    } as RomImportResult.Success
                    val bytes = AndroidRomPackage.write(imported.romSet)
                    val filename = if (revision == 0) "$machine.pgrom" else "$machine-replacement.pgrom"
                    File(fixtures, filename).writeBytes(bytes)
                    if (machine != "pc-1360") library.install(bytes, imported.romSet, filename)
                }
            }
            library.select(MachineId("pc-1245"))
            marker.writeText("synthetic only")
        }
        super.onCreate(savedInstanceState)
    }
}
