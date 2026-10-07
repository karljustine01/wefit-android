package com.wefit.app.tracking

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.acos
import kotlin.math.sqrt

/** Same three joint IDs, mirrored for each body side. */
data class AngleTriple(
    val leftA: Int, val leftB: Int, val leftC: Int,
    val rightA: Int, val rightB: Int, val rightC: Int
)

/**
 * A secondary form check evaluated at rep-completion time. Set minAngle
 * to require the angle be at least that (e.g. "extended"), maxAngle to
 * require at most that (e.g. "bent"), or both for a range.
 */
data class ValidationCheck(
    val label: String,
    val triple: AngleTriple,
    val minAngle: Double? = null,
    val maxAngle: Double? = null
)

data class RepExerciseConfig(
    val primary: AngleTriple,
    val topAngle: Double,
    val bottomAngle: Double,
    val hysteresis: Double = 5.0,
    val minLikelihood: Float = 0.5f,
    val validations: List<ValidationCheck> = emptyList(),
    val minRepIntervalMs: Long = 400L,
    val smoothingAlpha: Double = 0.35,
    val sideSwitchRequiredFrames: Int = 5,
    val useFrontCamera: Boolean = true
)

object RepExerciseConfigs {
    val SQUAT = RepExerciseConfig(
        primary = AngleTriple(
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        ),
        topAngle = 160.0,   // standing, legs extended
        bottomAngle = 100.0 // squatted down
    )

    val LUNGE = RepExerciseConfig(
        primary = AngleTriple(
            PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
        ),
        topAngle = 160.0,
        bottomAngle = 110.0
    )

    val SIT_UP = RepExerciseConfig(
        primary = AngleTriple(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE
        ),
        topAngle = 150.0,  // lying back, body roughly straight
        bottomAngle = 90.0 // curled up toward knees
    )

    val PUSHUP_MODIFIED = RepExerciseConfig(
        primary = AngleTriple(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        ),
        topAngle = 155.0,
        bottomAngle = 100.0,
        validations = listOf(
            // Defining difference from a standard push-up: knees must be
            // BENT (on the ground), not extended. This is the inverse
            // check of the regular push-up's knee-extended requirement.
            ValidationCheck(
                label = "Knees on ground",
                triple = AngleTriple(
                    PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE,
                    PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE
                ),
                maxAngle = 130.0
            )
        )
    )
}

private enum class RepMachineState { UNKNOWN, READY, TOWARD_PEAK, PEAK, RETURNING, COMPLETED, INVALID }

/**
 * Generalized version of the push-up state machine, applied to any
 * exercise driven by a single primary joint angle plus optional secondary
 * form checks. Same robustness measures as PushUpTrackingStrategy:
 *
 * - Side (left/right) selection based on which side has all required
 *   landmarks reliably detected; locks onto one side and only switches
 *   after several consecutive frames of the other side being clearly
 *   better, so the angle signal never jumps between unrelated limbs.
 * - Exponential moving average smoothing per tracked angle.
 * - A rep counts only on a genuine top -> bottom -> top cycle (READY ->
 *   TOWARD_PEAK -> PEAK -> RETURNING -> top), never on a single threshold
 *   crossing, so partial movement is never counted.
 * - Validation checks (e.g. "knees bent" for modified push-ups) are
 *   evaluated at the moment of completion; if they fail, the cycle still
 *   resets cleanly to READY but the rep is marked INVALID and not counted.
 * - Tracking loss (landmarks unreliable) forces state to UNKNOWN and,
 *   on recovery, always resumes at READY — never mid-rep — so a dropout
 *   can never produce a false count.
 *
 * LIMITATION (honest): validates joint geometry against configured
 * thresholds, not full exercise "quality." Lighting, distance and camera
 * angle affect detection reliability.
 */
class RepStateMachineTrackingStrategy(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: RepExerciseConfig
) : TrackingStrategy, PoseTrackingUi {

    override val isFrontCamera: Boolean get() = config.useFrontCamera

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private var cameraProvider: ProcessCameraProvider? = null
    private var pendingPreview: androidx.camera.core.Preview? = null
    private var hasStarted = false
    override var previewView: PreviewView? = null

    private var isPaused = false
    private var pauseStartMs = 0L
    private var pausedDurationMs = 0L
    private var startTimeMs = 0L
    private var countingEnabled = false

    private var repCount = 0
    private var state = RepMachineState.READY
    private var lastRepTimeMs = 0L

    private var activeSideIsLeft: Boolean? = null
    private var sideSwitchCandidateFrames = 0

    private val smoothed = mutableMapOf<String, Double>()

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    private val _skeletonPoints = MutableStateFlow<Map<Int, PosePoint>>(emptyMap())
    override val skeletonPoints: StateFlow<Map<Int, PosePoint>> = _skeletonPoints

    private val _currentAngle = MutableStateFlow<Double?>(null)
    override val currentAngle: StateFlow<Double?> = _currentAngle

    private val _repState = MutableStateFlow("ready")
    override val repState: StateFlow<String> = _repState

    private val _poseDetected = MutableStateFlow(false)
    override val poseDetected: StateFlow<Boolean> = _poseDetected

    private val _formStatus = MutableStateFlow<String?>(null)
    val formStatus: StateFlow<String?> = _formStatus

    private val _trackingStatusMessage = MutableStateFlow("Move into position")
    val trackingStatusMessage: StateFlow<String> = _trackingStatusMessage

    private val _validationResults = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val validationResults: StateFlow<Map<String, Boolean>> = _validationResults

    override fun checkAvailability(): TrackingAvailability {
        val hasCamera = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) return TrackingAvailability.UNAVAILABLE_NO_HARDWARE

        val hasPermission = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return if (hasPermission) TrackingAvailability.AVAILABLE else TrackingAvailability.UNAVAILABLE_NO_PERMISSION
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun start() {
        if (hasStarted) return
        hasStarted = true

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processFrame(imageProxy)
                }

                val preview = androidx.camera.core.Preview.Builder().build()
                pendingPreview = preview

                val selector = if (config.useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)

                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }
                android.util.Log.d("WeFitRep", "Camera bound OK")
            } catch (e: Exception) {
                android.util.Log.e("WeFitRep", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun attachPreviewSurface(view: PreviewView) {
        previewView = view
        pendingPreview?.setSurfaceProvider(view.surfaceProvider)
    }

    override fun beginCounting() {
        countingEnabled = true
        startTimeMs = System.currentTimeMillis()
        pausedDurationMs = 0L
        repCount = 0
        state = RepMachineState.READY
        lastRepTimeMs = 0L
        _repState.value = "ready"
        _formStatus.value = null
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(imageProxy: ImageProxy) {
        if (isPaused) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val (effWidth, effHeight) = if (rotation == 90 || rotation == 270) {
                imageProxy.height to imageProxy.width
            } else {
                imageProxy.width to imageProxy.height
            }
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            poseDetector.process(image)
                .addOnSuccessListener { pose -> onPoseDetected(pose, effWidth, effHeight) }
                .addOnFailureListener { e -> android.util.Log.e("WeFitRep", "Detection failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun reliable(l: PoseLandmark?): Boolean =
        l != null && l.inFrameLikelihood >= config.minLikelihood

    private fun getWithAnkleFallback(pose: Pose, landmarkId: Int): PoseLandmark? {
        val primary = pose.getPoseLandmark(landmarkId)
        if (reliable(primary)) return primary
        val isLeftAnkle = landmarkId == PoseLandmark.LEFT_ANKLE
        val isRightAnkle = landmarkId == PoseLandmark.RIGHT_ANKLE
        if (!isLeftAnkle && !isRightAnkle) return primary
        val heel = pose.getPoseLandmark(if (isLeftAnkle) PoseLandmark.LEFT_HEEL else PoseLandmark.RIGHT_HEEL)
        if (reliable(heel)) return heel
        val footIndex = pose.getPoseLandmark(if (isLeftAnkle) PoseLandmark.LEFT_FOOT_INDEX else PoseLandmark.RIGHT_FOOT_INDEX)
        if (reliable(footIndex)) return footIndex
        return primary
    }

    private fun requiredLandmarkIds(useLeft: Boolean): List<Int> {
        val ids = mutableListOf<Int>()
        ids += if (useLeft) listOf(config.primary.leftA, config.primary.leftB, config.primary.leftC)
        else listOf(config.primary.rightA, config.primary.rightB, config.primary.rightC)
        for (v in config.validations) {
            ids += if (useLeft) listOf(v.triple.leftA, v.triple.leftB, v.triple.leftC)
            else listOf(v.triple.rightA, v.triple.rightB, v.triple.rightC)
        }
        return ids
    }

    private fun sideReliable(pose: Pose, useLeft: Boolean): Boolean {
        return requiredLandmarkIds(useLeft).all { id -> reliable(getWithAnkleFallback(pose, id)) }
    }

    private fun avgLikelihood(pose: Pose, useLeft: Boolean): Float {
        val values = requiredLandmarkIds(useLeft).mapNotNull { getWithAnkleFallback(pose, it)?.inFrameLikelihood }
        return if (values.isEmpty()) 0f else values.sum() / values.size
    }

    private fun angleAt(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val abx = a.position.x - b.position.x
        val aby = a.position.y - b.position.y
        val cbx = c.position.x - b.position.x
        val cby = c.position.y - b.position.y
        val dot = abx * cbx + aby * cby
        val magAB = sqrt((abx * abx + aby * aby).toDouble())
        val magCB = sqrt((cbx * cbx + cby * cby).toDouble())
        if (magAB == 0.0 || magCB == 0.0) return 180.0
        val cosAngle = (dot / (magAB * magCB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosAngle))
    }

    private fun ema(key: String, raw: Double): Double {
        val prev = smoothed[key]
        val result = if (prev == null) raw else config.smoothingAlpha * raw + (1 - config.smoothingAlpha) * prev
        smoothed[key] = result
        return result
    }

    private fun onPoseDetected(pose: Pose, imageWidth: Int, imageHeight: Int) {
        val now = System.currentTimeMillis()

        val leftReliable = sideReliable(pose, useLeft = true)
        val rightReliable = sideReliable(pose, useLeft = false)

        val currentSideStillReliable = when (activeSideIsLeft) {
            true -> leftReliable
            false -> rightReliable
            null -> false
        }

        var trackingReliable: Boolean

        if (currentSideStillReliable) {
            sideSwitchCandidateFrames = 0
            trackingReliable = true
        } else if (leftReliable || rightReliable) {
            val preferLeft = leftReliable && (!rightReliable || avgLikelihood(pose, true) >= avgLikelihood(pose, false))
            if (activeSideIsLeft == null) {
                activeSideIsLeft = preferLeft
                sideSwitchCandidateFrames = 0
                trackingReliable = true
            } else {
                sideSwitchCandidateFrames++
                if (sideSwitchCandidateFrames >= config.sideSwitchRequiredFrames) {
                    activeSideIsLeft = preferLeft
                    sideSwitchCandidateFrames = 0
                    trackingReliable = true
                } else {
                    trackingReliable = false
                }
            }
        } else {
            trackingReliable = false
        }

        _poseDetected.value = trackingReliable

        if (trackingReliable) {
            val useLeft = activeSideIsLeft == true

            val pa = getWithAnkleFallback(pose, if (useLeft) config.primary.leftA else config.primary.rightA)!!
            val pb = getWithAnkleFallback(pose, if (useLeft) config.primary.leftB else config.primary.rightB)!!
            val pc = getWithAnkleFallback(pose, if (useLeft) config.primary.leftC else config.primary.rightC)!!
            val primaryAngle = ema("primary", angleAt(pa, pb, pc))
            _currentAngle.value = primaryAngle

            val validations = mutableMapOf<String, Boolean>()
            for (v in config.validations) {
                val va = getWithAnkleFallback(pose, if (useLeft) v.triple.leftA else v.triple.rightA)!!
                val vb = getWithAnkleFallback(pose, if (useLeft) v.triple.leftB else v.triple.rightB)!!
                val vc = getWithAnkleFallback(pose, if (useLeft) v.triple.leftC else v.triple.rightC)!!
                val angle = ema(v.label, angleAt(va, vb, vc))
                val passes = (v.minAngle == null || angle >= v.minAngle) && (v.maxAngle == null || angle <= v.maxAngle)
                validations[v.label] = passes
            }
            _validationResults.value = validations
            _trackingStatusMessage.value = "Tracking: Stable"

            runStateMachine(primaryAngle, validations, now)
        } else {
            state = RepMachineState.UNKNOWN
            _repState.value = "unknown"
            _currentAngle.value = null
            _trackingStatusMessage.value = "Move into position — full body not clearly visible"
        }

        if (imageWidth > 0 && imageHeight > 0) {
            val points = mutableMapOf<Int, PosePoint>()
            for (landmark in pose.allPoseLandmarks) {
                if (landmark.inFrameLikelihood >= 0.5f) {
                    points[landmark.landmarkType] = PosePoint(
                        x = landmark.position.x / imageWidth,
                        y = landmark.position.y / imageHeight
                    )
                }
            }
            _skeletonPoints.value = points
        }

        val elapsedSeconds = if (countingEnabled) ((now - startTimeMs - pausedDurationMs) / 1000).toInt() else 0
        _data.value = TrackingData(repetitions = repCount, durationSeconds = elapsedSeconds)
    }

    private fun runStateMachine(angle: Double, validations: Map<String, Boolean>, now: Long) {
        when (state) {
            RepMachineState.UNKNOWN -> {
                state = RepMachineState.READY
                _repState.value = "ready"
            }
            RepMachineState.READY -> {
                if (angle < config.topAngle - config.hysteresis) {
                    state = RepMachineState.TOWARD_PEAK
                    _repState.value = "moving"
                }
            }
            RepMachineState.TOWARD_PEAK -> {
                when {
                    angle <= config.bottomAngle -> { state = RepMachineState.PEAK; _repState.value = "peak" }
                    angle >= config.topAngle -> { state = RepMachineState.READY; _repState.value = "ready" }
                }
            }
            RepMachineState.PEAK -> {
                if (angle > config.bottomAngle + config.hysteresis) {
                    state = RepMachineState.RETURNING
                    _repState.value = "returning"
                }
            }
            RepMachineState.RETURNING -> {
                when {
                    angle <= config.bottomAngle -> { state = RepMachineState.PEAK; _repState.value = "peak" }
                    angle >= config.topAngle -> {
                        val canCount = (now - lastRepTimeMs) > config.minRepIntervalMs
                        if (canCount) {
                            lastRepTimeMs = now
                            val allPass = validations.values.all { it }
                            if (allPass) {
                                if (countingEnabled) repCount++
                                state = RepMachineState.COMPLETED
                                _repState.value = "completed"
                                _formStatus.value = "Good form"
                            } else {
                                state = RepMachineState.INVALID
                                _repState.value = "invalid"
                                val failed = validations.filterValues { !it }.keys.joinToString(", ")
                                _formStatus.value = "Rep not counted — check: $failed"
                            }
                        } else {
                            state = RepMachineState.READY
                            _repState.value = "ready"
                        }
                    }
                }
            }
            RepMachineState.COMPLETED, RepMachineState.INVALID -> {
                state = RepMachineState.READY
                _repState.value = "ready"
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
        cameraProvider?.unbindAll()
        poseDetector.close()
        _skeletonPoints.value = emptyMap()
        return _data.value
    }
}