package com.alican.atmos.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.alican.atmos.core.Place
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentPlace(context: Context, timeoutMs: Long = 12_000): Place? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val last = bestLastKnown(lm)
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60_000) {
            return toPlace(context, last)
        }
        val fresh = withTimeoutOrNull(timeoutMs) { awaitSingleUpdate(lm) }
        val location = fresh ?: last ?: return null
        return toPlace(context, location)
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnown(lm: LocationManager): Location? {
        return try {
            lm.getProviders(true)
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
        } catch (_: Throwable) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun awaitSingleUpdate(lm: LocationManager): Location? = suspendCancellableCoroutine { cont ->
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            cont.resume(null); return@suspendCancellableCoroutine
        }
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) cont.resume(location)
            }
            override fun onProviderDisabled(p: String) {
                if (cont.isActive) cont.resume(null)
            }
        }
        runCatching {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper())
        }
        cont.invokeOnCancellation {
            runCatching { lm.removeUpdates(listener) }
        }
    }

    suspend fun toPlace(context: Context, location: Location): Place = withContext(Dispatchers.IO) {
        var name = "My location"
        var admin: String? = null
        var country: String? = null
        runCatching {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                val a = results?.firstOrNull()
                if (a != null) {
                    name = a.locality ?: a.subAdminArea ?: a.adminArea ?: "My location"
                    admin = a.subAdminArea?.takeIf { it != name } ?: a.adminArea?.takeIf { it != name }
                    country = a.countryName
                }
            }
        }
        Place(
            id = "here",
            name = name,
            admin = admin,
            country = country,
            lat = location.latitude,
            lon = location.longitude,
            isCurrentLocation = true
        )
    }
}
