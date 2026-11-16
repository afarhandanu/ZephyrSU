package com.sukisu.ultra.ui.component.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.data.model.Module

// Ported from FinaSU (M3.6): ui/component/chart/ModulePieChart.kt.
// Adaptations for ZephyrSU:
// - Package me.weishu.kernelsu.* -> com.sukisu.ultra.*.
// - The center label style and the track colour are explicit parameters instead of reading
//   MaterialTheme inside the chart. ZephyrSU runs in two UI modes and Miuix mode provides no
//   MaterialTheme (see ui/theme/MiuixTheme.kt), so each call site passes its own theme tokens;
//   in Material mode the call site passes MaterialTheme values.
// - `titleLargeEmphasized` -> plain `titleLarge` at the Material call site: the emphasized
//   expressive style is not used anywhere in ZephyrSU, so it stays unverified.

/** How wide the donut ring is drawn. */
private val PieStrokeWidth = 20.dp

/** The gap left between two neighbouring slices, in degrees. */
private const val SliceGapDegrees = 2f

/** One count in the pie: how many it is, and the theme colour that carries it. */
@Immutable
data class PieSlice(
    val value: Int,
    val color: Color,
)

/**
 * The module counts as a donut, so a layout can show a proportion instead of a bare number.
 *
 * Slices without a count are dropped, which keeps the ring from repeating the track colour; a pie
 * where nothing is counted shows the bare track. The caller sizes the chart with [modifier].
 */
@Composable
fun ModulePieChart(
    slices: List<PieSlice>,
    centerLabel: String,
    trackColor: Color,
    centerStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val active = slices.filter { it.value > 0 }
    val total = active.sumOf { it.value }
    val gap = if (active.size > 1) SliceGapDegrees else 0f
    val available = 360f - gap * active.size

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = PieStrokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            if (total <= 0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                return@Canvas
            }

            var startAngle = -90f
            active.forEach { slice ->
                val sweep = slice.value.toFloat() / total * available
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep + gap
            }
        }
        Text(
            text = centerLabel,
            style = centerStyle,
        )
    }
}

/**
 * Module list stats header: a donut of enabled vs disabled modules with a legend.
 * The caller passes its own theme colours because Material and Miuix modes resolve
 * their palettes from different theme objects. Shown only when [modules] is non-empty
 * (the call sites gate on that); an empty list would render the bare track.
 * [labelStyle] is also caller-provided for the same reason: Miuix mode has no
 * MaterialTheme, so even the legend typography must come from the call site.
 */
@Composable
fun ModuleStatsDonut(
    modules: List<Module>,
    enabledColor: Color,
    disabledColor: Color,
    labelColor: Color,
    centerStyle: TextStyle,
    labelStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val enabledCount = modules.count { it.enabled }
    val disabledCount = modules.size - enabledCount

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModulePieChart(
            slices = listOf(
                PieSlice(value = enabledCount, color = enabledColor),
                PieSlice(value = disabledCount, color = disabledColor),
            ),
            centerLabel = modules.size.toString(),
            trackColor = disabledColor.copy(alpha = 0.3f),
            centerStyle = centerStyle,
            modifier = Modifier.size(76.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DonutLegend(
                label = stringResource(R.string.module_info_status_enabled),
                count = enabledCount,
                color = enabledColor,
                labelColor = labelColor,
                labelStyle = labelStyle,
            )
            DonutLegend(
                label = stringResource(R.string.module_info_status_disabled),
                count = disabledCount,
                color = disabledColor,
                labelColor = labelColor,
                labelStyle = labelStyle,
            )
        }
    }
}

/** One donut legend line: the slice colour, its name and its count. */
@Composable
private fun DonutLegend(
    label: String,
    count: Int,
    color: Color,
    labelColor: Color,
    labelStyle: TextStyle,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "$label · $count",
            style = labelStyle,
            color = labelColor,
        )
    }
}
