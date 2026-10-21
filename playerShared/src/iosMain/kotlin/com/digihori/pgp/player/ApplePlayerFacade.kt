package com.digihori.pgp.player

import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId
import com.digihori.pgp.core.api.InputResult
import com.digihori.pgp.core.api.OperatingMode
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245RomDefinition
import com.digihori.pgp.core.rom.RomComponent
import com.digihori.pgp.core.rom.RomRole
import com.digihori.pgp.core.rom.RomSet
import com.digihori.pgp.input.HostKeyState
import com.digihori.pgp.player.skin.PlayerSkinCatalog
import com.digihori.pgp.player.skin.withFixedModeIndicators
import com.digihori.pgp.player.skin.SkinDefinition
import com.digihori.pgp.core.emulator.machine.pc1245.Pc1245FlatRomImporter
import com.digihori.pgp.core.emulator.machine.pc1245.RomImportResult
import com.digihori.pgp.core.rom.RomPackageManifest
import com.digihori.pgp.core.rom.RomPackageEntryInfo
import com.digihori.pgp.core.rom.RomPackageValidator
import com.digihori.pgp.core.rom.RomPackageValidationResult

/** Explicit iOS integration targets. Enabling a shared machine requires host acceptance first. */
public object AppleMachineCatalog {
    public val machines: List<AppleMachineInfo> = listOf(AppleMachineInfo("pc-1245", "skin_pc1245")) +
        listOf("pc-1250", "pc-1251", "pc-1255").map { AppleMachineInfo(it, "skin_pc1251") } +
        listOf("pc-1260", "pc-1261").map { AppleMachineInfo(it, "skin_pc1261") } +
        listOf(AppleMachineInfo("pc-1350", "skin_pc1350")) +
        listOf("pc-1401", "pc-1402").map { AppleMachineInfo(it, "skin_pc1401") } +
        listOf(AppleMachineInfo("pc-1450", "skin_pc1450")) +
        listOf("1360", "1460", "1470u", "1360k").map { AppleMachineInfo("pc-$it", "skin_pc$it") }
    public val homeMachines: List<AppleMachineInfo> get() = machines.filter { it.id == it.romId }.sortedBy { it.displayName }
    public fun models(id: String): List<AppleMachineInfo> = find(id)?.let { machine -> machines.filter { it.romId == machine.romId } } ?: emptyList()
    public val defaultMachine: AppleMachineInfo get() = machines.first()
    public fun find(id: String): AppleMachineInfo? = machines.find { it.id == id }
}

public class AppleMachineInfo internal constructor(public val id: String, public val skinImageName: String) {
    private val definition = MachineCatalog.require(MachineId(id))
    public val romId: String get() = PlayerMachineCatalog.romId(MachineId(id)).value
    public val displayName: String get() = definition.displayName
    public val romBankCount: Int get() = when (id) { "pc-1360", "pc-1470u", "pc-1360k" -> 8; "pc-1460" -> 4; else -> 0 }
    public val supportsLegacyRom: Boolean get() = romBankCount == 0
    public val legacyRomSizes: String get() = if (romId in listOf("pc-1261", "pc-1350", "pc-1401", "pc-1450")) "64 KiB" else "32/64 KiB"
    public val standardRamStart: Int get() = definition.memoryRegions.first { it.id == "ram" }.startAddress
    public val characterColumns: Int get() = definition.characterColumns
    public val characterRows: Int get() = definition.characterRows
    // PC-1245 intentionally exposes RUN/PRO only, matching the existing Player policy.
    public val hasModeKey: Boolean get() = "MODE" in keys
    public val operatingModes: List<String> get() = if (hasModeKey) emptyList() else definition.supportedOperatingModes
        .filter { id != "pc-1245" || it != OperatingMode.RESERVE }.map(::appleModeName)
    public val keys: List<String> get() = com.digihori.pgp.input.GamepadKeys.forMachine(MachineId(id)).map { it.name }
}

private fun appleModeName(mode: OperatingMode): String = when (mode) {
    OperatingMode.PROGRAM -> "PRO"
    OperatingMode.RESERVE -> "RSV"
    OperatingMode.CALCULATOR -> "CAL"
    else -> mode.name
}

/** Swift-facing boundary. The iOS host serializes every call on its main thread. */
public class ApplePlayerFacade internal constructor(private val session: PlayerSession) {
    public val machine: AppleMachineInfo = requireNotNull(AppleMachineCatalog.find(session.machineId.value))
    public val skin: SkinDefinition = requireNotNull(PlayerSkinCatalog.forMachine(session.machineId)).withFixedModeIndicators()
    private val keys = HostKeyState(session::pressKey, session::releaseKey)
    private var executedCycles = 0L
    private val gamepad = com.digihori.pgp.input.GamepadInput(
        { button, key -> keys.press("gamepad:${button.name}", key) },
        { button, _ -> keys.release("gamepad:${button.name}") },
    )
    private val keyboard = com.digihori.pgp.input.PhysicalKeyInput(
        { owner, key -> keys.press(owner, key) }, { owner -> keys.release(owner) }, { code -> applePhysicalKeyForMachine(code, machine.id) },
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
        if (name !in machine.operatingModes) return false
        val mode = session.supportedOperatingModes.find { appleModeName(it) == name } ?: return false
        releaseAllKeys()
        return session.setOperatingMode(mode)
    }

    public fun setProgramMode(enabled: Boolean) {
        setOperatingMode(if (enabled) "PRO" else "RUN")
    }

    public fun snapshot(): ApplePlayerSnapshot = ApplePlayerSnapshot(
        session.runner.state.name,
        appleModeName(session.operatingMode),
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
    public fun prepareLegacy(bytes: ByteArray): AppleRomPreparation = prepareLegacyForMachine(bytes, AppleMachineCatalog.defaultMachine.id)

    public fun prepareLegacyForMachine(bytes: ByteArray, machineId: String): AppleRomPreparation {
        val machine = AppleMachineCatalog.find(machineId) ?: return failure("未対応の機種です。")
        if (!machine.supportsLegacyRom) return failure("${machine.displayName}はStudioで作成したpgromを選択してください。")
        val imported = when (machine.romId) {
            "pc-1245" -> Pc1245FlatRomImporter.importImage(bytes)
            "pc-1251" -> com.digihori.pgp.core.emulator.machine.pc1251.Pc1251FlatRomImporter.importImage(bytes)
            "pc-1261" -> com.digihori.pgp.core.emulator.machine.pc1261.Pc1261FlatRomImporter.importImage(bytes)
            "pc-1350" -> com.digihori.pgp.core.emulator.machine.pc1350.Pc1350FlatRomImporter.importImage(bytes)
            "pc-1401" -> com.digihori.pgp.core.emulator.machine.pc1401.Pc1401FlatRomImporter.importImage(bytes)
            "pc-1450" -> com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxFlatRomImporter.importImage(
                com.digihori.pgp.core.emulator.machine.pc14xx.Pc14xxModel.PC_1450, bytes)
            else -> return failure("単体ROMに対応していません。")
        }
        return when (imported) {
            is RomImportResult.Success -> prepare(imported.romSet, machineId)
            is RomImportResult.Failure -> failure("${machine.displayName}の${machine.legacyRomSizes} ROMを選択してください。")
        }
    }

    /** Host supplies decompressed bytes and hashes; archive/filesystem APIs never enter Core. */
    public fun preparePackage(manifest: RomPackageManifest, entries: List<AppleRomArchiveEntry>): AppleRomPreparation =
        preparePackageForMachine(manifest, entries, manifest.machineId)

    public fun preparePackageForMachine(manifest: RomPackageManifest, entries: List<AppleRomArchiveEntry>, machineId: String): AppleRomPreparation {
        val machine = AppleMachineCatalog.find(machineId) ?: return failure("未対応の機種です。")
        val source = AppleMachineCatalog.find(manifest.machineId)
        if (source == null || source.romId != machine.romId || entries.map { it.path }.distinct().size != entries.size) {
            return failure("未対応の機種・別シリーズまたは重複したROM entryです。")
        }
        val content = entries.associate { entry ->
            val bytes = entry.copyBytes()
            entry.path to (RomPackageEntryInfo(entry.path, bytes.size, entry.sha256) to bytes)
        }
        return when (val validated = RomPackageValidator.validate(manifest, content)) {
            is RomPackageValidationResult.Success -> prepare(validated.romSet, machineId)
            is RomPackageValidationResult.Failure -> failure("ROM packageのmanifest・サイズ・SHA-256を検証できませんでした。")
        }
    }

    private fun failure(message: String): AppleRomPreparation = AppleRomPreparation(null, null, message)

    private fun prepare(rom: RomSet, machineId: String): AppleRomPreparation {
        val bankCount = requireNotNull(AppleMachineCatalog.find(machineId)).romBankCount
        if (bankCount > 0) {
            val expected = listOf(Triple("internal", RomRole.INTERNAL, 0x2000)) +
                List(bankCount) { Triple("bank-$it", RomRole.EXTERNAL, 0x4000) }
            val kanji = rom.components.filter { it.id.value.startsWith("kanji-selector-") }
            val kanjiValid = if (machineId == "pc-1360k") {
                kanji.size in listOf(0, 2, 4) && kanji.map { it.id.value }.toSet() ==
                    (0 until kanji.size).map { "kanji-selector-$it" }.toSet() &&
                    kanji.all { it.role == RomRole.KANJI && it.size == 0x10000 }
            } else kanji.isEmpty()
            if (!kanjiValid || rom.components.size != expected.size + kanji.size || expected.any { (id, role, size) ->
                val component = rom.component(com.digihori.pgp.core.rom.RomComponentId(id))
                component == null || component.role != role || component.size != size
            }) return failure("内部ROMと全ROMバンクの構成・role・サイズが一致しません。")
        }
        val target = PlayerMachineCatalog.forModel(rom, MachineId(machineId)) ?: return failure("別シリーズのROMです。")
        val canonical = requireNotNull(PlayerMachineCatalog.forModel(rom, PlayerMachineCatalog.romId(target.machineId)))
        return when (val created = PlayerSessionFactory.create(target)) {
            is PlayerSessionCreationResult.Success -> AppleRomPreparation(ApplePlayerFacade(created.session), canonical, null)
            is PlayerSessionCreationResult.Failure -> failure("ROMの構成・サイズが一致しません。")
        }
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
    public val machineId: String? get() = rom?.machineId?.value
    public val components: List<AppleRomComponent> get() = rom?.components?.map { AppleRomComponent(it) } ?: emptyList()
    public fun internalBytes(): ByteArray? = rom?.component(Pc1245RomDefinition.INTERNAL_ID)?.copyBytes()
    public fun externalBytes(): ByteArray? = rom?.component(Pc1245RomDefinition.EXTERNAL_ID)?.copyBytes()
}

public class AppleRomComponent internal constructor(private val component: RomComponent) {
    public val id: String get() = component.id.value
    public val role: String get() = component.role.value
    public fun copyBytes(): ByteArray = component.copyBytes()
}

/** Pure machine-aware parser boundary: no Session access, safe for a host-owned I/O queue. */
public object AppleProgramCodec {
    public fun parse(bytes: ByteArray, basic: Boolean): AppleProgramParse =
        parseForMachine(bytes, basic, AppleMachineCatalog.defaultMachine.id)

    public fun parseForMachine(bytes: ByteArray, basic: Boolean, machineId: String): AppleProgramParse =
        when (val result = PlayerProgramParser.parse(bytes,
            if (basic) PlayerProgramFormat.BASIC else PlayerProgramFormat.MEMORY_DUMP,
            MachineId(machineId))) {
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
