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
import kotlin.math.hypot
import kotlin.math.sqrt

enum class PoseMetricType { ANGLE, SPREAD }
enum class RepPhase { UP, DOWN, NEITHER }

data class PoseExerciseConfig(
    val metricType: PoseMetricType,
    val leftPointA: Int = PoseLandmark.LEFT_SHOULDER,
    val leftPointB: Int = PoseLandmark.LEFT_ELBOW,
    val leftPointC: Int = PoseLandmark.LEFT_WRIST,
    val rightPointA: Int = PoseLandmark.RIGHT_SHOULDER,
    val rightPointB: Int = PoseLandmark.RIGHT_ELBOW,
    val rightPointC: Int = PoseLandmark.RIGHT_WRIST,
    val upThreshold: Double = 155.0,
    val downThreshold: Double = 95.0,
    val minLikelihood: Float = 0.4f,
    val minRepIntervalMs: Long = 500L,
    val smoothingWindow: Int = 4,
    val requiredConsecutiveFrames: Int = 2,
    val useFrontCamera: Boolean = true
)

object PoseExerciseConfigs {
    val PUSH_UP = PoseExerciseConfig(
        metricType = PoseMetricType.ANGLE,
        leftPointA = PoseLandmark.LEFT_SHOULDER, leftPointB = PoseLandmark.LEFT_ELBOW, leftPointC = PoseLandmark.LEFT_WRIST,
        rightPointA = PoseLandmark.RIGHT_SHOULDER, rightPointB = PoseLandmark.RIGHT_ELBOW, rightPointC = PoseLandmark.RIGHT_WRIST,
        upThreshold = 155.0,
        downThreshold = 95.0
    ) // Retained for compatibility; "pushup" is now routed to PushUpTrackingStrategy instead.
    val SQUAT = PoseExerciseConfig(
        metricType = PoseMetricType.ANGLE,
        leftPointA = PoseLandmark.LEFT_HIP, leftPointB = PoseLandmark.LEFT_KNEE, leftPointC = PoseLandmark.LEFT_ANKLE,
        rightPointA = PoseLandmark.RIGHT_HIP, rightPointB = PoseLandmark.RIGHT_KNEE, rightPointC = PoseLandmark.RIGHT_ANKLE,
        upThreshold = 155.0,
        downThreshold = 100.0
    )
    val SIT_UP = PoseExerciseConfig(
        metricType = PoseMetricType.ANGLE,
        leftPointA = PoseLandmark.LEFT_SHOULDER, leftPointB = PoseLandmark.LEFT_HIP, leftPointC = PoseLandmark.LEFT_KNEE,
        rightPointA = PoseLandmark.RIGHT_SHOULDER, rightPointB = PoseLandmark.RIGHT_HIP, rightPointC = PoseLandmark.RIGHT_KNEE,
        upThreshold = 145.0,
        downThreshold = 85.0
    )
    val JUMPING_JACK = PoseExerciseConfig(
        metricType = PoseMetricType.SPREAD,
        upThreshold = 1.3,
        downThreshold = 0.9
    )
}

class PoseTrackingStrategy(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val config: PoseExerciseConfig
) : TrackingStrategy, PoseTrackingUi {

    override val isFrontCamera: Boolean get() = config.useFrontCamera

    private val poseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )

    private var cameraProvider: ProcessCameraProvider? = null
    private var repCount = 0
    private var startTimeMs = 0L
    private var isPaused = false
    private var pausedDurationMs = 0L
    private var pauseStartMs = 0L
    private val recentMetrics = ArrayDeque<Double>()
    private var currentPhase = RepPhase.NEITHER
    private var consecutiveUp = 0
    private var consecutiveDown = 0
    private var consecutiveNeither = 0
    private var lastRepTimeMs = 0L
    private var awaitingUpToCompleteRep = false
    private var lastReliableMetricTimeMs = 0L
    private val poseLostResetMs = 1500L
    private var activeSideIsLeft: Boolean? = null
    private var sideSwitchCandidateFrames = 0
    private var countingEnabled = false
    private var hasStarted = false
    private var pendingPreview: androidx.camera.core.Preview? = null
    private var boundAnalysis: ImageAnalysis? = null
    private var lastDiagLogMs = 0L

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

    override var previewView: PreviewView? = null

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
        if (hasStarted) {
            android.util.Log.d("WeFitPose", "start() ignored, already started")
            return
        }
        hasStarted = true
        android.util.Log.d("WeFitPose", "start() front=${config.useFrontCamera}")

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                cameraProvider = provider

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                boundAnalysis = analysis

                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processFrame(imageProxy)
                }

                val preview = androidx.camera.core.Preview.Builder().build()
                pendingPreview = preview

                val selector = if (config.useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)

                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }

                android.util.Log.d("WeFitPose", "Camera bound OK")
            } catch (e: Exception) {
                android.util.Log.e("WeFitPose", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun attachPreviewSurface(view: PreviewView) {
        previewView = view
        pendingPreview?.setSurfaceProvider(view.surfaceProvider)
        android.util.Log.d("WeFitPose", "Preview surface attached, pendingPreview null? ${pendingPreview == null}")
    }

    override fun beginCounting() {
        countingEnabled = true
        startTimeMs = System.currentTimeMillis()
        pausedDurationMs = 0L
        repCount = 0
        currentPhase = RepPhase.NEITHER
        consecutiveUp = 0
        consecutiveDown = 0
        consecutiveNeither = 0
        awaitingUpToCompleteRep = false
        lastRepTimeMs = 0L
        recentMetrics.clear()
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
                .addOnFailureListener { e -> android.util.Log.e("WeFitPose", "Pose detection failed", e) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun onPoseDetected(pose: Pose, imageWidth: Int, imageHeight: Int) {
        val now = System.currentTimeMillis()
        val detectedLandmarkCount = pose.allPoseLandmarks.count { it.inFrameLikelihood >= 0.3f }
        _poseDetected.value = detectedLandmarkCount >= 15

        if (now - lastDiagLogMs > 1000) {
            lastDiagLogMs = now
            val la = pose.getPoseLandmark(config.leftPointA)
            val lb = pose.getPoseLandmark(config.leftPointB)
            val lc = pose.getPoseLandmark(config.leftPointC)
            val ra = pose.getPoseLandmark(config.rightPointA)
            val rb = pose.getPoseLandmark(config.rightPointB)
            val rc = pose.getPoseLandmark(config.rightPointC)
            android.util.Log.d(
                "WeFitPose",
                "totalLandmarks=${pose.allPoseLandmarks.size} detected>=0.3=$detectedLandmarkCount | " +
                        "L(a=${la?.inFrameLikelihood} b=${lb?.inFrameLikelihood} c=${lc?.inFrameLikelihood}) " +
                        "R(a=${ra?.inFrameLikelihood} b=${rb?.inFrameLikelihood} c=${rc?.inFrameLikelihood}) " +
                        "minLikelihood=${config.minLikelihood} activeSideIsLeft=$activeSideIsLeft"
            )
        }

        val metric = when (config.metricType) {
            PoseMetricType.ANGLE -> computeAngleMetric(pose)
            PoseMetricType.SPREAD -> computeSpreadMetric(pose)
        }

        if (metric != null) {
            lastReliableMetricTimeMs = now
            recentMetrics.addLast(metric)
            if (recentMetrics.size > config.smoothingWindow) recentMetrics.removeFirst()
            val smoothed = recentMetrics.average()
            _currentAngle.value = smoothed

            val frameClass = when {
                smoothed >= config.upThreshold -> RepPhase.UP
                smoothed <= config.downThreshold -> RepPhase.DOWN
                else -> RepPhase.NEITHER
            }

            when (frameClass) {
                RepPhase.UP -> { consecutiveUp++; consecutiveDown = 0; consecutiveNeither = 0 }
                RepPhase.DOWN -> { consecutiveDown++; consecutiveUp = 0; consecutiveNeither = 0 }
                RepPhase.NEITHER -> { consecutiveNeither++; consecutiveUp = 0; consecutiveDown = 0 }
            }

            val committedPhase = when {
                consecutiveUp >= config.requiredConsecutiveFrames -> RepPhase.UP
                consecutiveDown >= config.requiredConsecutiveFrames -> RepPhase.DOWN
                consecutiveNeither >= config.requiredConsecutiveFrames -> RepPhase.NEITHER
                else -> currentPhase
            }

            if (committedPhase != currentPhase) {
                currentPhase = committedPhase
                _repState.value = when (currentPhase) {
                    RepPhase.UP -> "up"
                    RepPhase.DOWN -> "down"
                    RepPhase.NEITHER -> "neither"
                }

                if (countingEnabled) {
                    if (currentPhase == RepPhase.DOWN) {
                        awaitingUpToCompleteRep = true
                    } else if (currentPhase == RepPhase.UP && awaitingUpToCompleteRep) {
                        val canCount = (now - lastRepTimeMs) > config.minRepIntervalMs
                        if (canCount) {
                            repCount++
                            lastRepTimeMs = now
                        }
                        awaitingUpToCompleteRep = false
                    }
                }
            }
        } else if (lastReliableMetricTimeMs != 0L && now - lastReliableMetricTimeMs > poseLostResetMs) {
            consecutiveUp = 0
            consecutiveDown = 0
            consecutiveNeither = 0
            activeSideIsLeft = null
            sideSwitchCandidateFrames = 0
            _currentAngle.value = null
            if (countingEnabled) _repState.value = "no pose detected"
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

    private fun landmarkReliable(landmark: PoseLandmark?): Boolean =
        landmark != null && landmark.inFrameLikelihood >= config.minLikelihood

    private fun computeAngleMetric(pose: Pose): Double? {
        val la = pose.getPoseLandmark(config.leftPointA)
        val lb = pose.getPoseLandmark(config.leftPointB)
        val lc = pose.getPoseLandmark(config.leftPointC)
        val ra = pose.getPoseLandmark(config.rightPointA)
        val rb = pose.getPoseLandmark(config.rightPointB)
        val rc = pose.getPoseLandmark(config.rightPointC)

        val leftReliable = landmarkReliable(la) && landmarkReliable(lb) && landmarkReliable(lc)
        val rightReliable = landmarkReliable(ra) && landmarkReliable(rb) && landmarkReliable(rc)

        if (!leftReliable && !rightReliable) {
            sideSwitchCandidateFrames = 0
            return null
        }

        val currentSideStillReliable = when (activeSideIsLeft) {
            true -> leftReliable
            false -> rightReliable
            null -> false
        }

        if (!currentSideStillReliable) {
            val preferLeft = leftReliable && (!rightReliable ||
                    ((la?.inFrameLikelihood ?: 0f) + (lb?.inFrameLikelihood ?: 0f) + (lc?.inFrameLikelihood ?: 0f)) >=
                    ((ra?.inFrameLikelihood ?: 0f) + (rb?.inFrameLikelihood ?: 0f) + (rc?.inFrameLikelihood ?: 0f)))

            sideSwitchCandidateFrames++
            if (sideSwitchCandidateFrames >= 3) {
                activeSideIsLeft = if (leftReliable && rightReliable) preferLeft else leftReliable
                sideSwitchCandidateFrames = 0
            } else {
                return null
            }
        } else {
            sideSwitchCandidateFrames = 0
        }

        return if (activeSideIsLeft == true) {
            calculateAngle(la!!.position.x, la.position.y, lb!!.position.x, lb.position.y, lc!!.position.x, lc.position.y)
        } else {
            calculateAngle(ra!!.position.x, ra.position.y, rb!!.position.x, rb.position.y, rc!!.position.x, rc.position.y)
        }
    }

    private fun computeSpreadMetric(pose: Pose): Double? {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        if (!landmarkReliable(leftShoulder) || !landmarkReliable(rightShoulder) ||
            !landmarkReliable(leftWrist) || !landmarkReliable(rightWrist) ||
            !landmarkReliable(leftAnkle) || !landmarkReliable(rightAnkle) ||
            !landmarkReliable(leftHip) || !landmarkReliable(rightHip)
        ) return null

        val shoulderWidth = hypot(
            (leftShoulder!!.position.x - rightShoulder!!.position.x).toDouble(),
            (leftShoulder.position.y - rightShoulder.position.y).toDouble()
        )
        val hipWidth = hypot(
            (leftHip!!.position.x - rightHip!!.position.x).toDouble(),
            (leftHip.position.y - rightHip.position.y).toDouble()
        )
        val wristSpread = hypot(
            (leftWrist!!.position.x - rightWrist!!.position.x).toDouble(),
            (leftWrist.position.y - rightWrist.position.y).toDouble()
        )
        val ankleSpread = hypot(
            (leftAnkle!!.position.x - rightAnkle!!.position.x).toDouble(),
            (leftAnkle.position.y - rightAnkle.position.y).toDouble()
        )

        if (shoulderWidth == 0.0 || hipWidth == 0.0) return null

        return (wristSpread / shoulderWidth) + (ankleSpread / hipWidth)
    }

    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val abx = ax - bx; val aby = ay - by
        val cbx = cx - bx; val cby = cy - by
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