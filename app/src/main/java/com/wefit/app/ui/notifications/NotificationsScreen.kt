package com.wefit.app.ui.notifications

import com.wefit.app.ui.components.WeFitCard
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: NotificationsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                actions = {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Text("Mark all read")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> {
                    WeFitError(
                        message = state.error ?: "",
                        onRetry = { viewModel.load() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.notifications.isEmpty() -> {
                    WeFitEmpty(message = "No notifications", modifier = Modifier.fillMaxSize())
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.notifications) { notification ->
                            WeFitCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { if (!notification.is_read) viewModel.markRead(notification.id) }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            notification.title,
                                            fontWeight = if (notification.is_read) FontWeight.Normal else FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(notification.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(6.dp))
                                        Text(notification.created_at, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!notification.is_read) {
                                        StatusPill("New", PillTone.Success)
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