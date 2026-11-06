package com.alican.atmos.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.core.PrecipUnit
import com.alican.atmos.core.TempUnit
import com.alican.atmos.ui.Fmt
import com.alican.atmos.ui.theme.Atmos
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Hourly temperature curve. Straight segments between the API's own hourly values — no
 * invented smoothing, no fake intermediates.
 */
@Composable
fun TemperatureCurve(
    values: List<Double>,
    times: List<LocalDateTime>,
    unit: TempUnit,
    modifier: Modifier = Modifier,
    color: Color = Atmos.Ice
) {
    if (values.size < 2) {
        Canvas(modifier) {}
        return
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        val left = 28.dp.toPx()
        val right = 4.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 18.dp.toPx()
        val chartW = size.width - left - right
        val chartH = size.height - top - bottom
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).coerceAtLeast(1.0)

        fun xAt(index: Int): Float =
            left + chartW * index / (values.size - 1).toFloat()

        fun yAt(value: Double): Float =
            top + chartH * (1f - ((value - minV) / span).toFloat())

        // Grid.
        for (g in 0..2) {
            val y = top + chartH * g / 2f
            drawLine(
                color = Color.White.copy(alpha = 0.055f),
                start = Offset(left, y),
                end = Offset(size.width - right, y),
                strokeWidth = 0.8.dp.toPx()
            )
        }

        val line = Path()
        values.forEachIndexed { i, v ->
            if (i == 0) line.moveTo(xAt(i), yAt(v)) else line.lineTo(xAt(i), yAt(v))
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(xAt(values.lastIndex), top + chartH)
            lineTo(xAt(0), top + chartH)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.01f)),
                startY = top,
                endY = top + chartH
            )
        )
        drawPath(
            line,
            color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Hour dots, slightly stronger every six hours.
        values.indices.forEach { i ->
            val major = i == 0 || i == values.lastIndex || i % 6 == 0
            drawCircle(
                color = if (i == 0) color else color.copy(alpha = if (major) 0.72f else 0.38f),
                radius = if (i == 0) 3.2.dp.toPx() else if (major) 2.2.dp.toPx() else 1.6.dp.toPx(),
                center = Offset(xAt(i), yAt(values[i]))
            )
        }
        // A soft halo on the current point.
        drawCircle(
            color = color.copy(alpha = 0.16f),
            radius = 8.dp.toPx(),
            center = Offset(xAt(0), yAt(values[0]))
        )

        paint.textSize = 9.sp.toPx()
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)

        // Temperature scale at the left edge.
        paint.color = Atmos.Muted.toArgb()
        paint.textAlign = Paint.Align.LEFT
        drawContext.canvas.nativeCanvas.drawText(
            "${Fmt.temp(maxV, unit)}°", 0f, top + 3.sp.toPx(), paint
        )
        drawContext.canvas.nativeCanvas.drawText(
            "${Fmt.temp(minV, unit)}°", 0f, top + chartH + 3.sp.toPx(), paint
        )

        // Five honest time ticks.
        paint.color = Atmos.Muted.copy(alpha = 0.85f).toArgb()
        val marks = listOf(0, values.size / 4, values.size / 2, values.size * 3 / 4, values.lastIndex)
            .distinct()
            .filter { it in values.indices }
        marks.forEachIndexed { order, i ->
            val text = times.getOrNull(i)?.let { Fmt.hourShort(it) } ?: ""
            if (text.isEmpty()) return@forEachIndexed
            paint.textAlign = when (order) {
                0 -> Paint.Align.LEFT
                marks.lastIndex -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                xAt(i),
                size.height - 3.dp.toPx(),
                paint
            )
        }
    }
}

/**
 * Hourly precipitation amounts as bars, in millimetres (or inches when selected).
 * Only the amount is encoded here; probability is shown as text so the two never blur.
 */
@Composable
fun PrecipBarChart(
    amounts: List<Double>,
    times: List<LocalDateTime>,
    unit: PrecipUnit,
    modifier: Modifier = Modifier,
    barColor: Color = Atmos.Ice
) {
    if (amounts.isEmpty()) {
        Canvas(modifier) {}
        return
    }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        val left = 2.dp.toPx()
        val right = 2.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = 18.dp.toPx()
        val chartW = size.width - left - right
        val chartH = size.height - top - bottom
        if (chartW <= 0f || chartH <= 0f) return@Canvas

        val peak = max(0.3, amounts.max())
        val baseY = top + chartH
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(left, baseY),
            end = Offset(size.width - right, baseY),
            strokeWidth = 0.8.dp.toPx()
        )

        val slot = chartW / amounts.size
        val barW = min(slot * 0.58f, 12.dp.toPx())
        var peakIndex = -1
        var peakValue = 0.0

        amounts.forEachIndexed { i, amount ->
            val x = left + slot * i + (slot - barW) / 2f
            if (amount > 0.001) {
                val fraction = (amount / peak).toFloat().coerceIn(0.03f, 1f)
                val barH = chartH * fraction
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            barColor.copy(alpha = 0.92f),
                            barColor.copy(alpha = 0.34f)
                        ),
                        startY = baseY - barH,
                        endY = baseY
                    ),
                    topLeft = Offset(x, baseY - barH),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                )
                if (amount > peakValue) {
                    peakValue = amount
                    peakIndex = i
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.13f),
                    radius = 1.dp.toPx(),
                    center = Offset(x + barW / 2f, baseY - 1.dp.toPx())
                )
            }
        }

        paint.textSize = 9.sp.toPx()
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        paint.color = Atmos.Muted.toArgb()

        // Label the single highest bar, not every bar: fewer numbers, more shape.
        if (peakIndex >= 0) {
            paint.textAlign = Paint.Align.CENTER
            val x = (left + slot * peakIndex + slot / 2f).coerceIn(10.dp.toPx(), size.width - 10.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                "${Fmt.precip(peakValue, unit)}",
                x,
                9.sp.toPx(),
                paint
            )
        }

        paint.color = Atmos.Muted.copy(alpha = 0.85f).toArgb()
        val marks = listOf(0, amounts.size / 4, amounts.size / 2, amounts.size * 3 / 4, amounts.lastIndex)
            .distinct()
            .filter { it in amounts.indices }
        marks.forEachIndexed { order, i ->
            val text = times.getOrNull(i)?.let { Fmt.hourShort(it) } ?: ""
            if (text.isEmpty()) return@forEachIndexed
            paint.textAlign = when (order) {
                0 -> Paint.Align.LEFT
                marks.lastIndex -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                (left + slot * i + slot / 2f).coerceIn(8.dp.toPx(), size.width - 8.dp.toPx()),
                size.height - 3.dp.toPx(),
                paint
            )
        }
    }
}

/** Segmented colour scale (UV, AQI). The marker is the real value, not a decoration. */
@Composable
fun SegmentedBar(
    segments: List<Color>,
    fraction: Float,
    modifier: Modifier = Modifier,
    showMarker: Boolean = true
) {
    if (segments.isEmpty()) {
        Canvas(modifier) {}
        return
    }
    Canvas(modifier) {
        val barH = min(8.dp.toPx(), size.height * 0.55f)
        val top = (size.height - barH) / 2f
        val gap = 2.dp.toPx()
        val segW = (size.width - gap * (segments.size - 1)) / segments.size
        if (segW <= 0f) return@Canvas
        segments.forEachIndexed { i, color ->
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (segW + gap), top),
                size = Size(segW, barH),
                cornerRadius = CornerRadius(barH / 2f, barH / 2f)
            )
        }
        if (showMarker) {
            val x = (fraction.coerceIn(0f, 1f) * size.width)
                .coerceIn(2.dp.toPx(), size.width - 2.dp.toPx())
            drawLine(
                color = Color.White.copy(alpha = 0.92f),
                start = Offset(x, 1.dp.toPx()),
                end = Offset(x, size.height - 1.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White,
                radius = 2.2.dp.toPx(),
                center = Offset(x, size.height / 2f)
            )
        }
    }
}

/** Sunrise → sunset arc with the sun at its real position between the two times. */
@Composable
fun SunArc(
    fraction: Float?,
    modifier: Modifier = Modifier,
    color: Color = Atmos.Warm
) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.88f
        val radius = min(size.width * 0.44f, size.height * 0.78f)
        if (radius <= 0f) return@Canvas

        // Horizon.
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(cx - radius - 6.dp.toPx(), cy),
            end = Offset(cx + radius + 6.dp.toPx(), cy),
            strokeWidth = 0.8.dp.toPx()
        )
        // The arc itself, warm at the apex and muted at the horizon.
        drawArc(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.14f),
                    color.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.14f)
                ),
                startX = cx - radius,
                endX = cx + radius
            ),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - radius, cy - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        if (fraction != null) {
            val angle = PI * (1.0 + fraction.coerceIn(0f, 1f))
            val sx = cx + radius * cos(angle).toFloat()
            val sy = cy + radius * sin(angle).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = 0.40f), Color.Transparent),
                    center = Offset(sx, sy),
                    radius = 13.dp.toPx()
                ),
                radius = 13.dp.toPx(),
                center = Offset(sx, sy)
            )
            drawCircle(color = color, radius = 3.6.dp.toPx(), center = Offset(sx, sy))
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 1.3.dp.toPx(), center = Offset(sx, sy))
        }
    }
}

/** Horizontal score bar for activity suitability. */
@Composable
fun ScoreBar(score: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.height / 2f
        drawRoundRect(
            color = Color.White.copy(alpha = 0.06f),
            cornerRadius = CornerRadius(r, r),
            size = size
        )
        val w = (size.width * score.coerceIn(0, 100) / 100f).coerceAtLeast(size.height)
        if (w > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(color.copy(alpha = 0.45f), color)),
                size = Size(w.coerceAtMost(size.width), size.height),
                cornerRadius = CornerRadius(r, r)
            )
        }
    }
}
