package com.example.repeatersmap.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.spatialk.geojson.Position

class LocationTracker(private val context: Context) {

    companion object {
        fun selectProvider(
            hasFine: Boolean,
            hasCoarse: Boolean,
            gpsEnabled: Boolean,
            fusedEnabled: Boolean,
            networkEnabled: Boolean
        ): String? {
            if (!hasFine && !hasCoarse) return null
            return when {
                hasFine && gpsEnabled -> LocationManager.GPS_PROVIDER
                fusedEnabled -> "fused"
                networkEnabled -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
        }
    }

    suspend fun getCurrentLocation(): Position? = suspendCancellableCoroutine { continuation ->
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (!hasFine && !hasCoarse) {
            if (continuation.isActive) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = selectProvider(
            hasFine = hasFine,
            hasCoarse = hasCoarse,
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER),
            fusedEnabled = locationManager.isProviderEnabled("fused"),
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )

        if (provider == null) {
            if (continuation.isActive) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val lastLocation = listOfNotNull(
                if (hasFine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null,
                if (locationManager.isProviderEnabled("fused")) locationManager.getLastKnownLocation("fused") else null,
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            ).maxByOrNull { it.time }

            if (lastLocation != null) {
                if (continuation.isActive) continuation.resume(Position(longitude = lastLocation.longitude, latitude = lastLocation.latitude))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(context)) { loc ->
                    val pos = loc?.let { Position(longitude = it.longitude, latitude = it.latitude) }
                    if (continuation.isActive) continuation.resume(pos)
                }
            } else {
                @Suppress("DEPRECATION")
                locationManager.requestSingleUpdate(provider, { loc ->
                    val pos = Position(longitude = loc.longitude, latitude = loc.latitude)
                    if (continuation.isActive) continuation.resume(pos)
                }, android.os.Looper.getMainLooper())
            }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
