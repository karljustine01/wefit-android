package com.wefit.app.tracking

import kotlinx.coroutines.flow.StateFlow

data class TrackingData(
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val repetitions: Int = 0,
    val durationSeconds: Int = 0,
    val speedMps: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

enum class TrackingAvailability {
    AVAILABLE, UNAVAILABLE_NO_HARDWARE, UNAVAILABLE_NO_PERMISSION
}

interface TrackingStrategy {
    val data: StateFlow<TrackingData>
    fun checkAvailability(): TrackingAvailability
    fun start()
    fun pause()
    fun resume()
    fun stop(): TrackingData
}