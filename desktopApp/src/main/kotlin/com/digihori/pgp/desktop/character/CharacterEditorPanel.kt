package com.digihori.pgp.desktop.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.digihori.pgp.core.character.CharacterEditorFormats
import com.digihori.pgp.desktop.theme.LocalStudioComponentColors

@Composable
internal fun CharacterEditorPanel() {
    val format = CharacterEditorFormats.FIVE_BY_SEVEN_COLUMN_LSB_TOP
    var model by remember { mutableStateOf(DesktopCharacterEditorModel(format)) }

    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Character Editor", style = MaterialTheme.typography.titleLarge)
                    Text(format.displayName)
                    Text(
                        "Column-major bytes; top dot is bit 0. Generic prototype format, not a verified machine UDC format.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CharacterGrid(
                    model = model,
                    onDotChange = { column, row, set -> model = model.withDot(column, row, set) },
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { model = model.clear() }) { Text("Clear") }
                        Button(onClick = { model = model.invert() }) { Text("Invert") }
                    }
                    OutlinedTextField(
                        value = model.byteText,
                        onValueChange = { model = model.withByteText(it) },
                        label = { Text("Byte values") },
                        supportingText = {
                            Text(
                                model.inputError
                                    ?: "${format.byteCount} decimal, 0xNN, or &NN values",
                            )
                        },
                        isError = model.inputError != null,
                        singleLine = true,
                        modifier = Modifier.widthIn(min = 360.dp, max = 640.dp),
                    )
                    Button(onClick = { model = model.applyByteText() }) { Text("Apply to grid") }
                }
            }

            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(model.ampersandHexOutput, fontFamily = FontFamily.Monospace)
                    Text(model.decimalOutput, fontFamily = FontFamily.Monospace)
                    Text(model.dollarHexOutput, fontFamily = FontFamily.Monospace)
                    Text(model.prefixedHexOutput, fontFamily = FontFamily.Monospace)
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(10.dp),
        )
    }
}

@Composable
private fun CharacterGrid(
    model: DesktopCharacterEditorModel,
    onDotChange: (column: Int, row: Int, set: Boolean) -> Unit,
) {
    val componentColors = LocalStudioComponentColors.current
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
            .width(160.dp)
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
                    color = if (model.pattern.isSet(column, row)) {
                        componentColors.characterDotOn
                    } else {
                        componentColors.characterDotOff
                    },
                    topLeft = topLeft,
                    size = Size(cellWidth, cellHeight),
                )
                drawRect(
                    color = componentColors.characterGrid,
                    topLeft = topLeft,
                    size = Size(cellWidth, cellHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
                )
            }
        }
    }
}
