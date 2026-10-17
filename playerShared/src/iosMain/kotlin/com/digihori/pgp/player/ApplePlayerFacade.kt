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
    private val gamepad = com.digihori.pgp.input.GamepadInput(
        { button, key -> keys.press("gamepad:${button.name}", key) },
        { button, _ -> keys.release("gamepad:${button.name}") },
    )
    private val keyboard = com.digihori.pgp.input.PhysicalKeyInput(
        { owner, key -> keys.press(owner, key) }, { owner -> keys.release(owner) }, ::applePhysicalKey,
    )

    public fun configureExternalInput(receiving: Boolean, enabled: Boolean, selectedId: String?,
                                      buttons: List<String>, keyNames: List<String>) {
        val settings = appleGamepadSettings(enabled, buttons, keyNames)
        val allowed = com.digihori.pgp.input.GamepadKeys.forMachine(session.machineId)
        gamepad.configure(settings, selectedId, allowed, receiving)
        val definition = com.digihori.pgp.core.api.MachineCatalog.require(session.machineId)
        keyboard.configure(receiving, allowed, definition.automaticKeyHoldCycles, definition.automaticKeyGapCycles)
    }
    public fun updateGamepad(id: String?, buttons: List<String>) {
        if (session.runner.state != PlayerRunState.RUNNING) gamepad.reset()
        gamepad.update(id, buttons.mapNotNull { name ->
            com.digihori.pgp.input.GamepadButton.entries.find { it.name == name }
        }.toSet())
    }
    public fun physicalKey(code: Int, down: Boolean, repeated: Boolean): Boolean {
        if (down && session.runner.state != PlayerRunState.RUNNING) return false
        return keyboard.handle(1, code, down, if (repeated) 1 else 0)
    }
    public fun disconnectKeyboard() = keyboard.reset(1)


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

    /** PCM belongs to this tick only, including omitted silent frames. */
    public fun tick(nowNanoseconds: Long): AppleAudioPacket? {
        val result = session.runner.tick(nowNanoseconds)
        val cycles = result.runResult?.executedCycles ?: 0
        executedCycles += cycles
        keyboard.advanceCycles(cycles)
        return result.audio?.let { AppleAudioPacket(it, result.speed.playbackRate(it.sampleRate)) }
    }

    public val speedId: String get() = session.runner.speed.id
    public fun setSpeed(id: String, nowNanoseconds: Long) {
        session.runner.setSpeed(PlayerExecutionSpeed.fromId(id), nowNanoseconds)
    }

    public fun press(keyName: String, owner: String): Boolean {
        val key = PocketKey.entries.find { it.name == keyName } ?: return false
        return keys.press(owner, key) is InputResult.Accepted
    }

    public fun release(owner: String) { keys.release(owner) }
    public fun releaseAllKeys() {
        gamepad.reset()
        keyboard.reset()
        keys.releaseAll()
    }

    public fun loadProgram(input: PlayerProgramInput): PlayerProgramError? =
        when (val result = session.loadProgram(input)) {
            PlayerProgramApplyResult.Success -> null
            is PlayerProgramApplyResult.Failure -> result.error
        }

    public fun exportBasic(): AppleProgramExport = AppleProgramExport.from(session.exportBasic())
    public fun exportDump(start: Int, end: Int): AppleProgramExport =
        AppleProgramExport.from(session.exportMemoryDump(start, end))

    public fun setOperatingMode(name: String): Boolean {
        val mode = when (name) {
            "RUN" -> OperatingMode.RUN
            "PRO" -> OperatingMode.PROGRAM
            "RSV" -> OperatingMode.RESERVE
            else -> return false
        }
        releaseAllKeys()
        return session.setOperatingMode(mode)
    }

    public fun setProgramMode(enabled: Boolean) {
        releaseAllKeys()
        session.setOperatingMode(if (enabled) OperatingMode.PROGRAM else OperatingMode.RUN)
    }

    public fun snapshot(): ApplePlayerSnapshot = ApplePlayerSnapshot(
        session.runner.state.name,
        when (session.operatingMode) {
            OperatingMode.PROGRAM -> "PRO"
            OperatingMode.RESERVE -> "RSV"
            else -> "RUN"
        },
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

/** Pure PC-1245 parser boundary: no Session access, safe for a host-owned I/O queue. */
public object AppleProgramCodec {
    public fun parse(bytes: ByteArray, basic: Boolean): AppleProgramParse =
        when (val result = PlayerProgramParser.parse(bytes,
            if (basic) PlayerProgramFormat.BASIC else PlayerProgramFormat.MEMORY_DUMP,
            Pc1245RomDefinition.MACHINE_ID)) {
            is PlayerProgramParseResult.Success -> AppleProgramParse(result.input, null)
            is PlayerProgramParseResult.Failure -> AppleProgramParse(null, result.error)
        }
}

public class AppleProgramParse internal constructor(
    public val input: PlayerProgramInput?, public val error: PlayerProgramError?,
)

public class AppleProgramExport private constructor(bytes: ByteArray?, public val error: PlayerProgramError?) {
    private val content = bytes?.copyOf()
    public fun copyBytes(): ByteArray? = content?.copyOf()
    internal companion object {
        fun from(result: PlayerProgramExportResult): AppleProgramExport = when (result) {
            is PlayerProgramExportResult.Success -> AppleProgramExport(result.copyBytes(), null)
            is PlayerProgramExportResult.Failure -> AppleProgramExport(null, result.error)
        }
    }
}

/** Immutable copy boundary; Swift copies again before sending PCM to its audio worker. */
public class AppleAudioPacket internal constructor(
    private val pcm: com.digihori.pgp.core.api.AudioPcmSnapshot,
    public val playbackRate: Int,
) {
    public val sampleRate: Int get() = pcm.sampleRate
    public val frameCount: Int get() = pcm.frameCount
    public val sampleCount: Int get() = pcm.sampleCount
    public fun copySamples(): ShortArray = pcm.copySamples()
}
