package com.digihori.pgp.desktop.wav

import androidx.compose.foundation.background
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import com.digihori.pgp.desktop.localization.*
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digihori.pgp.core.wav.OldSignalDecoder
import kotlin.math.*
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WavWaveform(output: DesktopWavResult, cursor: Int, onCursor: (Int) -> Unit, onRaw: (Int) -> Unit, strings: StudioStrings = StudioStrings.system) {
    val pcm = output.pcm
    val samples = remember(output) { pcm?.samples?.copySamples() }
    if (pcm == null || samples == null || samples.isEmpty()) { Text(strings.text("wave.noPcm")); return }
    val inspection = remember(output) { OldSignalDecoder.inspect(pcm.sampleRate, pcm.samples) }
    val differences = remember(inspection) { inspection.copyDifferences() }
    val amplitudes = remember(samples) { IntArray(samples.size) { samples[it].toInt() } }
    val statistics = remember(samples) {
        Triple(samples.min().toInt(), samples.max().toInt(), samples.sumOf { it.toLong() }.toDouble() / samples.size)
    }
    var start by remember(output) { mutableStateOf(0) }
    var span by remember(output) { mutableStateOf(samples.size) }
    var selectionEnd by remember(output) { mutableStateOf<Int?>(null) }
    var stepDisplay by remember(output) { mutableStateOf(output.wav != null) }
    var showDifference by remember(output) { mutableStateOf(false) }
    var amplitudeGuides by remember(output) { mutableStateOf(false) }
    var guide by remember(output) { mutableStateOf(5120f) }
    var gestureOffset by remember(output) { mutableStateOf(0.0) }
    var gestureStart by remember(output) { mutableStateOf(-1) }
    var plotWidth by remember { mutableStateOf(1.0) }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurface
    val gridColor = axisColor.copy(alpha = .18f)
    val adapter = remember(output) {
        object : ScrollbarAdapter {
            override val scrollOffset: Double get() = start.toDouble()
            override val contentSize: Double get() = samples.size.toDouble()
            override val viewportSize: Double get() = span.toDouble()
            override suspend fun scrollTo(scrollOffset: Double) {
                start = scrollOffset.roundToLong().coerceIn(0, (samples.size - span).coerceAtLeast(0).toLong()).toInt()
            }
        }
    }
    LaunchedEffect(cursor) {
        if (cursor !in start until start + span) start = (cursor - span / 2).coerceIn(0, samples.size - span)
    }
    fun zoom(newSpan: Int) {
        val next = newSpan.coerceIn(minOf(8, samples.size), samples.size)
        start = WaveformViewport.zoomStart(start, span, next, samples.size)
        span = next
    }
    Row {
        Checkbox(stepDisplay, { stepDisplay = it }, modifier = Modifier.semantics { contentDescription = strings.text("wave.hold") }); Text(strings.text("wave.hold"))
    }
    Text(if (stepDisplay) strings.text("wave.holdHelp") else strings.text("wave.connected"))
    Text(strings.text("wave.statistics", statistics.first.toString(), statistics.second.toString(), format(statistics.third, 1)))
    Row { Checkbox(amplitudeGuides, { amplitudeGuides = it }, modifier = Modifier.semantics { contentDescription = strings.text("wave.guides") }); Text(strings.text("wave.guides")) }
    if (amplitudeGuides) {
        Text(strings.text("wave.guideValue", guide.roundToInt().toString()))
        Slider(guide, { guide = it }, valueRange = 0f..32767f, modifier = Modifier.semantics { contentDescription = strings.text("wave.guideValue", guide.roundToInt().toString()) })
    }
    Row { Checkbox(showDifference, { showDifference = it }, modifier = Modifier.semantics { contentDescription = strings.text("wave.difference") }); Text(strings.text("wave.difference")) }
    Text(strings.text("wave.algorithm", inspection.tapSamples.toString(), inspection.threshold.toString()))
    Text(strings.text("wave.samples", start.toString(), (start + span).toString(), cursor.toString(), time(cursor.toDouble() / pcm.sampleRate), samples[cursor.coerceIn(samples.indices)].toString()))

    val values = amplitudes
    val bound = 32768f
    Text(strings.text("wave.blue"))
    if (showDifference) {
        Text(strings.text("wave.orange", inspection.threshold.toString()))
        if ((start until start + span).all { differences[it] == 0 }) {
            Text(strings.text("wave.zero"))
        }
    }
    Canvas(Modifier.semantics { contentDescription = strings.text("wave.canvas") }.fillMaxWidth().height(280.dp).onSizeChanged { plotWidth = (it.width - 80).toDouble().coerceAtLeast(1.0) }
        .onPointerEvent(PointerEventType.Scroll, pass = PointerEventPass.Initial) { event ->
            val delta = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
            val horizontal = if (abs(delta.x) > .01f) delta.x else if (event.keyboardModifiers.isShiftPressed) delta.y else 0f
            if (horizontal != 0f) {
                if (gestureStart != start) gestureOffset = start.toDouble()
                gestureOffset = WaveformViewport.panPosition(gestureOffset, span, samples.size, horizontal.toDouble(), plotWidth)
                start = gestureOffset.roundToInt()
                gestureStart = start
                event.changes.forEach { it.consume() }
            }
        }
        .pointerInput(start, span) {
            fun sample(x: Float): Int = (start + ((x - 64f) / (size.width - 80).coerceAtLeast(1)) * span).toInt().coerceIn(samples.indices)
            detectTapGestures(onTap = { selectionEnd = null; onCursor(sample(it.x)) }, onLongPress = { selectionEnd = sample(it.x) })
        }) {
        val left = 64f; val right = size.width - 16f
        val top = 22f; val bottom = size.height - 36f
        val width = (right - left).coerceAtLeast(1f)
        val height = bottom - top
        fun x(index: Int) = left + (index - start).toFloat() / span * width
        fun y(value: Float) = top + height / 2 - value / bound * height / 2
        fun label(text: String, position: Offset) = drawText(textMeasurer, text, position,
            style = TextStyle(color = axisColor, fontSize = 11.sp))
        for (fraction in listOf(1f, .5f, 0f, -.5f, -1f)) {
            val amplitude = bound * fraction
            drawLine(if (fraction == 0f) axisColor.copy(alpha = .65f) else gridColor,
                Offset(left, y(amplitude)), Offset(right, y(amplitude)), if (fraction == 0f) 1.5f else 1f)
            label((if (amplitude > 0) "+" else "") + amplitude.roundToInt(), Offset(0f, y(amplitude) - 7f))
        }
        val seconds = span.toDouble() / pcm.sampleRate
        val step = WaveformViewport.tickStep(seconds)
        var tick = ceil(start.toDouble() / pcm.sampleRate / step) * step
        val end = (start + span).toDouble() / pcm.sampleRate
        while (tick <= end) {
            val tickX = left + ((tick * pcm.sampleRate - start) / span * width).toFloat()
            drawLine(gridColor, Offset(tickX, top), Offset(tickX, bottom))
            drawLine(axisColor, Offset(tickX, bottom), Offset(tickX, bottom + 4))
            val tickDecimals = maxOf(3, ceil(-log10(step)).toInt()).coerceAtMost(9)
            val tickLabel = if (seconds < 1) "${format(tick * 1000, (tickDecimals - 3).coerceAtLeast(0))} ms" else "${format(tick, tickDecimals)} s"
            label(tickLabel, Offset((tickX - 22).coerceIn(0f, (size.width - 55).coerceAtLeast(0f)), bottom + 7))
            tick += step
        }
        label("PCM", Offset(0f, 0f))
        clipRect(left, top, right, bottom) {
            selectionEnd?.let { endSample ->
                val a = x(minOf(cursor, endSample)); val b = x(maxOf(cursor, endSample))
                drawRect(Color(0x334caf50), Offset(a, top), Size(b - a, height))
            }
            val pixels = width.toInt().coerceAtLeast(1)
            if (span >= pixels) {
                for (pixel in 0 until pixels) {
                    val a = start + (pixel.toLong() * span / pixels).toInt()
                    val b = (start + ((pixel + 1L) * span / pixels).toInt()).coerceAtMost(values.size)
                    var min = values[a]; var max = min
                    for (i in a until b.coerceAtLeast(a + 1)) { min = minOf(min, values[i]); max = maxOf(max, values[i]) }
                    drawLine(Color(0xff1976d2), Offset(left + pixel, y(min.toFloat())), Offset(left + pixel, y(max.toFloat())))
                }
            } else {
                for (i in start until (start + span).coerceAtMost(values.size)) {
                    val value = values[i].toFloat()
                    if (stepDisplay) {
                        drawLine(Color(0xff1976d2), Offset(x(i), y(value)), Offset(x(i + 1), y(value)), 1.5f)
                        if (i + 1 < values.size) drawLine(Color(0xff1976d2), Offset(x(i + 1), y(value)), Offset(x(i + 1), y(values[i + 1].toFloat())), 1.5f)
                    } else if (i + 1 < values.size) {
                        drawLine(Color(0xff1976d2), Offset(x(i), y(value)), Offset(x(i + 1), y(values[i + 1].toFloat())), 1.5f)
                    }
                    drawCircle(Color(0xff1976d2), 2f, Offset(x(i), y(value)))
                    if (i > 0 && ((values[i - 1] < 0 && values[i] >= 0) || (values[i - 1] > 0 && values[i] <= 0))) {
                        val fraction = -values[i - 1].toFloat() / (values[i] - values[i - 1])
                        val crossX = if (stepDisplay) x(i) else x(i - 1) + fraction * (x(i) - x(i - 1))
                        drawCircle(Color(0xff00897b), 3f, Offset(crossX, y(0f)))
                    }
                }
            }
            if (amplitudeGuides) for (value in listOf(guide, -guide)) {
                drawLine(Color(0xff8e24aa), Offset(left, y(value)), Offset(right, y(value)), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }
            if (showDifference) {
                fun deltaY(value: Int) = top + height / 2 - value.toFloat() / 65536 * height / 2
                val deltaColor = Color(0xffef6c00)
                if (span >= pixels) {
                    for (pixel in 0 until pixels) {
                        val a = start + (pixel.toLong() * span / pixels).toInt()
                        val b = (start + ((pixel + 1L) * span / pixels).toInt()).coerceAtMost(differences.size)
                        var min = differences[a]; var max = min
                        for (i in a until b.coerceAtLeast(a + 1)) { min = minOf(min, differences[i]); max = maxOf(max, differences[i]) }
                        if (min == max) drawLine(deltaColor, Offset(left + pixel, deltaY(min)), Offset(left + pixel + 1, deltaY(max)), 1.5f)
                        else drawLine(deltaColor, Offset(left + pixel, deltaY(min)), Offset(left + pixel, deltaY(max)), 1.5f)
                    }
                } else for (i in start until (start + span).coerceAtMost(differences.size)) {
                    if (stepDisplay) {
                        drawLine(deltaColor, Offset(x(i), deltaY(differences[i])), Offset(x(i + 1), deltaY(differences[i])), 1.5f)
                        if (i + 1 < differences.size) drawLine(deltaColor, Offset(x(i + 1), deltaY(differences[i])), Offset(x(i + 1), deltaY(differences[i + 1])), 1.5f)
                    } else if (i + 1 < differences.size) {
                        drawLine(deltaColor, Offset(x(i), deltaY(differences[i])), Offset(x(i + 1), deltaY(differences[i + 1])), 1.5f)
                    }
                }
                for (value in listOf(inspection.threshold, -inspection.threshold)) drawLine(deltaColor,
                    Offset(left, deltaY(value)), Offset(right, deltaY(value)), 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }
            output.syncPoints.filter { it.sampleOffset in start until start + span }.forEach {
                drawLine(Color(0x887e57c2), Offset(x(it.sampleOffset), top), Offset(x(it.sampleOffset), top + 10))
            }
            drawLine(Color.Red, Offset(x(cursor), top), Offset(x(cursor), bottom))
        }
    }
    HorizontalScrollbar(
        adapter,
        Modifier.fillMaxWidth().height(16.dp).background(gridColor),
        style = LocalScrollbarStyle.current.copy(
            minimalHeight = 36.dp, thickness = 12.dp,
            unhoverColor = axisColor.copy(alpha = .5f), hoverColor = axisColor.copy(alpha = .8f),
        ),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { zoom(span / 2) }) { Text(strings.text("wave.zoomIn")) }
        OutlinedButton(onClick = { zoom((span.toLong() * 2).coerceAtMost(samples.size.toLong()).toInt()) }) { Text(strings.text("wave.zoomOut")) }
        OutlinedButton(modifier = Modifier.semantics { contentDescription = strings.text("wave.left") }, onClick = { start = (start - span / 2).coerceAtLeast(0) }) { Text("←") }
        OutlinedButton(modifier = Modifier.semantics { contentDescription = strings.text("wave.right") }, onClick = { start = (start + span / 2).coerceAtMost(samples.size - span) }) { Text("→") }
        OutlinedButton(onClick = { start = 0; span = samples.size }) { Text(strings.text("wave.fit")) }
    }
    Text(strings.text("wave.panHelp"))
    Text(strings.text("wave.cursorHelp"))
    selectionEnd?.let { Text(strings.text("wave.selected", minOf(cursor, it).toString(), maxOf(cursor, it).toString())) }
    val raw = output.mappings.firstOrNull { cursor in it.source }
    Text(raw?.let { strings.text("wave.rawCursor", it.target.offset.toString()) } ?: strings.text("wave.outside"))
    raw?.let { OutlinedButton(onClick = { onRaw(it.target.offset) }) { Text(strings.text("wave.showRaw", it.target.offset.toString())) } }
}

private fun format(value: Double, decimals: Int) = String.format(Locale.ROOT, "%.${decimals}f", value)
private fun time(seconds: Double): String = if (abs(seconds) < 1.0) "${format(seconds * 1000, 3)} ms" else "${format(seconds, 3)} s"
