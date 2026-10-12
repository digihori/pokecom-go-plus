package com.digihori.pgp.player.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var format by remember { mutableStateOf(PlayerProgramFormat.BASIC) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    val valid = format == PlayerProgramFormat.BASIC ||
        (parseProgramAddress(start)?.let { first -> parseProgramAddress(end)?.let { first <= it } } == true)
    AlertDialog(
        onDismissRequest = io::dismiss, title = { Text("プログラムを保存") },
        text = { Column(Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                FilterChip(selected = format == PlayerProgramFormat.BASIC, onClick = { format = PlayerProgramFormat.BASIC }, label = { Text("BASIC (.bas)") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = format == PlayerProgramFormat.MEMORY_DUMP, onClick = { format = PlayerProgramFormat.MEMORY_DUMP }, label = { Text("ダンプ (.dmp)") })
            }
            if (format == PlayerProgramFormat.BASIC) Text("現在のBASICプログラム全体を保存します。") else {
                Text("保存するRAM範囲を16進数で指定してください。終了アドレスを含みます。")
                OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("開始アドレス（例 C500）") }, singleLine = true)
                OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("終了アドレス（例 C5FF）") }, singleLine = true)
            }
            Text("保存・共有後もPAUSEDです。")
        } },
        confirmButton = { TextButton(enabled = valid, onClick = { io.export(format, start, end, false) }) { Text("ファイルへ保存") } },
        dismissButton = { Row {
            TextButton(onClick = io::dismiss) { Text("キャンセル") }
            TextButton(enabled = valid, onClick = { io.export(format, start, end, true) }) { Text("共有") }
        } },
    )
}

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
