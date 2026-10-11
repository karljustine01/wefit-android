package com.wefit.app.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * Counts sharp motion peaks (jumps for rope, direction cuts for agility).
 * LIMITATION (honest): it counts acceleration spikes, not movement patterns.
 * It cannot verify jump quality or that agility cuts were made in the
 * correct quadrants. Thresholds are starting values and need tuning on
 * real devices.
 */
class MotionPeakTrackingStrategy(
    private val context: Context,
    private val peakThreshold: Float,
    private val releaseThreshold: Float,
    private val minIntervalMs: Long
) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val sensor get() = linear ?: accel
    private val gravity = FloatArray(3)

    private var count = 0
    private var above = false
    private var lastPeakMs = 0L
    private var startMs = 0L
    private var paused = false
    private var pausedTotalMs = 0L
    private var pauseStartMs = 0L

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability() =
        if (sensor != null) TrackingAvailability.AVAILABLE else TrackingAvailability.UNAVAILABLE_NO_HARDWARE

    override fun start() {
        startMs = System.currentTimeMillis()
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun pause() { paused = true; pauseStartMs = System.currentTimeMillis() }
    override fun resume() { paused = false; pausedTotalMs += System.currentTimeMillis() - pauseStartMs }

    override fun stop(): TrackingData {
        sensorManager.unregisterListener(this)
        return _data.value
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (paused || event == null) return
        val v = FloatArray(3)
        if (linear != null) {
            for (i in 0..2) v[i] = event.values[i]
        } else {
            val alpha = 0.8f
            for (i in 0..2) {
                gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                v[i] = event.values[i] - gravity[i]
            }
        }
        val mag = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
        val now = System.currentTimeMillis()

        if (!above && mag >= peakThreshold && now - lastPeakMs >= minIntervalMs) {
            above = true
            count++
            lastPeakMs = now
        } else if (above && mag < releaseThreshold) {
            above = false
        }

        _data.value = TrackingData(
            repetitions = count,
            durationSeconds = ((now - startMs - pausedTotalMs) / 1000).toInt()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}