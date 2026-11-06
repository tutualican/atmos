package com.alican.atmos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.Uncertainty
import com.alican.atmos.core.WeatherData
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.core.WeatherKindCodes
import com.alican.atmos.ui.AtmosUiState
import com.alican.atmos.ui.Fmt
import com.alican.atmos.ui.components.AtmosCard
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.components.Hairline
import com.alican.atmos.ui.components.Pill
import com.alican.atmos.ui.components.PrecipBars
import com.alican.atmos.ui.components.SectionHeader
import com.alican.atmos.ui.components.SegmentedOptions
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(
    state: AtmosUiState,
    onOpenLocation: () -> Unit,
    onOpenDetail: (com.alican.atmos.ui.DetailCard) -> Unit
) {
    val data = state.data
    var spanHours by remember { mutableStateOf(24) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 130.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Forecast", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = Atmos.White)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${state.place.name} · ${state.place.subtitle}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Atmos.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Pill(
                state.settings.model.label,
                icon = AtmosIcons.Layers,
                tint = Atmos.Secondary
            )
        }

        if (data == null) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (state.loading) "Loading forecast…" else "Forecast unavailable",
                    color = Atmos.Secondary
                )
            }
            return@Column
        }

        val now = data.current.time
        val hours = remember(data.fetchedAtMillis, spanHours) {
            data.hourly.filter { !it.time.isBefore(now.withMinute(0).withSecond(0).withNano(0)) }
                .take(spanHours.coerceAtMost(data.hourly.size))
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedOptions(
                options = listOf(24, 48),
                selected = spanHours,
                label = { "${it}h" },
                onSelect = { spanHours = it }
            )
            Spacer(Modifier.height(14.dp))

            TemperatureChartCard(data, hours, spanHours, state.settings)
            Spacer(Modifier.height(12.dp))
            PrecipChartCard(data, hours, state.settings)
            Spacer(Modifier.height(12.dp))
            UncertaintyCard(data, hours)
            Spacer(Modifier.height(26.dp))

            SectionHeader("Hour by hour", trailing = "Times are local")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp, horizontal = 16.dp)) {
                hours.take(spanHours).forEachIndexed { i, hour ->
                    HourRow(hour, data, state.settings)
                    if (i != hours.take(spanHours).lastIndex) Hairline()
                }
            }

            Spacer(Modifier.height(26.dp))
            SectionHeader("Daily outlook", trailing = "7 days")
            Spacer(Modifier.height(10.dp))
            data.daily.take(7).forEach { day ->
                DayCard(day, data, state.settings)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(14.dp))
            SunCard(data)
            Spacer(Modifier.height(20.dp))
            Text(
                "Forecast values are numerical model output. Probability is the chance of measurable precipitation, " +
                    "not an amount. Amounts are forecast totals for the stated period.",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
private fun TemperatureChartCard(data: WeatherData, hours: List<com.alican.atmos.core.HourPoint>, span: Int, settings: AtmosSettings) {
    val unit = settings.temperatureUnit
    val temps = hours.map { it.temperature }
    if (temps.size < 2) return
    val minT = temps.min()
    val maxT = temps.max()
    val band = data.uncertainty?.let { u ->
        val bandTimes = hours.map { it.time }
        val lo = bandTimes.map { t -> u.p10.getOrNull(u.times.indexOf(t)) }
        val hi = bandTimes.map { t -> u.p90.getOrNull(u.times.indexOf(t)) }
        if (lo.any { it != null } && hi.any { it != null }) lo to hi else null
    }
    AtmosCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Temperature", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Atmos.White)
                Text("Solid line: model median. Band: ensemble 10–90%", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Fmt.temp(minT, unit)}° – ${Fmt.temp(maxT, unit)}°", fontFamily = GeistMono, fontSize = 15.sp, color = Atmos.White)
                Text("next ${span}h", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(168.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val leftPad = 34.dp.toPx()
                val bottomPad = 22.dp.toPx()
                val topPad = 8.dp.toPx()
                val rightPad = 6.dp.toPx()
                val chartW = size.width - leftPad - rightPad
                val chartH = size.height - topPad - bottomPad
                val lo = min(minT, band?.first?.filterNotNull()?.minOrNull() ?: minT) - 1
                val hi = max(maxT, band?.second?.filterNotNull()?.maxOrNull() ?: maxT) + 1
                val range = (hi - lo).coerceAtLeast(1.0)
                fun xAt(i: Int) = leftPad + chartW * (i / (temps.size - 1f))
                fun yAt(v: Double) = topPad + chartH * (1f - ((v - lo) / range).toFloat())

                val gridPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(28, 255, 255, 255)
                    strokeWidth = 1f
                }
                repeat(4) { g ->
                    val y = topPad + chartH * (g / 3f)
                    drawLine(Color.White.copy(alpha = 0.06f), Offset(leftPad, y), Offset(leftPad + chartW, y), 1f)
                    drawContext.canvas.nativeCanvas.drawText(
                        "${Fmt.temp(hi - (range * g / 3.0), unit)}°",
                        2.dp.toPx(), y + 3.dp.toPx(),
                        gridPaint.apply {
                            textSize = 9.sp.toPx()
                            color = android.graphics.Color.argb(120, 166, 178, 196)
                        }
                    )
                }

                // Ensemble band, only where the model actually provides members.
                if (band != null) {
                    val path = Path()
                    var started = false
                    band.second.forEachIndexed { i, v ->
                        if (v == null) return@forEachIndexed
                        val x = xAt(i); val y = yAt(v)
                        if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                    }
                    for (i in band.first.indices.reversed()) {
                        val v = band.first[i] ?: continue
                        path.lineTo(xAt(i), yAt(v))
                    }
                    if (started) {
                        path.close()
                        drawPath(path, Brush.verticalGradient(listOf(Atmos.Ice.copy(alpha = 0.16f), Atmos.Ice.copy(alpha = 0.04f))))
                    }
                }

                val line = Path()
                temps.forEachIndexed { i, v ->
                    val x = xAt(i); val y = yAt(v)
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(xAt(temps.lastIndex), topPad + chartH)
                    lineTo(xAt(0), topPad + chartH)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(Atmos.Ice.copy(alpha = 0.18f), Color.Transparent)))
                drawPath(line, Atmos.Ice.copy(alpha = 0.95f), style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                temps.forEachIndexed { i, v ->
                    if (i % 6 == 0 || i == temps.lastIndex) {
                        drawCircle(Atmos.Ice, radius = 2.dp.toPx(), center = Offset(xAt(i), yAt(v)))
                    }
                }

                // Hour ticks.
                val tickPaint = android.graphics.Paint().apply {
                    textSize = 9.sp.toPx()
                    color = android.graphics.Color.argb(130, 116, 129, 152)
                    isAntiAlias = true
                }
                hours.indices.filter { it % 6 == 0 || it == hours.lastIndex }.forEach { i ->
                    drawContext.canvas.nativeCanvas.drawText(
                        Fmt.hourShort(hours[i].time) + ":00",
                        xAt(i) - 10.dp.toPx(),
                        size.height - 4.dp.toPx(),
                        tickPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun PrecipChartCard(data: WeatherData, hours: List<com.alican.atmos.core.HourPoint>, settings: AtmosSettings) {
    if (hours.size < 2) return
    val amounts = hours.map { it.precipitation.toFloat() }
    val probs = hours.map { (it.precipProbability ?: 0).toFloat() }
    val total = amounts.sum()
    AtmosCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Precipitation", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Atmos.White)
                Text("Amount bars · probability line", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${Fmt.precip(total.toDouble(), settings.precipitationUnit)} ${settings.precipitationUnit.symbol}",
                    fontFamily = GeistMono, fontSize = 15.sp, color = Atmos.White
                )
                Text("forecast total", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val bottomPad = 16.dp.toPx()
                val chartH = size.height - bottomPad
                val gap = 1.5.dp.toPx()
                val barW = (size.width - gap * (amounts.size - 1)) / amounts.size
                val maxAmount = max(0.4f, amounts.max())
                amounts.forEachIndexed { i, v ->
                    val hh = (v / maxAmount) * chartH
                    if (hh > 0.4f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(Atmos.Ice.copy(alpha = 0.75f), Atmos.Ice.copy(alpha = 0.22f))
                            ),
                            topLeft = Offset(i * (barW + gap), chartH - hh),
                            size = Size(barW, hh),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2, barW / 2)
                        )
                    }
                }
                val probLine = Path()
                probs.forEachIndexed { i, p ->
                    val x = i * (barW + gap) + barW / 2
                    val y = chartH - (p / 100f) * chartH
                    if (i == 0) probLine.moveTo(x, y) else probLine.lineTo(x, y)
                }
                drawPath(probLine, ActiveGold.copy(alpha = 0.9f), style = Stroke(width = 1.4.dp.toPx()))
                val tickPaint = android.graphics.Paint().apply {
                    textSize = 8.sp.toPx()
                    color = android.graphics.Color.argb(110, 116, 129, 152)
                    isAntiAlias = true
                }
                listOf(0, amounts.size / 2, amounts.lastIndex).forEach { i ->
                    val label = "${Fmt.hourShort(hours[i].time)}h"
                    drawContext.canvas.nativeCanvas.drawText(
                        label, (i * (barW + gap)).coerceIn(0f, size.width - 22.dp.toPx()),
                        size.height - 2.dp.toPx(), tickPaint
                    )
                }
            }
        }
    }
}

private val ActiveGold = Color(0xFFE3BE7C)

@Composable
private fun UncertaintyCard(data: WeatherData, hours: List<com.alican.atmos.core.HourPoint>) {
    val u = data.uncertainty ?: return
    val at24 = hours.getOrNull(min(23, hours.lastIndex))?.time
    val idx = at24?.let { t -> u.times.indexOf(t) } ?: -1
    val spread = if (idx >= 0) (u.p90.getOrNull(idx) ?: 0.0) - (u.p10.getOrNull(idx) ?: 0.0) else 0.0
    AtmosCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AtmosIcons.Layers, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Forecast uncertainty", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "ECMWF ensemble · ${u.members} members. Temperature spread at +24 h is " +
                (if (spread > 0) "±${String.format(java.util.Locale.US, "%.1f", spread / 2)}°C (10–90%)." else "unavailable right now."),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "A narrow band means the models agree. A wide band means plan with margin.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
    }
}

@Composable
private fun HourRow(hour: com.alican.atmos.core.HourPoint, data: WeatherData, settings: AtmosSettings) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (hour == data.hourly.firstOrNull { it.time == hour.time }) Fmt.hour(hour.time) else Fmt.hour(hour.time),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary,
            maxLines = 1,
            modifier = Modifier.width(50.dp)
        )
        Icon(
            Fmt.kindIcon(WeatherKind.fromCode(hour.weatherCode), hour.isDay),
            contentDescription = WeatherKindCodes.label(hour.weatherCode, hour.isDay),
            tint = Atmos.Secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            Fmt.shortCondition(hour.weatherCode),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(52.dp)) {
            Text("${Fmt.temp(hour.temperature, settings.temperatureUnit)}°", fontFamily = GeistMono, fontSize = 14.sp, color = Atmos.White, maxLines = 1)
            Text("${Fmt.temp(hour.apparent, settings.temperatureUnit)}° feels", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(62.dp)) {
            Text("${Fmt.precip(hour.precipitation, settings.precipitationUnit)} ${settings.precipitationUnit.symbol}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = if (hour.precipitation > 0.01) Atmos.Ice else Atmos.Muted, maxLines = 1)
            Text("${hour.precipProbability ?: 0}% chance", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun DayCard(day: com.alican.atmos.core.DayPoint, data: WeatherData, settings: AtmosSettings) {
    val unit = settings.temperatureUnit
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(74.dp)) {
                Text(
                    Fmt.dayMonth(day.date.atTime(12, 0)),
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.White,
                    maxLines = 1
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    WeatherKindCodes.label(day.weatherCode),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Fmt.kindIcon(WeatherKind.fromCode(day.weatherCode), true),
                contentDescription = null,
                tint = Atmos.Secondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${Fmt.temp(day.tempMax, unit)}°", fontFamily = GeistMono, fontSize = 18.sp, color = Atmos.White, maxLines = 1)
                    Text(" / ${Fmt.temp(day.tempMin, unit)}°", fontFamily = GeistMono, fontSize = 13.sp, color = Atmos.Muted, maxLines = 1)
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "Feels ${Fmt.temp(day.apparentMax ?: day.tempMax, unit)}° · UV ${day.uvMax?.let { String.format(java.util.Locale.US, "%.0f", it) } ?: "—"}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(74.dp)) {
                Text(
                    "${Fmt.precip(day.precipitationSum, settings.precipitationUnit)} ${settings.precipitationUnit.symbol}",
                    fontFamily = GeistMono, fontSize = 12.sp,
                    color = if (day.precipitationSum > 0.05) Atmos.Ice else Atmos.Muted,
                    maxLines = 1
                )
                Text(
                    "${day.precipProbabilityMax ?: 0}% chance",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 1
                )
            }
        }
        Row(Modifier.padding(top = 10.dp)) {
            if (day.windMax != null) {
                MiniStat("Wind", "${Fmt.wind(day.windMax, settings.windUnit)} ${settings.windUnit.symbol} ${Fmt.compass(day.windDirection ?: 0)}", Modifier.weight(1f))
            }
            if (day.gustMax != null) {
                MiniStat("Gusts", "${Fmt.wind(day.gustMax, settings.windUnit)} ${settings.windUnit.symbol}", Modifier.weight(1f))
            }
            if (day.sunrise != null) {
                MiniStat("Sun", "${Fmt.clock(day.sunrise)}–${day.sunset?.let { Fmt.clock(it) } ?: "—"}", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(end = 6.dp)) {
        Text(label.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1)
        Text(value, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Atmos.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SunCard(data: WeatherData) {
    val today = data.daily.firstOrNull() ?: return
    val rise = today.sunrise ?: return
    val set = today.sunset ?: return
    val total = java.time.Duration.between(rise, set).toMinutes().coerceAtLeast(1)
    val now = data.current.time
    val progress = (java.time.Duration.between(rise, now).toMinutes().toFloat() / total).coerceIn(0f, 1f)
    AtmosCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AtmosIcons.Sunrise, contentDescription = null, tint = Atmos.Gold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Daylight", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White, modifier = Modifier.weight(1f))
            Text(
                "${total / 60}h ${total % 60}m",
                fontFamily = GeistMono, fontSize = 13.sp, color = Atmos.Secondary
            )
        }
        Spacer(Modifier.height(12.dp))
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            drawRoundRect(color = Color.White.copy(alpha = 0.07f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Atmos.Gold.copy(alpha = 0.55f), Atmos.Gold)),
                size = Size(size.width * progress, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text("↑ ${Fmt.clock(rise)}", fontFamily = GeistMono, fontSize = 12.sp, color = Atmos.Secondary)
            Spacer(Modifier.weight(1f))
            Text("↓ ${Fmt.clock(set)}", fontFamily = GeistMono, fontSize = 12.sp, color = Atmos.Secondary)
        }
    }
}
