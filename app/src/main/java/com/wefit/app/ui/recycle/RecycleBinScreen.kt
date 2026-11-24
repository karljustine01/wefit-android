package com.wefit.app.ui.recycle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.data.remote.dto.RecycleItemDto
import com.wefit.app.ui.components.PillTone
import com.wefit.app.ui.components.StatusPill
import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitEmpty
import com.wefit.app.ui.components.WeFitError
import com.wefit.app.ui.navigation.AuthStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: RecycleBinViewModel = viewModel(),
    authStateViewModel: AuthStateViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val role by authStateViewModel.role.collectAsState()
    val isAdmin = role == "ADMIN"
    val snackbar = remember { SnackbarHostState() }

    var confirmDelete by remember { mutableStateOf<RecycleItemDto?>(null) }
    var confirmEmpty by remember { mutableStateOf(false) }

    LaunchedEffect(role) { if (role != null) viewModel.load(isAdmin) }
    LaunchedEffect(state.message, state.error) {
        (state.message ?: state.error)?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Recycle Bin") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val bin = state.bin
            when {
                state.isLoading && bin == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                bin == null ->
                    WeFitError(state.error ?: "Could not load", onRetry = { viewModel.load(isAdmin) }, modifier = Modifier.fillMaxSize())
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (isAdmin) {
                        state.stats?.let { s ->
                            item {
                                WeFitCard(Modifier.fillMaxWidth()) {
                                    Text("Storage & auto-cleanup", style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    val mb = "%.1f".format(s.video_bytes / 1048576.0)
                                    Text("Videos: ${s.videos} ($mb MB) • kept ${s.policy.videos_days} days")
                                    Text("Notifications: ${s.notifications} • read ones kept ${s.policy.notifications_read_days} days")
                                    Text("Activity logs: ${s.activity_logs} • kept ${s.policy.activity_logs_days} days")
                                    Text("GPS/rep samples: ${s.progress_points} • kept ${s.policy.progress_points_days} days")
                                    Text("Deleted accounts: personal data erased after ${s.policy.deleted_account_days} days")
                                    Text("Bin items: ${s.bin_items} • purged after ${s.policy.bin_days} days")
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "${s.pending_cleanup} rows are due for cleanup. It runs automatically every night at 02:00.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    s.last_runs.firstOrNull()?.let {
                                        Text(
                                            "Last run: ${it.created_at} (${it.trigger}, ${it.total_rows} rows)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = { viewModel.runCleanup(true) }, enabled = !state.busy) {
                                        Text("Run cleanup now")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Deleted items are kept ${bin.retention_days} days, then removed for good.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (bin.items.isNotEmpty()) {
                                TextButton(onClick = { confirmEmpty = true }, enabled = !state.busy) { Text("Empty") }
                            }
                        }
                    }

                    if (bin.items.isEmpty()) {
                        item { WeFitEmpty("The recycle bin is empty", modifier = Modifier.fillMaxWidth().height(200.dp)) }
                    }

                    items(bin.items, key = { it.type + it.id }) { item ->
                        WeFitCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                                    Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(6.dp))
                                    StatusPill(
                                        if (item.days_left <= 0) "Removed soon" else "${item.days_left} day(s) left",
                                        if (item.days_left <= 3) PillTone.Error else PillTone.Neutral
                                    )
                                }
                                IconButton(onClick = { viewModel.restore(item.type, item.id, isAdmin) }, enabled = !state.busy) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restore ${item.title}", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { confirmDelete = item }, enabled = !state.busy) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete ${item.title} forever", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete forever?") },
            text = { Text("\"${item.title}\" and its related progress and grades will be permanently removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteForever(item.type, item.id, isAdmin); confirmDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } }
        )
    }
    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Empty recycle bin?") },
            text = { Text("Everything in the bin will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.emptyBin(isAdmin); confirmEmpty = false }) { Text("Empty") }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel") } }
        )
    }
}
