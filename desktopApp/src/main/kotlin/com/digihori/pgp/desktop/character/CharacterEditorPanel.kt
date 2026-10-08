package com.digihori.pgp.desktop.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.character.CharacterEditorFormats
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
internal fun CharacterEditorPanel() {
    val format = CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP
    var model by remember { mutableStateOf(DesktopCharacterEditorModel(format)) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Character Editor", style = MaterialTheme.typography.titleLarge)
        Text(format.displayName)
        Text("Column-major bytes; the top dot is bit 0. This is a generic prototype format, not a verified machine UDC format.")

        CharacterGrid(
            model = model,
            onDotChange = { column, row, set -> model = model.withDot(column, row, set) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model = model.clear() }) { Text("Clear") }
            Button(onClick = { model = model.invert() }) { Text("Invert") }
        }

        OutlinedTextField(
            value = model.byteText,
            onValueChange = { model = model.withByteText(it) },
            label = { Text("Byte values") },
            supportingText = {
                Text(model.inputError ?: "Enter ${format.byteCount} decimal, 0xNN, or &NN values separated by commas or spaces.")
            },
            isError = model.inputError != null,
            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
        )
        Button(onClick = { model = model.applyByteText() }) { Text("Apply to grid") }

        OutputRow("HEX bytes", model.hexOutput) { copyToClipboard(model.hexOutput) }
        OutputRow("BASIC", model.basicOutput) { copyToClipboard(model.basicOutput) }
        OutputRow("Assembler", model.assemblerOutput) { copyToClipboard(model.assemblerOutput) }
    }
}

private fun copyToClipboard(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

@Composable
private fun CharacterGrid(
    model: DesktopCharacterEditorModel,
    onDotChange: (column: Int, row: Int, set: Boolean) -> Unit,
) {
    var dragSet by remember { mutableStateOf<Boolean?>(null) }
    var lastDragCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val currentModel by rememberUpdatedState(model)
    val currentOnDotChange by rememberUpdatedState(onDotChange)

    fun cellAt(offset: Offset, size: Size): Pair<Int, Int>? = CharacterGridGeometry.cellAt(
        x = offset.x,
        y = offset.y,
        width = size.width,
        height = size.height,
        columns = model.pattern.width,
        rows = model.pattern.height,
    )

    Canvas(
        modifier = Modifier
            .width(240.dp)
            .aspectRatio(model.pattern.width.toFloat() / model.pattern.height)
            .pointerInput(model.pattern.width, model.pattern.height) {
                detectTapGestures { offset ->
                    cellAt(offset, Size(size.width.toFloat(), size.height.toFloat()))?.let { (column, row) ->
                        currentOnDotChange(column, row, !currentModel.pattern.isSet(column, row))
                    }
                }
            }
            .pointerInput(model.pattern.width, model.pattern.height) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val cell = cellAt(offset, Size(size.width.toFloat(), size.height.toFloat()))
                        dragSet = cell?.let { (column, row) -> !currentModel.pattern.isSet(column, row) }
                        lastDragCell = cell
                        if (cell != null && dragSet != null) {
                            currentOnDotChange(cell.first, cell.second, checkNotNull(dragSet))
                        }
                    },
                    onDragEnd = { dragSet = null; lastDragCell = null },
                    onDragCancel = { dragSet = null; lastDragCell = null },
                ) { change, _ ->
                    val cell = cellAt(change.position, Size(size.width.toFloat(), size.height.toFloat()))
                    if (cell != null && cell != lastDragCell && dragSet != null) {
                        currentOnDotChange(cell.first, cell.second, checkNotNull(dragSet))
                        lastDragCell = cell
                    }
                    change.consume()
                }
            },
    ) {
        val cellWidth = size.width / model.pattern.width
        val cellHeight = size.height / model.pattern.height
        for (row in 0 until model.pattern.height) {
            for (column in 0 until model.pattern.width) {
                val topLeft = Offset(column * cellWidth, row * cellHeight)
                drawRect(
                    color = if (model.pattern.isSet(column, row)) Color(0xff202124) else Color(0xfff1f3f4),
                    topLeft = topLeft,
                    size = Size(cellWidth, cellHeight),
                )
                drawRect(
                    color = Color(0xff80868b),
                    topLeft = topLeft,
                    size = Size(cellWidth, cellHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
        }
    }
}

@Composable
private fun OutputRow(label: String, value: String, onCopy: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onCopy) { Text("Copy") }
        }
        Text(value, fontFamily = FontFamily.Monospace)
    }
}
