package com.alican.atmos.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import com.alican.atmos.i18n.Loc
import com.alican.atmos.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.core.DayPoint
import com.alican.atmos.core.WeatherData
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.core.WeatherKindCodes
import com.alican.atmos.ui.AtmosUiState
import com.alican.atmos.ui.DetailCard
import com.alican.atmos.ui.Fmt
import com.alican.atmos.ui.components.AtmosCard
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.components.Hairline
import com.alican.atmos.ui.components.IconButtonSoft
import com.alican.atmos.ui.components.Pill
import com.alican.atmos.ui.components.SectionHeader
import com.alican.atmos.ui.components.Sparkline
import com.alican.atmos.ui.components.StatCard
import com.alican.atmos.ui.components.ValueUnit
import com.alican.atmos.ui.scene.WeatherScene
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    state: AtmosUiState,
    onOpenLocation: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (DetailCard) -> Unit,
    onOpenSettings: () -> Unit,
    onLightning: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenMap: () -> Unit
) {
    val data = state.data
    val kind = data?.kind ?: WeatherKind.OVERCAST
    val isDay = data?.current?.isDay ?: true
    val settings = state.settings

    Box(Modifier.fillMaxSize()) {
        WeatherScene(
            kind = kind,
            isDay = isDay,
            precipitationMm = data?.current?.precipitation ?: 0.0,
            precipitationProbability = data?.current?.let { c ->
                data.hourly.firstOrNull { it.time.hour == c.time.hour || abs(it.time.hour - c.time.hour) <= 1 }
                    ?.precipProbability
            },
            windMs = data?.current?.windSpeed ?: 0.0,
            quality = settings.animationQuality,
            reduceMotion = settings.reduceMotion,
            onLightningFlash = onLightning,
            modifier = Modifier.fillMaxSize()
        )

        // Grounding scrim keeps typography legible over the scene without flattening it.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.18f to Color.Transparent,
                        0.36f to Atmos.Midnight.copy(alpha = 0.30f),
                        0.52f to Atmos.Midnight.copy(alpha = 0.88f),
                        0.62f to Atmos.Midnight,
                        1f to Atmos.Midnight
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 132.dp)
        ) {
            Header(state, onOpenLocation, onRefresh, onOpenSettings)

            Spacer(Modifier.height(8.dp))

            if (data == null) {
                HeroSkeleton(state, onRefresh)
            } else {
                Hero(data, state.settings, onRefresh)
                Spacer(Modifier.height(18.dp))
                ForecastHazardBanner(data, onOpenDetail)
                Spacer(Modifier.height(14.dp))
                FocusGrid(state, data, onOpenDetail)
                Spacer(Modifier.height(26.dp))
                HourlySection(data, state.settings, onOpenForecast)
                Spacer(Modifier.height(26.dp))
                DailySection(data, state.settings, onOpenForecast)
                Spacer(Modifier.height(26.dp))
                DataFootnote(data, state, onOpenSettings)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(
    state: AtmosUiState,
    onOpenLocation: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val rotating = state.refreshing || state.loading
    val transition = rememberInfiniteTransition(label = "refresh")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenLocation() }
                    .padding(vertical = 2.dp, horizontal = 2.dp)
            ) {
                Icon(AtmosIcons.Location, contentDescription = "Change location", tint = Atmos.Secondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    state.place.name,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = Atmos.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(3.dp))
                Icon(AtmosIcons.ChevronDown, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                buildString {
                    if (state.fromCache) append(Loc.tr("Cached")).append(" · ")
                    append(Fmt.relativeUpdate(state.data?.fetchedAtMillis ?: System.currentTimeMillis()))
                    state.data?.let { append(" · ${it.modelLabel}") }
                },
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 3.dp)
            )
        }
        IconButtonSoft(
            icon = AtmosIcons.Refresh,
            contentDescription = "Refresh weather",
            modifier = Modifier.rotate(if (rotating) angle else 0f),
            onClick = onRefresh
        )
        Spacer(Modifier.width(9.dp))
        IconButtonSoft(
            icon = AtmosIcons.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun Hero(data: WeatherData, settings: com.alican.atmos.core.AtmosSettings, onRefresh: () -> Unit) {
    val current = data.current
    val unit = settings.temperatureUnit
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 168.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                Fmt.temp(current.temperature, unit),
                fontFamily = GeistMono,
                fontWeight = FontWeight.Light,
                fontSize = 96.sp,
                lineHeight = 96.sp,
                letterSpacing = (-5).sp,
                color = Atmos.White.copy(alpha = 0.87f),
                maxLines = 1,
                softWrap = false
            )
            Text(
                "°",
                fontFamily = GeistMono,
                fontWeight = FontWeight.Light,
                fontSize = 44.sp,
                color = Atmos.White.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 22.dp, start = 2.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.padding(bottom = 18.dp).weight(1f)) {
                Text(
                    WeatherKindCodes.label(current.weatherCode, current.isDay),
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    color = Atmos.White.copy(alpha = 0.94f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "Feels like ${Fmt.tempWithUnit(current.apparent, unit)}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Atmos.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Humidity ${current.humidity}%", icon = AtmosIcons.Droplet, tint = Atmos.Ice, modifier = Modifier.weight(1f, fill = false))
            Pill(
                "${Fmt.wind(current.windSpeed, settings.windUnit)} ${settings.windUnit.symbol} · ${Fmt.compass(current.windDirection)}",
                icon = AtmosIcons.Wind,
                tint = Atmos.Secondary,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(4.dp).clip(RoundedCornerShape(50))
                    .background(if (data.fetchedAtMillis > 0) Atmos.Good.copy(alpha = 0.75f) else Atmos.Muted)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "Model analysis · updated ${Fmt.relativeUpdate(data.fetchedAtMillis)}",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroSkeleton(state: AtmosUiState, onRefresh: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 190.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (state.loading) {
            Text(
                "—",
                fontFamily = GeistMono,
                fontWeight = FontWeight.Light,
                fontSize = 96.sp,
                lineHeight = 96.sp,
                color = Atmos.White.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Loading conditions for ${state.place.name}…",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Atmos.Secondary
            )
        } else {
            Text(
                "No data available",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = Atmos.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                state.error ?: "The weather service could not be reached. Nothing is being invented to fill the gap.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = Atmos.Secondary
            )
            Spacer(Modifier.height(14.dp))
            Pill(
                "Try again",
                icon = AtmosIcons.Refresh,
                tint = Atmos.Ice,
                background = Color.White.copy(alpha = 0.08f),
                onClick = onRefresh
            )
        }
    }
}

@Composable
private fun ForecastHazardBanner(data: WeatherData, onOpenDetail: (DetailCard) -> Unit) {
    val severe = remember(data.fetchedAtMillis) {
        data.hourly.take(24).firstOrNull {
            it.weatherCode >= 95 || it.weatherCode == 65 || it.weatherCode == 82 || (it.windGusts > 20.0)
        }
    } ?: return
    val isThunder = severe.weatherCode >= 95
    AtmosCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        onClick = { onOpenDetail(DetailCard.PRECIP) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(11.dp))
                    .background(Atmos.Danger.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(AtmosIcons.Warning, contentDescription = null, tint = Atmos.Danger, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isThunder) "Thunderstorm signal · ${Fmt.hour(severe.time)}" else "Wind / rain signal · ${Fmt.hour(severe.time)}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Forecast signal, not an official warning. Check national alerts.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Atmos.Secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(AtmosIcons.ChevronRight, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun FocusGrid(state: AtmosUiState, data: WeatherData, onOpenDetail: (DetailCard) -> Unit) {
    val s = state.settings
    val current = data.current
    val imperial = Fmt.isImperial(s)
    val air = data.air
    val uv = data.hourly.firstOrNull()?.uvIndex
    val vis = data.hourly.firstOrNull()?.visibility
    val pressure = Fmt.pressure(current.pressureMsl, s.pressureUnit)
    val nowcast = data.minutely.take(4)
    val nextHourMm = nowcast.sumOf { it.precipitation }

    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Right now", trailing = "Tap any card for detail")
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "UV index",
                icon = AtmosIcons.Sun,
                value = uv?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                unit = "",
                caption = if (uv == null) "Unavailable" else Fmt.uvLabel(uv),
                accent = if (uv == null) Atmos.Muted else Fmt.uvColor(uv),
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.UV) }
            )
            StatCard(
                title = "Wind",
                icon = AtmosIcons.Wind,
                value = Fmt.wind(current.windSpeed, s.windUnit),
                unit = s.windUnit.symbol,
                caption = "${Fmt.compass(current.windDirection)} · gusts ${Fmt.wind(current.windGusts, s.windUnit)}",
                accent = Atmos.Ice,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.WIND) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Humidity",
                icon = AtmosIcons.Droplet,
                value = "${current.humidity}",
                unit = "%",
                caption = Fmt.humidityLabel(current.humidity),
                accent = Atmos.Ice,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.HUMIDITY) }
            )
            StatCard(
                title = "Pressure",
                icon = AtmosIcons.Gauge,
                value = pressure ?: "—",
                unit = if (pressure == null) "" else s.pressureUnit.symbol,
                caption = "${Fmt.pressureTrend(data.hourly)} · MSL",
                accent = Atmos.Gold,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.PRESSURE) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Visibility",
                icon = AtmosIcons.Eye,
                value = vis?.let { v -> Fmt.visibility(v, imperial)?.substringBefore(' ') } ?: "—",
                unit = vis?.let { if (imperial) "mi" else "km" } ?: "",
                caption = vis?.let { v ->
                    val km = v / 1000.0
                    when {
                        km >= 20 -> "Excellent"
                        km >= 10 -> "Good"
                        km >= 4 -> "Moderate"
                        km >= 1 -> "Poor"
                        else -> "Very poor"
                    }
                } ?: "Unavailable",
                accent = Atmos.Secondary,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.VISIBILITY) }
            )
            StatCard(
                title = "Air quality",
                icon = AtmosIcons.Leaf,
                value = air?.europeanAqi?.toString() ?: "—",
                unit = "EAQI",
                caption = air?.europeanAqi?.let { Fmt.aqiLabel(it) } ?: "Unavailable",
                accent = air?.europeanAqi?.let { Fmt.aqiColor(it) } ?: Atmos.Muted,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.AIR) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Rain next hour",
                icon = AtmosIcons.CloudRain,
                value = Fmt.precip(nextHourMm, s.precipitationUnit),
                unit = s.precipitationUnit.symbol,
                caption = if (nextHourMm > 0.02) "Forecast amount"
                else (data.minutely.firstOrNull()?.probability?.let { "Chance $it%" } ?: "No signal"),
                accent = Atmos.Ice,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.PRECIP) }
            )
            StatCard(
                title = "Sun",
                icon = AtmosIcons.Sunrise,
                value = data.daily.firstOrNull()?.sunrise?.let { Fmt.clock(it) } ?: "—",
                unit = "",
                caption = data.daily.firstOrNull()?.sunset?.let { "Sets ${Fmt.clock(it)}" } ?: "Unavailable",
                accent = Atmos.Gold,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.SUN) }
            )
        }
    }
}

@Composable
private fun HourlySection(data: WeatherData, settings: com.alican.atmos.core.AtmosSettings, onOpenForecast: () -> Unit) {
    val hours = remember(data.fetchedAtMillis) {
        val now = data.current.time
        data.hourly.filter { !it.time.isBefore(now.withMinute(0).withSecond(0).withNano(0)) }.take(24)
    }
    if (hours.isEmpty()) return
    Column {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("Next 24 hours", modifier = Modifier.weight(1f))
            Text(
                "Details",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = Atmos.Secondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenForecast() }
                    .padding(6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hours, key = { it.time.toString() }) { hour ->
                val isNow = hour == hours.first()
                val prob = hour.precipProbability
                Column(
                    Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isNow) Atmos.Ice.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.035f)
                        )
                        .padding(vertical = 12.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isNow) "Now" else Fmt.hourShort(hour.time) + ":00",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = if (isNow) Atmos.Ice else Atmos.Muted,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(9.dp))
                    Icon(
                        Fmt.kindIcon(WeatherKind.fromCode(hour.weatherCode), hour.isDay),
                        contentDescription = WeatherKindCodes.label(hour.weatherCode, hour.isDay),
                        tint = if (hour.isDay) Atmos.Secondary else Atmos.Ice.copy(alpha = 0.8f),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "${Fmt.temp(hour.temperature, settings.temperatureUnit)}°",
                        fontFamily = GeistMono,
                        fontSize = 16.sp,
                        color = Atmos.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AtmosIcons.Droplet,
                            contentDescription = null,
                            tint = Atmos.Ice.copy(alpha = if ((prob ?: 0) > 20) 0.9f else 0.28f),
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "${prob ?: 0}%",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = if ((prob ?: 0) > 20) Atmos.Ice else Atmos.Muted,
                            maxLines = 1,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySection(data: WeatherData, settings: com.alican.atmos.core.AtmosSettings, onOpenForecast: () -> Unit) {
    val days = data.daily
    if (days.isEmpty()) return
    val weekMin = days.minOf { it.tempMin }
    val weekMax = days.maxOf { it.tempMax }
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("7-day forecast", trailing = "Open forecast", trailingIcon = AtmosIcons.ChevronRight, onTrailingClick = onOpenForecast)
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
            days.take(7).forEachIndexed { index, day ->
                DayRow(day, index == 0, weekMin, weekMax, settings.temperatureUnit)
                if (index != days.take(7).lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun DayRow(day: DayPoint, isToday: Boolean, weekMin: Double, weekMax: Double, unit: com.alican.atmos.core.TempUnit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (isToday) "Today" else Fmt.weekday(day.date.atTime(12, 0)),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = if (isToday) Atmos.White else Atmos.Secondary,
            maxLines = 1,
            modifier = Modifier.width(62.dp)
        )
        Icon(
            Fmt.kindIcon(WeatherKind.fromCode(day.weatherCode), true),
            contentDescription = WeatherKindCodes.label(day.weatherCode),
            tint = Atmos.Secondary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Row(Modifier.width(46.dp), verticalAlignment = Alignment.CenterVertically) {
            if ((day.precipProbabilityMax ?: 0) >= 10) {
                Icon(AtmosIcons.Droplet, contentDescription = null, tint = Atmos.Ice.copy(alpha = 0.65f), modifier = Modifier.size(8.dp))
                Spacer(Modifier.width(2.dp))
                Text(
                    "${day.precipProbabilityMax}%",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Ice.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${Fmt.temp(day.tempMin, unit)}°",
                fontFamily = GeistMono,
                fontSize = 13.sp,
                color = Atmos.Muted,
                maxLines = 1,
                modifier = Modifier.width(30.dp)
            )
            RangeBar(
                min = day.tempMin,
                max = day.tempMax,
                weekMin = weekMin,
                weekMax = weekMax,
                modifier = Modifier.weight(1f).height(4.dp).padding(horizontal = 4.dp)
            )
            Text(
                "${Fmt.temp(day.tempMax, unit)}°",
                fontFamily = GeistMono,
                fontSize = 13.sp,
                color = Atmos.White.copy(alpha = 0.88f),
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(30.dp)
            )
        }
    }
}

@Composable
private fun RangeBar(min: Double, max: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val span = (weekMax - weekMin).coerceAtLeast(0.1)
    val start = ((min - weekMin) / span).toFloat().coerceIn(0f, 1f)
    val end = ((max - weekMin) / span).toFloat().coerceIn(0f, 1f)
    androidx.compose.foundation.Canvas(modifier) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.07f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
            size = size
        )
        val x = size.width * start
        val w = (size.width * (end - start)).coerceAtLeast(size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Atmos.Ice.copy(alpha = 0.55f), Atmos.Gold.copy(alpha = 0.75f))),
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(w.coerceAtMost(size.width - x), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        )
    }
}

@Composable
private fun DataFootnote(data: WeatherData, state: AtmosUiState, onOpenSettings: () -> Unit) {
    Column(Modifier.padding(horizontal = 22.dp)) {
        Hairline()
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AtmosIcons.Info, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                "Numerical model data, not a station observation",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append(Loc.tr("Source: Open-Meteo"))
                append(" · ${data.modelLabel}")
                data.generationTimeMs?.let { append(" · ${String.format(java.util.Locale.US, "%.1f", it)} ms") }
                append(" · ")
                append(Loc.tr("Air quality: CAMS European"))
                append(" ")
                append(data.air?.time?.let { "(${Loc.tr("obs.")} ${Fmt.clock(it)})" } ?: "")
            }.trim(),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted.copy(alpha = 0.8f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(
                "Data accuracy & sources",
                icon = AtmosIcons.Layers,
                tint = Atmos.Secondary,
                onClick = onOpenSettings
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "ATMOS · sensory weather",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted.copy(alpha = 0.55f)
        )
    }
}
