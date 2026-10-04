package com.wefit.app.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Quadrant Agility Test: primarily a TIMED test (student moves between
 * four quadrants as fast as possible). Accelerometer direction-change
 * detection is used as a supplementary "movement changes detected" count,
 * but the authoritative measurement is elapsed time — consistent with how
 * this test is actually scored (fastest completion time).
 *
 * LIMITATION (honest): detecting exact quadrant-to-quadrant transitions
 * from accelerometer data alone is not reliable enough to auto-score
 * pattern correctness. Time is the primary automatic measurement;
 * pattern verification would require CameraX + pose estimation, not
 * implemented here.
 */
class AgilityTrackingStrategy(private val context: Context) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var directionChanges = 0

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability(): TrackingAvailability {
        return if (accelSensor != null) TrackingAvailability.AVAILABLE
        else TrackingAvailability.UNAVAILABLE_NO_HARDWARE
    }

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
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
        sensorManager.unregisterListener(this)
        return _data.value
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isPaused || event == null) return
        val x = event.values[0]
        val y = event.values[1]

        if ((x > 0) != (lastX > 0) || (y > 0) != (lastY > 0)) {
            directionChanges++
        }
        lastX = x
        lastY = y

        val elapsedSeconds = ((System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000).toInt()
        _data.value = TrackingData(durationSeconds = elapsedSeconds, repetitions = directionChanges)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}