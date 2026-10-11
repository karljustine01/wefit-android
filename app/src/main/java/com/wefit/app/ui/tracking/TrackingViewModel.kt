package com.wefit.app.ui.tracking

import android.app.Application
import androidx.camera.core.ExperimentalGetImage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wefit.app.data.repository.SessionHandle
import com.wefit.app.data.repository.TrackingRepository
import com.wefit.app.tracking.GpsTrackingStrategy
import com.wefit.app.tracking.ManualEntryStrategy
import com.wefit.app.tracking.PosePoint
import com.wefit.app.tracking.PoseTrackingUi
import com.wefit.app.tracking.PushUpTrackingStrategy
import com.wefit.app.tracking.RepStateMachineTrackingStrategy
import com.wefit.app.tracking.TrackingAvailability
import com.wefit.app.tracking.TrackingData
import com.wefit.app.tracking.TrackingStrategy
import com.wefit.app.tracking.TrackingStrategyFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TrackingPhase { IDLE, PERMISSION_NEEDED, UNAVAILABLE, TRACKING, PAUSED, COMPLETED }

data class TrackingUiState(
    val phase: TrackingPhase = TrackingPhase.IDLE,
    val data: TrackingData = TrackingData(),
    val availability: TrackingAvailability? = null,
    val error: String? = null
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackingRepository(application)
    private var strategy: TrackingStrategy? = null
    private var sessionHandle: SessionHandle? = null
    private var preparedExerciseType: String? = null
    private var targetValue: Double? = null
    private var autoCompleteTriggered = false
    private var progressJob: Job? = null

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private val _skeletonPoints = MutableStateFlow<Map<Int, PosePoint>>(emptyMap())
    val skeletonPoints: StateFlow<Map<Int, PosePoint>> = _skeletonPoints.asStateFlow()

    private val _currentAngle = MutableStateFlow<Double?>(null)
    val currentAngle: StateFlow<Double?> = _currentAngle.asStateFlow()

    private val _repState = MutableStateFlow("waiting")
    val repState: StateFlow<String> = _repState.asStateFlow()

    private val _poseDetected = MutableStateFlow(false)
    val poseDetected: StateFlow<Boolean> = _poseDetected.asStateFlow()

    private val _formStatus = MutableStateFlow<String?>(null)
    val formStatus: StateFlow<String?> = _formStatus.asStateFlow()

    private val _trackingStatusMessage = MutableStateFlow<String?>(null)
    val trackingStatusMessage: StateFlow<String?> = _trackingStatusMessage.asStateFlow()

    private val _bodyAlignmentOk = MutableStateFlow(true)
    val bodyAlignmentOk: StateFlow<Boolean> = _bodyAlignmentOk.asStateFlow()

    private val _kneeOk = MutableStateFlow(true)
    val kneeOk: StateFlow<Boolean> = _kneeOk.asStateFlow()

    private val _validationResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val validationResults: StateFlow<Map<String, Boolean>> = _validationResults.asStateFlow()

    private val _routePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val routePoints: StateFlow<List<Pair<Double, Double>>> = _routePoints.asStateFlow()

    fun setTarget(value: Double?) {
        targetValue = value
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun prepare(exerciseType: String, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        if (preparedExerciseType == exerciseType && strategy != null) {
            return
        }
        preparedExerciseType = exerciseType
        autoCompleteTriggered = false

        val newStrategy = TrackingStrategyFactory.create(getApplication(), exerciseType, lifecycleOwner)
        strategy = newStrategy
        _skeletonPoints.value = emptyMap()
        _currentAngle.value = null
        _repState.value = "waiting"
        _poseDetected.value = false
        _formStatus.value = null
        _trackingStatusMessage.value = null
        _bodyAlignmentOk.value = true
        _kneeOk.value = true
        _validationResults.value = emptyMap()
        _routePoints.value = emptyList()

        val availability = newStrategy.checkAvailability()
        _uiState.value = when (availability) {
            TrackingAvailability.AVAILABLE -> TrackingUiState(phase = TrackingPhase.IDLE, availability = availability)
            TrackingAvailability.UNAVAILABLE_NO_PERMISSION -> TrackingUiState(phase = TrackingPhase.PERMISSION_NEEDED, availability = availability)
            TrackingAvailability.UNAVAILABLE_NO_HARDWARE -> TrackingUiState(phase = TrackingPhase.UNAVAILABLE, availability = availability)
        }

        viewModelScope.launch {
            newStrategy.data.collect { data ->
                if (_uiState.value.phase == TrackingPhase.TRACKING) {
                    _uiState.value = _uiState.value.copy(data = data)
                    if (!autoCompleteTriggered &&
                        TrackingStrategyFactory.targetReached(preparedExerciseType ?: "", data, targetValue)
                    ) {
                        autoCompleteTriggered = true
                        complete()
                    }
                }
            }
        }

        if (newStrategy is PoseTrackingUi) {
            viewModelScope.launch {
                newStrategy.skeletonPoints.collect { points -> _skeletonPoints.value = points }
            }
            viewModelScope.launch {
                newStrategy.currentAngle.collect { angle -> _currentAngle.value = angle }
            }
            viewModelScope.launch {
                newStrategy.repState.collect { state -> _repState.value = state }
            }
            viewModelScope.launch {
                newStrategy.poseDetected.collect { detected -> _poseDetected.value = detected }
            }

            if (newStrategy is PushUpTrackingStrategy) {
                viewModelScope.launch {
                    newStrategy.formStatus.collect { status -> _formStatus.value = status }
                }
                viewModelScope.launch {
                    newStrategy.trackingStatusMessage.collect { msg -> _trackingStatusMessage.value = msg }
                }
                viewModelScope.launch {
                    newStrategy.bodyAlignmentOk.collect { ok -> _bodyAlignmentOk.value = ok }
                }
                viewModelScope.launch {
                    newStrategy.kneeOk.collect { ok -> _kneeOk.value = ok }
                }
            }

            if (newStrategy is RepStateMachineTrackingStrategy) {
                viewModelScope.launch {
                    newStrategy.formStatus.collect { status -> _formStatus.value = status }
                }
                viewModelScope.launch {
                    newStrategy.trackingStatusMessage.collect { msg -> _trackingStatusMessage.value = msg }
                }
                viewModelScope.launch {
                    newStrategy.validationResults.collect { results -> _validationResults.value = results }
                }
            }

            if (availability == TrackingAvailability.AVAILABLE) {
                newStrategy.start()
            }
        }

        if (newStrategy is GpsTrackingStrategy) {
            viewModelScope.launch {
                newStrategy.routePoints.collect { points -> _routePoints.value = points }
            }
            if (availability == TrackingAvailability.AVAILABLE) {
                newStrategy.start()
            }
        }
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    fun forceReprepare(exerciseType: String, lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        preparedExerciseType = null
        strategy?.stop()
        strategy = null
        prepare(exerciseType, lifecycleOwner)
    }

    fun start(assignmentId: Int, exerciseType: String) {
        viewModelScope.launch {
            val trackingMethod = TrackingStrategyFactory.trackingMethodLabel(exerciseType)
            sessionHandle = repository.startSession(assignmentId, trackingMethod)
            val s = strategy
            if (s is PoseTrackingUi) {
                s.beginCounting()
            } else if (s !is GpsTrackingStrategy) {
                s?.start()
            }
            _uiState.value = _uiState.value.copy(phase = TrackingPhase.TRACKING)
            startProgressUploads()
        }
    }

    private fun startProgressUploads() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                if (_uiState.value.phase == TrackingPhase.TRACKING) {
                    val handle = sessionHandle ?: continue
                    val d = _uiState.value.data
                    val percent = targetValue?.let { t ->
                        if (t > 0) (maxOf(d.repetitions.toDouble(), d.distanceMeters, d.durationSeconds.toDouble()) / t * 100).coerceIn(0.0, 100.0) else 0.0
                    } ?: 0.0
                    repository.uploadProgress(handle, d, percent)
                }
            }
        }
    }

    fun pause() {
        strategy?.pause()
        _uiState.value = _uiState.value.copy(phase = TrackingPhase.PAUSED)
    }

    fun resume() {
        strategy?.resume()
        _uiState.value = _uiState.value.copy(phase = TrackingPhase.TRACKING)
    }

    fun complete() {
        progressJob?.cancel()
        viewModelScope.launch {
            val finalData = strategy?.stop() ?: _uiState.value.data
            sessionHandle?.let { handle ->
                repository.completeSession(handle, finalData)
            }
            _uiState.value = _uiState.value.copy(phase = TrackingPhase.COMPLETED, data = finalData)
        }
    }

    fun submitManualEntry(repetitions: Int?, distanceMeters: Double?) {
        val manualStrategy = strategy as? ManualEntryStrategy ?: return
        manualStrategy.setManualValue(repetitions = repetitions, distanceMeters = distanceMeters)
        complete()
    }

    fun submitManualFallback(assignmentId: Int, reps: Int?, distanceMeters: Double?, durationSeconds: Int?) {
        viewModelScope.launch {
            val handle = repository.startSession(assignmentId, "manual")
            sessionHandle = handle
            val data = TrackingData(
                repetitions = reps ?: 0,
                distanceMeters = distanceMeters ?: 0.0,
                durationSeconds = durationSeconds ?: 0
            )
            repository.completeSession(handle, data)
            _uiState.value = _uiState.value.copy(phase = TrackingPhase.COMPLETED, data = data)
        }
    }

    fun getPoseStrategy(): PoseTrackingUi? = strategy as? PoseTrackingUi

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        strategy?.stop()
        preparedExerciseType = null
    }
}