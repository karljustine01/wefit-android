package com.wefit.app.ui.assignments

import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitEmpty
import com.wefit.app.ui.components.WeFitError
import com.wefit.app.ui.components.StatusPill
import com.wefit.app.ui.components.PillTone
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    onAssignmentSelected: (assignmentId: Int, exerciseType: String, exerciseName: String) -> Unit,
    viewModel: AssignmentsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredAssignments = remember(state.assignments, searchQuery) {
        if (searchQuery.isBlank()) state.assignments
        else state.assignments.filter {
            it.exercise?.name?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Assignments") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search exercises") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing = state.isLoading),
                onRefresh = { viewModel.loadAssignments() },
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.error != null -> {
                        WeFitError(
                            message = state.error ?: "",
                            onRetry = { viewModel.loadAssignments() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    filteredAssignments.isEmpty() && !state.isLoading -> {
                        WeFitEmpty(
                            message = if (searchQuery.isBlank()) "No assignments yet — pull down to refresh" else "No matches",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredAssignments) { assignment ->
                                val exercise = assignment.exercise
                                WeFitCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (exercise != null) {
                                            onAssignmentSelected(assignment.id, exercise.exercise_type, exercise.name)
                                        }
                                    }
                                ) {
                                    Text(exercise?.name ?: "Unknown Exercise", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(6.dp))
                                    StatusPill(
                                        assignment.status,
                                        tone = if (assignment.status == "active") PillTone.Success else PillTone.Neutral
                                    )
                                    assignment.deadline?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text("Due: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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