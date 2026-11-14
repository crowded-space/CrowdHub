package app.crowdhub.feature.stocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.crowdhub.core.ui.theme.CrowColors
import app.crowdhub.core.ui.theme.TabularNumberStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SparklineChart(
    points: List<StockPricePoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minP = points.minOf { it.price }
        val maxP = points.maxOf { it.price }
        val rangeP = (maxP - minP).takeIf { it > 0.0 } ?: 1.0
        val minT = points.first().tsMs
        val maxT = points.last().tsMs
        val rangeT = (maxT - minT).coerceAtLeast(1L).toDouble()

        val path = Path()
        points.forEachIndexed { idx, pt ->
            val x = (((pt.tsMs - minT).toDouble() / rangeT) * size.width).toFloat()
            val y = (size.height - (((pt.price - minP) / rangeP) * size.height)).toFloat()
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun PriceChart(
    points: List<StockPricePoint>,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(points) { mutableStateOf<Int?>(null) }
    val timeFmt = remember { SimpleDateFormat("MMM d HH:mm", Locale.getDefault()) }

    Column(modifier = modifier) {
        val inspected = selectedIdx?.let { points.getOrNull(it) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (inspected != null) {
                Text(
                    text = "$${"%.2f".format(inspected.price)} · ${timeFmt.format(Date(inspected.tsMs))}",
                    style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
                    color = CrowColors.AccentD
                )
            } else if (points.size >= 2) {
                val minP = points.minOf { it.price }
                val maxP = points.maxOf { it.price }
                Text(
                    text = "Low $${"%.2f".format(minP)} · High $${"%.2f".format(maxP)}",
                    style = MaterialTheme.typography.labelSmall.merge(TabularNumberStyle),
                    color = CrowColors.TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (points.size < 2) {
                val sinceText = points.firstOrNull()?.let {
                    "Collecting data — first point at ${timeFmt.format(Date(it.tsMs))}"
                } ?: "Collecting data — snapshots record every minute."
                Text(
                    text = sinceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = CrowColors.TextMuted,
                    modifier = Modifier.padding(top = 60.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                selectedIdx = (ratio * (points.size - 1)).roundToInt()
                                    .coerceIn(0, points.lastIndex)
                            }
                        }
                        .pointerInput(points) {
                            detectDragGestures(
                                onDragEnd = { },
                                onDrag = { change, _ ->
                                    val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                    selectedIdx = (ratio * (points.size - 1)).roundToInt()
                                        .coerceIn(0, points.lastIndex)
                                }
                            )
                        }
                ) {
                    val gridRows = 4
                    for (i in 0..gridRows) {
                        val y = size.height * (i.toFloat() / gridRows)
                        drawLine(
                            color = CrowColors.Outline,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val minP = points.minOf { it.price }
                    val maxP = points.maxOf { it.price }
                    val rangeP = (maxP - minP).takeIf { it > 0.0 } ?: 1.0
                    val minT = points.first().tsMs
                    val maxT = points.last().tsMs
                    val rangeT = (maxT - minT).coerceAtLeast(1L).toDouble()

                    val path = Path()
                    val coords = ArrayList<Offset>(points.size)
                    points.forEachIndexed { idx, pt ->
                        val x = (((pt.tsMs - minT).toDouble() / rangeT) * size.width).toFloat()
                        val y = (size.height - (((pt.price - minP) / rangeP) * (size.height - 16f)) - 8f).toFloat()
                        coords += Offset(x, y)
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = CrowColors.Sheen,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    selectedIdx?.let { idx ->
                        val pt = coords.getOrNull(idx) ?: return@let
                        drawLine(
                            color = CrowColors.AccentD.copy(alpha = 0.7f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(
                            color = CrowColors.AccentD,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }
    }
}
