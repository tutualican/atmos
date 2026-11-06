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
import com.alican.atmos.core.Activities
import com.alican.atmos.core.ActivitySuitability
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.OutdoorActivity
import com.alican.atmos.data.OfficialAlert
import com.alican.atmos.notify.Nowcast
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
import com.alican.atmos.ui.components.PrecipBarChart
import com.alican.atmos.ui.components.ScoreBar
import com.alican.atmos.ui.components.SectionHeader
import com.alican.atmos.ui.components.SegmentedBar
import com.alican.atmos.ui.components.Sparkline
import com.alican.atmos.ui.components.StatCard
import com.alican.atmos.ui.components.SunArc
import com.alican.atmos.ui.components.TemperatureCurve
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
    hourOfDay: Int,
    onOpenLocation: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (DetailCard) -> Unit,
    onOpenSettings: () -> Unit,
    onLightning: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenAlert: (com.alican.atmos.data.OfficialAlert) -> Unit
) {
    val data = state.data
    val kind = data?.kind ?: WeatherKind.OVERCAST
    val isDay = data?.current?.isDay ?: true
    val settings = state.settings

    Box(Modifier.fillMaxSize()) {
        WeatherScene(
            kind = kind,
            isDay = isDay,
            hourOfDay = hourOfDay,
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
                        0f to Atmos.Midnight.copy(alpha = 0.52f),
                        0.14f to Color.Transparent,
                        0.34f to Atmos.Midnight.copy(alpha = 0.24f),
                        0.52f to Atmos.Midnight.copy(alpha = 0.86f),
                        0.64f to Atmos.Midnight,
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
                state.officialAlerts.firstOrNull()?.let { alert ->
                    OfficialAlertBanner(alert, onOpenAlert)
                    Spacer(Modifier.height(10.dp))
                }
                NowcastCards(data, state.settings, onOpenDetail)
                ForecastHazardBanner(data, onOpenDetail)
                Spacer(Modifier.height(22.dp))
                TrendsSection(data, state.settings, onOpenForecast, onOpenDetail)
                Spacer(Modifier.height(24.dp))
                UvSunSection(data, state.settings, onOpenDetail)
                Spacer(Modifier.height(24.dp))
                FocusGrid(state, data, onOpenDetail)
                Spacer(Modifier.height(24.dp))
                DailySection(data, state.settings, onOpenForecast)
                Spacer(Modifier.height(24.dp))
                ActivitiesSection(data)
                Spacer(Modifier.height(24.dp))
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
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Atmos.Secondary,
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
private fun NowcastCards(
    data: WeatherData,
    settings: AtmosSettings,
    onOpenDetail: (DetailCard) -> Unit
) {
    val outlook = remember(data.fetchedAtMillis, data.minutely.size) { Nowcast.outlook(data) }
    val next = data.hourly.firstOrNull()
    val amount = outlook?.amountNext60mm ?: next?.precipitation ?: 0.0
    val peak = outlook?.peakProbability ?: next?.precipProbability
    val starts = outlook?.startsInMinutes
    val status = when {
        outlook == null -> Loc.tr("Unavailable")
        outlook.rainingNow -> Loc.tr("Rain is falling now")
        starts != null && starts <= 120 -> Loc.t("Rain likely in about {} min", starts)
        else -> Loc.tr("Dry next 2 h")
    }
    val statusCaption = when {
        outlook == null -> Loc.tr("Minutely series not provided for this place")
        outlook.rainingNow && outlook.endsInMinutes != null ->
            Loc.t("Easing in about {} min", outlook.endsInMinutes)
        outlook.rainingNow -> Loc.tr("Nowcast · next 2 h")
        else -> Loc.tr("Nowcast · next 2 h")
    }

    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Live now", trailing = "Tap any card for detail")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtmosCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.PRECIP) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(11.dp))
                            .background(Atmos.Ice.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AtmosIcons.CloudRain, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Rain nowcast",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    status,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    statusCaption,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AtmosCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.PRECIP) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(11.dp))
                            .background(Atmos.Ice.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AtmosIcons.Droplet, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        "Next hour",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.Secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Fmt.precip(amount, settings.precipitationUnit),
                        fontFamily = GeistMono,
                        fontWeight = FontWeight.Light,
                        fontSize = (26 * Atmos.textScale).sp,
                        lineHeight = (30 * Atmos.textScale).sp,
                        color = Atmos.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        settings.precipitationUnit.symbol,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        color = Atmos.Muted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    peak?.let { Loc.t("Peak chance {}%", it) } ?: Loc.tr("Chance unavailable"),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = if ((peak ?: 0) >= 20) Atmos.Ice else Atmos.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (outlook != null) Loc.tr("Nowcast amount") else Loc.tr("Hourly forecast"),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OfficialAlertBanner(alert: OfficialAlert, onOpen: (OfficialAlert) -> Unit) {
    AtmosCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 10.dp),
        onClick = { onOpen(alert) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(11.dp))
                    .background(Atmos.Danger.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(AtmosIcons.Warning, contentDescription = null, tint = Atmos.Danger, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "OFFICIAL · ${alert.source}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Danger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    alert.event,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    alert.headline,
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
    val vis = data.hourly.firstOrNull()?.visibility
    val pressure = Fmt.pressure(current.pressureMsl, s.pressureUnit)

    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Conditions", trailing = "Tap any card for detail")
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Wind",
                icon = AtmosIcons.Wind,
                value = Fmt.wind(current.windSpeed, s.windUnit),
                unit = s.windUnit.symbol,
                caption = "${Fmt.compass(current.windDirection)} · ${Loc.tr("gusts")} ${Fmt.wind(current.windGusts, s.windUnit)}",
                accent = Atmos.Ice,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.WIND) }
            )
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
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            StatCard(
                title = "Visibility",
                icon = AtmosIcons.Eye,
                value = vis?.let { v -> Fmt.visibility(v, imperial)?.substringBefore(' ') } ?: "—",
                unit = vis?.let { if (imperial) "mi" else "km" } ?: "",
                caption = vis?.let { v ->
                    val km = v / 1000.0
                    when {
                        km >= 20 -> Loc.tr("Excellent")
                        km >= 10 -> Loc.tr("Good")
                        km >= 4 -> Loc.tr("Moderate")
                        km >= 1 -> Loc.tr("Poor")
                        else -> Loc.tr("Very poor")
                    }
                } ?: Loc.tr("Unavailable"),
                accent = Atmos.Secondary,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.VISIBILITY) }
            )
        }
        Spacer(Modifier.height(10.dp))
        AirQualityCard(air, onOpenDetail)
    }
}

@Composable
private fun AirQualityCard(air: com.alican.atmos.core.AirQuality?, onOpenDetail: (DetailCard) -> Unit) {
    val aqi = air?.europeanAqi
    val accent = aqi?.let { Fmt.aqiColor(it) } ?: Atmos.Muted
    AtmosCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenDetail(DetailCard.AIR) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(AtmosIcons.Leaf, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Air quality",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.Secondary,
                    maxLines = 1
                )
                Text(
                    aqi?.let { Fmt.aqiLabel(it) } ?: Loc.tr("Unavailable"),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = if (aqi == null) Atmos.Muted else Atmos.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (aqi != null) {
                Text(
                    aqi.toString(),
                    fontFamily = GeistMono,
                    fontWeight = FontWeight.Light,
                    fontSize = (24 * Atmos.textScale).sp,
                    color = accent,
                    maxLines = 1
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "EAQI",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SegmentedBar(
            segments = listOf(
                Atmos.Good,
                Color(0xFFB9C7A3),
                Atmos.Gold,
                Color(0xFFDD9F72),
                Atmos.Danger,
                Color(0xFFB85C6A)
            ),
            fraction = (aqi ?: 0) / 100f,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            showMarker = aqi != null
        )
        Spacer(Modifier.height(6.dp))
        Text(
            buildString {
                append(Loc.tr("European AQI · CAMS"))
                air?.pm25?.let { append(" · PM2.5 ${String.format(java.util.Locale.US, "%.0f", it)} µg/m³") }
            },
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = Atmos.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrendsSection(
    data: WeatherData,
    settings: AtmosSettings,
    onOpenForecast: () -> Unit,
    onOpenDetail: (DetailCard) -> Unit
) {
    val hours = remember(data.fetchedAtMillis) {
        val now = data.current.time.withMinute(0).withSecond(0).withNano(0)
        data.hourly.filter { !it.time.isBefore(now) }.take(24)
    }
    if (hours.size < 2) return
    val unit = settings.temperatureUnit
    val temps = hours.map { it.temperature }
    val minT = temps.min()
    val maxT = temps.max()
    val total = hours.sumOf { it.precipitation }
    val peakChance = hours.mapNotNull { it.precipProbability }.maxOrNull()

    Column(Modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("Next 24 hours", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Pill("Full forecast", icon = AtmosIcons.Calendar, tint = Atmos.Ice, onClick = onOpenForecast)
        }
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth(), onClick = onOpenForecast) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Temperature trend",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.White
                    )
                    Text(
                        "Hourly model curve · local time",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "H ${Fmt.temp(maxT, unit)}°  L ${Fmt.temp(minT, unit)}°",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = Atmos.Secondary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(11.dp))
            TemperatureCurve(
                values = temps,
                times = hours.map { it.time },
                unit = unit,
                modifier = Modifier.fillMaxWidth().height(118.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth(), onClick = { onOpenDetail(DetailCard.PRECIP) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Precipitation",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.White
                    )
                    Text(
                        "Hourly amount · not probability",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Fmt.precip(total, settings.precipitationUnit),
                        fontFamily = GeistMono,
                        fontWeight = FontWeight.Light,
                        fontSize = (22 * Atmos.textScale).sp,
                        color = Atmos.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        settings.precipitationUnit.symbol,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (total <= 0.005 && hours.all { it.precipitation <= 0.005 }) {
                Box(
                    Modifier.fillMaxWidth().height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No measurable precipitation modelled in the next 24 hours",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Atmos.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                PrecipBarChart(
                    amounts = hours.map { it.precipitation },
                    times = hours.map { it.time },
                    unit = settings.precipitationUnit,
                    modifier = Modifier.fillMaxWidth().height(108.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                peakChance?.let {
                    Loc.t("Bars show amount in {} · peak chance {}%", settings.precipitationUnit.symbol, it)
                } ?: Loc.t("Bars show amount in {}", settings.precipitationUnit.symbol),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UvSunSection(
    data: WeatherData,
    settings: AtmosSettings,
    onOpenDetail: (DetailCard) -> Unit
) {
    val today = data.daily.firstOrNull()
    val uv = data.hourly.firstOrNull()?.uvIndex ?: today?.uvMax
    val uvMax = today?.uvMax ?: data.hourly.take(24).mapNotNull { it.uvIndex }.maxOrNull()
    val peakHour = data.hourly.take(24)
        .filter { it.uvIndex != null }
        .maxByOrNull { it.uvIndex ?: 0.0 }
        ?.time
    val uvColor = uv?.let { Fmt.uvColor(it) } ?: Atmos.Muted
    val uvSegments = listOf(0.5, 2.0, 4.0, 5.5, 6.5, 7.5, 8.5, 9.5, 10.5, 12.0)
        .map { Fmt.uvColor(it) }

    val sunrise = today?.sunrise
    val sunset = today?.sunset
    val current = data.current
    val fraction = if (current.isDay && sunrise != null && sunset != null && sunset.isAfter(sunrise)) {
        val total = java.time.Duration.between(sunrise, sunset).seconds.toFloat()
        val elapsed = java.time.Duration.between(sunrise, current.time).seconds.toFloat()
        if (total > 0f && elapsed in 0f..total) elapsed / total else null
    } else null
    val daylight = if (sunrise != null && sunset != null && sunset.isAfter(sunrise)) {
        val minutes = java.time.Duration.between(sunrise, sunset).toMinutes().toInt()
        Loc.t("Daylight {} h {} min", minutes / 60, minutes % 60)
    } else {
        Loc.tr("Sun times unavailable")
    }

    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Sun & UV")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AtmosCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.UV) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "UV index",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.Secondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Icon(AtmosIcons.Sun, contentDescription = null, tint = uvColor, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        uv?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                        fontFamily = GeistMono,
                        fontWeight = FontWeight.Light,
                        fontSize = (26 * Atmos.textScale).sp,
                        lineHeight = (30 * Atmos.textScale).sp,
                        color = Atmos.White.copy(alpha = 0.94f),
                        maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        uv?.let { Fmt.uvLabel(it) } ?: Loc.tr("Unavailable"),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = uvColor,
                        modifier = Modifier.padding(bottom = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                SegmentedBar(
                    segments = uvSegments,
                    fraction = ((uv ?: 0.0) / 11.0).toFloat(),
                    modifier = Modifier.fillMaxWidth().height(16.dp),
                    showMarker = uv != null
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (uvMax != null && peakHour != null) {
                        Loc.t(
                            "Peak today {} · {}",
                            String.format(java.util.Locale.US, "%.1f", uvMax),
                            Fmt.clock(peakHour)
                        )
                    } else {
                        Loc.tr("Peak unavailable")
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    uvGuidance(uv),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = uvColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AtmosCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = { onOpenDetail(DetailCard.SUN) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sun",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.Secondary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Icon(AtmosIcons.Sunrise, contentDescription = null, tint = Atmos.Gold, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.height(2.dp))
                SunArc(
                    fraction = fraction,
                    modifier = Modifier.fillMaxWidth().height(78.dp)
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AtmosIcons.Sunrise,
                        contentDescription = Loc.tr("Sunrise"),
                        tint = Atmos.Gold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        sunrise?.let { Fmt.clock(it) } ?: "—",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Secondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        AtmosIcons.Sunset,
                        contentDescription = Loc.tr("Sunset"),
                        tint = Atmos.GoldSoft,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        sunset?.let { Fmt.clock(it) } ?: "—",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Secondary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    daylight,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun uvGuidance(uv: Double?): String = when {
    uv == null -> Loc.tr("UV guidance unavailable")
    uv < 3.0 -> Loc.tr("No sun protection needed")
    uv < 6.0 -> Loc.tr("Protection recommended")
    uv < 8.0 -> Loc.tr("Seek shade around midday")
    else -> Loc.tr("Avoid direct sun at midday")
}

@Composable
private fun ActivitiesSection(data: WeatherData) {
    val results = remember(data.fetchedAtMillis, data.current.isDay, data.air?.time) {
        Activities.evaluate(data)
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        SectionHeader("Outdoor activities", trailing = "Modelled")
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth()) {
            results.forEachIndexed { index, item ->
                ActivityRow(item)
                if (index != results.lastIndex) {
                    Spacer(Modifier.height(10.dp))
                    Hairline()
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Derived from the forecast. Not a safety guarantee.",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActivityRow(item: ActivitySuitability) {
    val color = when {
        item.score >= 80 -> Atmos.Good
        item.score >= 60 -> Atmos.Ice
        item.score >= 40 -> Atmos.Gold
        else -> Atmos.Danger
    }
    val icon = when (item.activity) {
        OutdoorActivity.WALK -> AtmosIcons.Walk
        OutdoorActivity.RUN -> AtmosIcons.Run
        OutdoorActivity.CYCLE -> AtmosIcons.Bike
    }
    val label = when (item.activity) {
        OutdoorActivity.WALK -> "Walk"
        OutdoorActivity.RUN -> "Run"
        OutdoorActivity.CYCLE -> "Cycle"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.11f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = Loc.tr(label), tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = Atmos.White,
                    maxLines = 1
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    Loc.tr(item.status),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = color,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(5.dp))
            ScoreBar(
                score = item.score,
                color = color,
                modifier = Modifier.fillMaxWidth().height(5.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                item.factors.joinToString(" · ") { Loc.tr(it) },
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        SectionHeader("7-day forecast")
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
            days.take(7).forEachIndexed { index, day ->
                DayRow(day, index == 0, weekMin, weekMax, settings.temperatureUnit)
                if (index != days.take(7).lastIndex) Hairline()
            }
        }
        Spacer(Modifier.height(10.dp))
        AtmosCard(Modifier.fillMaxWidth(), onClick = onOpenForecast) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(AtmosIcons.Calendar, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Full forecast",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = Atmos.White
                    )
                    Text(
                        "Hourly detail, precipitation chart and uncertainty band",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = Atmos.Muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(AtmosIcons.ChevronRight, contentDescription = null, tint = Atmos.Secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DayRow(day: DayPoint, isToday: Boolean, weekMin: Double, weekMax: Double, unit: com.alican.atmos.core.TempUnit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Fmt.kindIcon(WeatherKind.fromCode(day.weatherCode), true),
                contentDescription = WeatherKindCodes.label(day.weatherCode),
                tint = Atmos.Secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (isToday) Loc.tr("Today") else Fmt.weekday(day.date.atTime(12, 0)),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = if (isToday) Atmos.White else Atmos.Secondary,
                    maxLines = 1
                )
                Text(
                    Loc.tr(WeatherKindCodes.label(day.weatherCode)),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            Loc.t("Rain {}%", day.precipProbabilityMax ?: 0),
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = if ((day.precipProbabilityMax ?: 0) >= 20) Atmos.Ice else Atmos.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp)
        )
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${Fmt.temp(day.tempMin, unit)}°",
                fontFamily = GeistMono,
                fontSize = (14 * Atmos.textScale).sp,
                color = Atmos.Muted,
                maxLines = 1,
                modifier = Modifier.width(34.dp)
            )
            RangeBar(
                min = day.tempMin,
                max = day.tempMax,
                weekMin = weekMin,
                weekMax = weekMax,
                modifier = Modifier.weight(1f).height(5.dp).padding(horizontal = 4.dp)
            )
            Text(
                "${Fmt.temp(day.tempMax, unit)}°",
                fontFamily = GeistMono,
                fontSize = (14 * Atmos.textScale).sp,
                color = Atmos.White.copy(alpha = 0.9f),
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp)
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
