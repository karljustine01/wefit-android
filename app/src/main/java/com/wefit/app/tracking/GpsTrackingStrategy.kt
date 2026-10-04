package com.wefit.app.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

/**
 * GPS-based tracking for Running and the 1.0 Mile Walk Test.
 * Measures distance via consecutive location updates and computes speed/pace.
 * Also records each point visited as routePoints, so the UI can draw a
 * live route map.
 */
class GpsTrackingStrategy(private val context: Context) : TrackingStrategy {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Double, Double>>> = _routePoints

    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (isPaused) return
            val location = result.lastLocation ?: return

            lastLocation?.let { last ->
                totalDistanceMeters += last.distanceTo(location)
            }
            lastLocation = location

            _routePoints.value = _routePoints.value + (location.latitude to location.longitude)

            val elapsedSeconds = ((System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000).toInt()
            val speedMps = if (elapsedSeconds > 0) totalDistanceMeters / elapsedSeconds else 0.0

            _data.value = TrackingData(
                distanceMeters = totalDistanceMeters,
                durationSeconds = elapsedSeconds,
                speedMps = speedMps,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
    }

    override fun checkAvailability(): TrackingAvailability {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasFineLocation) TrackingAvailability.AVAILABLE
        else TrackingAvailability.UNAVAILABLE_NO_PERMISSION
    }

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        if (checkAvailability() != TrackingAvailability.AVAILABLE) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, context.mainLooper
            )
        } catch (e: SecurityException) {
            // Permission was revoked between check and request; caller should re-check availability
        }
    }

    override fun pause() {
        isPaused = true
        pauseStartMs = System.currentTimeMillis()
    }

    override fun resume() {
        isPaused = false
        pausedDurationMs += System.currentTimeMillis() - pauseStartMs
    }

    override fun stop(): TrackingData {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        _routePoints.value = emptyList()
        return _data.value
    }
}