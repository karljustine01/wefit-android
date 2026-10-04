package com.wefit.app.tracking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manual entry fallback — used when automatic sensor tracking cannot reliably
 * measure an exercise (e.g. Sit and Reach distance, Vertical Jump height).
 * This is NOT fake data: the user enters their own measured result,
 * and the app records exactly what they enter with no fabrication.
 */
class ManualEntryStrategy : TrackingStrategy {

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability(): TrackingAvailability = TrackingAvailability.AVAILABLE

    override fun start() {
        // No sensor to start; timer still runs for duration record-keeping
    }

    override fun pause() {}
    override fun resume() {}

    override fun stop(): TrackingData = _data.value

    fun setManualValue(repetitions: Int? = null, distanceMeters: Double? = null, durationSeconds: Int? = null) {
        _data.value = TrackingData(
            repetitions = repetitions ?: 0,
            distanceMeters = distanceMeters ?: 0.0,
            durationSeconds = durationSeconds ?: 0
        )
    }
}