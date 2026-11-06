package com.alican.atmos.data

import android.content.Context
import android.content.SharedPreferences
import com.alican.atmos.core.AppLanguage
import com.alican.atmos.core.AtmosSettings
import com.alican.atmos.core.CachedBundle
import com.alican.atmos.core.ForecastModel
import com.alican.atmos.core.Place
import com.alican.atmos.core.TempUnit
import com.alican.atmos.core.WindUnit
import com.alican.atmos.core.PressureUnit
import com.alican.atmos.core.PrecipUnit
import com.alican.atmos.core.AnimationQuality
import com.alican.atmos.core.WeatherData
import org.json.JSONArray
import org.json.JSONObject

class AtmosStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("atmos_store", Context.MODE_PRIVATE)

    var settings: AtmosSettings
        get() = runCatching {
            val raw = prefs.getString(KEY_SETTINGS, null) ?: return@runCatching AtmosSettings()
            val o = JSONObject(raw)
            AtmosSettings(
                temperatureUnit = enumOf(o.optString("temp"), TempUnit.CELSIUS),
                windUnit = enumOf(o.optString("wind"), WindUnit.KMH),
                pressureUnit = enumOf(o.optString("pressure"), PressureUnit.HPA),
                precipitationUnit = enumOf(o.optString("precip"), PrecipUnit.MM),
                model = enumOf(o.optString("model"), ForecastModel.BEST_MATCH),
                animationQuality = enumOf(o.optString("quality"), AnimationQuality.AUTO),
                lightningHaptics = o.optBoolean("haptics", true),
                reduceMotion = o.optBoolean("reduceMotion", false),
                useLocation = o.optBoolean("useLocation", true),
                refreshMinutes = o.optInt("refresh", 15),
                language = enumOf(o.optString("language"), AppLanguage.SYSTEM)
            )
        }.getOrDefault(AtmosSettings())
        set(value) {
            prefs.edit().putString(
                KEY_SETTINGS,
                JSONObject().apply {
                    put("temp", value.temperatureUnit.name)
                    put("wind", value.windUnit.name)
                    put("pressure", value.pressureUnit.name)
                    put("precip", value.precipitationUnit.name)
                    put("model", value.model.name)
                    put("quality", value.animationQuality.name)
                    put("haptics", value.lightningHaptics)
                    put("reduceMotion", value.reduceMotion)
                    put("useLocation", value.useLocation)
                    put("refresh", value.refreshMinutes)
                    put("language", value.language.name)
                }.toString()
            ).apply()
        }

    var selectedPlace: Place?
        get() = prefs.getString(KEY_PLACE, null)?.let { runCatching { placeFromJson(JSONObject(it)) }.getOrNull() }
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_PLACE) else putString(KEY_PLACE, placeToJson(value).toString())
            }.apply()
        }

    var savedPlaces: List<Place>
        get() = runCatching {
            val raw = prefs.getString(KEY_PLACES, null) ?: return@runCatching emptyList()
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { placeFromJson(arr.getJSONObject(i)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
        set(value) {
            val arr = JSONArray()
            value.take(24).forEach { arr.put(placeToJson(it)) }
            prefs.edit().putString(KEY_PLACES, arr.toString()).apply()
        }

    var lastBundle: CachedBundle?
        get() = runCatching {
            val raw = prefs.getString(KEY_CACHE, null) ?: return@runCatching null
            val o = JSONObject(raw)
            CachedBundle(
                place = placeFromJson(o.getJSONObject("place")),
                payload = o.getString("payload"),
                airPayload = o.optString("air").takeIf { it.isNotBlank() && it != "null" },
                ensemblePayload = o.optString("ensemble").takeIf { it.isNotBlank() && it != "null" },
                fetchedAt = o.getLong("fetchedAt"),
                model = o.optString("model", "best_match")
            )
        }.getOrNull()
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_CACHE).apply()
            } else {
                val o = JSONObject().apply {
                    put("place", placeToJson(value.place))
                    put("payload", value.payload)
                    put("air", value.airPayload ?: JSONObject.NULL)
                    put("ensemble", value.ensemblePayload ?: JSONObject.NULL)
                    put("fetchedAt", value.fetchedAt)
                    put("model", value.model)
                }
                prefs.edit().putString(KEY_CACHE, o.toString()).apply()
            }
        }

    private fun placeToJson(p: Place) = JSONObject().apply {
        put("id", p.id); put("name", p.name); put("admin", p.admin ?: JSONObject.NULL)
        put("country", p.country ?: JSONObject.NULL); put("lat", p.lat); put("lon", p.lon)
        put("tz", p.timezone ?: JSONObject.NULL); put("current", p.isCurrentLocation)
    }

    private fun placeFromJson(o: JSONObject) = Place(
        id = o.optString("id"),
        name = o.optString("name"),
        admin = o.optString("admin").takeIf { it.isNotBlank() && it != "null" },
        country = o.optString("country").takeIf { it.isNotBlank() && it != "null" },
        lat = o.optDouble("lat"),
        lon = o.optDouble("lon"),
        timezone = o.optString("tz").takeIf { it.isNotBlank() && it != "null" },
        isCurrentLocation = o.optBoolean("current", false)
    )

    private inline fun <reified T : Enum<T>> enumOf(name: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    companion object {
        private const val KEY_SETTINGS = "settings_v1"
        private const val KEY_PLACE = "place_v1"
        private const val KEY_PLACES = "places_v1"
        private const val KEY_CACHE = "cache_v1"
    }
}

data class LoadResult(
    val data: WeatherData,
    val fromCache: Boolean,
    val errorMessage: String? = null
)

class WeatherRepository(context: Context) {

    val store = AtmosStore(context)

    suspend fun load(place: Place, forceRefresh: Boolean = false, settings: AtmosSettings = store.settings): LoadResult {
        val cached = store.lastBundle
        val isSamePlace = cached?.place?.let { it.lat == place.lat && it.lon == place.lon } == true
        val age = System.currentTimeMillis() - (cached?.fetchedAt ?: 0L)
        val freshEnough = isSamePlace && age < 5 * 60_000 && cached?.model == settings.model.apiValue
        if (!forceRefresh && freshEnough && cached != null) {
            return runCatching { LoadResult(OpenMeteo.parse(cached), fromCache = false) }
                .getOrElse { LoadResult(OpenMeteo.parse(cached), true) }
        }
        return try {
            val bundle = OpenMeteo.fetchBundle(place, settings.model)
            val parsed = OpenMeteo.parse(bundle)
            store.lastBundle = bundle
            LoadResult(parsed, fromCache = false)
        } catch (t: Throwable) {
            if (cached != null && isSamePlace) {
                LoadResult(OpenMeteo.parse(cached), fromCache = true, errorMessage = t.message)
            } else {
                throw t
            }
        }
    }

    suspend fun search(query: String, language: String): List<Place> = OpenMeteo.searchPlaces(query, language)
}
