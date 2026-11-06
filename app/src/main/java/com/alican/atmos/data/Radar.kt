package com.alican.atmos.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.alican.atmos.core.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

data class RadarFrame(
    val epochSeconds: Long,
    val path: String,
    val isForecast: Boolean
)

data class RadarMeta(
    val host: String,
    val frames: List<RadarFrame>
)

/**
 * RainViewer publishes genuinely observed radar frames (past) separately from its short nowcast.
 * ATMOS labels those two groups differently and never treats the nowcast as an observation.
 */
object RadarApi {
    private const val UA = "ATMOS/1.0 (Android; https://github.com/tutualican/atmos)"

    suspend fun load(): RadarMeta = withContext(Dispatchers.IO) {
        val conn = (URL("https://api.rainviewer.com/public/weather-maps.json").openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", UA)
        }
        val body = try {
            if (conn.responseCode !in 200..299) throw java.io.IOException("radar HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
        val root = JSONObject(body)
        val host = root.optString("host", "https://tilecache.rainviewer.com")
        val frames = ArrayList<RadarFrame>()
        root.optJSONObject("radar")?.let { radar ->
            radar.optJSONArray("past")?.let { past ->
                for (i in 0 until past.length()) {
                    val o = past.getJSONObject(i)
                    frames += RadarFrame(o.getLong("time"), o.getString("path"), isForecast = false)
                }
            }
            radar.optJSONArray("nowcast")?.let { nowcast ->
                for (i in 0 until nowcast.length()) {
                    val o = nowcast.getJSONObject(i)
                    frames += RadarFrame(o.getLong("time"), o.getString("path"), isForecast = true)
                }
            }
        }
        RadarMeta(host, frames)
    }
}

object Tiles {
    private const val UA = "ATMOS/1.0 (Android; https://github.com/tutualican/atmos)"

    private val cache = object : LruCache<String, Bitmap>(7 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun cached(key: String): Bitmap? = cache.get(key)

    suspend fun load(key: String, url: String): Bitmap? {
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", UA)
                    setRequestProperty("Accept", "image/png,image/*")
                }
                try {
                    if (conn.responseCode !in 200..299) return@runCatching null
                    conn.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.also { cache.put(key, it) }
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }

    fun baseMapUrl(z: Int, x: Int, y: Int): String {
        val sub = when ((x + y) % 4) { 0 -> "a"; 1 -> "b"; 2 -> "c"; else -> "d" }
        return "https://$sub.basemaps.cartocdn.com/dark_all/$z/$x/$y.png"
    }

    fun radarUrl(host: String, path: String, z: Int, x: Int, y: Int): String =
        "$host$path/256/$z/$x/$y/4/1_1.png"
}

object GeoMath {
    private const val TILE = 256.0

    fun lonToWorldX(lon: Double, zoom: Double): Double = (lon + 180.0) / 360.0 * TILE * 2.0.pow(zoom)

    fun latToWorldY(lat: Double, zoom: Double): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val rad = Math.toRadians(clamped)
        return (1.0 - (Math.log(Math.tan(rad) + 1.0 / Math.cos(rad)) / Math.PI)) / 2.0 * TILE * 2.0.pow(zoom)
    }

    fun worldXToLon(x: Double, zoom: Double): Double = x / (TILE * 2.0.pow(zoom)) * 360.0 - 180.0

    fun worldYToLat(y: Double, zoom: Double): Double {
        val n = Math.PI - 2.0 * Math.PI * y / (TILE * 2.0.pow(zoom))
        return Math.toDegrees(Math.atan(Math.sinh(n)))
    }
}

fun Place.mapSubtitle(): String = subtitle
