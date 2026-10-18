package com.wefit.app.ui.assignments

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.wefit.app.tracking.ExerciseUnits
import com.wefit.app.ui.components.WeFitButton
import com.wefit.app.ui.components.WeFitCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentScreen(
    preselectedSectionId: Int? = null,
    onCreated: () -> Unit,
    viewModel: CreateAssignmentViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val preselected by viewModel.preselectedSectionId.collectAsState()

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseName by remember { mutableStateOf("") }
    var selectedExerciseType by remember { mutableStateOf("") }
    var exerciseExpanded by remember { mutableStateOf(false) }

    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var selectedSectionName by remember { mutableStateOf("") }
    var sectionExpanded by remember { mutableStateOf(false) }

    var hasTarget by remember { mutableStateOf(false) }
    var targetValue by remember { mutableStateOf("") }

    var hasDeadline by remember { mutableStateOf(false) }
    var deadline by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var instructions by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaIsVideo by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val unit = if (selectedExerciseType.isBlank()) null else ExerciseUnits.targetUnit(selectedExerciseType)
    val isFixedDistance = selectedExerciseType == "50m_dash" || selectedExerciseType == "mile_walk"

    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            mediaUri = uri
            mediaIsVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
        }
    }

    LaunchedEffect(state.success) { if (state.success) onCreated() }

    LaunchedEffect(selectedExerciseType) {
        if (selectedExerciseType.isNotBlank() && ExerciseUnits.targetUnit(selectedExerciseType) == null) {
            hasTarget = false
            targetValue = ""
        }
    }

    LaunchedEffect(state.sections, preselectedSectionId) {
        if (preselectedSectionId != null && state.sections.isNotEmpty()) viewModel.preselectSection(preselectedSectionId)
    }
    LaunchedEffect(preselected) {
        preselected?.let { id ->
            state.sections.find { it.id == id }?.let {
                selectedSectionId = it.id
                selectedSectionName = it.name
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Create Assignment") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                WeFitCard(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(expanded = exerciseExpanded, onExpandedChange = { exerciseExpanded = it }) {
                        OutlinedTextField(
                            value = selectedExerciseName, onValueChange = {}, readOnly = true,
                            label = { Text("Exercise") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = exerciseExpanded,
                            onDismissRequest = { exerciseExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            state.exercises.forEach { exercise ->
                                DropdownMenuItem(
                                    text = { Text(exercise.name) },
                                    onClick = {
                                        selectedExerciseId = exercise.id
                                        selectedExerciseName = exercise.name
                                        selectedExerciseType = exercise.exercise_type
                                        exerciseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(expanded = sectionExpanded, onExpandedChange = { sectionExpanded = it }) {
                        OutlinedTextField(
                            value = selectedSectionName, onValueChange = {}, readOnly = true,
                            label = { Text("Section") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sectionExpanded,
                            onDismissRequest = { sectionExpanded = false },
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            state.sections.forEach { section ->
                                DropdownMenuItem(
                                    text = { Text(section.name) },
                                    onClick = {
                                        selectedSectionId = section.id
                                        selectedSectionName = section.name
                                        sectionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    when {
                        isFixedDistance -> Text(
                            if (selectedExerciseType == "50m_dash") "This test ends automatically at 50 meters."
                            else "This test ends automatically at 1.0 mile.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        unit != null -> {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(checked = hasTarget, onCheckedChange = { hasTarget = it; if (!it) targetValue = "" })
                                Text("Set a target (auto-completes when reached)")
                            }
                            if (hasTarget) {
                                OutlinedTextField(
                                    value = targetValue,
                                    onValueChange = { targetValue = it; localError = null },
                                    label = { Text("Target ($unit)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        selectedExerciseType.isNotBlank() -> Text(
                            "This exercise is entered manually, so no target applies.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = hasDeadline, onCheckedChange = { hasDeadline = it; if (!it) deadline = "" })
                        Text("Set a deadline")
                    }
                    if (hasDeadline) {
                        OutlinedTextField(
                            value = deadline, onValueChange = {}, readOnly = true,
                            label = { Text("Deadline") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                WeFitCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Instructions for students", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Details / instructions (optional)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Demonstration (optional)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (mediaUri == null) {
                        OutlinedButton(onClick = {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add image or video")
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Images are shrunk automatically. Videos must be 10 MB or smaller.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        if (mediaIsVideo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Video selected")
                            }
                        } else {
                            AsyncImage(
                                model = mediaUri,
                                contentDescription = "Selected demonstration image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                        TextButton(onClick = { mediaUri = null }) { Text("Remove") }
                    }
                }

                val shownError = localError ?: state.error
                if (shownError != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(shownError, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(24.dp))

                if (state.createdAssignmentId != null && state.error != null) {
                    // Assignment exists already; only the extras failed. Avoid creating a duplicate.
                    WeFitButton(
                        text = "Retry upload",
                        onClick = { viewModel.retryDetails(instructions, mediaUri) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { viewModel.skipDetails() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Skip and finish")
                    }
                } else {
                    WeFitButton(
                        text = "Create Assignment",
                        onClick = {
                            val parsedTarget = if (hasTarget) targetValue.trim().replace(',', '.').toDoubleOrNull() else null
                            when {
                                hasTarget && (parsedTarget == null || parsedTarget <= 0.0) ->
                                    localError = "Enter a target greater than 0 (numbers only), or untick the option."
                                hasDeadline && deadline.isBlank() ->
                                    localError = "Pick a deadline date, or untick the option."
                                else -> {
                                    localError = null
                                    selectedExerciseId?.let { exId ->
                                        viewModel.createAssignment(
                                            exerciseId = exId,
                                            sectionId = selectedSectionId,
                                            targetValue = parsedTarget,
                                            deadline = if (hasDeadline) deadline else null,
                                            instructions = instructions.trim().ifBlank { null },
                                            mediaUri = mediaUri
                                        )
                                    }
                                }
                            }
                        },
                        enabled = selectedExerciseId != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        deadline = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}