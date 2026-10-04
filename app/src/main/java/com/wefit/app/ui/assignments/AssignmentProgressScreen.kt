package com.wefit.app.ui.assignments

import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitButton
import com.wefit.app.ui.components.WeFitEmpty
import com.wefit.app.ui.components.WeFitError
import com.wefit.app.ui.components.StatusPill
import com.wefit.app.ui.components.PillTone
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentProgressScreen(
    assignmentId: Int,
    viewModel: AssignmentProgressViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var gradingUserId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(assignmentId) {
        viewModel.load(assignmentId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Student Progress") }) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    WeFitError(
                        message = state.error ?: "",
                        onRetry = { viewModel.load(assignmentId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.sessions.isEmpty() -> {
                    WeFitEmpty(message = "No student submissions yet", modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sessions) { session ->
                            WeFitCard(modifier = Modifier.fillMaxWidth()) {
                                Text(session.user?.name ?: "Unknown Student", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                StatusPill(
                                    session.status,
                                    tone = if (session.status == "completed") PillTone.Success else PillTone.Neutral
                                )
                                Spacer(Modifier.height(8.dp))
                                if (session.repetitions != null && session.repetitions > 0) {
                                    Text("Reps: ${session.repetitions}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (session.distance != null && session.distance > 0) {
                                    Text("Distance: ${"%.1f".format(session.distance)}m", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Progress: ${session.progress_percentage.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(10.dp))
                                if (session.status == "completed") {
                                    WeFitButton(
                                        text = "Grade",
                                        onClick = { gradingUserId = session.user_id },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (state.gradeSuccessUserId == session.user_id) {
                                        Spacer(Modifier.height(6.dp))
                                        StatusPill("Grade posted ✓", PillTone.Success)
                                    }
                                } else {
                                    Text("Not yet completed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val targetUserId = gradingUserId
    if (targetUserId != null) {
        var gradeValue by remember { mutableStateOf("") }
        var remarks by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { gradingUserId = null },
            title = { Text("Post Grade") },
            text = {
                Column {
                    OutlinedTextField(
                        value = gradeValue,
                        onValueChange = { gradeValue = it },
                        label = { Text("Grade (0-100)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Remarks (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val g = gradeValue.toDoubleOrNull()
                    if (g != null) {
                        viewModel.postGrade(assignmentId, targetUserId, g, remarks.ifBlank { null })
                        gradingUserId = null
                    }
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { gradingUserId = null }) { Text("Cancel") }
            }
        )
    }
}