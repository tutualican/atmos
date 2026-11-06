package com.alican.atmos

import com.alican.atmos.core.CachedBundle
import com.alican.atmos.core.ForecastModel
import com.alican.atmos.core.Place
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.data.GeoMath
import com.alican.atmos.data.OpenMeteo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject
import kotlin.math.abs

class WeatherParsingTest {

    private val place = Place("test", "Istanbul", "Istanbul", "Türkiye", 41.01, 28.97)

    private fun payload(): String {
        val root = JSONObject()
        root.put("latitude", 41.02)
        root.put("longitude", 28.94)
        root.put("utc_offset_seconds", 10800)
        root.put("timezone_abbreviation", "GMT+3")
        root.put("elevation", 36.0)
        root.put("generationtime_ms", 0.42)
        root.put(
            "current",
            JSONObject()
                .put("time", "2026-11-06T02:30")
                .put("temperature_2m", 9.5)
                .put("relative_humidity_2m", 81)
                .put("apparent_temperature", 7.9)
                .put("is_day", 0)
                .put("precipitation", 0.4)
                .put("rain", 0.4)
                .put("showers", 0.0)
                .put("snowfall", 0.0)
                .put("weather_code", 63)
                .put("cloud_cover", 92)
                .put("pressure_msl", 1009.2)
                .put("wind_speed_10m", 4.8)
                .put("wind_direction_10m", 245)
                .put("wind_gusts_10m", 9.1)
        )
        val hourlyTimes = listOf("2026-11-06T02:00", "2026-11-06T03:00", "2026-11-06T04:00")
        root.put(
            "hourly",
            JSONObject()
                .put("time", org.json.JSONArray(hourlyTimes))
                .put("temperature_2m", org.json.JSONArray(listOf(9.4, 9.2, 8.8)))
                .put("apparent_temperature", org.json.JSONArray(listOf(8.0, 7.8, 7.4)))
                .put("relative_humidity_2m", org.json.JSONArray(listOf(81, 84, 86)))
                .put("precipitation_probability", org.json.JSONArray(listOf(55, 60, null)))
                .put("precipitation", org.json.JSONArray(listOf(0.2, 0.6, 0.0)))
                .put("weather_code", org.json.JSONArray(listOf(61, 63, 3)))
                .put("wind_speed_10m", org.json.JSONArray(listOf(4.8, 5.1, 4.2)))
                .put("wind_direction_10m", org.json.JSONArray(listOf(245, 250, 240)))
                .put("wind_gusts_10m", org.json.JSONArray(listOf(9.1, 10.0, 8.2)))
                .put("uv_index", org.json.JSONArray(listOf(0.0, 0.0, 0.0)))
                .put("visibility", org.json.JSONArray(listOf(12000.0, 8000.0, 15000.0)))
                .put("dew_point_2m", org.json.JSONArray(listOf(6.4, 6.7, 6.4)))
                .put("pressure_msl", org.json.JSONArray(listOf(1009.2, 1008.8, 1008.4)))
                .put("cloud_cover", org.json.JSONArray(listOf(92, 95, 88)))
                .put("is_day", org.json.JSONArray(listOf(0, 0, 0)))
                .put("snowfall", org.json.JSONArray(listOf(0.0, 0.0, 0.0)))
        )
        root.put(
            "daily",
            JSONObject()
                .put("time", org.json.JSONArray(listOf("2026-11-06")))
                .put("weather_code", org.json.JSONArray(listOf(63)))
                .put("temperature_2m_max", org.json.JSONArray(listOf(12.3)))
                .put("temperature_2m_min", org.json.JSONArray(listOf(8.1)))
                .put("apparent_temperature_max", org.json.JSONArray(listOf(11.0)))
                .put("apparent_temperature_min", org.json.JSONArray(listOf(6.5)))
                .put("sunrise", org.json.JSONArray(listOf("2026-11-06T07:41")))
                .put("sunset", org.json.JSONArray(listOf("2026-11-06T17:52")))
                .put("uv_index_max", org.json.JSONArray(listOf(1.6)))
                .put("precipitation_sum", org.json.JSONArray(listOf(8.4)))
                .put("rain_sum", org.json.JSONArray(listOf(8.4)))
                .put("snowfall_sum", org.json.JSONArray(listOf(0.0)))
                .put("precipitation_hours", org.json.JSONArray(listOf(7.0)))
                .put("precipitation_probability_max", org.json.JSONArray(listOf(80)))
                .put("wind_speed_10m_max", org.json.JSONArray(listOf(7.4)))
                .put("wind_gusts_10m_max", org.json.JSONArray(listOf(13.0)))
                .put("wind_direction_10m_dominant", org.json.JSONArray(listOf(240)))
        )
        root.put(
            "minutely_15",
            JSONObject()
                .put("time", org.json.JSONArray(listOf("2026-11-06T02:30", "2026-11-06T02:45")))
                .put("precipitation", org.json.JSONArray(listOf(0.1, 0.2)))
                .put("precipitation_probability", org.json.JSONArray(listOf(62, 68)))
                .put("weather_code", org.json.JSONArray(listOf(63, 63)))
        )
        return root.toString()
    }

    @Test
    fun `parses current, hourly and daily without inventing values`() {
        val data = OpenMeteo.parse(CachedBundle(place, payload(), null, null, 1_700_000_000_000, "best_match"))

        assertEquals(9.5, data.current.temperature, 0.0001)
        assertEquals(81, data.current.humidity)
        assertEquals(0, data.current.isDay.compareTo(false))
        assertEquals(3, data.hourly.size)
        assertEquals(1, data.daily.size)
        assertEquals(2, data.minutely.size)
        assertEquals("GMT+3", data.timezoneAbbreviation)
        assertEquals(10800, data.timezoneOffsetSeconds)
        assertEquals(WeatherKind.RAIN, data.kind)

        // Probability must stay distinct from amount, and null must stay null.
        assertEquals(55, data.hourly[0].precipProbability)
        assertNull(data.hourly[2].precipProbability)
        assertEquals(0.6, data.hourly[1].precipitation, 0.0001)
        assertEquals(8.4, data.daily[0].precipitationSum, 0.0001)
        assertEquals(80, data.daily[0].precipProbabilityMax)
    }

    @Test
    fun `ambiguous weather codes map safely`() {
        assertEquals(WeatherKind.THUNDER, WeatherKind.fromCode(95))
        assertEquals(WeatherKind.HAIL, WeatherKind.fromCode(96))
        assertEquals(WeatherKind.SNOW, WeatherKind.fromCode(75))
        assertEquals(WeatherKind.FOG, WeatherKind.fromCode(48))
        assertEquals(WeatherKind.OVERCAST, WeatherKind.fromCode(199))
        assertTrue(WeatherKind.THUNDER.isStorm)
    }

    @Test
    fun `web mercator round trip is stable`() {
        val zoom = 7.0
        val x = GeoMath.lonToWorldX(28.97, zoom)
        val y = GeoMath.latToWorldY(41.01, zoom)
        assertEquals(28.97, GeoMath.worldXToLon(x, zoom), 1e-9)
        assertEquals(41.01, GeoMath.worldYToLat(y, zoom), 1e-9)
        assertTrue(abs(GeoMath.latToWorldY(41.01, zoom) - GeoMath.latToWorldY(41.02, zoom)) > 0)
    }

    @Test
    fun `model identifiers are distinct and usable`() {
        val values = ForecastModel.entries.map { it.apiValue }
        assertEquals(values.size, values.distinct().size)
        assertTrue(values.contains("ecmwf_ifs025"))
    }
}
