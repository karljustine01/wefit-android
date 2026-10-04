package com.wefit.app.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Walking tracking strategy: Step Counter + Timer.
 * Distance is estimated using an average stride length (0.762m) since
 * precise distance requires GPS, which is a separate concern for Running.
 */
class StepCounterTrackingStrategy(private val context: Context) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private var initialSteps: Int? = null
    private var currentSteps = 0
    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    private val averageStrideMeters = 0.762

    override fun checkAvailability(): TrackingAvailability {
        return if (stepSensor != null) TrackingAvailability.AVAILABLE
        else TrackingAvailability.UNAVAILABLE_NO_HARDWARE
    }

    override fun start() {
        startTimeMs = System.currentTimeMillis()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
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
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            if (initialSteps == null) initialSteps = totalStepsSinceBoot
            currentSteps = totalStepsSinceBoot - (initialSteps ?: totalStepsSinceBoot)

            val elapsedSeconds = ((System.currentTimeMillis() - startTimeMs - pausedDurationMs) / 1000).toInt()

            _data.value = TrackingData(
                steps = currentSteps,
                distanceMeters = currentSteps * averageStrideMeters,
                durationSeconds = elapsedSeconds
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}