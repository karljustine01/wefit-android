package com.wefit.app.ui.admin

import androidx.compose.ui.Alignment
import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.components.WeFitEmpty
import com.wefit.app.ui.components.WeFitError
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
fun ActivityLogScreen(viewModel: ActivityLogViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var search by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Activity Log") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; viewModel.load(it) },
                label = { Text("Filter by action (e.g. login, register)") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> WeFitError(state.error ?: "", onRetry = { viewModel.load() }, modifier = Modifier.fillMaxSize())
                    state.logs.isEmpty() -> WeFitEmpty("No activity logs found", modifier = Modifier.fillMaxSize())
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.logs) { log ->
                                WeFitCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(log.action, style = MaterialTheme.typography.titleMedium)
                                    log.description?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${log.user?.name ?: "Unknown user"} • ${log.created_at}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}