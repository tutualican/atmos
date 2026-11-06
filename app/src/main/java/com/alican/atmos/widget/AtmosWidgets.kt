package com.alican.atmos.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.alican.atmos.MainActivity
import com.alican.atmos.R
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.ForecastModel
import com.alican.atmos.core.PrecipUnit
import com.alican.atmos.core.TempUnit
import com.alican.atmos.core.WeatherData
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.data.AtmosStore
import com.alican.atmos.data.OpenMeteo
import com.alican.atmos.i18n.Loc
import com.alican.atmos.ui.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

// Exact Midnight palette values, kept as ARGB so RemoteViews can apply them at runtime.
private const val W_TEXT = 0xFFFFFFFF.toInt()
private const val W_SECONDARY = 0xFFA6B2C4.toInt()
private const val W_MUTED = 0xFF748198.toInt()
private const val W_ICE = 0xFF9CC2E0.toInt()

/** Shared rendering + refresh pipeline for the three ATMOS widget families. */
object AtmosWidgets {

    fun refreshAll(context: Context) {
        val store = AtmosStore(context)
        val bundle = store.lastBundle ?: return
        val data = runCatching { OpenMeteo.parse(bundle) }.getOrNull() ?: return
        renderAll(context, data, store.settings)
    }

    fun renderAll(context: Context, data: WeatherData, settings: AtmosSettings) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            AtmosNowWidget::class.java,
            AtmosForecastWidget::class.java,
            AtmosDetailWidget::class.java,
            AtmosCompactWidget::class.java,
            AtmosStripWidget::class.java
        ).forEach { cls ->
            val ids = manager.getAppWidgetIds(ComponentName(context, cls))
            if (ids.isNotEmpty()) {
                when (cls) {
                    AtmosNowWidget::class.java -> renderNow(context, manager, ids, data, settings)
                    AtmosForecastWidget::class.java -> renderHourly(context, manager, ids, data, settings)
                    AtmosDetailWidget::class.java -> renderDetail(context, manager, ids, data, settings)
                    AtmosCompactWidget::class.java -> renderCompact(context, manager, ids, data, settings)
                    AtmosStripWidget::class.java -> renderStrip(context, manager, ids, data, settings)
                }
            }
        }
    }

    fun refreshFromNetworkIfStale(context: Context) {
        val store = AtmosStore(context)
        val age = System.currentTimeMillis() - (store.lastBundle?.fetchedAt ?: 0L)
        if (age < 15 * 60_000 && store.lastBundle != null) {
            refreshAll(context)
            return
        }
        val place = store.selectedPlace
        if (place == null) {
            refreshAll(context)
            return
        }
        runCatching {
            val bundle = OpenMeteo.fetchBundle(place, store.settings.model)
            store.lastBundle = bundle
        }
        refreshAll(context)
    }

    fun renderNow(context: Context, manager: AppWidgetManager, ids: IntArray, data: WeatherData, s: AtmosSettings) {
        val views = RemoteViews(context.packageName, R.layout.widget_now)
        val c = data.current
        val scale = textScale(s)
        val accent = accentArgb(context, s)
        views.setTextViewText(R.id.widget_location, data.place.name)
        views.setTextViewText(R.id.widget_updated, Loc.t("Updated {}", Fmt.relativeUpdate(data.fetchedAtMillis)))
        views.setTextViewText(R.id.widget_temp, Fmt.temp(c.temperature, s.temperatureUnit))
        views.setTextViewText(R.id.widget_unit, s.temperatureUnit.symbol)
        views.setTextViewText(R.id.widget_condition, Loc.tr(com.alican.atmos.core.WeatherKindCodes.label(c.weatherCode, c.isDay)))
        views.setTextViewText(
            R.id.widget_range,
            Loc.t(
                "H {}°  L {}°",
                Fmt.temp(data.daily.firstOrNull()?.tempMax ?: c.temperature, s.temperatureUnit),
                Fmt.temp(data.daily.firstOrNull()?.tempMin ?: c.temperature, s.temperatureUnit)
            )
        )
        views.setTextViewText(
            R.id.widget_meta,
            Loc.t(
                "Feels {}°   {}%   {}",
                Fmt.temp(c.apparent, s.temperatureUnit),
                c.humidity,
                "${Fmt.wind(c.windSpeed, s.windUnit)} ${s.windUnit.symbol}"
            )
        )
        views.setImageViewResource(R.id.widget_icon, iconRes(WeatherKind.fromCode(c.weatherCode), c.isDay))
        views.sizes(
            scale,
            R.id.widget_location to 15f,
            R.id.widget_updated to 11f,
            R.id.widget_temp to 52f,
            R.id.widget_unit to 16f,
            R.id.widget_condition to 15f,
            R.id.widget_range to 12f,
            R.id.widget_meta to 12f
        )
        views.colors(W_TEXT, R.id.widget_location, R.id.widget_temp, R.id.widget_condition)
        views.colors(W_SECONDARY, R.id.widget_range)
        views.colors(W_MUTED, R.id.widget_updated, R.id.widget_meta)
        views.colors(accent, R.id.widget_unit)
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        manager.updateAppWidget(ids, views)
    }

    fun renderHourly(context: Context, manager: AppWidgetManager, ids: IntArray, data: WeatherData, s: AtmosSettings) {
        val views = RemoteViews(context.packageName, R.layout.widget_hourly)
        val scale = textScale(s)
        val accent = accentArgb(context, s)
        views.setTextViewText(R.id.widget_location, data.place.name)
        views.setTextViewText(R.id.widget_updated, "${data.modelLabel} · ${Fmt.relativeUpdate(data.fetchedAtMillis)}")
        views.sizes(scale, R.id.widget_location to 14f, R.id.widget_updated to 10f)
        views.colors(W_TEXT, R.id.widget_location)
        views.colors(W_MUTED, R.id.widget_updated)
        val now = data.current.time
        val hours = data.hourly.filter { !it.time.isBefore(now.withMinute(0).withSecond(0).withNano(0)) }.take(6)
        val hourIds = intArrayOf(
            R.id.h0, R.id.h1, R.id.h2, R.id.h3, R.id.h4, R.id.h5
        )
        val iconIds = intArrayOf(
            R.id.i0, R.id.i1, R.id.i2, R.id.i3, R.id.i4, R.id.i5
        )
        val tempIds = intArrayOf(
            R.id.t0, R.id.t1, R.id.t2, R.id.t3, R.id.t4, R.id.t5
        )
        val probIds = intArrayOf(
            R.id.p0, R.id.p1, R.id.p2, R.id.p3, R.id.p4, R.id.p5
        )
        val dropIds = intArrayOf(
            R.id.d0, R.id.d1, R.id.d2, R.id.d3, R.id.d4, R.id.d5
        )
        hours.forEachIndexed { i, hour ->
            if (i > 5) return@forEachIndexed
            val wet = (hour.precipProbability ?: 0) >= 20
            views.setTextViewText(hourIds[i], if (i == 0) Loc.tr("Now") else Fmt.hourShort(hour.time) + ":00")
            views.setImageViewResource(iconIds[i], iconRes(WeatherKind.fromCode(hour.weatherCode), hour.isDay))
            views.setTextViewText(tempIds[i], "${Fmt.temp(hour.temperature, s.temperatureUnit)}°")
            views.setTextViewText(probIds[i], "${hour.precipProbability ?: 0}%")
            views.sizes(scale, hourIds[i] to 11f, tempIds[i] to 16f, probIds[i] to 11f)
            views.colors(W_SECONDARY, hourIds[i])
            views.colors(W_TEXT, tempIds[i])
            views.colors(if (wet) accent else W_SECONDARY, probIds[i])
            views.setInt(dropIds[i], "setColorFilter", if (wet) accent else W_MUTED)
        }
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        manager.updateAppWidget(ids, views)
    }

    fun renderDetail(context: Context, manager: AppWidgetManager, ids: IntArray, data: WeatherData, s: AtmosSettings) {
        val views = RemoteViews(context.packageName, R.layout.widget_detail)
        val scale = textScale(s)
        views.setTextViewText(R.id.widget_location, Loc.t("{} · exposure", data.place.name))
        views.setTextViewText(R.id.widget_updated, Loc.t("Updated {}", Fmt.relativeUpdate(data.fetchedAtMillis)))
        val aqi = data.air?.europeanAqi
        views.setTextViewText(R.id.air_value, aqi?.toString() ?: "—")
        views.setTextViewText(R.id.air_label, aqi?.let { Loc.tr(Fmt.aqiLabel(it)) } ?: Loc.tr("unavailable"))
        val uv = data.hourly.firstOrNull()?.uvIndex
        views.setTextViewText(R.id.uv_value, uv?.let { String.format(Locale.US, "%.1f", it) } ?: "—")
        views.setTextViewText(R.id.uv_label, uv?.let { Loc.tr(Fmt.uvLabel(it)) } ?: Loc.tr("unavailable"))
        views.setTextViewText(R.id.wind_value, "${Fmt.wind(data.current.windSpeed, s.windUnit)}")
        views.setTextViewText(R.id.wind_label, "${s.windUnit.symbol} ${Fmt.compass(data.current.windDirection)}")
        views.setTextViewText(R.id.hum_value, "${data.current.humidity}%")
        views.setTextViewText(R.id.hum_label, Loc.tr(Fmt.humidityLabel(data.current.humidity)))
        views.setTextViewText(R.id.air_title, Loc.tr("Air") + " · EAQI")
        views.setTextViewText(R.id.uv_title, Loc.tr("UV index"))
        views.setTextViewText(R.id.wind_title, Loc.tr("Wind"))
        views.setTextViewText(R.id.hum_title, Loc.tr("Humidity"))
        views.sizes(
            scale,
            R.id.widget_location to 13f,
            R.id.widget_updated to 10f,
            R.id.air_value to 26f,
            R.id.uv_value to 26f,
            R.id.wind_value to 26f,
            R.id.hum_value to 26f,
            R.id.air_label to 11f,
            R.id.uv_label to 11f,
            R.id.wind_label to 11f,
            R.id.hum_label to 11f,
            R.id.air_title to 10f,
            R.id.uv_title to 10f,
            R.id.wind_title to 10f,
            R.id.hum_title to 10f
        )
        views.colors(W_TEXT, R.id.widget_location)
        views.colors(W_MUTED, R.id.widget_updated, R.id.air_title, R.id.uv_title, R.id.wind_title, R.id.hum_title)
        views.colors(
            W_SECONDARY,
            R.id.air_label, R.id.uv_label, R.id.wind_label, R.id.hum_label
        )
        views.colors(aqi?.let { Fmt.aqiColor(it).toArgb() } ?: W_TEXT, R.id.air_value)
        views.colors(uv?.let { Fmt.uvColor(it).toArgb() } ?: W_TEXT, R.id.uv_value)
        views.colors(W_TEXT, R.id.wind_value, R.id.hum_value)
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        manager.updateAppWidget(ids, views)
    }

    fun renderCompact(context: Context, manager: AppWidgetManager, ids: IntArray, data: WeatherData, s: AtmosSettings) {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        val c = data.current
        val scale = textScale(s)
        val accent = accentArgb(context, s)
        val rain = data.hourly.take(12).mapNotNull { it.precipProbability }.maxOrNull() ?: 0
        val today = data.daily.firstOrNull()
        views.setTextViewText(R.id.widget_location, data.place.name)
        views.setTextViewText(R.id.widget_updated, Fmt.relativeUpdate(data.fetchedAtMillis))
        views.setTextViewText(R.id.widget_temp, Fmt.temp(c.temperature, s.temperatureUnit))
        views.setTextViewText(R.id.widget_unit, s.temperatureUnit.symbol)
        views.setTextViewText(R.id.widget_condition, Loc.tr(com.alican.atmos.core.WeatherKindCodes.label(c.weatherCode, c.isDay)))
        views.setTextViewText(
            R.id.widget_meta,
            Loc.t(
                "H {}°  L {}°",
                Fmt.temp(today?.tempMax ?: c.temperature, s.temperatureUnit),
                Fmt.temp(today?.tempMin ?: c.temperature, s.temperatureUnit)
            )
        )
        views.setTextViewText(R.id.widget_rain, Loc.t("Rain {}%", rain))
        views.setImageViewResource(R.id.widget_icon, iconRes(WeatherKind.fromCode(c.weatherCode), c.isDay))
        views.sizes(
            scale,
            R.id.widget_location to 13f,
            R.id.widget_updated to 10f,
            R.id.widget_temp to 40f,
            R.id.widget_unit to 14f,
            R.id.widget_condition to 13f,
            R.id.widget_meta to 11f,
            R.id.widget_rain to 11f
        )
        views.colors(W_TEXT, R.id.widget_location, R.id.widget_temp, R.id.widget_condition)
        views.colors(W_MUTED, R.id.widget_updated)
        views.colors(W_SECONDARY, R.id.widget_meta)
        views.colors(accent, R.id.widget_unit, R.id.widget_rain)
        views.setInt(R.id.widget_rain_icon, "setColorFilter", accent)
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        manager.updateAppWidget(ids, views)
    }

    fun renderStrip(context: Context, manager: AppWidgetManager, ids: IntArray, data: WeatherData, s: AtmosSettings) {
        val views = RemoteViews(context.packageName, R.layout.widget_strip)
        val c = data.current
        val scale = textScale(s)
        val accent = accentArgb(context, s)
        views.setTextViewText(R.id.widget_temp, Fmt.temp(c.temperature, s.temperatureUnit))
        views.setTextViewText(R.id.widget_unit, s.temperatureUnit.symbol)
        views.setTextViewText(R.id.widget_condition, Loc.tr(com.alican.atmos.core.WeatherKindCodes.label(c.weatherCode, c.isDay)))
        views.setTextViewText(R.id.widget_location, data.place.name)
        val outlook = com.alican.atmos.notify.Nowcast.outlook(data)
        val rainText = when {
            outlook?.rainingNow == true -> Loc.tr("raining")
            outlook?.startsInMinutes != null && outlook.startsInMinutes <= 120 ->
                Loc.t("in {} min", outlook.startsInMinutes)
            else -> {
                val prob = data.hourly.take(12).mapNotNull { it.precipProbability }.maxOrNull() ?: 0
                Loc.t("Rain {}%", prob)
            }
        }
        views.setTextViewText(R.id.widget_rain, rainText)
        views.setTextViewText(R.id.widget_updated, Fmt.relativeUpdate(data.fetchedAtMillis))
        views.setImageViewResource(R.id.widget_icon, iconRes(WeatherKind.fromCode(c.weatherCode), c.isDay))
        views.sizes(
            scale,
            R.id.widget_temp to 30f,
            R.id.widget_unit to 13f,
            R.id.widget_condition to 13f,
            R.id.widget_location to 11f,
            R.id.widget_rain to 14f,
            R.id.widget_updated to 10f
        )
        views.colors(W_TEXT, R.id.widget_temp, R.id.widget_condition)
        views.colors(W_SECONDARY, R.id.widget_location)
        views.colors(W_MUTED, R.id.widget_updated)
        views.colors(accent, R.id.widget_unit, R.id.widget_rain)
        views.setInt(R.id.widget_rain_icon, "setColorFilter", accent)
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent(context))
        manager.updateAppWidget(ids, views)
    }

    private fun textScale(s: AtmosSettings): Float = if (s.largerText) 1.12f else 1f

    private fun RemoteViews.sizes(scale: Float, vararg pairs: Pair<Int, Float>) {
        pairs.forEach { (id, sp) ->
            setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sp * scale)
        }
    }

    private fun RemoteViews.colors(color: Int, vararg ids: Int) {
        ids.forEach { setTextColor(it, color) }
    }

    private fun accentArgb(context: Context, settings: AtmosSettings): Int {
        if (!settings.systemAccent || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            return W_ICE
        }
        return runCatching { context.getColor(android.R.color.system_accent1_200) }
            .getOrDefault(W_ICE)
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun iconRes(kind: WeatherKind, isDay: Boolean): Int = when (kind) {
        WeatherKind.CLEAR -> if (isDay) R.drawable.ic_w_sun else R.drawable.ic_w_moon
        WeatherKind.MOSTLY_CLEAR, WeatherKind.PARTLY_CLOUDY ->
            if (isDay) R.drawable.ic_w_partly else R.drawable.ic_w_partly_night
        WeatherKind.OVERCAST -> R.drawable.ic_w_cloud
        WeatherKind.FOG -> R.drawable.ic_w_fog
        WeatherKind.DRIZZLE, WeatherKind.RAIN, WeatherKind.FREEZING_RAIN -> R.drawable.ic_w_rain
        WeatherKind.HEAVY_RAIN -> R.drawable.ic_w_heavy_rain
        WeatherKind.SNOW, WeatherKind.SLEET -> R.drawable.ic_w_snow
        WeatherKind.THUNDER, WeatherKind.HAIL -> R.drawable.ic_w_storm
    }

    fun schedule(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = WidgetRefreshReceiver.ACTION
        }
        val pending = PendingIntent.getBroadcast(
            context, 7, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + 30 * 60_000L,
            30 * 60_000L,
            pending
        )
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = WidgetRefreshReceiver.ACTION
        }
        val pending = PendingIntent.getBroadcast(
            context, 7, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.cancel(pending)
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(
            AtmosNowWidget::class.java,
            AtmosForecastWidget::class.java,
            AtmosDetailWidget::class.java,
            AtmosCompactWidget::class.java,
            AtmosStripWidget::class.java
        ).any { manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty() }
    }
}

abstract class BaseWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        AtmosWidgets.refreshAll(context)
        val pending = goAsync()
        widgetScope.launch {
            try {
                AtmosWidgets.refreshFromNetworkIfStale(context)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AtmosWidgets.schedule(context)
        AtmosWidgets.refreshAll(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!AtmosWidgets.hasWidgets(context)) AtmosWidgets.cancel(context)
    }
}

class AtmosNowWidget : BaseWidgetProvider()
class AtmosForecastWidget : BaseWidgetProvider()
class AtmosDetailWidget : BaseWidgetProvider()
class AtmosCompactWidget : BaseWidgetProvider()
class AtmosStripWidget : BaseWidgetProvider()

class WidgetRefreshReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        widgetScope.launch {
            try {
                AtmosWidgets.refreshFromNetworkIfStale(context)
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        const val ACTION = "com.alican.atmos.WIDGET_REFRESH"
    }
}

@Suppress("unused")
private fun unused(value: ForecastModel, unit: PrecipUnit, temp: TempUnit) = value.name + unit.name + temp.name
