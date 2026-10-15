package com.digihori.pgp.player.android

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.digihori.pgp.player.*
import java.util.concurrent.Executor
import com.digihori.pgp.core.api.MachineMemoryLayout

internal sealed interface ProgramDialogState {
    data object Save : ProgramDialogState
    data class Busy(val message: String) : ProgramDialogState
    data class Message(val title: String, val text: String) : ProgramDialogState
}

/** Android owns document URIs and file access. Session access stays on the main thread. */
internal class AndroidProgramIo(
    private val activity: ComponentActivity,
    private val executor: Executor,
    private val session: () -> PlayerSession?,
    private val pause: () -> Unit,
    private val refresh: () -> Unit,
    private val resumeAfterLoad: () -> Unit,
    private val onModalChange: () -> Unit,
) {
    var dialog: ProgramDialogState? by mutableStateOf(null)
        private set
    private var sourceSession: PlayerSession? = null
    private var output: ProgramOutput? = null
    private val saveResume = SaveResumeState<PlayerSession>()
    val resumesAfterSave: Boolean get() = saveResume.shouldResume
    val dumpAddressExample: Int get() = session()?.machineId?.let {
        MachineMemoryLayout(it).assemblyStartAddress
    } ?: 0xc500
    private val picker = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { dismiss(); return@registerForActivityResult }
        read(uri)
    }
    private val saver = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val snapshot = output
        output = null
        if (uri == null || snapshot == null) { dismiss(); return@registerForActivityResult }
        showDialog(ProgramDialogState.Busy("保存しています…"))
        executor.execute {
            val result = runCatching {
                activity.contentResolver.openOutputStream(uri, "wt")?.use { it.write(snapshot.bytes) }
                    ?: error("保存先を開けませんでした")
            }
            activity.runOnUiThread {
                if (!activity.isDestroyed) message(if (result.isSuccess) "保存完了" else "保存できませんでした",
                    result.exceptionOrNull()?.message ?: "${snapshot.name}を保存しました。")
                if (!activity.isDestroyed) restoreSaveRunState()
            }
        }
    }

    private data class ProgramOutput(val name: String, val bytes: ByteArray)

    fun openLoad() {
        pause()
        sourceSession = session()
        if (sourceSession == null) return
        showDialog(ProgramDialogState.Busy("ファイルを選択してください"))
        runCatching { picker.launch(arrayOf("*/*")) }.onFailure { message("選択できませんでした", it.message.orEmpty()) }
    }
    fun openSave() {
        val target = session() ?: return
        saveResume.begin(target, target.runner.state == PlayerRunState.RUNNING)
        open(ProgramDialogState.Save)
    }
    private fun open(state: ProgramDialogState) {
        pause()
        sourceSession = session()
        if (sourceSession != null) showDialog(state)
    }
    private fun showDialog(value: ProgramDialogState?) { dialog = value; onModalChange() }
    fun dismiss() {
        output = null
        sourceSession = null
        showDialog(null)
        restoreSaveRunState()
    }
    private fun restoreSaveRunState() {
        if (saveResume.takeFor(session())) resumeAfterLoad()
    }
    private fun message(title: String, text: String) = showDialog(ProgramDialogState.Message(title, text))

    private fun read(uri: Uri) {
        val target = sourceSession ?: run { dismiss(); return }
        showDialog(ProgramDialogState.Busy("読み込んでいます…"))
        executor.execute {
            val result = runCatching {
                val name = activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else null
                } ?: error("ファイル名を取得できませんでした")
                val format = programFormatForName(name) ?: error("bas／dmpファイルを選択してください")
                val bytes = activity.contentResolver.openInputStream(uri)?.use { input ->
                    readProgramBytes(input)
                } ?: error("ファイルを開けませんでした")
                name to PlayerProgramParser.parse(bytes, format, target.machineId)
            }
            activity.runOnUiThread {
                if (activity.isDestroyed) return@runOnUiThread
                if (session() !== target) { message("ロードできませんでした", "機種またはSessionが変更されました。再選択してください。"); return@runOnUiThread }
                result.fold(onSuccess = { (name, parsed) ->
                    when (parsed) {
                        is PlayerProgramParseResult.Failure -> message("ロードできませんでした", parsed.error.userMessage())
                        is PlayerProgramParseResult.Success -> {
                            pause()
                            when (val applied = target.loadProgram(parsed.input)) {
                                PlayerProgramApplyResult.Success -> {
                                    refresh()
                                    resumeAfterLoad()
                                    message("ロード完了", "$name をロードしました。CPUの自動実行を開始します。BASICのRUNコマンドは自動入力しません。")
                                }
                                is PlayerProgramApplyResult.Failure -> message("ロードできませんでした", applied.error.userMessage())
                            }
                        }
                    }
                }, onFailure = { message("ロードできませんでした", it.message.orEmpty()) })
            }
        }
    }

    fun export(format: PlayerProgramFormat, start: String, end: String) {
        val target = sourceSession ?: return
        if (session() !== target) { message("保存できませんでした", "Sessionが変更されました。"); return }
        val result = if (format == PlayerProgramFormat.BASIC) target.exportBasic() else {
            val first = parseProgramAddress(start)
            val last = parseProgramAddress(end)
            if (first == null || last == null || first > last) { message("保存できませんでした", "開始・終了アドレスを0000〜FFFFの16進数で指定してください。"); return }
            target.exportMemoryDump(first, last)
        }
        when (result) {
            is PlayerProgramExportResult.Failure -> message("保存できませんでした", result.error.userMessage())
            is PlayerProgramExportResult.Success -> {
                val name = "${target.machineId.value}-program.${if (format == PlayerProgramFormat.BASIC) "bas" else "dmp"}"
                val snapshot = ProgramOutput(name, result.copyBytes())
                output = snapshot
                showDialog(ProgramDialogState.Busy("保存先を選択してください"))
                runCatching { saver.launch(name) }.onFailure { output = null; message("保存できませんでした", it.message.orEmpty()) }
            }
        }
    }


}

internal fun parseProgramAddress(text: String): Int? = text.trim().removePrefix("0x").removePrefix("0X")
    .takeIf { it.length in 1..4 && it.all { c -> c in '0'..'9' || c.lowercaseChar() in 'a'..'f' } }?.toIntOrNull(16)

internal fun readProgramBytes(input: java.io.InputStream, maximumBytes: Int = 2 * 1024 * 1024): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val count = input.read(chunk)
        if (count < 0) break
        require(count <= maximumBytes - buffer.size()) { "ファイルが大きすぎます（上限2 MiB）" }
        buffer.write(chunk, 0, count)
    }
    return buffer.toByteArray()
}

internal fun programFormatForName(name: String): PlayerProgramFormat? = when (name.substringAfterLast('.', "").lowercase()) {
    "bas" -> PlayerProgramFormat.BASIC
    "dmp" -> PlayerProgramFormat.MEMORY_DUMP
    else -> null
}
