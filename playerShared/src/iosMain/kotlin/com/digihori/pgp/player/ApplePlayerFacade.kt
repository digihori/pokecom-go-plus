package com.digihori.pgp.player

import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.input.HostKeyState
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import com.digihori.pgp.player.skin.SkinDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomPackageManifest
import com.digihori.pgp.core.rom.RomPackageEntryInfo
import com.digihori.pgp.core.rom.RomPackageValidator
import com.digihori.pgp.core.rom.RomPackageValidationResult

/** Swift-facing boundary. The iOS host serializes every call on its main thread. */
public class ApplePlayerFacade internal constructor(private val session: PlayerSession) {
    public val skin: SkinDefinition = requireNotNull(PlayerSkinCatalog.forMachine(session.machineId))
    private val keys = HostKeyState(session::pressKey, session::releaseKey)
    private var executedCycles = 0L

    public fun run(nowNanoseconds: Long) = session.runner.run(nowNanoseconds)

    public fun pause() {
        session.runner.pause()
        releaseAllKeys()
    }

    public fun reset() {
        releaseAllKeys()
        session.reset()
        executedCycles = 0
    }

    public fun tick(nowNanoseconds: Long) {
        executedCycles += session.runner.tick(nowNanoseconds).runResult?.executedCycles ?: 0
    }

    public fun press(keyName: String, owner: String): Boolean {
        val key = PocketKey.entries.find { it.name == keyName } ?: return false
        return keys.press(owner, key) is InputResult.Accepted
    }

    public fun release(owner: String) { keys.release(owner) }
    public fun releaseAllKeys() = keys.releaseAll()

    public fun setProgramMode(enabled: Boolean) {
        releaseAllKeys()
        session.setOperatingMode(if (enabled) OperatingMode.PROGRAM else OperatingMode.RUN)
    }

    public fun snapshot(): ApplePlayerSnapshot = ApplePlayerSnapshot(
        session.runner.state.name,
        if (session.operatingMode == OperatingMode.PROGRAM) "PRO" else "RUN",
        executedCycles,
        keys.isHeld(PocketKey.BREAK),
        session.displayFrame(),
        skin.keyRegions.map { it.key }.filter(keys::isHeld).map { it.name },
    )
}

/** Owns a copied display frame; it never exposes mutable emulator arrays. */
public class ApplePlayerSnapshot internal constructor(
    public val runState: String,
    public val operatingMode: String,
    public val executedCycles: Long,
    public val breakHeld: Boolean,
    public val display: PlayerDisplayFrame,
    public val heldKeys: List<String>,
)

public object ApplePlayerFactory {
    public fun prepareLegacy(bytes: ByteArray): AppleRomPreparation =
        when (val imported = Pc1245FlatRomImporter.importImage(bytes)) {
            is RomImportResult.Success -> prepare(imported.romSet)
            is RomImportResult.Failure -> AppleRomPreparation(null, null, "PC-1245の32/64KiB ROMを選択してください。")
        }

    /** Host supplies decompressed bytes and hashes; archive/filesystem APIs never enter Core. */
    public fun preparePackage(manifest: RomPackageManifest, entries: List<AppleRomArchiveEntry>): AppleRomPreparation {
        if (manifest.machineId != Pc1245RomDefinition.MACHINE_ID.value || entries.map { it.path }.distinct().size != entries.size) {
            return AppleRomPreparation(null, null, "現在のiOS PlayerはPC-1245のみ対応しています。")
        }
        val content = entries.associate { entry ->
            val bytes = entry.copyBytes()
            entry.path to (RomPackageEntryInfo(entry.path, bytes.size, entry.sha256) to bytes)
        }
        return when (val validated = RomPackageValidator.validate(manifest, content)) {
            is RomPackageValidationResult.Success -> prepare(validated.romSet)
            is RomPackageValidationResult.Failure -> AppleRomPreparation(null, null, "ROM packageのmanifest・サイズ・SHA-256を検証できませんでした。")
        }
    }

    private fun prepare(rom: RomSet): AppleRomPreparation = when (val created = PlayerSessionFactory.create(rom)) {
        is PlayerSessionCreationResult.Success -> AppleRomPreparation(ApplePlayerFacade(created.session), rom, null)
        is PlayerSessionCreationResult.Failure -> AppleRomPreparation(null, null, "PC-1245 ROMの構成・サイズが一致しません。")
    }

    /** Small original NOP/jump program for Simulator structure checks, not pocket-computer firmware. */
    public fun structuralProbe(): ApplePlayerFacade {
        val internal = ByteArray(Pc1245RomDefinition.INTERNAL_SIZE) { 0x4d.toByte() }
        // Original checkerboard display exercise using LIDP/LIA/STD, then a NOPW/JP loop.
        // All bytes are generated from instruction definitions, not pocket-computer firmware.
        val program = mutableListOf<Int>()
        fun write(address: Int, value: Int) {
            program += listOf(0x10, address shr 8, address and 0xff, 0x02, value, 0x52)
        }
        repeat(80) { column ->
            val address = if (column < 60) 0xf800 + column else 0xf868 + (79 - column)
            write(address, if (column % 2 == 0) 0x55 else 0x2a)
        }
        write(0xf83c, 0xff)
        write(0xf83d, 0xff)
        val loop = program.size
        program += listOf(0x4d, 0x79, loop shr 8, loop and 0xff)
        program.forEachIndexed { index, value -> internal[index] = value.toByte() }
        val rom = RomSet(Pc1245RomDefinition.MACHINE_ID, listOf(
            RomComponent(Pc1245RomDefinition.INTERNAL_ID, RomRole.INTERNAL, internal),
            RomComponent(Pc1245RomDefinition.EXTERNAL_ID, RomRole.EXTERNAL,
                ByteArray(Pc1245RomDefinition.EXTERNAL_SIZE) { 0x4d.toByte() }),
        ))
        val created = PlayerSessionFactory.create(rom) as PlayerSessionCreationResult.Success
        return ApplePlayerFacade(created.session)
    }
}

public class AppleRomArchiveEntry(public val path: String, public val sha256: String, bytes: ByteArray) {
    private val content = bytes.copyOf()
    internal fun copyBytes(): ByteArray = content.copyOf()
}

public class AppleRomPreparation internal constructor(
    public val player: ApplePlayerFacade?, private val rom: RomSet?, public val errorMessage: String?,
) {
    public fun internalBytes(): ByteArray? = rom?.component(Pc1245RomDefinition.INTERNAL_ID)?.copyBytes()
    public fun externalBytes(): ByteArray? = rom?.component(Pc1245RomDefinition.EXTERNAL_ID)?.copyBytes()
}
