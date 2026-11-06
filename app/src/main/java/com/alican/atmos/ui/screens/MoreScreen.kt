package com.alican.atmos.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import com.alican.atmos.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alican.atmos.BuildConfig
import com.alican.atmos.core.AnimationQuality
import com.alican.atmos.core.AppLanguage
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.ForecastModel
import com.alican.atmos.core.PrecipUnit
import com.alican.atmos.core.PressureUnit
import com.alican.atmos.core.TempUnit
import com.alican.atmos.core.WindUnit
import com.alican.atmos.ui.AtmosUiState
import com.alican.atmos.ui.Fmt
import com.alican.atmos.ui.components.AtmosCard
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.components.Hairline
import com.alican.atmos.ui.components.Pill
import com.alican.atmos.ui.components.SectionHeader
import com.alican.atmos.ui.components.SegmentedOptions
import com.alican.atmos.ui.theme.Atmos
import com.alican.atmos.ui.theme.GeistMono
import com.alican.atmos.widget.AtmosDetailWidget
import com.alican.atmos.widget.AtmosForecastWidget
import com.alican.atmos.widget.AtmosNowWidget

@Composable
fun MoreScreen(
    state: AtmosUiState,
    onUpdateSettings: ((AtmosSettings) -> AtmosSettings) -> Unit,
    onOpenLocation: () -> Unit,
    onRefresh: () -> Unit,
    onRequestLocation: () -> Unit,
    onRemovePlace: (com.alican.atmos.core.Place) -> Unit
) {
    val context = LocalContext.current
    val s = state.settings
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(bottom = 132.dp)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp)) {
            Text("More", style = androidx.compose.material3.MaterialTheme.typography.headlineLarge, color = Atmos.White)
            Spacer(Modifier.height(2.dp))
            Text(
                "Units, motion, data integrity and widgets",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Atmos.Muted
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            // ---------- Language ----------
            SectionHeader("Language")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                Text(
                    "Interface language. Data sources keep their original names.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted
                )
                Spacer(Modifier.height(10.dp))
                AppLanguage.entries.forEach { language ->
                    LanguageRow(language, s.language == language) {
                        onUpdateSettings { it.copy(language = language) }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            // ---------- Accuracy / data ----------
            SectionHeader("Data & accuracy")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Atmos.Good.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AtmosIcons.Layers, contentDescription = null, tint = Atmos.Good, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Numerical model data",
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                            color = Atmos.White
                        )
                        Text(
                            "${state.data?.modelLabel ?: s.model.label} · fetched ${Fmt.relativeUpdate(state.data?.fetchedAtMillis ?: System.currentTimeMillis())}",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Atmos.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Pill("Refresh", onClick = onRefresh, tint = Atmos.Ice)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "ATMOS displays model output, not a physical weather station. Values are never invented or smoothed to look complete: " +
                        "a missing field stays missing. Probability is the chance of measurable precipitation; the amount is a separate number.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Atmos.Secondary
                )
                Spacer(Modifier.height(14.dp))
                Text("FORECAST MODEL", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
                Spacer(Modifier.height(6.dp))
                ForecastModel.entries.forEach { model ->
                    ModelRow(model, s.model == model) {
                        onUpdateSettings { it.copy(model = model) }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AtmosIcons.Info, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Ensemble spread (ECMWF, 51 members) is shown on the forecast tab as an uncertainty band.",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = Atmos.Muted
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("Open-Meteo", icon = AtmosIcons.External, tint = Atmos.Secondary, onClick = {
                        open(context, "https://open-meteo.com")
                    })
                    Pill("Official alerts", icon = AtmosIcons.Warning, tint = Atmos.Gold, onClick = {
                        open(context, "https://www.meteoalarm.org")
                    })
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "ATMOS does not ingest official national warnings; the in-app hazard banner is a forecast signal. " +
                        "For life-safety decisions always follow your national meteorological service.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.height(26.dp))
            // ---------- Units ----------
            SectionHeader("Units")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                UnitRow("Temperature") {
                    SegmentedOptions(
                        TempUnit.entries, s.temperatureUnit, { it.symbol },
                        modifier = Modifier.fillMaxWidth()
                    ) { u -> onUpdateSettings { it.copy(temperatureUnit = u) } }
                }
                Spacer(Modifier.height(10.dp))
                UnitRow("Wind") {
                    SegmentedOptions(
                        WindUnit.entries, s.windUnit, { it.symbol },
                        modifier = Modifier.fillMaxWidth()
                    ) { u -> onUpdateSettings { it.copy(windUnit = u) } }
                }
                Spacer(Modifier.height(10.dp))
                UnitRow("Pressure") {
                    SegmentedOptions(
                        PressureUnit.entries, s.pressureUnit, { it.symbol },
                        modifier = Modifier.fillMaxWidth()
                    ) { u -> onUpdateSettings { it.copy(pressureUnit = u) } }
                }
                Spacer(Modifier.height(10.dp))
                UnitRow("Precipitation") {
                    SegmentedOptions(
                        PrecipUnit.entries, s.precipitationUnit, { it.symbol },
                        modifier = Modifier.fillMaxWidth()
                    ) { u -> onUpdateSettings { it.copy(precipitationUnit = u) } }
                }
            }

            Spacer(Modifier.height(26.dp))
            // ---------- Motion ----------
            SectionHeader("Motion & atmosphere")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                Text("ANIMATION QUALITY", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
                Spacer(Modifier.height(7.dp))
                SegmentedOptions(
                    AnimationQuality.entries, s.animationQuality, { it.label }
                ) { q -> onUpdateSettings { it.copy(animationQuality = q) } }
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    title = "Lightning haptics",
                    subtitle = "Subtle pulse when a thunderstorm signal flashes. Never claims a strike.",
                    checked = s.lightningHaptics
                ) { on -> onUpdateSettings { it.copy(lightningHaptics = on) } }
                Hairline()
                ToggleRow(
                    title = "Reduce motion",
                    subtitle = "Freezes particles and flashes. Keeps lighting and layout intact.",
                    checked = s.reduceMotion
                ) { on -> onUpdateSettings { it.copy(reduceMotion = on) } }
                Hairline()
                ToggleRow(
                    title = "Use device location",
                    subtitle = "Otherwise ATMOS stays on the city you selected.",
                    checked = s.useLocation
                ) { on ->
                    onUpdateSettings { it.copy(useLocation = on) }
                    if (on) onRequestLocation()
                }
            }

            Spacer(Modifier.height(26.dp))
            // ---------- Location ----------
            SectionHeader("Places", trailing = "Manage", trailingIcon = AtmosIcons.ChevronRight, onTrailingClick = onOpenLocation)
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 16.dp)) {
                state.savedPlaces.ifEmpty {
                    listOf(state.place)
                }.take(6).forEachIndexed { i, p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(AtmosIcons.Location, contentDescription = null, tint = Atmos.Muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.subtitle.isNotBlank()) {
                                Text(p.subtitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Text(
                            if (p.isCurrentLocation) "Device" else "Saved",
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Atmos.Muted
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).clickable { onRemovePlace(p) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(AtmosIcons.Close, contentDescription = "Remove", tint = Atmos.Muted, modifier = Modifier.size(12.dp))
                        }
                    }
                    if (i != (state.savedPlaces.ifEmpty { listOf(state.place) }.take(6).lastIndex)) Hairline()
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(AtmosIcons.Plus, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add a city", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Atmos.Ice)
                    Spacer(Modifier.weight(1f))
                    Pill("Search", onClick = onOpenLocation, tint = Atmos.Secondary)
                }
            }

            Spacer(Modifier.height(26.dp))
            // ---------- Widgets ----------
            SectionHeader("Home screen widgets")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(Atmos.Ice.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AtmosIcons.Widget, contentDescription = null, tint = Atmos.Ice, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Three widget families", style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White)
                        Text("Pin any of them from here or from your launcher", style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                WidgetRow(
                    title = "Now",
                    subtitle = "Current temperature, condition and range",
                    onPin = { pinWidget(context, AtmosNowWidget::class.java) }
                )
                WidgetRow(
                    title = "Hourly",
                    subtitle = "Next six hours with precipitation chance",
                    onPin = { pinWidget(context, AtmosForecastWidget::class.java) }
                )
                WidgetRow(
                    title = "Air & exposure",
                    subtitle = "AQI, UV and wind at a glance",
                    onPin = { pinWidget(context, AtmosDetailWidget::class.java) }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Widgets refresh from the same cached model data. They never show a value the app has not fetched.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted
                )
            }

            Spacer(Modifier.height(26.dp))
            // ---------- About ----------
            SectionHeader("About")
            Spacer(Modifier.height(10.dp))
            AtmosCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ATMOS", fontFamily = GeistMono, fontSize = 22.sp, letterSpacing = 2.sp, color = Atmos.White)
                    Spacer(Modifier.weight(1f))
                    Text("v${BuildConfig.VERSION_NAME}", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = Atmos.Muted)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "A sensory weather surface built on measured data. Midnight palette, Geist typography, no decorative fakery.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = Atmos.Secondary
                )
                Spacer(Modifier.height(14.dp))
                SourceRow("Weather & forecast", "Open-Meteo numerical models")
                SourceRow("Global model", "ECMWF IFS 0.25°")
                SourceRow("Regional models", "DWD ICON · Météo-France AROME · NOAA GFS")
                SourceRow("Air quality", "CAMS European forecast")
                SourceRow("Radar", "RainViewer (observed + nowcast)")
                SourceRow("Map", "OpenStreetMap data © CARTO")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("GitHub", icon = AtmosIcons.External, tint = Atmos.Secondary, onClick = {
                        open(context, "https://github.com/tutualican/atmos")
                    })
                    Pill("Open-Meteo licence", icon = AtmosIcons.External, tint = Atmos.Secondary, onClick = {
                        open(context, "https://open-meteo.com/en/license")
                    })
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Built with Kotlin and Jetpack Compose. ${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = Atmos.Muted.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun LanguageRow(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Atmos.Ice.copy(alpha = 0.07f) else Color.Transparent, tween(160), label = "langBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (selected) Atmos.Ice else Color.Transparent)
                .then(
                    if (!selected) Modifier.background(Color.White.copy(alpha = 0.10f), CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(5.dp).clip(CircleShape).background(Atmos.Midnight))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            language.nativeName,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            color = if (selected) Atmos.White else Atmos.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (language != AppLanguage.SYSTEM) {
            Spacer(Modifier.width(8.dp))
            Text(
                language.englishName,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UnitRow(label: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = Atmos.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(5.dp))
        control()
    }
}

@Composable
private fun ModelRow(model: ForecastModel, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Atmos.Ice.copy(alpha = 0.07f) else Color.Transparent, tween(160), label = "modelBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (selected) Atmos.Ice else Color.Transparent)
                .then(
                    if (!selected) Modifier.background(Color.White.copy(alpha = 0.10f), CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(Modifier.size(5.dp).clip(CircleShape).background(Atmos.Midnight))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                model.label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = if (selected) Atmos.White else Atmos.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                modelDescription(model),
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Atmos.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun modelDescription(model: ForecastModel): String = when (model) {
    ForecastModel.BEST_MATCH -> "Blends the strongest regional model per location"
    ForecastModel.ECMWF -> "Global medium-range reference"
    ForecastModel.ICON -> "High-resolution European coverage"
    ForecastModel.GFS -> "NOAA global model"
    ForecastModel.METEOFRANCE -> "Very high resolution over France / W. Europe"
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted)
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked, onChange)
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val offset by animateDpAsState(if (checked) 18.dp else 2.dp, tween(190), label = "toggleOffset")
    val bg by animateColorAsState(if (checked) Atmos.Ice.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.10f), tween(190), label = "toggleBg")
    Box(
        Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(if (checked) Atmos.Midnight else Atmos.Secondary)
        )
    }
}

@Composable
private fun WidgetRow(title: String, subtitle: String, onPin: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(AtmosIcons.Widget, contentDescription = null, tint = Atmos.Secondary, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall, color = Atmos.White)
            Text(subtitle, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Pill("Pin", icon = AtmosIcons.Plus, tint = Atmos.Ice, onClick = onPin)
    }
}

@Composable
private fun SourceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Muted, modifier = Modifier.width(108.dp))
        Text(value, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, color = Atmos.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun open(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun pinWidget(context: Context, provider: Class<*>) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = AppWidgetManager.getInstance(context)
    val info = ComponentName(context, provider)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(info, null, null)
    }
}
