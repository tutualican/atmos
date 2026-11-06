package com.alican.atmos.core

import kotlin.math.max
import kotlin.math.min

enum class OutdoorActivity { WALK, RUN, CYCLE }

/**
 * A transparent, forecast-derived suitability signal. It never claims to be a safety
 * guarantee: it only weighs the values the model already gave us.
 */
data class ActivitySuitability(
    val activity: OutdoorActivity,
    val score: Int,
    /** English key, translated by the UI. */
    val status: String,
    /** English keys, translated by the UI. */
    val factors: List<String>
)

object Activities {

    fun evaluate(data: WeatherData): List<ActivitySuitability> {
        val current = data.current
        val now = current.time.withMinute(0).withSecond(0).withNano(0)
        val upcoming = data.hourly.filter { !it.time.isBefore(now) }.take(3)
        val window = if (upcoming.isEmpty()) data.hourly.take(3) else upcoming

        val probability = window.mapNotNull { it.precipProbability }.maxOrNull() ?: 0
        val amount = window.sumOf { it.precipitation }
        val wind = max(current.windSpeed, window.maxOfOrNull { it.windSpeed } ?: 0.0)
        val gusts = max(current.windGusts, window.maxOfOrNull { it.windGusts } ?: 0.0)
        val warmest = max(current.apparent, window.maxOfOrNull { it.apparent } ?: current.apparent)
        val coldest = min(current.apparent, window.minOfOrNull { it.apparent } ?: current.apparent)
        val uv = window.mapNotNull { it.uvIndex }.maxOrNull()
            ?: data.daily.firstOrNull()?.uvMax
            ?: 0.0
        val aqi = data.air?.europeanAqi
        val visibility = window.mapNotNull { it.visibility }.minOrNull()
        val storm = data.kind.isStorm || window.any { WeatherKind.fromCode(it.weatherCode).isStorm }
        val snow = data.kind == WeatherKind.SNOW || window.any { it.snowfall > 0.05 }
        val wetNow = data.kind.isWet || current.precipitation > 0.05
        val day = current.isDay

        val rainPenalty = when {
            probability >= 70 || amount >= 1.0 -> 38
            probability >= 40 || amount >= 0.2 -> 22
            probability >= 20 || amount > 0.02 || wetNow -> 10
            else -> 0
        }
        val aqiPenalty = when {
            aqi == null -> 0
            aqi > 80 -> 25
            aqi > 60 -> 15
            aqi > 40 -> 8
            else -> 0
        }
        val visibilityPenalty = when {
            visibility == null -> 0
            visibility < 2_000 -> 20
            visibility < 5_000 -> 10
            else -> 0
        }

        return OutdoorActivity.entries.map { activity ->
            val factors = LinkedHashSet<String>()
            var score = 100

            if (rainPenalty > 0) {
                score -= rainPenalty
                factors += if (rainPenalty >= 22) "Rain likely" else "Showers possible"
            }
            if (storm) {
                score -= 28
                factors += "Thunderstorms"
            }
            if (snow) {
                score -= 18
                factors += "Snow"
            }

            val idealLow: Double
            val idealHigh: Double
            when (activity) {
                OutdoorActivity.WALK -> { idealLow = 10.0; idealHigh = 26.0 }
                OutdoorActivity.RUN -> { idealLow = 6.0; idealHigh = 22.0 }
                OutdoorActivity.CYCLE -> { idealLow = 9.0; idealHigh = 25.0 }
            }
            val heat = ((warmest - idealHigh) * 3).coerceIn(0.0, 24.0)
            val cold = ((idealLow - coldest) * 3).coerceIn(0.0, 24.0)
            if (heat >= 3) {
                score -= heat.toInt()
                factors += "Hot"
            }
            if (cold >= 3) {
                score -= cold.toInt()
                factors += "Cold"
            }

            val windPenalty = when (activity) {
                OutdoorActivity.WALK ->
                    if (wind > 10.0 || gusts > 17.0) (if (gusts > 24.0) 20 else 10) else 0
                OutdoorActivity.RUN ->
                    if (wind > 11.0 || gusts > 20.0) (if (gusts > 26.0) 24 else 14) else 0
                OutdoorActivity.CYCLE ->
                    if (wind > 7.0 || gusts > 14.0) (if (gusts > 22.0 || wind > 12.0) 30 else 18) else 0
            }
            if (windPenalty > 0) {
                score -= windPenalty
                factors += "Windy"
            }

            if (uv >= 8.0) {
                score -= if (activity == OutdoorActivity.CYCLE) 8 else 12
                factors += "Strong UV"
            } else if (uv >= 6.0) {
                score -= 6
            }

            if (aqiPenalty > 0) {
                score -= aqiPenalty
                factors += "Poor air"
            }
            if (visibilityPenalty > 0) {
                score -= visibilityPenalty
                factors += "Low visibility"
            }
            if (!day) {
                score -= if (activity == OutdoorActivity.CYCLE) 12 else 4
                factors += "After dark"
            }

            val finalScore = score.coerceIn(0, 100)
            ActivitySuitability(
                activity = activity,
                score = finalScore,
                status = when {
                    finalScore >= 80 -> "Great"
                    finalScore >= 60 -> "Good"
                    finalScore >= 40 -> "Fair"
                    else -> "Poor"
                },
                factors = if (factors.isEmpty()) listOf("Comfortable") else factors.take(3).toList()
            )
        }
    }
}
