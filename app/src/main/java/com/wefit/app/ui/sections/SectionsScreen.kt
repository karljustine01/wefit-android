package com.wefit.app.ui.sections

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(
    userRole: String,
    onSectionSelected: (sectionId: Int, isOwner: Boolean) -> Unit = { _, _ -> },
    viewModel: SectionsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    val canCreate = userRole in listOf("TEACHER", "COACH", "ADMIN")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sections") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (canCreate) showCreateDialog = true else showJoinDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(if (canCreate) "+" else "Join")
            }
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = state.isLoading),
            onRefresh = { viewModel.loadSections() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.error != null -> {
                    WeFitError(
                        message = state.error ?: "",
                        onRetry = { viewModel.loadSections() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.sections.isEmpty() && !state.isLoading -> {
                    WeFitEmpty(
                        message = "No sections yet — pull down to refresh",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.sections) { section ->
                            WeFitCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onSectionSelected(section.id, canCreate) }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(section.name, style = MaterialTheme.typography.titleMedium)
                                        section.description?.let {
                                            Spacer(Modifier.height(2.dp))
                                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        StatusPill("${section.members_count ?: 0} members", PillTone.Neutral)
                                    }
                                    Text(section.section_code, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Section") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createSection(name, description.ifBlank { null })
                    showCreateDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showJoinDialog) {
        var code by remember { mutableStateOf("") }
        var joinError by remember { mutableStateOf<String?>(null) }
        var isJoining by remember { mutableStateOf(false) }

        LaunchedEffect(state.actionMessage) {
            if (state.actionMessage != null && isJoining) {
                showJoinDialog = false
                isJoining = false
            }
        }
        LaunchedEffect(state.error) {
            if (state.error != null && isJoining) {
                joinError = state.error
                isJoining = false
            }
        }

        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Join Section") },
            text = {
                Column {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.trim().uppercase(); joinError = null },
                        label = { Text("Section Code") }
                    )
                    if (joinError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(joinError ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearMessages()
                    isJoining = true
                    viewModel.joinSection(code.trim())
                }) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) { Text("Cancel") }
            }
        )
    }
}