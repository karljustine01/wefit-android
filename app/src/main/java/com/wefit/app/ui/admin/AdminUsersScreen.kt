package com.wefit.app.ui.admin

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
fun AdminUsersScreen(viewModel: AdminUsersViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var editingUser by remember { mutableStateOf<Int?>(null) }

    val roles = listOf("ADMIN", "TEACHER", "COACH", "STUDENT", "VARSITY")
    val statuses = listOf("active", "inactive", "suspended")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Manage Users") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it; viewModel.load(it.ifBlank { null }) },
                label = { Text("Search by name or email") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.load() }) { Text("Retry") }
                        }
                    }
                    state.users.isEmpty() -> {
                        Text("No users found", modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.users) { user ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text(user.name, style = MaterialTheme.typography.titleLarge)
                                        Text(user.email)
                                        Spacer(Modifier.height(8.dp))
                                        Text("Role: ${user.role}  •  Status: ${user.status}")
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(onClick = {
                                            editingUser = if (editingUser == user.id) null else user.id
                                        }) {
                                            Text(if (editingUser == user.id) "Close" else "Edit")
                                        }

                                        if (editingUser == user.id) {
                                            Text("Change role:", style = MaterialTheme.typography.labelLarge)
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                roles.forEach { role ->
                                                    TextButton(onClick = { viewModel.updateRole(user.id, role) }) {
                                                        Text(role, style = MaterialTheme.typography.labelLarge)
                                                    }
                                                }
                                            }
                                            Text("Change status:", style = MaterialTheme.typography.labelLarge)
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                statuses.forEach { status ->
                                                    TextButton(onClick = { viewModel.updateStatus(user.id, status) }) {
                                                        Text(status, style = MaterialTheme.typography.labelLarge)
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
            }
        }
    }
}