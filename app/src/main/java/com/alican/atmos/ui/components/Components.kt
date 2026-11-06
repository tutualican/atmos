package com.alican.atmos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import kotlin.math.max
import kotlin.math.min

val CardShape = RoundedCornerShape(22.dp)
val SmallShape = RoundedCornerShape(16.dp)

@Composable
fun AtmosCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = CardShape,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val base = Modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Atmos.Surface.copy(alpha = 0.92f),
                    Atmos.Charcoal.copy(alpha = 0.94f)
                )
            )
        )
        .border(
            BorderStroke(0.7.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f)))),
            shape
        )
        .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() } else Modifier)
        .padding(contentPadding)
    Column(modifier = base, content = content)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = Atmos.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onTrailingClick != null) {
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { onTrailingClick() }.padding(4.dp)
                } else Modifier.padding(4.dp)
            ) {
                Text(
                    text = trailing,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = Atmos.Secondary,
                    maxLines = 1
                )
                if (trailingIcon != null) {
                    Spacer(Modifier.width(4.dp))
                    Icon(trailingIcon, contentDescription = null, tint = Atmos.Secondary, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    icon: ImageVector,
    value: String,
    unit: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AtmosCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Text(
                title.uppercase(),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontFamily = GeistMono,
                fontWeight = FontWeight.Light,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                color = Atmos.White.copy(alpha = 0.94f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(
                    unit,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = Atmos.Muted,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            caption,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Atmos.Secondary,
    background: Color = Color.White.copy(alpha = 0.05f),
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun IconButtonSoft(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Atmos.Secondary,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.7.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Compact period selector used across settings and detail sheets. */
@Composable
fun <T> SegmentedOptions(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                if (isSelected) Atmos.SurfaceLift else Color.Transparent,
                tween(180), label = "segBg"
            )
            val fg by animateColorAsState(
                if (isSelected) Atmos.White else Atmos.Muted,
                tween(180), label = "segFg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Small, honest sparkline: no smoothing invention, just the values provided. */
@Composable
fun Sparkline(
    values: List<Float?>,
    modifier: Modifier = Modifier,
    color: Color = Atmos.Ice,
    fill: Boolean = true
) {
    val clean = values.filterNotNull()
    if (clean.size < 2) {
        Box(modifier)
        return
    }
    val minV = clean.min()
    val maxV = clean.max()
    val span = max(0.001f, maxV - minV)
    Canvas(modifier = modifier) {
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val path = Path()
        var started = false
        values.forEachIndexed { i, v ->
            if (v == null) return@forEachIndexed
            val x = i * step
            val y = size.height - ((v - minV) / span) * size.height * 0.86f - size.height * 0.07f
            if (!started) {
                path.moveTo(x, y); started = true
            } else path.lineTo(x, y)
        }
        if (fill) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                fillPath,
                Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent))
            )
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
        // Endpoint dot.
        values.lastOrNull { it != null }?.let { last ->
            val idx = values.indexOfLast { it != null }
            val x = idx * step
            val y = size.height - ((last - minV) / span) * size.height * 0.86f - size.height * 0.07f
            drawCircle(color, radius = 2.4.dp.toPx(), center = Offset(x, y))
        }
    }
}

/** Horizontal mini bar chart for precipitation amounts. */
@Composable
fun PrecipBars(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = Atmos.Ice
) {
    if (values.isEmpty()) return
    val maxV = max(0.1f, values.max())
    Canvas(modifier = modifier) {
        val gap = 2.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val hh = (v / maxV) * size.height
            if (hh > 0.5f) {
                drawRoundRect(
                    color = color.copy(alpha = 0.35f + 0.55f * (v / maxV)),
                    topLeft = Offset(i * (barW + gap), size.height - hh),
                    size = androidx.compose.ui.geometry.Size(barW, hh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2, barW / 2)
                )
            }
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(0.7.dp)
            .background(Atmos.Hairline)
    )
}

@Composable
fun ValueUnit(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    valueSize: Int = 34
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            value,
            fontFamily = GeistMono,
            fontWeight = FontWeight.Light,
            fontSize = valueSize.sp,
            lineHeight = (valueSize + 4).sp,
            color = Atmos.White.copy(alpha = 0.93f),
            maxLines = 1,
            softWrap = false
        )
        if (!unit.isNullOrBlank()) {
            Text(
                unit,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = Atmos.Muted,
                modifier = Modifier.padding(bottom = 5.dp, start = 4.dp),
                maxLines = 1
            )
        }
    }
}

fun mix(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)

internal fun clamp01(v: Float) = min(1f, max(0f, v))
