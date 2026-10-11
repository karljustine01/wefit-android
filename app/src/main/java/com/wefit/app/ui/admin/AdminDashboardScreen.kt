package com.wefit.app.ui.admin

import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitError
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AdminDashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("System Overview") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> WeFitError(state.error ?: "", onRetry = { viewModel.load() }, modifier = Modifier.fillMaxSize())
                state.data != null -> {
                    val d = state.data!!
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        listOf(
                            "Users" to d.total_users, "Sections" to d.total_sections,
                            "Exercises" to d.total_exercises, "Assignments" to d.total_assignments,
                            "Completed Sessions" to d.completed_sessions
                        ).forEach { (label, value) ->
                            WeFitCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
                                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Users by Role", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        d.users_by_role.forEach { (role, count) ->
                            val share = if (d.total_users > 0) count.toFloat() / d.total_users else 0f
                            WeFitCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text("$role: $count", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }
}