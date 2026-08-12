package com.fitnessquest.rpg.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt

data class ChartPoint(val x: Float, val y: Float, val label: String? = null, val date: String? = null)

@Composable
fun SimpleLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = Color.White.copy(alpha = 0.1f),
    showLabels: Boolean = true,
    unitLabel: String? = null
) {
    if (points.isEmpty()) {
        Box(modifier.background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("Not enough data yet", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.3f))
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))

    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = 0f
    val maxY = (points.maxOf { it.y } * 1.2f).coerceAtLeast(1f) // Slightly more headroom

    val rangeX = (maxX - minX).coerceAtLeast(1f)
    val rangeY = (maxY - minY).coerceAtLeast(1f)

    val hPadding = with(LocalDensity.current) { 12.dp.toPx() }
    val vPadding = with(LocalDensity.current) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(12.dp))
            .padding(start = 36.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        selectedPointIndex = findNearestPoint(offset, points, size.toSize(), minX, maxX, minY, maxY, hPadding, vPadding)
                    }
                }
                .pointerInput(points) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedPointIndex = findNearestPoint(offset, points, size.toSize(), minX, maxX, minY, maxY, hPadding, vPadding)
                        },
                        onDrag = { change, _ ->
                            selectedPointIndex = findNearestPoint(change.position, points, size.toSize(), minX, maxX, minY, maxY, hPadding, vPadding)
                        },
                        onDragEnd = { selectedPointIndex = -1 },
                        onDragCancel = { selectedPointIndex = -1 }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            
            val drawWidth = width - hPadding * 2
            val drawHeight = height - vPadding * 2

            // Draw Y-Axis Grid & Labels
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val valY = minY + (i * rangeY / gridSteps)
                val py = vPadding + (drawHeight - (i * drawHeight / gridSteps))
                
                drawLine(
                    color = gridColor,
                    start = Offset(0f, py),
                    end = Offset(width, py),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                if (showLabels) {
                    val label = when {
                        rangeY < 1f -> "%.2f".format(valY)
                        rangeY < 10f -> "%.1f".format(valY)
                        else -> valY.roundToInt().toString()
                    }
                    val result = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = result,
                        topLeft = Offset(-result.size.width.toFloat() - 8f, py - result.size.height / 2f)
                    )
                }
            }

            // Map points to screen coordinates
            val mappedPoints = points.map { point ->
                val px = hPadding + (if (rangeX == 0f) drawWidth / 2f else (point.x - minX) / rangeX * drawWidth)
                val py = vPadding + (drawHeight - ((point.y - minY) / rangeY * drawHeight))
                Offset(px, py)
            }

            // Draw Area Gradient
            if (mappedPoints.size > 1) {
                val fillPath = Path().apply {
                    moveTo(mappedPoints[0].x, vPadding + drawHeight)
                    mappedPoints.forEach { lineTo(it.x, it.y) }
                    lineTo(mappedPoints.last().x, vPadding + drawHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.2f), Color.Transparent),
                        startY = mappedPoints.minOf { it.y },
                        endY = vPadding + drawHeight
                    )
                )
            }

            // Draw Line
            val path = Path().apply {
                if (mappedPoints.isNotEmpty()) {
                    moveTo(mappedPoints[0].x, mappedPoints[0].y)
                    for (i in 1 until mappedPoints.size) {
                        lineTo(mappedPoints[i].x, mappedPoints[i].y)
                    }
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw Points
            mappedPoints.forEachIndexed { index, offset ->
                val isSelected = index == selectedPointIndex
                drawCircle(
                    color = if (isSelected) Color.White else lineColor,
                    radius = (if (isSelected) 6.dp else 4.dp).toPx(),
                    center = offset
                )
                if (isSelected) {
                    drawCircle(
                        color = lineColor,
                        radius = 3.dp.toPx(),
                        center = offset
                    )
                    
                    // Tooltip helper
                    val point = points[index]
                    val yVal = if (point.y < 10) "%.2f".format(point.y) else point.y.roundToInt().toString()
                    val tooltipText = "$yVal ${unitLabel ?: ""} (${point.date ?: ""})"
                    val result = textMeasurer.measure(tooltipText, labelStyle.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp))
                    val tooltipX = (offset.x - result.size.width / 2f).coerceIn(0f, width - result.size.width)
                    drawText(
                        textLayoutResult = result,
                        topLeft = Offset(tooltipX, offset.y - result.size.height - 12f)
                    )
                }
            }
            
            // X-Axis Labels (Date for first and last)
            if (showLabels && points.size >= 2) {
                val first = points.first()
                val last = points.last()
                
                if (first.date != null) {
                    val res = textMeasurer.measure(first.date, labelStyle)
                    drawText(res, topLeft = Offset(0f, height + 4f))
                }
                if (last.date != null) {
                    val res = textMeasurer.measure(last.date, labelStyle)
                    drawText(res, topLeft = Offset(width - res.size.width, height + 4f))
                }
            }
        }
    }
}

private fun findNearestPoint(
    offset: Offset,
    points: List<ChartPoint>,
    size: Size,
    minX: Float, maxX: Float, minY: Float, maxY: Float,
    hPadding: Float, vPadding: Float
): Int {
    val width = size.width
    val height = size.height
    
    val drawWidth = width - hPadding * 2
    val drawHeight = height - vPadding * 2
    
    val rangeX = (maxX - minX).coerceAtLeast(1f)
    val rangeY = (maxY - minY).coerceAtLeast(1f)
    
    return points.indices.minByOrNull { i ->
        val p = points[i]
        val px = hPadding + (if (rangeX == 0f) drawWidth / 2f else (p.x - minX) / rangeX * drawWidth)
        val py = vPadding + (drawHeight - ((p.y - minY) / rangeY * drawHeight))
        val dx = px - offset.x
        val dy = py - offset.y
        dx * dx + dy * dy
    } ?: -1
}
