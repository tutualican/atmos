package com.alican.atmos.data

import com.alican.atmos.core.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * A warning that was issued by a national meteorological authority, not by a model.
 *
 * ATMOS currently ingests the US National Weather Service CAP feed, which is public and keyless.
 * For other countries no machine-readable public feed is available to this app, so the list stays
 * empty and the UI says so instead of inventing an alert.
 */
data class OfficialAlert(
    val id: String,
    val event: String,
    val severity: String,
    val urgency: String,
    val headline: String,
    val description: String,
    val instruction: String?,
    val sender: String,
    val area: String,
    val onset: LocalDateTime?,
    val ends: LocalDateTime?,
    val source: String,
    val url: String?
)

object OfficialAlertsApi {
    private const val UA = "ATMOS/1.1 (Android; https://github.com/tutualican/atmos)"

    /** ISO-3166 alpha-2 codes covered by a public, keyless official CAP feed. */
    private val supportedCountries = setOf("US", "PR", "VI", "GU", "AS", "MP")

    fun isSupported(place: Place): Boolean {
        val country = place.country?.trim()?.uppercase().orEmpty()
        if (country in supportedCountries) return true
        val names = setOf("UNITED STATES", "USA", "US", "UNITED STATES OF AMERICA")
        if (country in names) return true
        // The NWS API rejects points outside its area of responsibility; require the bounding box too.
        return place.lat in 18.0..72.0 && place.lon in -180.0..-64.0 && country.isBlank().not()
    }

    suspend fun load(place: Place): List<OfficialAlert> = withContext(Dispatchers.IO) {
        if (!isSupported(place)) return@withContext emptyList()
        val url = "https://api.weather.gov/alerts/active?point=${place.lat},${place.lon}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/geo+json")
        }
        val body = try {
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val root = JSONObject(body)
        val features = root.optJSONArray("features") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until features.length()) {
                val props = features.optJSONObject(i)?.optJSONObject("properties") ?: continue
                val event = props.optString("event")
                if (event.isBlank()) continue
                val severity = props.optString("severity", "Unknown")
                // Only actionable levels; "Minor" advisories would drown the surface.
                if (severity.equals("Minor", true) || severity.equals("Unknown", true)) continue
                add(
                    OfficialAlert(
                        id = props.optString("id", "$url#$i"),
                        event = event,
                        severity = severity,
                        urgency = props.optString("urgency", "Unknown"),
                        headline = props.optString("headline").ifBlank { event },
                        description = props.optString("description"),
                        instruction = props.optString("instruction").takeIf { it.isNotBlank() },
                        sender = props.optString("senderName", "National Weather Service"),
                        area = props.optString("areaDesc"),
                        onset = parse(props.optString("onset"), props.optString("effective")),
                        ends = parse(props.optString("ends"), props.optString("expires")),
                        source = "NWS",
                        url = props.optString("@id").takeIf { it.startsWith("http") }
                    )
                )
            }
        }.sortedByDescending { severityRank(it.severity) }
    }

    private fun parse(primary: String, fallback: String): LocalDateTime? =
        runCatching {
            val value = primary.ifBlank { fallback }
            if (value.isBlank()) null
            else OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        }.getOrNull()

    fun severityRank(severity: String): Int = when (severity.lowercase()) {
        "extreme" -> 4
        "severe" -> 3
        "moderate" -> 2
        else -> 1
    }
}
