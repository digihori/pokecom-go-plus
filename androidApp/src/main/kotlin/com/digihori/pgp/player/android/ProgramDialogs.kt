package com.digihori.pgp.player.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.digihori.pgp.player.*
import com.digihori.pgp.core.source.basic.BasicProgramCompileError
import com.digihori.pgp.core.api.*
import com.digihori.pgp.core.source.machine.PgpMemoryDumpError
import com.digihori.pgp.core.source.basic.BasicProgramDecodeResult

@Composable
internal fun ProgramDialogs(io: AndroidProgramIo) {
    val state = io.dialog ?: return
    when (state) {
        ProgramDialogState.Save -> SaveProgramDialog(io)
        ProgramDialogState.SaveMemory -> SaveMemoryDialog(io)
        is ProgramDialogState.Busy -> AlertDialog(
            onDismissRequest = {}, title = { Text(state.message) },
            text = { CircularProgressIndicator() }, confirmButton = {},
        )
        is ProgramDialogState.Message -> AlertDialog(
            onDismissRequest = io::dismiss, title = { Text(state.title) },
            text = { Text(state.text, Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = io::dismiss) { Text("閉じる") } },
        )
    }
}

@Composable
private fun SaveProgramDialog(io: AndroidProgramIo) {
    AlertDialog(
        onDismissRequest = io::dismiss, title = { Text("BASICをセーブ") },
        text = { Text("現在のBASICプログラムをファイルに保存します。") },
        confirmButton = { TextButton(onClick = io::exportBasic) { Text("ファイルへセーブ") } },
        dismissButton = { TextButton(onClick = io::dismiss) { Text("キャンセル") } },
    )
}

@Composable
private fun SaveMemoryDialog(io: AndroidProgramIo) = MemorySaveDialog(io::dismiss, io::exportMemory, io.memorySaveAddresses)

@Composable
internal fun MemorySaveDialog(
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    addressState: MemorySaveAddressState? = null,
) {
    val addresses = addressState ?: remember { MemorySaveAddressState() }
    var editingStart by remember { mutableStateOf(true) }
    val start = parseSaveAddress(addresses.start)
    val end = parseSaveAddress(addresses.end)
    val valid = start != null && end != null && start <= end
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp).widthIn(max = 720.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            BoxWithConstraints {
            val inputWidth = maxOf(maxWidth - 24.dp, 540.dp)
            val showTitle = maxHeight >= 240.dp
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (showTitle) Text("マシン語を保存", style = MaterialTheme.typography.titleLarge)
                Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())) {
                Row(Modifier.width(inputWidth), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AddressPadTarget("開始", addresses.start, editingStart, { editingStart = true }, Modifier.fillMaxWidth())
                        AddressPadTarget("終了", addresses.end, !editingStart, { editingStart = false }, Modifier.fillMaxWidth())
                        Text(if (valid) "${end!! - start!! + 1} bytes（終了を含む）" else "0000〜FFFF・開始≦終了",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (keys in listOf("CDEF", "89AB", "4567", "0123")) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (key in keys) {
                                    OutlinedButton(
                                        onClick = {
                                            val current = if (editingStart) addresses.start else addresses.end
                                            if (current.length < 4) {
                                                val next = current + key
                                                if (editingStart) addresses.start = next else addresses.end = next
                                                if (next.length == 4) editingStart = !editingStart
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        contentPadding = PaddingValues(0.dp),
                                    ) { Text(key.toString(), fontFamily = FontFamily.Monospace) }
                                }
                            }
                        }
                    }
                    Column(Modifier.width(64.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        OutlinedButton(onClick = {
                            if (editingStart) addresses.start = addresses.start.dropLast(1) else addresses.end = addresses.end.dropLast(1)
                        }, modifier = Modifier.fillMaxWidth().height(48.dp).semantics { contentDescription = "1文字削除" },
                            contentPadding = PaddingValues(0.dp)) { Text("⌫") }
                        OutlinedButton(onClick = { if (editingStart) addresses.start = "" else addresses.end = "" },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentPadding = PaddingValues(0.dp)) { Text("クリア", style = MaterialTheme.typography.labelMedium) }
                    }
                }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    TextButton(enabled = valid, onClick = { onSave(start!!, end!!) }) { Text("保存") }
                }
            }
            }
        }
    }
}

@Composable
private fun AddressPadTarget(label: String, value: String, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics { selected = active },
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value.padEnd(4, '_'), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleLarge)
        }
    }
}

internal fun parseSaveAddress(text: String): Int? =
    text.trim().takeIf { it.matches(Regex("[0-9a-fA-F]{1,4}")) }?.toIntOrNull(16)

private fun hexAddress(value: Int): String = value.toString(16).uppercase().padStart(4, '0')

internal fun PlayerProgramError.userMessage(): String = when (this) {
    PlayerProgramError.NotPaused -> "実行を停止してから操作してください。"
    PlayerProgramError.WrongMachine -> "選択時と現在の機種が異なります。再選択してください。"
    PlayerProgramError.InvalidUtf8 -> "UTF-8テキストとして読み込めません。"
    PlayerProgramError.EmptyDump -> "ダンプにデータがありません。"
    PlayerProgramError.InvalidRange -> "保存するアドレス範囲が不正です。"
    is PlayerProgramError.NonRam -> "${hexAddress(address)}はプログラム用RAMではありません。"
    is PlayerProgramError.BasicOverlap -> "${hexAddress(address)}が現在のBASIC領域または管理ポインタと重なっています。"
    is PlayerProgramError.DumpParse -> when (val issue = error) {
        PgpMemoryDumpError.Empty -> "ダンプにデータがありません。"
        is PgpMemoryDumpError.Syntax -> "${issue.line}行 ${issue.column}列: ${issue.message}"
        is PgpMemoryDumpError.AddressOverflow -> "${issue.line}行: データがFFFFを超えています。"
        is PgpMemoryDumpError.Overlap -> "${issue.line}行と${issue.previousLine}行の${hexAddress(issue.address)}が重複しています。"
    }
    is PlayerProgramError.BasicCompile -> when (val issue = error) {
        BasicProgramCompileError.InvalidUtf8 -> "UTF-8テキストとして読み込めません。"
        is BasicProgramCompileError.Parse -> "${issue.error.line}行 ${issue.error.column}列: ${issue.error.message}"
        is BasicProgramCompileError.Tokenize -> "${issue.error.line}行 ${issue.error.column}列: ${issue.error.message}"
        is BasicProgramCompileError.S1Tokenize -> "${issue.error.line}行 ${issue.error.column}列: ${issue.error.message}"
        is BasicProgramCompileError.S2Tokenize -> "${issue.error.line}行 ${issue.error.column}列: ${issue.error.message}"
        is BasicProgramCompileError.UnsupportedMachine -> "この機種のBASIC変換は未対応です。"
    }
    is PlayerProgramError.BasicMemory -> when (val issue = error) {
        is BasicProgramMemoryError.ProgramTooLarge -> "BASICが容量を超えています（${issue.size} bytes／容量${issue.capacity} bytes）。"
        is BasicProgramMemoryError.InvalidPointer -> "BASIC領域が初期化されていません。ROMを実行して初期化してから再試行してください。"
        is BasicProgramMemoryError.InvalidProgram -> "BASICデータを読み込めません（位置${issue.offset}）。\n${issue.reason}"
        is BasicProgramMemoryError.UnsupportedMachine -> "この機種のBASIC保存は未対応です。"
    }
    is PlayerProgramError.BasicDecode -> when (val issue = result) {
        is BasicProgramDecodeResult.Failure -> "BASICの位置${issue.error.offset}: ${issue.error.message}"
        is BasicProgramDecodeResult.GenericFailure -> "BASICの位置${issue.offset}: ${issue.message}"
        is BasicProgramDecodeResult.UnsupportedMachine -> "この機種のBASIC変換は未対応です。"
        is BasicProgramDecodeResult.Success -> "BASICをテキストへ変換できませんでした。"
    }
    is PlayerProgramError.Placement -> when (val issue = error) {
        is MemoryImageLoadError.ReadOnlyAddress -> "${hexAddress(issue.address)}は書込みできません（${issue.sourceLine}行）。"
        is MemoryImageLoadError.MirrorOverlap -> "${hexAddress(issue.address)}と${hexAddress(issue.previousAddress)}が同じRAMを指しています。"
        is MemoryImageLoadError.BasicOverlap -> "${hexAddress(issue.address)}がBASIC領域と重なっています。"
    }
}
