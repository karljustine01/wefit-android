package com.wefit.app.ui.tracking

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.wefit.app.tracking.ExerciseInstructions
import com.wefit.app.tracking.TrackingStrategyFactory
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun TrackingScreen(
    assignmentId: Int,
    exerciseName: String,
    exerciseType: String,
    onDone: () -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val skeletonPoints by viewModel.skeletonPoints.collectAsState()
    val currentAngle by viewModel.currentAngle.collectAsState()
    val repState by viewModel.repState.collectAsState()
    val poseDetected by viewModel.poseDetected.collectAsState()
    val formStatus by viewModel.formStatus.collectAsState()
    val trackingStatusMessage by viewModel.trackingStatusMessage.collectAsState()
    val bodyAlignmentOk by viewModel.bodyAlignmentOk.collectAsState()
    val kneeOk by viewModel.kneeOk.collectAsState()
    val validationResults by viewModel.validationResults.collectAsState()
    val routePoints by viewModel.routePoints.collectAsState()
    val isPoseExercise = TrackingStrategyFactory.isPoseBased(exerciseType)
    val isPushUp = exerciseType == "pushup"
    val hasStateMachineFeedback = TrackingStrategyFactory.hasStateMachineFeedback(exerciseType)
    val isGpsExercise = exerciseType in listOf("running", "50m_dash", "mile_walk", "walking")

    val permissionsState = rememberMultiplePermissionsState(
        permissions = if (isPoseExercise) {
            listOf(Manifest.permission.CAMERA)
        } else {
            listOf(
                Manifest.permission.ACTIVITY_RECOGNITION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.prepare(exerciseType, lifecycleOwner)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(exerciseName) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when (state.phase) {
                TrackingPhase.PERMISSION_NEEDED -> {
                    Text(
                        if (isPoseExercise)
                            "This exercise needs camera permission for pose-based tracking."
                        else
                            "This exercise needs location/activity permission to track automatically."
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("Grant Permission")
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "If tapping this doesn't show a system prompt, the permission may have been denied previously. Open Settings → Apps → WeFit → Permissions and enable it manually, then reopen this screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (permissionsState.allPermissionsGranted) {
                            viewModel.forceReprepare(exerciseType, lifecycleOwner)
                        }
                    }
                }
                TrackingPhase.UNAVAILABLE -> {
                    Text("This device doesn't have the sensor needed for automatic tracking of this exercise.")
                    Spacer(Modifier.height(8.dp))
                    Text("NOT YET VERIFIED: manual entry fallback is not yet built.", style = MaterialTheme.typography.labelLarge)
                }
                TrackingPhase.IDLE -> {
                    if (TrackingStrategyFactory.isManualEntry(exerciseType)) {
                        var repsInput by remember { mutableStateOf("") }
                        var distanceInput by remember { mutableStateOf("") }

                        Text("This result requires manual measurement.", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        SetupTipsCard(exerciseType)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = distanceInput,
                            onValueChange = { distanceInput = it },
                            label = { Text("Distance (cm), if applicable") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = repsInput,
                            onValueChange = { repsInput = it },
                            label = { Text("Reps / Completed (1 = yes), if applicable") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.start(assignmentId, exerciseType)
                            viewModel.submitManualEntry(
                                repetitions = repsInput.toIntOrNull(),
                                distanceMeters = (distanceInput.toDoubleOrNull() ?: 0.0) / 100.0
                            )
                        }) {
                            Text("Submit Result")
                        }
                    } else if (isPoseExercise) {
                        Text("Position Yourself", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        SetupTipsCard(exerciseType)
                        Spacer(Modifier.height(12.dp))
                        PoseCameraBox(
                            viewModel = viewModel,
                            skeletonPoints = skeletonPoints,
                            currentAngle = currentAngle,
                            repState = null
                        )
                        Spacer(Modifier.height(12.dp))
                        if (poseDetected) {
                            Text("✅ Pose detected — you're good to start", color = Color(0xFF00C853))
                        } else {
                            Text("⚠️ Adjust your position so your whole body is visible", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.start(assignmentId, exerciseType) }) {
                            Text("Start Exercise")
                        }
                    } else {
                        Text("Ready to start", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        SetupTipsCard(exerciseType)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.start(assignmentId, exerciseType) }) {
                            Text("Start Exercise")
                        }
                    }
                }
                TrackingPhase.TRACKING, TrackingPhase.PAUSED -> {
                    Text("${state.data.durationSeconds}s", style = MaterialTheme.typography.titleLarge)
                    if (isPoseExercise) {
                        Spacer(Modifier.height(12.dp))
                        PoseCameraBox(
                            viewModel = viewModel,
                            skeletonPoints = skeletonPoints,
                            currentAngle = currentAngle,
                            repState = repState
                        )
                        Spacer(Modifier.height(12.dp))
                        if (hasStateMachineFeedback) {
                            Text("Reps: ${state.data.repetitions}", style = MaterialTheme.typography.titleMedium)
                            Text("State: $repState")
                            currentAngle?.let { Text("Angle: ${"%.0f".format(it)}°") }
                            if (isPushUp) {
                                Text("Body Alignment: ${if (bodyAlignmentOk) "Good" else "Check straightness"}")
                                Text("Knees: ${if (kneeOk) "Extended" else "Check knee position"}")
                            } else {
                                validationResults.forEach { (label, passed) ->
                                    Text("$label: ${if (passed) "OK" else "Check this"}")
                                }
                            }
                            Text("Tracking: ${trackingStatusMessage ?: "—"}")
                            formStatus?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, color = if (it == "Good form") Color(0xFF00C853) else MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (isGpsExercise) {
                        Spacer(Modifier.height(12.dp))
                        RouteMapView(
                            points = routePoints,
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    if (state.data.steps > 0) Text("Steps: ${state.data.steps}")
                    if (!hasStateMachineFeedback && state.data.repetitions > 0) Text("Reps: ${state.data.repetitions}")
                    if (state.data.distanceMeters > 0) Text("Distance: ${"%.1f".format(state.data.distanceMeters)}m")
                    Spacer(Modifier.height(24.dp))
                    Row {
                        if (state.phase == TrackingPhase.TRACKING) {
                            Button(onClick = { viewModel.pause() }) { Text("Pause") }
                        } else {
                            Button(onClick = { viewModel.resume() }) { Text("Resume") }
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = { viewModel.complete() }) { Text("Complete") }
                    }
                }
                TrackingPhase.COMPLETED -> {
                    Text("✅ Session Complete!", style = MaterialTheme.typography.titleLarge)
                    Text("Duration: ${state.data.durationSeconds}s")
                    if (state.data.repetitions > 0) Text("Reps: ${state.data.repetitions}")
                    if (state.data.distanceMeters > 0) Text("Distance: ${"%.1f".format(state.data.distanceMeters)}m")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDone) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun PoseCameraBox(
    viewModel: TrackingViewModel,
    skeletonPoints: Map<Int, com.wefit.app.tracking.PosePoint>,
    currentAngle: Double?,
    repState: String?
) {
    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onPreviewReady = { previewView ->
                viewModel.getPoseStrategy()?.attachPreviewSurface(previewView)
            }
        )
        val isFront = viewModel.getPoseStrategy()?.isFrontCamera ?: true
        SkeletonOverlay(
            points = skeletonPoints,
            mirror = isFront,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(8.dp)
        ) {
            Text(
                text = currentAngle?.let { "%.1f°".format(it) } ?: "—",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            if (repState != null) {
                Text(
                    text = "State: $repState",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SetupTipsCard(exerciseType: String) {
    val tips = ExerciseInstructions.tipsFor(exerciseType)
    if (tips.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Setup Tips", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            tips.forEach { tip ->
                Text("• $tip", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}