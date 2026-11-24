package com.wefit.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolYearScreen(
    onOpenRoster: () -> Unit = {},
    viewModel: SchoolYearViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var showStart by remember { mutableStateOf(false) }
    var showComplete by remember { mutableStateOf(false) }
    var yearInput by remember { mutableStateOf("") }

    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("School Year") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val d = state.data
            when {
                state.isLoading && d == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                d == null -> WeFitError(state.error ?: "Could not load", onRetry = { viewModel.load() }, modifier = Modifier.fillMaxSize())
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeFitCard(Modifier.fillMaxWidth()) {
                        Text("Current school year", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(d.school_year ?: "Not started", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Students enrolled in PE: ${d.enrolled_students} (${d.registered_enrolled} registered)")
                        Text("Old accounts waiting to be closed: ${d.pending_completion}")
                        Text("Closed accounts (grades kept ${d.completed_retention_days} days): ${d.completed_accounts}")
                    }

                    WeFitCard(Modifier.fillMaxWidth()) {
                        Text("Every year, in this order", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("1. Start the new school year.")
                        Text("2. Import the new PE roster (CSV). Only IDs on it can register as students.")
                        Text("3. Close the accounts of students who finished PE.")
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Returning students (e.g. repeating PE) who are on the new roster get their account back automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(onClick = { showStart = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text("1. Start new school year")
                    }
                    OutlinedButton(onClick = onOpenRoster, modifier = Modifier.fillMaxWidth()) {
                        Text("2. Import PE roster")
                    }
                    Button(
                        onClick = { showComplete = true },
                        enabled = !state.busy && d.school_year != null && d.pending_completion > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("3. Close accounts of finished students (${d.pending_completion})") }
                }
            }
        }
    }

    if (showStart) {
        AlertDialog(
            onDismissRequest = { showStart = false },
            title = { Text("Start new school year") },
            text = {
                OutlinedTextField(
                    value = yearInput,
                    onValueChange = { yearInput = it },
                    label = { Text("School year, e.g. 2027-2028") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.startYear(yearInput); showStart = false },
                    enabled = yearInput.isNotBlank()
                ) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showStart = false }) { Text("Cancel") } }
        )
    }
    if (showComplete) {
        AlertDialog(
            onDismissRequest = { showComplete = false },
            title = { Text("Close finished accounts?") },
            text = {
                Text(
                    "Students who are not on the ${state.data?.school_year} roster will no longer be able to log in and will be removed from their classes. " +
                        "Their grades are kept for ${state.data?.completed_retention_days} days, then their personal data is erased."
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.completeYear(); showComplete = false }) { Text("Close accounts") } },
            dismissButton = { TextButton(onClick = { showComplete = false }) { Text("Cancel") } }
        )
    }
}
