package com.alican.atmos.core

import java.time.LocalDate
import java.time.LocalDateTime

data class Place(
    val id: String,
    val name: String,
    val admin: String?,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val timezone: String? = null,
    val isCurrentLocation: Boolean = false
) {
    val subtitle: String
        get() = listOfNotNull(admin?.takeIf { it.isNotBlank() }, country?.takeIf { it.isNotBlank() })
            .joinToString(", ")
}

enum class WeatherKind {
    CLEAR, MOSTLY_CLEAR, PARTLY_CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, HEAVY_RAIN,
    FREEZING_RAIN, SNOW, SLEET, THUNDER, HAIL;

    val isWet: Boolean
        get() = this in setOf(DRIZZLE, RAIN, HEAVY_RAIN, FREEZING_RAIN, SLEET, THUNDER, HAIL, SNOW)

    val isStorm: Boolean get() = this == THUNDER || this == HAIL

    companion object {
        fun fromCode(code: Int): WeatherKind = when (code) {
            0 -> CLEAR
            1 -> MOSTLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63 -> RAIN
            65, 80, 81, 82 -> HEAVY_RAIN
            66, 67 -> FREEZING_RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95 -> THUNDER
            96, 99 -> HAIL
            else -> OVERCAST
        }
    }
}

data class CurrentConditions(
    val time: LocalDateTime,
    val temperature: Double,
    val apparent: Double,
    val humidity: Int,
    val isDay: Boolean,
    val precipitation: Double,
    val rain: Double,
    val showers: Double,
    val snowfall: Double,
    val weatherCode: Int,
    val cloudCover: Int,
    val pressureMsl: Double,
    val windSpeed: Double,
    val windDirection: Int,
    val windGusts: Double
)

data class HourPoint(
    val time: LocalDateTime,
    val temperature: Double,
    val apparent: Double,
    val humidity: Int,
    val precipProbability: Int?,
    val precipitation: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val windGusts: Double,
    val uvIndex: Double?,
    val visibility: Double?,
    val dewPoint: Double?,
    val pressureMsl: Double?,
    val cloudCover: Int?,
    val isDay: Boolean,
    val snowfall: Double
)

data class DayPoint(
    val date: LocalDate,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val apparentMax: Double?,
    val apparentMin: Double?,
    val sunrise: LocalDateTime?,
    val sunset: LocalDateTime?,
    val uvMax: Double?,
    val precipitationSum: Double,
    val rainSum: Double,
    val snowfallSum: Double,
    val precipitationHours: Double?,
    val precipProbabilityMax: Int?,
    val windMax: Double?,
    val gustMax: Double?,
    val windDirection: Int?
)

data class MinutePoint(
    val time: LocalDateTime,
    val precipitation: Double,
    val probability: Int?,
    val weatherCode: Int?
)

data class AirQuality(
    val europeanAqi: Int?,
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?,
    val no2: Double?,
    val so2: Double?,
    val co: Double?,
    val time: LocalDateTime?
)

/** P10..P90 spread of ECMWF ensemble members for the next 48 h. */
data class Uncertainty(
    val times: List<LocalDateTime>,
    val p10: List<Double>,
    val p50: List<Double>,
    val p90: List<Double>,
    val members: Int
)

data class WeatherData(
    val place: Place,
    val current: CurrentConditions,
    val hourly: List<HourPoint>,
    val daily: List<DayPoint>,
    val minutely: List<MinutePoint>,
    val air: AirQuality?,
    val uncertainty: Uncertainty?,
    val modelLabel: String,
    val fetchedAtMillis: Long,
    val timezoneAbbreviation: String?,
    val timezoneOffsetSeconds: Int,
    val elevation: Double?,
    val generationTimeMs: Double?
) {
    val kind: WeatherKind get() = WeatherKind.fromCode(current.weatherCode)
}

/** Raw API payloads cached together so offline mode is a faithful replay, not invented data. */
data class CachedBundle(
    val place: Place,
    val payload: String,
    val airPayload: String?,
    val ensemblePayload: String?,
    val fetchedAt: Long,
    val model: String
)

data class AtmosSettings(
    val temperatureUnit: TempUnit = TempUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val precipitationUnit: PrecipUnit = PrecipUnit.MM,
    val model: ForecastModel = ForecastModel.BEST_MATCH,
    val animationQuality: AnimationQuality = AnimationQuality.AUTO,
    val lightningHaptics: Boolean = true,
    val reduceMotion: Boolean = false,
    val useLocation: Boolean = true,
    val refreshMinutes: Int = 15,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val rainAlerts: Boolean = false,
    val systemAccent: Boolean = false,
    val largerText: Boolean = false
)

/** Languages shipped with this build. Adding a locale means adding a strings table. */
enum class AppLanguage(val tag: String, val nativeName: String, val englishName: String) {
    SYSTEM("system", "System default", "System default"),
    ENGLISH("en", "English", "English"),
    TURKISH("tr", "Türkçe", "Turkish"),
    GERMAN("de", "Deutsch", "German"),
    SPANISH("es", "Español", "Spanish"),
    FRENCH("fr", "Français", "French");

    companion object {
        fun fromTag(tag: String): AppLanguage? =
            entries.firstOrNull { it != SYSTEM && it.tag.equals(tag, ignoreCase = true) }
    }
}

enum class TempUnit(val symbol: String) { CELSIUS("°C"), FAHRENHEIT("°F") }
enum class WindUnit(val symbol: String) { KMH("km/h"), MS("m/s"), MPH("mph"), KNOTS("kn") }
enum class PressureUnit(val symbol: String) { HPA("hPa"), INHG("inHg") }
enum class PrecipUnit(val symbol: String) { MM("mm"), INCH("in") }

enum class ForecastModel(val apiValue: String, val label: String) {
    BEST_MATCH("best_match", "Best match"),
    ECMWF("ecmwf_ifs025", "ECMWF IFS 0.25°"),
    ICON("icon_seamless", "DWD ICON"),
    GFS("gfs_seamless", "NOAA GFS"),
    METEOFRANCE("meteofrance_seamless", "Météo-France AROME")
}

enum class AnimationQuality(val label: String) {
    AUTO("Adaptive"), HIGH("High"), BALANCED("Balanced"), LOW("Battery saver")
}

object WeatherKindCodes {
    fun label(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "Clear sky" else "Clear night"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45 -> "Fog"
        48 -> "Freezing fog"
        51 -> "Light drizzle"
        53 -> "Drizzle"
        55 -> "Dense drizzle"
        56 -> "Light freezing drizzle"
        57 -> "Freezing drizzle"
        61 -> "Light rain"
        63 -> "Rain"
        65 -> "Heavy rain"
        66 -> "Light freezing rain"
        67 -> "Freezing rain"
        71 -> "Light snow"
        73 -> "Snow"
        75 -> "Heavy snow"
        77 -> "Snow grains"
        80 -> "Light showers"
        81 -> "Showers"
        82 -> "Violent showers"
        85 -> "Snow showers"
        86 -> "Heavy snow showers"
        95 -> "Thunderstorm"
        96 -> "Thunderstorm with hail"
        99 -> "Severe thunderstorm with hail"
        else -> "Unknown conditions"
    }
}
