package com.wefit.app.tracking

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Simple timer-based tracking for Plank (duration-based hold exercises).
 * No sensor hardware required, so this is always available.
 */
class TimerTrackingStrategy : TrackingStrategy {

    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L
    private var tickerJob: kotlinx.coroutines.Job? = null

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability(): TrackingAvailability = TrackingAvailability.AVAILABLE

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        tickerJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            while (true) {
                if (!isPaused) {
                    val elapsed = ((System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000).toInt()
                    _data.value = TrackingData(durationSeconds = elapsed)
                }
                kotlinx.coroutines.delay(1000)
            }
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
        tickerJob?.cancel()
        return _data.value
    }
}