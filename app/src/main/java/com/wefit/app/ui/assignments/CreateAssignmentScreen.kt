package com.wefit.app.ui.assignments

import com.wefit.app.ui.components.WeFitButton
import com.wefit.app.ui.components.WeFitCard
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentScreen(
    preselectedSectionId: Int? = null,
    onCreated: () -> Unit,
    viewModel: CreateAssignmentViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val preselected by viewModel.preselectedSectionId.collectAsState()

    var selectedExerciseId by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseName by remember { mutableStateOf("") }
    var exerciseExpanded by remember { mutableStateOf(false) }

    var selectedSectionId by remember { mutableStateOf<Int?>(null) }
    var selectedSectionName by remember { mutableStateOf("") }
    var sectionExpanded by remember { mutableStateOf(false) }

    var targetValue by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    LaunchedEffect(state.success) {
        if (state.success) onCreated()
    }

    LaunchedEffect(state.sections, preselectedSectionId) {
        if (preselectedSectionId != null && state.sections.isNotEmpty()) {
            viewModel.preselectSection(preselectedSectionId)
        }
    }

    LaunchedEffect(preselected) {
        preselected?.let { id ->
            state.sections.find { it.id == id }?.let {
                selectedSectionId = it.id
                selectedSectionName = it.name
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Create Assignment") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator()
            } else {
                WeFitCard(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(expanded = exerciseExpanded, onExpandedChange = { exerciseExpanded = it }) {
                        OutlinedTextField(
                            value = selectedExerciseName,
                            onValueChange = {},
                            readOnly = true,
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
                                        exerciseExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = sectionExpanded,
                        onExpandedChange = { sectionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedSectionName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Section") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
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

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it },
                        label = { Text("Target Value (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Deadline (YYYY-MM-DD, optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(24.dp))
                WeFitButton(
                    text = "Create Assignment",
                    onClick = {
                        selectedExerciseId?.let { exId ->
                            viewModel.createAssignment(
                                exerciseId = exId,
                                sectionId = selectedSectionId,
                                targetValue = targetValue.toDoubleOrNull(),
                                deadline = deadline.ifBlank { null }
                            )
                        }
                    },
                    enabled = selectedExerciseId != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}