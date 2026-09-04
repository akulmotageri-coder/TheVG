package com.example.vga.insight

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs


private val Berry = Color(0xFF9E2A4B)
private val Slate = Color(0xFF2D3142)
private val Muted = Color(0xFF6C727F)
private val SoftMuted = Color(0xFF8D99AE)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFF1ECE7)
private val Rose = Color(0xFFFFE5EC)
private val Blue = Color(0xFFE3F2FD)
private val BlueText = Color(0xFF1E6091)
private val Mint = Color(0xFFE6F4EA)
private val MintText = Color(0xFF137333)
private val Butter = Color(0xFFFFF3CD)
private val ButterText = Color(0xFF854D0E)
private val TrackGrey = Color(0xFFF3F1EE)


/**
 * A single measured point on the trend chart. Values come straight from stored
 * transcript metrics - the chart never smooths, extrapolates or invents data.
 */
data class TrendPoint(
    val timestampMs: Long,
    val value: Double
)


/**
 * Line chart of one measured metric over time.
 *
 * Renders only the points that exist. With fewer than two points it shows an
 * explicit "not enough data" state rather than drawing a misleading line.
 */
@Composable
fun MetricTrendChart(
    metric: LanguageMetric,
    points: List<TrendPoint>,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = metric.label,
                color = Slate,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            points.lastOrNull()?.let {
                Text(
                    text = metric.format(it.value),
                    color = Berry,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${points.size} measured recording" + if (points.size == 1) "" else "s",
            color = SoftMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (points.size < 2) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(TrackGrey, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Not enough data yet — at least 2 analysed\nrecordings are needed for a trend.",
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

        } else {

            val values = points.map { it.value }
            val min = values.min()
            val max = values.max()
            val span = if (abs(max - min) < 1e-9) 1.0 else max - min

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {

                val leftPad = 6.dp.toPx()
                val rightPad = 6.dp.toPx()
                val topPad = 10.dp.toPx()
                val bottomPad = 10.dp.toPx()

                val chartWidth = size.width - leftPad - rightPad
                val chartHeight = size.height - topPad - bottomPad

                // horizontal guides
                repeat(4) { index ->
                    val y = topPad + chartHeight * index / 3f
                    drawLine(
                        color = Border,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1f
                    )
                }

                fun pointOffset(index: Int): Offset {
                    val x = leftPad + chartWidth * index / (points.size - 1).toFloat()
                    val normalised = ((values[index] - min) / span).toFloat()
                    val y = topPad + chartHeight * (1f - normalised)
                    return Offset(x, y)
                }

                // filled area
                val area = Path().apply {
                    moveTo(leftPad, topPad + chartHeight)
                    points.indices.forEach { index ->
                        val offset = pointOffset(index)
                        lineTo(offset.x, offset.y)
                    }
                    lineTo(leftPad + chartWidth, topPad + chartHeight)
                    close()
                }
                drawPath(area, color = Rose.copy(alpha = 0.55f))

                // line
                val line = Path().apply {
                    points.indices.forEach { index ->
                        val offset = pointOffset(index)
                        if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                drawPath(line, color = Berry, style = Stroke(width = 2.5.dp.toPx()))

                // markers
                points.indices.forEach { index ->
                    val offset = pointOffset(index)
                    drawCircle(color = White, radius = 4.5.dp.toPx(), center = offset)
                    drawCircle(color = Berry, radius = 3.dp.toPx(), center = offset)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = metric.format(values.first()),
                    color = SoftMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "oldest → newest",
                    color = SoftMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = metric.format(values.last()),
                    color = SoftMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}


/** Horizontal selector for which metric the trend chart shows. */
@Composable
fun MetricSelector(
    selected: LanguageMetric,
    onSelect: (LanguageMetric) -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        LanguageMetric.entries.forEach { metric ->

            val isSelected = metric == selected

            Box(
                modifier = Modifier
                    .background(
                        if (isSelected) Berry else White,
                        RoundedCornerShape(50.dp)
                    )
                    .border(
                        1.dp,
                        if (isSelected) Berry else Border,
                        RoundedCornerShape(50.dp)
                    )
                    .clickable { onSelect(metric) }
                    .padding(horizontal = 13.dp, vertical = 8.dp)
            ) {
                Text(
                    text = metric.label,
                    color = if (isSelected) White else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


/**
 * Current vs personal-baseline comparison.
 *
 * Shows an explicit "baseline not yet established" state when there is not
 * enough history, rather than inventing a reference value.
 */
@Composable
fun BaselineComparisonCard(
    current: TranscriptMetrics,
    baseline: Map<LanguageMetric, Double>?,
    historyCount: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {

        Text(
            text = "Current vs personal baseline",
            color = Slate,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (baseline == null) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Butter, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Baseline not yet established.\n" +
                        "$historyCount of ${TranscriptAnalytics.MIN_HISTORY_FOR_BASELINE} " +
                        "analysable recordings collected.",
                    color = ButterText,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

        } else {

            LanguageMetric.entries.forEachIndexed { index, metric ->

                if (index > 0) Spacer(modifier = Modifier.height(12.dp))

                val now = metric.valueOf(current)
                val before = baseline[metric] ?: 0.0
                val change = TranscriptAnalytics.percentChange(now, before)

                BaselineRow(
                    metric = metric,
                    current = now,
                    baseline = before,
                    changePercent = change
                )
            }
        }
    }
}


@Composable
private fun BaselineRow(
    metric: LanguageMetric,
    current: Double,
    baseline: Double,
    changePercent: Double?
) {

    val notable =
        changePercent != null &&
            abs(changePercent) >= 25.0 &&
            (changePercent > 0) == metric.higherIsNotable

    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = metric.label,
                color = Slate,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            changePercent?.let {
                Box(
                    modifier = Modifier
                        .background(
                            if (notable) Butter else Mint,
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${if (it > 0) "+" else ""}${it.toInt()}%",
                        color = if (notable) ButterText else MintText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val maxValue = maxOf(current, baseline, 1e-9)

        // current
        BarRow(
            label = "Current",
            valueText = metric.format(current),
            fraction = (current / maxValue).toFloat(),
            barColor = if (notable) ButterText else Berry
        )

        Spacer(modifier = Modifier.height(4.dp))

        // baseline
        BarRow(
            label = "Baseline",
            valueText = metric.format(baseline),
            fraction = (baseline / maxValue).toFloat(),
            barColor = SoftMuted
        )
    }
}


@Composable
private fun BarRow(
    label: String,
    valueText: String,
    fraction: Float,
    barColor: Color
) {

    Row(verticalAlignment = Alignment.CenterVertically) {

        Text(
            text = label,
            color = SoftMuted,
            fontSize = 10.sp,
            modifier = Modifier.width(56.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(9.dp)
                .background(TrackGrey, RoundedCornerShape(50.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(9.dp)
                    .background(barColor, RoundedCornerShape(50.dp))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = valueText,
            color = Slate,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(58.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}


/** Availability overview across the four signal families. */
@Composable
fun MultimodalOverview(
    statuses: List<TimelineBuilder.ModalityStatus>,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(22.dp))
            .border(1.dp, Border, RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {

        Text(
            text = "Available signals",
            color = Slate,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        statuses.chunked(2).forEach { row ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { status ->
                    ModalityTile(
                        status = status,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}


@Composable
private fun ModalityTile(
    status: TimelineBuilder.ModalityStatus,
    modifier: Modifier = Modifier
) {

    val background = if (status.available) Mint else TrackGrey
    val foreground = if (status.available) MintText else Muted

    Column(
        modifier = modifier
            .background(background, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(text = status.icon, fontSize = 14.sp)

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = status.label,
                color = foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(
                        if (status.available) MintText else SoftMuted,
                        CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = status.detail,
            color = foreground.copy(alpha = 0.85f),
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}
