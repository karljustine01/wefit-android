package com.wefit.app.ui.tracking

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.wefit.app.data.remote.api.ApiConfig
import com.wefit.app.data.remote.dto.AssignmentDetailsDto
import com.wefit.app.tracking.ExerciseInstructions
import com.wefit.app.tracking.ExerciseUnits
import com.wefit.app.tracking.TrackingStrategyFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TrackingScreen(
    assignmentId: Int,
    exerciseName: String,
    exerciseType: String,
    targetValue: Double? = null,
    onDone: () -> Unit,
    viewModel: TrackingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val details by viewModel.details.collectAsState()
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
    val isGpsExercise = exerciseType in listOf("running", "50m_dash", "mile_walk")
    val isFixedDistance = exerciseType == "50m_dash" || exerciseType == "mile_walk"
    val unit = ExerciseUnits.targetUnit(exerciseType)
    var showCamera by remember { mutableStateOf(true) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = when {
            isPoseExercise -> listOf(Manifest.permission.CAMERA)
            isGpsExercise -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else -> listOf(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.prepare(exerciseType, lifecycleOwner)
        viewModel.loadDetails(assignmentId)
    }
    LaunchedEffect(targetValue) { viewModel.setTarget(targetValue) }

    Scaffold(topBar = { TopAppBar(title = { Text(exerciseName) }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val active = state.phase == TrackingPhase.TRACKING || state.phase == TrackingPhase.PAUSED
            if (state.phase != TrackingPhase.COMPLETED && (targetValue != null || isFixedDistance)) {
                val goalText = when {
                    exerciseType == "50m_dash" -> "Goal: 50 m"
                    exerciseType == "mile_walk" -> "Goal: 1.0 mile (1,609 m)"
                    else -> "Target: ${formatTarget(targetValue ?: 0.0)} ${unit ?: ""}"
                }
                Text(goalText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (active) {
                    val percent = TrackingStrategyFactory.progressPercent(exerciseType, state.data, targetValue)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { (percent / 100.0).toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text("${percent.toInt()}% of target", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(12.dp))
            }

            when (state.phase) {
                TrackingPhase.PERMISSION_NEEDED -> {
                    Text(
                        when {
                            isPoseExercise -> "This exercise needs camera permission for pose-based tracking."
                            isGpsExercise -> "This exercise needs location permission to measure distance."
                            else -> "This exercise needs the Physical activity permission to count steps."
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) { Text("Grant Permission") }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "If no prompt appears, open Settings → Apps → WeFit → Permissions, enable it, then reopen this screen.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LaunchedEffect(permissionsState.allPermissionsGranted) {
                        if (permissionsState.allPermissionsGranted) viewModel.forceReprepare(exerciseType, lifecycleOwner)
                    }
                }

                TrackingPhase.UNAVAILABLE -> {
                    var fbReps by remember { mutableStateOf("") }
                    var fbDistance by remember { mutableStateOf("") }
                    var fbDuration by remember { mutableStateOf("") }

                    Text("Automatic tracking isn't available on this device.", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter the result you measured yourself. It is recorded as a manual entry so your teacher can see how it was measured.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = fbReps, onValueChange = { fbReps = it }, label = { Text("Repetitions") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = fbDuration, onValueChange = { fbDuration = it }, label = { Text("Duration (seconds)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = fbDistance, onValueChange = { fbDistance = it }, label = { Text("Distance (meters)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.submitManualFallback(assignmentId, fbReps.toIntOrNull(), fbDistance.toDoubleOrNull(), fbDuration.toIntOrNull())
                    }) { Text("Submit Result") }
                }

                TrackingPhase.IDLE -> {
                    if (TrackingStrategyFactory.isManualEntry(exerciseType)) {
                        var distanceInput by remember { mutableStateOf("") }

                        Text("This result requires manual measurement.", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        InstructionsBlock(exerciseType, details)
                        Spacer(Modifier.height(16.dp))

                        if (exerciseType != "project") {
                            OutlinedTextField(
                                value = distanceInput,
                                onValueChange = { distanceInput = it },
                                label = { Text("Distance (cm)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("No measurement needed — just submit to mark this complete.", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            viewModel.start(assignmentId, exerciseType)
                            viewModel.submitManualEntry(
                                repetitions = if (exerciseType == "project") 1 else null,
                                distanceMeters = (distanceInput.toDoubleOrNull() ?: 0.0) / 100.0
                            )
                        }) { Text("Submit Result") }
                    } else if (isPoseExercise) {
                        Text("Position Yourself", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        InstructionsBlock(exerciseType, details)
                        Spacer(Modifier.height(12.dp))
                        PoseCameraBox(viewModel, skeletonPoints, currentAngle, null, state.data.durationSeconds, showCamera)
                        TextButton(onClick = { showCamera = !showCamera }) {
                            Text(if (showCamera) "Hide camera preview" else "Show camera preview")
                        }
                        if (poseDetected) {
                            Text("✅ Pose detected — you're good to start", color = Color(0xFF00C853))
                        } else {
                            Text("⚠️ Adjust your position so your whole body is visible", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.start(assignmentId, exerciseType) }) { Text("Start Exercise") }
                    } else {
                        Text("Ready to start", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        InstructionsBlock(exerciseType, details)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.start(assignmentId, exerciseType) }) { Text("Start Exercise") }
                    }
                }

                TrackingPhase.TRACKING, TrackingPhase.PAUSED -> {
                    if (!isPoseExercise && !isGpsExercise) {
                        Text("${state.data.durationSeconds}s", style = MaterialTheme.typography.headlineSmall)
                    }
                    if (isPoseExercise) {
                        PoseCameraBox(viewModel, skeletonPoints, currentAngle, repState, state.data.durationSeconds, showCamera)
                        TextButton(onClick = { showCamera = !showCamera }) {
                            Text(if (showCamera) "Hide camera preview" else "Show camera preview")
                        }
                        if (hasStateMachineFeedback) {
                            Text("Reps: ${state.data.repetitions}", style = MaterialTheme.typography.titleMedium)
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
                                Text(it, color = if (it == "Good form") Color(0xFF00C853) else MaterialTheme.colorScheme.error)
                            }
                        } else if (state.data.repetitions > 0) {
                            Text("Reps: ${state.data.repetitions}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    if (isGpsExercise) {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                            RouteMapView(points = routePoints, modifier = Modifier.fillMaxSize())
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(10.dp)
                            ) {
                                Text("${state.data.durationSeconds}s", color = Color.White, style = MaterialTheme.typography.titleLarge)
                                if (state.data.distanceMeters > 0) {
                                    Text("${"%.0f".format(state.data.distanceMeters)}m", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                    if (exerciseType == "walking" || state.data.steps > 0) {
                        Text("Steps: ${state.data.steps}", style = MaterialTheme.typography.titleLarge)
                    }
                    if (!isPoseExercise && state.data.repetitions > 0) {
                        Text("Reps: ${state.data.repetitions}", style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.data.distanceMeters > 0 && !isGpsExercise) {
                        Text("Distance: ${"%.1f".format(state.data.distanceMeters)}m")
                    }
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
                    if (state.data.steps > 0) Text("Steps: ${state.data.steps}")
                    if (state.data.repetitions > 0) Text("Reps: ${state.data.repetitions}")
                    if (state.data.distanceMeters > 0) Text("Distance: ${"%.1f".format(state.data.distanceMeters)}m")
                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = onDone) { Text("Done") }
                        Spacer(Modifier.width(16.dp))
                        OutlinedButton(onClick = { viewModel.forceReprepare(exerciseType, lifecycleOwner) }) { Text("Retake") }
                    }
                }
            }
        }
    }
}

private fun formatTarget(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()

@Composable
private fun PoseCameraBox(
    viewModel: TrackingViewModel,
    skeletonPoints: Map<Int, com.wefit.app.tracking.PosePoint>,
    currentAngle: Double?,
    repState: String?,
    durationSeconds: Int,
    showCamera: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // One tree for both modes: the camera view stays alive (1 dp) when hidden so tracking keeps working.
        Box(modifier = Modifier.fillMaxWidth().height(if (showCamera) 260.dp else 1.dp)) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewReady = { previewView -> viewModel.getPoseStrategy()?.attachPreviewSurface(previewView) }
            )
            if (showCamera) {
                val isFront = viewModel.getPoseStrategy()?.isFrontCamera ?: true
                SkeletonOverlay(points = skeletonPoints, mirror = isFront, modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text("${durationSeconds}s", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(currentAngle?.let { "%.1f°".format(it) } ?: "—", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    if (repState != null) Text("State: $repState", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (!showCamera) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${durationSeconds}s", style = MaterialTheme.typography.headlineSmall)
                    Text(currentAngle?.let { "Angle: %.0f°".format(it) } ?: "Angle: —")
                    if (repState != null) Text("State: $repState")
                }
            }
        }
    }
}

/** Setup tips first, then the teacher's instructions, then the demonstration below them. */
@Composable
private fun InstructionsBlock(exerciseType: String, details: AssignmentDetailsDto?) {
    SetupTipsCard(exerciseType)

    val text = details?.instructions
    if (!text.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Teacher's Instructions", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    val path = details?.media_path
    val type = details?.media_type
    if (!path.isNullOrBlank() && !type.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Demonstration", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                DemoMedia(type = type, url = ApiConfig.BASE_URL + path)
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