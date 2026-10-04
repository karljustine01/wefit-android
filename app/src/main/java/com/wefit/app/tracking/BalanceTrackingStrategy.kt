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
 * Stork Balance Test: uses Accelerometer + Gyroscope to detect stability.
 * The timer runs continuously; if the device (held by the student, e.g. in
 * a pocket or armband) exceeds a rotation/movement threshold, this indicates
 * loss of balance. The app surfaces this as a "wobble" signal but does NOT
 * automatically stop the timer — the student/teacher judges when balance
 * is lost, consistent with how this test is actually administered.
 *
 * LIMITATION (honest): phone-based motion sensing is a proxy for the
 * student's own balance, not a direct physiological measurement. This is
 * disclosed to the user in the UI.
 */
class BalanceTrackingStrategy(private val context: Context) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L
    private var wobbleCount = 0

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    private val wobbleThreshold = 1.5f // rad/s

    override fun checkAvailability(): TrackingAvailability {
        return if (gyroSensor != null) TrackingAvailability.AVAILABLE
        else TrackingAvailability.UNAVAILABLE_NO_HARDWARE
    }

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
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
        val magnitude = sqrt(
            (event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]).toDouble()
        ).toFloat()

        if (magnitude > wobbleThreshold) wobbleCount++

        val elapsedSeconds = ((System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000).toInt()
        _data.value = TrackingData(durationSeconds = elapsedSeconds, repetitions = wobbleCount)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}