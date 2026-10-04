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
 * Rep-counting tracking strategy for Push-Ups, Sit-Ups, Squats, Jumping Jacks, Jumping Rope.
 *
 * Uses a peak-detection algorithm on the accelerometer's magnitude signal:
 * detects a "rep" when acceleration crosses above a threshold then back below it,
 * with a minimum time gap to avoid double-counting noise.
 *
 * LIMITATION (honest, per project spec): this is a real but basic algorithm.
 * It cannot distinguish between exercise types or verify correct form.
 * It will register a rep for any sufficiently sharp motion matching the
 * threshold/cooldown pattern. For reliable form verification, CameraX +
 * pose estimation would be required — that is a larger addition and is
 * flagged as a possible enhancement, not implemented in this pass.
 */
class AccelerometerRepTrackingStrategy(
    private val context: Context,
    private val peakThreshold: Float = 12.5f,
    private val minRepIntervalMs: Long = 400L
) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var repCount = 0
    private var lastRepTimeMs = 0L
    private var abovePeak = false
    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability(): TrackingAvailability {
        return if (accelSensor != null) TrackingAvailability.AVAILABLE
        else TrackingAvailability.UNAVAILABLE_NO_HARDWARE
    }

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
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
        sensorManager.unregisterListener(this)
        return _data.value
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isPaused || event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        val now = System.currentTimeMillis()

        if (!abovePeak && magnitude > peakThreshold && (now - lastRepTimeMs) > minRepIntervalMs) {
            abovePeak = true
        } else if (abovePeak && magnitude < peakThreshold * 0.7f) {
            abovePeak = false
            repCount++
            lastRepTimeMs = now
        }

        val elapsedSeconds = ((now - startTimeMs - pausedDurationMs) / 1000).toInt()

        _data.value = TrackingData(
            repetitions = repCount,
            durationSeconds = elapsedSeconds
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}