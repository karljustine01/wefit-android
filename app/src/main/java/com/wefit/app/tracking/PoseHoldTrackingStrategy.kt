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

enum class HoldCheckType { BODY_STRAIGHT, LEG_LIFTED }

data class PoseHoldConfig(
    val checkType: HoldCheckType,
    val minLikelihood: Float = 0.5f,
    val useFrontCamera: Boolean = true,
    // BODY_STRAIGHT (plank): shoulder-hip-ankle angle must stay >= this.
    val minStraightAngle: Double = 155.0,
    // LEG_LIFTED (stork balance): vertical gap between the lifted ankle and
    // the standing knee, as a fraction of hip-to-ankle leg length.
    val minLiftFraction: Double = 0.12,
    val graceMs: Long = 1200L // how long the position can briefly break before the timer actually pauses
)

object PoseHoldConfigs {
    val PLANK = PoseHoldConfig(checkType = HoldCheckType.BODY_STRAIGHT, minStraightAngle = 150.0)
    val STORK_BALANCE = PoseHoldConfig(checkType = HoldCheckType.LEG_LIFTED, minLiftFraction = 0.10)
}

/**
 * Pose-gated hold timer: duration only advances while the required position
 * is actually held, verified via real ML Kit pose landmarks (same
 * Accurate Pose model as push-up/squat/sit-up tracking). If the position
 * breaks for longer than graceMs, the timer pauses automatically; it
 * resumes the instant the position is valid again. No reps are counted —
 * this is a held-duration exercise.
 *
 * PLANK: checks shoulder-hip-ankle angle stays close to a straight line.
 * STORK_BALANCE: checks one ankle is lifted well above the standing knee.
 *
 * LIMITATION (honest): this verifies joint geometry from the camera's
 * viewpoint, not true 3D form — camera angle and distance affect accuracy.
 */
class PoseHoldTrackingStrategy(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: PoseHoldConfig
) : TrackingStrategy, PoseTrackingUi {

    override val isFrontCamera: Boolean get() = config.useFrontCamera

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder().setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE).build()
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

    private var holdValid = false
    private var lastValidMs = 0L
    private var autoPausedForBrokenHold = false
    private var activeSideIsLeft: Boolean? = null
    private var sideSwitchFrames = 0

    private val _data = MutableStateFlow(TrackingData())
    override val data: StateFlow<TrackingData> = _data

    private val _skeletonPoints = MutableStateFlow<Map<Int, PosePoint>>(emptyMap())
    override val skeletonPoints: StateFlow<Map<Int, PosePoint>> = _skeletonPoints

    private val _currentAngle = MutableStateFlow<Double?>(null)
    override val currentAngle: StateFlow<Double?> = _currentAngle

    private val _repState = MutableStateFlow("waiting")
    override val repState: StateFlow<String> = _repState

    private val _poseDetected = MutableStateFlow(false)
    override val poseDetected: StateFlow<Boolean> = _poseDetected

    override fun checkAvailability(): TrackingAvailability {
        val hasCamera = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) return TrackingAvailability.UNAVAILABLE_NO_HARDWARE
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
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
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { processFrame(it) }
                val preview = androidx.camera.core.Preview.Builder().build()
                pendingPreview = preview
                val selector = if (config.useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }
            } catch (e: Exception) {
                android.util.Log.e("WeFitHold", "Camera bind failed", e)
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
        holdValid = false
        lastValidMs = 0L
        _repState.value = "waiting"
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(imageProxy: ImageProxy) {
        if (isPaused) { imageProxy.close(); return }
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val (w, h) = if (rotation == 90 || rotation == 270) imageProxy.height to imageProxy.width else imageProxy.width to imageProxy.height
            val image = InputImage.fromMediaImage(mediaImage, rotation)
            poseDetector.process(image)
                .addOnSuccessListener { pose -> onPoseDetected(pose, w, h) }
                .addOnCompleteListener { imageProxy.close() }
        } else imageProxy.close()
    }

    private fun reliable(l: PoseLandmark?) = l != null && l.inFrameLikelihood >= config.minLikelihood

    private fun onPoseDetected(pose: Pose, w: Int, h: Int) {
        val now = System.currentTimeMillis()
        val detectedCount = pose.allPoseLandmarks.count { it.inFrameLikelihood >= 0.3f }
        _poseDetected.value = detectedCount >= 15

        val result = when (config.checkType) {
            HoldCheckType.BODY_STRAIGHT -> checkBodyStraight(pose)
            HoldCheckType.LEG_LIFTED -> checkLegLifted(pose)
        }

        if (result != null) {
            val (metric, isValid) = result
            _currentAngle.value = metric
            holdValid = isValid
            if (isValid) {
                lastValidMs = now
                autoPausedForBrokenHold = false
                _repState.value = "holding"
            } else {
                _repState.value = "broken"
            }
        } else {
            _currentAngle.value = null
            _repState.value = "no pose detected"
        }

        if (countingEnabled) {
            if (!holdValid && lastValidMs != 0L && now - lastValidMs > config.graceMs) {
                if (!autoPausedForBrokenHold) {
                    autoPausedForBrokenHold = true
                    pauseStartMs = now
                }
            } else if (holdValid && autoPausedForBrokenHold) {
                autoPausedForBrokenHold = false
                pausedDurationMs += now - pauseStartMs
            }
        }

        if (w > 0 && h > 0) {
            val points = mutableMapOf<Int, PosePoint>()
            for (landmark in pose.allPoseLandmarks) {
                if (landmark.inFrameLikelihood >= 0.5f) {
                    points[landmark.landmarkType] = PosePoint(landmark.position.x / w, landmark.position.y / h)
                }
            }
            _skeletonPoints.value = points
        }

        val elapsed = if (countingEnabled && !autoPausedForBrokenHold) {
            ((now - startTimeMs - pausedDurationMs) / 1000).toInt()
        } else if (countingEnabled) {
            ((pauseStartMs - startTimeMs - pausedDurationMs) / 1000).toInt()
        } else 0
        _data.value = TrackingData(durationSeconds = elapsed)
    }

    /** Returns (angle, isValid) or null if landmarks aren't reliable. */
    private fun checkBodyStraight(pose: Pose): Pair<Double, Boolean>? {
        val lShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftOk = reliable(lShoulder) && reliable(lHip) && reliable(lAnkle)
        val rightOk = reliable(rShoulder) && reliable(rHip) && reliable(rAnkle)
        val useLeft = when {
            leftOk && !rightOk -> true
            !leftOk && rightOk -> false
            leftOk && rightOk -> activeSideIsLeft ?: true
            else -> return null
        }
        activeSideIsLeft = useLeft

        val angle = if (useLeft) angleAt(lShoulder!!, lHip!!, lAnkle!!) else angleAt(rShoulder!!, rHip!!, rAnkle!!)
        return angle to (angle >= config.minStraightAngle)
    }

    private fun checkLegLifted(pose: Pose): Pair<Double, Boolean>? {
        // Compare both ankles' heights against both knees; whichever ankle
        // is clearly higher (smaller Y) than its opposite knee by the
        // required fraction of leg length counts as "lifted".
        val lHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val lKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val lAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        if (!reliable(lHip) || !reliable(lKnee) || !reliable(lAnkle) ||
            !reliable(rHip) || !reliable(rKnee) || !reliable(rAnkle)) return null

        val legLength = kotlin.math.abs((lHip!!.position.y + rHip!!.position.y) / 2 - (lAnkle!!.position.y + rAnkle!!.position.y) / 2).coerceAtLeast(1f)

        // Smaller Y = higher on screen = lifted higher.
        val leftLift = (rKnee!!.position.y - lAnkle.position.y) / legLength
        val rightLift = (lKnee!!.position.y - rAnkle.position.y) / legLength
        val bestLift = maxOf(leftLift.toDouble(), rightLift.toDouble())

        return bestLift to (bestLift >= config.minLiftFraction)
    }

    private fun angleAt(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val abx = a.position.x - b.position.x; val aby = a.position.y - b.position.y
        val cbx = c.position.x - b.position.x; val cby = c.position.y - b.position.y
        val dot = abx * cbx + aby * cby
        val magAB = sqrt((abx * abx + aby * aby).toDouble())
        val magCB = sqrt((cbx * cbx + cby * cby).toDouble())
        if (magAB == 0.0 || magCB == 0.0) return 180.0
        return Math.toDegrees(acos((dot / (magAB * magCB)).coerceIn(-1.0, 1.0)))
    }

    override fun pause() { isPaused = true }
    override fun resume() { isPaused = false }
    override fun stop(): TrackingData {
        cameraProvider?.unbindAll()
        poseDetector.close()
        _skeletonPoints.value = emptyMap()
        return _data.value
    }
}