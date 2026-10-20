package com.digihori.pgp.player.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.digihori.pgp.player.*
import com.digihori.pgp.player.android.rom.*
import com.digihori.pgp.core.api.MachineCatalog
import com.digihori.pgp.core.rom.MachineId
import java.util.concurrent.Executor

internal sealed interface RomManagerState {
    data class ListRoms(val entries: List<PlayerRomEntry>) : RomManagerState
    data class Busy(val text: String) : RomManagerState
    data class Confirm(val text: String, val action: () -> Unit, val onCancel: () -> Unit) : RomManagerState
    data class Message(val text: String) : RomManagerState
}

internal class AndroidRomManager(
    private val activity: ComponentActivity,
    private val executor: Executor,
    private val session: () -> PlayerSession?,
    private val pause: () -> Unit,
    private val publishSession: (PlayerSession) -> Unit,
    private val publishState: (PlayerScreenState) -> Unit,
    private val onModalChange: () -> Unit,
) {
    var state: RomManagerState? by mutableStateOf(null)
        private set
    var entries: List<PlayerRomEntry>? by mutableStateOf(null)
        private set
    private val library = AndroidRomLibrary(activity.filesDir)
    private var generation = 0
    private val restoredPicker = activity.savedStateRegistry.consumeRestoredStateForKey("rom-picker-machine")
    private var expectedMachine = restoredPicker?.getString("machine")?.let(::MachineId) ?: MachineId("pc-1245")
    private var pickerFromHome = restoredPicker?.getBoolean("fromHome") ?: false

    init {
        activity.savedStateRegistry.registerSavedStateProvider("rom-picker-machine") {
            Bundle().apply {
                putString("machine", expectedMachine.value)
                putBoolean("fromHome", pickerFromHome)
            }
        }
    }
    private val picker = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            if (pickerFromHome) dismiss() else open()
            return@registerForActivityResult
        }
        val cancel = if (pickerFromHome) ::dismiss else ::open
        val expected = expectedMachine
        work("ROMを検証しています…", { AndroidRomImporter(activity.contentResolver).prepare(uri, expected) }) { outcome ->
            when (outcome) {
                is RomImportOutcome.Failure -> show(RomManagerState.Message("ROMを取り込めませんでした（${outcome.kind}）。現在のROMは保持しています。"))
                is RomImportOutcome.Success -> {
                    val prepared = outcome.prepared
                    val id = prepared.romSet.machineId
                    if (PlayerMachineCatalog.definitions.none { it.id == id } || expected != id) {
                        show(RomManagerState.Message("選択したROMの機種は現在のPlayer対象と一致しません。"))
                        return@work
                    }
                    work("登録状況を確認しています…", { library.load(id) != null }) { replacing ->
                        val commit = { install(prepared) }
                        if (replacing || session() != null) {
                            show(RomManagerState.Confirm(
                                "${PlayerMachineCatalog.romLabel(id)}の共通ROMを${if (replacing) "置換" else "登録"}します。現在のRAMとロード済みプログラムは失われます。", commit, cancel,
                            ))
                        } else commit()
                    }
                }
            }
        }
    }

    private fun show(value: RomManagerState?) { state = value; onModalChange() }
    fun dismiss() { show(null) }
    fun open() {
        pause()
        work("ROM一覧を読み込んでいます…", { library.entries(PlayerMachineCatalog.homeDefinitions.map { it.id }) }) {
            show(RomManagerState.ListRoms(it))
        }
    }
    fun importRom(machineId: MachineId, fromHome: Boolean = false) {
        require(PlayerMachineCatalog.definitions.any { it.id == machineId })
        pause()
        generation++
        expectedMachine = machineId
        pickerFromHome = fromHome
        show(RomManagerState.Busy("ROMファイルを選択してください"))
        runCatching { picker.launch(arrayOf("*/*")) }.onFailure { show(RomManagerState.Message("ファイル選択を開けませんでした。")) }
    }

    private fun install(prepared: PreparedAndroidRom) {
        work("ROMを保存しています…", { library.install(prepared.bytes, prepared.romSet, prepared.sourceName) }) {
            publishSession(prepared.session)
            dismiss()
        }
    }

    fun use(machineId: MachineId, fromHome: Boolean = false, onReady: () -> Unit = {}) {
        require(PlayerMachineCatalog.definitions.any { it.id == machineId })
        if (session()?.machineId == machineId) { onReady(); return }
        pause()
        val start = {
            work("ROMを読み込んでいます…", {
                val created = createStoredSession(machineId)
                library.select(machineId)
                created
            }) { publishSession(it); dismiss(); onReady() }
        }
        if (session() == null) { start(); return }
        show(RomManagerState.Confirm(
            "${MachineCatalog.require(machineId).displayName}へ切り替えます。現在の${MachineCatalog.require(session()!!.machineId).displayName}のRAMとロード済みプログラムは失われます。",
            start,
            if (fromHome) ::dismiss else ::open,
        ))
    }

    fun delete(machineId: MachineId) {
        val active = session()?.machineId?.let(PlayerMachineCatalog::romId) == PlayerMachineCatalog.romId(machineId)
        show(RomManagerState.Confirm("${PlayerMachineCatalog.romLabel(machineId)}の保存済みROMを削除します。${if (active) "現在のSessionとRAMも破棄します。" else ""}", action = {
            work("ROMを削除しています…", { library.delete(machineId) }) {
                if (active) publishState(PlayerScreenState.RomMissing)
                open()
            }
        }, onCancel = ::open))
    }

    fun restore() {
        work("ROMを復元しています…", {
            library.activeMachineId()?.let { id ->
                require(PlayerMachineCatalog.definitions.any { it.id == id }) { "この機種は現在のPlayerでは選択できません" }
                createStoredSession(id)
            }
        }, initial = true) { restored ->
            if (restored == null) publishState(PlayerScreenState.RomMissing) else publishSession(restored)
            dismiss()
        }
    }

    private fun createStoredSession(id: MachineId): PlayerSession {
        val bytes = library.load(id) ?: error("保存済みROMが見つかりません")
        val parsed = PlayerRomDecoder.decode(bytes, id) ?: error("保存済みROMの検証に失敗しました")
        return (PlayerSessionFactory.create(parsed) as? PlayerSessionCreationResult.Success)?.session
            ?: error("Sessionを生成できませんでした")
    }

    private fun <T> work(text: String, operation: () -> T, initial: Boolean = false, complete: (T) -> Unit) {
        val ticket = ++generation
        if (!initial) show(RomManagerState.Busy(text))
        executor.execute {
            val result = runCatching(operation)
            val listing = runCatching { library.entries(PlayerMachineCatalog.homeDefinitions.map { it.id }) }
            activity.runOnUiThread {
                if (activity.isDestroyed || ticket != generation) return@runOnUiThread
                listing.getOrNull()?.let { entries = it }
                result.fold(onSuccess = complete, onFailure = {
                    if (initial) publishState(PlayerScreenState.Failure(PlayerFailureKind.ROM_VALIDATION))
                    show(RomManagerState.Message(it.message ?: "ROM操作に失敗しました。"))
                })
            }
        }
    }
}
