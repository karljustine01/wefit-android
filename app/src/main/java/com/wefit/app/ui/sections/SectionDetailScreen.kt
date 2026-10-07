package com.wefit.app.ui.sections

import com.wefit.app.ui.components.WeFitCard
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
fun SectionDetailScreen(
    sectionId: Int,
    isOwner: Boolean,
    onCreateAssignment: (sectionId: Int) -> Unit,
    onAssignmentSelected: (assignmentId: Int) -> Unit,
    onAssignmentSelectedForTracking: (assignmentId: Int, exerciseType: String, exerciseName: String) -> Unit,
    onViewMembers: (sectionId: Int, isOwner: Boolean) -> Unit,
    viewModel: SectionDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(sectionId) {
        viewModel.load(sectionId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(state.section?.name ?: "Section") }) },
        floatingActionButton = {
            if (isOwner) {
                FloatingActionButton(
                    onClick = { onCreateAssignment(sectionId) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("+")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    Text("Error: ${state.error}", modifier = Modifier.align(Alignment.Center))
                }
                state.section != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            WeFitCard(modifier = Modifier.fillMaxWidth()) {
                                state.section?.description?.let { Text(it) }
                                Spacer(Modifier.height(4.dp))
                                Text("Join Code: ${state.section?.section_code}", style = MaterialTheme.typography.titleMedium)
                                Text("Members: ${state.section?.members?.size ?: 0}")
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { onViewMembers(sectionId, isOwner) }) {
                                    Text("View Members")
                                }
                            }
                        }
                        item {
                            Text("Assignments", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(4.dp))
                        }
                        if (state.assignments.isEmpty()) {
                            item {
                                Text("No assignments yet" + if (isOwner) " — tap + to create one" else "")
                            }
                        } else {
                            items(state.assignments) { assignment ->
                                WeFitCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (isOwner) {
                                            onAssignmentSelected(assignment.id)
                                        } else {
                                            val exercise = assignment.exercise
                                            if (exercise != null) {
                                                onAssignmentSelectedForTracking(assignment.id, exercise.exercise_type, exercise.name)
                                            }
                                        }
                                    }
                                ) {
                                    Text(assignment.exercise?.name ?: "Unknown Exercise", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    StatusPill(assignment.status, tone = PillTone.Success)
                                    assignment.deadline?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Deadline: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isOwner) {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Tap to view student progress & grade", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}