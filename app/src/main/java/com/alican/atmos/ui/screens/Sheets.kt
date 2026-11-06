package com.alican.atmos.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import com.alican.atmos.i18n.Text
import com.alican.atmos.i18n.Loc
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.core.AirQuality
import com.alican.atmos.core.Place
import com.alican.atmos.core.WeatherData
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.core.WeatherKindCodes
import com.alican.atmos.data.OfficialAlert
import com.alican.atmos.ui.AtmosUiState
import com.alican.atmos.ui.DetailCard
import com.alican.atmos.ui.Fmt
import com.alican.atmos.ui.components.AtmosCard
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.components.Hairline
import com.alican.atmos.ui.components.IconButtonSoft
import com.alican.atmos.ui.components.Pill
import com.alican.atmos.ui.components.PrecipBars
import com.alican.atmos.ui.components.Sparkline
import com.alican.atmos.ui.components.ValueUnit
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import java.time.Duration
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailSheet(
    card: DetailCard,
    state: AtmosUiState,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Atmos.DeepNavy,
        contentColor = Atmos.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)))
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            when (card) {
                DetailCard.UV -> UvDetail(state)
                DetailCard.WIND -> WindDetail(state)
                DetailCard.HUMIDITY -> HumidityDetail(state)
                DetailCard.PRESSURE -> PressureDetail(state)
                DetailCard.VISIBILITY -> VisibilityDetail(state)
                DetailCard.AIR -> AirDetail(state)
                DetailCard.PRECIP -> PrecipDetail(state)
                DetailCard.SUN -> SunDetail(state)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SheetTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Atmos.Secondary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = Atmos.Muted)
            if (subtitle != null) {
                Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Atmos.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun NoData(message: String = "This field is unavailable for the current location. ATMOS leaves it empty rather than estimating.") {
    Text(message, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Atmos.Secondary)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun UvDetail(state: AtmosUiState) {
    val data = state.data
    val uv = data?.hourly?.firstOrNull()?.uvIndex
    SheetTitle(AtmosIcons.Sun, "UV index", "Model-derived erythermal UV")
    if (data == null || uv == null) {
        NoData()
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(String.format(Locale.US, "%.1f", uv), fontFamily = GeistMono, fontSize = 54.sp, fontWeight = FontWeight.Light, color = Atmos.White)
        Spacer(Modifier.width(10.dp))
        Text(Fmt.uvLabel(uv), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Fmt.uvColor(uv), modifier = Modifier.padding(bottom = 9.dp))
    }
    Spacer(Modifier.height(16.dp))
    val next = data.hourly.take(14)
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("NEXT 14 HOURS", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(8.dp))
        Sparkline(next.map { it.uvIndex?.toFloat() }, Modifier.fillMaxWidth().height(64.dp), color = Atmos.Gold)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(Fmt.hour(next.first().time), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            Spacer(Modifier.weight(1f))
            Text(Fmt.hour(next.last().time), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            uvAdvice(uv),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Peak today: ${data.daily.firstOrNull()?.uvMax?.let { String.format(Locale.US, "%.1f", it) } ?: "—"}. " +
            "UV is forecast for cloud cover and sun angle; reflections and altitude can raise real exposure.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
}

private fun uvAdvice(uv: Double): String = when {
    uv < 3 -> "Low exposure risk. No protection needed for most people."
    uv < 6 -> "Moderate. Shade around midday; sunscreen for extended outdoor time."
    uv < 8 -> "High. Reduce midday exposure, cover up, use SPF 30+."
    uv < 11 -> "Very high. Avoid direct sun between late morning and afternoon."
    else -> "Extreme. Unprotected skin can burn within minutes."
}

@Composable
private fun WindDetail(state: AtmosUiState) {
    val data = state.data ?: run { SheetTitle(AtmosIcons.Wind, "Wind"); NoData(); return }
    val s = state.settings
    val c = data.current
    SheetTitle(AtmosIcons.Wind, "Wind", "10 m above ground · model level")
    Row(verticalAlignment = Alignment.CenterVertically) {
        CompassDial(c.windDirection, Modifier.size(136.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            ValueUnit(Fmt.wind(c.windSpeed, s.windUnit), s.windUnit.symbol, valueSize = 40)
            Text(Fmt.windDescription(c.windSpeed), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Atmos.Secondary)
            Spacer(Modifier.height(10.dp))
            Text("From ${Fmt.compass(c.windDirection)} · ${c.windDirection}°", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            Spacer(Modifier.height(4.dp))
            Text("Gusts ${Fmt.wind(c.windGusts, s.windUnit)} ${s.windUnit.symbol}", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        }
    }
    Spacer(Modifier.height(16.dp))
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("WIND SPEED · NEXT 12 H", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(8.dp))
        val next = data.hourly.take(12)
        Sparkline(next.map { it.windSpeed.toFloat() }, Modifier.fillMaxWidth().height(56.dp), color = Atmos.Ice)
        Spacer(Modifier.height(6.dp))
        Row {
            Text(Fmt.hour(next.first().time), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
            Spacer(Modifier.weight(1f))
            Text(Fmt.hour(next.last().time), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Direction is where the wind comes from. Gusts are short-duration peaks and can exceed the sustained value shown above.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
}

@Composable
private fun CompassDial(direction: Int, modifier: Modifier = Modifier) {
    val rot = direction.toFloat()
    Canvas(modifier) {
        val r = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawCircle(Color.White.copy(alpha = 0.04f), radius = r, center = center)
        drawCircle(Color.White.copy(alpha = 0.12f), radius = r, center = center, style = Stroke(1f))
        drawCircle(Color.White.copy(alpha = 0.06f), radius = r * 0.72f, center = center, style = Stroke(1f))
        repeat(36) { i ->
            val angle = Math.toRadians(i * 10.0 - 90)
            val long = i % 9 == 0
            val inner = r * if (long) 0.82f else 0.90f
            val start = Offset(center.x + (inner * Math.cos(angle)).toFloat(), center.y + (inner * Math.sin(angle)).toFloat())
            val end = Offset(center.x + (r * 0.96f * Math.cos(angle)).toFloat(), center.y + (r * 0.96f * Math.sin(angle)).toFloat())
            drawLine(
                color = if (long) Atmos.Secondary else Color.White.copy(alpha = 0.18f),
                start = start, end = end,
                strokeWidth = if (long) 1.6.dp.toPx() else 1.dp.toPx()
            )
        }
        val labels = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            color = android.graphics.Color.argb(190, 166, 178, 196)
        }
        labels.forEach { (label, deg) ->
            val rad = Math.toRadians(deg - 90.0)
            drawContext.canvas.nativeCanvas.drawText(
                label,
                center.x + (r * 0.64f * Math.cos(rad)).toFloat(),
                center.y + (r * 0.64f * Math.sin(rad)).toFloat() + paint.textSize / 3,
                paint
            )
        }
        rotate(rot) {
            val tip = Offset(center.x, center.y - r * 0.58f)
            val path = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(center.x - r * 0.13f, center.y + r * 0.10f)
                lineTo(center.x, center.y + r * 0.02f)
                lineTo(center.x + r * 0.13f, center.y + r * 0.10f)
                close()
            }
            drawPath(path, brush = Brush.verticalGradient(listOf(Atmos.Ice, Atmos.Ice.copy(alpha = 0.35f))))
        }
        drawCircle(Atmos.Midnight, radius = r * 0.07f, center = center)
        drawCircle(Atmos.Ice, radius = r * 0.045f, center = center)
    }
}

@Composable
private fun HumidityDetail(state: AtmosUiState) {
    val data = state.data ?: run { SheetTitle(AtmosIcons.Droplet, "Humidity"); NoData(); return }
    val c = data.current
    val hour = data.hourly.firstOrNull()
    SheetTitle(AtmosIcons.Droplet, "Humidity", "Relative humidity & dew point")
    ValueUnit("${c.humidity}", "%", valueSize = 50)
    Spacer(Modifier.height(2.dp))
    Text(Fmt.humidityLabel(c.humidity), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Atmos.Ice)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniMetric("Dew point", hour?.dewPoint?.let { Fmt.tempWithUnit(it, state.settings.temperatureUnit) } ?: "—", Modifier.weight(1f))
        MiniMetric("Comfort", comfortLabel(c.temperature, c.humidity), Modifier.weight(1f))
    }
    Spacer(Modifier.height(16.dp))
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("RELATIVE HUMIDITY · NEXT 12 H", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(8.dp))
        val next = data.hourly.take(12)
        Sparkline(next.map { it.humidity.toFloat() }, Modifier.fillMaxWidth().height(56.dp), color = Atmos.Ice)
        Spacer(Modifier.height(8.dp))
        Text(
            "Relative humidity is temperature-dependent; the dew point is the better measure of absolute moisture.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
    }
}

private fun comfortLabel(temp: Double, humidity: Int): String {
    val humidex = temp + 0.5555 * (6.11 * Math.exp(5417.7530 * (1 / 273.16 - 1 / (temp + 273.15))) - 10)
    return when {
        temp < 10 -> "Cool air"
        humidity < 30 -> "Dry air"
        humidex > 40 -> "Oppressive"
        humidex > 34 -> "Muggy"
        humidity > 75 -> "Humid"
        else -> "Comfortable"
    }
}

@Composable
private fun PressureDetail(state: AtmosUiState) {
    val data = state.data ?: run { SheetTitle(AtmosIcons.Gauge, "Pressure"); NoData(); return }
    val s = state.settings
    val current = Fmt.pressure(data.current.pressureMsl, s.pressureUnit)
    SheetTitle(AtmosIcons.Gauge, "Pressure", "Mean sea-level pressure")
    Row(verticalAlignment = Alignment.Bottom) {
        ValueUnit(current ?: "—", s.pressureUnit.symbol, valueSize = 50)
        Spacer(Modifier.width(14.dp))
        Text(Fmt.pressureTrend(data.hourly), style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.Gold, modifier = Modifier.padding(bottom = 8.dp))
    }
    Spacer(Modifier.height(16.dp))
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("MSL PRESSURE · NEXT 12 H", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(8.dp))
        val next = data.hourly.take(12)
        Sparkline(next.map { it.pressureMsl?.toFloat() }, Modifier.fillMaxWidth().height(56.dp), color = Atmos.Gold)
        Spacer(Modifier.height(8.dp))
        Text(
            if (Fmt.pressureTrend(data.hourly) == "Falling") "Falling pressure often precedes unsettled weather."
            else if (Fmt.pressureTrend(data.hourly) == "Rising") "Rising pressure usually supports clearing conditions."
            else "Steady pressure suggests little short-term change.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
    }
}

@Composable
private fun VisibilityDetail(state: AtmosUiState) {
    val data = state.data ?: run { SheetTitle(AtmosIcons.Eye, "Visibility"); NoData(); return }
    val vis = data.hourly.firstOrNull()?.visibility
    val imperial = Fmt.isImperial(state.settings)
    SheetTitle(AtmosIcons.Eye, "Visibility", "Model visibility at the surface")
    if (vis == null) {
        NoData()
        return
    }
    ValueUnit(Fmt.visibility(vis, imperial)?.substringBefore(' ') ?: "—", if (imperial) "mi" else "km", valueSize = 50)
    Spacer(Modifier.height(16.dp))
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("VISIBILITY · NEXT 12 H", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(8.dp))
        val next = data.hourly.take(12)
        Sparkline(next.map { it.visibility?.div(1000.0)?.toFloat() }, Modifier.fillMaxWidth().height(56.dp), color = Atmos.Secondary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Visibility is strongly affected by fog, heavy rain and blowing snow. Local terrain can differ from the grid value.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
    }
}

@Composable
private fun AirDetail(state: AtmosUiState) {
    val air: AirQuality? = state.data?.air
    SheetTitle(AtmosIcons.Leaf, "Air quality", air?.time?.let { "CAMS · ${Fmt.clock(it)}" })
    if (air?.europeanAqi == null) {
        NoData("Air quality is not published for this location by the European CAMS service.")
        return
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text("${air.europeanAqi}", fontFamily = GeistMono, fontSize = 54.sp, fontWeight = FontWeight.Light, color = Fmt.aqiColor(air.europeanAqi))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 8.dp)) {
            Text(Fmt.aqiLabel(air.europeanAqi), style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = Atmos.White)
            Text("European AQI", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        }
    }
    Spacer(Modifier.height(16.dp))
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        PollutantRow("PM2.5", air.pm25, 15.0, "µg/m³")
        PollutantRow("PM10", air.pm10, 45.0, "µg/m³")
        PollutantRow("Ozone", air.ozone, 100.0, "µg/m³")
        PollutantRow("NO₂", air.no2, 25.0, "µg/m³")
        PollutantRow("SO₂", air.so2, 40.0, "µg/m³")
        PollutantRow("CO", air.co, 4000.0, "µg/m³")
        Spacer(Modifier.height(4.dp))
        Text(
            "Bars show concentration relative to the WHO 24-hour guideline value. US AQI: ${air.usAqi ?: "—"}.",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Air quality comes from CAMS model analysis and forecast, not a local sensor. Street-level concentrations can differ.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
}

@Composable
private fun PollutantRow(name: String, value: Double?, guideline: Double, unit: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = Atmos.Secondary, modifier = Modifier.width(44.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.07f))) {
                val frac = ((value ?: 0.0) / (guideline * 2)).toFloat().coerceIn(0f, 1f)
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                frac < 0.5f -> Atmos.Good
                                frac < 0.85f -> Atmos.Gold
                                else -> Atmos.Danger
                            }
                        )
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            value?.let { String.format(Locale.US, if (it < 10) "%.1f" else "%.0f", it) } ?: "—",
            fontFamily = GeistMono, fontSize = 12.sp, color = Atmos.White,
            modifier = Modifier.width(44.dp),
            maxLines = 1
        )
        Text(unit, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, modifier = Modifier.width(48.dp), maxLines = 1)
    }
}

@Composable
private fun PrecipDetail(state: AtmosUiState) {
    val data = state.data ?: run { SheetTitle(AtmosIcons.CloudRain, "Precipitation"); NoData(); return }
    val s = state.settings
    val minutes = data.minutely
    val nextHour = minutes.take(4).sumOf { it.precipitation }
    SheetTitle(AtmosIcons.CloudRain, "Precipitation", "15-minute nowcast · ${data.modelLabel}")
    Row(verticalAlignment = Alignment.Bottom) {
        ValueUnit(Fmt.precip(nextHour, s.precipitationUnit), s.precipitationUnit.symbol, valueSize = 44)
        Spacer(Modifier.width(12.dp))
        Text(
            "next 60 min",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Forecast amount — this is not a probability.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
    Spacer(Modifier.height(16.dp))
    if (minutes.isEmpty()) {
        NoData("The 15-minute precipitation detail is unavailable for this location.")
        return
    }
    AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Text("NEXT 2 HOURS · 15-MIN STEPS", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        Spacer(Modifier.height(3.dp))
        Text(
            "Percentage = chance of rain. Bars = forecast amount.",
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary
        )
        Spacer(Modifier.height(10.dp))
        PrecipBars(minutes.take(8).map { it.precipitation.toFloat() }, Modifier.fillMaxWidth().height(52.dp), color = Atmos.Ice)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            minutes.take(8).forEachIndexed { i, m ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        Fmt.hourShort(m.time) + ":" + m.time.minute.toString().padStart(2, '0'),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        maxLines = 1,
                        fontSize = (10 * Atmos.textScale).sp
                    )
                    Text(
                        "${m.probability ?: 0}%",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = if ((m.probability ?: 0) > 40) Atmos.Ice else Atmos.Secondary,
                        maxLines = 1,
                        fontSize = (10 * Atmos.textScale).sp
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniMetric(
            "Next 24 h",
            "${Fmt.precip(data.hourly.take(24).sumOf { it.precipitation }, s.precipitationUnit)} ${s.precipitationUnit.symbol}",
            Modifier.weight(1f)
        )
        MiniMetric(
            "Peak chance",
            "${data.hourly.take(24).mapNotNull { it.precipProbability }.maxOrNull() ?: 0}%",
            Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))
    val storm = data.hourly.take(24).firstOrNull { it.weatherCode >= 95 }
    if (storm != null) {
        AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Text("THUNDERSTORM SIGNAL", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Danger)
            Spacer(Modifier.height(4.dp))
            Text(
                "A thunderstorm code appears around ${Fmt.hour(storm.time)}. This is a model forecast signal, not an official warning and not a lightning-strike observation.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Atmos.Secondary
            )
        }
        Spacer(Modifier.height(10.dp))
    }
    Text(
        "Probability answers \"will it rain?\"; the amount answers \"how much?\". ATMOS keeps them separate everywhere.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
}

@Composable
private fun SunDetail(state: AtmosUiState) {
    val day = state.data?.daily?.firstOrNull()
    SheetTitle(AtmosIcons.Sunrise, "Sun", "Sunrise, sunset and daylight")
    if (day?.sunrise == null || day.sunset == null) {
        NoData("Sun times are unavailable for this location and date.")
        return
    }
    val total = Duration.between(day.sunrise, day.sunset).toMinutes().coerceAtLeast(1)
    val now = state.data?.current?.time
    val progress = if (now != null) (Duration.between(day.sunrise, now).toMinutes().toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(verticalAlignment = Alignment.Bottom) {
        Text("${total / 60}h ${total % 60}m", fontFamily = GeistMono, fontSize = 48.sp, fontWeight = FontWeight.Light, color = Atmos.White)
        Spacer(Modifier.width(10.dp))
        Text("daylight", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Atmos.Muted, modifier = Modifier.padding(bottom = 9.dp))
    }
    Spacer(Modifier.height(18.dp))
    Box(Modifier.fillMaxWidth().height(28.dp)) {
        Canvas(Modifier.fillMaxWidth().height(6.dp).align(Alignment.Center)) {
            drawRoundRect(Color.White.copy(alpha = 0.07f), size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            drawRoundRect(
                Brush.horizontalGradient(listOf(Atmos.Gold.copy(alpha = 0.4f), Atmos.Gold)),
                size = Size(size.width * progress, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(progress.coerceIn(0.02f, 0.98f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(Atmos.Gold))
        }
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniMetric("Sunrise", Fmt.clock(day.sunrise), Modifier.weight(1f))
        MiniMetric("Sunset", Fmt.clock(day.sunset), Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Times are local astronomical values for the selected coordinates. Terrain and elevation can shift the visible horizon.",
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = Atmos.Muted
    )
}

@Composable
private fun MiniMetric(label: String, value: String, modifier: Modifier = Modifier) {
    AtmosCard(modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label.uppercase(), style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, fontFamily = GeistMono, fontSize = 18.sp, color = Atmos.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSheet(
    state: AtmosUiState,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onSelect: (Place) -> Unit,
    onUseLocation: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf(state.searchQuery) }
    val focus = LocalFocusManager.current
    LaunchedEffect(Unit) { query = "" }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Atmos.DeepNavy,
        contentColor = Atmos.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)))
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding()) {
            Text("Choose a location", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = Atmos.White)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AtmosIcons.Search, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = Atmos.White),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Atmos.Ice),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                "Search any city",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = Atmos.Muted
                            )
                        }
                        inner()
                    }
                )
                if (state.searching) {
                    Text("…", color = Atmos.Muted)
                } else if (query.isNotEmpty()) {
                    Icon(
                        AtmosIcons.Close,
                        contentDescription = "Clear",
                        tint = Atmos.Muted,
                        modifier = Modifier.size(16.dp).clickable {
                            query = ""
                            onSearch("")
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Atmos.Ice.copy(alpha = 0.08f))
                    .clickable { onUseLocation() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(AtmosIcons.Crosshair, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.locating) "Locating…" else "Use my location",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.White
                    )
                    Text(
                        "Area-level coordinates, never stored as a track",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        maxLines = 1
                    )
                }
                Icon(AtmosIcons.ChevronRight, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(14.dp))

            if (state.searchError != null) {
                Text(state.searchError, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = Atmos.Danger)
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                if (state.searchResults.isNotEmpty()) {
                    item {
                        Text(
                            "SEARCH RESULTS",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Atmos.Muted,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(state.searchResults, key = { it.id }) { place ->
                        PlaceRow(place) { onSelect(place) }
                    }
                } else {
                    if (state.savedPlaces.isNotEmpty()) {
                        item {
                            Text(
                                "SAVED",
                                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                color = Atmos.Muted,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        items(state.savedPlaces, key = { it.id }) { place ->
                            PlaceRow(place) { onSelect(place) }
                        }
                    }
                    if (state.savedPlaces.isEmpty() && state.searchQuery.length < 2) {
                        item {
                            Text(
                                "Search for a city above, or use your device location. ATMOS saves only the places you choose.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = Atmos.Secondary,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Geocoding by Open-Meteo. Coordinates are rounded by the provider before conversion to a forecast grid.",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PlaceRow(place: Place, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (place.isCurrentLocation) AtmosIcons.Crosshair else AtmosIcons.Location,
            contentDescription = null,
            tint = Atmos.Secondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (place.subtitle.isNotBlank()) {
                Text(place.subtitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            String.format(Locale.US, "%.2f, %.2f", place.lat, place.lon),
            fontFamily = GeistMono,
            fontSize = 10.sp,
            color = Atmos.Muted.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialAlertSheet(alert: OfficialAlert, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Atmos.DeepNavy,
        contentColor = Atmos.White,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.width(34.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)))
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Atmos.Danger.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AtmosIcons.Warning, contentDescription = null, tint = Atmos.Danger, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        Loc.tr("Official warning").uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = Atmos.Danger
                    )
                    Text(
                        "${alert.source} · ${Loc.tr(alert.severity)}",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Pill(Loc.tr("Issued by authority"), tint = Atmos.Danger)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                alert.event,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = Atmos.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                alert.headline,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Atmos.Secondary
            )
            Spacer(Modifier.height(16.dp))
            Hairline()
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                AlertMeta(Loc.tr("Severity"), Loc.tr(alert.severity))
                AlertMeta(Loc.tr("Urgency"), Loc.tr(alert.urgency))
                if (alert.area.isNotBlank()) AlertMeta(Loc.tr("Area"), alert.area, Modifier.weight(1f))
            }
            if (alert.onset != null || alert.ends != null) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (alert.onset != null) AlertMeta(Loc.tr("From"), Fmt.clock(alert.onset))
                    if (alert.ends != null) AlertMeta(Loc.tr("Until"), Fmt.clock(alert.ends))
                }
            }

            if (alert.description.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    alert.description,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Atmos.Secondary
                )
            }
            if (!alert.instruction.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                AtmosCard(Modifier.fillMaxWidth()) {
                    Text(
                        Loc.tr("What to do").uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Gold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        alert.instruction,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = Atmos.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                Loc.t("Verbally unaltered official text from {}. ATMOS does not paraphrase warnings.", alert.sender),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted
            )
            if (!alert.url.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Pill(Loc.tr("Open official notice"), icon = AtmosIcons.External, tint = Atmos.Ice, onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(alert.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                })
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun AlertMeta(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label.uppercase(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted
        )
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = Atmos.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
