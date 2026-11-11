package com.aio.tracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Simple animated vertical bar chart. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int = -1,
    chartHeight: Dp = 120.dp,
    topLabels: List<String>? = null,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(700))
    }
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val track = barColor.copy(alpha = 0.12f)
    Column(modifier.fillMaxWidth()) {
        if (topLabels != null) {
            Row(Modifier.fillMaxWidth()) {
                topLabels.forEach {
                    Text(
                        it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(chartHeight).padding(vertical = 4.dp)) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.52f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(barWidth / 2, barWidth / 2)
            values.forEachIndexed { i, v ->
                val x = slot * i + (slot - barWidth) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barWidth, size.height), radius)
                val raw = (v / max) * size.height * anim.value
                if (raw > 0f) {
                    // Keep at least a full "pill" visible for tiny values, but never draw outside the chart.
                    val h = raw.coerceAtLeast(barWidth).coerceAtMost(size.height)
                    val c = if (highlightIndex == -1 || highlightIndex == i) barColor else barColor.copy(alpha = 0.55f)
                    drawRoundRect(c, Offset(x, size.height - h), Size(barWidth, h), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == highlightIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Animated donut chart with optional center content. */
@Composable
fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    thickness: Dp = 18.dp,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(values) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(800))
    }
    val total = values.sum()
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = thickness.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            if (total <= 0f) return@Canvas
            var start = -90f
            val gap = if (values.count { it > 0f } > 1) 2.5f else 0f
            values.forEachIndexed { i, v ->
                if (v <= 0f) return@forEachIndexed
                val sweep = 360f * (v / total) * anim.value
                drawArc(
                    colors[i % colors.size], start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), false, topLeft, arcSize,
                    style = Stroke(s, cap = StrokeCap.Butt),
                )
                start += sweep
            }
        }
        center()
    }
}

@Composable
fun LegendRow(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dot(color, size = 10.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}
