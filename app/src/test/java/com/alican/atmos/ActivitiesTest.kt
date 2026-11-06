package com.alican.atmos

import com.alican.atmos.core.Activities
import com.alican.atmos.core.CurrentConditions
import com.alican.atmos.core.OutdoorActivity
import com.alican.atmos.core.Place
import com.alican.atmos.core.WeatherData
import com.alican.atmos.core.WeatherKind
import com.alican.atmos.core.HourPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ActivitiesTest {

    private val place = Place("test", "Istanbul", "Istanbul", "Türkiye", 41.01, 28.97)
    private val start: LocalDateTime = LocalDateTime.of(2026, 7, 14, 12, 0)

    private fun data(
        apparent: Double,
        wind: Double,
        gusts: Double,
        code: Int,
        probability: Int,
        precipitation: Double,
        uv: Double,
        isDay: Boolean = true,
        aqi: Int? = null
    ): WeatherData {
        val current = CurrentConditions(
            time = start,
            temperature = apparent,
            apparent = apparent,
            humidity = 50,
            isDay = isDay,
            precipitation = precipitation,
            rain = precipitation,
            showers = 0.0,
            snowfall = 0.0,
            weatherCode = code,
            cloudCover = 20,
            pressureMsl = 1015.0,
            windSpeed = wind,
            windDirection = 180,
            windGusts = gusts
        )
        val hours = (0..5).map { offset ->
            HourPoint(
                time = start.plusHours(offset.toLong()),
                temperature = apparent,
                apparent = apparent,
                humidity = 50,
                precipProbability = probability,
                precipitation = precipitation,
                weatherCode = code,
                windSpeed = wind,
                windDirection = 180,
                windGusts = gusts,
                uvIndex = uv,
                visibility = 20_000.0,
                dewPoint = 5.0,
                pressureMsl = 1015.0,
                cloudCover = 20,
                isDay = isDay,
                snowfall = if (code in 71..77 || code in 85..86) 1.0 else 0.0
            )
        }
        return WeatherData(
            place = place,
            current = current,
            hourly = hours,
            daily = emptyList(),
            minutely = emptyList(),
            air = if (aqi == null) null else com.alican.atmos.core.AirQuality(
                europeanAqi = aqi,
                usAqi = null,
                pm25 = 5.0,
                pm10 = null,
                ozone = null,
                no2 = null,
                so2 = null,
                co = null,
                time = start
            ),
            uncertainty = null,
            modelLabel = "test",
            fetchedAtMillis = 1_700_000_000_000,
            timezoneAbbreviation = "GMT+3",
            timezoneOffsetSeconds = 10800,
            elevation = 30.0,
            generationTimeMs = 1.0
        )
    }

    @Test
    fun `mild dry day is suitable for every activity`() {
        val result = Activities.evaluate(data(apparent = 18.0, wind = 3.0, gusts = 6.0, code = 0, probability = 0, precipitation = 0.0, uv = 4.0))
        assertEquals(3, result.size)
        result.forEach { assertTrue("${it.activity} score ${it.score}", it.score >= 80) }
        assertEquals("Great", result.first().status)
        assertEquals(listOf("Comfortable"), result.first().factors)
    }

    @Test
    fun `thunderstorm and heavy rain make all activities poor`() {
        val result = Activities.evaluate(data(apparent = 17.0, wind = 6.0, gusts = 12.0, code = 95, probability = 85, precipitation = 2.0, uv = 2.0))
        result.forEach { assertTrue("${it.activity} score ${it.score}", it.score < 45) }
        assertTrue(result.first().factors.contains("Thunderstorms"))
        assertTrue(result.first().factors.contains("Rain likely"))
    }

    @Test
    fun `cycling is penalised more by strong wind than walking`() {
        val result = Activities.evaluate(data(apparent = 18.0, wind = 9.0, gusts = 16.0, code = 1, probability = 5, precipitation = 0.0, uv = 3.0))
        val walk = result.first { it.activity == OutdoorActivity.WALK }
        val cycle = result.first { it.activity == OutdoorActivity.CYCLE }
        assertTrue("walk ${walk.score} cycle ${cycle.score}", cycle.score < walk.score)
        assertTrue(cycle.factors.contains("Windy"))
    }

    @Test
    fun `poor air quality lowers suitability and is named honestly`() {
        val result = Activities.evaluate(data(apparent = 19.0, wind = 2.0, gusts = 4.0, code = 0, probability = 0, precipitation = 0.0, uv = 3.0, aqi = 85))
        assertTrue(result.first().score < 80)
        assertTrue(result.first().factors.contains("Poor air"))
    }

    @Test
    fun `night cycling is less suitable than night walking`() {
        val result = Activities.evaluate(data(apparent = 16.0, wind = 2.0, gusts = 4.0, code = 0, probability = 0, precipitation = 0.0, uv = 0.0, isDay = false))
        val walk = result.first { it.activity == OutdoorActivity.WALK }
        val cycle = result.first { it.activity == OutdoorActivity.CYCLE }
        assertTrue(cycle.score < walk.score)
        assertTrue(cycle.factors.contains("After dark"))
    }

    @Test
    fun `snow is reported and lowers the score`() {
        val result = Activities.evaluate(data(apparent = -1.0, wind = 3.0, gusts = 6.0, code = 75, probability = 30, precipitation = 0.4, uv = 1.0))
        assertTrue(result.first().factors.contains("Snow"))
        assertTrue(result.first().score < 60)
    }
}
