package com.wefit.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wefit.app.ui.components.WeFitCard
import com.wefit.app.ui.login.AuthViewModel
import com.wefit.app.ui.navigation.AuthStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLoggedOut: () -> Unit = {},
    onNavigateToSections: () -> Unit = {},
    onNavigateToAssignments: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCreateAssignment: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToAdminUsers: () -> Unit = {},
    onNavigateToActivityLog: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    authStateViewModel: AuthStateViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val role by authStateViewModel.role.collectAsState()
    val isTeacherOrCoach = role in listOf("TEACHER", "COACH", "ADMIN")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WeFit", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { authViewModel.logout(onLoggedOut) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            when {
                state.isLoading -> {}
                state.error != null -> {
                    WeFitCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Backend connection failed", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(state.error ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { viewModel.checkBackend() }) { Text("Retry") }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                state.connected -> {
                    WeFitCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("You're all set", style = MaterialTheme.typography.titleMedium)
                                Text("Connected to WeFit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            Text("Quick Access", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            QuickAccessGrid(
                items = buildList {
                    add(QuickAccessItem("Assignments", Icons.Default.Assignment, onNavigateToAssignments))
                    add(QuickAccessItem("Sections", Icons.Default.Group, onNavigateToSections))
                    add(QuickAccessItem("Analytics", Icons.Default.BarChart, onNavigateToAnalytics))
                    add(QuickAccessItem("Notifications", Icons.Default.Notifications, onNavigateToNotifications))
                }
            )

            if (isTeacherOrCoach) {
                Spacer(Modifier.height(28.dp))
                Text("Teacher Tools", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                WeFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToCreateAssignment
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Create Assignment", style = MaterialTheme.typography.titleMedium)
                            Text("Assign an exercise to a section", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (role == "ADMIN") {
                Spacer(Modifier.height(28.dp))
                Text("Admin Tools", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                WeFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToAdminUsers
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Manage Users", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                WeFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToActivityLog
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Activity Log", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private data class QuickAccessItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickAccessGrid(items: List<QuickAccessItem>) {
    Column {
        items.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    WeFitCard(
                        modifier = Modifier.weight(1f).padding(4.dp),
                        onClick = item.onClick
                    ) {
                        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text(item.label, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}