package com.wefit.app.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Walking: counts steps and runs its own timer.
 * Uses the hardware step detector, else the step counter, else falls back to
 * accelerometer peak detection so walking still works on phones without a step sensor.
 * LIMITATION: the accelerometer fallback is approximate; the hardware sensors are more accurate.
 */
class StepCounterTrackingStrategy(private val context: Context) : TrackingStrategy, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepDetector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val stepCounter: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val hasStepSensor get() = stepDetector != null || stepCounter != null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    private var steps = 0
    private var startMs = 0L
    private var pausedTotalMs = 0L
    private var pauseStartMs = 0L
    private var paused = false

    // STEP_COUNTER bookkeeping
    private var counterBaseline = -1f
    private var lastCounter = 0f

    // accelerometer fallback
    private val gravity = FloatArray(3)
    private var above = false
    private var lastStepMs = 0L

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    override fun checkAvailability(): TrackingAvailability {
        if (!hasStepSensor && accelerometer == null) return TrackingAvailability.UNAVAILABLE_NO_HARDWARE
        if (hasStepSensor && Build.VERSION.SDK_INT >= 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED
        ) return TrackingAvailability.UNAVAILABLE_NO_PERMISSION
        return TrackingAvailability.AVAILABLE
    }

    override fun start() {
        steps = 0
        counterBaseline = -1f
        paused = false
        pausedTotalMs = 0L
        startMs = System.currentTimeMillis()

        when {
            stepDetector != null -> sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL)
            stepCounter != null -> sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL)
            accelerometer != null -> sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        publish()
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(500)
                publish()
            }
        }
    }

    override fun pause() {
        if (paused) return
        paused = true
        pauseStartMs = System.currentTimeMillis()
    }

    override fun resume() {
        if (!paused) return
        pausedTotalMs += System.currentTimeMillis() - pauseStartMs
        paused = false
    }

    override fun stop(): TrackingData {
        sensorManager.unregisterListener(this)
        ticker?.cancel()
        publish()
        val result = _data.value
        scope.cancel()
        return result
    }

    private fun elapsedSeconds(): Int {
        val end = if (paused) pauseStartMs else System.currentTimeMillis()
        return ((end - startMs - pausedTotalMs) / 1000).toInt().coerceAtLeast(0)
    }

    private fun publish() {
        _data.value = TrackingData(steps = steps, durationSeconds = elapsedSeconds())
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (!paused) { steps += 1; publish() }
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val v = event.values[0]
                if (counterBaseline < 0f) { counterBaseline = v; lastCounter = v }
                val delta = v - lastCounter
                lastCounter = v
                if (paused) counterBaseline += delta // steps taken while paused are not counted
                steps = (v - counterBaseline).toInt().coerceAtLeast(0)
                publish()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (paused) return
                val alpha = 0.8f
                var sum = 0f
                for (i in 0..2) {
                    gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i]
                    val lin = event.values[i] - gravity[i]
                    sum += lin * lin
                }
                val mag = sqrt(sum)
                val now = System.currentTimeMillis()
                if (!above && mag > 2.0f && now - lastStepMs > 300) {
                    above = true; steps += 1; lastStepMs = now; publish()
                } else if (above && mag < 1.0f) {
                    above = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}