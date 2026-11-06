package com.alican.atmos.data

import com.alican.atmos.core.AirQuality
import com.alican.atmos.core.CachedBundle
import com.alican.atmos.core.CurrentConditions
import com.alican.atmos.core.DayPoint
import com.alican.atmos.core.ForecastModel
import com.alican.atmos.core.HourPoint
import com.alican.atmos.core.MinutePoint
import com.alican.atmos.core.Place
import com.alican.atmos.core.Uncertainty
import com.alican.atmos.core.WeatherData
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Open-Meteo is the single numerical backbone: one consistent model, explicit model selection,
 * explicit issue time. We never blend in unverified values and we never synthesise observations.
 */
object OpenMeteo {

    private const val USER_AGENT = "ATMOS/1.0 (Android; https://github.com/tutualican/atmos)"

    private fun get(url: String, timeoutMs: Int = 15_000): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw java.io.IOException("HTTP $code from ${url.substringBefore('?')}")
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun q(v: String): String = URLEncoder.encode(v, "UTF-8")

    fun fetchBundle(place: Place, model: ForecastModel): CachedBundle {
        val base = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.lat}&longitude=${place.lon}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,rain,showers,snowfall,weather_code,cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&minutely_15=precipitation,precipitation_probability,weather_code" +
            "&hourly=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,visibility,dew_point_2m,pressure_msl,cloud_cover,is_day,snowfall" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,uv_index_max,precipitation_sum,rain_sum,snowfall_sum,precipitation_hours,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant" +
            "&timezone=auto&forecast_days=7&forecast_hours=48&past_hours=1&wind_speed_unit=ms&precipitation_unit=mm" +
            "&models=${model.apiValue}"

        val airBase = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=${place.lat}&longitude=${place.lon}" +
            "&current=european_aqi,us_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone" +
            "&timezone=auto&forecast_days=1"

        val ensembleBase = "https://ensemble-api.open-meteo.com/v1/ensemble" +
            "?latitude=${place.lat}&longitude=${place.lon}" +
            "&hourly=temperature_2m&models=ecmwf_ifs025&forecast_days=2&timezone=auto"

        val payload = get(base)
        val air = runCatching { get(airBase, 12_000) }.getOrNull()
        val ensemble = runCatching { get(ensembleBase, 20_000) }.getOrNull()
        return CachedBundle(place, payload, air, ensemble, System.currentTimeMillis(), model.apiValue)
    }

    fun parse(bundle: CachedBundle): WeatherData {
        val root = JSONObject(bundle.payload)
        val offset = root.optInt("utc_offset_seconds", 0)
        val tzAbbr = root.optString("timezone_abbreviation").takeIf { it.isNotBlank() && it != "null" }

        val currentObj = root.getJSONObject("current")
        val current = CurrentConditions(
            time = parseTime(currentObj.getString("time")),
            temperature = currentObj.optDouble("temperature_2m"),
            apparent = currentObj.optDouble("apparent_temperature"),
            humidity = currentObj.optInt("relative_humidity_2m"),
            isDay = currentObj.optInt("is_day", 1) == 1,
            precipitation = currentObj.optDouble("precipitation", 0.0),
            rain = currentObj.optDouble("rain", 0.0),
            showers = currentObj.optDouble("showers", 0.0),
            snowfall = currentObj.optDouble("snowfall", 0.0),
            weatherCode = currentObj.optInt("weather_code"),
            cloudCover = currentObj.optInt("cloud_cover", 0),
            pressureMsl = currentObj.optDouble("pressure_msl"),
            windSpeed = currentObj.optDouble("wind_speed_10m"),
            windDirection = currentObj.optInt("wind_direction_10m"),
            windGusts = currentObj.optDouble("wind_gusts_10m")
        )

        val h = root.getJSONObject("hourly")
        val hourly = arrayOf(
            "temperature_2m", "apparent_temperature", "relative_humidity_2m",
            "precipitation_probability", "precipitation", "weather_code", "wind_speed_10m",
            "wind_direction_10m", "wind_gusts_10m", "uv_index", "visibility", "dew_point_2m",
            "pressure_msl", "cloud_cover", "is_day", "snowfall"
        ).associateWith { h.optJSONArray(it) }

        val times = h.getJSONArray("time")
        val hours = ArrayList<HourPoint>(times.length())
        for (i in 0 until times.length()) {
            hours += HourPoint(
                time = parseTime(times.getString(i)),
                temperature = hourly.dbl("temperature_2m", i),
                apparent = hourly.dbl("apparent_temperature", i),
                humidity = hourly.int("relative_humidity_2m", i, 0),
                precipProbability = hourly.intOrNull("precipitation_probability", i),
                precipitation = hourly.dbl("precipitation", i),
                weatherCode = hourly.int("weather_code", i, 0),
                windSpeed = hourly.dbl("wind_speed_10m", i),
                windDirection = hourly.int("wind_direction_10m", i, 0),
                windGusts = hourly.dbl("wind_gusts_10m", i),
                uvIndex = hourly.dblOrNull("uv_index", i),
                visibility = hourly.dblOrNull("visibility", i),
                dewPoint = hourly.dblOrNull("dew_point_2m", i),
                pressureMsl = hourly.dblOrNull("pressure_msl", i),
                cloudCover = hourly.intOrNull("cloud_cover", i),
                isDay = hourly.int("is_day", i, 1) == 1,
                snowfall = hourly.dbl("snowfall", i)
            )
        }

        val d = root.getJSONObject("daily")
        val days = ArrayList<DayPoint>(7)
        val dayTimes = d.getJSONArray("time")
        for (i in 0 until dayTimes.length()) {
            days += DayPoint(
                date = LocalDate.parse(dayTimes.getString(i)),
                weatherCode = d.getJSONArray("weather_code").optInt(i),
                tempMax = d.getJSONArray("temperature_2m_max").optDouble(i),
                tempMin = d.getJSONArray("temperature_2m_min").optDouble(i),
                apparentMax = d.optJSONArray("apparent_temperature_max")?.optDoubleOrNull(i),
                apparentMin = d.optJSONArray("apparent_temperature_min")?.optDoubleOrNull(i),
                sunrise = d.getJSONArray("sunrise").optString(i)?.takeIf { it.length > 5 }?.let(::parseTime),
                sunset = d.getJSONArray("sunset").optString(i)?.takeIf { it.length > 5 }?.let(::parseTime),
                uvMax = d.optJSONArray("uv_index_max")?.optDoubleOrNull(i),
                precipitationSum = d.getJSONArray("precipitation_sum").optDouble(i),
                rainSum = d.getJSONArray("rain_sum").optDouble(i),
                snowfallSum = d.optJSONArray("snowfall_sum")?.optDouble(i) ?: 0.0,
                precipitationHours = d.optJSONArray("precipitation_hours")?.optDoubleOrNull(i),
                precipProbabilityMax = d.optJSONArray("precipitation_probability_max")?.optIntOrNull(i),
                windMax = d.optJSONArray("wind_speed_10m_max")?.optDoubleOrNull(i),
                gustMax = d.optJSONArray("wind_gusts_10m_max")?.optDoubleOrNull(i),
                windDirection = d.optJSONArray("wind_direction_10m_dominant")?.optIntOrNull(i)
            )
        }

        val minutely = ArrayList<MinutePoint>()
        root.optJSONObject("minutely_15")?.let { m ->
            val mTimes = m.optJSONArray("time") ?: return@let
            for (i in 0 until mTimes.length()) {
                minutely += MinutePoint(
                    time = parseTime(mTimes.getString(i)),
                    precipitation = m.optJSONArray("precipitation")?.optDouble(i) ?: 0.0,
                    probability = m.optJSONArray("precipitation_probability")?.optIntOrNull(i),
                    weatherCode = m.optJSONArray("weather_code")?.optIntOrNull(i)
                )
            }
        }

        val air = bundle.airPayload?.let { runCatching { parseAir(it) }.getOrNull() }
        val ensemble = bundle.ensemblePayload?.let { runCatching { parseEnsemble(it) }.getOrNull() }

        return WeatherData(
            place = bundle.place,
            current = current,
            hourly = hours,
            daily = days,
            minutely = minutely,
            air = air,
            uncertainty = ensemble,
            modelLabel = ForecastModel.entries.firstOrNull { it.apiValue == bundle.model }?.label ?: bundle.model,
            fetchedAtMillis = bundle.fetchedAt,
            timezoneAbbreviation = tzAbbr,
            timezoneOffsetSeconds = offset,
            elevation = root.optDouble("elevation").takeIf { !it.isNaN() },
            generationTimeMs = root.optDouble("generationtime_ms").takeIf { !it.isNaN() }
        )
    }

    private fun parseAir(raw: String): AirQuality {
        val root = JSONObject(raw)
        val c = root.getJSONObject("current")
        return AirQuality(
            europeanAqi = c.optIntOrNull("european_aqi"),
            usAqi = c.optIntOrNull("us_aqi"),
            pm25 = c.optDoubleOrNull("pm2_5"),
            pm10 = c.optDoubleOrNull("pm10"),
            ozone = c.optDoubleOrNull("ozone"),
            no2 = c.optDoubleOrNull("nitrogen_dioxide"),
            so2 = c.optDoubleOrNull("sulphur_dioxide"),
            co = c.optDoubleOrNull("carbon_monoxide"),
            time = c.optString("time").takeIf { it.length > 5 }?.let(::parseTime)
        )
    }

    private fun parseEnsemble(raw: String): Uncertainty {
        val root = JSONObject(raw)
        val h = root.getJSONObject("hourly")
        val times = h.getJSONArray("time")
        val memberKeys = h.keys().asSequence()
            .filter { it.startsWith("temperature_2m") }
            .toList()
        if (memberKeys.isEmpty()) throw IllegalStateException("no ensemble members")
        val n = times.length()
        val p10 = ArrayList<Double>(n)
        val p50 = ArrayList<Double>(n)
        val p90 = ArrayList<Double>(n)
        val outTimes = ArrayList<LocalDateTime>(n)
        for (i in 0 until n) {
            val values = memberKeys.mapNotNull { key ->
                h.optJSONArray(key)?.optDoubleOrNull(i)
            }.sorted()
            if (values.size < 5) {
                outTimes += parseTime(times.getString(i))
                p10 += Double.NaN; p50 += Double.NaN; p90 += Double.NaN
                continue
            }
            fun pct(p: Double): Double {
                val idx = ((values.size - 1) * p).roundToInt().coerceIn(0, values.size - 1)
                return values[idx]
            }
            outTimes += parseTime(times.getString(i))
            p10 += pct(0.10); p50 += pct(0.50); p90 += pct(0.90)
        }
        return Uncertainty(outTimes, p10, p50, p90, memberKeys.size)
    }

    fun searchPlaces(query: String, language: String = "en"): List<Place> {
        if (query.trim().length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${q(query.trim())}" +
            "&count=12&language=$language&format=json"
        val root = JSONObject(get(url, 10_000))
        val results = root.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Place>(results.length())
        for (i in 0 until results.length()) {
            val o = results.getJSONObject(i)
            out += Place(
                id = "geo-${o.optLong("id")}",
                name = o.optString("name"),
                admin = o.optString("admin1").takeIf { it.isNotBlank() && it != "null" },
                country = o.optString("country").takeIf { it.isNotBlank() && it != "null" },
                lat = o.optDouble("latitude"),
                lon = o.optDouble("longitude"),
                timezone = o.optString("timezone").takeIf { it.isNotBlank() && it != "null" }
            )
        }
        return out
    }

    private fun parseTime(s: String): LocalDateTime = LocalDateTime.parse(s)

    private fun JSONArray.optDoubleOrNull(i: Int): Double? {
        if (i !in 0 until length() || isNull(i)) return null
        val v = optDouble(i)
        return if (v.isNaN()) null else v
    }

    private fun JSONArray.optIntOrNull(i: Int): Int? {
        if (i !in 0 until length() || isNull(i)) return null
        return optInt(i)
    }

    private fun Map<String, JSONArray?>.dbl(key: String, i: Int): Double =
        this[key]?.optDoubleOrNull(i) ?: 0.0

    private fun Map<String, JSONArray?>.dblOrNull(key: String, i: Int): Double? =
        this[key]?.optDoubleOrNull(i)

    private fun Map<String, JSONArray?>.int(key: String, i: Int, fallback: Int): Int =
        this[key]?.optIntOrNull(i) ?: fallback

    private fun Map<String, JSONArray?>.intOrNull(key: String, i: Int): Int? =
        this[key]?.optIntOrNull(i)

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return optInt(key)
    }
}
