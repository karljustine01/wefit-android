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

/** Configurable thresholds for the push-up state machine. */
data class PushUpConfig(
    val topAngle: Double = 160.0,          // elbow angle counted as "top" position
    val bottomAngle: Double = 100.0,       // elbow angle counted as "bottom" position
    val hysteresis: Double = 5.0,          // anti-flicker margin around thresholds
    val minLikelihood: Float = 0.5f,       // minimum landmark confidence to trust a joint
    val minBodyAlignmentAngle: Double = 160.0, // shoulder-hip-ankle; 180 = perfectly straight
    val minKneeAngle: Double = 150.0,      // hip-knee-ankle; 180 = fully extended
    val minRepIntervalMs: Long = 400L,
    val smoothingAlpha: Double = 0.35,     // exponential moving average factor
    val sideSwitchRequiredFrames: Int = 5,
    val useFrontCamera: Boolean = true
)

enum class PushUpState { UNKNOWN, READY, LOWERING, BOTTOM, RAISING, COMPLETED, INVALID }

/**
 * Dedicated side-view push-up tracker built on ML Kit's on-device Accurate
 * Pose Detection model (Google's BlazePose — the same model family used by
 * MediaPipe's Pose solution).
 *
 * LANDMARKS USED
 * Per side: shoulder, elbow, wrist, hip, knee, and ankle (falling back to
 * heel, then foot-index, if the ankle itself is briefly unreliable — all
 * are real ML Kit landmarks, nothing is invented). Nose/eyes/ears are
 * available from the model but are not part of the push-up geometry, so
 * they are not used here.
 *
 * SIDE SELECTION
 * Every frame, both the left and right landmark sets are scored by average
 * confidence. Whichever side has all six required joints above
 * minLikelihood is usable. If both sides qualify, the higher-confidence
 * side is preferred. Once a side is locked, the tracker keeps using it
 * until it becomes unreliable for sideSwitchRequiredFrames consecutive
 * frames, which prevents the angle signal from jumping between unrelated
 * limbs frame-to-frame.
 *
 * ELBOW ANGLE
 * angle(shoulder, elbow, wrist), computed from real landmark coordinates
 * using the law-of-cosines dot-product formula (not a raw X/Y comparison).
 *
 * BODY ALIGNMENT
 * angle(shoulder, hip, ankle). Since this angle formula always returns a
 * value in [0,180], "straight body" is angle close to 180; the check is
 * angle >= minBodyAlignmentAngle.
 *
 * KNEE CHECK
 * angle(hip, knee, ankle) >= minKneeAngle (legs extended, not kneeling).
 *
 * SMOOTHING
 * A simple exponential moving average is applied to each of the three
 * angles per frame, so single noisy frames cannot flip the state machine.
 *
 * STATE MACHINE (see PushUpState)
 * READY -> LOWERING -> BOTTOM -> RAISING -> (COMPLETED | INVALID) -> READY
 * A rep is only counted on a genuine RAISING -> top transition that was
 * preceded by an actual BOTTOM state — reaching partway down and coming
 * back up (READY -> LOWERING -> READY) does NOT count, by construction.
 * If a completed cycle's body alignment or knee angle failed their
 * tolerance at the moment of completion, the cycle still resets the state
 * machine to READY (so the user isn't stuck), but the rep is NOT counted,
 * and formStatus reports why.
 *
 * LANDMARK CONFIDENCE / TRACKING LOSS
 * If neither side has all six required joints reliably detected, the
 * state immediately becomes UNKNOWN, trackingStatusMessage reports
 * "Move into full side view", and no state-machine transition or rep
 * counting happens that frame. Reacquiring tracking always resets to
 * READY (never resumes mid-rep), so a body/camera dropout can never
 * produce a false rep.
 *
 * LIMITATION (honest): this validates joint geometry, not exercise
 * "quality" beyond straight-body/extended-knee checks. Lighting, distance
 * and camera angle still affect detection reliability.
 */
class PushUpTrackingStrategy(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: PushUpConfig = PushUpConfig()
) : TrackingStrategy, PoseTrackingUi {

    override val isFrontCamera: Boolean get() = config.useFrontCamera

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    // Camera plumbing (self-contained; mirrors the proven pattern already
    // used elsewhere in this project so CameraX/permission behavior is
    // consistent, without depending on the generic PoseTrackingStrategy).
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
    private var state = PushUpState.READY
    private var lastRepTimeMs = 0L

    private var activeSideIsLeft: Boolean? = null
    private var sideSwitchCandidateFrames = 0

    private var smoothedElbow: Double? = null
    private var smoothedBody: Double? = null
    private var smoothedKnee: Double? = null

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

    private val _bodyAlignmentOk = MutableStateFlow(true)
    val bodyAlignmentOk: StateFlow<Boolean> = _bodyAlignmentOk

    private val _kneeOk = MutableStateFlow(true)
    val kneeOk: StateFlow<Boolean> = _kneeOk

    private val _formStatus = MutableStateFlow<String?>(null)
    val formStatus: StateFlow<String?> = _formStatus

    private val _trackingStatusMessage = MutableStateFlow("Move into full side view")
    val trackingStatusMessage: StateFlow<String> = _trackingStatusMessage

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
                android.util.Log.d("WeFitPushUp", "Camera bound OK")
            } catch (e: Exception) {
                android.util.Log.e("WeFitPushUp", "Camera bind failed", e)
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
        state = PushUpState.READY
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
                .addOnFailureListener { e -> android.util.Log.e("WeFitPushUp", "Detection failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun reliable(l: PoseLandmark?): Boolean =
        l != null && l.inFrameLikelihood >= config.minLikelihood

    private fun pickAnkle(hipIsLeft: Boolean, pose: Pose): PoseLandmark? {
        val ankle = pose.getPoseLandmark(if (hipIsLeft) PoseLandmark.LEFT_ANKLE else PoseLandmark.RIGHT_ANKLE)
        if (reliable(ankle)) return ankle
        val heel = pose.getPoseLandmark(if (hipIsLeft) PoseLandmark.LEFT_HEEL else PoseLandmark.RIGHT_HEEL)
        if (reliable(heel)) return heel
        val footIndex = pose.getPoseLandmark(if (hipIsLeft) PoseLandmark.LEFT_FOOT_INDEX else PoseLandmark.RIGHT_FOOT_INDEX)
        if (reliable(footIndex)) return footIndex
        return ankle // may be null/unreliable; caller checks reliability separately
    }

    private fun onPoseDetected(pose: Pose, imageWidth: Int, imageHeight: Int) {
        val now = System.currentTimeMillis()

        // --- Side selection: shoulder, elbow, wrist, hip, knee, ankle ---
        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val lElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val lWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val lAnkle = pickAnkle(hipIsLeft = true, pose = pose)

        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rAnkle = pickAnkle(hipIsLeft = false, pose = pose)

        val leftReliable = reliable(lShoulder) && reliable(lElbow) && reliable(lWrist) &&
                reliable(lHip) && reliable(lKnee) && reliable(lAnkle)
        val rightReliable = reliable(rShoulder) && reliable(rElbow) && reliable(rWrist) &&
                reliable(rHip) && reliable(rKnee) && reliable(rAnkle)

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
            val preferLeft = leftReliable && (!rightReliable || avgLikelihood(lShoulder, lElbow, lWrist, lHip, lKnee, lAnkle) >=
                    avgLikelihood(rShoulder, rElbow, rWrist, rHip, rKnee, rAnkle))
            if (activeSideIsLeft == null) {
                // Nothing locked yet — acquire immediately.
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
            val shoulder = if (useLeft) lShoulder!! else rShoulder!!
            val elbow = if (useLeft) lElbow!! else rElbow!!
            val wrist = if (useLeft) lWrist!! else rWrist!!
            val hip = if (useLeft) lHip!! else rHip!!
            val knee = if (useLeft) lKnee!! else rKnee!!
            val ankle = if (useLeft) lAnkle!! else rAnkle!!

            val rawElbow = angleAt(shoulder, elbow, wrist)
            val rawBody = angleAt(shoulder, hip, ankle)
            val rawKnee = angleAt(hip, knee, ankle)

            smoothedElbow = ema(smoothedElbow, rawElbow)
            smoothedBody = ema(smoothedBody, rawBody)
            smoothedKnee = ema(smoothedKnee, rawKnee)

            _currentAngle.value = smoothedElbow

            val bodyOk = (smoothedBody ?: 180.0) >= config.minBodyAlignmentAngle
            val kneeOkNow = (smoothedKnee ?: 180.0) >= config.minKneeAngle
            _bodyAlignmentOk.value = bodyOk
            _kneeOk.value = kneeOkNow
            _trackingStatusMessage.value = "Tracking: Stable"

            runStateMachine(smoothedElbow!!, bodyOk, kneeOkNow, now)
        } else {
            state = PushUpState.UNKNOWN
            _repState.value = "unknown"
            _currentAngle.value = null
            _trackingStatusMessage.value = "Move into full side view"
        }

        // Skeleton overlay: draw whatever landmarks were actually detected,
        // regardless of which side is "active" for angle math — useful
        // visual feedback even while acquiring tracking.
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

    private fun runStateMachine(elbow: Double, bodyOk: Boolean, kneeOkNow: Boolean, now: Long) {
        when (state) {
            PushUpState.UNKNOWN -> {
                // Reacquired tracking — always resume from a clean READY,
                // never mid-rep, so a dropout can't produce a false count.
                state = PushUpState.READY
                _repState.value = "ready"
            }

            PushUpState.READY -> {
                if (elbow < config.topAngle - config.hysteresis) {
                    state = PushUpState.LOWERING
                    _repState.value = "lowering"
                }
            }

            PushUpState.LOWERING -> {
                when {
                    elbow <= config.bottomAngle -> {
                        state = PushUpState.BOTTOM
                        _repState.value = "bottom"
                    }
                    elbow >= config.topAngle -> {
                        // Returned to top without ever reaching bottom —
                        // an incomplete/half push-up. Does NOT count.
                        state = PushUpState.READY
                        _repState.value = "ready"
                    }
                }
            }

            PushUpState.BOTTOM -> {
                if (elbow > config.bottomAngle + config.hysteresis) {
                    state = PushUpState.RAISING
                    _repState.value = "raising"
                }
            }

            PushUpState.RAISING -> {
                when {
                    elbow <= config.bottomAngle -> {
                        // Bounced back down before finishing — still bottom.
                        state = PushUpState.BOTTOM
                        _repState.value = "bottom"
                    }
                    elbow >= config.topAngle -> {
                        val canCount = (now - lastRepTimeMs) > config.minRepIntervalMs
                        if (canCount) {
                            lastRepTimeMs = now
                            if (bodyOk && kneeOkNow) {
                                if (countingEnabled) repCount++
                                state = PushUpState.COMPLETED
                                _repState.value = "completed"
                                _formStatus.value = "Good form"
                            } else {
                                state = PushUpState.INVALID
                                _repState.value = "invalid"
                                _formStatus.value = "Rep not counted — check body alignment/knees"
                            }
                        } else {
                            state = PushUpState.READY
                            _repState.value = "ready"
                        }
                    }
                }
            }

            PushUpState.COMPLETED, PushUpState.INVALID -> {
                // One-frame display state — revert to READY immediately so
                // the next cycle can begin.
                state = PushUpState.READY
                _repState.value = "ready"
            }
        }
    }

    private fun avgLikelihood(vararg landmarks: PoseLandmark?): Float {
        val values = landmarks.mapNotNull { it?.inFrameLikelihood }
        return if (values.isEmpty()) 0f else values.sum() / values.size
    }

    private fun ema(prev: Double?, raw: Double): Double =
        if (prev == null) raw else config.smoothingAlpha * raw + (1 - config.smoothingAlpha) * prev

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