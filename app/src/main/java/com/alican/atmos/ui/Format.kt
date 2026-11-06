package com.alican.atmos.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.PrecipUnit
import com.alican.atmos.core.PressureUnit
import com.alican.atmos.core.TempUnit
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.core.WindUnit
import com.alican.atmos.i18n.Loc
import com.alican.atmos.ui.components.AtmosIcons
import com.alican.atmos.ui.theme.Atmos
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Fmt {

    fun temp(value: Double, unit: TempUnit): String {
        if (!value.isFinite()) return "—"
        val v = if (unit == TempUnit.FAHRENHEIT) value * 9 / 5 + 32 else value
        return v.roundToInt().toString()
    }

    fun tempSigned(value: Double, unit: TempUnit): String {
        if (!value.isFinite()) return "—"
        val v = if (unit == TempUnit.FAHRENHEIT) value * 9 / 5 + 32 else value
        return (if (v > 0) "+" else "") + v.roundToInt()
    }

    fun tempWithUnit(value: Double, unit: TempUnit): String = "${temp(value, unit)}${unit.symbol}"

    fun wind(ms: Double, unit: WindUnit): String {
        if (!ms.isFinite()) return "—"
        val v = when (unit) {
            WindUnit.KMH -> ms * 3.6
            WindUnit.MS -> ms
            WindUnit.MPH -> ms * 2.236936
            WindUnit.KNOTS -> ms * 1.943844
        }
        return if (v < 10) String.format(Locale.US, "%.1f", v) else v.roundToInt().toString()
    }

    fun pressure(hPa: Double?, unit: PressureUnit): String? {
        if (hPa == null || !hPa.isFinite() || hPa <= 0) return null
        return when (unit) {
            PressureUnit.HPA -> hPa.roundToInt().toString()
            PressureUnit.INHG -> String.format(Locale.US, "%.2f", hPa / 33.8639)
        }
    }

    fun precip(mm: Double, unit: PrecipUnit): String {
        if (!mm.isFinite()) return "—"
        val v = if (unit == PrecipUnit.INCH) mm / 25.4 else mm
        return when {
            v == 0.0 -> "0"
            v < 0.1 -> if (unit == PrecipUnit.INCH) "<0.01" else "<0.1"
            v < 10 -> String.format(Locale.US, "%.1f", v)
            else -> v.roundToInt().toString()
        }
    }

    fun visibility(meters: Double?, imperial: Boolean): String? {
        if (meters == null) return null
        val km = meters / 1000.0
        return if (imperial) {
            val mi = km / 1.609344
            if (mi >= 10) String.format(Locale.US, "%.0f mi", mi)
            else String.format(Locale.US, "%.1f mi", mi)
        } else {
            if (km >= 10) String.format(Locale.US, "%.0f km", km)
            else String.format(Locale.US, "%.1f km", km)
        }
    }

    fun clock(t: LocalDateTime, is24h: Boolean = true): String =
        t.format(if (is24h) DateTimeFormatter.ofPattern("HH:mm") else DateTimeFormatter.ofPattern("h:mm a", Loc.javaLocale))

    fun hour(t: LocalDateTime, is24h: Boolean = true): String =
        if (is24h) t.format(DateTimeFormatter.ofPattern("HH")) + ":00"
        else t.format(DateTimeFormatter.ofPattern("h a", Loc.javaLocale))

    fun hourShort(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("HH"))

    fun weekday(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("EEE", Loc.javaLocale))

    fun dayMonth(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("EEE, d MMM", Loc.javaLocale))

    fun dayNum(t: LocalDateTime): String = t.format(DateTimeFormatter.ofPattern("d", Loc.javaLocale))

    fun relativeUpdate(millis: Long): String {
        val minutes = ((System.currentTimeMillis() - millis) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 60 * 24 -> "${minutes / 60}h ago"
            else -> "${minutes / (60 * 24)}d ago"
        }
    }

    fun compass(degrees: Int): String {
        val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val idx = (((degrees % 360) + 360) % 360 / 22.5).roundToInt() % 16
        return dirs[idx]
    }

    fun isImperial(settings: AtmosSettings): Boolean = settings.temperatureUnit == TempUnit.FAHRENHEIT

    fun aqiLabel(aqi: Int): String = when {
        aqi <= 20 -> "Good"
        aqi <= 40 -> "Fair"
        aqi <= 60 -> "Moderate"
        aqi <= 80 -> "Poor"
        aqi <= 100 -> "Very poor"
        else -> "Extremely poor"
    }

    fun aqiColor(aqi: Int): Color = when {
        aqi <= 20 -> Atmos.Good
        aqi <= 40 -> Color(0xFFB9C7A3)
        aqi <= 60 -> Atmos.Gold
        aqi <= 80 -> Color(0xFFDD9F72)
        aqi <= 100 -> Atmos.Danger
        else -> Color(0xFFB85C6A)
    }

    fun uvLabel(uv: Double): String = when {
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very high"
        else -> "Extreme"
    }

    fun uvColor(uv: Double): Color = when {
        uv < 3 -> Atmos.Good
        uv < 6 -> Atmos.Gold
        uv < 8 -> Color(0xFFDD9F72)
        uv < 11 -> Atmos.Danger
        else -> Color(0xFFAA6BC4)
    }

    fun humidityLabel(h: Int): String = when {
        h < 30 -> "Dry"
        h < 60 -> "Comfortable"
        h < 80 -> "Humid"
        else -> "Very humid"
    }

    fun pressureTrend(hourly: List<com.alican.atmos.core.HourPoint>): String {
        if (hourly.size < 4) return "—"
        val now = hourly.firstOrNull()?.pressureMsl ?: return "—"
        val later = hourly.getOrNull(3)?.pressureMsl ?: return "—"
        val d = later - now
        return when {
            d > 1.0 -> "Rising"
            d < -1.0 -> "Falling"
            else -> "Steady"
        }
    }

    fun windDescription(ms: Double): String = when {
        ms < 0.5 -> "Calm"
        ms < 1.6 -> "Light air"
        ms < 3.4 -> "Light breeze"
        ms < 5.5 -> "Gentle breeze"
        ms < 8.0 -> "Moderate breeze"
        ms < 10.8 -> "Fresh breeze"
        ms < 13.9 -> "Strong breeze"
        ms < 17.2 -> "Near gale"
        ms < 20.8 -> "Gale"
        ms < 24.5 -> "Strong gale"
        ms < 28.5 -> "Storm"
        else -> "Violent storm"
    }

    fun kindIcon(kind: WeatherKind, isDay: Boolean): ImageVector = when (kind) {
        WeatherKind.CLEAR -> if (isDay) AtmosIcons.Sun else AtmosIcons.Moon
        WeatherKind.MOSTLY_CLEAR -> if (isDay) AtmosIcons.PartlyCloudy else AtmosIcons.PartlyCloudyNight
        WeatherKind.PARTLY_CLOUDY -> if (isDay) AtmosIcons.PartlyCloudy else AtmosIcons.PartlyCloudyNight
        WeatherKind.OVERCAST -> AtmosIcons.Cloud
        WeatherKind.FOG -> AtmosIcons.CloudFog
        WeatherKind.DRIZZLE -> AtmosIcons.Droplet
        WeatherKind.RAIN -> AtmosIcons.CloudRain
        WeatherKind.HEAVY_RAIN -> AtmosIcons.CloudRain
        WeatherKind.FREEZING_RAIN -> AtmosIcons.CloudRain
        WeatherKind.SNOW -> AtmosIcons.CloudSnow
        WeatherKind.SLEET -> AtmosIcons.CloudSnow
        WeatherKind.THUNDER -> AtmosIcons.CloudLightning
        WeatherKind.HAIL -> AtmosIcons.CloudLightning
    }

    fun shortCondition(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 66, 67 -> "Rain"
        65 -> "Heavy rain"
        71, 73, 75, 77 -> "Snow"
        80, 81 -> "Showers"
        82 -> "Heavy showers"
        85, 86 -> "Snow showers"
        95, 96, 99 -> "Thunderstorm"
        else -> "—"
    }

    fun partOfDay(): LocalDateTime = LocalDateTime.now()

    fun minutesText(minutes: Int): String = when {
        minutes <= 0 -> "now"
        minutes < 60 -> "${minutes} min"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    fun delta(a: Double, b: Double): String {
        val d = a - b
        return if (abs(d) < 0.5) "—" else (if (d > 0) "+" else "") + d.roundToInt()
    }

    fun hourLabel(t: LocalTime): String = t.format(DateTimeFormatter.ofPattern("HH:mm"))
}
