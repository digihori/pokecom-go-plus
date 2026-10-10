package com.digihori.pgp.controller.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.api.PocketKey
import com.digihori.pgp.input.*

/** Shared Studio / Android settings flow: one setting saved immediately on each change. */
@Composable
fun ControllerSettings(
    settings: GamepadSettings,
    devices: List<GamepadDevice>,
    selectedId: String?,
    pressed: Set<GamepadButton>,
    allowedKeys: Set<PocketKey>,
    onSelectDevice: (String) -> Unit,
    onSettingsChange: (GamepadSettings) -> Unit,
    availableButtons: Set<GamepadButton> = GamepadButton.entries.toSet(),
    status: String? = null,
) {
    var selectedButton by remember { mutableStateOf<GamepadButton?>(null) }
    var deviceMenu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ゲームコントローラー", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = settings.enabled, onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) })
            Spacer(Modifier.width(12.dp))
            Text("コントローラー入力を有効にする")
        }
        Box {
            OutlinedButton(onClick = { deviceMenu = true }, enabled = devices.isNotEmpty()) {
                Text(devices.find { it.id == selectedId }?.let { "${it.name} (${it.id})" }
                    ?: if (devices.isEmpty()) "未接続" else "使用する1台を選択")
            }
            DropdownMenu(expanded = deviceMenu, onDismissRequest = { deviceMenu = false }) {
                devices.forEach { device ->
                    DropdownMenuItem(
                        text = { Text("${device.name} (${device.id})") },
                        onClick = { deviceMenu = false; onSelectDevice(device.id) },
                    )
                }
            }
        }
        status?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("図のボタンを選び、ポケコンのキーを割り当てます。設定中の操作はテスト表示のみです。")
        ControllerDiagram(
            settings, pressed, allowedKeys, availableButtons,
            onClick = { selectedButton = it },
        )
        Text("ABXYは標準位置を表示しています。機器の印字が異なる場合は上下左右の位置で選んでください。",
            style = MaterialTheme.typography.bodySmall)
        Text("スティックは対象外です。変更は自動保存され、以前の設定を上書きします。",
            style = MaterialTheme.typography.bodySmall)
    }
    selectedButton?.let { button ->
        AlertDialog(
            onDismissRequest = { selectedButton = null },
            title = { Text("${button.label} の割当") },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    val choices = allowedKeys.sortedWith(compareBy<PocketKey> {
                        when {
                            it.name.startsWith("NUM_") -> 0
                            it.ordinal >= PocketKey.ENTER.ordinal -> 1
                            else -> 2
                        }
                    }.thenBy { it.ordinal })
                    (listOf<PocketKey?>(null) + choices).forEach { key ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onSettingsChange(settings.withBinding(button, key)); selectedButton = null },
                        ) { Text(GamepadKeys.label(key)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedButton = null }) { Text("閉じる") } },
        )
    }
}

@Composable
private fun ControllerDiagram(
    settings: GamepadSettings,
    pressed: Set<GamepadButton>,
    allowedKeys: Set<PocketKey>,
    availableButtons: Set<GamepadButton>,
    onClick: (GamepadButton) -> Unit,
) {
    val body = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    // Keep the artwork and button positions fixed; narrow screens can scroll it.
    val diagramWidth = 360.dp
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Box(Modifier.width(diagramWidth).height(260.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w * .18f, h * .12f)
                    cubicTo(w * .02f, h * .12f, 0f, h * .93f, w * .12f, h * .96f)
                    cubicTo(w * .20f, h, w * .26f, h * .75f, w * .34f, h * .72f)
                    lineTo(w * .66f, h * .72f)
                    cubicTo(w * .74f, h * .75f, w * .80f, h, w * .88f, h * .96f)
                    cubicTo(w, h * .93f, w * .98f, h * .12f, w * .82f, h * .12f)
                    close()
                }
                drawPath(path, body)
                drawRoundRect(accent.copy(alpha = .25f), Offset(w * .45f, h * .39f),
                    Size(w * .1f, h * .12f), CornerRadius(8.dp.toPx()))
            }
            val positions = mapOf(
                GamepadButton.L1 to (.12f to .0f), GamepadButton.R1 to (.88f to .0f),
                GamepadButton.UP to (.15f to .30f), GamepadButton.LEFT to (.0f to .60f),
                GamepadButton.RIGHT to (.30f to .60f), GamepadButton.DOWN to (.15f to .90f),
                GamepadButton.NORTH to (.85f to .30f), GamepadButton.WEST to (.70f to .60f),
                GamepadButton.EAST to (1f to .60f), GamepadButton.SOUTH to (.85f to .90f),
            )
            val buttonWidth = 72.dp
            positions.forEach { (button, position) ->
                val key = settings.bindings[button]
                val unsupported = key != null && key !in allowedKeys
                OutlinedButton(
                    onClick = { onClick(button) },
                    modifier = Modifier.offset(
                        x = (diagramWidth - buttonWidth) * position.first,
                        y = 200.dp * position.second,
                    ).width(buttonWidth).height(56.dp),
                    contentPadding = PaddingValues(2.dp),
                    border = BorderStroke(if (button in pressed) 3.dp else 1.dp,
                        if (button in pressed) accent else MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (button in pressed) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(button.label, style = MaterialTheme.typography.labelSmall)
                        Text(GamepadKeys.label(key), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium)
                        if (unsupported || button !in availableButtons) {
                            Text("利用不可", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
